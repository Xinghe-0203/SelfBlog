package com.selfblog.service.site.seo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteOriginTest {

    /** 未配置后台域名时的纯 origin 行为。 */
    private static SiteOrigin origin(String configured) {
        return origin(configured, null);
    }

    /** configured=环境变量；stored=后台设置。 */
    private static SiteOrigin origin(String configured, String stored) {
        var settings = mock(com.selfblog.service.site.SiteSettingsService.class);
        when(settings.getSettings()).thenReturn(stored == null ? Map.of() : Map.of("siteBaseUrl", stored));
        return new SiteOrigin(configured, settings);
    }

    @Test void configuredOriginWinsOverAllRequestHosts() {
        var request = new MockHttpServletRequest();
        request.setScheme("http"); request.setServerName("internal"); request.setServerPort(8825);
        request.addHeader("Host", "attacker.test"); request.addHeader("X-Forwarded-Host", "attacker.test");
        request.addHeader("X-Forwarded-Proto", "javascript");
        assertEquals("https://blog.example.com", origin("https://BLOG.example.com:443/").resolve(request));
    }
    @Test void blankConfigurationUsesOnlyValidatedDirectRequestOrigin() {
        var request = new MockHttpServletRequest();
        request.setScheme("http"); request.setServerName("localhost"); request.setServerPort(8825);
        request.addHeader("X-Forwarded-Host", "attacker.test"); request.addHeader("X-Forwarded-Proto", "https");
        assertEquals("http://localhost:8825", origin("").resolve(request));
    }
    @Test void adminSettingOverridesEnvironmentAndCanBeCleared() {
        var request = new MockHttpServletRequest();
        request.setScheme("http"); request.setServerName("internal"); request.setServerPort(8825);
        assertEquals("https://blog.example.com", origin("https://env.example.com", "https://BLOG.example.com/").resolve(request));
        assertEquals("https://env.example.com", origin("https://env.example.com", "").resolve(request));
        assertEquals("https://env.example.com", origin("https://env.example.com", "   ").resolve(request));
    }
    @Test void invalidStoredSettingFallsBackInsteadOfBreakingEveryPage() {
        var request = new MockHttpServletRequest();
        request.setScheme("http"); request.setServerName("local"); request.setServerPort(8080);
        assertEquals("https://env.example.com", origin("https://env.example.com", "https://broken.example.com/path").resolve(request));
        assertEquals("http://local:8080", origin("", "not-a-url").resolve(request));
    }
    @ParameterizedTest @ValueSource(strings = {"javascript://example.com", "https://a:b@example.com", "https://example.com/path", "https://example.com?q=x", "https://example.com/#x", "https://example.com\\x", "https://example.com,evil.test", " https://example.com", "https://example.com\n", "https://example.com:99999", "https://example.com:0", "//example.com"})
    void rejectsInvalidExplicitConfiguration(String value) { assertThrows(IllegalArgumentException.class, () -> origin(value)); }
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
