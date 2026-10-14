package com.selfblog.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 保存草稿请求 DTO
 */
@Data
@Schema(description = "保存草稿请求")
public class SaveDraftRequest {

    @Schema(description = "草稿ID（更新时需要）")
    private Long draftId;

    @Schema(description = "草稿标题")
    @Size(max = 200, message = "文章标题不能超过200字符")
    private String title;

    @Schema(description = "文章摘要")
    @Size(max = 500, message = "文章摘要不能超过500字符")
    private String summary;

    @Schema(description = "文章内容（Markdown）")
    @Size(max = 300000, message = "文章内容不能超过30万字符")
    private String content;

    @Schema(description = "文章分类")
    private String category;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "封面图片URL")
    private String coverImage;

    @Schema(description = "标签名称列表")
    private List<String> tagNames;

    @Schema(description = "话题ID")
    private Long topicId;

    @Schema(description = "关联的文章ID（编辑已有文章时使用）")
    private Long postId;
}
