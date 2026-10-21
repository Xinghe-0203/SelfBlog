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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @DisplayName("S3：已注册邮箱收到验证码，返回统一成功信封")
    void sendCode_RegisteredEmail_SendsMailWithNeutralEnvelope() throws Exception {
        when(emailService.isEmailRegistered("test@example.com")).thenReturn(true);

        mockMvc.perform(post("/auth/password/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(emailService).sendVerificationCode(eq("test@example.com"), any());
    }

    @Test
    @DisplayName("S3：未知邮箱与已注册邮箱的状态码与响应体完全一致，且未知邮箱不发信")
    void sendCode_UnknownAndKnownEmailsReturnIdenticalEnvelope() throws Exception {
        String unknownBody = "{\"email\":\"unknown@example.com\"}";
        String knownBody = "{\"email\":\"known@example.com\"}";
        when(emailService.isEmailRegistered("unknown@example.com")).thenReturn(false);
        when(emailService.isEmailRegistered("known@example.com")).thenReturn(true);

        var unknown = mockMvc.perform(post("/auth/password/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownBody))
                .andReturn().getResponse();

        var known = mockMvc.perform(post("/auth/password/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(knownBody))
                .andReturn().getResponse();

        // 邮箱枚举的判据就是「状态码 + 响应体」不可区分，因此两者逐字节比较。
        // timestamp 每次请求都会变（是信封自带的当前时间），比较前先归一化掉。
        assertEquals(known.getStatus(), unknown.getStatus());
        assertEquals(normalizeTimestamp(known.getContentAsString()),
                normalizeTimestamp(unknown.getContentAsString()));
        assertTrue(known.getContentAsString().contains("\"code\":200"));

        // 只有已注册地址真的发信，避免对未知地址做无用投递。
        verify(emailService, times(1)).sendVerificationCode(eq("known@example.com"), any());
        verify(emailService, never()).sendVerificationCode(eq("unknown@example.com"), any());
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

    /** 去掉信封里的 timestamp（每次请求必然不同），只留下可枚举的部分。 */
    private static String normalizeTimestamp(String body) {
        return body.replaceAll("\"timestamp\":\\d+", "\"timestamp\":0");
    }
}
