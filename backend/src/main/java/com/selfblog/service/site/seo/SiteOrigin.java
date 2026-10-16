package com.selfblog.service.site.seo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** One canonical origin for HTML, runtime metadata and discovery documents. */
@Component
public class SiteOrigin {
    private final String configured;

    public SiteOrigin(@Value("${SITE_BASE_URL:}") String configured) {
        this.configured = configured == null || configured.isEmpty() ? null : normalize(configured);
    }

    public String resolve(HttpServletRequest request) {
        if (configured != null) return configured;
        String host = request.getServerName();
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        return normalize(request.getScheme() + "://" + host + ":" + request.getServerPort());
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
