package com.selfblog.dto.social;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 关注请求DTO
 */
@Data
@Schema(description = "关注请求")
public class FollowRequest {

    @NotNull(message = "目标用户ID不能为空")
    @Schema(description = "目标用户ID", requiredMode = RequiredMode.REQUIRED)
    private Long targetUserId;
}