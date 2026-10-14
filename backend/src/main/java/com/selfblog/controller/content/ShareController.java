package com.selfblog.controller.content;

import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.service.content.ShareService;
import com.selfblog.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 分享控制器
 */
@Tag(name = "分享管理", description = "分享相关接口")
@RestController
@RequestMapping("/share")
@Validated
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /**
     * 记录分享
     */
    @RequiresAuth
    @Operation(summary = "记录分享")
    @PostMapping("/{postId}")
    public Result<Void> recordShare(
            @PathVariable Long postId,
            @RequestParam(required = false, defaultValue = "web") String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        shareService.recordShare(postId, userId, platform);
        return Result.success(null);
    }

    /**
     * 获取分享数
     */
    @Operation(summary = "获取分享数")
    @GetMapping("/count/{postId}")
    public Result<Integer> getShareCount(@PathVariable Long postId) {
        if (postId == null) {
            throw new BusinessException(400, "文章ID不能为空");
        }
        int count = shareService.getShareCount(postId);
        return Result.success(count);
    }
}