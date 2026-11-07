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
import com.selfblog.utils.SqlLikeUtils;
import com.selfblog.vo.post.ArchivePostVO;
import com.selfblog.vo.post.AdjacentPostVO;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;
import com.selfblog.vo.post.RelatedPostVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * 自身的代理引用：getTagsMapByPostIds / getTagsByPostId 上标了 @Cacheable，
     * 但类内部直接调用属于自调用，不会经过缓存代理，那两个缓存区域永远不会被写入。
     * 走 self 才能让注解真正生效；@Lazy 用来打破自引用造成的循环依赖。
     */
    @Autowired
    @Lazy
    private PostQueryService self;

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

        return assemblePostDetail(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getPublishedPostDetail(Long postId) {
        BlogPost post = getOne(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getId, postId).eq(BlogPost::getStatus, PostStatus.PUBLISHED.getValue()));
        if (post == null) throw new BusinessException(404, "文章不存在");
        return assemblePostDetail(post);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<BlogPost> getPublishedMetadata(long page, long size) {
        if (page < 1 || size < 1 || size > 1000 || page > Long.MAX_VALUE / size) throw new IllegalArgumentException("Invalid metadata page");
        return page(new Page<>(page, size), new LambdaQueryWrapper<BlogPost>()
                .select(BlogPost::getId, BlogPost::getTitle, BlogPost::getSummary, BlogPost::getCategory,
                        BlogPost::getCoverUrl, BlogPost::getCreateTime, BlogPost::getUpdateTime)
                .eq(BlogPost::getStatus, PostStatus.PUBLISHED.getValue()).orderByAsc(BlogPost::getId));
    }

    private PostDetailResponse assemblePostDetail(BlogPost post) {
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

        response.setTags(self.getTagsByPostId(post.getId()));

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
            key = "'postList:' + #request.pageNum + ':' + #request.pageSize + ':' + #request.category + ':' + #request.sort + ':' + (#request.keyword ?: '') + ':' + (#request.userId ?: '') + ':' + (#request.tagId ?: '')")
    @Transactional(readOnly = true)
    public IPage<PostListResponse> getPostList(PostQueryRequest request) {
        Page<BlogPost> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPost::getStatus, 1)
                .ne(BlogPost::getIsDeleted, 1);

        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            String pattern = SqlLikeUtils.containsPattern(request.getKeyword());
            wrapper.and(w -> w.apply("title LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("content LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern));
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

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = self.getTagsMapByPostIds(postIds);

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
            String pattern = SqlLikeUtils.containsPattern(request.getKeyword().trim());
            wrapper.and(w -> w.apply("title LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("content LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("summary LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern));
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
        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = self.getTagsMapByPostIds(postIds);
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
                .apply("title LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, SqlLikeUtils.containsPattern(keyword.trim()))
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

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = self.getTagsMapByPostIds(postIds);

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
            String pattern = SqlLikeUtils.containsPattern(request.getKeyword().trim());
            wrapper.and(w -> w.apply("title LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("content LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern));
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

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = self.getTagsMapByPostIds(postIds);

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
            String pattern = SqlLikeUtils.containsPattern(keyword.trim());
            wrapper.and(w -> w.apply("title LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("content LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern));
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

        Map<Long, List<PostDetailResponse.TagVO>> postTagsMap = self.getTagsMapByPostIds(postIds);

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

    /**
     * 标签映射按 postIds 缓存。缓存键必须是参数的纯函数：这里用列表的 toString()。
     * 注意不能再写 `#postIds.![#this].sort().join(',')` —— 投影结果是没有无参 sort() 的
     * java.util.ArrayList，一旦注解真正生效（走 self 代理后）就会抛
     * SpelEvaluationException: Method sort() cannot be found on type java.util.ArrayList，
     * 让所有列表接口 500。入参顺序不同只会多一次缓存未命中，不会返回错误数据。
     */
    @Override
    @Cacheable(value = CaffeineCacheConfig.CATEGORY_CACHE,
            key = "'postTags:' + (#postIds == null ? 'empty' : #postIds.toString())")
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

    @Override
    public Map<String, AdjacentPostVO> getAdjacentPosts(Long postId) {
        BlogPost current = getById(postId);
        if (current == null || current.getCreateTime() == null) {
            return Collections.emptyMap();
        }

        LocalDateTime created = current.getCreateTime();
        Map<String, AdjacentPostVO> result = new HashMap<>(4);
        // 阅读顺序按发布时间：prev = 更早的一篇，next = 更新的一篇
        result.put("prev", findAdjacent(created, current.getId(), false));
        result.put("next", findAdjacent(created, current.getId(), true));
        return result;
    }

    /**
     * 取相邻的一篇已发布文章。
     *
     * @param newer true 取更新的一篇（时间更晚，取其中最早的），false 取更早的一篇（取其中最晚的）
     */
    private AdjacentPostVO findAdjacent(LocalDateTime created, Long currentId, boolean newer) {
        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getStatus, PostStatus.PUBLISHED.getValue())
                .ne(BlogPost::getId, currentId);

        // 发布时间相同时用 id 兜底，避免漏掉文章或重复指回自己
        if (newer) {
            wrapper.and(w -> w.gt(BlogPost::getCreateTime, created)
                            .or(inner -> inner.eq(BlogPost::getCreateTime, created)
                                            .gt(BlogPost::getId, currentId)))
                    .orderByAsc(BlogPost::getCreateTime)
                    .orderByAsc(BlogPost::getId);
        } else {
            wrapper.and(w -> w.lt(BlogPost::getCreateTime, created)
                            .or(inner -> inner.eq(BlogPost::getCreateTime, created)
                                            .lt(BlogPost::getId, currentId)))
                    .orderByDesc(BlogPost::getCreateTime)
                    .orderByDesc(BlogPost::getId);
        }

        // 分页取第一条（不查总数），避免依赖 MySQL 专有的 LIMIT 写法
        Page<BlogPost> page = page(new Page<>(1, 1, false), wrapper);
        if (page.getRecords().isEmpty()) {
            return null;
        }
        BlogPost adjacent = page.getRecords().get(0);
        AdjacentPostVO vo = new AdjacentPostVO();
        vo.setId(adjacent.getId());
        vo.setTitle(adjacent.getTitle());
        return vo;
    }

    /** 相关文章推荐的目标篇数。 */
    private static final int RELATED_POST_LIMIT = 5;

    /** 归档条目的安全上限：个人博客量级不分页，超出部分宁可截断也不拖垮归档页。 */
    private static final int ARCHIVES_SAFETY_CAP = 2000;

    /** 相关文章摘要的展示上限（字符），超出截断。 */
    private static final int RELATED_SUMMARY_LIMIT = 100;

    @Override
    @Transactional(readOnly = true)
    public List<RelatedPostVO> getRelatedPosts(Long postId) {
        BlogPost current = getById(postId);
        if (current == null) {
            // 与 getPostDetail 同一套 404 语义
            throw new BusinessException(404, "文章不存在");
        }
        // 未发布（草稿/待审/驳回）：公开页只对已发布文章调用；作者预览草稿时拿到空推荐
        if (current.getStatus() == null || current.getStatus() != PostStatus.PUBLISHED.getValue()) {
            return Collections.emptyList();
        }

        // 复用既有标签查询取当前文章的标签；再按 blog_post_tag 找共享这些标签的其他文章。
        // BlogPostTag 带 @TableLogic，selectList 自动过滤 is_deleted=0，与全库语义一致。
        LinkedHashMap<Long, Integer> sharedTagCounts = new LinkedHashMap<>();
        List<PostDetailResponse.TagVO> ownTags = self.getTagsByPostId(postId);
        if (ownTags != null && !ownTags.isEmpty()) {
            List<Long> ownTagIds = ownTags.stream()
                    .map(PostDetailResponse.TagVO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!ownTagIds.isEmpty()) {
                List<BlogPostTag> relations = blogPostTagMapper.selectList(new LambdaQueryWrapper<BlogPostTag>()
                        .in(BlogPostTag::getTagId, ownTagIds)
                        .ne(BlogPostTag::getPostId, postId));
                for (BlogPostTag relation : relations) {
                    if (relation.getPostId() != null) {
                        sharedTagCounts.merge(relation.getPostId(), 1, Integer::sum);
                    }
                }
            }
        }

        List<RelatedPostVO> result = new ArrayList<>(RELATED_POST_LIMIT);
        List<Long> pickedIds = new ArrayList<>(RELATED_POST_LIMIT);
        if (!sharedTagCounts.isEmpty()) {
            // 共享标签数降序，同数按 id 降序（新文章优先的近因代理）
            List<Map.Entry<Long, Integer>> ranked = new ArrayList<>(sharedTagCounts.entrySet());
            ranked.sort((a, b) -> {
                int byCount = Integer.compare(b.getValue(), a.getValue());
                return byCount != 0 ? byCount : Long.compare(b.getKey(), a.getKey());
            });
            List<Long> rankedIds = ranked.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            // 候选只取已发布（status=1）；BlogPost 带 @TableLogic，逻辑删除自动过滤。
            // 先过滤再排序取前 5：草稿/已删除的候选不能占用排名席位，否则一篇草稿
            // 会把有效候选挤出前 5。个人博客量级下 rankedIds 规模有限，IN 查询可承受。
            Map<Long, BlogPost> postMap = list(new LambdaQueryWrapper<BlogPost>()
                            .eq(BlogPost::getStatus, PostStatus.PUBLISHED.getValue())
                            .in(BlogPost::getId, rankedIds))
                    .stream()
                    .collect(Collectors.toMap(BlogPost::getId, p -> p, (a, b) -> a));
            for (Long id : rankedIds) {
                BlogPost candidate = postMap.get(id);
                if (candidate == null) {
                    continue; // 草稿/已删除/取数瞬间下架的候选不占席位，直接跳过
                }
                result.add(toRelatedPostVO(candidate));
                pickedIds.add(id);
                if (result.size() >= RELATED_POST_LIMIT) {
                    break;
                }
            }
        }

        // 不足 5 篇：用最新已发布文章补齐（排除当前篇与已入选的）
        if (result.size() < RELATED_POST_LIMIT) {
            List<Long> excluded = new ArrayList<>(pickedIds);
            excluded.add(postId); // notIn 列表永不空（至少含当前篇），不会生成非法 SQL
            int need = RELATED_POST_LIMIT - result.size();
            List<BlogPost> filler = page(new Page<>(1, need, false), new LambdaQueryWrapper<BlogPost>()
                            .eq(BlogPost::getStatus, PostStatus.PUBLISHED.getValue())
                            .notIn(BlogPost::getId, excluded)
                            .orderByDesc(BlogPost::getCreateTime)
                            .orderByDesc(BlogPost::getId))
                    .getRecords();
            for (BlogPost post : filler) {
                result.add(toRelatedPostVO(post));
            }
        }
        return result;
    }

    private RelatedPostVO toRelatedPostVO(BlogPost post) {
        RelatedPostVO vo = new RelatedPostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        String summary = post.getSummary() == null ? "" : post.getSummary().trim();
        vo.setSummary(summary.length() > RELATED_SUMMARY_LIMIT
                ? summary.substring(0, RELATED_SUMMARY_LIMIT) : summary);
        vo.setCoverUrl(post.getCoverUrl());
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchivePostVO> getArchives() {
        // 只投影归档页需要的 6 列，绝不拉取 content；status=1 + @TableLogic 双重过滤
        List<BlogPost> posts = page(new Page<>(1, ARCHIVES_SAFETY_CAP, false),
                        new LambdaQueryWrapper<BlogPost>()
                                .select(BlogPost::getId, BlogPost::getTitle, BlogPost::getCategory,
                                        BlogPost::getCreateTime, BlogPost::getUpdateTime, BlogPost::getViewCount)
                                .eq(BlogPost::getStatus, PostStatus.PUBLISHED.getValue())
                                .orderByDesc(BlogPost::getCreateTime)
                                .orderByDesc(BlogPost::getId))
                .getRecords();
        return posts.stream()
                .map(post -> {
                    ArchivePostVO vo = new ArchivePostVO();
                    vo.setId(post.getId());
                    vo.setTitle(post.getTitle());
                    vo.setCategory(post.getCategory());
                    vo.setCreateTime(post.getCreateTime());
                    vo.setUpdateTime(post.getUpdateTime());
                    vo.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
