package com.selfblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.controller.social.NotificationController;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.social.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NotificationController 单元测试
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Long userId) {
        UserContext ctx = new UserContext(userId, "user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList()));
    }

    @Test
    @DisplayName("获取通知列表")
    void getNotificationList_Success() throws Exception {
        setUpSecurityContext(1L);
        Page<NotificationVO> page = new Page<>(1, 20, 0);
        when(notificationService.getNotificationList(anyInt(), anyInt(), eq(1L))).thenReturn(page);

        mockMvc.perform(get("/notification/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取通知列表 - 未登录返回401")
    void getNotificationList_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/notification/list"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("获取未读通知数量")
    void getUnreadCount_Success() throws Exception {
        setUpSecurityContext(1L);
        when(notificationService.getUnreadCount(eq(1L))).thenReturn(5L);

        mockMvc.perform(get("/notification/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("标记单条通知为已读")
    void markAsRead_Success() throws Exception {
        setUpSecurityContext(1L);
        doNothing().when(notificationService).markAsRead(eq(100L), eq(1L));

        mockMvc.perform(put("/notification/100/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("标记所有通知为已读")
    void markAllAsRead_Success() throws Exception {
        setUpSecurityContext(1L);
        doNothing().when(notificationService).markAllAsRead(eq(1L));

        mockMvc.perform(put("/notification/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除通知")
    void deleteNotification_Success() throws Exception {
        setUpSecurityContext(1L);
        doNothing().when(notificationService).deleteNotification(eq(100L), eq(1L));

        mockMvc.perform(delete("/notification/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
