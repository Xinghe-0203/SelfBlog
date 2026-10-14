package com.selfblog.service.post.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.IsDeleted;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.dto.post.AdminPostQueryRequest;
import com.selfblog.dto.post.PostAdvancedSearchRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.dto.post.SaveDraftRequest;
import com.selfblog.entity.*;
import com.selfblog.mapper.*;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.content.TopicService;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子查询服务实现 — 负责帖子查询、搜索、分页
 */
@Slf4j
@Service
public class PostQueryServiceImpl extends ServiceImpl<BlogPostMapper, BlogPost> implements PostQueryService {

    @Value("${DB_TYPE:mysql}")
    private String dbType;

    private boolean isSqlite() { return "sqlite".equalsIgnoreCase(dbType); }

    @Autowired
    private BlogPostTagMapper blogPostTagMapper;

    @Autowired
    private BlogTagMapper blogTagMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogDraftMapper blogDraftMapper;

    @Autowired
    private BlogTagService blogTagService;

    @Autowired
    private TopicService topicService;

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {
        BlogPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getIsDeleted() != null && post.getIsDeleted() == IsDeleted.DELETED.getValue()) {
            throw new BusinessException(404, "文章不存在");
        }

        Long currentUserId = com.selfblog.utils.SecurityUtils.getCurrentUserIdOrNull();
        boolean isAuthor = currentUserId != null && currentUserId.equals(post.getUserId());

        if (post.getStatus() == null || post.getStatus() != PostStatus.PUBLISHED.getValue()) {
            if (!isAuthor) {
                throw new BusinessException(404, "文章不存在");
            }
        }

        PostDetailResponse response = new PostDetailResponse();
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setTitle(post.getTitle());
        response.setSummary(post.getSummary());
        response.setContent(post.getContent());
        response.setCategory(post.getCategory());
        response.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setCollectCount(post.getCollectCount());
        response.setStatus(post.getStatus());
        response.setCreateTime(post.getCreateTime());
        response.setUpdateTime(post.getUpdateTime());
        response.setCoverImage(post.getCoverUrl());

