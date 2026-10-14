package com.selfblog.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.result.Result;
import com.selfblog.dto.content.PageRequest;
import com.selfblog.service.social.BlogCommentService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.post.CommentWithPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员评论管理控制器
 */
@Slf4j
@Tag(name = "管理员-评论管理", description = "管理员评论管理接口")
@RestController
@RequestMapping("/admin/comment")
@Validated
@RequiredArgsConstructor
public class AdminCommentController {

    private final BlogCommentService blogCommentService;

    @Operation(summary = "获取评论列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<CommentWithPostVO>> getCommentList(@Valid PageRequest request) {
        IPage<CommentWithPostVO> result = blogCommentService.getAllComments(request.getPageNum(), request.getPageSize());
        return Result.success(result);
    }

    /**
     * 管理员删除评论（级联删除子评论）
     */
    @Operation(summary = "管理员删除评论")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        blogCommentService.adminDeleteComment(id, adminId);
        return Result.success(null);
    }
}
