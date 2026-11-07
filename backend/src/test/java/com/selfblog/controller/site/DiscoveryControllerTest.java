package com.selfblog.controller.site;

import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.service.post.BlogPostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.reading-template=static/reading-fixture.html",
        "app.api-prefix=/api",
        "app.single-author-mode=true",
        "SITE_BASE_URL=https://blog.example.com"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DiscoveryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.selfblog.service.site.SiteSettingsService settings;
    @Autowired org.springframework.cache.CacheManager caches;
    @Autowired BlogPostService blogPostService;
    // 订阅源全文渲染走阅读页同一入口；用 spy 数真实渲染次数来断言缓存命中（返回值本身无法区分）。
    @SpyBean com.selfblog.service.site.seo.SafeMarkdown markdown;

    @BeforeEach void clearCache() {
        var cache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) cache.clear();
        // /sitemap.xml → getManifest 现在走 pageCache；本类用 SQL 删 page.% 行且事务回滚，
        // 缓存不参与回滚，需与 siteSettingsCache 一起清。
        var pageCache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.PAGE_CACHE);
        if (pageCache != null) pageCache.clear();
        // feedCache 同理：订阅源整段正文按「站点源:格式」缓存，测试数据全部回滚而缓存不回滚。
        var feedCache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.FEED_CACHE);
        if (feedCache != null) feedCache.clear();
    }

    @AfterEach void clearCacheAfter() { clearCache(); }

    @BeforeEach void data() {
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_tag");
        jdbc.update("DELETE FROM site_setting WHERE setting_key IN ('adminPath') OR setting_key LIKE 'page.%'");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,create_time) VALUES(991001,1,'一号文章','摘要一','正文一',1,0,TIMESTAMP '2026-10-10 09:00:00')");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(991002,1,'草稿','不可见',0,0,TIMESTAMP '2026-10-11 09:00:00')");
        jdbc.update("INSERT INTO blog_tag(id,name,is_deleted) VALUES(992001,'技术',0)");
    }

    @Test void robotsFollowsTheConfiguredAdminPathAndCanonicalOrigin() throws Exception {
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /admin")))
                .andExpect(content().string(containsString("Disallow: /search")))
                .andExpect(content().string(containsString("Sitemap: https://blog.example.com/sitemap.xml")));
        settings.updateSettings(java.util.Map.of("adminPath", "author"));
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /author")))
                .andExpect(content().string(not(containsString("Disallow: /admin"))));
    }

    @Test void sitemapIndexListsRealShardsAndServesThem() throws Exception {
        String index = mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn().getResponse().getContentAsString();
        assertTrue(index.contains("https://blog.example.com/sitemap-site.xml"));
        assertTrue(index.contains("https://blog.example.com/sitemap-posts-1.xml"));
        assertTrue(index.contains("https://blog.example.com/sitemap-tags-1.xml"));
        assertFalse(index.contains("sitemap-pages-1.xml"));

        String site = mvc.perform(get("/sitemap-site.xml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(site.contains("<loc>https://blog.example.com/</loc>"));
        assertTrue(site.contains("https://blog.example.com/about"));
        assertFalse(site.contains("/search"));

        String posts = mvc.perform(get("/sitemap-posts-1.xml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(posts.contains("https://blog.example.com/post/991001"));
        assertFalse(posts.contains("991002"));

        mvc.perform(get("/sitemap-posts-2.xml")).andExpect(status().isNotFound());
        mvc.perform(get("/sitemap-unknown-1.xml")).andExpect(status().isNotFound());
    }

    @Test void feedsKeepTheirOwnHundredItemLimitAndPublishedOnlyContent() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        for (long id = 1000000; id < 1000110; id++) {
            rows.add(new Object[] { id, "批量文章" + id });
        }
        jdbc.batchUpdate("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(?,1,?,'正文',1,0,TIMESTAMP '2026-10-12 09:00:00')", rows);
        String feed = mvc.perform(get("/feed.xml")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(100, feed.split("<item>", -1).length - 1);
        assertFalse(feed.contains("991002"));
        assertTrue(feed.contains("https://blog.example.com/post/"));
    }

    @Test void adminConfiguredDomainTakesEffectImmediatelyAndFallsBackWhenCleared() throws Exception {
        settings.updateSettings(java.util.Map.of("siteBaseUrl", "https://custom.example.com"));

        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sitemap: https://custom.example.com/sitemap.xml")));
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("content=\"https://custom.example.com\"")))
                .andExpect(content().string(not(containsString("blog.example.com"))));
        mvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(containsString("https://custom.example.com/sitemap-site.xml")));

        settings.updateSettings(java.util.Map.of("siteBaseUrl", ""));
        mvc.perform(get("/robots.txt"))
                .andExpect(content().string(containsString("Sitemap: https://blog.example.com/sitemap.xml")));
    }

    // ------------------------------------------------------------------
    // 全文订阅源（RSS content:encoded / Atom content type="html"）
    // ------------------------------------------------------------------

    @Test void rssFeedCarriesFullRenderedHtmlAndSplitsCdataTerminators() throws Exception {
        // 正文里带字面 ]]>（代码围栏内）：未拆分会让 CDATA 提前终止、整份 XML 解析失败
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,create_time) "
                        + "VALUES(991003,1,'代码文章','摘要甲',?,1,0,TIMESTAMP '2026-10-13 09:00:00')",
                "段落文字\n\n```text\nBefore ]]> After\n```\n");
        // 已发布但软删除的文章同样不能进全文订阅（摘要与正文都不得出现）
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,create_time) "
                + "VALUES(991004,1,'被删文章','不可见正文','不可见正文',1,1,TIMESTAMP '2026-10-14 09:00:00')");

        MvcResult result = mvc.perform(get("/feed.xml")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
                .andReturn();
        String feed = utf8(result);

        assertTrue(feed.contains("xmlns:content=\"http://purl.org/rss/1.0/modules/content/\""),
                "content:encoded 命名空间必须声明在 rss 根上");
        assertTrue(feed.contains("<content:encoded><![CDATA["));

        Document rss = parseXml(feed);
        assertEquals("rss", rss.getDocumentElement().getTagName());
        assertEquals("2.0", rss.getDocumentElement().getAttribute("version"));

        NodeList bodies = rss.getElementsByTagName("content:encoded");
        assertEquals(2, bodies.getLength(), "已发布且未删除的文章 = 991001 + 991003");
        boolean codeItemFound = false;
        for (int i = 0; i < bodies.getLength(); i++) {
            String html = bodies.item(i).getTextContent();
            if (!html.contains("Before")) continue;
            codeItemFound = true;
            assertTrue(html.contains("<pre><code"), "围栏代码块必须渲染成 pre/code（与阅读页同一条 SafeMarkdown 路径）");
            assertTrue(html.contains("After"));
            assertTrue(html.contains("<p>段落文字</p>"));
            // 摘要 description 原样保留，正文走新的 content:encoded
            assertTrue(html.contains("Before"));
        }
        assertTrue(codeItemFound, "991003 的全文必须出现在 content:encoded 里");
        // 草稿（991002 正文「不可见」）与软删除（991004 摘要/正文「不可见正文」）都不得泄漏
        assertFalse(feed.contains("不可见"));
        assertFalse(feed.contains("被删文章"));

        // CDATA 拆分契约直接钉死：载荷里的 ]]> 必须拆成 ]]]]><![CDATA[>
        assertEquals("<![CDATA[a ]]]]><![CDATA[> b]]>", SeoController.cdata("a ]]> b"));
        assertEquals("<![CDATA[plain]]>", SeoController.cdata("plain"));
    }

    @Test void atomFeedEscapesRenderedHtmlContent() throws Exception {
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) "
                        + "VALUES(991005,1,'脚本文章',?,1,0,TIMESTAMP '2026-10-15 09:00:00')",
                "A & B\n\n<script>alert(1)</script>\n");

        MvcResult result = mvc.perform(get("/atom.xml")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/atom+xml"))
                .andReturn();
        String atom = utf8(result);

        assertTrue(atom.contains("<content type=\"html\">"));
        // 双层转义的精确形状：SafeMarkdown 把 <script> 转成 HTML 实体文本（&lt;script&gt;），
        // XML 层再把每个 & 升级成 &amp; —— 原样 &lt;script&gt; 在 XML 里必须是 &amp;lt;script&amp;gt;
        assertTrue(atom.contains("&amp;lt;script&amp;gt;"), "content type=html 必须是 XML 转义后的渲染 HTML");

        Document doc = parseXml(atom);
        assertEquals("feed", doc.getDocumentElement().getTagName());
        assertEquals(0, doc.getElementsByTagName("script").getLength(), "任何层都不允许出现活的 script 元素");

        NodeList contents = doc.getElementsByTagName("content");
        boolean scriptItemFound = false;
        for (int i = 0; i < contents.getLength(); i++) {
            String html = contents.item(i).getTextContent();
            if (!html.contains("alert(1)")) continue;
            scriptItemFound = true;
            assertTrue(html.contains("&lt;script&gt;"), "HTML 载荷里 script 是被转义的文本");
            assertTrue(html.contains("&amp;"), "正文里的 & 以 HTML 实体形式保留");
            assertFalse(html.contains("<script>"));
        }
        assertTrue(scriptItemFound, "991005 的全文必须出现在 atom content 里");
    }

    @Test void feedCacheServesSecondRequestWithoutRerendering() throws Exception {
        // data() 里只有 991001 一篇已发布 → 首次构建每个格式各渲染一次
        mvc.perform(get("/feed.xml")).andExpect(status().isOk());
        mvc.perform(get("/atom.xml")).andExpect(status().isOk());
        verify(markdown, times(2)).render(ArgumentMatchers.anyString());

        clearInvocations(markdown);
        mvc.perform(get("/feed.xml")).andExpect(status().isOk())
                .andExpect(content().string(containsString("一号文章")));
        mvc.perform(get("/atom.xml")).andExpect(status().isOk());
        verify(markdown, never()).render(ArgumentMatchers.anyString());

        // 键确实带站点源：两种格式的键并存于 feedCache
        var feedCache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.FEED_CACHE);
        assertNotNull(feedCache.get("https://blog.example.com:rss"), "feedCache 键必须是 <站点源>:rss");
        assertNotNull(feedCache.get("https://blog.example.com:atom"), "feedCache 键必须是 <站点源>:atom");
    }

    @Test void feedCacheEvictsOnPostMutationSoTheNextFeedReflectsTheChange() throws Exception {
        String before = utf8(mvc.perform(get("/feed.xml")).andExpect(status().isOk()).andReturn());
        assertTrue(before.contains("一号文章"));
        assertTrue(before.contains("<p>正文一</p>"));

        PostCreateRequest change = new PostCreateRequest();
        change.setId(991001L);
        change.setTitle("一号文章改版");
        change.setContent("改版后的正文");
        blogPostService.updatePost(change, 1L, true);

        String after = utf8(mvc.perform(get("/feed.xml")).andExpect(status().isOk()).andReturn());
        assertTrue(after.contains("一号文章改版"), "updatePost 必须驱逐 feedCache，下一次请求重建");
        assertTrue(after.contains("改版后的正文"));
        assertFalse(after.contains("正文一"));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** feed 响应声明 charset=UTF-8；按字节显式解码，避免容器默认编码差异。 */
    private static String utf8(MvcResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
}
