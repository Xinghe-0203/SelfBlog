package com.selfblog.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户资料请求 DTO
 */
@Data
@Schema(description = "修改用户资料请求")
public class UserProfileRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 50, message = "昵称长度必须在1-50字符之间")
    @Schema(description = "昵称", requiredMode = RequiredMode.REQUIRED)
    private String nickname;

    @Size(max = 200, message = "个人简介不能超过200字符")
    @Schema(description = "个人简介")
    private String bio;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;
}