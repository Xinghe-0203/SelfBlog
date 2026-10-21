package com.selfblog.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.auth.ChangePasswordRequest;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.dto.user.UserProfileRequest;
import com.selfblog.dto.user.UserSearchRequest;
import com.selfblog.entity.SysUser;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.user.UserLoginResponse;
import com.selfblog.vo.user.UserVO;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/user")
@Validated
@Slf4j
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    private final JwtUtils jwtUtils;

    /**
     * 用户注册（个人博客仅作者登录）
     */
    @Operation(summary = "用户注册", description = "个人博客不开放访客注册")
    @PostMapping("/register")
    public Result<Void> register() {
        throw new BusinessException(403, "此博客不开放访客注册");
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = sysUserService.login(request);
        return Result.success(response);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtils.getUserIdFromToken(token);
                if (userId != null) {
                    jwtUtils.logoutDevice(userId, token);
                } else {
                    jwtUtils.revokeToken(token);
                }
            }
            return Result.success("登出成功", null);
        } catch (JwtException e) {
            log.warn("登出失败: 无效的Token, error={}", e.getMessage());
            throw new BusinessException(401, "无效的Token");
        }
    }

    /**
     * 登出其他所有设备（保留当前设备）
     */
    @Operation(summary = "登出其他设备", description = "登出除当前设备外的所有设备")
    @PostMapping("/logout-other-devices")
    public Result<Void> logoutOtherDevices(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new BusinessException(400, "无效的Token");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(401, "无效的Token");
            }
            jwtUtils.logoutOtherDevices(userId, token);
            return Result.success("其他设备已登出", null);
        } catch (JwtException e) {
            throw new BusinessException(401, "无效的Token");
        }
    }

    /**
     * 获取当前用户设备数量
     */
    @Operation(summary = "获取设备数量", description = "获取当前用户登录的设备数量")
    @GetMapping("/devices")
    public Result<Integer> getDeviceCount(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new BusinessException(400, "无效的Token");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(401, "无效的Token");
            }
            int count = jwtUtils.getUserDeviceCount(userId);
            return Result.success(count);
        } catch (JwtException e) {
            throw new BusinessException(401, "无效的Token");
        }
    }

    /**
     * 根据ID查询用户
     */
    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        UserVO userVO = sysUserService.getUserById(id);
        if (userVO == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 仅用户本人或管理员可见敏感信息（邮箱、角色）
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        boolean isOwner = currentUserId != null && currentUserId.equals(id);
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        if (!isOwner && !isAdmin) {
            userVO.setEmail(null);
            userVO.setRole(null);
        }

        return Result.success(userVO);
    }

    /**
     * 刷新Token
     */
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<UserLoginResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new BusinessException(400, "无效的Token");
            }
            String refreshToken = authHeader.substring(7);

            // 验证刷新Token
            if (!jwtUtils.isRefreshToken(refreshToken)) {
                throw new BusinessException(401, "无效的刷新Token");
            }
            if (jwtUtils.isTokenExpired(refreshToken) || jwtUtils.isTokenRevoked(refreshToken)) {
                throw new BusinessException(401, "刷新Token已过期或已撤销");
            }

            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            String username = jwtUtils.getUsernameFromToken(refreshToken);
            String role = jwtUtils.getRoleFromToken(refreshToken);

            SysUser currentUser = sysUserService.getById(userId);
            if (currentUser == null) {
                throw new BusinessException(401, "用户不存在或已注销");
            }
            if (currentUser.getIsDeleted() != null && currentUser.getIsDeleted() == 1) {
                throw new BusinessException(401, "用户不存在或已注销");
            }
            if (currentUser.getStatus() != null && currentUser.getStatus() == 0) {
                throw new BusinessException(403, "账号已被禁用");
            }
            if (currentUser.getLockUntil() != null
                    && currentUser.getLockUntil().isAfter(java.time.LocalDateTime.now())) {
                throw new BusinessException(403, "账号已被锁定");
            }

            // 必须继承已验证的旧版本，不能因检查后的并发改密而签发新密码版本的令牌。
            String credentialVersion = jwtUtils.getCredentialVersionFromToken(refreshToken);
            jwtUtils.revokeToken(refreshToken);

            String newToken = jwtUtils.generateToken(userId, username, role, credentialVersion);
            String newRefreshToken = jwtUtils.generateRefreshToken(userId, username, role, credentialVersion);
            jwtUtils.registerDeviceToken(userId, newToken);

            UserLoginResponse response = new UserLoginResponse();
            response.setToken(newToken);
            response.setRefreshToken(newRefreshToken);
            return Result.success(response);
        } catch (JwtException e) {
            throw new BusinessException(401, "无效的Token");
        }
    }

    /**
     * 修改密码
     */
    @RequiresAuth
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        sysUserService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    /**
     * 搜索用户
     */
    @Operation(summary = "搜索用户")
    @GetMapping("/search")
    public Result<IPage<UserVO>> searchUsers(@Valid UserSearchRequest request) {
        IPage<UserVO> result = sysUserService.searchUsers(request);
        return Result.success(result);
    }

    /**
     * 修改用户资料
     */
    @RequiresAuth
    @Operation(summary = "修改用户资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        sysUserService.updateUserProfile(userId, request.getNickname(), request.getBio(), request.getEmail());
        return Result.success("资料修改成功", null);
    }

    /**
     * 修改头像
     */
    @Value("${avatar.allowed-domains:localhost,127.0.0.1}")
    private String allowedAvatarDomainsConfig;

    @RequiresAuth
    @Operation(summary = "修改头像")
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestParam String avatar) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (avatar == null || avatar.trim().isEmpty()) {
            throw new BusinessException(400, "头像URL不能为空");
        }
        validateImageUrl(avatar, "头像URL");
        sysUserService.updateAvatar(userId, avatar);
        return Result.success("头像修改成功", null);
    }

    /**
     * 修改封面图
     */
    @RequiresAuth
    @Operation(summary = "修改封面图")
    @PutMapping("/cover-image")
    public Result<Void> updateCoverImage(@RequestParam String coverImage) {
        Long userId = SecurityUtils.getCurrentUserId();
        validateImageUrl(coverImage, "封面图URL");
        sysUserService.updateCoverImage(userId, coverImage);
        return Result.success("封面图修改成功", null);
    }

    private void validateImageUrl(String url, String fieldName) {
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException(400, fieldName + "不能为空");
        }
        if (url.length() > 2048) {
            throw new BusinessException(400, fieldName + "长度不能超过2048字符");
        }
        // 允许相对路径（本地上传）和绝对URL（CDN）
        if (url.startsWith("/")) {
            // S13：只接受站内绝对路径。「//evil.com/x」是协议相对 URL，浏览器会把它当作
            // 指向 evil.com 的外部地址；「/\evil.com」在部分解析器里等价。两者都必须拒绝，
            // 否则前面的 http(s) 校验与域名白名单可以被完全绕过。
            if (url.startsWith("//") || url.startsWith("/\\") || url.startsWith("/%2F") || url.startsWith("/%2f")) {
                throw new BusinessException(400, fieldName + "格式无效");
            }
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BusinessException(400, fieldName + "格式无效");
        }
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost().toLowerCase();
            List<String> allowedDomains = Arrays.asList(allowedAvatarDomainsConfig.split(","));
            boolean allowed = allowedDomains.stream().anyMatch(domain ->
                    host.equals(domain.trim()) || host.endsWith("." + domain.trim()));
            if (!allowed) {
                throw new BusinessException(400, fieldName + "域名不在允许范围内");
            }
        } catch (java.net.MalformedURLException e) {
            throw new BusinessException(400, fieldName + "格式无效");
        }
    }
}
