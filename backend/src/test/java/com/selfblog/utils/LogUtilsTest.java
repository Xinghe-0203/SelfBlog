package com.selfblog.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogUtils 客户端 IP 解析测试。
 *
 * 回归背景：原实现只要请求带 X-Forwarded-For 就采信其中第一个合法 IP，
 * 攻击者可伪造该头污染审计日志，而限流计数用的是另一套（带可信代理校验的）实现，
 * 同一个请求在两处得到不同 IP。这里锁定「只有直连对端是可信代理时才采信代理头」。
 */
class LogUtilsTest {

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/post/list");
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    // ==================== 不可信来源：伪造头必须被忽略 ====================

    @Test
    @DisplayName("非可信直连对端伪造 X-Forwarded-For 时，仍归因到真实 remoteAddr")
    void forgedXForwardedForFromUntrustedPeerIsIgnored() {
        MockHttpServletRequest request = request("203.0.113.10");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("203.0.113.10", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("非可信直连对端伪造 X-Real-IP / Proxy-Client-IP / WL-Proxy-Client-IP 时同样被忽略")
    void otherForgedProxyHeadersFromUntrustedPeerAreIgnored() {
        MockHttpServletRequest request = request("203.0.113.11");
        request.addHeader("X-Real-IP", "1.2.3.4");
        request.addHeader("Proxy-Client-IP", "5.6.7.8");
        request.addHeader("WL-Proxy-Client-IP", "9.10.11.12");

        assertEquals("203.0.113.11", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("非可信对端即使 remoteAddr 无法解析也不采信代理头")
    void untrustedPeerFallsBackToDefaultWhenRemoteAddrInvalid() {
        MockHttpServletRequest request = request("not-an-ip");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("0.0.0.0", LogUtils.getClientIp(request));
    }

    // ==================== 可信来源：代理头生效，取第一个合法 IP ====================

    @Test
    @DisplayName("可信代理下取 X-Forwarded-For 列表中的第一个合法 IP")
    void trustedProxyUsesFirstValidIpInXForwardedFor() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        assertEquals("203.0.113.9", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("可信代理下跳过 X-Forwarded-For 中的非法段，取其后第一个合法 IP")
    void trustedProxySkipsInvalidXForwardedForEntries() {
        MockHttpServletRequest request = request("10.1.2.3");
        request.addHeader("X-Forwarded-For", "garbage, 203.0.113.20");

        assertEquals("203.0.113.20", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("可信代理下 X-Forwarded-For 为 unknown/非法时回退到 remoteAddr")
    void trustedProxyFallsBackToRemoteAddrWhenHeadersUnusable() {
        MockHttpServletRequest request = request("192.168.1.7");
        request.addHeader("X-Forwarded-For", "unknown");

        assertEquals("192.168.1.7", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("可信代理下依次使用 Proxy-Client-IP / WL-Proxy-Client-IP / X-Real-IP")
    void trustedProxyUsesRemainingProxyHeaders() {
        MockHttpServletRequest proxyClient = request("::1");
        proxyClient.addHeader("Proxy-Client-IP", "203.0.113.30");
        assertEquals("203.0.113.30", LogUtils.getClientIp(proxyClient));

        MockHttpServletRequest wlProxy = request("0:0:0:0:0:0:0:1");
        wlProxy.addHeader("WL-Proxy-Client-IP", "203.0.113.31");
        assertEquals("203.0.113.31", LogUtils.getClientIp(wlProxy));

        MockHttpServletRequest realIp = request("172.16.5.5");
        realIp.addHeader("X-Real-IP", "203.0.113.32");
        assertEquals("203.0.113.32", LogUtils.getClientIp(realIp));
    }

    @Test
    @DisplayName("无任何代理头时直接使用 remoteAddr")
    void remoteAddrIsUsedWithoutProxyHeaders() {
        assertEquals("203.0.113.40", LogUtils.getClientIp(request("203.0.113.40")));
    }

    // ==================== 可信代理判定语义 ====================

    @Test
    @DisplayName("可信代理网段判定与限流拦截器历史语义一致")
    void trustedProxyRangeSemantics() {
        assertTrue(LogUtils.isTrustedProxy("10.0.0.1"));
        assertTrue(LogUtils.isTrustedProxy("10.255.255.255"));
        assertTrue(LogUtils.isTrustedProxy("192.168.0.1"));
        assertTrue(LogUtils.isTrustedProxy("127.0.0.1"));
        assertTrue(LogUtils.isTrustedProxy("::1"));
        assertTrue(LogUtils.isTrustedProxy("0:0:0:0:0:0:0:1"));
        assertTrue(LogUtils.isTrustedProxy("172.16.0.1"));
        assertTrue(LogUtils.isTrustedProxy("172.31.255.254"));

        assertFalse(LogUtils.isTrustedProxy(null));
        assertFalse(LogUtils.isTrustedProxy("172.15.0.1"));
        assertFalse(LogUtils.isTrustedProxy("172.32.0.1"));
        assertFalse(LogUtils.isTrustedProxy("172."));
        assertFalse(LogUtils.isTrustedProxy("11.0.0.1"));
        assertFalse(LogUtils.isTrustedProxy("192.169.0.1"));
        assertFalse(LogUtils.isTrustedProxy("203.0.113.1"));
        // 前缀匹配不能把 100.x / 192.1680.x 误判为私网
        assertFalse(LogUtils.isTrustedProxy("100.64.0.1"));
        assertFalse(LogUtils.isTrustedProxy("192.1680.0.1"));
    }

    // ==================== 当前请求上下文取值 ====================

    @Test
    @DisplayName("无请求上下文时 getClientIpFromCurrentRequest 返回 unknown")
    void currentRequestIpIsUnknownOutsideRequestThread() {
        RequestContextHolder.resetRequestAttributes();

        assertEquals("unknown", LogUtils.getClientIpFromCurrentRequest());
    }

    @Test
    @DisplayName("请求上下文存在时 getClientIpFromCurrentRequest 复用同一套可信代理判定")
    void currentRequestIpReusesTrustedProxyRules() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            assertEquals("203.0.113.50", LogUtils.getClientIpFromCurrentRequest());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
