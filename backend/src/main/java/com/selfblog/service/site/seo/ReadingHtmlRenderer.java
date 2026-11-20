package com.selfblog.service.site.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把构建产物 index.html 克隆成每次请求独立的公开阅读文档。
 *
 * <p>head 中的 title/description/robots/canonical/OG/Twitter/JSON-LD 全部重建，
 * 避免旧值与服务器渲染值叠加；Vite 的哈希资源、图标与订阅发现引用原样保留。
 */
@Component
public class ReadingHtmlRenderer {

    private final ReadingTemplate template;
    private final ReadingContent content = new ReadingContent();
    private final ObjectMapper json = new ObjectMapper();

    public ReadingHtmlRenderer(ReadingTemplate template) {
        this.template = template;
    }

    public String render(SeoModels.ReadingPage page, String origin) {
        Document doc = Jsoup.parse(template.load());
        Element head = doc.head();

        head.select("title, meta[name=description], meta[name=robots], meta[name=keywords], "
                + "meta[property^=og:], meta[name^=twitter:], "
                + "link[rel=canonical], meta[name=selfblog-site-origin], script[type=application/ld+json]").remove();

        SeoModels.Meta meta = page.meta();
        head.appendElement("meta").attr("name", "selfblog-site-origin").attr("content", origin);
        head.appendElement("title").text(meta.title());
        if (meta.description() != null && !meta.description().isBlank()) {
            head.appendElement("meta").attr("name", "description").attr("content", meta.description());
        }
        // keywords：优先后台 SEO 关键词，缺省回落到文章标签；两者都空则不输出该标签。
        String keywords = joinKeywords(meta);
        if (!keywords.isEmpty()) {
            head.appendElement("meta").attr("name", "keywords").attr("content", keywords);
        }
        head.appendElement("meta").attr("name", "robots").attr("content", meta.indexable() ? "index,follow" : "noindex,follow");

        String canonical = absolute(origin, meta.path());
        if (canonical != null) head.appendElement("link").attr("rel", "canonical").attr("href", canonical);
        meta(head, "property", "og:type", meta.type() == null ? "website" : meta.type());
        meta(head, "property", "og:title", meta.title());
        if (meta.description() != null && !meta.description().isBlank()) meta(head, "property", "og:description", meta.description());
        // 分享图与阅读区头图分开：后台可以只换社交分享图而不动正文版式。
        String image = absolute(origin, shareImage(meta));
        if (image != null) meta(head, "property", "og:image", image);
        if (canonical != null) meta(head, "property", "og:url", canonical);
        meta(head, "property", "og:locale", "zh_CN");
        // 分享图的替代文字：社交平台用它决定卡片版式与无图时的回退文案。
        if (image != null) meta(head, "property", "og:image:alt", meta.title());
        meta(head, "name", "twitter:card", "summary_large_image");
        meta(head, "name", "twitter:title", meta.title());
        if (meta.description() != null && !meta.description().isBlank()) meta(head, "name", "twitter:description", meta.description());
        if (image != null) meta(head, "name", "twitter:image", image);
        if (image != null) meta(head, "name", "twitter:image:alt", meta.title());

        if (page.kind() == SeoModels.Kind.POST) appendJsonLd(head, page, canonical, image, origin);
        if (page.kind() == SeoModels.Kind.HOME) appendWebsiteJsonLd(head, page, origin);

        Element app = doc.getElementById("app");
        if (app != null && page.kind() != SeoModels.Kind.SHELL) {
            head.appendElement("link").attr("rel", "stylesheet").attr("href", "/seo-reading.css");
            app.empty();
            app.appendChild(content.render(page));
        }
        return doc.outerHtml();
    }

    /** 极简安全错误页：模板缺失或渲染失败时也返回可读、不可索引的文档。 */
    public String error(int status) {
        return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"robots\" content=\"noindex,follow\"><title>页面暂时无法显示</title></head>"
                + "<body><main><h1>页面暂时无法显示</h1><p>请稍后重试。</p></main></body></html>";
    }

    /**
     * {@code meta[name=keywords]} 的内容：后台关键词优先，否则回落到文章标签。
     * 去空、去重并保持顺序；都为空时返回空串（调用方据此决定不输出该标签）。
     */
    private static String joinKeywords(SeoModels.Meta meta) {
        List<String> source = meta.keywords() != null && !meta.keywords().isEmpty() ? meta.keywords() : meta.tags();
        if (source == null || source.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String keyword : source) {
            if (keyword == null || keyword.isBlank()) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append(keyword.trim());
        }
        return builder.toString();
    }

    /** 社交分享封面：{@code shareImage} 优先，缺省沿用阅读区头图。 */
    private static String shareImage(SeoModels.Meta meta) {
        String share = meta.shareImage();
        return share != null && !share.isBlank() ? share : meta.image();
    }

