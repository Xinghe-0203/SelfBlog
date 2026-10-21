package com.selfblog.service.circle.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.BooleanStatus;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.common.enums.Visibility;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.*;
import com.selfblog.mapper.*;
import com.selfblog.service.circle.CircleQueryService;
import com.selfblog.service.social.FollowService;
import com.selfblog.service.content.TopicService;
import com.selfblog.utils.SqlLikeUtils;
import com.selfblog.utils.TimeUtils;
import com.selfblog.vo.circle.CirclePostVO;
import com.selfblog.vo.user.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 校友圈查询服务实现 — 负责圈子查询、搜索
 */
@Service
public class CircleQueryServiceImpl extends ServiceImpl<CirclePostMapper, CirclePost> implements CircleQueryService {

    @Value("${DB_TYPE:mysql}")
    private String dbType;

    private boolean isSqlite() { return "sqlite".equalsIgnoreCase(dbType); }

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private FollowService followService;

    @Autowired
    private CircleLikeMapper circleLikeMapper;

    @Autowired
    private CircleRepostMapper circleRepostMapper;

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicMapper topicMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> getRecommendFeed(int page, int pageSize, Long currentUserId) {
        LambdaQueryWrapper<CirclePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePost::getStatus, 1)
                .and(w -> w.eq(CirclePost::getVisibility, 0)
                        .or(currentUserId != null, w2 -> w2
                                .eq(CirclePost::getUserId, currentUserId)
                                .ne(CirclePost::getVisibility, 0)))
                .orderByDesc(CirclePost::getIsTop)
                .orderByDesc(CirclePost::getCreateTime);

        List<CirclePost> posts = this.page(
                new Page<>(page, pageSize),
                wrapper
        ).getRecords();

