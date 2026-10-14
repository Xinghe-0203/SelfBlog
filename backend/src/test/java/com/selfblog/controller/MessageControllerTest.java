package com.selfblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.controller.social.MessageController;
import com.selfblog.service.social.MessageService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.social.MessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MessageController 单元测试
 */
@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

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
    @DisplayName("发送私信成功")
    void sendMessage_Success() throws Exception {
        setUpSecurityContext(1L);
        MessageVO vo = new MessageVO();
        vo.setId(100L);
        vo.setContent("你好");
        when(messageService.sendMessage(eq(1L), eq(2L), eq("你好"))).thenReturn(vo);

        mockMvc.perform(post("/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":2,\"content\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("发送私信 - 未登录返回401")
    void sendMessage_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(post("/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":2,\"content\":\"你好\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("获取收到的私信列表")
    void getReceivedMessages_Success() throws Exception {
        setUpSecurityContext(1L);
        Page<MessageVO> page = new Page<>(1, 20, 0);
        when(messageService.getReceivedMessages(anyInt(), anyInt(), eq(1L))).thenReturn(page);

        mockMvc.perform(get("/message/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取发送的私信列表")
    void getSentMessages_Success() throws Exception {
        setUpSecurityContext(1L);
        Page<MessageVO> page = new Page<>(1, 20, 0);
        when(messageService.getSentMessages(anyInt(), anyInt(), eq(1L))).thenReturn(page);

        mockMvc.perform(get("/message/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("标记私信为已读")
    void markAsRead_Success() throws Exception {
        setUpSecurityContext(1L);
        doNothing().when(messageService).markAsRead(eq(100L), eq(1L));

        mockMvc.perform(put("/message/100/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除私信")
    void deleteMessage_Success() throws Exception {
        setUpSecurityContext(1L);
        doNothing().when(messageService).deleteMessage(eq(100L), eq(1L));

        mockMvc.perform(delete("/message/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取未读私信数量")
    void getUnreadCount_Success() throws Exception {
        setUpSecurityContext(1L);
        when(messageService.getUnreadCount(eq(1L))).thenReturn(3L);

        mockMvc.perform(get("/message/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("获取会话列表")
    void getConversations_Success() throws Exception {
        setUpSecurityContext(1L);
        when(messageService.getConversations(eq(1L))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/message/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取会话消息")
    void getConversationMessages_Success() throws Exception {
        setUpSecurityContext(1L);
        Page<MessageVO> page = new Page<>(1, 20, 0);
        when(messageService.getConversationMessages(eq(1L), eq(2L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/message/conversation/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
