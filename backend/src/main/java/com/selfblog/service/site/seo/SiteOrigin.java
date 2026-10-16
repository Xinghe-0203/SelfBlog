package com.selfblog.service.site.seo;

import com.selfblog.service.site.SiteSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * One canonical origin for HTML, runtime metadata and discovery documents.
 *
 * <p>优先级：后台「网站设置 → 网站域名」（保存即生效，缓存随设置更新清理）→ 环境变量
 * {@code SITE_BASE_URL}（部署默认值）→ 直连请求推导（仅本地开发）。数据库里存在直接改坏的值时
 * 记录警告并安全回落，不能让整站无法访问。
 */
@Component
public class SiteOrigin {
    private static final Logger log = LoggerFactory.getLogger(SiteOrigin.class);

    private final String configured;
    private final SiteSettingsService settings;

    public SiteOrigin(@Value("${SITE_BASE_URL:}") String configured, SiteSettingsService settings) {
        this.configured = configured == null || configured.isEmpty() ? null : normalize(configured);
        this.settings = settings;
    }

    public String resolve(HttpServletRequest request) {
        String stored = storedOrigin();
        if (stored != null) return stored;
        if (configured != null) return configured;
        String host = request.getServerName();
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        return normalize(request.getScheme() + "://" + host + ":" + request.getServerPort());
    }

    /** 后台设置里的域名；空或非法都返回 null 交给下一优先级。 */
    private String storedOrigin() {
        String raw = settings.getSettings().get("siteBaseUrl");
        if (raw == null || raw.isBlank()) return null;
        try {
            return normalize(raw);
        } catch (IllegalArgumentException error) {
            log.warn("siteBaseUrl 配置无效，已回落到 SITE_BASE_URL/请求地址（请到后台网站设置修正）");
            return null;
        }
    }

    public static String normalize(String raw) {
        try {
            if (unsafe(raw) || raw.indexOf(',') >= 0 || raw.indexOf('%') >= 0) throw new IllegalArgumentException();
            URI uri = URI.create(raw);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            int port = uri.getPort();
            if (!Set.of("http", "https").contains(scheme) || host == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null || port == 0 || port > 65535
                    || !(uri.getRawPath() == null || uri.getRawPath().isEmpty() || uri.getRawPath().equals("/"))) throw new IllegalArgumentException();
            boolean defaultPort = port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
            return scheme + "://" + host.toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("SITE_BASE_URL must be a valid http(s) origin without credentials, path, query or fragment");
        }
    }

    public static String absolute(String base, String path) {
        if (unsafe(path) || path.startsWith("//")) return "";
        try {
            URI uri = URI.create(path);
            if (path.startsWith("/")) return base + path;
            if (uri.getScheme() != null && Set.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT))
                    && uri.getHost() != null && uri.getUserInfo() == null) return path;
        } catch (IllegalArgumentException ignored) { }
        return "";
    }

    private static boolean unsafe(String value) {
        return value == null || value.isEmpty() || value.indexOf('\\') >= 0
                || value.chars().anyMatch(c -> Character.isISOControl(c) || Character.isWhitespace(c));
    }
}
