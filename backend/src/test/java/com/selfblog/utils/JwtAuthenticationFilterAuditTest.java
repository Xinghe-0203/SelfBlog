package com.selfblog.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.selfblog.config.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 管理员写操作审计测试（task-6 要求 E）。
 *
 * 回归背景：{@code SecurityConfig.logAdminOperation} 是死代码（全仓 0 调用点），
 * audit.log 里 {@code event=ADMIN_OPERATION} 计数为 0。现在审计落在
 * {@link JwtAuthenticationFilter} 中，位于鉴权链之后，因此 401/403 不会被误记成管理操作。
 *
 * 放在 utils 包下是因为本任务的可写测试目录只有 config/web 与 utils；
 * 被测类仍从 config.security 导入。
 */
class JwtAuthenticationFilterAuditTest {

    private JwtAuthenticationFilter filter;
    private JwtUtils jwtUtils;
    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger auditLogger;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.extractTokenFromRequest(any())).thenReturn(null);

        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);

        auditLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LogUtils.class);
        appender = new ListAppender<>();
        appender.start();
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        auditLogger.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsAdmin() {
        when(jwtUtils.extractTokenFromRequest(any())).thenReturn("token");
        when(jwtUtils.isRefreshToken("token")).thenReturn(false);
        when(jwtUtils.isTokenExpired("token")).thenReturn(false);
        when(jwtUtils.isTokenRevoked("token")).thenReturn(false);
        when(jwtUtils.getUserIdFromToken("token")).thenReturn(7L);
        when(jwtUtils.getUsernameFromToken("token")).thenReturn("admin");
        when(jwtUtils.getRoleFromToken("token")).thenReturn("admin");
    }

    private List<String> auditMessages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private void run(String method, String uri, String remoteAddr) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    @DisplayName("管理员写操作成功时写入 ADMIN_OPERATION 审计（含 method/uri/status/userId）")
    void adminWriteOperationIsAudited() throws Exception {
        authenticateAsAdmin();

        run("POST", "/api/admin/posts", "203.0.113.60");

        List<String> messages = auditMessages();
        assertTrue(messages.stream().anyMatch(m -> m.contains("event=ADMIN_OPERATION")),
                "管理写操作必须落审计，实际: " + messages);
        assertTrue(messages.stream().anyMatch(m -> m.contains("method=POST")
                        && m.contains("/api/admin/posts")
                        && m.contains("status=200")
                        && m.contains("result=success")
                        && m.contains("userId=7")),
                "审计内容必须带方法/路径/结果/操作者，实际: " + messages);
    }

    @Test
    @DisplayName("链中抛异常时记为 rejected，不因状态码仍是 200 而误判成功")
    void adminWriteOperationFailingWithExceptionIsRecordedAsRejected() throws Exception {
        authenticateAsAdmin();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/posts");
        request.setRemoteAddr("203.0.113.65");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failing = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, failing));

        assertTrue(auditMessages().stream().anyMatch(m -> m.contains("event=ADMIN_OPERATION")
                        && m.contains("result=rejected")),
                "异常必须记为 rejected，实际: " + auditMessages());
    }

    @Test
    @DisplayName("未通过鉴权的管理写操作记为 rejected，不被当成管理操作")
    void unauthenticatedAdminWriteIsRecordedAsRejected() throws Exception {
        // 无 token → SecurityContext 为空，即真实环境下的 401 路径。
        run("DELETE", "/api/admin/posts/1", "203.0.113.61");

        List<String> messages = auditMessages();
        assertTrue(messages.stream().anyMatch(m -> m.contains("event=ADMIN_OPERATION")
                        && m.contains("result=rejected")
                        && m.contains("userId=anonymous")),
                "未鉴权的写请求必须记为 rejected 且标 anonymous，实际: " + messages);
    }

    @Test
    @DisplayName("管理端只读请求不写审计，避免刷爆 audit.log")
    void adminReadRequestsAreNotAudited() throws Exception {
        authenticateAsAdmin();

        run("GET", "/api/admin/posts", "203.0.113.62");

        assertFalse(auditMessages().stream().anyMatch(m -> m.contains("event=ADMIN_OPERATION")),
                "GET 不应被审计，实际: " + auditMessages());
    }

    @Test
    @DisplayName("非管理端写请求不写审计")
    void nonAdminWriteRequestsAreNotAudited() throws Exception {
        authenticateAsAdmin();

        run("POST", "/api/post/create", "203.0.113.63");
        run("POST", "/api/guest-comments", "203.0.113.63");

        assertFalse(auditMessages().stream().anyMatch(m -> m.contains("event=ADMIN_OPERATION")),
                "只有 /api/admin/** 写操作需要审计，实际: " + auditMessages());
    }

    @Test
    @DisplayName("审计中的 clientIp 与限流同源：不可信对端伪造 XFF 不影响归因")
    void auditClientIpIgnoresForgedHeaderFromUntrustedPeer() throws Exception {
        authenticateAsAdmin();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/posts");
        request.setRemoteAddr("203.0.113.64");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(auditMessages().stream().anyMatch(m -> m.contains("clientIp=203.0.113.64")),
                "伪造的 XFF 不得污染审计 clientIp，实际: " + auditMessages());
        assertFalse(auditMessages().stream().anyMatch(m -> m.contains("clientIp=1.2.3.4")),
                "伪造 IP 不应出现在审计里，实际: " + auditMessages());
    }
}