        SysUser user = sysUserMapper.selectById(post.getUserId());
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());
        }

        response.setTags(getTagsByPostId(post.getId()));

        if (post.getTopicIds() != null && !post.getTopicIds().isEmpty()) {
            try {
                List<Long> ids = cn.hutool.json.JSONUtil.toList(post.getTopicIds(), Long.class);
                if (!ids.isEmpty()) {
                    response.setTopicId(ids.get(0));
                    Topic topic = topicService.getTopicById(ids.get(0));
                    response.setTopicName(topic.getName());
                }
            } catch (Exception e) {
                response.setTopicId(null);
                response.setTopicName(null);
            }
        }

        return response;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.TRENDING_CACHE,
            key = "'postList:' + #request.pageNum + ':' + #request.pageSize + ':' + #request.category + ':' + #request.sort")
    @Transactional(readOnly = true)
    public IPage<PostListResponse> getPostList(PostQueryRequest request) {
        Page<BlogPost> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPost::getStatus, 1)
                .ne(BlogPost::getIsDeleted, 1);

        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(BlogPost::getTitle, request.getKeyword())
                    .or()
                    .like(BlogPost::getContent, request.getKeyword()));
        }

        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            wrapper.eq(BlogPost::getCategory, request.getCategory());
        }

        if (request.getUserId() != null) {
            wrapper.eq(BlogPost::getUserId, request.getUserId());
        }

        if (request.getTagId() != null) {
            LambdaQueryWrapper<BlogPostTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(BlogPostTag::getTagId, request.getTagId());
            List<BlogPostTag> taggedPosts = blogPostTagMapper.selectList(tagWrapper);
            if (taggedPosts.isEmpty()) {
                return new Page<>(request.getPageNum(), request.getPageSize(), 0);
            }
            List<Long> taggedPostIds = taggedPosts.stream()
                    .map(BlogPostTag::getPostId)
                    .collect(Collectors.toList());
            wrapper.in(BlogPost::getId, taggedPostIds);
        }

        if (request.getSort() != null) {
            switch (request.getSort()) {
                case "hot":
                    wrapper.orderByDesc(BlogPost::getViewCount);
                    break;
                case "essence":
                    wrapper.orderByDesc(BlogPost::getLikeCount);
                    break;
                default:
                    wrapper.orderByDesc(BlogPost::getCreateTime);
                    break;
            }
        } else {
            wrapper.orderByDesc(BlogPost::getCreateTime);
        }

        IPage<BlogPost> postPage = this.page(page, wrapper);

        List<BlogPost> posts = postPage.getRecords();
        if (posts.isEmpty()) {
            return new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        }

        List<Long> userIds = posts.stream()
                .map(BlogPost::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> postIds = posts.stream()
                .map(BlogPost::getId)
                .collect(Collectors.toList());

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = getTagsMapByPostIds(postIds);

        Map<Long, String> topicNameMap = getTopicNamesMapByPostIds(postIds, posts);

        IPage<PostListResponse> result = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        result.setRecords(posts.stream()
                .map(post -> convertToListResponse(post, userMap.get(post.getUserId()), postTagsMap.get(post.getId()), topicNameMap.get(post.getTopicId())))
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostListResponse> advancedSearch(PostAdvancedSearchRequest request) {
        return advancedSearchLike(request);
    }

    private IPage<PostListResponse> advancedSearchLike(PostAdvancedSearchRequest request) {
        Page<BlogPost> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPost::getStatus, 1)
                .ne(BlogPost::getIsDeleted, 1);

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            wrapper.and(w -> w.like(BlogPost::getTitle, request.getKeyword())
                    .or()
                    .like(BlogPost::getContent, request.getKeyword())
                    .or()
                    .like(BlogPost::getSummary, request.getKeyword()));
        }

        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            wrapper.eq(BlogPost::getCategory, request.getCategory());
        }

        if (request.getUserId() != null) {
            wrapper.eq(BlogPost::getUserId, request.getUserId());
        }

        if (request.getTagId() != null) {
            LambdaQueryWrapper<BlogPostTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(BlogPostTag::getTagId, request.getTagId());
            List<BlogPostTag> taggedPosts = blogPostTagMapper.selectList(tagWrapper);
            if (taggedPosts.isEmpty()) {
                return new Page<>(request.getPageNum(), request.getPageSize(), 0);
            }
            List<Long> taggedPostIds = taggedPosts.stream()
                    .map(BlogPostTag::getPostId)
                    .collect(Collectors.toList());
            wrapper.in(BlogPost::getId, taggedPostIds);
        }

        if (request.getTopicId() != null) {
            if (isSqlite()) {
                wrapper.apply("EXISTS (SELECT 1 FROM json_each(topic_ids) WHERE CAST(value AS INTEGER) = {0})", request.getTopicId());
            } else {
                wrapper.apply("JSON_CONTAINS(topic_ids, CAST({0} AS JSON))", request.getTopicId());
            }
        }

        String sortBy = request.getSortBy();
        if ("view".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(BlogPost::getViewCount);
        } else if ("like".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(BlogPost::getLikeCount);
        } else {
            wrapper.orderByDesc(BlogPost::getCreateTime);
        }

        IPage<BlogPost> postPage = this.page(page, wrapper);

        List<BlogPost> posts = postPage.getRecords();
        if (posts.isEmpty()) {
            return new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        }

        List<Long> userIds = posts.stream()
                .map(BlogPost::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> postIds = posts.stream()
                .map(BlogPost::getId)
                .collect(Collectors.toList());

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = getTagsMapByPostIds(postIds);
        Map<Long, String> topicNameMap = getTopicNamesMapByPostIds(postIds, posts);

        IPage<PostListResponse> result = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        result.setRecords(posts.stream()
                .map(post -> convertToListResponse(post, userMap.get(post.getUserId()), postTagsMap.get(post.getId()), topicNameMap.get(post.getTopicId())))
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (keyword.trim().length() > 200) {
            keyword = keyword.trim().substring(0, 200);
        }

        if (keyword.trim().length() < 2) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPost::getStatus, 1)
                .ne(BlogPost::getIsDeleted, 1)
                .like(BlogPost::getTitle, keyword.trim())
                .select(BlogPost::getTitle)
                .orderByDesc(BlogPost::getViewCount);

        List<BlogPost> posts = this.page(new Page<>(1, 10), wrapper).getRecords();
        return posts.stream()
                .map(BlogPost::getTitle)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostListResponse> getMyPosts(Long userId, Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<BlogPost> pageObj = new Page<>(page, pageSize);

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPost::getUserId, userId)
                .eq(BlogPost::getStatus, 1)
                .ne(BlogPost::getIsDeleted, 1)
                .orderByDesc(BlogPost::getCreateTime);

        IPage<BlogPost> postPage = this.page(pageObj, wrapper);

        List<BlogPost> posts = postPage.getRecords();
        if (posts.isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        List<Long> postIds = posts.stream()
                .map(BlogPost::getId)
                .collect(Collectors.toList());

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = getTagsMapByPostIds(postIds);

        Map<Long, String> topicNameMap = getTopicNamesMapByPostIds(postIds, posts);

        SysUser user = sysUserMapper.selectById(userId);

        IPage<PostListResponse> result = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        result.setRecords(posts.stream()
                .map(post -> convertToListResponse(post, user, postTagsMap.get(post.getId()), topicNameMap.get(post.getTopicId())))
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostDetailResponse> getAdminPostList(AdminPostQueryRequest request) {
        Page<BlogPost> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(BlogPost::getIsDeleted, 1);

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            wrapper.and(w -> w.like(BlogPost::getTitle, request.getKeyword().trim())
                    .or()
                    .like(BlogPost::getContent, request.getKeyword().trim()));
        }
        if (request.getStatus() != null) {
            wrapper.eq(BlogPost::getStatus, request.getStatus());
        }
        if (request.getUserId() != null) {
            wrapper.eq(BlogPost::getUserId, request.getUserId());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            wrapper.eq(BlogPost::getCategory, request.getCategory().trim());
        }

        wrapper.orderByDesc(BlogPost::getCreateTime);

        IPage<BlogPost> postPage = this.page(page, wrapper);

        List<BlogPost> posts = postPage.getRecords();
        if (posts.isEmpty()) {
            return new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        }

        List<Long> userIds = posts.stream()
                .map(BlogPost::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> postIds = posts.stream()
                .map(BlogPost::getId)
                .collect(Collectors.toList());

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = getTagsMapByPostIds(postIds);

        Map<Long, String> topicNameMap = getTopicNamesMapByPostIds(postIds, posts);

        IPage<PostDetailResponse> result = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        result.setRecords(posts.stream()
                .map(post -> convertToDetailResponse(post, userMap.get(post.getUserId()), postTagsMap.get(post.getId()), topicNameMap.get(post.getTopicId())))
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PostDetailResponse> getReviewList(String keyword, Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<BlogPost> pageObj = new Page<>(page, pageSize);

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPost::getStatus, 0)
                .ne(BlogPost::getIsDeleted, 1);

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(BlogPost::getTitle, keyword.trim())
                    .or()
                    .like(BlogPost::getContent, keyword.trim()));
        }

        wrapper.orderByDesc(BlogPost::getCreateTime);

        IPage<BlogPost> postPage = this.page(pageObj, wrapper);

        List<BlogPost> posts = postPage.getRecords();
        if (posts.isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        List<Long> userIds = posts.stream()
                .map(BlogPost::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> postIds = posts.stream()
                .map(BlogPost::getId)
                .collect(Collectors.toList());

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = getTagsMapByPostIds(postIds);

        Map<Long, String> topicNameMap = getTopicNamesMapByPostIds(postIds, posts);

        IPage<PostDetailResponse> result = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        result.setRecords(posts.stream()
                .map(post -> convertToDetailResponse(post, userMap.get(post.getUserId()), postTagsMap.get(post.getId()), topicNameMap.get(post.getTopicId())))
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<SaveDraftRequest> getMyDrafts(Long userId, Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<BlogDraft> pageObj = new Page<>(page, pageSize);

        LambdaQueryWrapper<BlogDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDraft::getUserId, userId)
                .orderByDesc(BlogDraft::getUpdateTime);

        IPage<BlogDraft> draftPage = blogDraftMapper.selectPage(pageObj, wrapper);

        IPage<SaveDraftRequest> result = new Page<>(draftPage.getCurrent(), draftPage.getSize(), draftPage.getTotal());
        result.setRecords(draftPage.getRecords().stream()
                .map(this::convertToSaveDraftRequest)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SaveDraftRequest getLatestDraft(Long userId) {
        LambdaQueryWrapper<BlogDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDraft::getUserId, userId)
                .orderByDesc(BlogDraft::getUpdateTime)
                .last("LIMIT 1");
        BlogDraft draft = blogDraftMapper.selectOne(wrapper);

        if (draft == null) {
            return null;
        }

        return convertToSaveDraftRequest(draft);
    }

    @Override
    @Transactional(readOnly = true)
    public SaveDraftRequest getDraft(Long draftId, Long userId) {
        BlogDraft draft = blogDraftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(404, "草稿不存在");
        }
        if (!Objects.equals(userId, draft.getUserId())) {
            throw new BusinessException(403, "无权查看此草稿");
        }

        return convertToSaveDraftRequest(draft);
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.CATEGORY_CACHE, key = "'postTags:' + #postIds.![#this].sort().join(',')")
    public Map<Long, List<PostDetailResponse.TagVO>> getTagsMapByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<BlogPostTag> postTagWrapper = new LambdaQueryWrapper<>();
        postTagWrapper.in(BlogPostTag::getPostId, postIds);
        List<BlogPostTag> postTags = blogPostTagMapper.selectList(postTagWrapper);

        if (postTags == null || postTags.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> tagIds = postTags.stream()
                .map(BlogPostTag::getTagId)
                .distinct()
                .collect(Collectors.toList());

        List<BlogTag> tags = blogTagMapper.selectBatchIds(tagIds);
        Map<Long, String> tagNameMap = tags.stream()
                .collect(Collectors.toMap(BlogTag::getId, BlogTag::getName, (a, b) -> a));

        return postTags.stream()
                .collect(Collectors.groupingBy(
                        BlogPostTag::getPostId,
                        Collectors.mapping(tag -> {
                            PostDetailResponse.TagVO tagVO = new PostDetailResponse.TagVO();
                            tagVO.setId(tag.getTagId());
                            tagVO.setName(tagNameMap.get(tag.getTagId()));
                            return tagVO;
                        }, Collectors.toList())
                ));
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.CATEGORY_CACHE, key = "'tagsByPost:' + #postId")
    public List<PostDetailResponse.TagVO> getTagsByPostId(Long postId) {
        LambdaQueryWrapper<BlogPostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPostTag::getPostId, postId);
        List<BlogPostTag> postTags = blogPostTagMapper.selectList(wrapper);

        if (postTags == null || postTags.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = postTags.stream()
                .map(BlogPostTag::getTagId)
                .collect(Collectors.toList());

        List<BlogTag> tags = blogTagMapper.selectBatchIds(tagIds);

        return tags.stream()
                .map(tag -> {
                    PostDetailResponse.TagVO tagVO = new PostDetailResponse.TagVO();
                    tagVO.setId(tag.getId());
                    tagVO.setName(tag.getName());
                    return tagVO;
                })
                .collect(Collectors.toList());
    }

    // ==================== Private helper methods ====================

    private Map<Long, String> getTopicNamesMapByPostIds(List<Long> postIds, List<BlogPost> posts) {
        Map<Long, String> map = new HashMap<>();
        List<Long> allTopicIds = new ArrayList<>();
        for (BlogPost post : posts) {
            if (post.getTopicIds() != null && !post.getTopicIds().isEmpty()) {
                try {
                    List<Long> ids = cn.hutool.json.JSONUtil.toList(post.getTopicIds(), Long.class);
                    allTopicIds.addAll(ids);
                } catch (Exception e) {
                    // ignore parse errors
                }
            }
        }
        if (allTopicIds.isEmpty()) {
            return map;
        }
        List<Topic> topics = topicService.listByIds(allTopicIds.stream().distinct().collect(Collectors.toList()));
        // 过滤掉已禁用的话题
        return topics.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                .collect(Collectors.toMap(Topic::getId, Topic::getName, (a, b) -> a));
    }

    private void setCommonPostFields(PostListResponse response, BlogPost post, SysUser user, List<PostDetailResponse.TagVO> tags, String topicName) {
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setTitle(post.getTitle());
        response.setSummary(post.getSummary());
        response.setCategory(post.getCategory());
        response.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setCollectCount(post.getCollectCount());
        response.setShareCount(post.getShareCount());
        response.setCreateTime(post.getCreateTime());
        response.setCoverImage(post.getCoverUrl());
        if (post.getTopicIds() != null && !post.getTopicIds().isEmpty()) {
            try {
                List<Long> ids = cn.hutool.json.JSONUtil.toList(post.getTopicIds(), Long.class);
                if (!ids.isEmpty()) {
                    response.setTopicId(ids.get(0));
                }
            } catch (Exception e) {
                response.setTopicId(null);
            }
        }
        response.setTopicName(topicName);

        if (user != null) {
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());
        }

        if (tags != null) {
            response.setTags(tags.stream()
                    .map(tag -> {
                        PostListResponse.TagVO tagVO = new PostListResponse.TagVO();
                        tagVO.setId(tag.getId());
                        tagVO.setName(tag.getName());
                        return tagVO;
                    })
                    .collect(Collectors.toList()));
        } else {
            response.setTags(Collections.emptyList());
        }
    }

    private PostListResponse convertToListResponse(BlogPost post, SysUser user, List<PostDetailResponse.TagVO> tags, String topicName) {
        PostListResponse response = new PostListResponse();
        setCommonPostFields(response, post, user, tags, topicName);
        return response;
    }

    private void setCommonPostFields(PostDetailResponse response, BlogPost post, SysUser user, List<PostDetailResponse.TagVO> tags, String topicName) {
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setTitle(post.getTitle());
        response.setSummary(post.getSummary());
        response.setCategory(post.getCategory());
        response.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setCollectCount(post.getCollectCount());
        response.setStatus(post.getStatus());
        response.setReviewerId(post.getReviewerId());
        response.setReviewTime(post.getReviewTime());
        response.setRejectReason(post.getRejectReason());
        response.setCreateTime(post.getCreateTime());
        response.setUpdateTime(post.getUpdateTime());
        response.setCoverImage(post.getCoverUrl());
        response.setTopicId(post.getTopicId());
        response.setTopicName(topicName);

        if (user != null) {
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());
        }

        response.setTags(tags != null ? tags : Collections.emptyList());
    }

    private PostDetailResponse convertToDetailResponse(BlogPost post, SysUser user, List<PostDetailResponse.TagVO> tags, String topicName) {
        PostDetailResponse response = new PostDetailResponse();
        setCommonPostFields(response, post, user, tags, topicName);
        response.setContent(post.getContent());
        return response;
    }

    private SaveDraftRequest convertToSaveDraftRequest(BlogDraft draft) {
        SaveDraftRequest request = new SaveDraftRequest();
        request.setDraftId(draft.getId());
        request.setTitle(draft.getTitle());
        request.setContent(draft.getContent());
        request.setSummary(draft.getSummary());
        request.setCategory(draft.getCategory());
        request.setPostId(draft.getPostId());
        request.setTopicId(draft.getTopicId());

        if (draft.getTagIds() != null && !draft.getTagIds().isEmpty()) {
            try {
                String[] tagIdStrs = draft.getTagIds().split(",");
                request.setTagIds(java.util.Arrays.stream(tagIdStrs)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
            } catch (NumberFormatException e) {
                log.warn("草稿 tagIds 格式错误: draftId={}, tagIds={}", draft.getId(), draft.getTagIds());
            }
        }

        request.setCoverImage(draft.getCoverImage());

        return request;
    }
}
