package com.selfblog.config.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 站点品牌资产（SVG 等）的响应安全头（S1 的第二半）。
 *
 * <p>{@code /uploads/site-assets/**} 下的资源是同源公开服务的，且可以被直接当作顶层文档打开。
 * 上传时已按白名单净化 SVG，但净化是"尽力而为"的一层：只要浏览器把响应当文档渲染，
 * 任何漏网的内联样式/脚本都有机会以本站源执行并读取 localStorage 里的 JWT。
 * 因此这里再加一层强制：{@code default-src 'none'} 关掉一切外部加载与脚本，
 * {@code sandbox} 让文档运行在不透明源上（拿不到同源存储），{@code nosniff} 阻止
 * 浏览器按内容嗅探把文件当 HTML 解析。</p>
 *
 * <p>只作用于站点资产路径，不设全局 CSP —— Vue SPA 需要内联样式与构建期脚本。</p>
 */
@Component
public class SiteAssetSecurityHeaderInterceptor implements HandlerInterceptor {

    /** 站点资产路径前缀，与 SiteAssetServiceImpl 落盘目录、WebMvcConfig 资源映射一致。 */
    public static final String SITE_ASSET_PATH_PREFIX = "/uploads/site-assets/";

    private static final String CSP_VALUE = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("Content-Security-Policy", CSP_VALUE);
        response.setHeader("X-Content-Type-Options", "nosniff");
        return true;
    }
}
