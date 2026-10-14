package com.selfblog.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建话题请求DTO
 */
@Schema(description = "创建话题请求")
@Data
public class TopicCreateRequest {

    @Schema(description = "话题名称", example = "Java")
    @NotBlank(message = "话题名称不能为空")
    @Size(max = 50, message = "话题名称不能超过50字符")
    private String name;

    @Schema(description = "话题描述", example = "分享Java相关的技术内容")
    @Size(max = 500, message = "话题描述不能超过500字符")
    private String description;
}