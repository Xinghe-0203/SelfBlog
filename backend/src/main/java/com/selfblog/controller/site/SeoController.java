package com.selfblog.controller.site;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.BlogTag;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.site.SiteSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 站点 SEO 与订阅源。
 *
 * <p>个人博客最需要、而此前完全缺失的三样东西：订阅源（RSS/Atom）、
 * sitemap.xml、以及给分享链接用的绝对地址。三者都必须是公开可访问的，
 * 因此这里用 {@link Controller} 而不是 {@code @RestController}——
 * 项目通过 {@code WebMvcConfig#configurePathMatch} 给所有
 * {@code @RestController} 自动加了 {@code /api} 前缀，走 RestController
 * 会让地址变成 {@code /api/feed.xml}，订阅器与爬虫不会去那里找。
 *
 * <p>对应的放行规则见 {@code SecurityConfig}：SPA 回退只匹配「不含点号」的
 * 路径（{@code [^\.]*}），所以带扩展名的这三个地址不会被回退命中，
 * 必须显式 permitAll，否则会落到「仅作者可访问」的兜底规则返回 401。
 */
@Controller
public class SeoController {

    /** 已发布状态。见 CLAUDE.md：用户文章与动态发布即 status=1，无待审流程。 */
    private static final int STATUS_PUBLISHED = 1;

    /** 订阅源与站点地图里最多收录的文章数。 */
    private static final int MAX_ITEMS = 100;

    private static final DateTimeFormatter RSS_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final PostQueryService postQueryService;
    private final BlogTagService blogTagService;
    private final SiteSettingsService siteSettingsService;

    /**
     * 反向代理后 request 里的 host 可能不是对外域名，可用环境变量覆盖。
     * 未配置时按请求头的 scheme/host 推导。
     */
    @Value("${SITE_BASE_URL:}")
    private String configuredBaseUrl;

    public SeoController(PostQueryService postQueryService,
                         BlogTagService blogTagService,
                         SiteSettingsService siteSettingsService) {
        this.postQueryService = postQueryService;
        this.blogTagService = blogTagService;
        this.siteSettingsService = siteSettingsService;
    }

    // ------------------------------------------------------------------
    // RSS 2.0
    // ------------------------------------------------------------------

    @GetMapping(value = "/feed.xml", produces = "application/rss+xml;charset=UTF-8")
    @ResponseBody
    public String rss(HttpServletRequest request) {
        String base = baseUrl(request);
        Map<String, String> settings = siteSettingsService.getPublicSettings();
        String title = setting(settings, "siteTitle", "个人博客");
        String description = setting(settings, "siteDescription", "");
        List<BlogPost> posts = publishedPosts();

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
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
            String url = base + "/post/" + post.getId();
            sb.append("    <item>\n");
            sb.append("      <title>").append(escape(post.getTitle())).append("</title>\n");
            sb.append("      <link>").append(escape(url)).append("</link>\n");
            sb.append("      <guid isPermaLink=\"true\">").append(escape(url)).append("</guid>\n");
            if (post.getSummary() != null && !post.getSummary().isBlank()) {
                sb.append("      <description>").append(escape(post.getSummary())).append("</description>\n");
            }
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
    public String atom(HttpServletRequest request) {
        String base = baseUrl(request);
        Map<String, String> settings = siteSettingsService.getPublicSettings();
        String title = setting(settings, "siteTitle", "个人博客");
        String author = setting(settings, "authorName", "");
        List<BlogPost> posts = publishedPosts();

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
            sb.append("  </entry>\n");
        }
        sb.append("</feed>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // sitemap.xml
    // ------------------------------------------------------------------

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String sitemap(HttpServletRequest request) {
        String base = baseUrl(request);
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (String path : List.of("/", "/archives", "/about", "/gallery", "/search")) {
            sb.append("  <url><loc>").append(escape(base)).append(path).append("</loc></url>\n");
        }
        for (BlogPost post : publishedPosts()) {
            sb.append("  <url><loc>").append(escape(base)).append("/post/").append(post.getId()).append("</loc>");
            LocalDateTime upd = post.getUpdateTime() != null ? post.getUpdateTime() : post.getCreateTime();
            if (upd != null) {
                sb.append("<lastmod>").append(upd.toLocalDate()).append("</lastmod>");
            }
            sb.append("</url>\n");
        }
        for (BlogTag tag : blogTagService.listAllTags()) {
            sb.append("  <url><loc>").append(escape(base)).append("/tag/").append(tag.getId()).append("</loc></url>\n");
        }

        sb.append("</urlset>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // robots.txt
    // ------------------------------------------------------------------

    /**
     * 动态生成而不是放静态文件：Sitemap 指令必须是绝对地址，
     * 静态文件在构建时并不知道站点域名。此前该路径被 SecurityConfig 放行
     * 但没有对应资源，访问会返回 500（爬虫看到的是一台坏掉的站点）。
     */
    @GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String robots(HttpServletRequest request) {
        String base = baseUrl(request);
        return """
                User-agent: *
                Allow: /
                # 后台入口由站点设置决定，不允许收录
                Disallow: /admin
                Disallow: /api/

                Sitemap: %s/sitemap.xml
                """.formatted(base);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * 已发布文章，按发布时间倒序。用 MyBatis-Plus 的链式查询，
     * {@code @TableLogic} 会自动带上 is_deleted=0，无需手写软删除条件。
     */
    private List<BlogPost> publishedPosts() {
        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getStatus, STATUS_PUBLISHED)
                .orderByDesc(BlogPost::getCreateTime);
        Page<BlogPost> page = postQueryService.page(new Page<>(1, MAX_ITEMS, false), wrapper);
        return page.getRecords();
    }

    /** 站点对外基地址，去掉结尾斜杠。 */
    private String baseUrl(HttpServletRequest request) {
        String base = configuredBaseUrl;
        if (base == null || base.isBlank()) {
            String scheme = request.getScheme();
            String host = request.getHeader("X-Forwarded-Host");
            if (host == null || host.isBlank()) {
                host = request.getServerName();
                int port = request.getServerPort();
                boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
                if (!defaultPort) {
                    host = host + ":" + port;
                }
            }
            String proto = request.getHeader("X-Forwarded-Proto");
            base = (proto != null && !proto.isBlank() ? proto : scheme) + "://" + host;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static String setting(Map<String, String> settings, String key, String fallback) {
        String value = settings.get(key);
        return value == null || value.isBlank() ? fallback : value;
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
