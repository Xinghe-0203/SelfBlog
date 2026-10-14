package com.selfblog.controller.post;

import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.content.TagCreateRequest;
import com.selfblog.entity.BlogTag;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标签管理控制器
 */
@Tag(name = "标签管理", description = "标签相关接口")
@RestController
@RequestMapping("/tag")
@Validated
@RequiredArgsConstructor
public class BlogTagController {

    private final BlogTagService blogTagService;

    private Map<String, Object> toTagMap(BlogTag tag) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tag.getId());
        map.put("name", tag.getName());
        return map;
    }

    /**
     * 获取所有标签列表
     */
    @Operation(summary = "获取所有标签")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listAllTags() {
        List<BlogTag> tags = blogTagService.listAllTags();
        List<Map<String, Object>> result = tags.stream().map(this::toTagMap).collect(Collectors.toList());
        return Result.success(result);
    }

    /**
     * 根据ID获取标签
     */
    @Operation(summary = "根据ID获取标签")
    @GetMapping("/{tagId}")
    public Result<Map<String, Object>> getTagById(@PathVariable Long tagId) {
        BlogTag tag = blogTagService.getById(tagId);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        return Result.success(toTagMap(tag));
    }

    /**
     * 创建标签
     */
    @RequiresAuth
    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Map<String, Object>> createTag(@Valid @RequestBody TagCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        BlogTag tag = blogTagService.createTag(request.getName());
        return Result.success(toTagMap(tag));
    }

    /**
     * 删除标签（仅管理员可操作）
     */
    @RequiresAuth
    @Operation(summary = "删除标签")
    @DeleteMapping("/{tagId}")
    public Result<Void> deleteTag(@PathVariable Long tagId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "仅管理员可执行此操作");
        }
        blogTagService.deleteTag(tagId);
        return Result.success(null);
    }

    /**
     * 搜索标签
     */
    @Operation(summary = "搜索标签")
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchTags(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }
        List<BlogTag> tags = blogTagService.searchTags(keyword);
        List<Map<String, Object>> result = tags.stream()
            .map(tag -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", tag.getId());
                map.put("name", tag.getName());
                return map;
            })
            .collect(Collectors.toList());
        return Result.success(result);
    }
}