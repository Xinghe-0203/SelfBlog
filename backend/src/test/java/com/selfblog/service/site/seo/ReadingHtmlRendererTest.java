package com.selfblog.service.site.seo;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.PageBanner;
import com.selfblog.dto.site.PageModels.PageConfig;
import com.selfblog.dto.site.PageModels.PageSeo;
import com.selfblog.dto.site.PageModels.PageStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReadingHtmlRendererTest {
    private final ReadingHtmlRenderer renderer = new ReadingHtmlRenderer(new ReadingTemplate("static/reading-fixture.html"));
    private final String base = "https://blog.example.com";

    private SeoModels.Meta meta(String title, String path, boolean indexable) {
        return new SeoModels.Meta(title, "页面描述", "/uploads/a.jpg", "website", path, null, null, List.of(), indexable,
                List.of(), null);
    }
    private SeoModels.ReadingPage page(SeoModels.Kind kind, String title, String path, String body, boolean indexable) {
        return new SeoModels.ReadingPage(200, kind, kind == SeoModels.Kind.POST ? "post" : "about", PageConfig.empty(),
                meta(title, path, indexable), title, body, List.of(), List.of(),
                new SeoModels.SiteInfo("示例博客", "站点说明", "示例作者", "作者简介"));
    }

    /** 带横幅的阅读页：样式里只多一个 banner，其余走默认。 */
    private SeoModels.ReadingPage pageWithBanner(SeoModels.Kind kind, String title, String path, PageBanner banner) {
        var config = new PageConfig(null, null, null, null, null,
                new PageStyle(null, null, null, null, banner), null);
        return new SeoModels.ReadingPage(200, kind, kind == SeoModels.Kind.POST ? "post" : "about", config,
                meta(title, path, true), title, "", List.of(), List.of(),
                new SeoModels.SiteInfo("示例博客", "站点说明", "示例作者", "作者简介"));
    }

    @Test void readingDocumentReusesBuiltAssetsAndReplacesHeadOnce() {
        Document doc = Jsoup.parse(renderer.render(page(SeoModels.Kind.POST, "真实标题", "/post/1", "<p>正文段落</p>", true), base));
        assertEquals(1, doc.select("#app").size());
        assertTrue(doc.selectFirst("#app article, #app main").text().contains("正文段落"));
        assertEquals("/assets/index-abc123.js", doc.selectFirst("script[src]").attr("src"));
        assertTrue(doc.selectFirst("link[rel=stylesheet]").attr("href").contains("/assets/index-abc123.css"));
        assertEquals(1, doc.select("link[rel=canonical]").size());
        assertEquals("https://blog.example.com/post/1", doc.selectFirst("link[rel=canonical]").attr("href"));
        assertEquals(1, doc.select("meta[property=og:title]").size());
        assertEquals(1, doc.select("meta[name=description]").size());
        assertEquals(0, doc.select("meta[property=og:description][content='旧的通用描述']").size());
        assertEquals("index,follow", doc.selectFirst("meta[name=robots]").attr("content"));
        assertEquals("https://blog.example.com/favicon.svg", base + doc.selectFirst("link[rel=icon]").attr("href"));
        assertTrue(doc.selectFirst("link[rel=alternate]").attr("href").contains("/feed.xml"));
    }

    @Test void structuredDataIsSafeJsonWithoutBreakingOutOfScript() {
        var unsafe = new SeoModels.Meta("标题</script><script>alert(1)</script>", "描述", null, "article", "/post/2", "2026-10-16T10:00:00", "作者", List.of("技术"), true,
                List.of(), null);
        var post = new SeoModels.ReadingPage(200, SeoModels.Kind.POST, "post", PageConfig.empty(), unsafe, "标题</script>", "<p>x</p>", List.of(), List.of(),
                new SeoModels.SiteInfo("示例博客", "站点说明", "示例作者", "作者简介"));
        Document doc = Jsoup.parse(renderer.render(post, base));
        assertEquals(1, doc.select("script[type=application/ld+json]").size());
        String json = doc.selectFirst("script[type=application/ld+json]").data();
        assertFalse(json.contains("</script"));
        assertTrue(json.contains("\\u003c"));
        assertEquals("https://blog.example.com/post/2", doc.selectFirst("link[rel=canonical]").attr("href"));
        assertEquals("article", doc.selectFirst("meta[property=og:type]").attr("content"));
    }

    @Test void noindexDocumentsNeverClaimIndexPermission() {
        Document search = Jsoup.parse(renderer.render(page(SeoModels.Kind.SEARCH, "搜索文章", "/search", "", false), base));
        assertEquals("noindex,follow", search.selectFirst("meta[name=robots]").attr("content"));
        Document shell = Jsoup.parse(renderer.render(page(SeoModels.Kind.SHELL, "示例博客", null, "", false), base));
        assertEquals("noindex,follow", shell.selectFirst("meta[name=robots]").attr("content"));
        assertEquals(0, shell.select(".seo-reading").size());
        assertEquals(0, shell.select("link[rel=canonical]").size());
    }

    @Test void missingTemplateFailsClosedAs503WithSafeErrorDocument() {
        var missing = new ReadingHtmlRenderer(new ReadingTemplate("static/does-not-exist.html"));
        assertEquals(503, assertThrows(BusinessException.class, () -> missing.render(page(SeoModels.Kind.HOME, "首页", "/", "", true), base)).getCode());
        Document error = Jsoup.parse(missing.error(503));
        assertEquals("noindex,follow", error.selectFirst("meta[name=robots]").attr("content"));
        assertFalse(error.text().contains("Exception"));
        assertTrue(error.select("script").isEmpty());
    }

    @Test void configuredOriginWinsOverTemplateOnlyOnce() {
        Document doc = Jsoup.parse(renderer.render(page(SeoModels.Kind.HOME, "首页", "/", "", true), "https://public.example.com"));
        assertEquals("https://public.example.com/", doc.selectFirst("link[rel=canonical]").attr("href"));
        assertEquals(1, doc.select("meta[name=selfblog-site-origin]").size());
        assertEquals("https://public.example.com", doc.selectFirst("meta[name=selfblog-site-origin]").attr("content"));
    }

    /** 首页声明 WebSite + SearchAction，让搜索引擎知道站内搜索入口（/search 本身 noindex）。 */
    @Test void homePageAdvertisesWebSiteWithSearchAction() {
        Document doc = Jsoup.parse(renderer.render(page(SeoModels.Kind.HOME, "首页", "/", "<p>欢迎</p>", true), base));
        assertEquals(1, doc.select("script[type=application/ld+json]").size());
        String json = doc.selectFirst("script[type=application/ld+json]").data();
        assertTrue(json.contains("\"@type\":\"WebSite\""));
        assertTrue(json.contains("\"name\":\"示例博客\""));
        assertTrue(json.contains("\"url\":\"https://blog.example.com/\""));
        assertTrue(json.contains("\"potentialAction\""));
        assertTrue(json.contains("https://blog.example.com/search?q={search_term_string}"));
        assertTrue(json.contains("required name=search_term_string"));
    }

    /** 阅读 HTML 必须保留模板里的首屏主题脚本，否则深色读者打开页脚会先闪白再翻黑。 */
    @Test void inlineThemeBootScriptSurvivesRendering() {
        Document doc = Jsoup.parse(renderer.render(page(SeoModels.Kind.POST, "标题", "/post/1", "<p>x</p>", true), base));
        var inline = doc.select("script:not([src]):not([type=application/ld+json])");
        assertEquals(1, inline.size());
        assertTrue(inline.first().data().contains("edu_theme"));
    }

    /** 自定义关键词优先，渲染为 meta[name=keywords]，逗号分隔。 */
    @Test void keywordsMetaUsesCustomKeywords() {
        var withKeywords = new SeoModels.Meta("标题", "描述", "/uploads/a.jpg", "article", "/post/3", null, null,
                List.of("技术"), true, List.of("Spring Boot", "事务", "SEO"), null);
        var post = new SeoModels.ReadingPage(200, SeoModels.Kind.POST, "post", PageConfig.empty(), withKeywords,
                "标题", "<p>x</p>", List.of(), List.of(),
                new SeoModels.SiteInfo("示例博客", "站点说明", "示例作者", "作者简介"));
        Document doc = Jsoup.parse(renderer.render(post, base));
        assertEquals(1, doc.select("meta[name=keywords]").size());
        assertEquals("Spring Boot, 事务, SEO", doc.selectFirst("meta[name=keywords]").attr("content"));
    }

    /** 没有自定义关键词时回落到文章标签，保证老文章也有 keywords。 */
    @Test void keywordsMetaFallsBackToTags() {
        var withTags = new SeoModels.Meta("标题", "描述", "/uploads/a.jpg", "article", "/post/4", null, null,
                List.of("技术", "后端"), true, List.of(), null);
        var post = new SeoModels.ReadingPage(200, SeoModels.Kind.POST, "post", PageConfig.empty(), withTags,
                "标题", "<p>x</p>", List.of(), List.of(),
                new SeoModels.SiteInfo("示例博客", "站点说明", "示例作者", "作者简介"));
        Document doc = Jsoup.parse(renderer.render(post, base));
        assertEquals("技术, 后端", doc.selectFirst("meta[name=keywords]").attr("content"));
    }

    /** 两者都没有时不该输出空 keywords 标签。 */
    @Test void keywordsMetaOmittedWhenEmpty() {
        var empty = new SeoModels.Meta("标题", "描述", "/uploads/a.jpg", "article", "/post/5", null, null,
                List.of(), true, List.of(), null);
        var post = new SeoModels.ReadingPage(200, SeoModels.Kind.POST, "post", PageConfig.empty(), empty,
                "标题", "<p>x</p>", List.of(), List.of(),
                new SeoModels.SiteInfo("示例博客", "站点说明", "示例作者", "作者简介"));
        Document doc = Jsoup.parse(renderer.render(post, base));
        assertEquals(0, doc.select("meta[name=keywords]").size());
    }

    /** 分享图优先用 post_seo.og_image_url，正文封面（hero）不受影响。 */
    @Test void shareImageOverridesOpenGraphImageOnly() {
        var withShare = new SeoModels.Meta("标题", "描述", "/uploads/cover.jpg", "article", "/post/6", null, null,
                List.of(), true, List.of(), "/uploads/share.png");
        var post = new SeoModels.ReadingPage(200, SeoModels.Kind.POST, "post", PageConfig.empty(), withShare,
                "标题", "<p>x</p>", List.of(), List.of(),
                new SeoModels.SiteInfo("示例博客", "站点说明", "示例作者", "作者简介"));
        Document doc = Jsoup.parse(renderer.render(post, base));
        assertEquals("https://blog.example.com/uploads/share.png",
                doc.selectFirst("meta[property=og:image]").attr("content"));
        assertEquals("https://blog.example.com/uploads/share.png",
                doc.selectFirst("meta[name=twitter:image]").attr("content"));
        assertEquals("/uploads/cover.jpg",
                doc.selectFirst("#app img").attr("src"));
    }

    // ---------- 页面横幅 ----------

    /** 去掉空格再比对 style，避免 Jsoup 序列化时在冒号后补空格。 */
    private String compactStyle(Element root) {
        return root.attr("style").replace(" ", "");
    }

    /** 首页介绍区的横幅：图片进 DOM（爬虫可见），裁切/遮罩等进受控 class 与 CSS 变量。 */
    @Test void homeBannerRendersTheImageAndTheControlledClasses() {
        var banner = new PageBanner("/uploads/site-assets/banner-home.jpg", "cover", "center", "standard", 30, "light", "split");
        Document doc = Jsoup.parse(renderer.render(pageWithBanner(SeoModels.Kind.HOME, "首页", "/", banner), base));
        var root = doc.selectFirst(".seo-reading");
        assertNotNull(root);
        assertEquals(1, doc.select(".seo-banner").size());
        assertEquals("/uploads/site-assets/banner-home.jpg", doc.selectFirst(".seo-banner-image").attr("src"));
        assertTrue(root.hasClass("seo-banner-fit-cover"));
        assertTrue(root.hasClass("seo-banner-position-center"));
        assertTrue(root.hasClass("seo-banner-height-standard"));
        assertTrue(root.hasClass("seo-banner-overlay-30"));
        assertTrue(root.hasClass("seo-banner-tone-light"));
        assertTrue(root.hasClass("seo-banner-layout-split"));
        String style = compactStyle(root);
        assertTrue(style.contains("--seo-banner-image:url("), style);
        assertTrue(style.contains("/uploads/site-assets/banner-home.jpg"), style);
        assertTrue(style.contains("--seo-banner-overlay:30"), style);
    }

    /** 关于页的横幅落在作者卡里，让爬虫在作者介绍上方看到图片。 */
    @Test void aboutBannerRendersInsideTheAuthorCard() {
        var banner = new PageBanner("https://cdn.example.com/about.jpg", "contain", "bottom", "tall", 0, "dark", "stacked");
        Document doc = Jsoup.parse(renderer.render(pageWithBanner(SeoModels.Kind.ABOUT, "关于", "/about", banner), base));
        assertEquals(1, doc.select(".seo-author .seo-banner").size());
        assertEquals("https://cdn.example.com/about.jpg", doc.selectFirst(".seo-author .seo-banner-image").attr("src"));
        var root = doc.selectFirst(".seo-reading");
        assertTrue(root.hasClass("seo-banner-fit-contain"));
        assertTrue(root.hasClass("seo-banner-layout-stacked"));
        assertTrue(root.hasClass("seo-banner-overlay-0"));
    }

    /** 没有横幅时不许凭空多出横幅元素或变量。 */
    @Test void pagesWithoutABannerRenderNoBannerMarkup() {
        Document doc = Jsoup.parse(renderer.render(page(SeoModels.Kind.HOME, "首页", "/", "", true), base));
        assertEquals(0, doc.select(".seo-banner").size());
        var root = doc.selectFirst(".seo-reading");
        assertFalse(root.hasClass("seo-banner-fit-cover"));
        assertFalse(compactStyle(root).contains("--seo-banner-image"));
    }

    /** 图片地址里带括号/引号时只渲染图片，绝不写进 CSS 变量（防 CSS 注入）。 */
    @Test void bannerImageThatCouldBreakCssIsNeverWrittenIntoAStyleVariable() {
        var banner = new PageBanner("/uploads/banner).png", "cover", "center", "standard", 20, "auto", "split");
        Document doc = Jsoup.parse(renderer.render(pageWithBanner(SeoModels.Kind.HOME, "首页", "/", banner), base));
        var root = doc.selectFirst(".seo-reading");
        assertNotNull(root);
        assertFalse(compactStyle(root).contains("--seo-banner-image"), compactStyle(root));
        assertTrue(root.hasClass("seo-banner-fit-cover"));
        assertTrue(root.hasClass("seo-banner-overlay-20"));
    }
}
