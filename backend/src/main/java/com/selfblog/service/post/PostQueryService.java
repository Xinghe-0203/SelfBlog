package com.selfblog.service.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.dto.post.AdminPostQueryRequest;
import com.selfblog.dto.post.PostAdvancedSearchRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.dto.post.SaveDraftRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.vo.post.ArchivePostVO;
import com.selfblog.vo.post.AdjacentPostVO;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;
import com.selfblog.vo.post.RelatedPostVO;

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

    /**
     * 获取当前文章的相关文章推荐（文章详情页「相关阅读」板块）。
     *
     * <p>当前文章必须已发布，否则返回空列表（作者预览草稿时不展示推荐）；
     * 不存在的文章与详情接口同一套 404 语义。推荐优先按共享标签数降序，
     * 同数按 id 降序（新文章优先），不足 5 篇用最新已发布文章补齐；
     * 全程只读，不触碰阅读计数。</p>
     *
     * @param postId 当前文章ID
     * @return 最多 5 篇的相关文章（可能少于 5 篇甚至为空）
     */
    List<RelatedPostVO> getRelatedPosts(Long postId);

    /**
     * 获取全站归档（归档页时间轴），只返回已发布文章。
     *
     * <p>仅投影 id/title/category/createTime/updateTime/viewCount 六列（绝不拉正文），
     * 按创建时间倒序、同时间戳 id 降序；无分页（个人博客量级），上限 2000 条兜底。</p>
     *
     * @return 归档条目列表
     */
    List<ArchivePostVO> getArchives();
}
