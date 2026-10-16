package com.selfblog.service.circle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfblog.common.enums.BooleanStatus;
import com.selfblog.common.enums.IsDeleted;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.*;
import com.selfblog.mapper.*;
import com.selfblog.service.circle.CircleInteractionService;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.utils.TimeUtils;
import com.selfblog.utils.UserConverter;
import com.selfblog.vo.circle.CircleCommentVO;
import com.selfblog.vo.circle.CircleLikeResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 校友圈交互服务实现 — 负责点赞、评论、转发
 */
@Service
public class CircleInteractionServiceImpl implements CircleInteractionService {

    @Autowired
    private CirclePostMapper circlePostMapper;

    @Autowired
    private CircleLikeMapper circleLikeMapper;

    @Autowired
    private CircleCommentMapper circleCommentMapper;

    @Autowired
    private CircleRepostMapper circleRepostMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.selfblog.service.social.FollowService followService;

    // ==================== 点赞相关方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CircleLikeResultVO toggleLike(Long postId, Long userId) {
        CircleLikeResultVO result = new CircleLikeResultVO();

        CirclePost post = circlePostMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(404, "动态不存在");
        }

        if (!canViewPost(post, userId)) {
            throw new BusinessException(403, "无权操作此动态");
        }

        LambdaQueryWrapper<CircleLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleLike::getUserId, userId)
               .eq(CircleLike::getPostId, postId)
               .eq(CircleLike::getIsDeleted, 0);

        CircleLike existingLike = circleLikeMapper.selectOne(wrapper);

        if (existingLike != null) {
            circleLikeMapper.logicalDeleteById(existingLike.getId());
            circlePostMapper.decrementLikeCount(postId);
            result.setAction("unlike");
        } else {
            CircleLike newLike = new CircleLike();
            newLike.setUserId(userId);
            newLike.setPostId(postId);
            try {
                circleLikeMapper.insert(newLike);
                circlePostMapper.incrementLikeCount(postId);
                result.setAction("like");
                Long postAuthorId = post.getUserId();
                if (postAuthorId != null && !postAuthorId.equals(userId)) {
                    notificationService.sendNotification(
                            "LIKE",
                            "有人赞了你的动态",
                            "用户赞了你的动态",
                            userId,
                            postAuthorId,
                            "POST",
                            postId
                    );
                }
            } catch (DataAccessException e) {
                CircleLike concurrentLike = circleLikeMapper.selectOne(wrapper);
                if (concurrentLike != null) {
                    circleLikeMapper.logicalDeleteById(concurrentLike.getId());
                    circlePostMapper.decrementLikeCount(postId);
                    result.setAction("unlike");
                } else {
                    try {
                        circleLikeMapper.insert(newLike);
                        circlePostMapper.incrementLikeCount(postId);
                        result.setAction("like");
                    } catch (DataAccessException e2) {
                        // 二次并发冲突：重新查询确认实际状态并执行对应操作
                        CircleLike finalLike = circleLikeMapper.selectOne(wrapper);
                        if (finalLike != null) {
                            // 活跃点赞存在 → 取消点赞
                            circleLikeMapper.logicalDeleteById(finalLike.getId());
                            circlePostMapper.decrementLikeCount(postId);
                            result.setAction("unlike");
                        } else {
                            // 无活跃点赞 → 重新插入
                            circleLikeMapper.insert(newLike);
                            circlePostMapper.incrementLikeCount(postId);
                            result.setAction("like");
                        }
                    }
                }
            }
        }

        CirclePost updatedPost = circlePostMapper.selectById(postId);
        result.setLikeCount(updatedPost != null ? updatedPost.getLikeCount() : 0);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkLikeStatus(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<CircleLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleLike::getUserId, userId)
               .eq(CircleLike::getPostId, postId)
               .eq(CircleLike::getIsDeleted, 0);
        return circleLikeMapper.selectCount(wrapper) > 0;
    }

    // ==================== 评论相关方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long postId, String content, Long parentId, Long replyToUserId, Long userId) {
        CirclePost post = circlePostMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(404, "动态不存在");
        }

        if (post.getAllowComment() != null && post.getAllowComment() == BooleanStatus.DISABLE.getValue()) {
            throw new BusinessException(403, "该动态禁止评论");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(400, "评论内容不能超过500字符");
        }

        if (parentId != null) {
            CircleComment parentComment = circleCommentMapper.selectById(parentId);
            if (parentComment == null || parentComment.getIsDeleted() == IsDeleted.DELETED.getValue()) {
                throw new BusinessException(404, "父评论不存在");
            }
            if (!Objects.equals(parentComment.getPostId(), postId)) {
                throw new BusinessException(400, "父评论不属于该动态");
            }
        }

        String sanitizedContent = htmlSanitizer.sanitizePlainText(content);

        CircleComment comment = new CircleComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(sanitizedContent);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setLikeCount(0);

        circleCommentMapper.insert(comment);

        circlePostMapper.incrementCommentCount(postId);

        Long postAuthorId = post.getUserId();
        if (postAuthorId != null && !postAuthorId.equals(userId)) {
            notificationService.sendNotification(
                    "COMMENT",
                    "有人评论了你的动态",
                    "用户评论了你的动态",
                    userId,
                    postAuthorId,
                    "POST",
                    postId
            );
        }

        return comment.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleCommentVO> getComments(Long postId, Long currentUserId) {
        CirclePost post = circlePostMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(404, "动态不存在");
        }
        if (!canViewPost(post, currentUserId)) {
            throw new BusinessException(403, "无权查看此动态的评论");
        }

        LambdaQueryWrapper<CircleComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleComment::getPostId, postId)
               .eq(CircleComment::getIsDeleted, 0)
               .orderByAsc(CircleComment::getCreateTime);
        List<CircleComment> comments = circleCommentMapper.selectList(wrapper);

        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = comments.stream()
                .map(CircleComment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> replyToUserIds = comments.stream()
                .map(CircleComment::getReplyToUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        userIds.addAll(replyToUserIds);

        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<CircleCommentVO> commentVOs = comments.stream()
                .map(comment -> convertToCommentVO(comment, userMap))
                .collect(Collectors.toList());

        Map<Long, CircleCommentVO> voMap = commentVOs.stream()
                .collect(Collectors.toMap(CircleCommentVO::getId, c -> c));

        List<CircleCommentVO> rootComments = new ArrayList<>();
        for (CircleCommentVO vo : commentVOs) {
            if (vo.getParentId() == null) {
                rootComments.add(vo);
            } else {
                CircleCommentVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getReplies() == null) {
                        parent.setReplies(new ArrayList<>());
                    }
                    parent.getReplies().add(vo);
                }
            }
        }

        return rootComments;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        CircleComment comment = circleCommentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == IsDeleted.DELETED.getValue()) {
            throw new BusinessException(404, "评论不存在");
        }

        if (!Objects.equals(userId, comment.getUserId()) && !com.selfblog.utils.SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权删除此评论");
        }

        List<Long> commentIdsToDelete = new ArrayList<>();
        commentIdsToDelete.add(commentId);
        collectChildCommentIdsBatch(comment.getPostId(), commentIdsToDelete);

        circleCommentMapper.deleteBatchIds(commentIdsToDelete);

        circlePostMapper.decrementCommentCount(comment.getPostId(), commentIdsToDelete.size());
    }

    // ==================== 转发相关方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long repostPost(Long originalPostId, String content, Long userId) {
        CirclePost originalPost = circlePostMapper.selectById(originalPostId);
        if (originalPost == null || originalPost.getStatus() != 1) {
            throw new BusinessException(404, "原动态不存在");
        }

        if (originalPost.getAllowRepost() != null && originalPost.getAllowRepost() == BooleanStatus.DISABLE.getValue()) {
            throw new BusinessException(403, "该动态禁止转发");
        }

        if (!canViewPost(originalPost, userId)) {
            throw new BusinessException(403, "无权转发此动态");
        }

        String sanitizedContent = content != null ? htmlSanitizer.sanitizePlainText(content) : null;

        CirclePost newPost = new CirclePost();
        newPost.setUserId(userId);
        newPost.setContent(sanitizedContent != null ? sanitizedContent : "");
        newPost.setContentType(3);
        newPost.setRepostId(originalPostId);
        newPost.setRepostUserId(originalPost.getUserId());
        newPost.setLikeCount(0);
        newPost.setCommentCount(0);
        newPost.setRepostCount(0);
        newPost.setViewCount(0L);
        newPost.setIsTop(0);
        newPost.setStatus(1);
        newPost.setVisibility(0);
        newPost.setAllowComment(originalPost.getAllowComment());
        newPost.setAllowRepost(originalPost.getAllowRepost());

        circlePostMapper.insert(newPost);

        CircleRepost repost = new CircleRepost();
        repost.setUserId(userId);
        repost.setOriginalPostId(originalPostId);
        repost.setNewPostId(newPost.getId());
        circleRepostMapper.insert(repost);

        circlePostMapper.incrementRepostCount(originalPostId);

        Long originalAuthorId = originalPost.getUserId();
        if (originalAuthorId != null && !originalAuthorId.equals(userId)) {
            notificationService.sendNotification(
                    "REPOST",
                    "有人转发了你的动态",
                    "用户转发了你的动态",
                    userId,
                    originalAuthorId,
                    "POST",
                    originalPostId
            );
        }

        return newPost.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkRepostStatus(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<CircleRepost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleRepost::getUserId, userId)
               .eq(CircleRepost::getOriginalPostId, postId)
               .eq(CircleRepost::getIsDeleted, 0);
        return circleRepostMapper.selectCount(wrapper) > 0;
    }

    // ==================== 私有辅助方法 ====================

    private boolean canViewPost(CirclePost post, Long currentUserId) {
        if (currentUserId != null && post.getUserId() != null && post.getUserId().equals(currentUserId)) {
            return true;
        }

        if (post.getVisibility() == null || post.getVisibility() == com.selfblog.common.enums.Visibility.PUBLIC.getValue()) {
            return true;
        }

        if (post.getVisibility() == com.selfblog.common.enums.Visibility.FOLLOWERS.getValue()) {
            if (currentUserId == null) {
                return false;
            }
            return followService.isFollowing(post.getUserId(), currentUserId);
        }

        return false;
    }

    private static final int MAX_RECURSION_DEPTH = 100;

    private void collectChildCommentIdsBatch(Long postId, List<Long> result) {
        LambdaQueryWrapper<CircleComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleComment::getPostId, postId)
               .eq(CircleComment::getIsDeleted, 0);
        List<CircleComment> allComments = circleCommentMapper.selectList(wrapper);

        Map<Long, List<CircleComment>> childrenMap = allComments.stream()
                .collect(Collectors.groupingBy(CircleComment::getParentId));

        Deque<Long> stack = new ArrayDeque<>();
        List<Long> initialRoots = new ArrayList<>(result);
        for (Long rootId : initialRoots) {
            stack.push(rootId);
        }

        int depth = 0;
        while (!stack.isEmpty() && depth <= MAX_RECURSION_DEPTH) {
            int levelSize = stack.size();
            for (int i = 0; i < levelSize; i++) {
                Long parentId = stack.pop();
                List<CircleComment> children = childrenMap.get(parentId);
                if (children != null) {
                    for (CircleComment child : children) {
                        if (child.getIsDeleted() == IsDeleted.NORMAL.getValue()) {
                            result.add(child.getId());
                            stack.push(child.getId());
                        }
                    }
                }
            }
            depth++;
        }

        if (depth > MAX_RECURSION_DEPTH) {
            throw new BusinessException(400, "评论层级过深");
        }
    }

    private CircleCommentVO convertToCommentVO(CircleComment comment, Map<Long, SysUser> userMap) {
        CircleCommentVO vo = new CircleCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreateTime(comment.getCreateTime());
        vo.setTimeAgo(TimeUtils.getTimeAgo(comment.getCreateTime()));
        vo.setReplies(new ArrayList<>());

        SysUser user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setUser(UserConverter.toUserVO(user));
        }

        if (comment.getReplyToUserId() != null) {
            SysUser replyUser = userMap.get(comment.getReplyToUserId());
            if (replyUser != null) {
                vo.setReplyToNickname(replyUser.getNickname());
            }
        }

        return vo;
    }
}
