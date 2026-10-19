package com.selfblog.config.web;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.selfblog.utils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "rateLimitProperties", new RateLimitProperties());
    }

    @Test
    void commentCreationWithoutTrailingSlashIsLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertTrue(call("/api/guest-comments", "203.0.113.1", new MockHttpServletResponse()));
        }
        var response = new MockHttpServletResponse();
        assertFalse(call("/api/guest-comments", "203.0.113.1", response));
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":429"));
        assertTrue(call("/api/guest-comments", "203.0.113.2", new MockHttpServletResponse()));
    }

    @Test
    void trailingSlashAndChildPathsRemainLimited() throws Exception {
        for (String path : new String[]{"/api/guest-comments/", "/api/guest-comments/post/1"}) {
            for (int i = 0; i < 5; i++) {
                assertTrue(call(path, "203.0.113.3", new MockHttpServletResponse()));
            }
            assertFalse(call(path, "203.0.113.3", new MockHttpServletResponse()));
        }
    }

    @Test
    void prefixMustEndAtAPathSegmentBoundary() throws Exception {
        for (int i = 0; i < 35; i++) {
            assertTrue(call("/api/guest-comments-other", "203.0.113.4", new MockHttpServletResponse()));
        }
        for (int i = 0; i < 30; i++) {
            assertTrue(call("/api/user/login-other", "203.0.113.4", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/user/login-other", "203.0.113.4", new MockHttpServletResponse()));
    }

    @Test
    void longestRuleWinsRegardlessOfMapOrder() throws Exception {
        var properties = mock(RateLimitProperties.class);
        Map<String, Integer> rules = new LinkedHashMap<>();
        rules.put("/api/user/", 30);
        rules.put("/api/user/login", 2);
        when(properties.getAllLimits()).thenReturn(rules);
        ReflectionTestUtils.setField(interceptor, "rateLimitProperties", properties);
        assertTrue(call("/api/user/login", "203.0.113.5", new MockHttpServletResponse()));
        assertTrue(call("/api/user/login", "203.0.113.5", new MockHttpServletResponse()));
        assertFalse(call("/api/user/login", "203.0.113.5", new MockHttpServletResponse()));
    }

    @Test
    void encodedAndContextPrefixedUrisShareTheActualRouteQuota() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertTrue(call("/api/guest-comments", "203.0.113.7", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/%67uest-comments", "203.0.113.7", new MockHttpServletResponse()));
        var contextRequest = new MockHttpServletRequest("POST", "/blog/api/guest-comments");
        contextRequest.setContextPath("/blog");
        contextRequest.setRemoteAddr("203.0.113.7");
        assertFalse(interceptor.preHandle(contextRequest, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void readingCommentsDoesNotConsumeCreationQuota() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertTrue(call("/api/guest-comments/post/1", "203.0.113.6", new MockHttpServletResponse()));
        }
        assertTrue(call("/api/guest-comments", "203.0.113.6", new MockHttpServletResponse()));
    }

    // ==================== IP 归因与审计日志同源 ====================

    @Test
    void forgedXForwardedForCannotBypassRateLimitFromUntrustedPeer() throws Exception {
        // 直连对端不是可信代理：伪造 XFF 不能改变限流归属，否则攻击者每次换头即可绕过。
        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("POST", "/api/guest-comments");
            request.setRemoteAddr("203.0.113.20");
            request.addHeader("X-Forwarded-For", "1.1.1." + i);
            assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        }
        var sixth = new MockHttpServletRequest("POST", "/api/guest-comments");
        sixth.setRemoteAddr("203.0.113.20");
        sixth.addHeader("X-Forwarded-For", "2.2.2.2");
        assertFalse(interceptor.preHandle(sixth, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void trustedProxyXForwardedForIsHonouredForRateLimit() throws Exception {
        // 可信代理下沿用既有语义：按 XFF 中的第一个合法 IP 计数，不同客户端互不影响。
        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("POST", "/api/guest-comments");
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.21");
            assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        }
        var otherClient = new MockHttpServletRequest("POST", "/api/guest-comments");
        otherClient.setRemoteAddr("127.0.0.1");
        otherClient.addHeader("X-Forwarded-For", "203.0.113.22");
        assertTrue(interceptor.preHandle(otherClient, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rateLimitExceededIsWrittenToAuditLog() throws Exception {
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        var auditLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LogUtils.class);
        auditLogger.addAppender(appender);

        try {
            for (int i = 0; i < 5; i++) {
                assertTrue(call("/api/guest-comments", "203.0.113.30", new MockHttpServletResponse()));
            }
            assertFalse(call("/api/guest-comments", "203.0.113.30", new MockHttpServletResponse()));

            var messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertTrue(messages.stream().anyMatch(m -> m.contains("event=RATE_LIMIT_EXCEEDED")),
                    "限流命中必须写入审计日志，实际: " + messages);
            assertTrue(messages.stream().anyMatch(m -> m.contains("/api/guest-comments") && m.contains("203.0.113.30")),
                    "审计内容必须带 path 与 clientIp，实际: " + messages);
        } finally {
            auditLogger.detachAppender(appender);
        }
    }

    // ==================== 新增读路径阈值 ====================

    @Test
    void postListReadPathHasItsOwnLooseThreshold() throws Exception {
        // /api/post/list 必须走 60/min 的读路径阈值，而不是 /api/post/ 的 10/min，
        // 否则首页/归档页正常翻页就会被 429 误伤。
        for (int i = 0; i < 60; i++) {
            assertTrue(call("/api/post/list", "203.0.113.40", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/post/list", "203.0.113.40", new MockHttpServletResponse()));
        // 写路径仍受 10/min 保护，未被读路径放宽。
        for (int i = 0; i < 10; i++) {
            assertTrue(call("/api/post/", "203.0.113.41", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/post/", "203.0.113.41", new MockHttpServletResponse()));
    }

    @Test
    void postListSubPathsAlsoUseTheReadPathThreshold() throws Exception {
        for (int i = 0; i < 60; i++) {
            assertTrue(call("/api/post/list/featured", "203.0.113.42", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/post/list/featured", "203.0.113.42", new MockHttpServletResponse()));
    }

    private boolean call(String path, String ip, MockHttpServletResponse response) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        return interceptor.preHandle(request, response, new Object());
    }
}
