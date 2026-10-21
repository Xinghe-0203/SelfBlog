package com.selfblog.config.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Web MVC 配置
 * 配置拦截器、静态资源映射和上传文件访问
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.base-path:uploads}")
    private String uploadBasePath;

    @Value("${upload.root-path:#{systemProperties['user.dir']}}")
    private String uploadRootPath;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private HttpRequestLoggingInterceptor httpRequestLoggingInterceptor;

    @Autowired
    private SiteAssetSecurityHeaderInterceptor siteAssetSecurityHeaderInterceptor;

    @Value("${app.api-prefix:/api}")
    private String apiPrefix;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        if (apiPrefix != null && !apiPrefix.isBlank()) {
            configurer.addPathPrefix(apiPrefix, HandlerTypePredicate.forAnnotation(RestController.class));
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 请求日志拦截器 - 对所有请求生效
        registry.addInterceptor(httpRequestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/uploads/**",           // 静态资源
                        "/actuator/**",          // 监控端点
                        "/doc.html",             // Knife4j UI
                        "/swagger-resources/**", // Swagger资源
                        "/v3/api-docs/**"        // OpenAPI文档
                )
                .order(1); // 最高优先级

        // 限流拦截器 - 覆盖所有API路径，由拦截器内部按路径匹配合适的速率限制
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(2);

        // 站点资产响应安全头（S1）：/uploads/site-assets/** 是公开同源资源且可作顶层文档打开，
        // 这里补 CSP + nosniff。日志拦截器已排除 /uploads/**，所以两者不冲突。
        registry.addInterceptor(siteAssetSecurityHeaderInterceptor)
                .addPathPatterns("/uploads/site-assets/**")
                .order(0);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        java.nio.file.Path basePath = Paths.get(uploadRootPath, uploadBasePath).toAbsolutePath().normalize();
        String uploadPath = basePath.toString().replace('\\', '/');

        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }

        // 上传文件：文件名不保证带内容哈希（用户可覆盖同名文件），所以只做短缓存 +
        // 允许协商，不能 immutable，否则作者换了头像/封面访客长时间看不到。
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());

        // Vite 产物按内容哈希命名（assets/index-BNvWXfcT.js 等），内容一变文件名就变，
        // 因此可以安全地长期强缓存。此前没有任何 Cache-Control，生产日志里同一个
        // /assets/*.css|js 被重复 200 下载 12-15 次（1300 行里只有 2 次 304）。
        // 【location 语义（已实测）】对 /** 模式，Spring 用 extractPathWithinPattern 取
        // 通配部分作为资源路径：请求 /assets/x.js 得到 "x.js"，因此 location 必须包含
        // assets 段（classpath:/static/assets/）；写成 classpath:/static/ 会解析到
        // classpath:/static/x.js 而 404。精确模式（/favicon.svg）走直匹配分支、用完整
        // 路径，所以那里的 location 是 classpath:/static/。两者都已用真实请求验证。
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        // favicon 与阅读页样式表不在哈希目录下，给中等缓存（1 天）并允许协商：
        // 换图标后最多 1 天生效，同时避免每次访问都重新下载。
        // 精确路径模式 → location 用 classpath:/static/（见上），两个路径都由
        // SecurityConfig 显式放行，且不含点号的 SPA 通配不会命中它们。
        registry.addResourceHandler("/favicon.svg")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic().mustRevalidate());
        registry.addResourceHandler("/seo-reading.css")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic().mustRevalidate());

        // 其余 public/ 资源（占位图等）保持默认的 classpath:/static/ 资源链，不额外设缓存。
    }
}
