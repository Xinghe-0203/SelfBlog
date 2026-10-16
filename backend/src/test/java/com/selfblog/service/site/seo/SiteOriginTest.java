package com.selfblog.service.site.seo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class SiteOriginTest {
    @Test void configuredOriginWinsOverAllRequestHosts() {
        var request = new MockHttpServletRequest();
        request.setScheme("http"); request.setServerName("internal"); request.setServerPort(8825);
        request.addHeader("Host", "attacker.test"); request.addHeader("X-Forwarded-Host", "attacker.test");
        request.addHeader("X-Forwarded-Proto", "javascript");
        assertEquals("https://blog.example.com", new SiteOrigin("https://BLOG.example.com:443/").resolve(request));
    }
    @Test void blankConfigurationUsesOnlyValidatedDirectRequestOrigin() {
        var request = new MockHttpServletRequest();
        request.setScheme("http"); request.setServerName("localhost"); request.setServerPort(8825);
        request.addHeader("X-Forwarded-Host", "attacker.test"); request.addHeader("X-Forwarded-Proto", "https");
        assertEquals("http://localhost:8825", new SiteOrigin("").resolve(request));
    }
    @ParameterizedTest @ValueSource(strings = {"javascript://example.com", "https://a:b@example.com", "https://example.com/path", "https://example.com?q=x", "https://example.com/#x", "https://example.com\\x", "https://example.com,evil.test", " https://example.com", "https://example.com\n", "https://example.com:99999", "https://example.com:0", "//example.com"})
    void rejectsInvalidExplicitConfiguration(String value) { assertThrows(IllegalArgumentException.class, () -> new SiteOrigin(value)); }
    @Test void supportsIpv6AndNondefaultPorts() {
        assertEquals("http://[::1]:8825", SiteOrigin.normalize("http://[::1]:8825/"));
        assertEquals("https://blog.example.com:8443", SiteOrigin.normalize("https://blog.example.com:8443"));
    }
    @Test void absoluteUrlsRejectExecutableAndNetworkPathInput() {
        String base = "https://blog.example.com";
        assertEquals(base + "/post/1", SiteOrigin.absolute(base, "/post/1"));
        assertEquals("https://images.example.com/a.jpg", SiteOrigin.absolute(base, "https://images.example.com/a.jpg"));
        assertEquals("", SiteOrigin.absolute(base, "javascript:alert(1)"));
        assertEquals("", SiteOrigin.absolute(base, "//evil.test/a.jpg"));
        assertEquals("", SiteOrigin.absolute(base, "https://user:pass@evil.test/a.jpg"));
    }
}
