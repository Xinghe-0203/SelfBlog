package com.selfblog.config.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private boolean call(String path, String ip, MockHttpServletResponse response) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        return interceptor.preHandle(request, response, new Object());
    }
}
