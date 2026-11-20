package com.selfblog.service.site.seo;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafeMarkdownTest {
    private final SafeMarkdown markdown = new SafeMarkdown();
    @Test void retainsSemanticHeadingsCodeAndRelativeReadingLinks() {
        var doc = Jsoup.parse(markdown.render("## 标题\n\n[文章](/post/1)\n\n```java\nSystem.out.println(1);\n```"));
        assertEquals("标题", doc.selectFirst("h3").text());
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

    /**
     * HTML 实体必须两侧一致：{@code &nbsp;} 渲染成不换行空格，{@code &amp;} 渲染成
     * {@code &}，{@code &lt;} 渲染成字面的 {@code <}。
     *
     * <p>修复前 CommonMark 的 {@code escapeHtml(true)} 把文本里的 {@code &} 转义成
     * {@code &amp;}，服务端 HTML 里就出现字面量「&nbsp;」—— 与前端 marked 的结果
     * 不一致（浏览器渲染成空格，服务端渲染成七个字符）。</p>
     */
    @Test void decodesHtmlEntitiesTheSameWayTheClientDoes() {
        // 用独立的小样本而不是整篇 fixture：fixture 里故意把实体写在行内代码里
        // （`&nbsp;`），那里面显示字面量是**正确**的（代码段就该显示源文），
        // 混进来会让断言失去意义。
        String html = markdown.render("A&nbsp;B\n\nA&amp;B\n\nA&lt;B");
        // 实体不能被**二次转义**：`&nbsp;` 必须原样进入 HTML（浏览器渲染成不换行空格），
        // 而不是变成 `&amp;nbsp;`（浏览器渲染成字面的「&nbsp;」）。
        assertTrue(html.contains("A&nbsp;B"), "应保留 &nbsp; 实体");
        assertFalse(html.contains("&amp;nbsp;"), "不应把 &nbsp; 二次转义");
        assertTrue(html.contains("A&amp;B"), "应保留 &amp; 实体");
        assertFalse(html.contains("&amp;amp;"), "不应把 &amp; 二次转义");
        assertTrue(html.contains("A&lt;B"), "应保留 &lt; 实体");
        assertFalse(html.contains("&amp;lt;"), "不应把 &lt; 二次转义");
        // 解码后的文本：&amp; → &，&lt; → <
        var doc = Jsoup.parse(html);
        assertTrue(doc.text().contains("&"), "应包含解码后的 &");
        assertTrue(doc.text().contains("<"), "应包含解码后的 <");
    }

    /**
     * 正文标题整体降一级：{@code #}→{@code h2}、{@code ##}→{@code h3} …，{@code h6} 封顶。
     * 阅读页的 {@code h1} 只属于文章标题，否则服务端 HTML 里会有两个一级标题
     * （文档大纲被读成「并列的两个主题」）。与前端 {@code demoteMarkdownHeadings} 同策略。
     */
    @Test void demotesBodyHeadingsSoThePageKeepsOneH1() {
        String html = markdown.render("# 一级\n\n## 二级\n\n### 三级\n\n#### 四级\n\n##### 五级\n\n###### 六级");
        var doc = Jsoup.parse(html);
        assertTrue(doc.select("h1").isEmpty(), "正文里不允许再出现 h1");
        assertEquals("一级", doc.selectFirst("h2").text());
        assertEquals("二级", doc.selectFirst("h3").text());
        assertEquals("三级", doc.selectFirst("h4").text());
        assertEquals("四级", doc.selectFirst("h5").text());
        // 五级与六级都落到 h6（封顶，不再继续下沉）
        assertEquals(2, doc.select("h6").size());
    }

    /**
     * 软换行必须渲染成换行（与前端 marked 的 {@code breaks: true} 一致）。
     *
     * <p>CommonMark 默认的软换行是「输出一个换行符」，而 HTML 里的换行符只是空白，
     * 会被折叠成空格 —— 服务端把两行渲染成一行，前端渲染成两行。</p>
     */
    @Test void rendersSoftLineBreaksAsLineBreaks() {
        String html = markdown.render("第一行\n第二行");
        assertTrue(html.contains("<br"), "软换行应渲染成 <br>");
        assertTrue(html.contains("第一行") && html.contains("第二行"));
    }

    /**
     * 行内代码里的实体显示为**字面量**（代码段就该显示源文），两侧行为一致。
     */
    @Test void keepsEntitiesLiteralInsideCodeSpans() {
        String html = markdown.render("行内 `&nbsp;` 代码");
        assertTrue(html.contains("<code>&amp;nbsp;</code>"), "代码段内应转义成字面量");
    }

    /**
     * 关闭实体转义后，原始 HTML 仍然必须被白名单清洗干净：
     * {@code <script>} 与事件属性不能进到输出里。
     */
    @Test void rawHtmlIsStillStrippedAfterDisablingEntityEscaping() {
        String html = markdown.render("前文 <script>alert(1)</script> 后文 <div onclick=\"x()\">文本</div>");
        assertFalse(html.contains("script"), "不应保留 script 标签");
        assertFalse(html.contains("onclick"), "不应保留事件属性");
        assertTrue(html.contains("文本"), "正文内容应保留");
    }
}
