package com.selfblog.service.post.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.IsDeleted;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.dto.post.AdminPostQueryRequest;
import com.selfblog.dto.post.PostAdvancedSearchRequest;
import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.dto.post.SaveDraftRequest;
import com.selfblog.entity.*;
import com.selfblog.mapper.*;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.service.content.TopicService;
import com.selfblog.service.content.TrendingService;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.service.post.PostInteractionService;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章服务实现类 — 负责核心 CRUD（创建、更新、删除、审核）
 * 查询和交互方法已委托给 PostQueryService 和 PostInteractionService
 */
@Slf4j
@Service
public class BlogPostServiceImpl extends ServiceImpl<BlogPostMapper, BlogPost> implements BlogPostService {

    @Autowired
    private BlogPostTagMapper blogPostTagMapper;

    @Autowired
    private BlogTagMapper blogTagMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogCommentMapper blogCommentMapper;

    @Autowired
    private BlogLikeMapper blogLikeMapper;

    @Autowired
    private BlogCollectMapper blogCollectMapper;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Autowired
    private BlogDraftMapper blogDraftMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TrendingService trendingService;

    @Autowired
    private BlogTagService blogTagService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private PostQueryService postQueryService;

    @Autowired
    private PostInteractionService postInteractionService;

    private void validatePostRequest(PostCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "文章标题不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException(400, "文章内容不能为空");
        }
        if (request.getTitle().length() > 200) {
            throw new BusinessException(400, "文章标题不能超过200字符");
        }
        if (request.getContent() != null && request.getContent().length() > 300000) {
            throw new BusinessException(400, "文章内容不能超过30万字符");
        }
    }

    // ==================== 核心 CRUD ====================

    @Override
    @CacheEvict(value = {CaffeineCacheConfig.TRENDING_CACHE, CaffeineCacheConfig.STATS_CACHE}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateRequest request, Long userId, boolean isAdmin) {
        validatePostRequest(request);

        String sanitizedTitle = htmlSanitizer.sanitizeRichText(request.getTitle());
        String sanitizedSummary = htmlSanitizer.sanitizeRichText(request.getSummary());
        String sanitizedContent = htmlSanitizer.sanitizeMarkdown(request.getContent());

        List<Long> tagIds = resolveTagIds(request.getTagIds(), request.getTagNames());
        if (tagIds != null && !tagIds.isEmpty()) {
            validateTagIds(tagIds);
        }

        BlogPost post = new BlogPost();
        post.setUserId(userId);
        post.setTitle(sanitizedTitle);
        post.setSummary(sanitizedSummary);
        post.setContent(sanitizedContent);
        post.setCategory(request.getCategory() != null ? htmlSanitizer.sanitizePlainText(request.getCategory()) : "默认分类");
        post.setViewCount(0L);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setStatus(PostStatus.PUBLISHED.getValue());
        post.setCoverUrl(request.getCoverImage());
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            post.setTopicIds(cn.hutool.json.JSONUtil.toJsonStr(request.getTopicIds()));
        }

        this.save(post);

        if (tagIds != null && !tagIds.isEmpty()) {
            savePostTags(post.getId(), tagIds);
        }

        // 更新关联话题的计数
        if (post.getTopicIds() != null && !post.getTopicIds().isEmpty()) {
            for (Long topicId : parseTopicIds(post.getTopicIds())) {
                topicMapper.incrementPostCount(topicId);
            }
        }

        log.info("文章创建成功: postId={}, userId={}, title={}", post.getId(), userId, post.getTitle());
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateRequest request, Long userId) {
        return createPost(request, userId, false);
    }

    @Override
    @CacheEvict(value = {CaffeineCacheConfig.TRENDING_CACHE, CaffeineCacheConfig.STATS_CACHE}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostCreateRequest request, Long userId, boolean isAdmin) {
        if (request.getId() == null) {
            throw new BusinessException(400, "文章ID不能为空");
        }
        validatePostRequest(request);

        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        String sanitizedTitle = htmlSanitizer.sanitizeRichText(request.getTitle());
        String sanitizedSummary = htmlSanitizer.sanitizeRichText(request.getSummary());
        String sanitizedContent = htmlSanitizer.sanitizeMarkdown(request.getContent());

        List<Long> tagIds = resolveTagIds(request.getTagIds(), request.getTagNames());
        if (tagIds != null && !tagIds.isEmpty()) {
            validateTagIds(tagIds);
        }

        BlogPost post = this.getById(request.getId());
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (!Objects.equals(userId, post.getUserId()) && !isAdmin) {
            throw new BusinessException(403, "无权修改此文章");
        }

        post.setTitle(sanitizedTitle);
        post.setSummary(sanitizedSummary);
        post.setContent(sanitizedContent);
        if (request.getCategory() != null) {
            post.setCategory(htmlSanitizer.sanitizePlainText(request.getCategory()));
        }
        post.setCoverUrl(request.getCoverImage());

        // 计算话题变更并更新计数
        Set<Long> oldTopicIds = parseTopicIds(post.getTopicIds());
        Set<Long> newTopicIds;
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            post.setTopicIds(cn.hutool.json.JSONUtil.toJsonStr(request.getTopicIds()));
            newTopicIds = new java.util.HashSet<>(request.getTopicIds());
        } else {
            post.setTopicIds(null);
            newTopicIds = java.util.Collections.emptySet();
        }
        // 减少被移除的话题计数
        for (Long tid : oldTopicIds) {
            if (!newTopicIds.contains(tid)) {
                topicMapper.decrementPostCount(tid);
            }
        }
        // 增加新增的话题计数
        for (Long tid : newTopicIds) {
            if (!oldTopicIds.contains(tid)) {
                topicMapper.incrementPostCount(tid);
            }
        }

        if (post.getStatus() == null) {
            post.setStatus(PostStatus.PUBLISHED.getValue());
        }

        this.updateById(post);

        LambdaQueryWrapper<BlogPostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPostTag::getPostId, post.getId());
        blogPostTagMapper.delete(wrapper);

        if (tagIds != null && !tagIds.isEmpty()) {
            savePostTags(post.getId(), tagIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostCreateRequest request, Long userId) {
        updatePost(request, userId, false);
    }

    @Override
    @CacheEvict(value = {CaffeineCacheConfig.TRENDING_CACHE, CaffeineCacheConfig.STATS_CACHE, CaffeineCacheConfig.CATEGORY_CACHE}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId, boolean isAdmin) {
        BlogPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getIsDeleted() != null && post.getIsDeleted() == IsDeleted.DELETED.getValue()) {
            throw new BusinessException(404, "文章不存在");
        }
        if (!Objects.equals(userId, post.getUserId()) && !isAdmin) {
            throw new BusinessException(403, "无权删除此文章");
        }

        this.removeById(postId);

        // 清理点赞和收藏记录，并重置计数
        blogLikeMapper.update(null, new LambdaUpdateWrapper<BlogLike>()
                .eq(BlogLike::getPostId, postId)
                .set(BlogLike::getIsDeleted, 1));
        blogCollectMapper.update(null, new LambdaUpdateWrapper<BlogCollect>()
                .eq(BlogCollect::getPostId, postId)
                .set(BlogCollect::getIsDeleted, 1));
        this.update().set("like_count", 0).set("collect_count", 0).eq("id", postId).update();

        LambdaQueryWrapper<BlogPostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPostTag::getPostId, postId);
        blogPostTagMapper.delete(wrapper);

        // 更新关联话题的计数
        if (post.getTopicIds() != null && !post.getTopicIds().isEmpty()) {
            for (Long topicId : parseTopicIds(post.getTopicIds())) {
                topicMapper.decrementPostCount(topicId);
            }
        }

        log.info("文章删除成功: postId={}, userId={}, title={}", postId, userId, post.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId) {
        deletePost(postId, userId, false);
    }

    // ==================== 草稿管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(Long userId, SaveDraftRequest request) {
        String sanitizedTitle = request.getTitle() != null ? htmlSanitizer.sanitizeRichText(request.getTitle()) : null;
        String sanitizedSummary = request.getSummary() != null ? htmlSanitizer.sanitizeRichText(request.getSummary()) : null;
        String sanitizedContent = request.getContent() != null ? htmlSanitizer.sanitizeMarkdown(request.getContent()) : null;
        String sanitizedCategory = request.getCategory() != null ? htmlSanitizer.sanitizePlainText(request.getCategory()) : null;
        String sanitizedCoverImage = request.getCoverImage() != null ? htmlSanitizer.sanitizePlainText(request.getCoverImage()) : null;

        List<Long> mergedTagIds = resolveTagIds(request.getTagIds(), request.getTagNames());
        if (mergedTagIds != null && !mergedTagIds.isEmpty()) {
            validateTagIds(mergedTagIds);
        }

        String tagIdsStr = null;
        if (mergedTagIds != null && !mergedTagIds.isEmpty()) {
            tagIdsStr = mergedTagIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        if (request.getDraftId() != null) {
            BlogDraft targetDraft = blogDraftMapper.selectById(request.getDraftId());
            if (targetDraft == null) {
                throw new BusinessException(404, "草稿不存在");
            }
            if (!Objects.equals(userId, targetDraft.getUserId())) {
                throw new BusinessException(403, "无权修改此草稿");
            }
            targetDraft.setTitle(sanitizedTitle);
            targetDraft.setContent(sanitizedContent);
            targetDraft.setSummary(sanitizedSummary);
            targetDraft.setCategory(sanitizedCategory);
            targetDraft.setTagIds(tagIdsStr);
            targetDraft.setCoverImage(sanitizedCoverImage);
            targetDraft.setPostId(request.getPostId());
            targetDraft.setTopicId(request.getTopicId());
            blogDraftMapper.updateById(targetDraft);
            return targetDraft.getId();
        }

        if (request.getPostId() != null) {
            LambdaQueryWrapper<BlogDraft> postWrapper = new LambdaQueryWrapper<>();
            postWrapper.eq(BlogDraft::getUserId, userId)
                    .eq(BlogDraft::getPostId, request.getPostId());
            BlogDraft existingByPost = blogDraftMapper.selectOne(postWrapper);
            if (existingByPost != null) {
                existingByPost.setTitle(sanitizedTitle);
                existingByPost.setContent(sanitizedContent);
                existingByPost.setSummary(sanitizedSummary);
                existingByPost.setCategory(sanitizedCategory);
                existingByPost.setTagIds(tagIdsStr);
                existingByPost.setCoverImage(sanitizedCoverImage);
                existingByPost.setPostId(request.getPostId());
                existingByPost.setTopicId(request.getTopicId());
                blogDraftMapper.updateById(existingByPost);
                return existingByPost.getId();
            }
        }

        BlogDraft draft = new BlogDraft();
        draft.setUserId(userId);
        draft.setTitle(sanitizedTitle);
        draft.setContent(sanitizedContent);
        draft.setSummary(sanitizedSummary);
        draft.setCategory(sanitizedCategory);
        draft.setTagIds(tagIdsStr);
        draft.setCoverImage(sanitizedCoverImage);
        draft.setPostId(request.getPostId());
        draft.setTopicId(request.getTopicId());
        blogDraftMapper.insert(draft);
        return draft.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long draftId, Long userId, boolean isAdmin) {
        BlogDraft draft = blogDraftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(404, "草稿不存在");
        }
        if (!Objects.equals(userId, draft.getUserId()) && !isAdmin) {
            throw new BusinessException(403, "无权删除此草稿");
        }

        blogDraftMapper.deleteById(draftId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long draftId, Long userId) {
        deleteDraft(draftId, userId, false);
    }

    // ==================== 管理员操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeletePost(Long postId, Long adminId) {
        BlogPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getIsDeleted() != null && post.getIsDeleted() == IsDeleted.DELETED.getValue()) {
            throw new BusinessException(404, "文章不存在");
        }

        this.update().set("is_deleted", 1).eq("id", postId).update();

        LambdaQueryWrapper<BlogPostTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(BlogPostTag::getPostId, postId);
        blogPostTagMapper.delete(tagWrapper);

        blogCommentMapper.update(null, new LambdaUpdateWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .set(BlogComment::getIsDeleted, 1));

        blogLikeMapper.update(null, new LambdaUpdateWrapper<BlogLike>()
                .eq(BlogLike::getPostId, postId)
                .set(BlogLike::getIsDeleted, 1));

        blogCollectMapper.update(null, new LambdaUpdateWrapper<BlogCollect>()
                .eq(BlogCollect::getPostId, postId)
                .set(BlogCollect::getIsDeleted, 1));

        // 重置文章的点赞和收藏计数
        this.update().set("like_count", 0).set("collect_count", 0).eq("id", postId).update();

        // 更新关联话题的计数
        if (post.getTopicIds() != null && !post.getTopicIds().isEmpty()) {
            for (Long topicId : parseTopicIds(post.getTopicIds())) {
                topicMapper.decrementPostCount(topicId);
            }
        }

        log.info("管理员删除文章: postId={}, adminId={}, title={}", postId, adminId, post.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePost(Long postId, Long reviewerId) {
        BlogPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getIsDeleted() != null && post.getIsDeleted() == IsDeleted.DELETED.getValue()) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getStatus() != 0) {
            throw new BusinessException(400, "文章不在待审核状态，无法审核");
        }

        post.setStatus(PostStatus.PUBLISHED.getValue());
        post.setReviewerId(reviewerId);
        post.setReviewTime(LocalDateTime.now());
        post.setRejectReason(null);
        this.updateById(post);

        trendingService.updatePostTrending(postId);

        String approveTitle = "文章审核通过";
        String approveContent = "您的文章《" + post.getTitle() + "》已通过审核，现已正式发布";
        notificationService.sendNotification("AUDIT", approveTitle, approveContent, reviewerId, post.getUserId(), "POST", post.getId());

        log.info("文章审核通过: postId={}, reviewerId={}, title={}", postId, reviewerId, post.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectPost(Long postId, Long reviewerId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(400, "驳回原因不能为空");
        }
        if (reason.length() > 500) {
            throw new BusinessException(400, "驳回原因不能超过500字符");
        }

        BlogPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getIsDeleted() != null && post.getIsDeleted() == IsDeleted.DELETED.getValue()) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getStatus() != 0) {
            throw new BusinessException(400, "文章不在待审核状态，无法驳回");
        }

        post.setStatus(PostStatus.REJECTED.getValue());
        post.setReviewerId(reviewerId);
        post.setReviewTime(LocalDateTime.now());
        post.setRejectReason(htmlSanitizer.sanitizePlainText(reason));
        this.updateById(post);

        String rejectTitle = "文章审核驳回";
        String rejectContent = "您的文章《" + post.getTitle() + "》未通过审核，驳回原因：" + reason;
        notificationService.sendNotification("AUDIT", rejectTitle, rejectContent, reviewerId, post.getUserId(), "POST", post.getId());

        log.info("文章审核驳回: postId={}, reviewerId={}, reason={}, title={}", postId, reviewerId, reason, post.getTitle());
    }

    // ==================== 委托方法 — 查询 ====================

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {
        return postQueryService.getPostDetail(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostListResponse> getPostList(PostQueryRequest request) {
        return postQueryService.getPostList(request);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostListResponse> advancedSearch(PostAdvancedSearchRequest request) {
        return postQueryService.advancedSearch(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword) {
        return postQueryService.getSearchSuggestions(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostListResponse> getMyPosts(Long userId, Integer page, Integer pageSize) {
        return postQueryService.getMyPosts(userId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostDetailResponse> getAdminPostList(AdminPostQueryRequest request) {
        return postQueryService.getAdminPostList(request);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostDetailResponse> getReviewList(String keyword, Integer page, Integer pageSize) {
        return postQueryService.getReviewList(keyword, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<SaveDraftRequest> getMyDrafts(Long userId, Integer page, Integer pageSize) {
        return postQueryService.getMyDrafts(userId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public SaveDraftRequest getLatestDraft(Long userId) {
        return postQueryService.getLatestDraft(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public SaveDraftRequest getDraft(Long draftId, Long userId) {
        return postQueryService.getDraft(draftId, userId);
    }

    // ==================== 委托方法 — 交互 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long postId, String userKey) {
        postInteractionService.incrementViewCount(postId, userKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long postId) {
        postInteractionService.incrementViewCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementLikeCount(Long postId) {
        postInteractionService.incrementLikeCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementLikeCount(Long postId) {
        postInteractionService.decrementLikeCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementCommentCount(Long postId) {
        postInteractionService.incrementCommentCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementCommentCount(Long postId, int count) {
        postInteractionService.decrementCommentCount(postId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementCollectCount(Long postId) {
        postInteractionService.incrementCollectCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementCollectCount(Long postId) {
        postInteractionService.decrementCollectCount(postId);
    }

    // ==================== 私有工具方法 ====================

    private Set<Long> parseTopicIds(String topicIdsJson) {
        if (topicIdsJson == null || topicIdsJson.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        try {
            return new java.util.HashSet<>(cn.hutool.json.JSONUtil.toList(topicIdsJson, Long.class));
        } catch (Exception e) {
            return java.util.Collections.emptySet();
        }
    }

    private void savePostTags(Long postId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        blogPostTagMapper.batchInsertPostTags(postId, tagIds);
    }

    private void validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<BlogTag> existingTags = blogTagMapper.selectBatchIds(tagIds);
        if (existingTags.size() != tagIds.size()) {
            throw new BusinessException(400, "部分标签ID不存在");
        }
    }

    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        if (tagIds != null && !tagIds.isEmpty()) {
            return tagIds;
        }
        if (tagNames == null || tagNames.isEmpty()) {
            return null;
        }
        return tagNames.stream()
                .map(name -> blogTagService.getOrCreateTag(name))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
