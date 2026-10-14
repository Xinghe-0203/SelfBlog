package com.selfblog.config.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;

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
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        java.nio.file.Path basePath = Paths.get(uploadRootPath, uploadBasePath).toAbsolutePath().normalize();
        String uploadPath = basePath.toString().replace('\\', '/');

        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
