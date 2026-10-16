package com.selfblog.service.site.seo;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.post.PostAdvancedSearchRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.dto.site.PageModels.PageConfig;
import com.selfblog.dto.site.PageModels.PublicPage;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.BlogTag;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.site.PageService;
import com.selfblog.service.site.PhotoAlbumService;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;
import com.selfblog.vo.site.PhotoAlbumVO;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 公开阅读页面的组装入口：把路径解析为只含已发布数据的 {@link SeoModels.ReadingPage}。
 *
 * <p>只读；不增加阅读量、不记录访客、不读取管理草稿或相册解锁令牌。
 * 未知路径按 404 处理，后台命名空间与历史账户路由保留启动壳并强制 noindex。
 *
 * <p><b>刻意不声明类级 {@code @Transactional}</b>：这里会调用 PageService/PostQueryService
 * 的“找不到就抛 404”查询，而异常会让共享事务被标记为 rollback-only；一旦外层与内层
 * 共享事务，404 就会在提交时变成 {@code UnexpectedRollbackException}（实测为 500）。
 * 让每个内层查询使用自己的只读事务，才能把“不存在”安全地转换成 404 页面。
 */
@Service
public class ReadingPageService {
    private static final int HOME_PAGE_SIZE = 9;
    private static final int MAX_LIST = 100;
    private static final Pattern ID = Pattern.compile("\\d{1,19}");
    private static final Pattern PAGE = Pattern.compile("[1-9]\\d{0,5}");
    private static final Set<String> SHELL_PATHS = Set.of(
            "/login", "/profile", "/profile-edit", "/password-change", "/drafts", "/collections",
            "/following", "/followers", "/my-reports", "/notifications", "/messages");

    private final SiteSettingsService settings;
    private final PostQueryService posts;
    private final BlogTagService tags;
    private final PageService pages;
    private final PhotoAlbumService albums;
    private final SafeMarkdown markdown;

    public ReadingPageService(SiteSettingsService settings, PostQueryService posts, BlogTagService tags,
                              PageService pages, PhotoAlbumService albums, SafeMarkdown markdown) {
        this.settings = settings;
        this.posts = posts;
        this.tags = tags;
        this.pages = pages;
        this.albums = albums;
        this.markdown = markdown;
    }

    public SeoModels.ReadingPage resolve(String path, String rawQuery) {
        String normalized = normalize(path);
        SeoModels.SiteInfo site = siteInfo();
        String adminRoot = "/" + settings.getSettings().getOrDefault("adminPath", "admin");
        if (normalized.equals(adminRoot) || normalized.startsWith(adminRoot + "/")) return shell(site);
        switch (normalized) {
            case "/": return home(rawQuery, site);
            case "/archives": return archives(site);
            case "/about": return about(site);
            case "/gallery": return gallery(site);
            case "/search": return search(site);
            default:
                if (SHELL_PATHS.contains(normalized) || normalized.startsWith("/user/") || normalized.startsWith("/report/")) {
                    return shell(site);
                }
                if (normalized.startsWith("/pages/")) return custom(normalized.substring("/pages/".length()), site);
                if (normalized.startsWith("/post/")) return post(normalized.substring("/post/".length()), site);
                if (normalized.startsWith("/tag/")) return tag(normalized.substring("/tag/".length()), site);
                return notFound(site);
        }
    }

    // ------------------------------------------------------------------
    // 页面族
    // ------------------------------------------------------------------

    private SeoModels.ReadingPage home(String rawQuery, SeoModels.SiteInfo site) {
        PageConfig config = config("home");
        long page = parsePage(rawQuery);
        PostQueryRequest request = new PostQueryRequest();
        request.setPageNum((int) page);
        request.setPageSize(HOME_PAGE_SIZE);
        request.setSort("latest");
        var result = posts.getPostList(request);
        if (page > Math.max(1, result.getPages())) return notFound(site);
        List<SeoModels.Entry> entries = new ArrayList<>();
        for (PostListResponse record : result.getRecords()) entries.add(entry(record));
        String title = config.title() != null ? config.title() : site.title();
        String path = page <= 1 ? "/" : "/?page=" + page;
        return page(200, SeoModels.Kind.HOME, "home", config,
                meta(title, description(config, site.description()), null, "website", path, null, null, List.of(), indexable(config, false)),
                title, "", entries, navigation(), site);
    }

