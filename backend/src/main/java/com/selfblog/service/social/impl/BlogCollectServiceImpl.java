package com.selfblog.service.social.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.*;
import com.selfblog.event.CollectCreatedEvent;
import com.selfblog.mapper.*;
import com.selfblog.service.social.BlogCollectService;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.utils.FineGrainedLockManager;
import com.selfblog.vo.post.CollectItemVO;
import com.selfblog.vo.post.CollectResultVO;
import com.selfblog.vo.post.CollectStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类
 */
@Slf4j
@Service
public class BlogCollectServiceImpl extends ServiceImpl<BlogCollectMapper, BlogCollect> implements BlogCollectService {

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogPostTagMapper blogPostTagMapper;

    @Autowired
    private BlogTagMapper blogTagMapper;

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private BlogCollectMapper blogCollectMapper;

    private final FineGrainedLockManager lockManager = FineGrainedLockManager.getInstance();

    @Override
    @CacheEvict(value = CaffeineCacheConfig.STATUS_CACHE, key = "'collect:' + #userId + ':' + #postId")
    @Transactional(rollbackFor = Exception.class)
    public CollectResultVO toggleCollect(Long postId, Long userId) {
        // 检查文章是否存在
        BlogPost post = blogPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        CollectResultVO result = new CollectResultVO();

        // 使用细粒度锁：同一用户对同一文章的收藏操作串行执行
        String lockKey = userId + "-" + postId;
        synchronized (lockManager.getLock(lockKey)) {
            // 检查是否存在非删除的收藏记录（绕过 @TableLogic，兼容 is_deleted IS NULL 的历史数据）
            BlogCollect activeCollect = blogCollectMapper.selectActiveByUserAndPost(userId, postId);

            if (activeCollect != null) {
                // 取消收藏：逻辑删除记录（解决软删除+唯一约束冲突）
                blogCollectMapper.logicalDeleteById(activeCollect.getId());
                blogPostService.decrementCollectCount(postId);
                result.setAction("uncollect");
            } else {
                // 绕过 @TableLogic 查找任意状态的记录（兼容 is_deleted = 1 和 NULL）
                BlogCollect existingCollect = blogCollectMapper.selectRawByUserAndPost(userId, postId);

                if (existingCollect != null) {
                    if (existingCollect.getIsDeleted() != null && existingCollect.getIsDeleted() == 1) {
                        // 已软删除 → 恢复收藏（使用自定义SQL绕过@TableLogic）
                        blogCollectMapper.logicalRestoreById(existingCollect.getId());
                        blogPostService.incrementCollectCount(postId);
                        result.setAction("collect");
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                eventPublisher.publishEvent(new CollectCreatedEvent(userId, post.getUserId(), postId, post.getTitle()));
                            }
                        });
                    } else if (existingCollect.getIsDeleted() == null) {
                        // 历史遗留 NULL → 修复为 0，不修改收藏数（已计入，使用自定义SQL绕过@TableLogic）
                        blogCollectMapper.logicalRestoreById(existingCollect.getId());
                        result.setAction("collect");
                    } else {
                        // 正常活跃状态 → 取消收藏
                        blogCollectMapper.logicalDeleteById(existingCollect.getId());
                        blogPostService.decrementCollectCount(postId);
                        result.setAction("uncollect");
                    }
                } else {
                    // 首次收藏
                    BlogCollect newCollect = new BlogCollect();
                    newCollect.setUserId(userId);
                    newCollect.setPostId(postId);
                    newCollect.setIsDeleted(0);
                    boolean shouldPublishEvent = false;
                    try {
                        this.save(newCollect);
                        blogPostService.incrementCollectCount(postId);
                        result.setAction("collect");
                        shouldPublishEvent = true;
                    } catch (DataIntegrityViolationException e) {
                        log.warn("收藏保存遇到并发冲突，重新确认状态: userId={}, postId={}", userId, postId);
                        // 并发冲突：重新查询确认记录是否已被其他线程成功保存
                        BlogCollect confirmedCollect = blogCollectMapper.selectActiveByUserAndPost(userId, postId);
                        if (confirmedCollect != null) {
                            // 记录已存在（并发线程成功保存），不重复增加计数
                            result.setAction("collect");
                            shouldPublishEvent = true;
                        } else {
                            // 记录不存在（保存确实失败），不增加计数
                            result.setAction("collect_failed");
                        }
                    }
                    if (shouldPublishEvent) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                eventPublisher.publishEvent(new CollectCreatedEvent(userId, post.getUserId(), postId, post.getTitle()));
                            }
                        });
                    }
                }
            }

            // 获取实时收藏数，避免基于缓存值+1/-1计算的TOCTOU风险
            BlogPost updatedPost = blogPostMapper.selectById(postId);
            int realTimeCount = updatedPost != null && updatedPost.getCollectCount() != null ? updatedPost.getCollectCount() : 0;
            result.setCollectCount(realTimeCount);
        }

        return result;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.STATUS_CACHE, key = "'collect:' + #userId + ':' + #postId")
    @Transactional(readOnly = true)
    public CollectStatusVO checkCollectStatus(Long postId, Long userId) {
        CollectStatusVO status = new CollectStatusVO();
        if (userId == null) {
            status.setCollected(false);
            status.setCollectCount(0);
            return status;
        }

        // 使用自定义 SQL 绕过 @TableLogic，兼容 is_deleted = 0 和 NULL
        BlogCollect collect = blogCollectMapper.selectActiveByUserAndPost(userId, postId);
        status.setCollected(collect != null);

        BlogPost post = blogPostMapper.selectById(postId);
        status.setCollectCount(post != null && post.getCollectCount() != null ? post.getCollectCount() : 0);
        return status;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<CollectItemVO> getMyCollections(Long userId, Integer page, Integer pageSize) {
        // 使用自定义 SQL 绕过 @TableLogic，兼容 is_deleted = 0 和 NULL 的历史数据
        int offset = (page - 1) * pageSize;
        List<BlogCollect> records = pageSize > 0 ? blogCollectMapper.selectPageByUserId(userId, offset, pageSize) : List.of();
        Long total = blogCollectMapper.countByUserId(userId);

        Page<BlogCollect> collectResult = new Page<>(page, pageSize, total != null ? total : 0);
        collectResult.setRecords(records);

        // 如果没有收藏记录
        if (collectResult.getRecords().isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        // 获取所有相关文章ID
        List<Long> postIds = collectResult.getRecords().stream()
                .map(BlogCollect::getPostId)
                .collect(Collectors.toList());

        // 批量查询文章
        List<BlogPost> posts = blogPostMapper.selectBatchIds(postIds);
        // 过滤掉未发布和已删除的文章
        posts = posts.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == PostStatus.PUBLISHED.getValue())
                .filter(p -> p.getIsDeleted() == null || p.getIsDeleted() != 1)
                .collect(Collectors.toList());
        Map<Long, BlogPost> postMap = posts.stream()
                .collect(Collectors.toMap(BlogPost::getId, p -> p, (a, b) -> a));

        // 获取所有作者ID
        List<Long> userIds = posts.stream()
                .map(BlogPost::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 获取所有文章标签
        LambdaQueryWrapper<BlogPostTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.in(BlogPostTag::getPostId, postIds);
        List<BlogPostTag> postTags = blogPostTagMapper.selectList(tagWrapper);

        // 获取标签详情
        List<Long> tagIds = postTags.stream()
                .map(BlogPostTag::getTagId)
                .distinct()
                .collect(Collectors.toList());
        List<BlogTag> tags = tagIds.isEmpty() ? List.of() : blogTagMapper.selectBatchIds(tagIds);
        Map<Long, String> tagNameMap = tags.stream()
                .collect(Collectors.toMap(BlogTag::getId, BlogTag::getName, (a, b) -> a));

        // 按文章分组标签
        Map<Long, List<String>> postTagsMap = postTags.stream()
                .collect(Collectors.groupingBy(
                        BlogPostTag::getPostId,
                        Collectors.mapping(pt -> tagNameMap.get(pt.getTagId()), Collectors.toList())
                ));

        // 构建返回结果
        IPage<CollectItemVO> resultPage = new Page<>(
                collectResult.getCurrent(),
                collectResult.getSize(),
                collectResult.getTotal()
        );

        List<CollectItemVO> items = collectResult.getRecords().stream()
                .map(collect -> {
                    CollectItemVO item = new CollectItemVO();
                    item.setCollectId(collect.getId());
                    item.setCollectTime(collect.getCreateTime());

                    BlogPost post = postMap.get(collect.getPostId());
                    if (post != null) {
                        item.setPostId(post.getId());
                        item.setTitle(post.getTitle());
                        item.setSummary(post.getSummary());
                        item.setCategory(post.getCategory());
                        item.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
                        item.setLikeCount(post.getLikeCount());
                        item.setCommentCount(post.getCommentCount());
                        item.setCollectCount(post.getCollectCount());

                        SysUser author = userMap.get(post.getUserId());
                        if (author != null) {
                            item.setAuthorNickname(author.getNickname());
                        }

                        item.setTags(postTagsMap.getOrDefault(post.getId(), List.of()));
                    } else {
                        item.setPostId(collect.getPostId());
                        item.setTitle("文章已删除");
                    }
                    return item;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(items);
        return resultPage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Boolean> checkCollectStatusBatch(List<Long> postIds, Long userId) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        // 使用自定义 SQL 绕过 @TableLogic，兼容 is_deleted = 0 和 NULL 的历史数据
        List<Long> collectedPostIds = blogCollectMapper.selectActivePostIdsByUserAndPosts(userId, postIds);
        java.util.Set<Long> collectedSet = new java.util.HashSet<>(collectedPostIds);

        return postIds.stream()
                .map(collectedSet::contains)
                .collect(Collectors.toList());
    }
}
