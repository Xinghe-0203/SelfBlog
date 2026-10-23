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
    @DisplayName("appearanceKeys_ArePresentInDefaultsWithGlassBaseline")
    void appearanceKeys_ArePresentInDefaultsWithGlassBaseline() {
        Map<String, String> settings = siteSettingsService.getSettings();
        assertEquals("gradient", settings.get("backgroundType"));
        assertEquals("#f5f6f8", settings.get("backgroundColor"));
        assertEquals("#f4f7fc", settings.get("backgroundGradientFrom"));
        assertEquals("#e9eef7", settings.get("backgroundGradientTo"));
        assertEquals("", settings.get("backgroundImageUrl"));
        assertEquals("0", settings.get("backgroundOverlay"));
        assertEquals("true", settings.get("backgroundFixed"));
        assertEquals("88", settings.get("surfaceOpacity"));
        assertEquals("12", settings.get("surfaceBlur"));
        assertEquals("on", settings.get("glassMode"));
    }

    @Test
    @DisplayName("appearanceKeys_AreAllExposedByPublicSettings")
    void appearanceKeys_AreAllExposedByPublicSettings() {
        assertTrue(siteSettingsService.getPublicSettings().keySet().containsAll(java.util.List.of(
                "backgroundType", "backgroundColor", "backgroundGradientFrom", "backgroundGradientTo",
                "backgroundImageUrl", "backgroundOverlay", "backgroundFixed", "surfaceOpacity", "surfaceBlur",
                "glassMode")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"on", "off"})
    @DisplayName("glassMode_AcceptsOnAndOff")
    void glassMode_AcceptsOnAndOff(String mode) {
        assertEquals(mode, siteSettingsService.updateSettings(Map.of("glassMode", mode)).get("glassMode"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ON", "Off", "true", "1", "yes", "glass"})
    @DisplayName("glassMode_RejectsAnythingElse")
    void glassMode_RejectsAnythingElse(String mode) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("glassMode", mode)), mode);
        assertEquals(400, error.getCode());
        assertEquals("请选择有效的玻璃感模式", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ocean", "forest", "lavender", "sunset", "midnight", "ink", "dusk", "sand"})
    @DisplayName("themeSkin_AcceptsEveryPublishedSkin")
    void themeSkin_AcceptsEveryPublishedSkin(String skin) {
        assertEquals(skin, siteSettingsService.updateSettings(Map.of("themeSkin", skin)).get("themeSkin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "OCEAN", "ink-gradient", "noir"})
    @DisplayName("themeSkin_RejectsUnknownSkins")
    void themeSkin_RejectsUnknownSkins(String skin) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("themeSkin", skin)), skin);
        assertEquals(400, error.getCode());
        assertEquals("请选择有效的页面皮肤", error.getMessage());
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

    // ---------- 外观深度自定义（11 个新键：排版 / 布局 / 呈现 / 动效） ----------

    /**
     * 默认值必须与改造前的硬编码取值逐字相同，否则老站点升级后观感会变。
     * 这里的断言就是「零回归」的可执行定义：16px 正文字号、1.75 行高、760px 阅读栏宽、
     * 1em 段间距、默认圆角/阴影/密度/头图/封面/动效。
     */
    @Test
    @DisplayName("deepAppearanceKeys_ArePresentInDefaultsWithFrozenDefaults")
    void deepAppearanceKeys_ArePresentInDefaultsWithFrozenDefaults() {
        Map<String, String> settings = siteSettingsService.getSettings();
        assertEquals("system", settings.get("fontFamily"));
        assertEquals("16", settings.get("baseFontSize"));
        assertEquals("1.75", settings.get("bodyLineHeight"));
        assertEquals("760", settings.get("readerWidth"));
        assertEquals("1", settings.get("paragraphSpacing"));
        assertEquals("default", settings.get("radiusScale"));
        assertEquals("default", settings.get("shadowDepth"));
        assertEquals("default", settings.get("layoutDensity"));
        assertEquals("default", settings.get("homeHeroStyle"));
        assertEquals("default", settings.get("postCoverStyle"));
        assertEquals("default", settings.get("motionIntensity"));
    }

    /**
     * 前台渲染依赖这些键：公开设置必须下发它们，否则前端只能永远用默认值。
     * 同时确认它们**没有**被误加进 LLM 过滤组（那组整组不进公开设置）。
     */
    @Test
    @DisplayName("deepAppearanceKeys_AreAllExposedByPublicSettings")
    void deepAppearanceKeys_AreAllExposedByPublicSettings() {
        Map<String, String> publicSettings = siteSettingsService.getPublicSettings();
        assertTrue(publicSettings.keySet().containsAll(java.util.List.of(
                "fontFamily", "baseFontSize", "bodyLineHeight", "readerWidth", "paragraphSpacing",
                "radiusScale", "shadowDepth", "layoutDensity",
                "homeHeroStyle", "postCoverStyle", "motionIntensity")));
        assertEquals("760", publicSettings.get("readerWidth"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"system", "sans", "serif", "mono"})
    @DisplayName("fontFamily_AcceptsEveryDocumentedFamily")
    void fontFamily_AcceptsEveryDocumentedFamily(String family) {
        assertEquals(family, siteSettingsService.updateSettings(Map.of("fontFamily", family)).get("fontFamily"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "SANS", "serif-sans", "sans-serif", "kai"})
    @DisplayName("fontFamily_RejectsAnythingElse")
    void fontFamily_RejectsAnythingElse(String family) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("fontFamily", family)), family);
        assertEquals(400, error.getCode(), family);
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "sharp", "default", "soft", "round"})
    @DisplayName("radiusScale_AcceptsEveryDocumentedScale")
    void radiusScale_AcceptsEveryDocumentedScale(String scale) {
        assertEquals(scale, siteSettingsService.updateSettings(Map.of("radiusScale", scale)).get("radiusScale"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "DEFAULT", "pill", "square", "0"})
    @DisplayName("radiusScale_RejectsAnythingElse")
    void radiusScale_RejectsAnythingElse(String scale) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("radiusScale", scale)), scale);
        assertEquals(400, error.getCode(), scale);
    }

    @ParameterizedTest
    @ValueSource(strings = {"flat", "default", "deep"})
    @DisplayName("shadowDepth_AcceptsEveryDocumentedDepth")
    void shadowDepth_AcceptsEveryDocumentedDepth(String depth) {
        assertEquals(depth, siteSettingsService.updateSettings(Map.of("shadowDepth", depth)).get("shadowDepth"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "FLAT", "heavy", "none", "light"})
    @DisplayName("shadowDepth_RejectsAnythingElse")
    void shadowDepth_RejectsAnythingElse(String depth) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("shadowDepth", depth)), depth);
        assertEquals(400, error.getCode(), depth);
    }

    @ParameterizedTest
    @ValueSource(strings = {"compact", "default", "comfortable"})
    @DisplayName("layoutDensity_AcceptsEveryDocumentedDensity")
    void layoutDensity_AcceptsEveryDocumentedDensity(String density) {
        assertEquals(density, siteSettingsService.updateSettings(Map.of("layoutDensity", density)).get("layoutDensity"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "COMPACT", "wide", "tight", "loose"})
    @DisplayName("layoutDensity_RejectsAnythingElse")
    void layoutDensity_RejectsAnythingElse(String density) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("layoutDensity", density)), density);
        assertEquals(400, error.getCode(), density);
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "compact", "minimal", "off"})
    @DisplayName("homeHeroStyle_AcceptsEveryDocumentedStyle")
    void homeHeroStyle_AcceptsEveryDocumentedStyle(String style) {
        assertEquals(style, siteSettingsService.updateSettings(Map.of("homeHeroStyle", style)).get("homeHeroStyle"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "DEFAULT", "hidden", "full", "none"})
    @DisplayName("homeHeroStyle_RejectsAnythingElse")
    void homeHeroStyle_RejectsAnythingElse(String style) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("homeHeroStyle", style)), style);
        assertEquals(400, error.getCode(), style);
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "wide", "hidden"})
    @DisplayName("postCoverStyle_AcceptsEveryDocumentedStyle")
    void postCoverStyle_AcceptsEveryDocumentedStyle(String style) {
        assertEquals(style, siteSettingsService.updateSettings(Map.of("postCoverStyle", style)).get("postCoverStyle"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "DEFAULT", "full", "banner", "off"})
    @DisplayName("postCoverStyle_RejectsAnythingElse")
    void postCoverStyle_RejectsAnythingElse(String style) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("postCoverStyle", style)), style);
        assertEquals(400, error.getCode(), style);
    }

    @ParameterizedTest
    @ValueSource(strings = {"off", "subtle", "default", "lively"})
    @DisplayName("motionIntensity_AcceptsEveryDocumentedLevel")
    void motionIntensity_AcceptsEveryDocumentedLevel(String level) {
        assertEquals(level, siteSettingsService.updateSettings(Map.of("motionIntensity", level)).get("motionIntensity"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "OFF", "fast", "slow", "none"})
    @DisplayName("motionIntensity_RejectsAnythingElse")
    void motionIntensity_RejectsAnythingElse(String level) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("motionIntensity", level)), level);
        assertEquals(400, error.getCode(), level);
    }

    @ParameterizedTest
    @ValueSource(strings = {"14", "16", "19"})
    @DisplayName("baseFontSize_AcceptsInclusiveRange")
    void baseFontSize_AcceptsInclusiveRange(String value) {
        assertEquals(value, siteSettingsService.updateSettings(Map.of("baseFontSize", value)).get("baseFontSize"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"13", "20", "16.5", "+16", "16px", "", "sixteen"})
    @DisplayName("baseFontSize_RejectsOutOfRangeAndNonIntegers")
    void baseFontSize_RejectsOutOfRangeAndNonIntegers(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("baseFontSize", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "1.75", "2", "2.2"})
    @DisplayName("bodyLineHeight_AcceptsInclusiveRange")
    void bodyLineHeight_AcceptsInclusiveRange(String value) {
        assertEquals(value, siteSettingsService.updateSettings(Map.of("bodyLineHeight", value)).get("bodyLineHeight"));
    }

    /**
     * 拒绝面必须包含科学计数法与省略整数位的写法：这两个值会被前端原样拼进 CSS，
     * 校验与消费两侧对文法的理解必须一致（见 requireDecimalInRange 的 javadoc）。
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.4", "2.21", "1e1", ".5", "+1.75", "1.75em", "", "loose"})
    @DisplayName("bodyLineHeight_RejectsOutOfRangeAndNonPlainDecimals")
    void bodyLineHeight_RejectsOutOfRangeAndNonPlainDecimals(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("bodyLineHeight", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"640", "760", "960"})
    @DisplayName("readerWidth_AcceptsInclusiveRange")
    void readerWidth_AcceptsInclusiveRange(String value) {
        assertEquals(value, siteSettingsService.updateSettings(Map.of("readerWidth", value)).get("readerWidth"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"639", "961", "760.5", "760px", "0", ""})
    @DisplayName("readerWidth_RejectsOutOfRangeAndNonIntegers")
    void readerWidth_RejectsOutOfRangeAndNonIntegers(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("readerWidth", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.5", "1", "2"})
    @DisplayName("paragraphSpacing_AcceptsInclusiveRange")
    void paragraphSpacing_AcceptsInclusiveRange(String value) {
        assertEquals(value, siteSettingsService.updateSettings(Map.of("paragraphSpacing", value)).get("paragraphSpacing"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "2.1", "1e0", ".5", "1em", ""})
    @DisplayName("paragraphSpacing_RejectsOutOfRangeAndNonPlainDecimals")
    void paragraphSpacing_RejectsOutOfRangeAndNonPlainDecimals(String value) {
        var error = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("paragraphSpacing", value)), value);
        assertEquals(400, error.getCode(), value);
    }

    @Test
    @DisplayName("deepAppearanceKeys_AreAcceptedTogether")
    void deepAppearanceKeys_AreAcceptedTogether() {
        // Map.of 最多支持 10 对键值，这里有 11 个键，必须用 ofEntries。
        Map<String, String> updated = siteSettingsService.updateSettings(Map.ofEntries(
                Map.entry("fontFamily", "serif"),
                Map.entry("baseFontSize", "18"),
                Map.entry("bodyLineHeight", "1.9"),
                Map.entry("readerWidth", "820"),
                Map.entry("paragraphSpacing", "1.4"),
                Map.entry("radiusScale", "soft"),
                Map.entry("shadowDepth", "deep"),
                Map.entry("layoutDensity", "comfortable"),
                Map.entry("homeHeroStyle", "minimal"),
                Map.entry("postCoverStyle", "wide"),
                Map.entry("motionIntensity", "subtle")));

        assertEquals("serif", updated.get("fontFamily"));
        assertEquals("18", updated.get("baseFontSize"));
        assertEquals("1.9", updated.get("bodyLineHeight"));
        assertEquals("820", updated.get("readerWidth"));
        assertEquals("1.4", updated.get("paragraphSpacing"));
        assertEquals("soft", updated.get("radiusScale"));
        assertEquals("deep", updated.get("shadowDepth"));
        assertEquals("comfortable", updated.get("layoutDensity"));
        assertEquals("minimal", updated.get("homeHeroStyle"));
        assertEquals("wide", updated.get("postCoverStyle"));
        assertEquals("subtle", updated.get("motionIntensity"));
    }
}
