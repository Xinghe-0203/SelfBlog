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

    /** 首次响应里的 head 元数据（canonical/robots/OG/Twitter 由渲染层使用）。 */
    public record Meta(String title, String description, String image, String type, String path,
                       String publishedTime, String author, List<String> tags, boolean indexable) { }

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
