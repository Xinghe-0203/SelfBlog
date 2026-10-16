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
}
