package com.selfblog.controller.content;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.result.Result;
import com.selfblog.service.content.TrendingService;
import com.selfblog.vo.content.HotContentVO;
import com.selfblog.vo.content.HotPostVO;
import com.selfblog.vo.content.HotTagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 趋势/热门内容控制器
 */
@Tag(name = "趋势/热门", description = "热门文章和标签相关接口")
@RestController
@RequestMapping("/trending")
@Validated
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    /**
     * 获取热门文章列表（公开，支持分页）
     */
    @Operation(summary = "获取热门文章列表")
    @GetMapping("/posts")
    public Result<IPage<HotPostVO>> getHotPosts(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        IPage<HotPostVO> result = trendingService.getHotPosts(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取热门内容列表（文章+动态统一），按热度评分降序
     */
    @Operation(summary = "获取热门内容（文章+动态统一）")
    @GetMapping("/content")
    public Result<IPage<HotContentVO>> getHotContent(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        IPage<HotContentVO> result = trendingService.getHotContent(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取热门标签（公开）
     */
    @Operation(summary = "获取热门标签")
    @GetMapping("/hot-tags")
    public Result<IPage<HotTagVO>> getHotTags() {
        IPage<HotTagVO> result = trendingService.getHotTags();
        return Result.success(result);
    }

    /**
     * 更新单篇文章热度（手动触发，仅管理员可用）
     */
    @Operation(summary = "更新文章热度")
    @PutMapping("/{postId}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> updatePostTrending(@PathVariable Long postId) {
        trendingService.updatePostTrending(postId);
        return Result.success(null);
    }
}