    private void appendJsonLd(Element head, SeoModels.ReadingPage page, String canonical, String image, String origin) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("@context", "https://schema.org");
        payload.put("@type", "BlogPosting");
        payload.put("headline", page.displayTitle());
        if (canonical != null) payload.put("mainEntityOfPage", canonical);
        if (image != null) payload.put("image", List.of(image));
        SeoModels.Meta meta = page.meta();
        if (meta.description() != null && !meta.description().isBlank()) payload.put("description", meta.description());
        if (meta.publishedTime() != null) payload.put("datePublished", meta.publishedTime());
        if (meta.author() != null) payload.put("author", Map.of("@type", "Person", "name", meta.author()));
        if (page.site() != null && page.site().title() != null && !page.site().title().isBlank()) {
            payload.put("publisher", Map.of("@type", "Organization", "name", page.site().title()));
        }
        Map<String, Object> breadcrumb = breadcrumbList(page, canonical, origin);
        if (breadcrumb != null) payload.put("breadcrumb", breadcrumb);
        writeJsonLd(head, payload, "selfblog-article-jsonld");
    }

    /**
     * 面包屑结构化数据（schema.org 的 {@code CreativeWork.breadcrumb}）：站点首页 → 当前文章。
     *
     * <p>两条 ListItem：position 1 为站点首页（name = 站点标题，item = {@code <origin>/}），
     * position 2 为文章本身（name = 文章标题，item = {@code <origin>/post/{id}}，即 canonical）。
     * URL 一律经 {@link SiteOrigin} 的规范 origin 绝对化，与同一块里的 canonical/mainEntityOfPage
     * 同源；origin 或 canonical 缺失时整块省略，绝不输出相对地址。
     * 站点标题未配置时只省略首页的 name，位置与 URL 仍然有效。</p>
     */
    private static Map<String, Object> breadcrumbList(SeoModels.ReadingPage page, String canonical, String origin) {
        if (canonical == null || origin == null || origin.isBlank()) return null;
        String siteTitle = page.site() == null ? null : page.site().title();
        Map<String, Object> home = new LinkedHashMap<>();
        home.put("@type", "ListItem");
        home.put("position", 1);
        if (siteTitle != null && !siteTitle.isBlank()) home.put("name", siteTitle);
        home.put("item", origin + "/");
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("@type", "ListItem");
        article.put("position", 2);
        article.put("name", page.displayTitle());
        article.put("item", canonical);
        Map<String, Object> breadcrumb = new LinkedHashMap<>();
        breadcrumb.put("@type", "BreadcrumbList");
        breadcrumb.put("itemListElement", List.of(home, article));
        return breadcrumb;
    }

    /**
     * 首页声明 WebSite + SearchAction：搜索引擎据此识别站点与站内搜索入口。
     * /search 本身保持 noindex，只把它作为可执行的搜索动作暴露。
     */
    private void appendWebsiteJsonLd(Element head, SeoModels.ReadingPage page, String origin) {
        if (origin == null || origin.isBlank()) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("@context", "https://schema.org");
        payload.put("@type", "WebSite");
        if (page.site() != null && page.site().title() != null && !page.site().title().isBlank()) {
            payload.put("name", page.site().title());
        }
        payload.put("url", origin + "/");
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("@type", "EntryPoint");
        target.put("urlTemplate", origin + "/search?q={search_term_string}");
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("@type", "SearchAction");
        action.put("target", target);
        action.put("query-input", "required name=search_term_string");
        payload.put("potentialAction", action);
        // 首页额外声明一个 ItemList：把「最新文章」这条列表本身结构化，
        // 搜索引擎可以据此把首页当成一个文章集合来理解（而不只是一个页面）。
        // 条目 URL 一律绝对化；绝对化失败（origin 缺失）的条目整条跳过。
        if (page.kind() == SeoModels.Kind.HOME && !page.entries().isEmpty()) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (SeoModels.Entry entry : page.entries()) {
                String url = absolute(origin, entry.url());
                if (url == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("@type", "ListItem");
                item.put("position", items.size() + 1);
                item.put("url", url);
                if (entry.title() != null && !entry.title().isBlank()) item.put("name", entry.title());
                items.add(item);
            }
            if (!items.isEmpty()) {
                Map<String, Object> list = new LinkedHashMap<>();
                list.put("@type", "ItemList");
                list.put("itemListElement", items);
                payload.put("mainEntity", list);
            }
        }
        writeJsonLd(head, payload, "selfblog-site-jsonld");
    }

    private void writeJsonLd(Element head, Map<String, Object> payload, String id) {
        try {
            String jsonText = json.writeValueAsString(payload)
                    .replace("<", "\\u003c")
                    .replace("\u2028", "\\u2028")
                    .replace("\u2029", "\\u2029");
            Element script = head.appendElement("script").attr("type", "application/ld+json")
                    .attr("id", id);
            script.appendChild(new org.jsoup.nodes.DataNode(jsonText));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(500, "页面元数据生成失败");
        }
    }

    private static void meta(Element head, String attr, String key, String value) {
        head.appendElement("meta").attr(attr, key).attr("content", value);
    }

    private String absolute(String origin, String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank() || origin == null) return null;
        String value = SiteOrigin.absolute(origin, pathOrUrl);
        return value.isEmpty() ? null : value;
    }
}
