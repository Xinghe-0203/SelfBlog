package com.selfblog.service.site.seo;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Static public reading only; raw author HTML is never a trusted template.
 *
 * <p>GFM 能力必须与前端 marked（{@code gfm: true}）一致：表格、删除线、任务列表在两侧
 * 都要正常渲染，否则同一篇文章在浏览器里正常、在服务端 HTML 里退化成纯文本。
 * 任务列表的复选框会被 Jsoup 清掉——与前端 DOMPurify 的 FORBID_TAGS 行为一致，
 * 两侧都只保留条目文字。安全回归由 {@code SafeMarkdownTest} 与共享 fixture 固定。
 *
 * <p>链接协议补全必须与前端 {@code frontend/src/utils/markdown.ts} 一致：CommonMark 与 marked
 * 都不会给 {@code [文字](example.com)} 这种写法补协议，而 Jsoup 的协议白名单会把没有协议的
 * href 直接删掉——服务端阅读 HTML 里就变成一个点不动的链接，与浏览器里能跳转的行为不一致。
 * 因此渲染后先按同一套「裸域名补 https://」规则修正 href，再进白名单清洗。
 */
@Component
public class SafeMarkdown {
    private static final List<Extension> EXTENSIONS = List.of(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListItemsExtension.create());

    /** 与前端 SAFE_SCHEME 对应：这些协议原样保留。 */
    private static final Pattern SAFE_SCHEME =
            Pattern.compile("^(?:https?|mailto|tel|ftp|sms|irc|news|nntp|feed|magnet):", Pattern.CASE_INSENSITIVE);

