package com.selfblog.controller.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.dto.post.PostAdvancedSearchRequest;
import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.dto.post.SaveDraftRequest;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.post.AdjacentPostVO;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文章管理控制器
 */
@Tag(name = "文章管理", description = "文章相关接口")
@RestController
@RequestMapping("/post")
@Validated
@RequiredArgsConstructor
public class BlogPostController {
    private static final Logger log = LoggerFactory.getLogger(BlogPostController.class);

    private final BlogPostService blogPostService;

    /**
     * 获取用户标识：优先用userId，未登录用指纹（IP + User-Agent组合）
     * 注意：IP可能被伪造，但结合User-Agent会增加伪造成本
     */
    private String getUserIdentifier(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId != null) {
            return "user-" + userId;
        }
        String ip = request.getRemoteAddr();
        if (ip == null || ip.isEmpty()) {
            ip = "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "unknown";
        }
        return "guest-" + ip + "-" + userAgent.hashCode();
    }

    /**
     * 创建文章
     */
    @RequiresAuth
    @Operation(summary = "发布文章")
    @PostMapping
    public Result<Long> createPost(@Valid @RequestBody PostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        Long postId = blogPostService.createPost(request, userId, isAdmin);
        return Result.success(postId);
    }

    /**
     * 更新文章
     */
    @RequiresAuth
    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public Result<Void> updatePost(@PathVariable Long id,
                                    @Valid @RequestBody PostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setId(id);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        blogPostService.updatePost(request, userId, isAdmin);
        return Result.success(null);
    }

    /**
     * 删除文章
     */
    @RequiresAuth
    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        blogPostService.deletePost(id, userId, isAdmin);
        return Result.success(null);
    }

    /**
     * 获取文章详情
     */
    @Operation(summary = "获取文章详情")
    @GetMapping("/{id}")
    public Result<PostDetailResponse> getPostDetail(@PathVariable Long id, HttpServletRequest request) {
        String userKey = getUserIdentifier(request);
        try {
            blogPostService.incrementViewCount(id, userKey);
        } catch (Exception e) {
            log.warn("Failed to increment view count for post {}", id, e);
        }
        PostDetailResponse detail = blogPostService.getPostDetail(id);
        return Result.success(detail);
    }

    /**
     * 获取相邻文章（上一篇 / 下一篇）
     */
    @Operation(summary = "获取相邻文章")
    @GetMapping("/{id}/adjacent")
    public Result<Map<String, AdjacentPostVO>> getAdjacentPosts(@PathVariable Long id) {
        return Result.success(blogPostService.getAdjacentPosts(id));
    }

    /**
     * 获取文章列表（分页）
     */
    @Operation(summary = "获取文章列表")
    @GetMapping("/list")
    public Result<IPage<PostListResponse>> getPostList(@Valid PostQueryRequest request) {
        IPage<PostListResponse> list = blogPostService.getPostList(request);
        return Result.success(list);
    }

    /**
     * 增加阅读量
     */
    @Operation(summary = "增加阅读量")
    @PutMapping("/{id}/view")
    public Result<Void> incrementViewCount(@PathVariable Long id, HttpServletRequest request) {
        String userKey = getUserIdentifier(request);
        blogPostService.incrementViewCount(id, userKey);
        return Result.success(null);
    }

    /**
     * 保存草稿
     */
    @RequiresAuth
    @Operation(summary = "保存草稿")
    @PostMapping("/draft")
    public Result<Long> saveDraft(@Valid @RequestBody SaveDraftRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long draftId = blogPostService.saveDraft(userId, request);
        return Result.success(draftId);
    }

    /**
     * 获取我的最新草稿
     */
    @RequiresAuth
    @Operation(summary = "获取我的最新草稿")
    @GetMapping("/draft/latest")
    public Result<SaveDraftRequest> getLatestDraft() {
        Long userId = SecurityUtils.getCurrentUserId();
        SaveDraftRequest draft = blogPostService.getLatestDraft(userId);
        return Result.success(draft);
    }

    /**
     * 删除草稿
     */
    @RequiresAuth
    @Operation(summary = "删除草稿")
    @DeleteMapping("/draft/{draftId}")
    public Result<Void> deleteDraft(@PathVariable Long draftId) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        blogPostService.deleteDraft(draftId, userId, isAdmin);
        return Result.success(null);
    }

    /**
     * 获取指定草稿
     */
    @RequiresAuth
    @Operation(summary = "获取指定草稿")
    @GetMapping("/draft/{draftId}")
    public Result<SaveDraftRequest> getDraft(@PathVariable Long draftId) {
        Long userId = SecurityUtils.getCurrentUserId();
        SaveDraftRequest draft = blogPostService.getDraft(draftId, userId);
        return Result.success(draft);
    }

    /**
     * 获取我的草稿列表
     */
    @RequiresAuth
    @Operation(summary = "获取我的草稿列表")
    @GetMapping("/draft/my")
    public Result<IPage<SaveDraftRequest>> getMyDrafts(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<SaveDraftRequest> result = blogPostService.getMyDrafts(userId, page, pageSize);
        return Result.success(result);
    }

    /**
     * 文章高级搜索（公开接口）
     */
    @Operation(summary = "文章高级搜索")
    @GetMapping("/search/advanced")
    public Result<IPage<PostListResponse>> advancedSearch(@Valid PostAdvancedSearchRequest request) {
        IPage<PostListResponse> result = blogPostService.advancedSearch(request);
        return Result.success(result);
    }

    /**
     * 获取搜索建议（标题自动补全）
     */
    @Operation(summary = "获取搜索建议")
    @GetMapping("/search/suggest")
    public Result<List<String>> getSearchSuggestions(@RequestParam(required = false) String keyword) {
        // 搜索建议是公开功能，不需要登录
        List<String> suggestions = blogPostService.getSearchSuggestions(keyword);
        return Result.success(suggestions);
    }

    /**
     * 获取我的文章列表
     */
    @RequiresAuth
    @Operation(summary = "获取我的文章列表")
    @GetMapping("/my")
    public Result<IPage<PostListResponse>> getMyPosts(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<PostListResponse> result = blogPostService.getMyPosts(userId, page, pageSize);
        return Result.success(result);
    }
}
