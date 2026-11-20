package com.selfblog.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

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
     * 过滤 Markdown 内容（文章正文、草稿正文）。
     *
     * <p><b>正文按原样保存</b>：这里既不转义、也不删改任何字符，只做两件不影响语义的事
     * ——统一换行符（CRLF → LF）与去掉 UTF-8 BOM。</p>
     *
     * <p><b>为什么不能整篇转义。</b>曾经的实现是 {@code HtmlUtils.htmlEscape(markdown)}，
     * 把全文的 {@code & < > " '} 换成实体。那会直接破坏 Markdown 语法本身：</p>
     * <ul>
     *   <li>引用块标记 {@code >} 变成 {@code &gt;}，**不再是引用块**；</li>
     *   <li>代码块里的 {@code >} 与 {@code '} 变成 {@code &gt;} / {@code &#39;}，
     *       读者在代码块里看到的是一串实体而不是代码；</li>
     *   <li>段落里的 {@code &} 变成 {@code &amp;}。</li>
     * </ul>
     * <p>这就是「正文里直接显示 {@code &nbsp;} 这种」的根因：实体被存进了数据库，
     * 而代码块按规范**不解码**实体，于是它们就以字面量出现在读者眼前。</p>
     *
     * <p><b>安全边界在渲染时，两侧都有：</b></p>
     * <ul>
     *   <li>服务端：{@code service/site/seo/SafeMarkdown} 用 Jsoup 白名单清洗
     *       （{@code <script>}、{@code onclick}、{@code style} 属性全部剔除）；</li>
     *   <li>前端：{@code utils/markdown.ts} 用 DOMPurify 清洗（FORBID_TAGS / FORBID_ATTR）。</li>
     * </ul>
     * <p>两侧都在**渲染时**处理，因此代码块里的内容不受影响（代码段里是文本，不是标记，
     * 渲染器不会把 {@code <script>} 当成可执行标签）。这也是 GitHub 等成熟博客的做法：
     * 源文原样存，渲染时净化。</p>
     */
    public String sanitizeMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }
        // 去掉 UTF-8 BOM（从 Word 粘贴的正文常带），它会出现在第一行开头。
        String text = markdown.charAt(0) == '\uFEFF' ? markdown.substring(1) : markdown;
        // 统一换行符：CRLF 与 CR 都换成 LF，否则换行在不同系统上不一致。
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
