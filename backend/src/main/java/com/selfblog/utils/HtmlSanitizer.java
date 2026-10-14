package com.selfblog.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * HTML 内容 sanititizer 工具类
 * 用于防止 XSS 攻击，对用户输入的内容进行过滤
 */
@Component
public class HtmlSanitizer {

    /**
     * 宽松白名单：允许部分 HTML 标签（用于文章标题、摘要等富文本）
     */
    private static final Safelist RELAXED_WHITELIST = Safelist.relaxed()
            .addTags("span", "div", "hr", "table", "thead", "tbody", "tr", "th", "td")
            .addAttributes("a", "href", "title", "target")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto")
            .preserveRelativeLinks(false);

    /**
     * 严格白名单：只允许纯文本
     */
    private static final Safelist STRICT_WHITELIST = Safelist.none();

    /**
     * 过滤富文本内容（文章标题、摘要）
     * 保留基本格式标签，移除危险标签和脚本
     */
    public String sanitizeRichText(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, RELAXED_WHITELIST);
    }

    /**
     * 过滤纯文本内容
     * 完全移除所有 HTML 标签，只保留纯文本
     */
    public String sanitizePlainText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Jsoup.clean(text, STRICT_WHITELIST);
    }

    /**
     * 过滤 Markdown 内容（文章正文）
     * Markdown 不是 HTML —— 用 Jsoup 处理会破坏换行和语法。
     * 本方法只对 HTML 特殊字符进行转义（XSS 防护），保留所有换行和 Markdown 语法。
     */
    public String sanitizeMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }
        return HtmlUtils.htmlEscape(markdown);
    }
}
