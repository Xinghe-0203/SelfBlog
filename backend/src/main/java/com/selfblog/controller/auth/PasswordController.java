package com.selfblog.controller.auth;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.auth.ResetPasswordRequest;
import com.selfblog.dto.auth.SendCodeRequest;
import com.selfblog.service.auth.EmailService;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.StringMaskUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 密码找回控制器
 */
@Slf4j
@Tag(name = "密码找回", description = "密码找回相关接口")
@RestController
@RequestMapping("/auth/password")
@Validated
@RequiredArgsConstructor
public class PasswordController {

    private final EmailService emailService;

    private final SysUserService sysUserService;

    /**
     * 发送验证码到邮箱
     */
    @Operation(summary = "发送验证码", description = "发送密码找回验证码到指定邮箱")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        // 防邮箱枚举（S3）：无论邮箱是否已注册，都返回同一个成功信封。
        // 只有已注册的地址才真的发信，所以调用方无法用状态码/文案区分二者。
        if (emailService.isEmailRegistered(request.getEmail())) {
            emailService.sendVerificationCode(request.getEmail(), EmailService.VerificationType.PASSWORD_RESET);
        } else {
            log.info("密码找回请求：邮箱未注册，按统一信封静默返回: {}", StringMaskUtils.maskEmail(request.getEmail()));
        }
        return Result.success("如果该邮箱已注册，验证码将发送到该邮箱", null);
    }

    /**
     * 验证验证码并重置密码
     */
    @Operation(summary = "重置密码", description = "验证验证码后重置密码")
    @PutMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // 验证验证码
        if (!emailService.verifyCode(request.getEmail(), request.getCode(), EmailService.VerificationType.PASSWORD_RESET)) {
            throw new BusinessException(400, "验证码验证失败");
        }

        // 重置密码
        sysUserService.resetPassword(request.getEmail(), request.getNewPassword());

        log.info("密码重置成功: {}", StringMaskUtils.maskEmail(request.getEmail()));
        return Result.success("密码重置成功", null);
    }
}