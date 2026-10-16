package com.selfblog.config.security;

import com.selfblog.utils.LogUtils;
import com.selfblog.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * CORS 允许的来源列表
     * 可通过环境变量 CORS_ALLOWED_ORIGINS 配置，多个来源用逗号分隔
     * 示例: http://localhost:8080,http://127.0.0.1:8080,https://selfblog.com
     */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8825,http://127.0.0.1:3000,http://127.0.0.1:8825}")
    private String allowedOrigins;

    @Value("${bcrypt.strength:12}")
    private int bcryptStrength;

    @Value("${app.single-author-mode:true}")
    private boolean singleAuthorMode;

    @Value("${app.api-prefix:/api}")
    private String apiPrefix;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        List<String> validatedOrigins = new ArrayList<>();
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.endsWith("*")) {
                    log.warn("CORS 配置包含通配符，已拒绝: {}", trimmed);
                    continue;
                }
                configuration.addAllowedOriginPattern(trimmed);
            }
        }
        configuration.addAllowedHeader("Authorization");
        configuration.addAllowedHeader("Content-Type");
        configuration.addAllowedHeader("Refresh-Token");
        configuration.addAllowedHeader("X-Requested-With");
        configuration.addAllowedHeader("X-Trace-Id");
        configuration.addAllowedHeader("X-XSRF-TOKEN");
        configuration.addAllowedHeader("X-Visitor-Id");
        configuration.addAllowedMethod(HttpMethod.GET);
        configuration.addAllowedMethod(HttpMethod.POST);
        configuration.addAllowedMethod(HttpMethod.PUT);
        configuration.addAllowedMethod(HttpMethod.DELETE);
        configuration.addAllowedMethod(HttpMethod.PATCH);
        configuration.addAllowedMethod(HttpMethod.OPTIONS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 记录管理员操作审计日志
     */
    private void logAdminOperation(String action, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        String clientIp = LogUtils.getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();
        LogUtils.logSecurityEvent("ADMIN_OPERATION",
                "action=" + action + ", uri=" + uri + ", method=" + method + ", userId=" + (userId != null ? userId : "anonymous"),
                clientIp);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled原因: 本系统使用JWT Bearer Token认证，token通过Authorization header传递
            // 浏览器同源策略阻止跨域网站读取Authorization header，因此CSRF攻击不可行
            // 如未来改用Cookie认证，需重新启用CSRF保护
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), geolocation=(), microphone=(), payment=(), usb=()"))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    String json = "{\"code\":401,\"message\":\"认证失败，请先登录\"}";
                    response.getWriter().write(json);
                    response.getWriter().flush();
                    LogUtils.logSecurityEvent("AUTH_ENTRY_POINT", authException.getMessage(),
                            LogUtils.getClientIp(request));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    String json = "{\"code\":403,\"message\":\"权限不足，拒绝访问\"}";
                    response.getWriter().write(json);
                    response.getWriter().flush();
                    LogUtils.logSecurityEvent("ACCESS_DENIED", accessDeniedException.getMessage(),
                            LogUtils.getClientIp(request));
                })
            )
            .authorizeHttpRequests(authorize -> {
                authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error", "/error/**").permitAll()
                        // Let disabled registration handlers return their explicit 403 response,
                        // rather than converting anonymous denials to the authentication entry point's 401.
                        .requestMatchers("/api/user/register", "/api/auth/register/**", "/user/register", "/auth/register/**").permitAll()
                        .requestMatchers("/api/user/login", "/api/user/refresh", "/api/auth/password/send-code", "/api/auth/password/reset-password",
                                "/user/login", "/user/refresh", "/auth/password/send-code", "/auth/password/reset-password").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**",
                                "/api/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("admin")
                         .requestMatchers(HttpMethod.GET, "/api/site/public", "/site/public").permitAll()
                         .requestMatchers(HttpMethod.GET, "/api/site/pages", "/api/site/pages/**", "/site/pages", "/site/pages/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/visitor/track", "/visitor/track").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/post/{id}", "/post/{id}").permitAll()
                        // 文章详情页底部的上一篇/下一篇，访客也要能拿
                        .requestMatchers(HttpMethod.GET, "/api/post/{id}/adjacent", "/post/{id}/adjacent").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/post/list", "/post/list").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/post/search/**", "/post/search/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/post/{id}/view", "/post/{id}/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tag/**", "/tag/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/post/**", "/media/post/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/statistics/community", "/statistics/community").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trending/**", "/trending/**").permitAll()
                        .requestMatchers("/api/anonymous-likes/**", "/anonymous-likes/**").permitAll()
                        .requestMatchers("/api/guest-comments/**", "/guest-comments/**").permitAll()
                        .requestMatchers("/api/gallery/**", "/gallery/**").permitAll();

                // Outside /api the production app only exposes the bundled SPA and public uploads.
                // Disable this broad history fallback in tests, where controller paths are unprefixed.
                if (apiPrefix != null && !apiPrefix.isBlank()) {
                    // /feed.xml、/atom.xml、/sitemap.xml 带扩展名，不会被下面的 SPA 回退
                    // （[^\.]* 只匹配不含点号的路径）命中，必须单独放行，否则会落到
                    // 末尾「仅 ROLE_admin」的兜底规则返回 401，订阅器与爬虫都取不到。
                    // /favicon.svg 同理：它是 index.html 的默认图标兜底。
                    authorize.requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/favicon.svg",
                            "/robots.txt", "/feed.xml", "/atom.xml", "/sitemap.xml",
                            "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}",
                            "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}/**").permitAll();
                }

                // The public site is read-only. Every other API, including legacy social
                // endpoints and all content mutations, is reserved for the site author.
                authorize.anyRequest().access((authentication, context) -> {
                    if (!singleAuthorMode) {
                        return new org.springframework.security.authorization.AuthorizationDecision(authentication.get().isAuthenticated());
                    }
                    return new org.springframework.security.authorization.AuthorizationDecision(
                            authentication.get().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_admin")));
                });
            })
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
