package com.selfblog.service.social.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.*;
import com.selfblog.event.LikeCreatedEvent;
import com.selfblog.mapper.*;
import com.selfblog.service.social.BlogLikeService;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.service.content.TrendingService;
import com.selfblog.vo.post.LikeItemVO;
import com.selfblog.vo.post.LikeResultVO;
import com.selfblog.vo.post.LikeStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 点赞服务实现类
 */
@Slf4j
@Service
public class BlogLikeServiceImpl extends ServiceImpl<BlogLikeMapper, BlogLike> implements BlogLikeService {

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TrendingService trendingService;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogPostTagMapper blogPostTagMapper;

    @Autowired
    private BlogTagMapper blogTagMapper;

    @Autowired
    private BlogLikeMapper blogLikeMapper;

    @Override
    @CacheEvict(value = CaffeineCacheConfig.STATUS_CACHE, key = "'like:' + #userId + ':' + #postId")
    @Transactional(rollbackFor = Exception.class)
    public LikeResultVO toggleLike(Long postId, Long userId) {
        // 检查文章是否存在
        BlogPost post = blogPostService.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        LikeResultVO result = new LikeResultVO();

        // 并发安全改由数据库唯一约束（user_id, post_id）保证，不再使用 synchronized：
        // JVM 监视器在方法返回时释放，而 Spring 事务提交发生在其后，两个并发请求都可能走到
        // “无记录 → 插入”分支，第二个插入撞唯一键。冲突已在下面的 catch 中重新读取记录。
        {
            // 检查是否存在非删除的点赞记录（绕过 @TableLogic，兼容 is_deleted IS NULL 的历史数据）
            BlogLike activeLike = blogLikeMapper.selectActiveByUserAndPost(userId, postId);

            if (activeLike != null) {
                // 取消点赞：逻辑删除记录（解决软删除+唯一约束冲突）
                blogLikeMapper.logicalDeleteById(activeLike.getId());
                blogPostService.decrementLikeCount(postId);
                trendingService.updatePostTrending(postId);
                result.setAction("unlike");
            } else {
                // 绕过 @TableLogic 查找任意状态的记录（兼容 is_deleted = 1 和 NULL）
                BlogLike existingLike = blogLikeMapper.selectRawByUserAndPost(userId, postId);

                if (existingLike != null) {
                    if (existingLike.getIsDeleted() != null && existingLike.getIsDeleted() == 1) {
                        // 已软删除 → 恢复点赞（使用自定义SQL绕过@TableLogic）
                        blogLikeMapper.logicalRestoreById(existingLike.getId());
                        blogPostService.incrementLikeCount(postId);
                        trendingService.updatePostTrending(postId);
                        result.setAction("like");
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                eventPublisher.publishEvent(new LikeCreatedEvent(userId, post.getUserId(), postId, post.getTitle()));
                            }
                        });
                    } else if (existingLike.getIsDeleted() == null) {
                        // 历史遗留 NULL → 修复为 0，不修改点赞数（已计入，使用自定义SQL绕过@TableLogic）
                        blogLikeMapper.logicalRestoreById(existingLike.getId());
                        trendingService.updatePostTrending(postId);
                        result.setAction("like");
                    } else {
                        // 正常活跃状态 → 取消点赞
                        blogLikeMapper.logicalDeleteById(existingLike.getId());
                        blogPostService.decrementLikeCount(postId);
                        trendingService.updatePostTrending(postId);
                        result.setAction("unlike");
                    }
                } else {
                    // 首次点赞
                    BlogLike newLike = new BlogLike();
                    newLike.setUserId(userId);
                    newLike.setPostId(postId);
                    newLike.setIsDeleted(0);
                    boolean shouldPublishEvent = false;
                    try {
                        this.save(newLike);
                        blogPostService.incrementLikeCount(postId);
                        trendingService.updatePostTrending(postId);
                        result.setAction("like");
                        shouldPublishEvent = true;
                    } catch (DataAccessException e) {
                        log.warn("点赞保存遇到并发冲突，重新确认状态: userId={}, postId={}", userId, postId);
                        // 并发冲突：重新查询确认记录是否已被其他线程成功保存
                        BlogLike confirmedLike = blogLikeMapper.selectActiveByUserAndPost(userId, postId);
                        if (confirmedLike != null) {
                            // 记录已存在（并发线程成功保存），不重复增加计数
                            trendingService.updatePostTrending(postId);
                            result.setAction("like");
                            shouldPublishEvent = true;
                        } else {
                            // 记录不存在（保存确实失败），不增加计数
                            result.setAction("like_failed");
                        }
                    }
                    if (shouldPublishEvent) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                eventPublisher.publishEvent(new LikeCreatedEvent(userId, post.getUserId(), postId, post.getTitle()));
                            }
                        });
                    }
                }
            }

            BlogPost updatedPost = blogPostService.getById(postId);
            int realTimeCount = updatedPost != null && updatedPost.getLikeCount() != null ? updatedPost.getLikeCount() : 0;
            result.setLikeCount(realTimeCount);
        }

        return result;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.STATUS_CACHE, key = "'like:' + #userId + ':' + #postId")
    @Transactional(readOnly = true)
    public LikeStatusVO checkLikeStatus(Long postId, Long userId) {
        LikeStatusVO status = new LikeStatusVO();

        // 检查是否已点赞
        boolean liked = hasLiked(postId, userId);
        status.setLiked(liked);

        // 获取文章点赞数
        BlogPost post = blogPostService.getById(postId);
        if (post != null) {
            status.setLikeCount(post.getLikeCount());
        } else {
            status.setLikeCount(0);
        }

        return status;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLiked(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        // 使用自定义 SQL 绕过 @TableLogic，兼容 is_deleted = 0 和 NULL
        BlogLike like = blogLikeMapper.selectActiveByUserAndPost(userId, postId);
        return like != null;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<LikeItemVO> getMyLikes(Long userId, Integer page, Integer pageSize) {
        // 使用自定义 SQL 绕过 @TableLogic，兼容 is_deleted = 0 和 NULL 的历史数据
        // 分页参数可能为 null 或非正数（Integer 拆箱会 NPE、page<1 会算出负 offset），统一钳制
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (safePage - 1) * safePageSize;

        // C21：records 与 total 必须来自同一个谓词。旧实现按「全部点赞」分页/计数，再在 Java 里
        // 丢掉未发布/已删除文章的记录，于是 total 大于真实可见条数、深页返回空数组却仍宣称有数据。
        // 现在把文章可见性（status = 1 且 is_deleted = 0）下推到 SQL 的 JOIN 里，两边同源。
        List<BlogLike> records = blogLikeMapper.selectPublishedPageByUserId(userId, offset, safePageSize);
        Long total = blogLikeMapper.countPublishedByUserId(userId);
        long safeTotal = total != null ? total : 0L;

        if (records.isEmpty()) {
            return new Page<>(safePage, safePageSize, safeTotal);
        }

        // 获取所有文章ID
        List<Long> postIds = records.stream()
                .map(BlogLike::getPostId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询文章（SQL 已过滤可见性，这里仍按同一谓词兜底，避免查询间隙状态变化）
        List<BlogPost> posts = blogPostMapper.selectBatchIds(postIds);
        posts = posts.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == PostStatus.PUBLISHED.getValue())
                .filter(p -> p.getIsDeleted() == null || p.getIsDeleted() != 1)
                .collect(Collectors.toList());
        Map<Long, BlogPost> postMap = posts.stream()
                .collect(Collectors.toMap(BlogPost::getId, p -> p, (a, b) -> a));

        // 获取所有作者ID
        List<Long> authorIds = posts.stream()
                .map(BlogPost::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<SysUser> users = authorIds.isEmpty() ? List.of() : sysUserMapper.selectBatchIds(authorIds);
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 构建返回结果
        List<LikeItemVO> items = records.stream()
                .map(like -> {
                    LikeItemVO item = new LikeItemVO();
                    item.setLikeId(like.getId());
                    item.setPostId(like.getPostId());
                    item.setLikeTime(like.getCreateTime());

                    BlogPost post = postMap.get(like.getPostId());
                    if (post != null) {
                        item.setTitle(post.getTitle());
                        item.setSummary(post.getSummary());
                        item.setCategory(post.getCategory());
                        item.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
                        item.setLikeCount(post.getLikeCount());
                        item.setCommentCount(post.getCommentCount());
                        item.setCollectCount(post.getCollectCount());

                        SysUser author = userMap.get(post.getUserId());
                        if (author != null) {
                            item.setAuthorId(author.getId());
                            item.setAuthorNickname(author.getNickname());
                            item.setAuthorAvatar(author.getAvatar());
                        }
                    } else {
                        // 仅当查询间隙文章状态变化时才会命中，保留占位以免整行消失
                        item.setTitle("文章已删除");
                    }
                    return item;
                })
                .collect(Collectors.toList());

        IPage<LikeItemVO> resultPage = new Page<>(safePage, safePageSize, safeTotal);
        resultPage.setRecords(items);
        return resultPage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Boolean> checkLikeStatusBatch(List<Long> postIds, Long userId) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        // 使用自定义 SQL 绕过 @TableLogic，兼容 is_deleted = 0 和 NULL 的历史数据
        List<Long> likedPostIds = blogLikeMapper.selectActivePostIdsByUserAndPosts(userId, postIds);
        java.util.Set<Long> likedSet = new java.util.HashSet<>(likedPostIds);

        return postIds.stream()
                .map(likedSet::contains)
                .collect(Collectors.toList());
    }
}
