package com.selfblog.service.site.seo;

import com.selfblog.dto.site.PageModels.PageConfig;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 构建服务端阅读区域（禁用 JavaScript 时也能读取正文与链接）。
 *
 * <p>所有作者数据都通过 Jsoup 的 text/attr 写入，绝不拼接原始 HTML；
 * 链接只允许站内相对路径或 http(s) 绝对地址。样式仅以局部 class/变量表达，
 * 不覆盖全站字号、导航或已批准比例。
 */
public class ReadingContent {
    private static final Pattern ACCENT = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final Set<String> BACKGROUNDS = Set.of("plain", "soft", "paper");
    private static final Set<String> WIDTHS = Set.of("narrow", "standard", "wide");
    private static final Set<String> DENSITIES = Set.of("compact", "comfortable");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 返回完整的阅读容器；Vue 挂载时会整体替换它。 */
    public Element render(SeoModels.ReadingPage page) {
        Element root = new Element(Tag.valueOf("div"), "").addClass("seo-reading");
        root.attr("data-seo-reading", "");
        applyStyle(root, page.config());
        if (!page.navigation().isEmpty()) root.appendChild(navigation(page.navigation()));
        Element main = root.appendElement("main").addClass("seo-main");
        switch (page.kind()) {
            case HOME -> home(main, page);
            case ARCHIVES -> list(main, page, "文章归档");
            case ABOUT -> about(main, page);
            case GALLERY -> gallery(main, page);
            case SEARCH -> search(main, page);
            case POST -> post(main, page);
            case TAG -> list(main, page, page.displayTitle());
            case NOT_FOUND -> notFound(main, page);
            default -> { }
        }
        return root;
    }

    private void home(Element main, SeoModels.ReadingPage page) {
        heading(main, page.displayTitle(), page.meta().description());
        String eyebrow = text(page.config(), "eyebrow");
        if (module(page.config(), "hero") && eyebrow != null && !eyebrow.isBlank()) {
            main.appendElement("p").addClass("seo-eyebrow").text(eyebrow);
        }
        String listTitle = text(page.config(), "listTitle");
        appendEntries(main, page, listTitle == null || listTitle.isBlank() ? "最新文章" : listTitle);
        Element footer = main.appendElement("p").addClass("seo-hint");
        footer.appendElement("a").attr("href", "/about").text("关于");
        footer.appendText(" · ");
        footer.appendElement("a").attr("href", "/sitemap.xml").text("站点地图");
    }

    private void list(Element main, SeoModels.ReadingPage page, String listTitle) {
        heading(main, page.displayTitle(), page.meta().description());
        appendEntries(main, page, listTitle);
        main.appendElement("p").addClass("seo-hint")
                .appendElement("a").attr("href", "/").text("返回首页");
    }

    private void about(Element main, SeoModels.ReadingPage page) {
        heading(main, page.displayTitle(), page.meta().description());
        if (module(page.config(), "author") && page.site() != null) {
            Element author = main.appendElement("section").addClass("seo-author");
            author.appendElement("h2").text(page.site().author());
            if (page.site().authorBio() != null && !page.site().authorBio().isBlank()) {
                author.appendElement("p").text(page.site().authorBio());
            }
        }
        appendBody(main, page.bodyHtml());
        if (module(page.config(), "site") && page.site() != null) {
            Element site = main.appendElement("section").addClass("seo-site");
            site.appendElement("h2").text(page.site().title());
            if (page.site().description() != null && !page.site().description().isBlank()) {
                site.appendElement("p").text(page.site().description());
            }
        }
        if (module(page.config(), "links", true)) {
            Element links = main.appendElement("nav").addClass("seo-links");
            links.attr("aria-label", "继续阅读");
            for (String[] link : List.of(new String[] { "/archives", "文章归档" }, new String[] { "/gallery", "相册" }, new String[] { "/search", "搜索文章" })) {
                links.appendElement("a").attr("href", link[0]).text(link[1]);
            }
        }
    }

    private void gallery(Element main, SeoModels.ReadingPage page) {
        heading(main, page.displayTitle(), text(page.config(), "description", page.meta().description()));
        if (page.entries().isEmpty()) {
            main.appendElement("p").addClass("seo-empty").text("相册还是空的。");
            return;
        }
        Element list = main.appendElement("ul").addClass("seo-entries");
        for (SeoModels.Entry entry : page.entries()) {
            Element item = list.appendElement("li");
            item.appendElement("strong").text(entry.title());
            if (module(page.config(), "count", true) && entry.summary() != null) {
                item.appendElement("span").addClass("seo-count").text(entry.summary());
            }
        }
    }

    private void search(Element main, SeoModels.ReadingPage page) {
        heading(main, page.displayTitle(), text(page.config(), "description", page.meta().description()));
        String placeholder = text(page.config(), "placeholder");
        if (placeholder != null && !placeholder.isBlank()) main.appendElement("p").addClass("seo-hint").text(placeholder);
        String empty = text(page.config(), "empty");
        if (empty != null && !empty.isBlank()) main.appendElement("p").addClass("seo-hint").text(empty);
    }

