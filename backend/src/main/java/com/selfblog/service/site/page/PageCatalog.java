package com.selfblog.service.site.page;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Whitelisted page capabilities; no stored CSS, executable templates or arbitrary routes. */
public final class PageCatalog {
    public record Definition(String label, String path, Set<String> text, Set<String> modules) { }
    public static final Map<String, Definition> BUILTINS;
    private static final Definition CUSTOM = new Definition("内容页", null, Set.of(), Set.of("header", "updated", "links"));
    static {
        var definitions = new LinkedHashMap<String, Definition>();
        definitions.put("home", new Definition("首页", "/", Set.of("eyebrow", "listTitle"), Set.of("hero", "cover", "author", "tags", "archiveLink")));
        definitions.put("about", new Definition("关于", "/about", Set.of(), Set.of("author", "site", "links")));
        definitions.put("archives", new Definition("归档", "/archives", Set.of(), Set.of("category", "count")));
        definitions.put("gallery", new Definition("相册", "/gallery", Set.of(), Set.of("description", "count")));
        definitions.put("search", new Definition("搜索", null, Set.of("placeholder", "empty"), Set.of("count")));
        definitions.put("post", new Definition("文章详情模板", null, Set.of(), Set.of("cover", "meta", "toc", "adjacent")));
        definitions.put("tag", new Definition("标签详情模板", null, Set.of(), Set.of("count")));
        definitions.put("notFound", new Definition("404 页面", null, Set.of(), Set.of("homeLink", "search")));
        BUILTINS = java.util.Collections.unmodifiableMap(definitions);
    }
    private PageCatalog() { }

    public static boolean builtin(String id) { return BUILTINS.containsKey(id); }
    public static Definition definition(String id) {
        if (builtin(id)) return BUILTINS.get(id);
        if (id != null && id.matches("custom-[a-f0-9]{32}")) return CUSTOM;
        throw new BusinessException(404, "页面不存在");
    }
    public static String slug(String slug) {
        if (slug == null || slug.length() < 2 || slug.length() > 60 || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new BusinessException(400, "页面地址需为2-60位小写字母、数字或单横线");
        }
        return slug;
    }
    public static void validate(String id, PageConfig config) {
        var definition = definition(id);
        if (config == null) throw new BusinessException(400, "请填写页面配置");
        length(config.title(), 120, "页面标题");
        length(config.description(), 1000, "页面介绍");
        length(config.body(), 10000, "页面正文");
        if (!builtin(id) && (config.title() == null || config.title().isBlank())) throw new BusinessException(400, "页面标题不能为空");
        if (config.title() != null && config.title().isBlank()) throw new BusinessException(400, "页面标题不能为空，继承默认请清除覆盖");
        if (Set.of("post", "tag").contains(id) && config.title() != null) throw new BusinessException(400, "详情模板不能覆盖真实文章或标签标题");
        if (!"about".equals(id) && builtin(id) && config.body() != null && !config.body().isEmpty()) throw new BusinessException(400, "此页面请使用原内容管理功能编辑正文");
        if (!definition.text().containsAll(config.text().keySet()) || !definition.modules().containsAll(config.modules().keySet())) throw new BusinessException(400, "包含不支持的页面字段或模块");
        config.text().values().forEach(text -> length(text, 300, "页面文字"));
        var style = config.style();
        if (style != null) {
            if (style.accent() != null && !style.accent().isEmpty() && !style.accent().matches("#[a-fA-F0-9]{6}")) throw new BusinessException(400, "强调色需为 #RRGGBB");
            choice(style.background(), Set.of("inherit", "plain", "soft", "paper"));
            choice(style.width(), Set.of("inherit", "narrow", "standard", "wide"));
            choice(style.density(), Set.of("inherit", "compact", "comfortable"));
        }
        var seo = config.seo();
        if (seo != null) {
            length(seo.title(), 160, "SEO标题");
            length(seo.description(), 400, "SEO描述");
            if (Boolean.TRUE.equals(seo.indexable()) && Set.of("search", "notFound").contains(id)) throw new BusinessException(400, "搜索和404页面不允许收录");
            imageUrl(seo.image());
        }
    }
    private static void length(String value, int limit, String field) {
        if (value != null && value.length() > limit) throw new BusinessException(400, field + "不能超过" + limit + "个字符");
    }
    private static void choice(String value, Set<String> choices) {
        if (value != null && !choices.contains(value)) throw new BusinessException(400, "请选择有效的页面样式");
    }
    private static void imageUrl(String value) {
        if (value == null || value.isEmpty()) return;
        length(value, 2048, "图片地址");
        try {
            URI uri = URI.create(value);
            if (value.startsWith("/uploads/") && !value.contains("\\") && !value.contains("%")
                    && !value.contains("..") && !value.contains("\r") && !value.contains("\n")) return;
            if (uri.getHost() != null && uri.getScheme() != null && uri.getUserInfo() == null && Set.of("http", "https").contains(uri.getScheme())) return;
        } catch (IllegalArgumentException ignored) { }
        throw new BusinessException(400, "分享图需为合法http(s)地址或站内/uploads/路径");
    }
}
