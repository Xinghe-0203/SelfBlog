package com.selfblog.service.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.dto.post.AdminPostQueryRequest;
import com.selfblog.dto.post.PostAdvancedSearchRequest;
import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.dto.post.SaveDraftRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;

import java.util.List;

/**
 * 文章服务接口
 */
public interface BlogPostService extends IService<BlogPost> {

    /**
     * 创建文章
     * @param request 创建请求
     * @param userId 作者ID
     * @return 创建的文章ID
     */
    Long createPost(PostCreateRequest request, Long userId);

    /**
     * 创建文章
     * @param request 创建请求
     * @param userId 作者ID
     * @param isAdmin 是否是管理员
     * @return 创建的文章ID
     */
    Long createPost(PostCreateRequest request, Long userId, boolean isAdmin);

    /**
     * 更新文章
     * @param request 更新请求
     * @param userId 操作人ID
     */
    void updatePost(PostCreateRequest request, Long userId);

    /**
     * 更新文章
     * @param request 更新请求
     * @param userId 操作人ID
     * @param isAdmin 是否是管理员
     */
    void updatePost(PostCreateRequest request, Long userId, boolean isAdmin);

    /**
     * 删除文章
     * @param postId 文章ID
     * @param userId 操作人ID
     */
    void deletePost(Long postId, Long userId);

    /**
     * 删除文章
     * @param postId 文章ID
     * @param userId 操作人ID
     * @param isAdmin 是否是管理员
     */
    void deletePost(Long postId, Long userId, boolean isAdmin);

    /**
     * 获取文章详情
     * @param postId 文章ID
     * @return 文章详情
     */
    PostDetailResponse getPostDetail(Long postId);

    /**
     * 分页查询文章列表
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<PostListResponse> getPostList(PostQueryRequest request);

    /**
     * 增加阅读量
     * @param postId 文章ID
     */
    void incrementViewCount(Long postId);

    /**
     * 增加阅读量（带防刷校验）
     * @param postId 文章ID
     * @param userKey 用户标识（用于防刷校验）
     */
    void incrementViewCount(Long postId, String userKey);

    /**
     * 增加点赞数
     * @param postId 文章ID
     */
    void incrementLikeCount(Long postId);

    /**
     * 减少点赞数
     * @param postId 文章ID
     */
    void decrementLikeCount(Long postId);

    /**
     * 增加评论数
     * @param postId 文章ID
     */
    void incrementCommentCount(Long postId);

    /**
     * 减少评论数
     * @param postId 文章ID
     * @param count 减少数量
     */
    void decrementCommentCount(Long postId, int count);

    /**
     * 增加收藏数
     * @param postId 文章ID
     */
    void incrementCollectCount(Long postId);

    /**
     * 减少收藏数
     * @param postId 文章ID
     */
    void decrementCollectCount(Long postId);

    /**
     * 保存草稿
     * @param userId 用户ID
     * @param request 草稿请求
     * @return 草稿ID
     */
    Long saveDraft(Long userId, SaveDraftRequest request);

    /**
     * 获取我的最新草稿
     * @param userId 用户ID
     * @return 最新草稿
     */
    SaveDraftRequest getLatestDraft(Long userId);

    /**
     * 删除草稿
     * @param draftId 草稿ID
     * @param userId 操作人ID
     */
    void deleteDraft(Long draftId, Long userId);

    /**
     * 删除草稿
     * @param draftId 草稿ID
     * @param userId 操作人ID
     * @param isAdmin 是否是管理员
     */
    void deleteDraft(Long draftId, Long userId, boolean isAdmin);

    /**
     * 获取指定草稿
     * @param draftId 草稿ID
     * @param userId 操作人ID
     * @return 草稿详情
     */
    SaveDraftRequest getDraft(Long draftId, Long userId);

    /**
     * 获取我的草稿列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<SaveDraftRequest> getMyDrafts(Long userId, Integer page, Integer pageSize);

    /**
     * 文章高级搜索（公开接口）
     */
    IPage<PostListResponse> advancedSearch(PostAdvancedSearchRequest request);

    /**
     * 获取搜索建议（标题自动补全）
     * @param keyword 关键词
     * @return 建议标题列表
     */
    List<String> getSearchSuggestions(String keyword);

    /**
     * 获取我的文章列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<PostListResponse> getMyPosts(Long userId, Integer page, Integer pageSize);

    /**
     * 获取管理员文章列表（支持状态筛选、用户筛选、分类筛选）
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<PostDetailResponse> getAdminPostList(AdminPostQueryRequest request);

    /**
     * 管理员删除文章（级联删除评论、点赞、收藏、关联关系）
     * @param postId 文章ID
     * @param adminId 管理员ID
     */
    void adminDeletePost(Long postId, Long adminId);

    /**
     * 获取待审核文章列表
     * @param keyword 关键词（可选）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<PostDetailResponse> getReviewList(String keyword, Integer page, Integer pageSize);

    /**
     * 审核通过文章
     * @param postId 文章ID
     * @param reviewerId 审核人ID
     */
    void approvePost(Long postId, Long reviewerId);

    /**
     * 驳回文章
     * @param postId 文章ID
     * @param reviewerId 审核人ID
     * @param reason 驳回原因
     */
    void rejectPost(Long postId, Long reviewerId, String reason);
}
