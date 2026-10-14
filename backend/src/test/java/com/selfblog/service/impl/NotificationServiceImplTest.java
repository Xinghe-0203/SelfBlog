package com.selfblog.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogNotification;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.BlogNotificationMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.social.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceImplTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BlogNotificationMapper blogNotificationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private SysUser testUser;
    private SysUser senderUser;

    @BeforeEach
    void setUp() {
        blogNotificationMapper.delete(null);
        sysUserMapper.delete(null);
        SecurityContextHolder.clearContext();

        testUser = new SysUser();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setNickname("Test User");
        testUser.setRole("user");
        testUser.setStatus(1);
        sysUserMapper.insert(testUser);

        senderUser = new SysUser();
        senderUser.setUsername("senderuser");
        senderUser.setPassword("password");
        senderUser.setNickname("Sender");
        senderUser.setRole("user");
        senderUser.setStatus(1);
        sysUserMapper.insert(senderUser);
    }

    private void setUpSecurityContext(Long userId, String role) {
        UserContext ctx = new UserContext(userId, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private BlogNotification createNotification(Long fromUserId, Long toUserId, String type, boolean isRead) {
        BlogNotification n = new BlogNotification();
        n.setType(type);
        n.setTitle("测试通知");
        n.setContent("测试内容");
        n.setFromUserId(fromUserId);
        n.setToUserId(toUserId);
        n.setTargetType("POST");
        n.setTargetId(1L);
        n.setIsRead(isRead ? 1 : 0);
        blogNotificationMapper.insert(n);
        return n;
    }

    @Test
    @DisplayName("发送通知成功")
    void sendNotification_Success() {
        notificationService.sendNotification("LIKE", "有人点赞了", "内容",
                senderUser.getId(), testUser.getId(), "POST", 1L);

        Page<NotificationVO> page = notificationService.getNotificationList(1, 10, testUser.getId());
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("不给自己发通知")
    void sendNotification_Self_NoOp() {
        notificationService.sendNotification("LIKE", "有人点赞了", "内容",
                testUser.getId(), testUser.getId(), "POST", 1L);

        Page<NotificationVO> page = notificationService.getNotificationList(1, 10, testUser.getId());
        assertEquals(0, page.getTotal());
    }

    @Test
    @DisplayName("发送通知给不存在的用户 - 静默忽略")
    void sendNotification_TargetNotFound_NoOp() {
        notificationService.sendNotification("LIKE", "有人点赞了", "内容",
                senderUser.getId(), 99999L, "POST", 1L);
        // 不抛异常
    }

    @Test
    @DisplayName("获取通知列表（分页）")
    void getNotificationList_Success() {
        createNotification(senderUser.getId(), testUser.getId(), "LIKE", false);
        createNotification(senderUser.getId(), testUser.getId(), "COMMENT", false);

        Page<NotificationVO> page = notificationService.getNotificationList(1, 10, testUser.getId());
        assertNotNull(page);
        assertEquals(2, page.getTotal());
        assertEquals(2, page.getRecords().size());
    }

    @Test
    @DisplayName("获取未读通知数量")
    void getUnreadCount_Success() {
        createNotification(senderUser.getId(), testUser.getId(), "LIKE", false);
        createNotification(senderUser.getId(), testUser.getId(), "COMMENT", false);
        createNotification(senderUser.getId(), testUser.getId(), "FOLLOW", true);

        Long count = notificationService.getUnreadCount(testUser.getId());
        assertEquals(2L, count);
    }

    @Test
    @DisplayName("标记单条通知为已读")
    void markAsRead_Success() {
        BlogNotification n = createNotification(senderUser.getId(), testUser.getId(), "LIKE", false);
        setUpSecurityContext(testUser.getId(), "user");

        notificationService.markAsRead(n.getId(), testUser.getId());

        BlogNotification updated = blogNotificationMapper.selectById(n.getId());
        assertEquals(1, updated.getIsRead());
    }

    @Test
    @DisplayName("标记不存在的通知为已读抛404")
    void markAsRead_NotFound_ThrowsException() {
        setUpSecurityContext(testUser.getId(), "user");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(99999L, testUser.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("标记他人通知为已读 - 非管理员抛403")
    void markAsRead_NotOwner_NotAdmin_ThrowsException() {
        BlogNotification n = createNotification(senderUser.getId(), testUser.getId(), "LIKE", false);
        setUpSecurityContext(999L, "user"); // 不是通知所有者，也不是管理员

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(n.getId(), 999L));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("标记所有通知为已读")
    void markAllAsRead_Success() {
        createNotification(senderUser.getId(), testUser.getId(), "LIKE", false);
        createNotification(senderUser.getId(), testUser.getId(), "COMMENT", false);
        createNotification(senderUser.getId(), testUser.getId(), "FOLLOW", false);

        assertEquals(3L, notificationService.getUnreadCount(testUser.getId()));

        notificationService.markAllAsRead(testUser.getId());

        assertEquals(0L, notificationService.getUnreadCount(testUser.getId()));
    }

    @Test
    @DisplayName("删除通知成功")
    void deleteNotification_Success() {
        BlogNotification n = createNotification(senderUser.getId(), testUser.getId(), "LIKE", false);
        setUpSecurityContext(testUser.getId(), "user");

        notificationService.deleteNotification(n.getId(), testUser.getId());

        BlogNotification deleted = blogNotificationMapper.selectById(n.getId());
        assertNull(deleted); // 逻辑删除后查不到
    }

    @Test
    @DisplayName("删除不存在的通知抛404")
    void deleteNotification_NotFound_ThrowsException() {
        setUpSecurityContext(testUser.getId(), "user");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.deleteNotification(99999L, testUser.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("删除他人通知 - 非管理员抛403")
    void deleteNotification_NotOwner_NotAdmin_ThrowsException() {
        BlogNotification n = createNotification(senderUser.getId(), testUser.getId(), "LIKE", false);
        setUpSecurityContext(999L, "user");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.deleteNotification(n.getId(), 999L));
        assertEquals(403, ex.getCode());
    }
}
