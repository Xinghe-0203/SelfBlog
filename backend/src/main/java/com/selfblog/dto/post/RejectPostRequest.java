package com.selfblog.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 驳回文章请求 DTO
 */
@Data
@Schema(description = "驳回文章请求")
public class RejectPostRequest {

    @Schema(description = "驳回原因", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因不能超过500字符")
    private String reason;
}