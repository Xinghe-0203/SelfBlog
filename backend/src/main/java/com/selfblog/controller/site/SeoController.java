package com.selfblog.controller.site;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.BlogPost;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.service.site.seo.SafeMarkdown;
import com.selfblog.service.site.seo.SiteOrigin;
import com.selfblog.service.site.seo.SitemapService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 站点订阅源与发现文档。
 *
 * <p>刻意使用 {@link Controller} 而非 {@code @RestController}，避免被自动加上 /api 前缀：
 * 订阅器与爬虫只会访问根路径下的 /feed.xml、/atom.xml、/sitemap*.xml、/robots.txt。
 *
 * <p>对外地址统一走 {@link SiteOrigin}：显式配置 SITE_BASE_URL 后不再受伪造 Host /
 * 转发头影响。sitemap 委托给分片服务，订阅源保留各自最近 100 篇的上限。
 *
 * <p>订阅源是全文输出：每篇文章用与阅读页完全相同的数据路径（getPublishedPostDetail）
 * 和渲染入口（{@link SafeMarkdown#render}）生成 HTML——RSS 放进 {@code content:encoded}
 * 的 CDATA，Atom 放进 {@code content type="html"} 的转义文本。整段订阅正文按「站点源 + 格式」
 * 缓存在 {@link CaffeineCacheConfig#FEED_CACHE}（5 分钟 TTL，文章写路径全部失效），
 * 因为一次重建意味着百篇详情查询加百次渲染。
 */
@Controller
public class SeoController {

    /** 已发布状态。见 CLAUDE.md：用户文章与动态发布即 status=1，无待审流程。 */
    private static final int STATUS_PUBLISHED = 1;

    /** 订阅源最多收录的文章数（站点地图不受此限制）。 */
    private static final int FEED_ITEMS = 100;

    private static final DateTimeFormatter RSS_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final PostQueryService postQueryService;
    private final SiteSettingsService siteSettingsService;
    private final SitemapService sitemaps;
    private final SiteOrigin origin;
    private final SafeMarkdown markdown;

    public SeoController(PostQueryService postQueryService,
                         SiteSettingsService siteSettingsService,
                         SitemapService sitemaps,
                         SiteOrigin origin,
                         SafeMarkdown markdown) {
        this.postQueryService = postQueryService;
        this.siteSettingsService = siteSettingsService;
        this.sitemaps = sitemaps;
        this.origin = origin;
        this.markdown = markdown;
    }

    // ------------------------------------------------------------------
    // RSS 2.0
    // ------------------------------------------------------------------

    @GetMapping(value = "/feed.xml", produces = "application/rss+xml;charset=UTF-8")
    @ResponseBody
    @Cacheable(value = CaffeineCacheConfig.FEED_CACHE, key = "@siteOrigin.resolve(#request) + ':rss'")
    public String rss(HttpServletRequest request) {
        String base = origin.resolve(request);
        Map<String, String> settings = siteSettingsService.getPublicSettings();
        String title = setting(settings, "siteTitle", "个人博客");
        String description = setting(settings, "siteDescription", "");
        List<BlogPost> posts = feedPosts();

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\"")
          .append(" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">\n");
        sb.append("  <channel>\n");
        sb.append("    <title>").append(escape(title)).append("</title>\n");
        sb.append("    <link>").append(escape(base)).append("</link>\n");
        sb.append("    <description>").append(escape(description)).append("</description>\n");
        sb.append("    <language>zh-CN</language>\n");
        sb.append("    <atom:link href=\"").append(escape(base)).append("/feed.xml\" rel=\"self\" type=\"application/rss+xml\"/>\n");
        if (!posts.isEmpty() && posts.get(0).getCreateTime() != null) {
            sb.append("    <lastBuildDate>")
              .append(RSS_DATE.format(posts.get(0).getCreateTime().atZone(ZoneId.systemDefault())))
              .append("</lastBuildDate>\n");
        }
        for (BlogPost post : posts) {
            String bodyHtml = renderedBody(post.getId());
            if (bodyHtml == null) continue;
            String url = base + "/post/" + post.getId();
            sb.append("    <item>\n");
            sb.append("      <title>").append(escape(post.getTitle())).append("</title>\n");
            sb.append("      <link>").append(escape(url)).append("</link>\n");
            sb.append("      <guid isPermaLink=\"true\">").append(escape(url)).append("</guid>\n");
            if (post.getSummary() != null && !post.getSummary().isBlank()) {
                sb.append("      <description>").append(escape(post.getSummary())).append("</description>\n");
            }
            sb.append("      <content:encoded>").append(cdata(bodyHtml)).append("</content:encoded>\n");
            if (post.getCategory() != null && !post.getCategory().isBlank()) {
                sb.append("      <category>").append(escape(post.getCategory())).append("</category>\n");
            }
            if (post.getCreateTime() != null) {
                sb.append("      <pubDate>")
                  .append(RSS_DATE.format(post.getCreateTime().atZone(ZoneId.systemDefault())))
                  .append("</pubDate>\n");
            }
            sb.append("    </item>\n");
        }
        sb.append("  </channel>\n</rss>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Atom 1.0
    // ------------------------------------------------------------------

    @GetMapping(value = "/atom.xml", produces = "application/atom+xml;charset=UTF-8")
    @ResponseBody
    @Cacheable(value = CaffeineCacheConfig.FEED_CACHE, key = "@siteOrigin.resolve(#request) + ':atom'")
    public String atom(HttpServletRequest request) {
        String base = origin.resolve(request);
        Map<String, String> settings = siteSettingsService.getPublicSettings();
        String title = setting(settings, "siteTitle", "个人博客");
        String author = setting(settings, "authorName", "");
        List<BlogPost> posts = feedPosts();

        LocalDateTime updated = posts.isEmpty() || posts.get(0).getCreateTime() == null
                ? LocalDateTime.now()
                : posts.get(0).getCreateTime();

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
        sb.append("  <title>").append(escape(title)).append("</title>\n");
        sb.append("  <link href=\"").append(escape(base)).append("/\"/>\n");
        sb.append("  <link href=\"").append(escape(base)).append("/atom.xml\" rel=\"self\"/>\n");
        sb.append("  <id>").append(escape(base)).append("/</id>\n");
        sb.append("  <updated>").append(ISO_DATE.format(updated.atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS))).append("</updated>\n");
        if (!author.isBlank()) {
            sb.append("  <author><name>").append(escape(author)).append("</name></author>\n");
        }
        for (BlogPost post : posts) {
            String bodyHtml = renderedBody(post.getId());
            if (bodyHtml == null) continue;
            String url = base + "/post/" + post.getId();
            sb.append("  <entry>\n");
            sb.append("    <title>").append(escape(post.getTitle())).append("</title>\n");
            sb.append("    <link href=\"").append(escape(url)).append("\"/>\n");
            sb.append("    <id>").append(escape(url)).append("</id>\n");
            if (post.getCreateTime() != null) {
                sb.append("    <published>")
                  .append(ISO_DATE.format(post.getCreateTime().atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)))
                  .append("</published>\n");
            }
            LocalDateTime upd = post.getUpdateTime() != null ? post.getUpdateTime() : post.getCreateTime();
            if (upd != null) {
                sb.append("    <updated>")
                  .append(ISO_DATE.format(upd.atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)))
                  .append("</updated>\n");
            }
            if (post.getSummary() != null && !post.getSummary().isBlank()) {
                sb.append("    <summary>").append(escape(post.getSummary())).append("</summary>\n");
            }
            sb.append("    <content type=\"html\">").append(escape(bodyHtml)).append("</content>\n");
            sb.append("  </entry>\n");
        }
        sb.append("</feed>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // sitemap：index + 分片
    // ------------------------------------------------------------------

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String sitemapIndex(HttpServletRequest request) {
        return sitemaps.index(origin.resolve(request));
    }

    @GetMapping(value = "/sitemap-site.xml", produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String sitemapSite(HttpServletRequest request) {
        return sitemaps.shard("site", 1, origin.resolve(request));
    }

    @GetMapping(value = "/sitemap-{kind:[a-z]{2,10}}-{part:\\d{1,5}}.xml",
            produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String sitemapShard(@PathVariable String kind, @PathVariable String part, HttpServletRequest request) {
        int number;
        try {
            number = Integer.parseInt(part);
        } catch (NumberFormatException e) {
            throw new com.selfblog.common.exception.BusinessException(404, "站点地图分片不存在");
        }
        return sitemaps.shard(kind, number, origin.resolve(request));
    }

    // ------------------------------------------------------------------
    // robots.txt
    // ------------------------------------------------------------------

    /**
     * 动态生成而不是放静态文件：Sitemap 指令必须是绝对地址，且后台入口由站点设置决定；
     * 写死 /admin 会在作者修改入口后留下错误的抓取规则。
     */
    @GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String robots(HttpServletRequest request) {
        String base = origin.resolve(request);
        String adminPath = setting(siteSettingsService.getPublicSettings(), "adminPath", "admin");
        return """
                User-agent: *
                Allow: /
                # 后台入口由站点设置决定，不允许收录；这不是鉴权替代
                Disallow: /%s
                Disallow: /api/
                Disallow: /search

                Sitemap: %s/sitemap.xml
                """.formatted(adminPath, base);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * 订阅源用的已发布文章：只取元数据（不加载正文），按发布时间倒序，上限 100。
     * 与站点地图不同，这里保留订阅器习惯的“最近文章”语义。
     */
    private List<BlogPost> feedPosts() {
        return postQueryService.getPublishedMetadata(1, FEED_ITEMS).getRecords().stream()
                .sorted(Comparator.comparing(BlogPost::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BlogPost::getId, Comparator.reverseOrder()))
                .toList();
    }

    private static String setting(Map<String, String> settings, String key, String fallback) {
        String value = settings.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 渲染某篇已发布文章的正文 HTML：与阅读页完全同一条公开数据路径
     * ({@link PostQueryService#getPublishedPostDetail}，草稿/已删除不可见、不碰计数)
     * 加同一个 {@link SafeMarkdown#render} 入口，保证订阅器看到的内容与浏览器渲染一致。
     *
     * <p>文章在元数据列表与详情读取之间被下线/删除时（并发窗口）返回 {@code null}，
     * 该条目整体跳过，绝不能让订阅源因此失败。</p>
     */
    private String renderedBody(Long postId) {
        try {
            return markdown.render(postQueryService.getPublishedPostDetail(postId).getContent());
        } catch (BusinessException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    /**
     * CDATA 载荷安全化：载荷里出现字面 {@code ]]>} 时必须拆成 {@code ]]]]><![CDATA[>}，
     * 否则它会提前终止 CDATA 让整个 XML 解析失败。SafeMarkdown 目前总会转义 {@code >}，
     * 这里是对渲染器未来行为的防御，拆分对 DOM 读取方完全透明（解析后仍是原文）。
     */
    static String cdata(String payload) {
        return "<![CDATA[" + payload.replace("]]>", "]]]]><![CDATA[>") + "]]>";
    }

    /** XML 文本与属性值转义。 */
    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> {
                    // 丢弃 XML 1.0 不允许的控制字符，避免订阅器解析失败
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
