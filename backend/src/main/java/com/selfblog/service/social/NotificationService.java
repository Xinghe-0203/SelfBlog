package com.selfblog.service.social;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.BlogNotification;
import com.selfblog.vo.social.NotificationVO;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<BlogNotification> {

    /**
     * 获取当前用户的通知列表（分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param userId 当前用户ID
     * @return 分页通知列表
     */
    Page<NotificationVO> getNotificationList(Integer pageNum, Integer pageSize, Long userId);

    /**
     * 获取当前用户的未读通知数量
     * @param userId 当前用户ID
     * @return 未读数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 标记单条通知为已读
     * @param notificationId 通知ID
     * @param userId 当前用户ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 标记所有通知为已读
     * @param userId 当前用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 删除通知
     * @param notificationId 通知ID
     * @param userId 当前用户ID
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * 发送通知
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param fromUserId 发送者ID
     * @param toUserId 接收者ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     */
    void sendNotification(String type, String title, String content, Long fromUserId, Long toUserId, String targetType, Long targetId);
}
