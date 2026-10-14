package com.selfblog.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.user.AdminUserQueryRequest;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.user.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理控制器
 */
@Slf4j
@Tag(name = "管理员-用户管理", description = "管理员用户管理接口")
@RestController
@RequestMapping("/admin/user")
@Validated
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserService sysUserService;

    /**
     * 获取用户列表（管理员）
     */
    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<AdminUserVO>> getUserList(@Valid AdminUserQueryRequest request) {
        IPage<AdminUserVO> result = sysUserService.getAdminUserList(request);
        return Result.success(result);
    }

    /**
     * 修改用户状态（封禁/解封）
     */
    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        sysUserService.updateUserStatus(id, request.getStatus(), adminId);

        return Result.success(null);
    }

    /**
     * 重置用户密码（管理员）
     * 注意：生成的临时密码会返回给管理员，应由管理员通过安全渠道转交给用户
     * 建议用户登录后立即修改密码
     */
    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('admin')")
    public Result<String> resetUserPassword(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        if (id.equals(adminId)) {
            throw new BusinessException(400, "不能重置自己的密码");
        }
        String newPassword = sysUserService.resetPassword(id);
        return Result.success(newPassword);
    }

    /**
     * 封禁/解封用户
     */
    @Operation(summary = "封禁/解封用户")
    @PutMapping("/{id}/ban")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> banUser(
            @PathVariable Long id,
            @Valid @RequestBody BanRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        if (id.equals(adminId)) {
            throw new BusinessException(400, "不能封禁/解封自己的账号");
        }
        sysUserService.banUser(id, request.getBan());

        log.info("管理员{}用户: adminId={}, targetUserId={}, ban={}",
                request.getBan() ? "封禁" : "解封", adminId, id, request.getBan());

        return Result.success(null);
    }

    /**
     * 状态修改请求
     */
    @Validated
    public static class StatusRequest {
        @jakarta.validation.constraints.NotNull(message = "状态不能为空")
        @jakarta.validation.constraints.Min(value = 0, message = "状态值无效")
        @jakarta.validation.constraints.Max(value = 1, message = "状态值无效")
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * 封禁/解封用户请求
     */
    @Validated
    public static class BanRequest {
        @jakarta.validation.constraints.NotNull(message = "ban参数不能为空")
        private Boolean ban;

        public Boolean getBan() {
            return ban;
        }

        public void setBan(Boolean ban) {
            this.ban = ban;
        }
    }
}