package com.selfblog.service.site.seo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.PublicPage;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.BlogTag;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.site.PageService;
import com.selfblog.service.site.page.PageCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 完整而有界的站点地图：index + 分片，不再复用订阅源的 100 篇上限。
 *
 * <p>每片最多 {@value #SHARD_SIZE} 个 URL，只查询公开元数据；不读取草稿、
 * 不加载正文。协议上限（50000 条 / 50MiB）超限时明确失败而不是静默截断。
 */
@Service
@Transactional(readOnly = true)
public class SitemapService {
    static final int SHARD_SIZE = 1000;
    private static final int MAX_INDEX_ENTRIES = 50_000;

    private final PostQueryService posts;
    private final BlogTagService tags;
    private final PageService pages;

    public SitemapService(PostQueryService posts, BlogTagService tags, PageService pages) {
        this.posts = posts;
        this.tags = tags;
        this.pages = pages;
    }

    /** /sitemap.xml：标准 sitemap index，只列出真正存在分片的类型。 */
    public String index(String base) {
        List<String> parts = new ArrayList<>();
        if (!siteEntries().isEmpty()) parts.add(part("site", 1));
        long postCount = posts.count(new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getStatus, 1));
        for (int part = 1; part <= shardCount(postCount); part++) parts.add(part("posts", part));
        long tagCount = tags.count(null);
        for (int part = 1; part <= shardCount(tagCount); part++) parts.add(part("tags", part));
        if (!customPages().isEmpty()) parts.add(part("pages", 1));
        if (parts.size() > MAX_INDEX_ENTRIES) throw new BusinessException(500, "站点地图分片数量超出协议上限");

        StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : parts) sb.append("  <sitemap><loc>").append(xml(base)).append('/').append(path).append("</loc></sitemap>\n");
        sb.append("</sitemapindex>\n");
        return sb.toString();
    }

    /** 单个分片；类型或序号不存在时 404，绝不返回截断的“成功”内容。 */
    public String shard(String kind, int part, String base) {
        List<Url> urls = switch (kind) {
            case "site" -> {
                if (part != 1) yield null;
                yield siteEntries();
            }
            case "pages" -> {
                if (part != 1) yield null;
                yield customPages();
            }
            case "posts" -> {
                long count = posts.count(new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getStatus, 1));
                if (part < 1 || part > shardCount(count)) yield null;
                List<Url> list = new ArrayList<>();
                for (BlogPost post : posts.getPublishedMetadata(part, SHARD_SIZE).getRecords()) {
                    list.add(new Url("/post/" + post.getId(), date(post.getUpdateTime() != null ? post.getUpdateTime() : post.getCreateTime())));
                }
                yield list;
            }
            case "tags" -> {
                long count = tags.count(null);
                if (part < 1 || part > shardCount(count)) yield null;
                List<Url> list = new ArrayList<>();
                for (BlogTag tag : tags.page(new Page<>(part, SHARD_SIZE),
                        new LambdaQueryWrapper<BlogTag>().orderByAsc(BlogTag::getId)).getRecords()) {
                    list.add(new Url("/tag/" + tag.getId(), null));
                }
                yield list;
            }
            default -> null;
        };
        if (urls == null || urls.isEmpty()) throw new BusinessException(404, "站点地图分片不存在");

        StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (Url url : urls) {
            sb.append("  <url><loc>").append(xml(base)).append(xml(url.path())).append("</loc>");
            if (url.lastmod() != null) sb.append("<lastmod>").append(url.lastmod()).append("</lastmod>");
            sb.append("</url>\n");
        }
        sb.append("</urlset>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------

    /** 首页/归档/关于/相册：遵循各页面已发布配置的 indexable。 */
    List<Url> siteEntries() {
        List<Url> urls = new ArrayList<>();
        for (String id : List.of("home", "archives", "about", "gallery")) {
            PublicPage page = pages.getPublished(id);
            if (!indexable(page)) continue;
            String path = PageCatalog.definition(id).path();
            if (path == null) continue;
            urls.add(new Url(path, date(page.updatedAt())));
        }
        return urls;
    }

    /** 已发布自建内容页；下线或 indexable=false 的不进入站点地图。 */
    List<Url> customPages() {
        List<Url> urls = new ArrayList<>();
        for (PublicPage page : pages.getManifest().pages()) {
            if (PageCatalog.builtin(page.id()) || page.slug() == null || !indexable(page)) continue;
            urls.add(new Url("/pages/" + page.slug(), date(page.updatedAt())));
        }
        return urls;
    }

    private static boolean indexable(PublicPage page) {
        var seo = page.config().seo();
        return seo == null || seo.indexable() == null || seo.indexable();
    }

    private static int shardCount(long total) {
        // 空集合没有分片；索引与分片必须对“存在性”给出同一答案。
        return (int) ((total + SHARD_SIZE - 1) / SHARD_SIZE);
    }

    private static String part(String kind, int number) {
        // 与规格一致：站点分片无序号，其余按 kind-N 命名。
        return "site".equals(kind) ? "sitemap-site.xml" : "sitemap-" + kind + "-" + number + ".xml";
    }

    private static String date(String isoInstantOrDateTime) {
        if (isoInstantOrDateTime == null || isoInstantOrDateTime.length() < 10) return null;
        return isoInstantOrDateTime.substring(0, 10);
    }

    private static String date(LocalDateTime value) {
        return value == null ? null : value.toLocalDate().toString();
    }

    private static String xml(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> {
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') out.append(c);
                }
            }
        }
        return out.toString();
    }

    private record Url(String path, String lastmod) { }
}