    private void post(Element main, SeoModels.ReadingPage page) {
        Element header = main.appendElement("header").addClass("seo-post-header");
        header.appendElement("h1").text(page.displayTitle());
        if (module(page.config(), "meta", true)) {
            Element meta = header.appendElement("p").addClass("seo-post-meta");
            if (page.meta().author() != null) meta.appendElement("span").text(page.meta().author());
            if (page.meta().publishedTime() != null) {
                if (meta.childNodeSize() > 0) meta.appendText(" · ");
                Element time = meta.appendElement("time");
                time.attr("datetime", page.meta().publishedTime());
                time.text(datePart(page.meta().publishedTime()));
            }
            if (!page.meta().tags().isEmpty()) {
                if (meta.childNodeSize() > 0) meta.appendText(" · ");
                meta.appendElement("span").text(String.join("、", page.meta().tags()));
            }
        }
        if (module(page.config(), "cover", true) && safeUrl(page.meta().image()) != null) {
            main.appendElement("img").addClass("seo-cover").attr("src", page.meta().image()).attr("alt", "");
        }
        appendBody(main, page.bodyHtml());
    }

    private void notFound(Element main, SeoModels.ReadingPage page) {
        heading(main, page.displayTitle(), page.meta().description());
        Element links = main.appendElement("nav").addClass("seo-links");
        links.attr("aria-label", "返回");
        if (module(page.config(), "homeLink", true)) links.appendElement("a").attr("href", "/").text("返回首页");
        if (module(page.config(), "search", true)) links.appendElement("a").attr("href", "/search").text("搜索文章");
    }

    private Element navigation(List<SeoModels.NavLink> links) {
        Element header = new Element(Tag.valueOf("header"), "").addClass("seo-nav");
        Element nav = header.appendElement("nav");
        nav.attr("aria-label", "站点导航");
        for (SeoModels.NavLink link : links) {
            Element anchor = nav.appendElement("a").text(link.label());
            String url = safeUrl(link.to());
            if (url != null) anchor.attr("href", url);
        }
        return header;
    }

    private void heading(Element main, String title, String description) {
        Element header = main.appendElement("header").addClass("seo-heading");
        header.appendElement("h1").text(title == null || title.isBlank() ? "页面" : title);
        if (description != null && !description.isBlank()) header.appendElement("p").text(description);
    }

    private void appendEntries(Element main, SeoModels.ReadingPage page, String listTitle) {
        if (page.entries().isEmpty()) {
            main.appendElement("p").addClass("seo-empty").text("这里还没有发布内容。");
            return;
        }
        main.appendElement("h2").text(listTitle);
        Element list = main.appendElement("ul").addClass("seo-entries");
        for (SeoModels.Entry entry : page.entries()) {
            Element item = list.appendElement("li");
            Element anchor = item.appendElement("a").text(entry.title());
            String url = safeUrl(entry.url());
            if (url != null) anchor.attr("href", url);
            if (entry.date() != null) {
                Element time = item.appendElement("time");
                time.attr("datetime", entry.date());
                time.text(entry.date());
            }
            if (entry.summary() != null && !entry.summary().isBlank()) item.appendElement("p").text(entry.summary());
        }
    }

    private void appendBody(Element main, String html) {
        if (html == null || html.isBlank()) return;
        Element body = main.appendElement("div").addClass("seo-body");
        // html 已由 SafeMarkdown 白名单净化；仅取片段子节点，避免嵌套文档结构。
        var fragment = org.jsoup.Jsoup.parseBodyFragment(html).body();
        for (Element child : List.copyOf(fragment.children())) body.appendChild(child);
    }

    private void applyStyle(Element root, PageConfig config) {
        if (config == null || config.style() == null) return;
        String accent = config.style().accent();
        if (accent != null && ACCENT.matcher(accent).matches()) root.attr("style", "--seo-accent:" + accent + ";");
        choice(root, "seo-bg-", config.style().background(), BACKGROUNDS);
        choice(root, "seo-width-", config.style().width(), WIDTHS);
        choice(root, "seo-density-", config.style().density(), DENSITIES);
    }

    private void choice(Element root, String prefix, String value, Set<String> allowed) {
        if (value != null && allowed.contains(value)) root.addClass(prefix + value);
    }

    private static boolean module(PageConfig config, String key) {
        return module(config, key, true);
    }

    private static boolean module(PageConfig config, String key, boolean fallback) {
        if (config == null || config.modules() == null) return fallback;
        Map<String, Boolean> modules = config.modules();
        return modules.get(key) == null ? fallback : Boolean.TRUE.equals(modules.get(key));
    }

    private static String text(PageConfig config, String key) {
        return text(config, key, null);
    }

    private static String text(PageConfig config, String key, String fallback) {
        if (config == null || config.text() == null) return fallback;
        String value = config.text().get(key);
        return value == null ? fallback : value;
    }

    /** 只接受站内相对路径或 http(s) 绝对地址；其余（javascript:、data:、协议相对）返回 null。 */
    static String safeUrl(String value) {
        if (value == null || value.isBlank() || value.startsWith("//")) return null;
        if (value.startsWith("/")) {
            return value.contains("\\") || value.contains("..") || value.chars().anyMatch(c -> Character.isISOControl(c) || Character.isWhitespace(c)) ? null : value;
        }
        try {
            var uri = java.net.URI.create(value);
            if (uri.getHost() != null && uri.getUserInfo() == null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) return value;
        } catch (IllegalArgumentException ignored) { }
        return null;
    }

    static String datePart(String value) {
        if (value == null) return null;
        LocalDateTime parsed = null;
        try {
            parsed = LocalDateTime.parse(value);
        } catch (RuntimeException ignored) { }
        return parsed == null ? (value.length() >= 10 ? value.substring(0, 10) : value) : DATE.format(parsed);
    }
}
