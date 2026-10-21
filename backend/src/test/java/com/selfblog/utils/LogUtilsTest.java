package com.selfblog.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 *
 * <p><b>S2（2026-10-20）</b>：可信代理判定从「整个私有网段都可信」改为显式可配置白名单，
 * 默认只信任环回地址（{@code 127.0.0.1/32, ::1/128}）。原因是用 {@code docker run -p 8825:8825}
 * 发布端口时直连对端是网桥网关 {@code 172.17.0.1}，落在旧私有网段里，于是任何客户端都能伪造
 * {@code X-Forwarded-For} 来控制限流桶、审计 IP 与访客哈希输入。本测试随之更新到新契约，
 * 并显式断言「默认不再信任私有网段」。配置注入通过
 * {@link LogUtils#configureTrustedProxies(String)} 模拟 {@code TrustedProxyConfig} 的启动行为。</p>
 */
class LogUtilsTest {

    /** 每个用例都从默认白名单（仅环回）出发，避免用例之间互相污染静态配置。 */
    @BeforeEach
    void resetTrustedProxies() {
        LogUtils.configureTrustedProxies(null);
    }

    @AfterEach
    void restoreDefaultTrustedProxies() {
        LogUtils.configureTrustedProxies(null);
    }

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
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "garbage, 203.0.113.20");

        assertEquals("203.0.113.20", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("私有网段直连对端默认不再可信：伪造的代理头被忽略，归因到对端地址")
    void privateRangePeerIsNoLongerTrustedByDefault() {
        // 这是 S2 的核心回归：docker -p 发布端口时对端是 172.17.0.1，旧实现会采信伪造头。
        MockHttpServletRequest request = request("172.17.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("172.17.0.1", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("可信代理下 X-Forwarded-For 为 unknown/非法时回退到 remoteAddr")
    void trustedProxyFallsBackToRemoteAddrWhenHeadersUnusable() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "unknown");

        assertEquals("127.0.0.1", LogUtils.getClientIp(request));
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

        MockHttpServletRequest realIp = request("127.0.0.1");
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
    @DisplayName("默认白名单只信任环回地址，私有网段一律不可信")
    void trustedProxyRangeSemantics() {
        // 环回地址：IPv4 与 IPv6 等价写法都可信。
        assertTrue(LogUtils.isTrustedProxy("127.0.0.1"));
        assertTrue(LogUtils.isTrustedProxy("::1"));
        assertTrue(LogUtils.isTrustedProxy("0:0:0:0:0:0:0:1"));

        // S2 的核心变更：私有网段默认不再可信（docker -p 发布端口时对端就是 172.17.0.1）。
        assertFalse(LogUtils.isTrustedProxy("10.0.0.1"));
        assertFalse(LogUtils.isTrustedProxy("10.255.255.255"));
        assertFalse(LogUtils.isTrustedProxy("192.168.0.1"));
        assertFalse(LogUtils.isTrustedProxy("172.16.0.1"));
        assertFalse(LogUtils.isTrustedProxy("172.17.0.1"));
        assertFalse(LogUtils.isTrustedProxy("172.31.255.254"));

        // 其余非法/非环回地址同样不可信。
        assertFalse(LogUtils.isTrustedProxy(null));
        assertFalse(LogUtils.isTrustedProxy(""));
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

    @Test
    @DisplayName("显式登记后私有网段才可信：10.0.0.0/8 内的地址可采信代理头")
    void explicitlyConfiguredPrivateRangeBecomesTrusted() {
        LogUtils.configureTrustedProxies("10.0.0.0/8");

        assertTrue(LogUtils.isTrustedProxy("10.1.2.3"));
        assertTrue(LogUtils.isTrustedProxy("10.255.255.255"));
        // 未登记的网段不受影响。
        assertFalse(LogUtils.isTrustedProxy("192.168.0.1"));
        assertFalse(LogUtils.isTrustedProxy("127.0.0.1"));

        MockHttpServletRequest request = request("10.1.2.3");
        request.addHeader("X-Forwarded-For", "203.0.113.60");
        assertEquals("203.0.113.60", LogUtils.getClientIp(request));
    }

    @Test
    @DisplayName("单 IP（无掩码）按 /32 处理，只精确匹配该地址")
    void bareIpEntryMatchesExactly() {
        LogUtils.configureTrustedProxies("192.168.1.7");

        assertTrue(LogUtils.isTrustedProxy("192.168.1.7"));
        assertFalse(LogUtils.isTrustedProxy("192.168.1.8"));
        assertFalse(LogUtils.isTrustedProxy("127.0.0.1"));
    }

    @Test
    @DisplayName("配置全为垃圾值时回退到默认环回白名单，绝不退化成全部信任")
    void invalidConfigurationFallsBackToLoopbackOnly() {
        // 三个条目分别因：非 IP 字面量、含非十六进制字符、掩码超出地址位宽 —— 全部解析失败。
        LogUtils.configureTrustedProxies("not-an-ip, example.com, 10.0.0.0/99");

        assertTrue(LogUtils.isTrustedProxy("127.0.0.1"));
        assertTrue(LogUtils.isTrustedProxy("::1"));
        assertFalse(LogUtils.isTrustedProxy("10.1.2.3"));
        assertFalse(LogUtils.isTrustedProxy("203.0.113.1"));
    }

    @Test
    @DisplayName("配置值不会被当作主机名解析：字母域名条目被忽略而不是触发 DNS")
    void hostnameEntriesAreRejectedWithoutDnsLookup() {
        LogUtils.configureTrustedProxies("evil.example.com");

        assertFalse(LogUtils.isTrustedProxy("evil.example.com"));
        assertTrue(LogUtils.isTrustedProxy("127.0.0.1"));
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