    private SeoModels.ReadingPage archives(SeoModels.SiteInfo site) {
        PageConfig config = config("archives");
        List<BlogPost> published = posts.getPublishedMetadata(1, MAX_LIST).getRecords();
        List<SeoModels.Entry> entries = published.stream()
                .sorted(Comparator.comparing(BlogPost::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BlogPost::getId, Comparator.reverseOrder()))
                .map(post -> new SeoModels.Entry("/post/" + post.getId(), post.getTitle(), post.getSummary(), date(post.getCreateTime())))
                .toList();
        String title = config.title() != null ? config.title() : "文章归档";
        String fallback = "按时间回看这里发布过的文章。";
        return page(200, SeoModels.Kind.ARCHIVES, "archives", config,
                meta(title, description(config, fallback), null, "website", "/archives", null, null, List.of(), indexable(config, false)),
                title, "", entries, navigation(), site);
    }

    private SeoModels.ReadingPage about(SeoModels.SiteInfo site) {
        PageConfig config = config("about");
        String title = config.title() != null ? config.title() : site.author();
        String body = markdown.render(config.body());
        return page(200, SeoModels.Kind.ABOUT, "about", config,
                meta(title, description(config, site.authorBio()), null, "website", "/about", null, null, List.of(), indexable(config, false)),
                title, body, List.of(), navigation(), site);
    }

    private SeoModels.ReadingPage gallery(SeoModels.SiteInfo site) {
        PageConfig config = config("gallery");
        List<SeoModels.Entry> entries = new ArrayList<>();
        for (PhotoAlbumVO album : albums.getPublicAlbums()) {
            String note = (album.isPrivateAlbum() ? "需要密码" : "公开") + " · " + album.getPhotoCount() + " 张";
            entries.add(new SeoModels.Entry(null, album.getTitle() == null ? "相册" : album.getTitle(), note, null));
        }
        String title = config.title() != null ? config.title() : "相册";
        String fallback = "把途中的光影，慢慢收进记忆。";
        return page(200, SeoModels.Kind.GALLERY, "gallery", config,
                meta(title, description(config, fallback), null, "website", "/gallery", null, null, List.of(), indexable(config, false)),
                title, "", entries, navigation(), site);
    }

    private SeoModels.ReadingPage search(SeoModels.SiteInfo site) {
        PageConfig config = config("search");
        String title = config.title() != null ? config.title() : "搜索文章";
        String fallback = "按标题、摘要或正文内容查找。";
        return page(200, SeoModels.Kind.SEARCH, "search", config,
                meta(title, description(config, fallback), null, "website", "/search", null, null, List.of(), false),
                title, "", List.of(), navigation(), site);
    }

    private SeoModels.ReadingPage post(String idText, SeoModels.SiteInfo site) {
        Long id = parseId(idText);
        if (id == null) return notFound(site);
        PostDetailResponse detail;
        try {
            detail = posts.getPublishedPostDetail(id);
        } catch (BusinessException e) {
            if (e.getCode() == 404) return notFound(site);
            throw e;
        }
        PageConfig config = config("post");
        String body = markdown.render(detail.getContent());
        String author = firstNonBlank(detail.getNickname(), site.author(), detail.getUsername());
        String description = detail.getSummary();
        if (description == null || description.isBlank()) {
            String text = Jsoup.parse(body).text();
            description = text.length() > 140 ? text.substring(0, 140) : text;
        }
        List<String> tagNames = detail.getTags() == null ? List.of()
                : detail.getTags().stream().map(PostDetailResponse.TagVO::getName).filter(n -> n != null && !n.isBlank()).toList();
        String image = firstNonBlank(detail.getCoverImage(), seoImage(config));
        String published = detail.getCreateTime() == null ? null : detail.getCreateTime().toString();
        String title = suffix(detail.getTitle(), config);
        return page(200, SeoModels.Kind.POST, "post", config,
                meta(documentTitle(title, site), description, image, "article", "/post/" + detail.getId(),
                        published, author, tagNames, indexable(config, false)),
                detail.getTitle(), body, List.of(), navigation(), site);
    }

    private SeoModels.ReadingPage tag(String idText, SeoModels.SiteInfo site) {
        Long id = parseId(idText);
        if (id == null) return notFound(site);
        BlogTag tag = tags.getById(id);
        if (tag == null) return notFound(site);
        PageConfig config = config("tag");
        PostAdvancedSearchRequest request = new PostAdvancedSearchRequest();
        request.setTagId(id);
        request.setPageNum(1);
        request.setPageSize(MAX_LIST);
        List<SeoModels.Entry> entries = new ArrayList<>();
        for (PostListResponse record : posts.advancedSearch(request).getRecords()) entries.add(entry(record));
        String title = suffix(tag.getName(), config);
        return page(200, SeoModels.Kind.TAG, "tag", config,
                meta(documentTitle(title, site), description(config, ""), null, "website", "/tag/" + id,
                        null, null, List.of(), indexable(config, false)),
                tag.getName(), "", entries, navigation(), site);
    }

