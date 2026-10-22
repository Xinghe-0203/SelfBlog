package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LLM 相关站点设置的契约测试，重点是密钥的可见性边界：
 * 公开接口必须完全看不到 {@code llm*} 键，后台读接口必须只看到掩码。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SiteSettingsServiceImpl - LLM 设置")
class SiteSettingsLlmTest {

    private static final String API_KEY = "sk-unit-test-secret-key-123456";
    private static final String MASK = SiteSettingsServiceImpl.SECRET_MASK;

    @Autowired
    private SiteSettingsService siteSettingsService;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterEach
    void clearCacheAfter() {
        var cache = cacheManager.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private void storeLlmSettings() {
        siteSettingsService.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", "https://api.example.com/v1",
                "llmApiKey", API_KEY,
                "llmModel", "gpt-4o-mini",
                "llmTimeoutSeconds", "45",
                "llmAutoOnPublish", "true"));
    }

    @Test
    @DisplayName("getPublicSettings_不含任何 llm 键")
    void publicSettings_NeverExposeLlmKeys() {
        storeLlmSettings();

        Map<String, String> publicSettings = siteSettingsService.getPublicSettings();

        assertTrue(publicSettings.keySet().stream().noneMatch(key -> key.startsWith("llm")),
                "公开设置泄露了 LLM 配置：" + publicSettings.keySet());
        assertFalse(publicSettings.containsValue(API_KEY), "公开设置泄露了 API Key");
        // 非 llm 的公开设置必须照旧存在，别把公开接口整体改坏
        assertTrue(publicSettings.containsKey("siteBaseUrl"));
        assertTrue(publicSettings.containsKey("siteTitle"));
    }

    @Test
    @DisplayName("getAdminSettings_密钥只返回掩码")
    void adminSettings_MasksApiKey() {
        storeLlmSettings();

        Map<String, String> adminSettings = siteSettingsService.getAdminSettings();

        assertEquals(MASK, adminSettings.get("llmApiKey"));
        assertFalse(adminSettings.containsValue(API_KEY));
        // 非密钥项照常明文返回，否则后台表单没法回填
        assertEquals("https://api.example.com/v1", adminSettings.get("llmBaseUrl"));
        assertEquals("gpt-4o-mini", adminSettings.get("llmModel"));
        assertEquals("45", adminSettings.get("llmTimeoutSeconds"));
        assertEquals("true", adminSettings.get("llmEnabled"));
        assertEquals("true", adminSettings.get("llmAutoOnPublish"));
    }

    @Test
    @DisplayName("未配置时后台读接口返回空字符串而不是 null")
    void adminSettings_UnconfiguredKeyIsEmptyString() {
        siteSettingsService.updateSettings(Map.of("llmApiKey", ""));

        Map<String, String> adminSettings = siteSettingsService.getAdminSettings();

        assertEquals("", adminSettings.get("llmApiKey"));
    }

    @Test
    @DisplayName("保存掩码不会破坏已存的密钥")
    void updateSettings_MaskRoundTripKeepsStoredKey() {
        storeLlmSettings();

        // 后台表单把读到的掩码原样提交回来（用户只改了别的字段）
        siteSettingsService.updateSettings(Map.of("llmApiKey", MASK, "llmModel", "gpt-4o"));

        Map<String, String> adminSettings = siteSettingsService.getAdminSettings();
        assertEquals(MASK, adminSettings.get("llmApiKey"));
        assertEquals("gpt-4o", adminSettings.get("llmModel"));
        assertEquals(API_KEY, siteSettingsService.getSettings().get("llmApiKey"),
                "掩码回传把真实密钥冲掉了");
    }

    @Test
    @DisplayName("保存空字符串会清除密钥")
    void updateSettings_EmptyValueClearsKey() {
        storeLlmSettings();

        siteSettingsService.updateSettings(Map.of("llmApiKey", ""));

        assertEquals("", siteSettingsService.getSettings().get("llmApiKey"));
        assertEquals("", siteSettingsService.getAdminSettings().get("llmApiKey"));
    }

    @Test
    @DisplayName("保存新密钥会替换旧密钥")
    void updateSettings_NewValueReplacesKey() {
        storeLlmSettings();

        siteSettingsService.updateSettings(Map.of("llmApiKey", "sk-brand-new-key-9999"));

        assertEquals("sk-brand-new-key-9999", siteSettingsService.getSettings().get("llmApiKey"));
        assertEquals(MASK, siteSettingsService.getAdminSettings().get("llmApiKey"));
    }

    @Test
    @DisplayName("updateSettings 的返回值也是掩码视图")
    void updateSettings_ResponseIsMasked() {
        Map<String, String> response = siteSettingsService.updateSettings(Map.of("llmApiKey", API_KEY));

        assertEquals(MASK, response.get("llmApiKey"));
        assertFalse(response.containsValue(API_KEY));
    }

    @Test
    @DisplayName("开关只接受 true/false")
    void updateSettings_RejectsNonBooleanSwitches() {
        BusinessException enabled = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmEnabled", "yes")));
        assertEquals(400, enabled.getCode());

        BusinessException auto = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmAutoOnPublish", "1")));
        assertEquals(400, auto.getCode());
    }

    @Test
    @DisplayName("接口地址必须是 http(s)")
    void updateSettings_RejectsNonHttpBaseUrl() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmBaseUrl", "api.example.com/v1")));

        assertEquals(400, error.getCode());
    }

    @Test
    @DisplayName("接口地址允许清空")
    void updateSettings_AllowsClearingBaseUrl() {
        storeLlmSettings();

        siteSettingsService.updateSettings(Map.of("llmBaseUrl", ""));

        assertEquals("", siteSettingsService.getSettings().get("llmBaseUrl"));
    }

    @Test
    @DisplayName("超时时间限定在 5-120 秒")
    void updateSettings_RejectsOutOfRangeTimeout() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmTimeoutSeconds", "3"))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmTimeoutSeconds", "121"))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmTimeoutSeconds", "abc"))).getCode());
        assertEquals("120", siteSettingsService.updateSettings(
                Map.of("llmTimeoutSeconds", "120")).get("llmTimeoutSeconds"));
    }

    @Test
    @DisplayName("模型名与密钥长度上限")
    void updateSettings_RejectsOverlongModelAndKey() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmModel", "m".repeat(101)))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("llmApiKey", "k".repeat(501)))).getCode());
    }

    @Test
    @DisplayName("默认值齐全，未配置时不会出现 null")
    void defaults_ContainAllLlmKeys() {
        Map<String, String> adminSettings = siteSettingsService.getAdminSettings();

        assertEquals("false", adminSettings.get("llmEnabled"));
        assertEquals("", adminSettings.get("llmBaseUrl"));
        assertEquals("", adminSettings.get("llmApiKey"));
        assertEquals("", adminSettings.get("llmModel"));
        assertEquals("30", adminSettings.get("llmTimeoutSeconds"));
        assertEquals("false", adminSettings.get("llmAutoOnPublish"));
    }
}
