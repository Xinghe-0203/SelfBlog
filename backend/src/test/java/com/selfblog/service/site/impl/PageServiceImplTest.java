package com.selfblog.service.site.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.*;
import com.selfblog.mapper.SiteSettingMapper;
import com.selfblog.entity.SiteSetting;
import com.selfblog.service.site.PageService;
import com.selfblog.service.site.SiteSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PageServiceImplTest {
    @Autowired PageService pages;
    @Autowired SiteSettingMapper mapper;
    @Autowired SiteSettingsService settings;
    @Autowired ObjectMapper json;
    @Autowired CacheManager caches;

    @BeforeEach @AfterEach void clearSiteCache() {
        var cache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) cache.clear();
    }

    PageConfig config(Map<String, Object> values) { return json.convertValue(values, PageConfig.class); }
    void rejects(int code, Runnable action) {
        assertEquals(code, assertThrows(BusinessException.class, action::run).getCode());
    }

    @Test void defaultsDoNotWriteOrOverrideGlobalAppearance() {
        long count = mapper.selectCount(null);
        assertEquals(8, pages.listPages().size());
        assertNull(pages.getPublished("about").config().title());
        assertEquals(List.of("home", "archives", "gallery", "about"),
                pages.getManifest().navigation().stream().map(PublicNavigationItem::pageId).toList());
        assertEquals(count, mapper.selectCount(null));
    }

    @Test void unknownStoredSchemaFailsClosedInsteadOfReturningRawData() {
        var row = new SiteSetting(); row.setSettingKey("page.published.about");
        row.setSettingValue("{\"schemaVersion\":2,\"revision\":1,\"everPublished\":true,\"config\":{\"title\":\"不兼容的数据\"}}");
        mapper.insert(row);
        rejects(500, () -> pages.getPublished("about"));
        rejects(500, () -> pages.getManifest());
    }

    @Test void legacyPagesAdminEntryRequiresMigrationInsteadOfBeingSeized() {
        var existing = mapper.selectById("adminPath");
        if (existing == null) {
            existing = new SiteSetting(); existing.setSettingKey("adminPath"); existing.setSettingValue("pages"); mapper.insert(existing);
        } else { existing.setSettingValue("pages"); mapper.updateById(existing); }
        rejects(409, () -> pages.createPage("projects", "项目"));
        assertEquals("pages", settings.getSettings().get("adminPath"));
    }

    @Test void draftContentNeverLeaksThroughPublicReads() {
        pages.saveDraft("about", 0, null, config(Map.of("title", "未公开", "body", "保密草稿")));
        assertNull(pages.getPublished("about").config().title());
        assertTrue(pages.getManifest().pages().stream().allMatch(p -> p.config().body() == null));
        assertFalse(settings.getPublicSettings().keySet().stream().anyMatch(k -> k.startsWith("page.")));
    }

    @Test void publishingCopiesOnlyTheSavedRevisionAndExplicitEmptySurvives() {
        var saved = pages.saveDraft("about", 0, null, config(Map.of("title", "关于我", "description", "", "body", "## 自我介绍")));
        var published = pages.publish("about", saved.draft().revision());
        assertEquals("", pages.getPublished("about").config().description());
        assertEquals("## 自我介绍", pages.getPublished("about").config().body());
        pages.saveDraft("about", published.draft().revision(), null, config(Map.of("title", "下一稿")));
        assertEquals("关于我", pages.getPublished("about").config().title());
    }

    @Test void staleWritesAndPublicationConflictRatherThanOverwrite() {
        var saved = pages.saveDraft("about", 0, null, config(Map.of("title", "第一稿")));
        rejects(409, () -> pages.saveDraft("about", 0, null, config(Map.of("title", "过期窗口"))));
        rejects(409, () -> pages.publish("about", 0));
        assertEquals(saved.draft().revision(), pages.getEditor("about").draft().revision());
    }

    @Test void customPageLifecycleKeepsPublicAndDraftSeparate() {
        var created = pages.createPage("my-projects", "项目");
        rejects(404, () -> pages.getBySlug("my-projects"));
        var published = pages.publish(created.id(), created.draft().revision());
        assertEquals("项目", pages.getBySlug("my-projects").config().title());
        var offline = pages.unpublish(created.id(), published.draft().revision());
        rejects(404, () -> pages.getBySlug("my-projects"));
        assertTrue(offline.draft().everPublished());
        pages.deletePage(created.id(), offline.draft().revision());
        rejects(404, () -> pages.getEditor(created.id()));
        assertNotNull(pages.createPage("my-projects", "可重新使用地址"));
    }

    @Test void slugReservationsAreUniqueAndRenamesOnlyWorkBeforeFirstPublication() {
        var created = pages.createPage("my-projects", "项目");
        rejects(409, () -> pages.createPage("my-projects", "重复地址"));
        var renamed = pages.saveDraft(created.id(), created.draft().revision(), "new-projects", created.draft().config());
        assertEquals(created.id(), renamed.id());
        assertNotNull(pages.createPage("my-projects", "地址已释放"));
        var published = pages.publish(created.id(), renamed.draft().revision());
        rejects(400, () -> pages.saveDraft(created.id(), published.draft().revision(), "third-projects", published.draft().config()));
    }

    @Test void navigationOnlyAcceptsPublishedTargetsAndFiltersOfflinePages() {
        var created = pages.createPage("my-projects", "项目");
        var links = List.of(new NavigationItem("home", "主页"), new NavigationItem(created.id(), "作品"));
        rejects(400, () -> pages.saveNavigation(0, links));
        var published = pages.publish(created.id(), created.draft().revision());
        var navigation = pages.saveNavigation(0, links);
        assertEquals("/pages/my-projects", pages.getManifest().navigation().get(1).to());
        rejects(409, () -> pages.saveNavigation(0, links));
        pages.unpublish(created.id(), published.draft().revision());
        assertEquals(List.of("home"), pages.getManifest().navigation().stream().map(PublicNavigationItem::pageId).toList());
        assertEquals(List.of("home"), pages.getNavigation().items().stream().map(NavigationItem::pageId).toList());
        assertEquals(1, navigation.revision());
    }

    @Test void navigationCannotRemoveHomeDuplicateTargetsOrLinkDynamicTemplates() {
        rejects(400, () -> pages.saveNavigation(0, List.of(new NavigationItem("about", "关于"))));
        rejects(400, () -> pages.saveNavigation(0, List.of(new NavigationItem("home", "首页"), new NavigationItem("home", "重复"))));
        rejects(400, () -> pages.saveNavigation(0, List.of(new NavigationItem("home", "首页"), new NavigationItem("post", "文章模板"))));
    }

    @Test void builtinsCannotBeDeletedUnpublishedOrRenamed() {
        rejects(400, () -> pages.deletePage("about", 0));
        rejects(400, () -> pages.unpublish("home", 0));
        rejects(400, () -> pages.saveDraft("about", 0, "other-slug", config(Map.of())));
    }

    @Test void resetIsOnlyADraftUntilPublished() {
        var saved = pages.saveDraft("about", 0, null, config(Map.of("title", "个性化")));
        var published = pages.publish("about", saved.draft().revision());
        var reset = pages.saveDraft("about", published.draft().revision(), null, config(Map.of()));
        assertEquals("个性化", pages.getPublished("about").config().title());
        pages.publish("about", reset.draft().revision());
        assertNull(pages.getPublished("about").config().title());
    }

    @Test void rejectsUnrecognizedModulesTextKeysAndInvalidAppearance() {
        rejects(400, () -> pages.saveDraft("about", 0, null, config(Map.of("modules", Map.of("executeJs", true)))));
        rejects(400, () -> pages.saveDraft("about", 0, null, config(Map.of("text", Map.of("unknown", "x")))));
        rejects(400, () -> pages.saveDraft("about", 0, null, config(Map.of("style", Map.of("width", "999999px")))));
        rejects(400, () -> pages.saveDraft("about", 0, null, config(Map.of("style", Map.of("accent", "red;display:none")))));
    }

    @Test void acceptsEveryBuiltinModuleAndAllowsExplicitFalse() {
        Map<String, List<String>> modules = Map.of(
            "home", List.of("hero", "cover", "author", "tags", "archiveLink"),
            "about", List.of("author", "site", "links"),
            "archives", List.of("category", "count"),
            "gallery", List.of("description", "count"),
            "search", List.of("count"),
            "post", List.of("cover", "meta", "toc", "adjacent"),
            "tag", List.of("count"),
            "notFound", List.of("homeLink", "search"));
        modules.forEach((id, keys) -> keys.forEach(key -> {
            var editor = pages.getEditor(id);
            var result = pages.saveDraft(id, editor.draft().revision(), null, config(Map.of("modules", Map.of(key, false))));
            assertEquals(false, result.draft().config().modules().get(key));
        }));
    }

    @Test void validatesTextBodyLengthAndSerializedUtf8Limit() {
        rejects(400, () -> pages.saveDraft("about", 0, null, config(Map.of("body", "a".repeat(10001)))));
        rejects(400, () -> pages.saveDraft("about", 0, null, config(Map.of("body", "\u0001".repeat(10000)))));
        rejects(400, () -> pages.createPage("../bad", "不合法"));
        rejects(400, () -> pages.createPage("repeated--dash", "不合法"));
        rejects(400, () -> pages.createPage("good-slug", ""));
    }

    @Test void rejectsUnsafeAndMalformedShareImages() {
        for (String url : List.of("javascript:alert(1)", "//evil.example/image", "/uploads/../secret", "https://")) {
            rejects(400, () -> pages.saveDraft("about", 0, null, config(Map.of("seo", Map.of("image", url)))));
        }
    }

    @Test void cannotForceIndexSearchNotFoundOrReplaceDynamicIdentity() {
        rejects(400, () -> pages.saveDraft("search", 0, null, config(Map.of("seo", Map.of("indexable", true)))));
        rejects(400, () -> pages.saveDraft("notFound", 0, null, config(Map.of("seo", Map.of("indexable", true)))));
        rejects(400, () -> pages.saveDraft("post", 0, null, config(Map.of("title", "同一个文章标题"))));
        rejects(400, () -> pages.saveDraft("tag", 0, null, config(Map.of("title", "同一个标签标题"))));
    }

    // ---------- 页面横幅（首页介绍区 / 关于页作者卡） ----------

    /** 造一份完整横幅；字段名与前端 PageBanner 逐字一致。 */
    private Map<String, Object> banner(String image) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("image", image);
        values.put("fit", "cover");
        values.put("position", "center");
        values.put("height", "standard");
        values.put("overlay", 30);
        values.put("tone", "light");
        values.put("layout", "split");
        return values;
    }

    private PageConfig bannerConfig(Map<String, Object> banner) {
        return config(Map.of("style", Map.of("banner", banner)));
    }

    @Test void bannerSurvivesTheDraftAndPublishRoundTrip() {
        var saved = pages.saveDraft("home", 0, null, bannerConfig(banner("/uploads/site-assets/banner-home.jpg")));
        pages.publish("home", saved.draft().revision());
        var published = pages.getPublished("home").config().style().banner();
        assertNotNull(published);
        assertEquals("/uploads/site-assets/banner-home.jpg", published.image());
        assertEquals("cover", published.fit());
        assertEquals("center", published.position());
        assertEquals("standard", published.height());
        assertEquals(Integer.valueOf(30), published.overlay());
        assertEquals("light", published.tone());
        assertEquals("split", published.layout());
        // 编辑器读回的已发布版本也必须带着横幅，不能被 sanitize 吃掉。
        assertEquals("/uploads/site-assets/banner-home.jpg",
                pages.getEditor("home").published().config().style().banner().image());
    }

    @Test void bannerIsAllowedOnTheAboutPage() {
        var saved = pages.saveDraft("about", 0, null, bannerConfig(banner("https://cdn.example.com/about.jpg")));
        pages.publish("about", saved.draft().revision());
        assertEquals("https://cdn.example.com/about.jpg",
                pages.getPublished("about").config().style().banner().image());
    }

    @Test void bannerIsRejectedOnEveryPageOtherThanHomeAndAbout() {
        for (String id : List.of("archives", "gallery", "search", "post", "tag", "notFound")) {
            var error = assertThrows(BusinessException.class,
                    () -> pages.saveDraft(id, 0, null, bannerConfig(banner("/uploads/site-assets/banner.jpg"))), id);
            assertEquals(400, error.getCode(), id);
            assertEquals("此页面不支持自定义横幅", error.getMessage(), id);
        }
        var created = pages.createPage("my-projects", "项目");
        var error = assertThrows(BusinessException.class,
                () -> pages.saveDraft(created.id(), created.draft().revision(), null,
                        config(Map.of("title", "项目", "style", Map.of("banner", banner("/uploads/site-assets/banner.jpg"))))));
        assertEquals(400, error.getCode());
        assertEquals("此页面不支持自定义横幅", error.getMessage());
    }

    @Test void bannerRejectsUnknownStyleValues() {
        for (Map.Entry<String, String> field : List.of(
                Map.entry("fit", "fill"),
                Map.entry("position", "left"),
                Map.entry("height", "huge"),
                Map.entry("tone", "neon"),
                Map.entry("layout", "diagonal"))) {
            Map<String, Object> values = new LinkedHashMap<>(banner("/uploads/site-assets/banner.jpg"));
            values.put(field.getKey(), field.getValue());
            var error = assertThrows(BusinessException.class,
                    () -> pages.saveDraft("home", 0, null, bannerConfig(values)), field.getValue());
            assertEquals(400, error.getCode(), field.getValue());
            assertEquals("请选择有效的横幅样式", error.getMessage(), field.getValue());
        }
    }

    @Test void bannerOverlayIsLimitedToZeroThroughEighty() {
        for (int overlay : List.of(-1, 81, 200)) {
            Map<String, Object> values = new LinkedHashMap<>(banner("/uploads/site-assets/banner.jpg"));
            values.put("overlay", overlay);
            var error = assertThrows(BusinessException.class,
                    () -> pages.saveDraft("home", 0, null, bannerConfig(values)), String.valueOf(overlay));
            assertEquals(400, error.getCode());
            assertEquals("横幅遮罩需为 0-80 之间的整数", error.getMessage());
        }
        long revision = 0;
        for (int overlay : List.of(0, 40, 80)) {
            Map<String, Object> values = new LinkedHashMap<>(banner("/uploads/site-assets/banner.jpg"));
            values.put("overlay", overlay);
            var saved = pages.saveDraft("home", revision, null, bannerConfig(values));
            revision = saved.draft().revision();
            assertEquals(Integer.valueOf(overlay), saved.draft().config().style().banner().overlay());
        }
    }

    @Test void bannerImageReusesTheSharedImageUrlRules() {
        for (String url : List.of("javascript:alert(1)", "//evil.example/banner.jpg", "/uploads/../secret.jpg",
                "/uploads/%2e%2e/secret.jpg", "https://")) {
            var error = assertThrows(BusinessException.class,
                    () -> pages.saveDraft("home", 0, null, bannerConfig(banner(url))), url);
            assertEquals(400, error.getCode(), url);
            assertEquals("分享图需为合法http(s)地址或站内/uploads/路径", error.getMessage(), url);
        }
    }

    @Test void anEmptyBannerObjectIsTreatedAsNotConfigured() {
        // 前端表单可能给所有页面都塞一个空 banner；它不该把无关页面拦下来。
        var saved = pages.saveDraft("archives", 0, null, bannerConfig(Map.of()));
        assertNull(saved.draft().config().style().banner().image());
        var home = pages.saveDraft("home", 0, null, bannerConfig(Map.of()));
        assertNull(home.draft().config().style().banner().image());
    }

    @Test void pagesWithoutABannerKeepTheirBannerFreeStyle() {
        var saved = pages.saveDraft("home", 0, null, config(Map.of("style", Map.of("accent", "#165DFF"))));
        assertNull(saved.draft().config().style().banner());
        pages.publish("home", saved.draft().revision());
        assertNull(pages.getPublished("home").config().style().banner());
        assertEquals("#165DFF", pages.getPublished("home").config().style().accent());
    }
}
