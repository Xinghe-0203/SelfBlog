package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
