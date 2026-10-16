package com.selfblog.service.social.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.BlogFollow;
import com.selfblog.entity.SysUser;
import com.selfblog.event.FollowCreatedEvent;
import com.selfblog.mapper.BlogFollowMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.FollowService;
import com.selfblog.utils.FineGrainedLockManager;
import com.selfblog.utils.UserConverter;
import com.selfblog.vo.social.FollowStatusVO;
import com.selfblog.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 关注服务实现类
 */
@Slf4j
@Service
public class FollowServiceImpl extends ServiceImpl<BlogFollowMapper, BlogFollow> implements FollowService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private BlogFollowMapper blogFollowMapper;

    private final FineGrainedLockManager lockManager = FineGrainedLockManager.getInstance();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FollowStatusVO follow(Long targetUserId, Long currentUserId) {
        // 参数校验
        if (currentUserId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (targetUserId == null) {
            throw new BusinessException(400, "目标用户ID不能为空");
        }
        // 不能关注自己
        if (Objects.equals(targetUserId, currentUserId)) {
            throw new BusinessException(400, "不能关注自己");
        }

        // 检查目标用户是否存在
        SysUser targetUser = sysUserMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(404, "用户不存在");
        }

        FollowStatusVO result = new FollowStatusVO();

        // 使用细粒度锁
        String lockKey = currentUserId + "-" + targetUserId;
        synchronized (lockManager.getLock(lockKey)) {
            // 检查是否已关注
            LambdaQueryWrapper<BlogFollow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlogFollow::getFollowerId, currentUserId)
                  .eq(BlogFollow::getFollowingId, targetUserId);
            BlogFollow existingFollow = this.getOne(wrapper);

            if (existingFollow != null) {
                if (existingFollow.getIsDeleted() != null && existingFollow.getIsDeleted() == 1) {
                    blogFollowMapper.logicalRestoreById(existingFollow.getId());
                    sysUserMapper.incrementFollowerCount(targetUserId);
                    sysUserMapper.incrementFollowingCount(currentUserId);
                    result.setFollowing(true);
                    result.setAction("follow");
                    log.info("用户重新关注: followerId={}, followingId={}", currentUserId, targetUserId);
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishEvent(new FollowCreatedEvent(currentUserId, targetUserId));
                        }
                    });
                } else {
                    result.setFollowing(true);
                    result.setAction("already_following");
                }
            } else {
                // 尝试关注
                BlogFollow newFollow = new BlogFollow();
                newFollow.setFollowerId(currentUserId);
                newFollow.setFollowingId(targetUserId);
                try {
                    this.save(newFollow);
                    // 原子更新计数
                    sysUserMapper.incrementFollowerCount(targetUserId);
                    sysUserMapper.incrementFollowingCount(currentUserId);
                    result.setFollowing(true);
                    result.setAction("follow");
                    log.info("用户关注成功: followerId={}, followingId={}", currentUserId, targetUserId);
                    // 发布关注事件，事务提交后异步发送通知
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishEvent(new FollowCreatedEvent(currentUserId, targetUserId));
                        }
                    });
                } catch (DataAccessException e) {
                    // 并发冲突：记录已存在，使用绕过 @TableLogic 的查询确认实际状态
                    // 捕获父类 DataAccessException：SQLite 的约束错误是 UncategorizedSQLException。
                    BlogFollow concurrentFollow = blogFollowMapper.selectRawByFollowerAndFollowing(currentUserId, targetUserId);
                    if (concurrentFollow != null) {
                        if (concurrentFollow.getIsDeleted() != null && concurrentFollow.getIsDeleted() == 1) {
                            // 软删除状态 → 恢复关注
                            blogFollowMapper.logicalRestoreById(concurrentFollow.getId());
                            sysUserMapper.incrementFollowerCount(targetUserId);
                            sysUserMapper.incrementFollowingCount(currentUserId);
                            result.setFollowing(true);
                            result.setAction("follow");
                            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    eventPublisher.publishEvent(new FollowCreatedEvent(currentUserId, targetUserId));
                                }
                            });
                        } else {
                            // 活跃状态 → 已关注
                            result.setFollowing(true);
                            result.setAction("already_following");
                        }
                    } else {
                        // 理论上不应到达此处（DuplicateKeyException 意味着记录存在），但为安全起见尝试创建
                        log.warn("关注遇到DuplicateKeyException但未找到记录，重新尝试: followerId={}, followingId={}", currentUserId, targetUserId);
                        BlogFollow retryFollow = new BlogFollow();
                        retryFollow.setFollowerId(currentUserId);
                        retryFollow.setFollowingId(targetUserId);
                        this.save(retryFollow);
                        sysUserMapper.incrementFollowerCount(targetUserId);
                        sysUserMapper.incrementFollowingCount(currentUserId);
                        result.setFollowing(true);
                        result.setAction("follow");
                    }
                }
            }

            // 获取实时关注计数，避免基于缓存值+1/-1计算的TOCTOU风险
            SysUser updatedTargetUser = sysUserMapper.selectById(targetUserId);
            SysUser updatedCurrentUser = sysUserMapper.selectById(currentUserId);
            result.setFollowerCount(updatedTargetUser != null ? updatedTargetUser.getFollowerCount() : 0);
            result.setFollowingCount(updatedCurrentUser != null ? updatedCurrentUser.getFollowingCount() : 0);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FollowStatusVO unfollow(Long targetUserId, Long currentUserId) {
        // 参数校验
        if (currentUserId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (targetUserId == null) {
            throw new BusinessException(400, "目标用户ID不能为空");
        }
        // 不能取消关注自己
        if (Objects.equals(targetUserId, currentUserId)) {
            throw new BusinessException(400, "不能取消关注自己");
        }

        // 检查目标用户是否存在
        SysUser targetUser = sysUserMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(404, "用户不存在");
        }

        FollowStatusVO result = new FollowStatusVO();

        // 使用细粒度锁
        String lockKey = currentUserId + "-" + targetUserId;
        synchronized (lockManager.getLock(lockKey)) {
            // 检查是否已关注
            LambdaQueryWrapper<BlogFollow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlogFollow::getFollowerId, currentUserId)
                  .eq(BlogFollow::getFollowingId, targetUserId);
            BlogFollow existingFollow = this.getOne(wrapper);

            if (existingFollow == null) {
                // 未关注
                result.setFollowing(false);
                result.setAction("not_following");
            } else {
                // 取消关注：逻辑删除记录（解决软删除+唯一约束冲突）
                blogFollowMapper.logicalDeleteById(existingFollow.getId());
                // 原子更新计数
                sysUserMapper.decrementFollowerCount(targetUserId);
                sysUserMapper.decrementFollowingCount(currentUserId);
                result.setFollowing(false);
                result.setAction("unfollow");
                log.info("用户取消关注: followerId={}, followingId={}", currentUserId, targetUserId);
            }

            // 获取实时关注计数，避免基于缓存值+1/-1计算的TOCTOU风险
            SysUser updatedTargetUser = sysUserMapper.selectById(targetUserId);
            SysUser updatedCurrentUser = sysUserMapper.selectById(currentUserId);
            result.setFollowerCount(updatedTargetUser != null ? updatedTargetUser.getFollowerCount() : 0);
            result.setFollowingCount(updatedCurrentUser != null ? updatedCurrentUser.getFollowingCount() : 0);
        }

        return result;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.STATUS_CACHE, key = "#currentUserId + ':' + #targetUserId")
    @Transactional(readOnly = true)
    public boolean isFollowing(Long targetUserId, Long currentUserId) {
        if (currentUserId == null || targetUserId == null) {
            return false;
        }
        LambdaQueryWrapper<BlogFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogFollow::getFollowerId, currentUserId)
              .eq(BlogFollow::getFollowingId, targetUserId)
              .ne(BlogFollow::getIsDeleted, 1)
              .last("LIMIT 1");
        return this.getOne(wrapper) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVO> getFollowers(Long userId) {
        // 检查用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 查询粉丝
        LambdaQueryWrapper<BlogFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogFollow::getFollowingId, userId)
              .orderByDesc(BlogFollow::getCreateTime);
        List<BlogFollow> follows = this.list(wrapper);

        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取粉丝用户信息，排除已封禁用户
        List<Long> followerIds = follows.stream()
                .map(BlogFollow::getFollowerId)
                .collect(java.util.stream.Collectors.toList());
        List<SysUser> followers = sysUserMapper.selectBatchIds(followerIds);

        return followers.stream()
                .filter(u -> u.getStatus() == null || u.getStatus() != 0)
                .map(UserConverter::toUserVO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVO> getFollowing(Long userId) {
        // 检查用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 查询关注
        LambdaQueryWrapper<BlogFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogFollow::getFollowerId, userId)
              .orderByDesc(BlogFollow::getCreateTime);
        List<BlogFollow> follows = this.list(wrapper);

        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取关注用户信息，排除已封禁用户
        List<Long> followingIds = follows.stream()
                .map(BlogFollow::getFollowingId)
                .collect(java.util.stream.Collectors.toList());
        List<SysUser> followings = sysUserMapper.selectBatchIds(followingIds);

        return followings.stream()
                .filter(u -> u.getStatus() == null || u.getStatus() != 0)
                .map(UserConverter::toUserVO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountsVO getCounts(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        FollowCountsVO counts = new FollowCountsVO();
        counts.setUserId(userId);
        counts.setFollowerCount(user.getFollowerCount() != null ? user.getFollowerCount() : 0);
        counts.setFollowingCount(user.getFollowingCount() != null ? user.getFollowingCount() : 0);
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<UserVO> getFollowers(Long userId, Integer page, Integer pageSize) {
        // 检查用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 分页查询粉丝
        Page<BlogFollow> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<BlogFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogFollow::getFollowingId, userId)
              .orderByDesc(BlogFollow::getCreateTime);
        IPage<BlogFollow> followPage = this.page(pageParam, wrapper);

        if (followPage.getRecords().isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        // 获取粉丝用户信息
        List<Long> followerIds = followPage.getRecords().stream()
                .map(BlogFollow::getFollowerId)
                .collect(java.util.stream.Collectors.toList());
        List<SysUser> followers = sysUserMapper.selectBatchIds(followerIds);
        Map<Long, SysUser> userMap = followers.stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 转换，过滤掉已封禁用户
        IPage<UserVO> resultPage = followPage.convert(follow -> {
            SysUser follower = userMap.get(follow.getFollowerId());
            if (follower != null && (follower.getStatus() == null || follower.getStatus() != 0)) {
                return UserConverter.toUserVO(follower);
            }
            return null;
        });
        resultPage.setRecords(resultPage.getRecords().stream().filter(Objects::nonNull).toList());
        return resultPage;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<UserVO> getFollowing(Long userId, Integer page, Integer pageSize) {
        // 检查用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 分页查询关注
        Page<BlogFollow> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<BlogFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogFollow::getFollowerId, userId)
              .orderByDesc(BlogFollow::getCreateTime);
        IPage<BlogFollow> followPage = this.page(pageParam, wrapper);

        if (followPage.getRecords().isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        // 获取关注用户信息
        List<Long> followingIds = followPage.getRecords().stream()
                .map(BlogFollow::getFollowingId)
                .collect(java.util.stream.Collectors.toList());
        List<SysUser> followings = sysUserMapper.selectBatchIds(followingIds);
        Map<Long, SysUser> userMap = followings.stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 转换，过滤掉已封禁用户
        IPage<UserVO> resultPage = followPage.convert(follow -> {
            SysUser following = userMap.get(follow.getFollowingId());
            if (following != null && (following.getStatus() == null || following.getStatus() != 0)) {
                return UserConverter.toUserVO(following);
            }
            return null;
        });
        resultPage.setRecords(resultPage.getRecords().stream().filter(Objects::nonNull).toList());
        return resultPage;
    }

}