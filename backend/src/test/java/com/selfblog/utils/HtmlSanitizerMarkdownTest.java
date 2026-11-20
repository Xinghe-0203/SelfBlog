package com.selfblog.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 正文（Markdown）的存储策略：**原样保存**。
 *
 * <p>曾经的实现是整篇 {@code HtmlUtils.htmlEscape}，那会把 Markdown 语法本身破坏掉：
 * 引用块标记 {@code >} 变成 {@code &gt;}（不再是引用块）、代码块里的 {@code >} 与
 * {@code '} 变成实体（读者在代码块里看到的是 {@code &gt;} 而不是代码）。
 * 「正文里直接显示 &nbsp; 这种」的根因就在这里。</p>
 *
 * <p>安全边界在渲染时（服务端 Jsoup 白名单 / 前端 DOMPurify），两侧都覆盖到了，
 * 所以存储层不需要、也不应该动语法。</p>
 */
class HtmlSanitizerMarkdownTest {
    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test void keepsMarkdownSyntaxIntact() {
        String source = "# 标题\n\n> 引用块\n\n```js\nconst a = 1 > 0 ? 'x' : 'y'\n```\n\n段落 & 符号\n";
        String stored = sanitizer.sanitizeMarkdown(source);
        // 引用块标记必须还是 `>`
        assertTrue(stored.contains("\n> 引用块"), "引用块标记不应被转义");
        // 代码块里的 `>` 与 `'` 必须原样
        assertTrue(stored.contains("const a = 1 > 0 ? 'x' : 'y'"), "代码块内容不应被转义");
        // `&` 不应变成 `&amp;`
        assertTrue(stored.contains("段落 & 符号"), "`&` 不应被转义");
        assertFalse(stored.contains("&gt;"), "不应出现 &gt;");
        assertFalse(stored.contains("&#39;"), "不应出现 &#39;");
        assertFalse(stored.contains("&amp;"), "不应出现 &amp;");
    }

    @Test void keepsRawHtmlForTheRenderersToSanitize() {
        // 原始 HTML 保留在源文里，由渲染时的白名单（Jsoup / DOMPurify）处理。
        // 提前删掉会破坏「讲安全」这类文章里的代码示例。
        String stored = sanitizer.sanitizeMarkdown("示例：<script>alert(1)</script>\n");
        assertTrue(stored.contains("<script>"), "源文应原样保留，由渲染时净化");
    }

    @Test void normalizesLineEndingsAndStripsBom() {
        assertEquals("a\nb\n", sanitizer.sanitizeMarkdown("a\r\nb\r\n"));
        assertEquals("a\nb\n", sanitizer.sanitizeMarkdown("a\nb\n"));
        assertEquals("标题", sanitizer.sanitizeMarkdown("\uFEFF标题"));
    }

    @Test void leavesEmptyInputAlone() {
        assertEquals("", sanitizer.sanitizeMarkdown(""));
        assertEquals(null, sanitizer.sanitizeMarkdown(null));
    }
}
