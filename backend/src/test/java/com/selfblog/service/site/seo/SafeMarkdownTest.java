package com.selfblog.service.site.seo;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafeMarkdownTest {
    private final SafeMarkdown markdown = new SafeMarkdown();
    @Test void retainsSemanticHeadingsCodeAndRelativeReadingLinks() {
        var doc = Jsoup.parse(markdown.render("## 标题\n\n[文章](/post/1)\n\n```java\nSystem.out.println(1);\n```"));
        assertEquals("标题", doc.selectFirst("h2").text());
        assertEquals("/post/1", doc.selectFirst("a").attr("href"));
        assertTrue(doc.selectFirst("pre code").text().contains("println"));
    }
    @Test void blocksRawExecutableHtmlAndUnsafeMarkdownUrls() {
        var doc = Jsoup.parse(markdown.render("<script>alert(1)</script><svg onload=alert(1)></svg><iframe src=x></iframe>\n\n[x](javascript:alert(1))\n\n![x](data:image/svg+xml,bad)"));
        assertTrue(doc.select("script,svg,iframe,[onload],[style]").isEmpty());
        assertTrue(doc.select("[href],[src]").stream().noneMatch(el -> el.attr("href").startsWith("javascript:") || el.attr("src").startsWith("data:")));
    }
    @Test void handlesEmptyContentAndPreservesSafeImages() {
        assertEquals("", markdown.render(null));
        var doc = Jsoup.parse(markdown.render("![说明](/uploads/a.jpg)"));
        assertEquals("/uploads/a.jpg", doc.selectFirst("img").attr("src"));
        assertEquals("说明", doc.selectFirst("img").attr("alt"));
    }
}
