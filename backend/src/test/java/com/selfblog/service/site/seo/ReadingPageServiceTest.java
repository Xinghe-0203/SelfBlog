package com.selfblog.service.site.seo;

import com.selfblog.dto.site.PageModels.PageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class ReadingPageServiceTest {
    @Autowired ReadingPageService reading;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.selfblog.service.site.PageService pages;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired org.springframework.cache.CacheManager caches;
    @Autowired com.selfblog.mapper.SiteSettingMapper settingMapper;
    @Autowired com.selfblog.mapper.PostSeoMapper postSeoMapper;

    /** 直接写 SQL 绕过服务层时缓存不会自动失效；与 PageServiceImplTest 相同，测试前后清缓存。 */
    void clearSiteCache() {
        var cache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) cache.clear();
        // 本类用 SQL 删 page.% 行且测试事务回滚，pageCache（公开页面读）不参与回滚。
        var pageCache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.PAGE_CACHE);
        if (pageCache != null) pageCache.clear();
    }

    @BeforeEach void data() {
        clearSiteCache();
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM post_seo");
        jdbc.update("DELETE FROM blog_tag");
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM photo_album");
        jdbc.update("DELETE FROM site_setting WHERE setting_key LIKE 'page.%'");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,create_time,view_count) VALUES(770001,1,'公开文章','公开摘要','## 正文标题\n\n正文内容段落。',1,0,TIMESTAMP '2026-10-15 10:00:00',3)");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(770002,1,'未发布草稿','秘密内容',0,0,TIMESTAMP '2026-10-16 10:00:00')");
        jdbc.update("INSERT INTO blog_tag(id,name,is_deleted) VALUES(660001,'技术',0)");
        jdbc.update("INSERT INTO blog_post_tag(id,post_id,tag_id,is_deleted) VALUES(1,770001,660001,0)");
        jdbc.update("INSERT INTO photo_album(id,title,description,is_private,password_hash,sort_order,create_time,is_deleted) VALUES(550001,'旅行',NULL,0,NULL,0,TIMESTAMP '2026-10-01 10:00:00',0)");
        jdbc.update("INSERT INTO photo_album(id,title,description,is_private,password_hash,sort_order,create_time,is_deleted) VALUES(550002,'私密',NULL,1,'x',1,TIMESTAMP '2026-10-01 10:00:00',0)");
    }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); clearSiteCache(); }

    @Test void publishedPostProvidesFullBodyButDraftStayHiddenEvenFromAdmin() {
        var post = reading.resolve("/post/770001", null);
        assertEquals(200, post.status());
        assertEquals(SeoModels.Kind.POST, post.kind());
        assertTrue(post.bodyHtml().contains("正文内容段落"));
        assertTrue(post.meta().indexable());
        assertEquals("公开文章", post.displayTitle());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        assertEquals(404, reading.resolve("/post/770002", null).status());
        assertEquals(404, reading.resolve("/post/999", null).status());
    }

    @Test void tagsIndexListsTagsWithPublishedPostCounts() {
        // 标签索引页：公开可读、可收录，条目是标签 + 已发布文章数。
        var page = reading.resolve("/tags", null);
        assertEquals(200, page.status());
        assertEquals(SeoModels.Kind.TAGS, page.kind());
        assertTrue(page.meta().indexable());
        assertEquals("/tags", page.meta().path());
        assertFalse(page.entries().isEmpty());
        var entry = page.entries().stream().filter(item -> "技术".equals(item.title())).findFirst().orElseThrow();
        assertEquals("/tag/660001", entry.url());
        // 「技术」下只有一篇已发布文章（770002 是草稿，不计入）。
        assertTrue(entry.summary().contains("1"));
    }

    /**
     * 站点主色作为「页面未单独指定强调色」时的缺省值：服务端首屏的导航链接、
     * 引用块竖线与行内代码都用它，写死 #165DFF 会让改过主色的站点先闪一下蓝色。
     */
    @Test void siteAccentFillsInWhenThePageHasNone() {
        // 写入必须走 MyBatis mapper：JdbcTemplate 的写不会让 MyBatis 的本地会话缓存失效，
        // 同一个事务里再读 mapper 仍会拿到旧行（真实存在的坑，见 docs/agents/backend.md）。
        writePrimaryColor("#2d7c54");
        clearSiteCache();
        assertEquals("#2d7c54", siteAccent(), "站点主色应成为缺省强调色");

        // 脏值不写进样式：非法颜色必须被丢弃（不会污染 --seo-accent）
        writePrimaryColor("red");
        clearSiteCache();
        String dirty = siteAccent();
        assertTrue(dirty == null || dirty.isBlank(), "非法颜色不应写进样式，实际=" + dirty);
    }

    /** 经 MyBatis 写 themePrimaryColor（存在则更新，否则插入）。 */
    private void writePrimaryColor(String value) {
        var row = settingMapper.selectById("themePrimaryColor");
        var setting = row != null ? row : new com.selfblog.entity.SiteSetting();
        setting.setSettingKey("themePrimaryColor");
        setting.setSettingValue(value);
        if (row != null) settingMapper.updateById(setting);
        else settingMapper.insert(setting);
    }

    /** 首页配置里的强调色（style 可能为 null）。 */
    private String siteAccent() {
        var style = reading.resolve("/", null).config().style();
        return style == null ? null : style.accent();
    }

    /**
     * 空标签页保持可访问，但必须 noindex：只有标题的清单页是薄内容，
     * 收录它只会稀释整站质量。挂上第一篇文章后自动恢复 index。
     */
    @Test void emptyTagPageStaysAccessibleButIsNotIndexable() {
        jdbc.update("INSERT INTO blog_tag(id,name,is_deleted) VALUES(660003,'空标签页',0)");

        var empty = reading.resolve("/tag/660003", null);
        assertEquals(200, empty.status(), "空标签页仍然可访问（作者可能刚建好就分享链接）");
        assertFalse(empty.meta().indexable(), "空标签页不应被收录");
        assertTrue(empty.entries().isEmpty());

        var filled = reading.resolve("/tag/660001", null);
        assertTrue(filled.meta().indexable(), "有已发布文章的标签页必须可收录");
    }

    /**
     * 0 篇已发布文章的标签不进公开索引：它们点进去是空白页（薄内容），
     * 但仍留在后台「标签管理」里可管理。计数本身走同一条聚合查询。
     */
    @Test void tagsIndexSkipsTagsWithoutPublishedPosts() {
        jdbc.update("INSERT INTO blog_tag(id,name,is_deleted) VALUES(660002,'空标签',0)");
        var page = reading.resolve("/tags", null);
        assertEquals(200, page.status());
        assertTrue(page.entries().stream().anyMatch(item -> "技术".equals(item.title())));
        assertFalse(page.entries().stream().anyMatch(item -> "空标签".equals(item.title())),
                "0 篇已发布文章的标签不应出现在公开索引里");
    }

    @Test void aboutUsesPublishedMarkdownAndCustomPageLifecycle() {
        pages.saveDraft("about", 0, null, config(Map.of("title", "关于我", "body", "## 独立介绍\n\n详情段落。")));
        pages.publish("about", 1);
        var about = reading.resolve("/about", null);
        assertEquals(200, about.status());
        assertTrue(about.bodyHtml().contains("独立介绍"));
        assertEquals("关于我", about.displayTitle());
        var created = pages.createPage("projects", "我的项目");
        assertEquals(404, reading.resolve("/pages/projects", null).status());
        pages.saveDraft(created.id(), created.draft().revision(), null, config(Map.of("title", "我的项目", "body", "**项目**介绍。")));
        var published = pages.publish(created.id(), pages.getEditor(created.id()).draft().revision());
        var custom = reading.resolve("/pages/projects", null);
        assertEquals(200, custom.status());
        assertEquals(SeoModels.Kind.CUSTOM, custom.kind());
        assertTrue(custom.bodyHtml().contains("<strong>项目</strong>"));
        assertTrue(custom.meta().indexable());
        pages.unpublish(created.id(), published.draft().revision());
        assertEquals(404, reading.resolve("/pages/projects", null).status());
    }

    @Test void forcedNoindexAndAdminNamespaceCannotBeOverridden() {
        assertEquals(404, reading.resolve("/missing/page", null).status());
        assertEquals(false, reading.resolve("/search", null).meta().indexable());
        assertEquals(SeoModels.Kind.NOT_FOUND, reading.resolve("/missing/page", null).kind());
        assertEquals(SeoModels.Kind.SHELL, reading.resolve("/admin/pages/about", null).kind());
        assertEquals(false, reading.resolve("/admin", null).meta().indexable());
        // 通过 MyBatis 写入以触发 SqlSession 本地缓存清理（JdbcTemplate 不会）。
        settingMapper.deleteById("adminPath");
        var legacyRow = new com.selfblog.entity.SiteSetting();
        legacyRow.setSettingKey("adminPath");
        legacyRow.setSettingValue("pages");
        settingMapper.insert(legacyRow);
        clearSiteCache();
        var legacy = reading.resolve("/pages/projects", null);
        assertEquals(SeoModels.Kind.SHELL, legacy.kind());
        assertEquals(false, legacy.meta().indexable());
    }

    @Test void homePaginationIsValidatedAndCanonicalised() {
        var first = reading.resolve("/", null);
        assertEquals(200, first.status());
        assertEquals("/", first.meta().path());
        assertTrue(first.entries().stream().anyMatch(entry -> entry.url().equals("/post/770001")));
        assertEquals("/", reading.resolve("/", "page=abc").meta().path());
        assertEquals("/", reading.resolve("/", "page=0").meta().path());
        assertEquals("/", reading.resolve("/", "page=1").meta().path());
        assertEquals("/", reading.resolve("/", "tracking=1").meta().path());
        assertEquals(404, reading.resolve("/", "page=99").status());
    }

    @Test void archivesGalleryAndTagExposeOnlySafePublicData() {
        var archives = reading.resolve("/archives", null);
        assertEquals(SeoModels.Kind.ARCHIVES, archives.kind());
        assertTrue(archives.entries().stream().anyMatch(entry -> entry.url().equals("/post/770001")));
        assertFalse(archives.entries().stream().anyMatch(entry -> entry.url().contains("770002")));
        var gallery = reading.resolve("/gallery", null);
        assertEquals(SeoModels.Kind.GALLERY, gallery.kind());
        assertTrue(gallery.entries().stream().anyMatch(entry -> entry.title().equals("旅行")));
        assertTrue(gallery.entries().stream().noneMatch(entry -> entry.title().equals("私密") && entry.url() != null));
        var tag = reading.resolve("/tag/660001", null);
        assertEquals(SeoModels.Kind.TAG, tag.kind());
        assertEquals("技术", tag.displayTitle());
        assertEquals(404, reading.resolve("/tag/999", null).status());
        assertEquals(404, reading.resolve("/tag/abc", null).status());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void missingTargetsAre404EvenWithoutAnEnclosingTransaction() {
        // 真实请求没有测试事务：内层服务的 404 不得把外层事务标记为 rollback-only，
        // 否则生产环境会变成 500（本轮真实联调发现）。
        var slug = "regress-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        var created = pages.createPage(slug, "回归页面");
        String customId = null;
        try {
            customId = created.id();
            var saved = pages.saveDraft(customId, created.draft().revision(), null,
                    config(Map.of("title", "回归页面", "body", "正文")));
            var published = pages.publish(customId, saved.draft().revision());
            assertEquals(200, reading.resolve("/pages/" + slug, null).status());
            pages.unpublish(customId, published.draft().revision());
            var offline = reading.resolve("/pages/" + slug, null);
            assertEquals(404, offline.status());
            assertEquals(SeoModels.Kind.NOT_FOUND, offline.kind());
            assertEquals(404, reading.resolve("/post/424242", null).status());
            assertEquals(404, reading.resolve("/tag/424242", null).status());
        } finally {
            if (customId != null) {
                var current = pages.getEditor(customId);
                pages.deletePage(customId, current.draft().revision());
            }
            settingMapper.deleteById("page.slug." + slug);
            // NOT_SUPPORTED 测试的数据会被真实提交，逐个清理，避免污染其他测试类。
            jdbc.update("DELETE FROM blog_post_tag WHERE post_id = 770001");
            jdbc.update("DELETE FROM blog_post WHERE id IN (770001, 770002)");
            jdbc.update("DELETE FROM blog_tag WHERE id = 660001");
            jdbc.update("DELETE FROM photo_album WHERE id IN (550001, 550002)");
        }
    }

    private PageConfig config(Map<String, Object> values) { return json.convertValue(values, PageConfig.class); }

    /** 自定义 SEO 优先于派生值：标题、描述、关键词、分享图都要生效。 */
    @Test void postSeoOverridesDerivedMetadata() {
        var seo = new com.selfblog.entity.PostSeo();
        seo.setPostId(770001L);
        seo.setSeoTitle("自定义 SEO 标题");
        seo.setSeoDescription("自定义 SEO 描述，用于验证渲染优先级。");
        seo.setSeoKeywords("关键词甲,关键词乙");
        seo.setOgImageUrl("/uploads/share.png");
        seo.setSource("manual");
        postSeoMapper.insert(seo);

        var post = reading.resolve("/post/770001", null);

        assertTrue(post.meta().title().startsWith("自定义 SEO 标题"));
        assertEquals("自定义 SEO 描述，用于验证渲染优先级。", post.meta().description());
        assertEquals(List.of("关键词甲", "关键词乙"), post.meta().keywords());
        assertEquals("/uploads/share.png", post.meta().shareImage());
        // 正文标题仍是文章原标题，SEO 标题只影响 head
        assertEquals("公开文章", post.displayTitle());

        postSeoMapper.deleteById(770001L);
    }

    /** 没有 SEO 记录时回落到派生值，行为与改动前一致。 */
    @Test void derivedMetadataStillUsedWhenNoSeoRow() {
        var post = reading.resolve("/post/770001", null);

        assertEquals("公开摘要", post.meta().description());
        assertEquals(List.of("技术"), post.meta().keywords());
        assertNull(post.meta().shareImage());
        assertTrue(post.meta().title().startsWith("公开文章"));
    }

    /** 只填了部分字段时逐字段回落，不能因为有一条记录就整体放弃派生值。 */
    @Test void blankSeoFieldsFallBackPerField() {
        var seo = new com.selfblog.entity.PostSeo();
        seo.setPostId(770001L);
        seo.setSeoTitle("只改了标题");
        seo.setSource("manual");
        postSeoMapper.insert(seo);

        var post = reading.resolve("/post/770001", null);

        assertTrue(post.meta().title().startsWith("只改了标题"));
        assertEquals("公开摘要", post.meta().description());
        assertEquals(List.of("技术"), post.meta().keywords());

        postSeoMapper.deleteById(770001L);
    }

    /** 发布的横幅配置必须原样进入可抓取的阅读页：存-取往返不能丢字段。 */
    @Test void publishedBannerReachesTheCrawlableReadingPage() {
        Map<String, Object> banner = Map.<String, Object>of("image", "/uploads/site-assets/banner-home.jpg",
                "fit", "cover", "height", "tall", "overlay", 30);
        pages.saveDraft("home", 0, null, config(Map.of("style", Map.of("banner", banner))));
        pages.publish("home", 1);

        var home = reading.resolve("/", null);

        assertEquals(200, home.status());
        var published = home.config().style().banner();
        assertNotNull(published);
        assertEquals("/uploads/site-assets/banner-home.jpg", published.image());
        assertEquals("cover", published.fit());
        assertEquals("tall", published.height());
        assertEquals(Integer.valueOf(30), published.overlay());
    }
}
