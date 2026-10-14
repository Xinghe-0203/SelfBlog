package com.selfblog.service.circle.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.BooleanStatus;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.common.enums.Visibility;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.CirclePost;
import com.selfblog.entity.SysUser;
import com.selfblog.entity.Topic;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.mapper.TopicMapper;
import com.selfblog.service.circle.CircleInteractionService;
import com.selfblog.service.circle.CircleQueryService;
import com.selfblog.service.circle.CircleService;
import com.selfblog.service.content.TopicService;
import com.selfblog.service.social.FollowService;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.FineGrainedLockManager;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.vo.circle.CircleCommentVO;
import com.selfblog.vo.circle.CircleLikeResultVO;
import com.selfblog.vo.circle.CirclePostVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 校友圈服务实现类 — 负责核心 CRUD 和帖子管理
 * 查询和交互方法已委托给 CircleQueryService 和 CircleInteractionService
 */
@Service
public class CircleServiceImpl extends ServiceImpl<CirclePostMapper, CirclePost> implements CircleService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private FollowService followService;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private CircleQueryService circleQueryService;

    @Autowired
    private CircleInteractionService circleInteractionService;

    private final FineGrainedLockManager lockManager = FineGrainedLockManager.getInstance();

    // ==================== 核心 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(String content, List<String> images, List<String> videos, String location, Long repostId,
                           Long userId,
                           Integer visibility, Integer allowComment, Integer allowRepost,
                           List<Long> explicitTopicIds) {
        if (StrUtil.isBlank(content) && (images == null || images.isEmpty()) && (videos == null || videos.isEmpty()) && repostId == null) {
            throw new BusinessException(400, "动态内容不能为空");
        }
        if (content != null && content.length() > 2000) {
            throw new BusinessException(400, "动态内容不能超过2000字符");
        }
        if (location != null && location.length() > 100) {
            throw new BusinessException(400, "位置信息不能超过100字符");
        }

        Integer contentType = 1;
        if (repostId != null) {
            CirclePost originalPost = this.getById(repostId);
            if (originalPost == null) {
                throw new BusinessException(404, "原动态不存在");
            }
            if (originalPost.getAllowRepost() != null && originalPost.getAllowRepost() == BooleanStatus.DISABLE.getValue()) {
                throw new BusinessException(403, "该动态禁止转发");
            }
            if (!canViewPost(originalPost, userId)) {
                throw new BusinessException(403, "无权转发此动态");
            }
            if (originalPost.getVisibility() != null && originalPost.getVisibility() == Visibility.PRIVATE.getValue()) {
                visibility = Visibility.FOLLOWERS.getValue();
            }
            contentType = 3;
        } else if (videos != null && !videos.isEmpty()) {
            contentType = 4;
        } else if (images != null && !images.isEmpty()) {
            contentType = 2;
        }

        String sanitizedContent = content != null ? htmlSanitizer.sanitizePlainText(content) : null;

        List<SysUser> mentionedUsers = new ArrayList<>();
        if (sanitizedContent != null) {
            mentionedUsers = parseMentions(sanitizedContent, userId);
        }

        List<Long> topicIds = new ArrayList<>();
        if (sanitizedContent != null) {
            topicIds = parseAndGetTopicIds(sanitizedContent);
        }
        if (explicitTopicIds != null && !explicitTopicIds.isEmpty()) {
            for (Long tid : explicitTopicIds) {
                if (!topicIds.contains(tid)) {
                    Topic topic = topicService.getById(tid);
                    if (topic != null && topic.getStatus() == PostStatus.PUBLISHED.getValue()) {
                        topicIds.add(tid);
                    }
                }
            }
        }

        CirclePost post = new CirclePost();
        post.setUserId(userId);
        post.setContent(sanitizedContent);
        post.setContentType(contentType);
        post.setLocation(location != null ? htmlSanitizer.sanitizePlainText(location) : null);
        post.setRepostId(repostId);
        post.setViewCount(0L);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setRepostCount(0);
        post.setIsTop(0);
        post.setStatus(1);
        post.setVisibility(visibility != null ? visibility : Visibility.PUBLIC.getValue());
        post.setAllowComment(allowComment != null ? allowComment : BooleanStatus.ENABLE.getValue());
        post.setAllowRepost(allowRepost != null ? allowRepost : BooleanStatus.ENABLE.getValue());

        if (images != null && !images.isEmpty()) {
            post.setImageUrls(cn.hutool.json.JSONUtil.toJsonStr(images));
        }

        if (videos != null && !videos.isEmpty()) {
            post.setVideoUrls(cn.hutool.json.JSONUtil.toJsonStr(videos));
        }

        if (!mentionedUsers.isEmpty()) {
            List<Long> mentionedUserIds = mentionedUsers.stream().map(SysUser::getId).collect(Collectors.toList());
            post.setMentions(cn.hutool.json.JSONUtil.toJsonStr(mentionedUserIds));
        }

        if (!topicIds.isEmpty()) {
            post.setTopicIds(topicIds);
        }

        this.save(post);

        if (!topicIds.isEmpty()) {
            Long postIdVal = post.getId();
            List<Long> topicsToUpdate = new ArrayList<>(topicIds);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (Long tid : topicsToUpdate) {
                            topicMapper.incrementPostCount(tid);
                        }
                    }
                });
            } else {
                for (Long tid : topicIds) {
                    topicMapper.incrementPostCount(tid);
                }
            }
        }

        if (repostId != null) {
            baseMapper.incrementRepostCount(repostId);
        }

        if (!mentionedUsers.isEmpty()) {
            Long postAuthorId = userId;
            Long postIdVal = post.getId();
            List<SysUser> usersToNotify = new ArrayList<>(mentionedUsers);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (SysUser mentionedUser : usersToNotify) {
                            notificationService.sendNotification(
                                    "MENTION",
                                    "有人在动态中@了你",
                                    "用户 @" + mentionedUser.getUsername() + " 在动态中提及了你",
                                    postAuthorId,
                                    mentionedUser.getId(),
                                    "POST",
                                    postIdVal
                            );
                        }
                    }
                });
            } else {
                for (SysUser mentionedUser : usersToNotify) {
                    notificationService.sendNotification(
                            "MENTION",
                            "有人在动态中@了你",
                            "用户 @" + mentionedUser.getUsername() + " 在动态中提及了你",
                            postAuthorId,
                            mentionedUser.getId(),
                            "POST",
                            postIdVal
                    );
                }
            }
        }

        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(Long postId, String content, List<String> images, List<String> videos,
                           String location, List<Long> topicIds,
                           Integer visibility, Integer allowComment, Integer allowRepost,
                           Long userId) {
        CirclePost post = this.getById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(404, "动态不存在");
        }
        if (!Objects.equals(userId, post.getUserId())) {
            throw new BusinessException(403, "无权编辑此动态");
        }

        if (content != null) {
            String sanitizedContent = htmlSanitizer.sanitizePlainText(content);
            if (sanitizedContent.length() > 2000) {
                throw new BusinessException(400, "动态内容不能超过2000字符");
            }
            post.setContent(sanitizedContent);
        }

        if (images != null) {
            post.setImageUrls(images.isEmpty() ? null : cn.hutool.json.JSONUtil.toJsonStr(images));
        }
        if (videos != null) {
            post.setVideoUrls(videos.isEmpty() ? null : cn.hutool.json.JSONUtil.toJsonStr(videos));
        }
        if (location != null) {
            post.setLocation(htmlSanitizer.sanitizePlainText(location));
        }

        if (topicIds != null) {
            List<Long> oldTopicIds = post.getTopicIds();
            post.setTopicIds(topicIds.isEmpty() ? null : topicIds);
            List<Long> oldValid = oldTopicIds != null ? oldTopicIds : Collections.emptyList();
            for (Long tid : oldValid) {
                if (!topicIds.contains(tid)) {
                    topicMapper.decrementPostCount(tid);
                }
            }
            for (Long tid : topicIds) {
                if (!oldValid.contains(tid)) {
                    topicMapper.incrementPostCount(tid);
                }
            }
        }

        if (visibility != null) {
            post.setVisibility(visibility);
        }
        if (allowComment != null) {
            post.setAllowComment(allowComment);
        }
        if (allowRepost != null) {
            post.setAllowRepost(allowRepost);
        }

        this.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId) {
        CirclePost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        if (!Objects.equals(userId, post.getUserId()) && !com.selfblog.utils.SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权删除此动态");
        }

        if (post.getRepostId() != null) {
            baseMapper.decrementRepostCount(post.getRepostId());
        }

        if (post.getTopicIds() != null && !post.getTopicIds().isEmpty()) {
            for (Long tid : post.getTopicIds()) {
                topicMapper.decrementPostCount(tid);
            }
        }

        this.removeById(postId);
    }

    // ==================== 私有辅助方法 ====================

    private List<SysUser> parseMentions(String content, Long authorId) {
        List<SysUser> mentionedUsers = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([\\w\\u4e00-\\u9fa5]{1,20}?)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        Set<String> mentionedUsernames = new HashSet<>();

        while (matcher.find()) {
            mentionedUsernames.add(matcher.group(1));
        }

        if (!mentionedUsernames.isEmpty()) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.in(SysUser::getUsername, mentionedUsernames)
                              .or().in(SysUser::getNickname, mentionedUsernames));
            List<SysUser> foundUsers = sysUserMapper.selectList(wrapper);
            for (SysUser user : foundUsers) {
                if (!user.getId().equals(authorId)) {
                    mentionedUsers.add(user);
                }
            }
        }

        return mentionedUsers;
    }

    private List<Long> parseAndGetTopicIds(String content) {
        List<Long> topicIds = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#([\\w\\u4e00-\\u9fa5]{1,30}?)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        Set<String> topicNames = new HashSet<>();

        while (matcher.find()) {
            String topicName = matcher.group(1);
            if (!topicName.isEmpty()) {
                topicNames.add(topicName);
            }
        }

        for (String topicName : topicNames) {
            Long topicId = topicService.getOrCreateTopic(topicName);
            if (topicId != null) {
                topicIds.add(topicId);
            }
        }

        return topicIds;
    }

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

    // ==================== 委托方法 — 查询 ====================

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> getRecommendFeed(int page, int pageSize, Long currentUserId) {
        return circleQueryService.getRecommendFeed(page, pageSize, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> getFollowingFeed(int page, int pageSize, Long userId) {
        return circleQueryService.getFollowingFeed(page, pageSize, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CirclePostVO getPostDetail(Long postId, Long currentUserId) {
        return circleQueryService.getPostDetail(postId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> searchPosts(String keyword, int page, int pageSize, Long currentUserId) {
        return circleQueryService.searchPosts(keyword, page, pageSize, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CirclePostVO> getPostsByTopic(Long topicId, int page, int pageSize, Long currentUserId) {
        return circleQueryService.getPostsByTopic(topicId, page, pageSize, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<CirclePostVO> getUserPosts(Long targetUserId, int page, int pageSize, Long currentUserId) {
        return circleQueryService.getUserPosts(targetUserId, page, pageSize, currentUserId);
    }

    // ==================== 委托方法 — 交互 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CircleLikeResultVO toggleLike(Long postId, Long userId) {
        return circleInteractionService.toggleLike(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkLikeStatus(Long postId, Long userId) {
        return circleInteractionService.checkLikeStatus(postId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long postId, String content, Long parentId, Long replyToUserId, Long userId) {
        return circleInteractionService.createComment(postId, content, parentId, replyToUserId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleCommentVO> getComments(Long postId, Long currentUserId) {
        return circleInteractionService.getComments(postId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        circleInteractionService.deleteComment(commentId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long repostPost(Long originalPostId, String content, Long userId) {
        return circleInteractionService.repostPost(originalPostId, content, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkRepostStatus(Long postId, Long userId) {
        return circleInteractionService.checkRepostStatus(postId, userId);
    }
}