    private SeoModels.ReadingPage custom(String slug, SeoModels.SiteInfo site) {
        if (slug.isBlank() || slug.contains("/")) return notFound(site);
        PublicPage page;
        try {
            page = pages.getBySlug(slug);
        } catch (BusinessException e) {
            if (e.getCode() == 404) return notFound(site);
            throw e;
        }
        PageConfig config = page.config();
        String title = config.title() != null ? config.title() : "内容页";
        return page(200, SeoModels.Kind.CUSTOM, page.id(), config,
                meta(documentTitle(title, site), description(config, ""), seoImage(config), "website", "/pages/" + slug,
                        null, null, List.of(), indexable(config, false)),
                title, markdown.render(config.body()), List.of(), navigation(), site);
    }

    private SeoModels.ReadingPage shell(SeoModels.SiteInfo site) {
        return page(200, SeoModels.Kind.SHELL, null, PageConfig.empty(),
                meta(site.title(), "", null, "website", null, null, null, List.of(), false),
                site.title(), "", List.of(), List.of(), site);
    }

    private SeoModels.ReadingPage notFound(SeoModels.SiteInfo site) {
        PageConfig config = config("notFound");
        String title = config.title() != null ? config.title() : "页面不存在";
        return page(404, SeoModels.Kind.NOT_FOUND, "notFound", config,
                meta(documentTitle(title, site), description(config, ""), null, "website", null, null, null, List.of(), false),
                title, "", List.of(), navigation(), site);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private SeoModels.ReadingPage page(int status, SeoModels.Kind kind, String id, PageConfig config,
                                       SeoModels.Meta meta, String displayTitle, String bodyHtml,
                                       List<SeoModels.Entry> entries, List<SeoModels.NavLink> navigation,
                                       SeoModels.SiteInfo site) {
        return new SeoModels.ReadingPage(status, kind, id, config, meta, displayTitle, bodyHtml, entries, navigation, site);
    }

    private SeoModels.Meta meta(String title, String description, String image, String type, String path,
                                String publishedTime, String author, List<String> tags, boolean indexable) {
        return new SeoModels.Meta(title, description, image, type, path, publishedTime, author, tags, indexable);
    }

    private PageConfig config(String id) {
        return pages.getPublished(id).config();
    }

    private List<SeoModels.NavLink> navigation() {
        return pages.getManifest().navigation().stream()
                .map(item -> new SeoModels.NavLink(item.label(), item.to()))
                .toList();
    }

    private SeoModels.Entry entry(PostListResponse record) {
        return new SeoModels.Entry("/post/" + record.getId(), record.getTitle(), record.getSummary(), date(record.getCreateTime()));
    }

    private SeoModels.SiteInfo siteInfo() {
        var values = settings.getSettings();
        return new SeoModels.SiteInfo(values.getOrDefault("siteTitle", ""), values.getOrDefault("siteDescription", ""),
                values.getOrDefault("authorName", ""), values.getOrDefault("authorBio", ""));
    }

    private static boolean indexable(PageConfig config, boolean forcedNoindex) {
        if (forcedNoindex) return false;
        var seo = config.seo();
        return seo == null || seo.indexable() == null || seo.indexable();
    }

    private static String description(PageConfig config, String fallback) {
        return config.description() != null ? config.description() : fallback;
    }

    private static String seoImage(PageConfig config) {
        return config.seo() == null ? null : config.seo().image();
    }

    private static String suffix(String base, PageConfig config) {
        var seo = config.seo();
        return seo != null && seo.title() != null && !seo.title().isBlank() ? base + " - " + seo.title() : base;
    }

    private static String documentTitle(String pageTitle, SeoModels.SiteInfo site) {
        if (pageTitle == null || pageTitle.isBlank()) return site.title();
        if (site.title() == null || site.title().isBlank() || pageTitle.equals(site.title())) return pageTitle;
        return pageTitle + " - " + site.title();
    }

    static String normalize(String path) {
        if (path == null || path.isBlank()) return "/";
        String value = path.replaceAll("/{2,}", "/");
        if (value.length() > 1 && value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value.isEmpty() ? "/" : value;
    }

    private static Long parseId(String value) {
        if (value == null || !ID.matcher(value).matches()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static long parsePage(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return 1;
        for (String part : rawQuery.split("&")) {
            int split = part.indexOf('=');
            if (split <= 0 || !"page".equals(part.substring(0, split))) continue;
            String value = URLDecoder.decode(part.substring(split + 1), StandardCharsets.UTF_8);
            if (PAGE.matcher(value).matches()) return Long.parseLong(value);
            return 1;
        }
        return 1;
    }

    private static String date(LocalDateTime value) {
        return value == null ? null : value.toLocalDate().toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }
}
