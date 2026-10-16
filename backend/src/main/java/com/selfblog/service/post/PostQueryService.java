package com.selfblog.service.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.dto.post.AdminPostQueryRequest;
import com.selfblog.dto.post.PostAdvancedSearchRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.dto.post.SaveDraftRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.vo.post.AdjacentPostVO;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;

import java.util.List;
import java.util.Map;

/**
 * 帖子查询服务接口 — 负责帖子查询、搜索、分页
 */
public interface PostQueryService extends IService<BlogPost> {

    /**
     * 获取文章详情
     */
    PostDetailResponse getPostDetail(Long postId);

    /** Published-only public document; unlike author detail, never exposes an unpublished post. No counters. */
    PostDetailResponse getPublishedPostDetail(Long postId);

    /** Stable ID-ordered metadata pagination for discovery; size1..1000, no body selected. */
    IPage<BlogPost> getPublishedMetadata(long page, long size);

    /**
     * 分页查询文章列表
     */
    IPage<PostListResponse> getPostList(PostQueryRequest request);

    /**
     * 文章高级搜索
     */
    IPage<PostListResponse> advancedSearch(PostAdvancedSearchRequest request);

    /**
     * 获取搜索建议
     */
    List<String> getSearchSuggestions(String keyword);

    /**
     * 获取我的文章列表
     */
    IPage<PostListResponse> getMyPosts(Long userId, Integer page, Integer pageSize);

    /**
     * 获取管理员文章列表
     */
    IPage<PostDetailResponse> getAdminPostList(AdminPostQueryRequest request);

    /**
     * 获取待审核文章列表
     */
    IPage<PostDetailResponse> getReviewList(String keyword, Integer page, Integer pageSize);

    /**
     * 获取我的草稿列表
     */
    IPage<SaveDraftRequest> getMyDrafts(Long userId, Integer page, Integer pageSize);

    /**
     * 获取最新草稿
     */
    SaveDraftRequest getLatestDraft(Long userId);

    /**
     * 获取指定草稿
     */
    SaveDraftRequest getDraft(Long draftId, Long userId);

    /**
     * 根据文章ID批量获取标签映射
     */
    Map<Long, List<PostDetailResponse.TagVO>> getTagsMapByPostIds(List<Long> postIds);

    /**
     * 根据文章ID获取标签列表
     */
    List<PostDetailResponse.TagVO> getTagsByPostId(Long postId);

    /**
     * 获取相邻文章，用于文章详情页底部的上一篇/下一篇导航。
     *
     * <p>按发布时间（createTime）排序，只返回已发布文章；同一时间戳用 id 兜底排序。
     *
     * @param postId 当前文章ID
     * @return 固定包含 "prev"（更早一篇）与 "next"（更新一篇）两个键，不存在时值为 null
     */
    Map<String, AdjacentPostVO> getAdjacentPosts(Long postId);
}