    /** 与前端 DANGEROUS_SCHEME 对应：危险协议判空，随后 Jsoup 白名单会兜底删掉 href。 */
    private static final Pattern DANGEROUS_SCHEME =
            Pattern.compile("^(?:javascript|vbscript|data|file):", Pattern.CASE_INSENSITIVE);

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@/?#]+@[^\\s@/?#]+\\.[a-zA-Z]{2,24}$");
    private static final Pattern LABEL = Pattern.compile("^[a-zA-Z0-9-]+$");
    private static final Pattern TLD = Pattern.compile("^[a-zA-Z]{2,24}$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    /** 与前端 FILE_EXTENSIONS 对应：带这些后缀的多半是相对文件引用，不该补协议。 */
    private static final Set<String> FILE_EXTENSIONS = Set.of(
            "md", "markdown", "png", "jpg", "jpeg", "gif", "webp", "svg", "avif", "bmp", "ico",
            "pdf", "txt", "json", "js", "mjs", "ts", "tsx", "jsx", "vue", "css", "scss", "html",
            "htm", "xml", "yml", "yaml", "csv", "zip", "tar", "gz", "mp4", "mov", "avi", "mp3",
            "wav", "m4a", "ogg", "exe", "apk");

    /** 正文标题降级映射，顺序从高到低（见 {@link #demoteHeadings}）。 */
    private static final List<String[]> HEADING_DEMOTION = List.of(
            new String[]{"h5", "h6"},
            new String[]{"h4", "h5"},
            new String[]{"h3", "h4"},
            new String[]{"h2", "h3"},
            new String[]{"h1", "h2"});

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    /**
     * {@code escapeHtml(false)}：**文本里的 HTML 实体不转义**，与前端的 marked 一致。
     *
     * <p>为什么必须关掉：CommonMark 默认把文本里的 {@code &} 转义成 {@code &amp;}，
     * 于是作者写的 {@code &nbsp;} 在服务端 HTML 里变成字面量「&amp;nbsp;」——
     * 读者看到的就是「&nbsp;」这七个字符，而不是一个不换行空格。
     * 而前端的 marked 保留实体原样，浏览器渲染成真正的空格：<b>同一篇文章在
     * 服务端渲染的 HTML 里与浏览器里长得不一样</b>（禁用 JS 的读者与搜索引擎
     * 看到的还是错的那一份）。关掉转义后两侧一致：{@code &nbsp;} → U+00A0、
     * {@code &amp;} → {@code &}、{@code &lt;} → 字面的 {@code <}。</p>
     *
     * <p>安全性不受影响：原始 HTML 仍会原样进入输出，但紧接着就交给
     * {@link Jsoup#clean} 的白名单清洗 —— {@code <script>}、{@code <style>}、
     * {@code onclick}、{@code style} 属性全部被剔除（与前端 DOMPurify 的
     * FORBID_TAGS / FORBID_ATTR 同等效果）。本类的职责边界一直是
     *「Jsoup 白名单是唯一信任的净化器」。</p>
     */
    private final HtmlRenderer renderer = HtmlRenderer.builder().extensions(EXTENSIONS)
            .escapeHtml(false).sanitizeUrls(true)
            /**
             * 软换行渲染成 {@code <br />}：与前端 marked 的 {@code breaks: true} 对齐。
             *
             * <p>CommonMark 默认的软换行是「输出一个换行符」，而 HTML 里的换行符
             * 只是空白，会被折叠成空格——于是服务端把「第一行\n第二行」渲染成**一行**，
             * 前端却渲染成**两行**。同一篇文章在服务端 HTML 与浏览器里长得不一样。
             * 这里显式指定 {@code <br />}，两侧就一致了。</p>
             */
            .softbreak("<br />\n")
            .build();
    private final Safelist allow = Safelist.relaxed().preserveRelativeLinks(true)
            .addTags("del")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    public String render(String source) {
        if (source == null || source.isEmpty()) return "";
        Document doc = Jsoup.parseBodyFragment(renderer.render(parser.parse(source)));
        normalizeLinks(doc);
        demoteHeadings(doc);
        // Jsoup needs an absolute base to check relative URL protocols; keep their original spelling.
        return Jsoup.clean(doc.body().html(), "https://selfblog.invalid/", allow);
    }

    /** 把「缺协议的裸域名」补成 https://，其余 href 原样返回；危险协议交给白名单清掉。 */
    private void normalizeLinks(Document doc) {
        for (Element anchor : doc.select("a[href]")) {
            String href = normalizeHref(anchor.attr("href"));
            if (href.isEmpty()) anchor.removeAttr("href");
            else anchor.attr("href", href);
        }
    }

    /**
     * 正文标题统一降一级（{@code #}→{@code h2}、{@code ##}→{@code h3} …，{@code h6} 封顶）。
     *
     * <p>每个阅读页已经有**一个** {@code h1}（文章标题）。正文里的 {@code #} 若也输出成
     * {@code h1}，服务端 HTML 里就有两个一级标题，文档大纲会被读成「并列的两个主题」。
     * 降级后 {@code h1} 只剩页面标题，正文从 {@code h2} 起，相对层级不变。</p>
     *
     * <p>必须与前端 {@code utils/markdown.ts} 的 {@code demoteMarkdownHeadings} 一致，
     * 否则服务端 HTML 与浏览器里的大纲不同。处理顺序从 {@code h5} 向下降到 {@code h1}：
     * 先动「源级别更深」的，新产生的 {@code h5} 不会被 {@code h5 → h6} 再动一次。</p>
     */
    private static void demoteHeadings(Document doc) {
        for (String[] pair : HEADING_DEMOTION) {
            for (Element heading : new ArrayList<>(doc.select(pair[0]))) {
                heading.tagName(pair[1]);
            }
        }
    }

    static String normalizeHref(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "";
        if (DANGEROUS_SCHEME.matcher(value).find()) return "";
        if (SAFE_SCHEME.matcher(value).find()) return value;
        if (value.startsWith("//") || value.startsWith("#") || value.startsWith("/")
                || value.startsWith("?") || value.startsWith(".")) return value;
        if (EMAIL.matcher(value).matches()) return "mailto:" + value;
        if (looksLikeBareHost(value)) return "https://" + value;
        return value;
    }

    private static boolean looksLikeBareHost(String value) {
        if (WHITESPACE.matcher(value).find()) return false;
        String authority = value.split("[/?#]", 2)[0];
        if (authority.isEmpty() || authority.contains("@")) return false;
        String[] labels = authority.split("\\.");
        if (labels.length < 2) return false;
        if (labels[0].length() < 2) return false;
        String last = labels[labels.length - 1];
        if (!TLD.matcher(last).matches()) return false;
        if (FILE_EXTENSIONS.contains(last.toLowerCase(Locale.ROOT))) return false;
        for (String label : labels) {
            if (!LABEL.matcher(label).matches()) return false;
        }
        return true;
    }
}
