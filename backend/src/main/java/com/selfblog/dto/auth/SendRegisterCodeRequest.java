package com.selfblog.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送注册验证码请求 DTO
 */
@Data
@Schema(description = "发送注册验证码请求")
public class SendRegisterCodeRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 6, max = 20, message = "用户名长度必须在6-20个字符之间")
    @Schema(description = "用户名", requiredMode = RequiredMode.REQUIRED)
    private String username;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Schema(description = "邮箱", requiredMode = RequiredMode.REQUIRED)
    private String email;
}
