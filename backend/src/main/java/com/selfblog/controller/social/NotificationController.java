package com.selfblog.controller.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.social.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@Tag(name = "通知管理", description = "通知相关接口")
@RestController
@RequestMapping("/notification")
@Validated
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表（分页）
     */
    @RequiresAuth
    @Operation(summary = "获取通知列表")
    @GetMapping("/list")
    public Result<IPage<NotificationVO>> getNotificationList(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<NotificationVO> notificationPage = notificationService.getNotificationList(pageNum, pageSize, userId);
        return Result.success(notificationPage);
    }

    /**
     * 获取未读通知数量
     */
    @RequiresAuth
    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记单条通知为已读
     */
    @RequiresAuth
    @Operation(summary = "标记单条通知为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success(null);
    }

    /**
     * 标记所有通知为已读
     */
    @RequiresAuth
    @Operation(summary = "标记所有通知为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success(null);
    }

    /**
     * 删除通知
     */
    @RequiresAuth
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return Result.success(null);
    }
}
