package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SiteSettingsServiceImpl 集成测试。
 * 沿用本仓库 ServiceImplTest 的既有约定：真实 H2 + @Transactional 回滚。
 *
 * 注意：`getSettings()` 带 `@Cacheable`，而 **Caffeine 缓存不参与事务回滚**。
 * 若不清理，某个用例在事务里写进缓存的值会活到事务结束之后，让后面的用例
 * 读到脏数据（而且是否复现取决于用例执行顺序）。所以每个用例前强制清缓存。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SiteSettingsServiceImpl")
class SiteSettingsServiceImplTest {

    @Autowired
    private SiteSettingsService siteSettingsService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearSettingsCache() {
        Cache cache = cacheManager.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("brandAssetKeys_ArePresentInDefaults")
    void brandAssetKeys_ArePresentInDefaults() {
        Map<String, String> settings = siteSettingsService.getSettings();
        assertTrue(settings.containsKey("faviconUrl"));
        assertTrue(settings.containsKey("siteLogoUrl"));
        assertEquals("", settings.get("faviconUrl"));
        assertEquals("", settings.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_AcceptsUploadedPaths")
    void updateSettings_AcceptsUploadedPaths() {
        Map<String, String> updated = siteSettingsService.updateSettings(Map.of(
                "faviconUrl", "/uploads/site-assets/favicon/abc.png",
                "siteLogoUrl", "/uploads/site-assets/logo/def.svg"));

        assertEquals("/uploads/site-assets/favicon/abc.png", updated.get("faviconUrl"));
        assertEquals("/uploads/site-assets/logo/def.svg", updated.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_AcceptsAbsoluteHttpUrls")
    void updateSettings_AcceptsAbsoluteHttpUrls() {
        Map<String, String> updated = siteSettingsService.updateSettings(Map.of(
                "siteLogoUrl", "https://cdn.example.com/logo.svg"));

        assertEquals("https://cdn.example.com/logo.svg", updated.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_AllowsClearingBackToEmpty")
    void updateSettings_AllowsClearingBackToEmpty() {
        siteSettingsService.updateSettings(Map.of("siteLogoUrl", "/uploads/site-assets/logo/def.svg"));

        Map<String, String> cleared = siteSettingsService.updateSettings(Map.of("siteLogoUrl", ""));
        assertEquals("", cleared.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_RejectsRelativeGarbageForBrandAssets")
    void updateSettings_RejectsRelativeGarbageForBrandAssets() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("siteLogoUrl", "javascript:alert(1)")));
        assertEquals(400, e.getCode());
    }

    @Test
    @DisplayName("updateSettings_RejectsPathTraversalForBrandAssets")
    void updateSettings_RejectsPathTraversalForBrandAssets() {
        assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("faviconUrl", "../../etc/passwd")));
    }

    @Test
    @DisplayName("updateSettings_StillAcceptsUnrelatedKeys")
    void updateSettings_StillAcceptsUnrelatedKeys() {
        Map<String, String> updated = siteSettingsService.updateSettings(Map.of("siteTitle", "新标题"));
        assertEquals("新标题", updated.get("siteTitle"));
        assertFalse(updated.isEmpty());
    }

    @Test
    void newAdminPathCannotTakeCustomPageNamespace() {
        assertThrows(BusinessException.class, () -> siteSettingsService.updateSettings(Map.of("adminPath", "pages")));
    }

    @Test
    @DisplayName("updateSettings_AcceptsSiteOriginAndAllowsClearingIt")
    void updateSettings_AcceptsSiteOriginAndAllowsClearingIt() {
        var updated = siteSettingsService.updateSettings(Map.of("siteBaseUrl", "https://blog.example.com"));
        assertEquals("https://blog.example.com", updated.get("siteBaseUrl"));
        assertTrue(siteSettingsService.getPublicSettings().containsKey("siteBaseUrl"));

        var cleared = siteSettingsService.updateSettings(Map.of("siteBaseUrl", ""));
        assertEquals("", cleared.get("siteBaseUrl"));
    }

    @Test
    @DisplayName("updateSettings_RejectsNonOriginSiteDomains")
    void updateSettings_RejectsNonOriginSiteDomains() {
        for (String value : java.util.List.of("blog.example.com", "https://blog.example.com/path",
                "https://user:pass@blog.example.com", "javascript://blog.example.com",
                "https://blog.example.com,evil.test", "https://blog.example.com?q=1", "https://blog.example.com#frag")) {
            var error = assertThrows(BusinessException.class,
                    () -> siteSettingsService.updateSettings(Map.of("siteBaseUrl", value)), value);
            assertEquals(400, error.getCode(), value);
        }
    }

    // ---------- 外观定制（9 个新键） ----------

    @Test
    @DisplayName("appearanceKeys_ArePresentInDefaultsWithFrozenDefaults")
    void appearanceKeys_ArePresentInDefaultsWithFrozenDefaults() {
        Map<String, String> settings = siteSettingsService.getSettings();
        assertEquals("solid", settings.get("backgroundType"));
        assertEquals("#f5f6f8", settings.get("backgroundColor"));
        assertEquals("#f5f6f8", settings.get("backgroundGradientFrom"));
        assertEquals("#e7eaf0", settings.get("backgroundGradientTo"));
        assertEquals("", settings.get("backgroundImageUrl"));
        assertEquals("0", settings.get("backgroundOverlay"));
        assertEquals("true", settings.get("backgroundFixed"));
        assertEquals("100", settings.get("surfaceOpacity"));
        assertEquals("0", settings.get("surfaceBlur"));
    }

    @Test
    @DisplayName("appearanceKeys_AreAllExposedByPublicSettings")
    void appearanceKeys_AreAllExposedByPublicSettings() {
        assertTrue(siteSettingsService.getPublicSettings().keySet().containsAll(java.util.List.of(
                "backgroundType", "backgroundColor", "backgroundGradientFrom", "backgroundGradientTo",
                "backgroundImageUrl", "backgroundOverlay", "backgroundFixed", "surfaceOpacity", "surfaceBlur")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"solid", "gradient", "image"})
    @DisplayName("backgroundType_AcceptsEveryDocumentedMode")
    void backgroundType_AcceptsEveryDocumentedMode(String mode) {
        assertEquals(mode, siteSettingsService.updateSettings(Map.of("backgroundType", mode)).get("backgroundType"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "SOLID", "photo", "gradient-image", "linear-gradient"})
    @DisplayName("backgroundType_RejectsAnythingElse")
    void backgroundType_RejectsAnythingElse(String mode) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("backgroundType", mode)), mode);
        assertEquals(400, error.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"backgroundColor", "backgroundGradientFrom", "backgroundGradientTo"})
    @DisplayName("backgroundColors_AcceptRrggbbAndRejectEverythingElse")
    void backgroundColors_AcceptRrggbbAndRejectEverythingElse(String key) {
        assertEquals("#0A1B2C", siteSettingsService.updateSettings(Map.of(key, "#0A1B2C")).get(key));
        // 注意：updateSettings 会先 trim 值，因此「首尾空白」不是可测的拒绝路径，这里只测真正非法的内容。
        for (String bad : java.util.List.of("", "#fff", "fff000", "#ff00000", "red", "#GGGGGG",
                "#ff0000; background:url(x)", "rgb(255,0,0)", "#ff0000ff")) {
            var error = assertThrows(BusinessException.class,
                    () -> siteSettingsService.updateSettings(Map.of(key, bad)), key + "=" + bad);
            assertEquals(400, error.getCode(), key + "=" + bad);
        }
    }

    @Test
    @DisplayName("backgroundImageUrl_AcceptsEmptyUploadPathAndAbsoluteUrl")
    void backgroundImageUrl_AcceptsEmptyUploadPathAndAbsoluteUrl() {
        assertEquals("", siteSettingsService.updateSettings(Map.of("backgroundImageUrl", "")).get("backgroundImageUrl"));
        assertEquals("/uploads/site-assets/background/x.png",
                siteSettingsService.updateSettings(Map.of("backgroundImageUrl", "/uploads/site-assets/background/x.png"))
                        .get("backgroundImageUrl"));
        assertEquals("https://cdn.example.com/bg.webp",
                siteSettingsService.updateSettings(Map.of("backgroundImageUrl", "https://cdn.example.com/bg.webp"))
                        .get("backgroundImageUrl"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://cdn.example.com/a\").png",   // 引号可闭合 CSS 字符串
            "https://cdn.example.com/a'.png",
            "https://cdn.example.com/a(b).png",   // 括号可伪造 url()
            "https://cdn.example.com/a)b.png",
            "https://cdn.example.com/a\\b.png",   // 反斜杠可转义收尾引号
            "https://cdn.example.com/a b.png",    // 空白可截断声明
            "https://cdn.example.com/a\tb.png",
            "/uploads/site-assets/background/a b.png",
            "javascript:alert(1)",                // 非 http(s)/uploads 前缀
            "data:image/png;base64,AAAA",
            "//evil.test/bg.png"
    })
    @DisplayName("backgroundImageUrl_RejectsCssInjectionVectors")
    void backgroundImageUrl_RejectsCssInjectionVectors(String url) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("backgroundImageUrl", url)), url);
        assertEquals(400, error.getCode(), url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "45", "90"})
    @DisplayName("backgroundOverlay_AcceptsInclusiveRange")
    void backgroundOverlay_AcceptsInclusiveRange(String value) {
        assertEquals(value, siteSettingsService.updateSettings(Map.of("backgroundOverlay", value)).get("backgroundOverlay"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "91", "100", "abc", "10.5", "1e2", ""})
    @DisplayName("backgroundOverlay_RejectsOutOfRangeAndNonIntegers")
    void backgroundOverlay_RejectsOutOfRangeAndNonIntegers(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("backgroundOverlay", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"60", "80", "100"})
    @DisplayName("surfaceOpacity_AcceptsInclusiveRange")
    void surfaceOpacity_AcceptsInclusiveRange(String value) {
        assertEquals(value, siteSettingsService.updateSettings(Map.of("surfaceOpacity", value)).get("surfaceOpacity"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"59", "101", "0", "-5", "opaque", "60%", ""})
    @DisplayName("surfaceOpacity_RejectsOutOfRangeAndNonIntegers")
    void surfaceOpacity_RejectsOutOfRangeAndNonIntegers(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("surfaceOpacity", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "20", "40"})
    @DisplayName("surfaceBlur_AcceptsInclusiveRange")
    void surfaceBlur_AcceptsInclusiveRange(String value) {
        assertEquals(value, siteSettingsService.updateSettings(Map.of("surfaceBlur", value)).get("surfaceBlur"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "41", "999", "blur", "8px", ""})
    @DisplayName("surfaceBlur_RejectsOutOfRangeAndNonIntegers")
    void surfaceBlur_RejectsOutOfRangeAndNonIntegers(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("surfaceBlur", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "TRUE", "False"})
    @DisplayName("backgroundFixed_AcceptsBooleansCaseInsensitively")
    void backgroundFixed_AcceptsBooleansCaseInsensitively(String value) {
        String stored = siteSettingsService.updateSettings(Map.of("backgroundFixed", value)).get("backgroundFixed");
        assertEquals(value.trim(), stored);
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "1", "0", "on", "fixed", ""})
    @DisplayName("backgroundFixed_RejectsNonBooleans")
    void backgroundFixed_RejectsNonBooleans(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("backgroundFixed", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @Test
    @DisplayName("appearanceKeys_AreAcceptedTogether")
    void appearanceKeys_AreAcceptedTogether() {
        Map<String, String> updated = siteSettingsService.updateSettings(Map.of(
                "backgroundType", "image",
                "backgroundImageUrl", "/uploads/site-assets/background/bg.webp",
                "backgroundOverlay", "55",
                "backgroundFixed", "false",
                "surfaceOpacity", "72",
                "surfaceBlur", "12"));

        assertEquals("image", updated.get("backgroundType"));
        assertEquals("/uploads/site-assets/background/bg.webp", updated.get("backgroundImageUrl"));
        assertEquals("55", updated.get("backgroundOverlay"));
        assertEquals("false", updated.get("backgroundFixed"));
        assertEquals("72", updated.get("surfaceOpacity"));
        assertEquals("12", updated.get("surfaceBlur"));
    }
}
