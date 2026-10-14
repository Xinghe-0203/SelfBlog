package com.selfblog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.entity.BlogPostTag;
import com.selfblog.entity.BlogTag;
import com.selfblog.mapper.BlogPostTagMapper;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.utils.HtmlSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Tag(name = "管理员-标签管理", description = "管理员标签管理接口")
@RestController
@RequestMapping("/admin/tag")
@Validated
@RequiredArgsConstructor
public class AdminTagController {

    private final BlogTagService blogTagService;

    private final BlogPostTagMapper blogPostTagMapper;

    private final HtmlSanitizer htmlSanitizer;

    @Operation(summary = "获取标签列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<Map<String, Object>>> getTagList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        LambdaQueryWrapper<BlogTag> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(BlogTag::getName, keyword.trim());
        }
        wrapper.orderByDesc(BlogTag::getId);

        IPage<BlogTag> page = blogTagService.page(new Page<>(pageNum, pageSize), wrapper);

        IPage<Map<String, Object>> result = page.convert(tag -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", tag.getId());
            map.put("name", tag.getName());
            long postCount = blogPostTagMapper.selectCount(
                    new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getTagId, tag.getId()));
            map.put("postCount", postCount);
            return map;
        });

        return Result.success(result);
    }

    @Operation(summary = "创建标签")
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> createTag(@Validated @RequestBody CreateTagRequest request) {
        String sanitizedName = htmlSanitizer.sanitizePlainText(request.getName().trim());
        if (sanitizedName.length() > 20) {
            throw new BusinessException(400, "标签名称不能超过20个字符");
        }
        BlogTag tag = blogTagService.createTag(sanitizedName);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tag.getId());
        map.put("name", tag.getName());
        return Result.success(map);
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> updateTag(@PathVariable Long id,
                                                  @Validated @RequestBody UpdateTagRequest request) {
        BlogTag tag = blogTagService.getById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        String sanitizedName = htmlSanitizer.sanitizePlainText(request.getName().trim());
        if (sanitizedName.length() > 20) {
            throw new BusinessException(400, "标签名称不能超过20个字符");
        }
        tag.setName(sanitizedName);
        blogTagService.updateById(tag);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tag.getId());
        map.put("name", tag.getName());
        return Result.success(map);
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> deleteTag(@PathVariable Long id) {
        blogTagService.deleteTag(id);
        return Result.success(null);
    }

    @Validated
    public static class CreateTagRequest {
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 20, message = "标签名称不能超过20个字符")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Validated
    public static class UpdateTagRequest {
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 20, message = "标签名称不能超过20个字符")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
