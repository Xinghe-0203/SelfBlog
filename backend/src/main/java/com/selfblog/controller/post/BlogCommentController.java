package com.selfblog.controller.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.dto.social.CommentCreateRequest;
import com.selfblog.service.social.BlogCommentService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.post.CommentVO;
import com.selfblog.vo.post.CommentWithPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 */
@Tag(name = "评论管理", description = "评论相关接口")
@RestController
@RequestMapping("/comment")
@Validated
@RequiredArgsConstructor
public class BlogCommentController {

    private final BlogCommentService blogCommentService;

    /**
     * 发表评论
     */
    @RequiresAuth
    @Operation(summary = "发表评论")
    @PostMapping
    public Result<Long> createComment(@Valid @RequestBody CommentCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long commentId = blogCommentService.createComment(request, userId);
        return Result.success(commentId);
    }

    /**
     * 获取文章评论列表
     */
    @Operation(summary = "获取文章评论列表")
    @GetMapping("/post/{postId}")
    public Result<IPage<CommentVO>> getCommentsByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        IPage<CommentVO> comments = blogCommentService.getCommentsByPostId(postId, pageNum, pageSize);
        return Result.success(comments);
    }

    /**
     * 获取单个评论详情
     */
    @Operation(summary = "获取单个评论详情")
    @GetMapping("/{commentId}")
    public Result<CommentVO> getCommentById(@PathVariable Long commentId) {
        CommentVO comment = blogCommentService.getCommentById(commentId);
        return Result.success(comment);
    }

    /**
     * 删除评论
     */
    @RequiresAuth
    @Operation(summary = "删除评论")
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        blogCommentService.deleteComment(commentId, userId);
        return Result.success(null);
    }

    /**
     * 获取我的评论列表
     */
    @RequiresAuth
    @Operation(summary = "获取我的评论列表")
    @GetMapping("/my")
    public Result<IPage<CommentWithPostVO>> getMyComments(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<CommentWithPostVO> result = blogCommentService.getMyComments(userId, page, pageSize);
        return Result.success(result);
    }
}
