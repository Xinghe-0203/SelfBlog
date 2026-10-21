package com.selfblog.service.social.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.BlogNotification;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.BlogNotificationMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.utils.TimeUtils;
import com.selfblog.utils.UserConverter;
import com.selfblog.vo.social.NotificationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<BlogNotificationMapper, BlogNotification> implements NotificationService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Override
    @Cacheable(value = CaffeineCacheConfig.NOTIFICATION_LIST_CACHE, key = "'notifList:' + #userId + ':' + #pageNum + ':' + #pageSize")
    @Transactional(readOnly = true)
    public Page<NotificationVO> getNotificationList(Integer pageNum, Integer pageSize, Long userId) {
        Page<BlogNotification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BlogNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogNotification::getToUserId, userId)
               .orderByDesc(BlogNotification::getCreateTime);

        Page<BlogNotification> notificationPage = this.page(page, wrapper);

        Page<NotificationVO> voPage = new Page<>(notificationPage.getCurrent(), notificationPage.getSize(), notificationPage.getTotal());
        voPage.setRecords(convertToVOList(notificationPage.getRecords()));

        return voPage;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadNotif:' + #userId")
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<BlogNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogNotification::getToUserId, userId)
               .eq(BlogNotification::getIsRead, 0);
        return this.count(wrapper);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadNotif:' + #userId"),
            @CacheEvict(value = CaffeineCacheConfig.NOTIFICATION_LIST_CACHE, allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        BlogNotification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        // 允许管理员或通知接收者标记为已读
        if (!Objects.equals(notification.getToUserId(), userId) && !SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权操作此通知");
        }
        notification.setIsRead(1);
        this.updateById(notification);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadNotif:' + #userId"),
            @CacheEvict(value = CaffeineCacheConfig.NOTIFICATION_LIST_CACHE, allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<BlogNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogNotification::getToUserId, userId)
               .eq(BlogNotification::getIsRead, 0);

        BlogNotification update = new BlogNotification();
        update.setIsRead(1);
        this.update(update, wrapper);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadNotif:' + #userId"),
            @CacheEvict(value = CaffeineCacheConfig.NOTIFICATION_LIST_CACHE, allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        BlogNotification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        // 允许管理员删除任意通知，或通知所有者删除自己的通知
        if (!Objects.equals(notification.getToUserId(), userId) && !SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权删除此通知");
        }
        // 逻辑删除
        this.removeById(notificationId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadNotif:' + #toUserId"),
            @CacheEvict(value = CaffeineCacheConfig.NOTIFICATION_LIST_CACHE, allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(String type, String title, String content, Long fromUserId, Long toUserId, String targetType, Long targetId) {
        // 不通知自己
        if (fromUserId != null && fromUserId.equals(toUserId)) {
            return;
        }

        // 校验目标用户是否存在
        if (toUserId != null) {
            SysUser targetUser = sysUserMapper.selectById(toUserId);
            if (targetUser == null) {
                return; // 用户不存在，不发送通知
            }
        }

        BlogNotification notification = new BlogNotification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(htmlSanitizer.sanitizePlainText(content));
        notification.setFromUserId(fromUserId);
        notification.setToUserId(toUserId);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setIsRead(0);

        this.save(notification);
    }

    /**
     * 批量转换通知列表为VO（优化N+1：收集所有fromUserId后批量查询）
     */
    List<NotificationVO> convertToVOList(List<BlogNotification> notifications) {
        if (notifications.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有fromUserId
        Set<Long> fromUserIds = notifications.stream()
                .map(BlogNotification::getFromUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!fromUserIds.isEmpty()) {
            sysUserMapper.selectBatchIds(fromUserIds)
                    .forEach(user -> userMap.put(user.getId(), user));
        }

        // 转换VO
        return notifications.stream().map(notification -> {
            NotificationVO vo = new NotificationVO();
            BeanUtils.copyProperties(notification, vo);
            vo.setTimeAgo(TimeUtils.getTimeAgo(notification.getCreateTime()));

            // 使用Map匹配用户信息
            if (notification.getFromUserId() != null) {
                SysUser fromUser = userMap.get(notification.getFromUserId());
                if (fromUser != null) {
                    vo.setFromUser(UserConverter.toUserVO(fromUser));
                }
            }
            return vo;
        }).toList();
    }
}
