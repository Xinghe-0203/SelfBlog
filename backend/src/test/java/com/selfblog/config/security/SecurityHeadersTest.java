package com.selfblog.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * S9 回归：全局安全响应头。
 *
 * <p>修复前 {@code SecurityConfig} 只设置了 frameOptions / referrerPolicy / permissionsPolicy，
 * 既没有 HSTS 也没有显式的 {@code X-Content-Type-Options: nosniff}。</p>
 *
 * <p>这里刻意**只**验证 HSTS 与 nosniff，并额外断言不存在全局 CSP：Vue SPA 依赖内联样式
 * 与构建期注入的脚本，加全局 CSP 会直接白屏，所以本任务的 S9 是「部分」实现。
 * 站点资源路径的严格 CSP 由 {@code SiteAssetSecurityHeaderInterceptor} 单独下发
 * （见 {@code SiteAssetSecurityHeaderTest}）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("S9：全局安全响应头")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("HTTPS 请求带 HSTS（一年 + includeSubDomains）")
    void secureRequestCarriesStrictTransportSecurity() throws Exception {
        // HSTS 只应出现在安全请求上（Spring Security 默认语义）：MockMvc 默认是明文请求，
        // 因此这里显式把请求标记为 secure，模拟真实 HTTPS 入口。
        mvc.perform(secure(get("/feed.xml")))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000 ; includeSubDomains"));
    }

    @Test
    @DisplayName("所有响应都带 X-Content-Type-Options: nosniff")
    void everyResponseCarriesContentTypeOptions() throws Exception {
        mvc.perform(get("/feed.xml"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("不引入全局 CSP：SPA 依赖内联样式与构建期脚本，全局 CSP 会白屏")
    void globalContentSecurityPolicyIsDeliberatelyAbsent() throws Exception {
        mvc.perform(get("/feed.xml"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    private static MockHttpServletRequestBuilder secure(MockHttpServletRequestBuilder builder) {
        return builder.secure(true);
    }
}
