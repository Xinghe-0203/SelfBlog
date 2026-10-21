package com.selfblog.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.BlogNotificationMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.NotificationService;
import com.selfblog.vo.social.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：通知列表缓存必须在发送通知后失效。
 *
 * <p>修复前 {@code getNotificationList} 与未读计数共用 userCache（键 {@code notifList:...}），
 * 而所有 {@code @CacheEvict} 只清 {@code unreadNotif:} 前缀，于是 notifList 条目永远不会失效：
 * 用户收到新通知后刷新列表仍看不到它（最长要等 10 分钟 TTL）。</p>
 *
 * <p>修复后通知列表有独立的具名缓存 notificationListCache，发送/标记已读/删除时整体失效。
 * 本测试先填充缓存，再发送通知，最后断言列表**包含**新通知——只看返回值不够，
 * 关键是"发送后必须重查"。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("通知列表缓存失效")
class NotificationListCacheTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BlogNotificationMapper blogNotificationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CacheManager cacheManager;

    private SysUser recipient;
    private SysUser sender;

    @BeforeEach
    void setUp() {
        blogNotificationMapper.delete(null);
        sysUserMapper.delete(null);
        SecurityContextHolder.clearContext();

        recipient = new SysUser();
        recipient.setUsername("notif-cache-recipient");
        recipient.setPassword("password");
        recipient.setNickname("Recipient");
        recipient.setRole("user");
        recipient.setStatus(1);
        sysUserMapper.insert(recipient);

        sender = new SysUser();
        sender.setUsername("notif-cache-sender");
        sender.setPassword("password");
        sender.setNickname("Sender");
        sender.setRole("user");
        sender.setStatus(1);
        sysUserMapper.insert(sender);

        Cache cache = cacheManager.getCache(CaffeineCacheConfig.NOTIFICATION_LIST_CACHE);
        assertNotNull(cache, "测试 profile 下必须存在通知列表缓存，否则本用例无意义");
        cache.clear();
    }

    @Test
    @DisplayName("发送通知后，已缓存的列表必须能查到新通知")
    void sendingANotificationInvalidatesTheCachedList() {
        Long recipientId = recipient.getId();

        // 1) 先查一次列表，把（空）结果写进通知列表缓存。
        Page<NotificationVO> before = notificationService.getNotificationList(1, 10, recipientId);
        assertEquals(0, before.getTotal(), "初始应无通知");

        // 2) 发送一条通知：这一步必须清掉通知列表缓存。
        notificationService.sendNotification("like", "有人点赞了你的文章", "内容",
                sender.getId(), recipientId, "post", 12345L);

        // 3) 再次查询：若缓存未失效，这里会拿回第 1 步的空结果。
        Page<NotificationVO> after = notificationService.getNotificationList(1, 10, recipientId);
        assertEquals(1, after.getTotal(), "发送通知后列表必须重新查询并包含新通知");
        assertTrue(after.getRecords().stream()
                        .anyMatch(vo -> "有人点赞了你的文章".equals(vo.getTitle())),
                "新通知必须出现在列表中");
    }

    @Test
    @DisplayName("标记全部已读后，已缓存的列表状态必须刷新")
    void markingAllAsReadInvalidatesTheCachedList() {
        Long recipientId = recipient.getId();
        notificationService.sendNotification("like", "待读通知", "内容",
                sender.getId(), recipientId, "post", 1L);

        Page<NotificationVO> first = notificationService.getNotificationList(1, 10, recipientId);
        assertEquals(1, first.getTotal());
        List<NotificationVO> records = first.getRecords();
        assertEquals(0, records.get(0).getIsRead(), "初始应为未读");

        notificationService.markAllAsRead(recipientId);

        Page<NotificationVO> second = notificationService.getNotificationList(1, 10, recipientId);
        assertEquals(1, second.getRecords().get(0).getIsRead(), "标记全部已读后列表必须反映新状态");
    }
}
