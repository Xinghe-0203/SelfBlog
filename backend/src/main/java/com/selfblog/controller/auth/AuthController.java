package com.selfblog.controller.auth;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.auth.RegisterVerifyRequest;
import com.selfblog.dto.auth.SendRegisterCodeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册认证控制器
 */
@Tag(name = "注册认证", description = "个人博客不开放访客注册")
@RestController
@RequestMapping("/auth/register")
@Validated
public class AuthController {

    @Operation(summary = "发送注册验证码", description = "注册功能已关闭")
    @PostMapping("/send-code")
    public Result<Void> sendRegisterCode(@Valid @RequestBody SendRegisterCodeRequest request) {
        throw new BusinessException(403, "此博客不开放访客注册");
    }

    @Operation(summary = "验证注册验证码", description = "注册功能已关闭")
    @PostMapping("/verify")
    public Result<Void> verifyAndRegister(@Valid @RequestBody RegisterVerifyRequest request) {
        throw new BusinessException(403, "此博客不开放访客注册");
    }
}
