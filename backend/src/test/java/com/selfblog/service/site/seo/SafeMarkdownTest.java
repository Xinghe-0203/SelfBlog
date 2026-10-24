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

    /**
     * 与前端 {@code frontend/src/utils/markdown.ts} 的 normalizeMarkdownHref 保持同一套规则：
     * CommonMark 不会给 {@code [文字](example.com)} 补协议，若不在这里补，Jsoup 的协议白名单
     * 会把 href 整条删掉，服务端阅读 HTML 里就出现点不动的链接。
     */
    @Test void completesBareHostLinksLikeTheClient() {
        var doc = Jsoup.parse(markdown.render(
                "[示例](example.com/page) [带www](www.example.com) [子域](sub.example.co.uk/a?x=1)"));
        var links = doc.select("a");
        assertEquals("https://example.com/page", links.get(0).attr("href"));
        assertEquals("https://www.example.com", links.get(1).attr("href"));
        assertEquals("https://sub.example.co.uk/a?x=1", links.get(2).attr("href"));
    }

    @Test void keepsAlreadyQualifiedAndInternalLinksUntouched() {
        var doc = Jsoup.parse(markdown.render(
                "[绝对](https://example.com/a) [站内](/post/1) [锚点](#top) [邮件](mailto:a@b.com) [裸邮箱](a@b.com)"));
        var links = doc.select("a");
        assertEquals("https://example.com/a", links.get(0).attr("href"));
        assertEquals("/post/1", links.get(1).attr("href"));
        assertEquals("#top", links.get(2).attr("href"));
        assertEquals("mailto:a@b.com", links.get(3).attr("href"));
        assertEquals("mailto:a@b.com", links.get(4).attr("href"));
    }

    @Test void doesNotMistakeRelativeFileNamesForHosts() {
        assertEquals("a.md", SafeMarkdown.normalizeHref("a.md"));
        assertEquals("notes.txt", SafeMarkdown.normalizeHref("notes.txt"));
        assertEquals("photo.png", SafeMarkdown.normalizeHref("photo.png"));
        assertEquals("single", SafeMarkdown.normalizeHref("single"));
        assertEquals("", SafeMarkdown.normalizeHref("javascript:alert(1)"));
    }

    /**
     * 与前端共享同一篇 fixture（docs/fixtures/gfm-sample.md）：表格/删除线/任务列表
     * 在服务端 HTML 里必须与浏览器里一致，不能退化成纯文本。
     */
    @Test void rendersGfmTablesStrikethroughAndTaskListsLikeTheClient() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of("..", "docs", "fixtures", "gfm-sample.md"));
        var doc = Jsoup.parse(markdown.render(source));
        assertEquals(1, doc.select("table").size());
        assertEquals(2, doc.select("thead th").size());
        assertEquals(2, doc.select("tbody tr").size());
        assertEquals("删除线", doc.selectFirst("del").text());
        assertEquals(2, doc.select("li").size());
        assertEquals("已完成的任务", doc.select("li").first().text());
        assertEquals("待办任务", doc.select("li").last().text());
        // 复选框被 Jsoup 清掉：与前端 DOMPurify 的 FORBID_TAGS 一致，只保留条目文字
        assertTrue(doc.select("input").isEmpty());
        assertEquals("引用块也必须保留。", doc.selectFirst("blockquote p").text());
        assertTrue(doc.selectFirst("pre code").text().contains("safe"));
    }
}
