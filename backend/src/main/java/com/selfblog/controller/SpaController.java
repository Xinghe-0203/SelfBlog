package com.selfblog.controller;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.site.seo.ReadingHtmlRenderer;
import com.selfblog.service.site.seo.ReadingPageService;
import com.selfblog.service.site.seo.SeoModels;
import com.selfblog.service.site.seo.SiteOrigin;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 公开历史路由入口：返回可直接阅读的 HTML（禁用 JavaScript 也能读正文）。
 *
 * <p>不再统一回退到空壳 index.html：正文、状态码与元数据由阅读服务按已发布内容组装，
 * Vue 启动后仍会照常接管交互。资源/API 等带扩展名或保留前缀的路径不会进入这里。
 * 渲染失败按安全 5xx + noindex 返回，不伪装成 404 或成功空页。
 */
@Controller
public class SpaController {
    private static final Logger log = LoggerFactory.getLogger(SpaController.class);
    private static final String CACHEABLE_CONTROL = "public, max-age=0, must-revalidate";

    private final ReadingPageService reading;
    private final ReadingHtmlRenderer renderer;
    private final SiteOrigin origin;

    public SpaController(ReadingPageService reading, ReadingHtmlRenderer renderer, SiteOrigin origin) {
        this.reading = reading;
        this.renderer = renderer;
        this.origin = origin;
    }

    /** 入口别名归一到根路径，避免同一内容出现两份可收录地址。 */
    @GetMapping("/index.html")
    public ResponseEntity<Void> indexAlias() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT).location(URI.create("/")).build();
    }

    @GetMapping({
            "/",
            "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}",
            "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}/**"
    })
    public ResponseEntity<String> readingPage(HttpServletRequest request) {
        String resolvedOrigin = origin.resolve(request);
        boolean head = "HEAD".equalsIgnoreCase(request.getMethod());
        try {
            var page = reading.resolve(request.getRequestURI(), request.getQueryString());
            String body = renderer.render(page, resolvedOrigin);
            if (cacheable(page)) {
                String etag = etag(body);
                if (etagMatches(request, etag)) {
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .header("ETag", etag)
                            .header("Cache-Control", CACHEABLE_CONTROL)
                            .header("Vary", "Accept-Encoding")
                            .build();
                }
                return ResponseEntity.status(page.status())
                        .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                        .header("ETag", etag)
                        .header("Cache-Control", CACHEABLE_CONTROL)
                        .header("X-Content-Type-Options", "nosniff")
                        .header("Vary", "Accept-Encoding")
                        .body(head ? null : body);
            }
            return html(page.status(), head ? null : body);
        } catch (BusinessException e) {
            int status = e.getCode() >= 400 && e.getCode() <= 599 ? e.getCode() : 500;
            log.warn("公开阅读页生成失败: path={}, code={}", request.getRequestURI(), status);
            return html(status, head ? null : renderer.error(status));
        } catch (RuntimeException e) {
            log.error("公开阅读页渲染异常: path={}", request.getRequestURI(), e);
            return html(500, head ? null : renderer.error(500));
        }
    }

    /**
     * 公开阅读页 200 可复用但必须每次回源校验：内容没有版本号，直接缓存旧 HTML
     * 会在改标题/改配置后长时间不生效；ETag 让未变更的请求以 304 结束。
     * 后台启动壳（noindex）与错误页仍不缓存。
     */
    private static boolean cacheable(SeoModels.ReadingPage page) {
        return page.status() == 200 && page.kind() != SeoModels.Kind.SHELL;
    }

    private static String etag(String body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(digest, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static boolean etagMatches(HttpServletRequest request, String etag) {
        String header = request.getHeader("If-None-Match");
        if (header == null || header.isBlank()) return false;
        if ("*".equals(header.trim())) return true;
        for (String candidate : header.split(",")) {
            String value = candidate.trim();
            if (value.equals(etag) || value.equals("W/" + etag)) return true;
        }
        return false;
    }

    private ResponseEntity<String> html(int status, String body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Vary", "Accept-Encoding")
                .body(body);
    }
}