        return convertToVOList(posts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> getFollowingFeed(int page, int pageSize, Long userId) {
        List<UserVO> followingList = followService.getFollowing(userId);
        if (followingList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> followingUserIds = followingList.stream()
                .map(UserVO::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<CirclePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePost::getStatus, 1)
                .in(CirclePost::getUserId, followingUserIds)
                .and(w -> w
                        .eq(CirclePost::getVisibility, 0)
                        .or()
                        .eq(CirclePost::getVisibility, 1)
                        .or()
                        .eq(CirclePost::getVisibility, 2).eq(CirclePost::getUserId, userId))
                .orderByDesc(CirclePost::getIsTop)
                .orderByDesc(CirclePost::getCreateTime);

        List<CirclePost> posts = this.page(
                new Page<>(page, pageSize),
                wrapper
        ).getRecords();

        return convertToVOList(posts, userId);
    }

    /**
     * 读取动态详情。**纯只读**：不写库、不开写事务。
     *
     * <p>浏览量自增由 {@code CircleController} 在调用本方法之前通过
     * {@code CircleInteractionService.incrementViewCount} 完成（与文章详情
     * {@code BlogPostController.getPostDetail} 同形）。历史实现曾在只读事务里用
     * REQUIRES_NEW 另开写事务自增，调用方一旦持有该行的未提交写锁（真实 MySQL 上
     * 可复现，测试事务里必现），内层事务就会阻塞到 innodb_lock_wait_timeout 后抛
     * CannotAcquireLockException；同时每次详情请求会占用两条池连接。两点都已消除。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public CirclePostVO getPostDetail(Long postId, Long currentUserId) {
        CirclePost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }

        if (!canViewPost(post, currentUserId)) {
            throw new BusinessException(403, "无权查看此动态");
        }

        List<CirclePost> posts = Collections.singletonList(post);
        List<CirclePostVO> voList = convertToVOList(posts, currentUserId);
        return voList.isEmpty() ? null : voList.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> searchPosts(String keyword, int page, int pageSize, Long currentUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (keyword.length() > 200) {
            throw new BusinessException(400, "搜索关键词不能超过200字符");
        }

        List<Long> followingIds = currentUserId != null
                ? followService.getFollowing(currentUserId).stream().map(UserVO::getId).collect(Collectors.toList())
                : Collections.emptyList();

        LambdaQueryWrapper<CirclePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePost::getStatus, 1)
                .and(w -> w.eq(CirclePost::getVisibility, 0)
                        .or(currentUserId != null, w2 -> w2
                                .eq(CirclePost::getUserId, currentUserId))
                        .or(currentUserId != null && !followingIds.isEmpty(), w3 -> w3
                                .eq(CirclePost::getVisibility, 1)
                                .in(CirclePost::getUserId, followingIds)))
                .apply("content LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, SqlLikeUtils.containsPattern(keyword.trim()))
                .orderByDesc(CirclePost::getCreateTime);

        List<CirclePost> posts = this.page(
                new Page<>(page, pageSize),
                wrapper
        ).getRecords();

        return convertToVOList(posts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> getPostsByTopic(Long topicId, int page, int pageSize, Long currentUserId) {
        Topic topic = topicService.getById(topicId);
        if (topic == null || topic.getStatus() != 1) {
            throw new BusinessException(404, "话题不存在");
        }

        LambdaQueryWrapper<CirclePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePost::getStatus, 1)
                .and(w -> w.eq(CirclePost::getVisibility, 0)
                        .or(currentUserId != null, w2 -> w2
                                .eq(CirclePost::getUserId, currentUserId)))
                .apply(isSqlite()
                ? "EXISTS (SELECT 1 FROM json_each(topic_ids) WHERE CAST(value AS INTEGER) = {0})"
                : "JSON_CONTAINS(JSON_UNQUOTE(topic_ids), CAST({0} AS JSON))", topicId)
                .orderByDesc(CirclePost::getIsTop)
                .orderByDesc(CirclePost::getCreateTime);

        List<CirclePost> posts = this.page(
                new Page<>(page, pageSize),
                wrapper
        ).getRecords();

        return convertToVOList(posts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<CirclePostVO> getUserPosts(Long targetUserId, int page, int pageSize, Long currentUserId) {
        Page<CirclePost> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<CirclePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePost::getUserId, targetUserId)
                .eq(CirclePost::getStatus, 1)
                .and(w -> w.eq(CirclePost::getVisibility, 0)
                        .or(currentUserId != null && currentUserId.equals(targetUserId), w2 -> w2
                                .eq(CirclePost::getUserId, targetUserId)))
                .orderByDesc(CirclePost::getCreateTime);

        IPage<CirclePost> postPage = this.page(pageObj, wrapper);

        List<CirclePostVO> voList = convertToVOList(postPage.getRecords(), currentUserId);
        IPage<CirclePostVO> result = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private boolean canViewPost(CirclePost post, Long currentUserId) {
        if (currentUserId != null && post.getUserId() != null && post.getUserId().equals(currentUserId)) {
            return true;
        }

        if (post.getVisibility() == null || post.getVisibility() == Visibility.PUBLIC.getValue()) {
            return true;
        }

        if (post.getVisibility() == Visibility.FOLLOWERS.getValue()) {
            if (currentUserId == null) {
                return false;
            }
            return followService.isFollowing(post.getUserId(), currentUserId);
        }

        return false;
    }

    private List<CirclePostVO> convertToVOList(List<CirclePost> posts, Long currentUserId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> likedPostIds = new HashSet<>();
        Set<Long> repostedPostIds = new HashSet<>();
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(CirclePost::getId).collect(Collectors.toList());

            LambdaQueryWrapper<CircleLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(CircleLike::getUserId, currentUserId)
                       .eq(CircleLike::getIsDeleted, 0)
                       .in(CircleLike::getPostId, postIds);
            List<CircleLike> likes = circleLikeMapper.selectList(likeWrapper);
            likedPostIds = new HashSet<>(likes.stream().map(CircleLike::getPostId).toList());

            LambdaQueryWrapper<CircleRepost> repostWrapper = new LambdaQueryWrapper<>();
            repostWrapper.eq(CircleRepost::getUserId, currentUserId)
                         .eq(CircleRepost::getIsDeleted, 0)
                         .in(CircleRepost::getOriginalPostId, postIds);
            List<CircleRepost> reposts = circleRepostMapper.selectList(repostWrapper);
            repostedPostIds = new HashSet<>(reposts.stream().map(CircleRepost::getOriginalPostId).toList());
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        final Set<Long> finalRepostedPostIds = repostedPostIds;

        Set<Long> userIds = new HashSet<>();
        posts.forEach(post -> {
            if (post.getUserId() != null) userIds.add(post.getUserId());
            if (post.getRepostUserId() != null) userIds.add(post.getRepostUserId());
        });

        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            sysUserMapper.selectBatchIds(userIds)
                    .forEach(user -> userMap.put(user.getId(), user));
        }

        List<Long> repostIds = posts.stream()
                .map(CirclePost::getRepostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, CirclePost> repostPostMap = new HashMap<>();
        if (!repostIds.isEmpty()) {
            this.listByIds(repostIds).forEach(post -> repostPostMap.put(post.getId(), post));
        }

        final Map<Long, CirclePost> finalRepostPostMap = repostPostMap;

        Map<Long, String> topicNameMap = new HashMap<>();
        Set<Long> allTopicIds = posts.stream()
                .map(CirclePost::getTopicIds)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        if (!allTopicIds.isEmpty()) {
            topicService.listByIds(allTopicIds).stream()
                    .filter(t -> t.getStatus() == PostStatus.PUBLISHED.getValue())
                    .forEach(t -> topicNameMap.put(t.getId(), t.getName()));
        }

        return posts.stream().map(post -> {
            CirclePostVO vo = new CirclePostVO();
            vo.setId(post.getId());
            vo.setContent(post.getContent());
            vo.setContentType(post.getContentType());
            vo.setLocation(post.getLocation());
            vo.setLikeCount(post.getLikeCount());
            vo.setCommentCount(post.getCommentCount());
            vo.setRepostCount(post.getRepostCount());
            vo.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
            vo.setIsTop(post.getIsTop() == BooleanStatus.ENABLE.getValue());
            vo.setIsLiked(finalLikedPostIds.contains(post.getId()));
            vo.setIsReposted(finalRepostedPostIds.contains(post.getId()));
            vo.setVisibility(post.getVisibility());
            vo.setAllowComment(post.getAllowComment());
            vo.setAllowRepost(post.getAllowRepost());
            vo.setCreateTime(post.getCreateTime());
            vo.setTimeAgo(TimeUtils.getTimeAgo(post.getCreateTime()));

            if (StrUtil.isNotBlank(post.getImageUrls())) {
                vo.setImages(cn.hutool.json.JSONUtil.toList(post.getImageUrls(), String.class));
            } else {
                vo.setImages(new ArrayList<>());
            }

            if (StrUtil.isNotBlank(post.getVideoUrls())) {
                vo.setVideos(cn.hutool.json.JSONUtil.toList(post.getVideoUrls(), String.class));
            } else {
                vo.setVideos(new ArrayList<>());
            }

            SysUser user = userMap.get(post.getUserId());
            if (user != null) {
                vo.setUserId(user.getId());
                vo.setUserUsername(user.getUsername());
                vo.setUserNickname(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }

            if (post.getRepostId() != null) {
                CirclePost repostPost = finalRepostPostMap.get(post.getRepostId());
                if (repostPost != null) {
                    if (canViewPost(repostPost, currentUserId)) {
                        CirclePostVO repostVO = new CirclePostVO();
                        repostVO.setId(repostPost.getId());
                        repostVO.setContent(repostPost.getContent());
                        repostVO.setContentType(repostPost.getContentType());
                        repostVO.setVisibility(repostPost.getVisibility());
                        repostVO.setAllowComment(repostPost.getAllowComment());
                        repostVO.setAllowRepost(repostPost.getAllowRepost());

                        SysUser repostUser = userMap.get(repostPost.getUserId());
                        if (repostUser != null) {
                            repostVO.setUserId(repostUser.getId());
                            repostVO.setUserUsername(repostUser.getUsername());
                            repostVO.setUserNickname(repostUser.getNickname());
                            repostVO.setUserAvatar(repostUser.getAvatar());
                        }

                        if (StrUtil.isNotBlank(repostPost.getImageUrls())) {
                            repostVO.setImages(cn.hutool.json.JSONUtil.toList(repostPost.getImageUrls(), String.class));
                        }

                        repostVO.setCreateTime(repostPost.getCreateTime());
                        repostVO.setTimeAgo(TimeUtils.getTimeAgo(repostPost.getCreateTime()));

                        vo.setRepostPost(repostVO);
                    } else {
                        vo.setRepostPost(null);
                        vo.setOriginalPostHidden(true);
                    }
                }
            }

            List<Long> rawTopicIds = post.getTopicIds();
            if (rawTopicIds != null && !rawTopicIds.isEmpty()) {
                List<Long> validIds = rawTopicIds.stream()
                        .filter(topicNameMap::containsKey)
                        .collect(Collectors.toList());
                vo.setTopicIds(validIds);
                vo.setTopicNames(validIds.stream()
                        .map(topicNameMap::get)
                        .collect(Collectors.toList()));
            } else {
                vo.setTopicIds(new ArrayList<>());
                vo.setTopicNames(new ArrayList<>());
            }

            return vo;
        }).collect(Collectors.toList());
    }
}
