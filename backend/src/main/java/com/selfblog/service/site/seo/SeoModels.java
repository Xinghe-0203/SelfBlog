package com.selfblog.service.site.seo;

import com.selfblog.dto.site.PageModels.PageConfig;

import java.util.List;

/**
 * 服务端阅读 HTML 的传输模型。
 *
 * <p>只承载公开数据：草稿、账户、解锁令牌或管理设置不允许出现在这里。
 * 阅读 DOM 与 Vue 交互层共享同一份 {@link PageConfig} 白名单语义。
 */
public final class SeoModels {
    private SeoModels() { }

    public enum Kind { SHELL, POST, TAG, HOME, ARCHIVES, ABOUT, GALLERY, SEARCH, NOT_FOUND, CUSTOM }

    /**
     * 首次响应里的 head 元数据（canonical/robots/OG/Twitter 由渲染层使用）。
     *
     * @param tags      文章标签，阅读区「标签」行使用
     * @param indexable 是否允许搜索引擎收录
     * @param keywords  后台 SEO 关键词（{@code post_seo.seo_keywords} 或标签派生），
     *                  渲染为 {@code meta[name=keywords]}；为空则回落到 {@code tags}
     * @param shareImage 社交分享封面（{@code post_seo.og_image_url} → 文章封面 → 站点 Logo）；
     *                  只用于 og:image / twitter:image / JSON-LD，<b>不影响</b>阅读区头图
     *                  （头图仍取 {@code image}，避免后台换分享图时页面版式跟着跳）
     */
    public record Meta(String title, String description, String image, String type, String path,
                       String publishedTime, String author, List<String> tags, boolean indexable,
                       List<String> keywords, String shareImage) { }

    /** 阅读区域内的一条公开链接。 */
    public record Entry(String url, String title, String summary, String date) { }

    /** 公开导航项（由已发布页面派生，绝不含草稿）。 */
    public record NavLink(String label, String to) { }

    /** 公开站点信息，用于关于页的作者/站点模块与标题后缀。 */
    public record SiteInfo(String title, String description, String author, String authorBio) { }

    public record ReadingPage(int status, Kind kind, String id, PageConfig config, Meta meta,
                              String displayTitle, String bodyHtml, List<Entry> entries,
                              List<NavLink> navigation, SiteInfo site) { }
}
