package com.selfblog.service.site.seo;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.PageConfig;
import com.selfblog.dto.site.PageModels.PageSeo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReadingHtmlRendererTest {
    private final ReadingHtmlRenderer renderer = new ReadingHtmlRenderer(new ReadingTemplate("static/reading-fixture.html"));
    private final String base = "https://blog.example.com";

    private SeoModels.Meta meta(String title, String path, boolean indexable) {
        return new SeoModels.Meta(title, "页面描述", "/uploads/a.jpg", "website", path, null, null, List.of(), indexable);
    }
    private SeoModels.ReadingPage page(SeoModels.Kind kind, String title, String path, String body, boolean indexable) {
        return new SeoModels.ReadingPage(200, kind, kind == SeoModels.Kind.POST ? "post" : "about", PageConfig.empty(),
                meta(title, path, indexable), title, body, List.of(), List.of(),
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
        var unsafe = new SeoModels.Meta("标题</script><script>alert(1)</script>", "描述", null, "article", "/post/2", "2026-10-16T10:00:00", "作者", List.of("技术"), true);
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
}
