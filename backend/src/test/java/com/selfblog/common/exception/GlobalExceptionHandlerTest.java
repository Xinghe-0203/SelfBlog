package com.selfblog.common.exception;

import com.selfblog.common.result.Result;
import com.selfblog.config.web.RateLimitProperties;
import com.selfblog.utils.JwtUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 单元测试
 * 使用MockMvc测试异常处理
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@Import({GlobalExceptionHandlerTest.TestController.class, RateLimitProperties.class})
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUpMock() {
        when(jwtUtils.extractTokenFromRequest(any())).thenReturn("mock-jwt-token");
        when(jwtUtils.isRefreshToken(anyString())).thenReturn(false);
        when(jwtUtils.isTokenExpired(anyString())).thenReturn(false);
        when(jwtUtils.isTokenRevoked(anyString())).thenReturn(false);
        when(jwtUtils.getUsernameFromToken(anyString())).thenReturn("testuser");
        when(jwtUtils.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtUtils.getRoleFromToken(anyString())).thenReturn("user");
    }

    @TestConfiguration
    static class Config {
        @Bean
        public MethodValidationPostProcessor methodValidationPostProcessor() {
            return new MethodValidationPostProcessor();
        }
    }

    @RestController
    @Validated
    static class TestController {

        // BusinessException 测试
        @GetMapping("/test/business")
        public Result<String> testBusiness() {
            throw new BusinessException(400, "业务错误测试");
        }

        @GetMapping("/test/business/401")
        public Result<String> testBusiness401() {
            throw new BusinessException(401, "未授权访问");
        }

        @GetMapping("/test/business/403")
        public Result<String> testBusiness403() {
            throw new BusinessException(403, "权限不足");
        }

        @GetMapping("/test/business/404")
        public Result<String> testBusiness404() {
            throw new BusinessException(404, "资源不存在");
        }

        @GetMapping("/test/business/500")
        public Result<String> testBusiness500() {
            throw new BusinessException(500, "服务器内部错误");
        }

        // 参数校验测试（@RequestParam）
        @GetMapping("/test/validation")
        public Result<String> testValidation(@RequestParam @NotBlank @Size(min = 3) String param) {
            return Result.success(param);
        }

        // 参数校验测试（@RequestBody）
        @PostMapping("/test/validation/body")
        public Result<ValidRequest> testValidationBody(@Valid @RequestBody ValidRequest request) {
            return Result.success(request);
        }

        // 404测试
        @GetMapping("/test/notfound/{id}")
        public Result<String> testNotFound(@PathVariable Long id) throws NoHandlerFoundException {
            throw new NoHandlerFoundException("GET", "/test/notfound/" + id, null);
        }

        // 权限不足测试
        @GetMapping("/test/accessdenied")
        @WithMockUser(roles = "USER")
        public Result<String> testAccessDenied() {
            throw new AccessDeniedException("访问被拒绝");
        }

        // 认证失败测试
        @GetMapping("/test/auth")
        public Result<String> testAuth() {
            throw new AuthenticationException("认证失败") {};
        }

        // BadCredentials测试
        @GetMapping("/test/badcredentials")
        public Result<String> testBadCredentials() {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 数据完整性违反测试
        @GetMapping("/test/dataintegrity")
        public Result<String> testDataIntegrity() {
            throw new DataIntegrityViolationException("数据完整性违反");
        }

        // 通用异常测试
        @GetMapping("/test/runtime")
        public Result<String> testRuntime() {
            throw new RuntimeException("运行时异常");
        }

        // 空指针异常测试
        @GetMapping("/test/nullpointer")
        public Result<String> testNullPointer() {
            throw new NullPointerException("空指针异常");
        }

        // 非法参数异常测试
        @GetMapping("/test/illegalargument")
        public Result<String> testIllegalArgument(@RequestParam(required = false) String param) {
            if (param == null) {
                throw new IllegalArgumentException("参数不能为空");
            }
            return Result.success(param);
        }
    }

    // 用于@RequestBody验证的请求对象
    static class ValidRequest {
        @NotBlank(message = "内容不能为空")
        @Size(min = 3, max = 100, message = "内容长度必须在3-100之间")
        private String content;

        @NotNull(message = "数量不能为空")
        private Integer count;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    // ==================== BusinessException 测试 ====================

    @Test
    @DisplayName("处理业务异常 - 400错误")
    void testHandleBusinessException_400() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("业务错误测试"));
    }

    @Test
    @DisplayName("处理业务异常 - 401错误")
    void testHandleBusinessException_401() throws Exception {
        mockMvc.perform(get("/test/business/401"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未授权访问"));
    }

    @Test
    @DisplayName("处理业务异常 - 403错误")
    void testHandleBusinessException_403() throws Exception {
        mockMvc.perform(get("/test/business/403"))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限不足"));
    }

    @Test
    @DisplayName("处理业务异常 - 404错误")
    void testHandleBusinessException_404() throws Exception {
        mockMvc.perform(get("/test/business/404"))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    @DisplayName("处理业务异常 - 500错误")
    void testHandleBusinessException_500() throws Exception {
        mockMvc.perform(get("/test/business/500"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    // ==================== 参数校验异常测试 ====================

    @Test
    @DisplayName("处理参数校验异常 - @RequestParam空参数")
    void testHandleValidationException_RequestParam() throws Exception {
        // 传入空参数触发校验失败
        mockMvc.perform(get("/test/validation").param("param", ""))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("处理参数校验异常 - @RequestParam参数过短")
    void testHandleValidationException_RequestParamTooShort() throws Exception {
        // 传入过短的参数
        mockMvc.perform(get("/test/validation").param("param", "ab"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("处理参数校验异常 - @Valid + @RequestBody空内容")
    void testHandleValidationException_RequestBody() throws Exception {
        // 传入空的JSON对象
        mockMvc.perform(post("/test/validation/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("处理参数校验异常 - @Valid + @RequestBody部分字段为空")
    void testHandleValidationException_RequestBodyPartial() throws Exception {
        // 只提供content，缺少count
        mockMvc.perform(post("/test/validation/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test content\"}"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("处理参数校验异常 - @Valid + @RequestBody内容过短")
    void testHandleValidationException_RequestBodyTooShort() throws Exception {
        // content长度不足
        mockMvc.perform(post("/test/validation/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"ab\",\"count\":1}"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("处理参数校验异常 - 成功通过验证")
    void testHandleValidationException_Success() throws Exception {
        mockMvc.perform(get("/test/validation").param("param", "valid param"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("valid param"));
    }

    // ==================== 404资源不存在测试 ====================

    @Test
    @DisplayName("处理404资源不存在")
    void testHandleNotFound() throws Exception {
        mockMvc.perform(get("/test/notfound/999"))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    // ==================== 安全异常测试 ====================

    @Test
    @DisplayName("处理权限不足异常 - AccessDeniedException")
    void testHandleAccessDenied() throws Exception {
        mockMvc.perform(get("/test/accessdenied"))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限不足，拒绝访问"));
    }

    @Test
    @DisplayName("处理认证失败异常 - AuthenticationException")
    void testHandleAuthenticationException() throws Exception {
        mockMvc.perform(get("/test/auth"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("认证失败，请先登录"));
    }

    @Test
    @DisplayName("处理认证失败异常 - BadCredentialsException")
    void testHandleBadCredentialsException() throws Exception {
        mockMvc.perform(get("/test/badcredentials"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("认证失败，请先登录"));
    }

    // ==================== 数据相关异常测试 ====================

    @Test
    @DisplayName("处理数据完整性违反异常")
    void testHandleDataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/test/dataintegrity"))
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("数据完整性冲突"));
    }

    // ==================== 通用异常测试 ====================

    @Test
    @DisplayName("处理运行时异常 - RuntimeException")
    void testHandleRuntimeException() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    @Test
    @DisplayName("处理空指针异常")
    void testHandleNullPointerException() throws Exception {
        mockMvc.perform(get("/test/nullpointer"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    @Test
    @DisplayName("处理非法参数异常 - IllegalArgumentException")
    void testHandleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegalargument"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }
}
