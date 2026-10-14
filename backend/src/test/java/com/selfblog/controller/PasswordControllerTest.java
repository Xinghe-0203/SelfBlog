package com.selfblog.controller;

import com.selfblog.controller.auth.PasswordController;
import com.selfblog.service.auth.EmailService;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PasswordController 单元测试
 */
@WebMvcTest(PasswordController.class)
class PasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SysUserService sysUserService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("发送验证码成功")
    void sendCode_Success() throws Exception {
        when(emailService.isEmailRegistered("test@example.com")).thenReturn(true);

        mockMvc.perform(post("/auth/password/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("发送验证码 - 邮箱未注册抛400")
    void sendCode_EmailNotRegistered_Returns400() throws Exception {
        when(emailService.isEmailRegistered("unknown@example.com")).thenReturn(false);

        mockMvc.perform(post("/auth/password/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().is(400));
    }

    @Test
    @DisplayName("重置密码成功")
    void resetPassword_Success() throws Exception {
        when(emailService.verifyCode(eq("test@example.com"), eq("123456"), any())).thenReturn(true);
        doNothing().when(sysUserService).resetPassword(anyString(), anyString());

        mockMvc.perform(put("/auth/password/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"code\":\"123456\",\"newPassword\":\"NewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("重置密码 - 验证码错误抛400")
    void resetPassword_InvalidCode_Returns400() throws Exception {
        when(emailService.verifyCode(eq("test@example.com"), eq("wrong"), any())).thenReturn(false);

        mockMvc.perform(put("/auth/password/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"code\":\"wrong\",\"newPassword\":\"NewPass123\"}"))
                .andExpect(status().is(400));
    }
}
