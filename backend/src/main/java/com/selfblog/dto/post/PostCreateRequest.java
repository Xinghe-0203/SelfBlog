package com.selfblog.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章创建/更新请求 DTO
 */
@Data
@Schema(description = "文章创建/更新请求")
public class PostCreateRequest {

    @Schema(description = "文章ID（更新时需要）")
    private Long id;

    @Schema(description = "文章标题", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过200字符")
    private String title;

    @Schema(description = "文章摘要")
    @Size(max = 500, message = "文章摘要不能超过500字符")
    private String summary;

    @Schema(description = "文章内容（Markdown）", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "文章内容不能为空")
    @Size(max = 300000, message = "文章内容不能超过30万字符")
    private String content;

    @Schema(description = "文章分类")
    @Size(max = 50, message = "文章分类不能超过50字符")
    private String category;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "标签名称列表")
    private List<String> tagNames;

    @Schema(description = "话题ID数组")
    private List<Long> topicIds;

    @Schema(description = "封面图片URL")
    @Size(max = 500, message = "封面图片地址不能超过500字符")
    private String coverImage;
}
