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

    // ==================== 路径变量不得各自成桶 ====================

    @Test
    void differentIdsUnderTheSamePrefixShareOneQuota() throws Exception {
        // 回归：计数原先按具体路径归桶，/api/post/10、/api/post/11… 各自一份 10/min，
        // 换 id 就能无限刷。这里要求同一前缀下的不同 id 共享同一份配额。
        for (int id = 1; id <= 10; id++) {
            assertTrue(call("/api/post/" + id, "203.0.113.50", new MockHttpServletResponse()),
                    "第 " + id + " 个 id 仍应在 /api/post/ 的 10/min 配额内");
        }
        // 第 11 次请求即使换成全新的 id，也必须被同一前缀配额拦下。
        assertFalse(call("/api/post/999999", "203.0.113.50", new MockHttpServletResponse()));
    }

    @Test
    void distinctPrefixesStillHaveIndependentQuotas() throws Exception {
        // 按前缀归桶不能把不同前缀合并：刷满 /api/post/ 不应影响 /api/topic/。
        for (int id = 1; id <= 11; id++) {
            call("/api/post/" + id, "203.0.113.51", new MockHttpServletResponse());
        }
        assertTrue(call("/api/topic/7", "203.0.113.51", new MockHttpServletResponse()));
    }

    @Test
    void postListKeepsItsOwnBucketUnderThePrefixRule() throws Exception {
        // /api/post/list 必须继续走自己的 60/min 桶：既不能被 /api/post/ 的 10/min 误伤，
        // 也不能反过来把 /api/post/ 的计数算进自己头上。
        for (int i = 0; i < 10; i++) {
            assertTrue(call("/api/post/list", "203.0.113.52", new MockHttpServletResponse()));
        }
        // /api/post/list 自己的 10 次不消耗 /api/post/ 的配额：此处还能再发 10 次。
        for (int id = 1; id <= 10; id++) {
            assertTrue(call("/api/post/" + id, "203.0.113.52", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/post/11", "203.0.113.52", new MockHttpServletResponse()));
        // 且 /api/post/ 已满时，/api/post/list 仍未被牵连（第 11 次 list 请求仍放行）。
        assertTrue(call("/api/post/list", "203.0.113.52", new MockHttpServletResponse()));
    }

    // ==================== S8：后台与未登记前缀的兜底限流 ====================

    @Test
    void adminNamespaceWithoutANamedPrefixIsLimitedByTheCatchAll() throws Exception {
        // 回归：/api/admin/users 这类后台接口此前不匹配任何前缀，直接无限放行。
        for (int i = 0; i < 60; i++) {
            assertTrue(call("/api/admin/users", "203.0.113.60", new MockHttpServletResponse()),
                    "第 " + (i + 1) + " 次后台请求仍应在 60/min 兜底内");
        }
        assertFalse(call("/api/admin/users", "203.0.113.60", new MockHttpServletResponse()),
                "后台兜底阈值用尽后必须拒绝，而不是继续放行");
    }

    @Test
    void namedAdminPrefixesStillWinOverTheCatchAll() throws Exception {
        // 最长前缀优先必须继续成立：/api/admin/site-assets 是 30/min，
        // 不能被 /api/admin/ 的 60/min 顶掉（否则更严格的阈值被兜底放宽）。
        for (int i = 0; i < 30; i++) {
            assertTrue(call("/api/admin/site-assets", "203.0.113.61", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/admin/site-assets", "203.0.113.61", new MockHttpServletResponse()),
                "具名 admin 前缀的 30/min 必须优先于 /api/admin/ 的兜底");
        // 兜底桶与具名桶彼此独立：另一个后台路径还有自己的 60 次。
        assertTrue(call("/api/admin/users", "203.0.113.61", new MockHttpServletResponse()));
    }

    @Test
    void unregisteredApiPathsFallBackToTheGlobalDefaultInsteadOfUnlimited() throws Exception {
        // 回归：未命中任何前缀的 /api/** 原先 `return true` 无条件放行。
        for (int i = 0; i < 60; i++) {
            assertTrue(call("/api/not-registered-anywhere", "203.0.113.62", new MockHttpServletResponse()));
        }
        assertFalse(call("/api/not-registered-anywhere", "203.0.113.62", new MockHttpServletResponse()),
                "未登记路径必须受 /api/ 兜底限流约束");
    }

    @Test
    void nonApiPathsAreNotLimitedByTheFallback() throws Exception {
        // 兜底只覆盖 /api/ 命名空间；SEO 等非 /api 路径不在本拦截器职责内。
        for (int i = 0; i < 80; i++) {
            assertTrue(call("/feed.xml", "203.0.113.63", new MockHttpServletResponse()));
        }
    }

    private boolean call(String path, String ip, MockHttpServletResponse response) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        return interceptor.preHandle(request, response, new Object());
    }
}
