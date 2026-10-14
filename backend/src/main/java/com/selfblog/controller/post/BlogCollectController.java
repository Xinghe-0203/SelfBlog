package com.selfblog.controller.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.service.social.BlogCollectService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.post.CollectItemVO;
import com.selfblog.vo.post.CollectResultVO;
import com.selfblog.vo.post.CollectStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏控制器
 */
@Tag(name = "收藏管理", description = "收藏相关接口")
@RestController
@RequestMapping("/collect")
@Validated
@RequiredArgsConstructor
public class BlogCollectController {

    private final BlogCollectService blogCollectService;

    /**
     * 收藏/取消收藏
     */
    @RequiresAuth
    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/{postId}")
    public Result<CollectResultVO> toggleCollect(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CollectResultVO result = blogCollectService.toggleCollect(postId, userId);
        return Result.success(result);
    }

    /**
     * 检查是否已收藏
     */
    @Operation(summary = "检查是否已收藏")
    @GetMapping("/{postId}/status")
    public Result<CollectStatusVO> checkCollectStatus(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        CollectStatusVO status = blogCollectService.checkCollectStatus(postId, userId);
        return Result.success(status);
    }

    /**
     * 获取我的收藏列表
     */
    @RequiresAuth
    @Operation(summary = "获取我的收藏列表")
    @GetMapping("/my")
    public Result<IPage<CollectItemVO>> getMyCollections(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<CollectItemVO> result = blogCollectService.getMyCollections(userId, page, pageSize);
        return Result.success(result);
    }

    /**
     * 批量检查收藏状态
     */
    @RequiresAuth
    @Operation(summary = "批量检查收藏状态")
    @PostMapping("/check/batch")
    public Result<List<Boolean>> checkCollectStatusBatch(@RequestBody @NotEmpty @Size(max = 50) List<Long> postIds) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Boolean> results = blogCollectService.checkCollectStatusBatch(postIds, userId);
        return Result.success(results);
    }
}
