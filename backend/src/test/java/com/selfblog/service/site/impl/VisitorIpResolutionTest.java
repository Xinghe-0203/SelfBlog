package com.selfblog.service.site.impl;

import com.selfblog.dto.site.TrackVisitorRequest;
import com.selfblog.service.site.VisitorService;
import com.selfblog.utils.LogUtils;
import com.selfblog.utils.VisitorHashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：访客埋点记录的 IP 必须与限流/审计同源。
 *
 * <p>修复前 {@code VisitorServiceImpl.track} 直接用 {@code httpRequest.getRemoteAddr()}，
 * 而限流与安全审计用 {@link LogUtils#getClientIp(javax.servlet.http.HttpServletRequest)}
 * （仅在可信代理下解析 X-Forwarded-For）。部署在反向代理后，两者归属的 IP 不一致：
 * 访客表里存的是代理地址，等于把全站访客合并成同一个 IP。</p>
 *
 * <p>断言的是**入库后的 ip_hash 真实取值**，而不是调用参数，才能证明语义确实改了。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("访客埋点 IP 归属")
class VisitorIpResolutionTest {

    private static final String PATH = "/visitor-ip-test";
    private static final String VISITOR_ID = "visitor-ip-test-0001";

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${app.visitor-hash-secret:}")
    private String hashSecret;

    @BeforeEach
    void clearEvents() {
        jdbc.update("DELETE FROM visitor_event");
    }

    @Test
    @DisplayName("可信代理下记录 X-Forwarded-For 中的访客 IP，而不是代理地址")
    void trustedProxyRecordsTheForwardedClientIp() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/visitors/track");
        request.setRemoteAddr("127.0.0.1"); // 可信代理
        request.addHeader("X-Forwarded-For", "203.0.113.7, 127.0.0.1");
        request.addHeader("User-Agent", "JUnit");

        visitorService.track(trackRequest(VISITOR_ID), request);

        String storedIpHash = jdbc.queryForObject("SELECT ip_hash FROM visitor_event", String.class);
        assertEquals(VisitorHashUtils.hash("203.0.113.7", hashSecret), storedIpHash,
                "可信代理后必须记录真实访客 IP，而不是 127.0.0.1");
        assertNotEquals(VisitorHashUtils.hash("127.0.0.1", hashSecret), storedIpHash,
                "若存的是代理地址即为本回归缺陷");
    }

    @Test
    @DisplayName("不可信来源伪造 X-Forwarded-For 时不改变记录的 IP")
    void forgedForwardedHeaderFromUntrustedPeerIsIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/visitors/track");
        request.setRemoteAddr("198.51.100.9"); // 非可信来源
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        visitorService.track(trackRequest(VISITOR_ID), request);

        String storedIpHash = jdbc.queryForObject("SELECT ip_hash FROM visitor_event", String.class);
        assertEquals(VisitorHashUtils.hash("198.51.100.9", hashSecret), storedIpHash,
                "不可信来源的 X-Forwarded-For 必须被忽略，避免 IP 伪造");
    }

    @Test
    @DisplayName("去重语义保持不变：30 秒内同一访客同一路径只写一条")
    void dedupBehaviourIsPreserved() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/visitors/track");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        visitorService.track(trackRequest(VISITOR_ID), request);
        visitorService.track(trackRequest(VISITOR_ID), request);

        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM visitor_event WHERE page_path = ?", Integer.class, PATH);
        assertEquals(1, rows, "快速刷新不应重复入库");

        // 换一个访客标识则应正常写入第二条
        visitorService.track(trackRequest("visitor-ip-test-0002"), request);
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM visitor_event WHERE page_path = ?", Integer.class, PATH) >= 2);
    }

    private TrackVisitorRequest trackRequest(String visitorId) {
        TrackVisitorRequest request = new TrackVisitorRequest();
        request.setVisitorId(visitorId);
        request.setPath(PATH);
        return request;
    }
}
