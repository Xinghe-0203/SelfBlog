package com.selfblog.controller;

import com.selfblog.controller.content.ShareController;
import com.selfblog.service.content.ShareService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
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
 * ShareController 单元测试
 */
@WebMvcTest(ShareController.class)
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShareService shareService;

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
    @DisplayName("记录分享成功")
    void recordShare_Success() throws Exception {
        setUpSecurityContext(1L);
        doNothing().when(shareService).recordShare(eq(100L), eq(1L), eq("weibo"));

        mockMvc.perform(post("/share/100").param("platform", "weibo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("记录分享 - 未登录返回401")
    void recordShare_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(post("/share/100"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("获取分享数")
    void getShareCount_Success() throws Exception {
        when(shareService.getShareCount(100L)).thenReturn(5);

        mockMvc.perform(get("/share/count/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }
}
