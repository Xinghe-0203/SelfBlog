package com.selfblog.dto.circle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 校友圈评论请求 DTO
 */
@Data
@Schema(description = "校友圈评论请求")
public class CircleCommentRequest {

    @Schema(description = "动态ID")
    @NotNull(message = "动态ID不能为空")
    private Long postId;

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字符")
    private String content;

    @Schema(description = "父评论ID（用于回复功能）")
    private Long parentId;

    @Schema(description = "回复目标用户ID")
    private Long replyToUserId;
}