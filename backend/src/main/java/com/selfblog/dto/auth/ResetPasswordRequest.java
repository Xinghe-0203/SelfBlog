package com.selfblog.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求 DTO
 */
@Data
@Schema(description = "重置密码请求")
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", requiredMode = RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码", requiredMode = RequiredMode.REQUIRED, example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度必须在8-128个字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "密码必须包含大小写字母和数字")
    @Schema(description = "新密码", requiredMode = RequiredMode.REQUIRED)
    private String newPassword;
}