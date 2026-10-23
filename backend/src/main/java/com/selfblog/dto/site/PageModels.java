package com.selfblog.dto.site;

import java.util.List;
import java.util.Map;

/** Page transport/storage contracts. Drafts are never part of the public manifest. */
public final class PageModels {
    private PageModels() { }

    /**
     * 页面级横幅配置（首页介绍区 / 关于页作者卡）。
     *
     * <p>字段名与前端 {@code frontend/src/types/page.ts} 的 {@code PageBanner} 逐字一致，
     * 少一个字段就会出现「前端写好了、后端读不出」的静默丢失。所有取值都走
     * {@code PageCatalog} 的白名单校验，不接受任意 CSS 片段。</p>
     */
    public record PageBanner(String image, String fit, String position, String height,
                             Integer overlay, String tone, String layout) { }
    public record PageStyle(String accent, String background, String width, String density, PageBanner banner) { }
    public record PageSeo(String title, String description, String image, Boolean indexable) { }
    public record PageConfig(String title, String description, String body,
                             Map<String, String> text, Map<String, Boolean> modules,
                             PageStyle style, PageSeo seo) {
        public PageConfig {
            text = text == null ? Map.of() : Map.copyOf(text);
            modules = modules == null ? Map.of() : Map.copyOf(modules);
        }
        public static PageConfig empty() { return new PageConfig(null, null, null, null, null, null, null); }
        public PageConfig withoutBody() { return new PageConfig(title, description, null, text, modules, style, seo); }
    }
    public record PageState(int schemaVersion, long revision, String slug, boolean everPublished,
                            String updatedAt, PageConfig config) { }
    public record PageEditor(String id, boolean builtin, String label, PageState draft, PageState published) { }
    public record PublicPage(String id, String slug, String updatedAt, PageConfig config) { }
    public record NavigationItem(String pageId, String label) { }
    public record PublicNavigationItem(String pageId, String label, String to) { }
    public record NavigationState(int schemaVersion, long revision, List<NavigationItem> items) { }
    public record PublicManifest(List<PublicPage> pages, List<PublicNavigationItem> navigation) { }
    public record CreatePage(String slug, String title) { }
    public record SavePage(long revision, String slug, PageConfig config) { }
    public record Revision(long revision) { }
    public record SaveNavigation(long revision, List<NavigationItem> items) { }
}
