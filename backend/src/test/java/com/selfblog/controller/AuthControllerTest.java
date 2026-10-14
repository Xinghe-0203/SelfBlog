package com.selfblog.controller;

import com.selfblog.controller.auth.AuthController;
import com.selfblog.service.auth.EmailService;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 单元测试
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SysUserService sysUserService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("sendRegisterCode_RegistrationDisabled_Returns403")
    void sendRegisterCode_RegistrationDisabled_Returns403() throws Exception {
        mockMvc.perform(post("/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"username\":\"testuser\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("verifyAndRegister_RegistrationDisabled_Returns403")
    void verifyAndRegister_RegistrationDisabled_Returns403() throws Exception {
        mockMvc.perform(post("/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"code\":\"123456\",\"username\":\"testuser\",\"password\":\"Test1234!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
