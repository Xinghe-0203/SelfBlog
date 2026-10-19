package com.selfblog.config.security;

import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.LogUtils;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.utils.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器
 * 每次请求都检查 Authorization Header 中的 Token
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 写操作审计的路径前缀。刻意与 SecurityConfig 的 `.requestMatchers("/api/admin/**").hasRole("admin")`
     * 硬编码保持一致：审计范围必须等于鉴权范围，否则会出现「记了没鉴权的」或「鉴权了没记的」。
     * 该前缀不跟 app.api-prefix 走，因为 REST 的 /api 前缀由 WebMvcConfig 硬编码，
     * 而鉴权规则也硬编码 /api/admin/**，配置项只影响 SPA 回退。
     */
    private static final String AUDITED_ADMIN_PREFIX = "/api/admin/";

    /** 会被审计的写方法。GET/HEAD/OPTIONS 等只读请求不记录，避免刷爆 audit.log。 */
    private static final Set<String> AUDITED_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 如果请求携带 X-Trace-Id，预置到 MDC 中供 initMdc 使用
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId != null && !traceId.isEmpty()) {
                MDC.put(LogUtils.TRACE_ID, traceId);
            }
            LogUtils.initMdc();

            String token = jwtUtils.extractTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                try {
                    // 拒绝 refresh token 用于 API 访问，防止泄露后长期滥用
                    if (jwtUtils.isRefreshToken(token)) {
                        String uri = request.getRequestURI();
                        if (!uri.endsWith("/user/refresh")) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"message\":\"Refresh token cannot be used for API access\"}");
                            LogUtils.logSecurityEvent("REFUSED_REFRESH_TOKEN",
                                    "Refresh token used for API access",
                                    LogUtils.getClientIp(request));
                            return;
                        }
                        log.debug("Refresh token allowed on /user/refresh endpoint");
                    }

                    // 验证签名、过期、黑名单及当前数据库凭据版本（改密后跨重启失效）。
                    if (!jwtUtils.isTokenExpired(token) && !jwtUtils.isTokenRevoked(token)) {
                        Long userId = jwtUtils.getUserIdFromToken(token);
                        String username = jwtUtils.getUsernameFromToken(token);
                        String role = jwtUtils.getRoleFromToken(token);

                        LogUtils.setUserContext(userId, username);

                        UserContext userContext = new UserContext(userId, role);

                        List<GrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority(role != null ? "ROLE_" + role : "ROLE_USER")
                        );
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userContext, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT Authentication successful for user: {}", username);
                    }
                } catch (Exception e) {
                    log.warn("JWT Authentication failed: {}", e.getMessage());
                    LogUtils.logSecurityEvent("JWT_AUTH_FAILED", e.getMessage(),
                            LogUtils.getClientIp(request));
                }
            }

            // 管理写操作审计：只对已通过鉴权（ROLE_admin）的 /api/admin/** 写请求记录。
            // 必须在 filterChain 之后写：结果状态码只有链执行完才知道，这样 401/403
            // 不会被误记成管理操作。此时 SecurityContext 仍可读——清空发生在更外层的
            // SecurityContextHolderFilter，要等整条链返回之后。
            // 没有新增 HandlerInterceptor 是刻意的：WebMvcConfig 是 task-7 的写入范围，
            // 复用本过滤器可避免写冲突，且本过滤器正好位于鉴权链的正确位置。
            boolean auditAdminWrite = isAuditedAdminWrite(request);
            Throwable auditError = null;
            try {
                filterChain.doFilter(request, response);
            } catch (Throwable e) {
                auditError = e;
                throw e;
            } finally {
                if (auditAdminWrite) {
                    logAdminWriteOperation(request, response, auditError);
                }
            }
        } finally {
            LogUtils.clearMdc();
        }
    }

    /**
     * 是否属于需要审计的管理端写操作。只看请求本身，不看鉴权结果，
     * 鉴权结论在 {@link #logAdminWriteOperation} 里用响应状态码与 SecurityContext 判定。
     */
    private boolean isAuditedAdminWrite(HttpServletRequest request) {
        return AUDITED_METHODS.contains(request.getMethod())
                && request.getRequestURI().startsWith(AUDITED_ADMIN_PREFIX);
    }

    /**
     * 记录管理员写操作审计日志。
     * status &lt; 400 且当前身份为管理员才算成功执行，否则记为 rejected（含 401/403）。
     * 链中抛异常时状态码可能是 200，故用 {@code error != null} 一并判为失败。
     */
    private void logAdminWriteOperation(HttpServletRequest request, HttpServletResponse response,
                                        Throwable error) {
        int status = response.getStatus();
        boolean admin = SecurityUtils.isCurrentUserAdmin();
        boolean success = error == null && status < 400 && admin;
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        LogUtils.logSecurityEvent("ADMIN_OPERATION",
                "method=" + request.getMethod()
                        + ", uri=" + request.getRequestURI()
                        + ", status=" + status
                        + ", result=" + (success ? "success" : "rejected")
                        + ", userId=" + (userId != null ? userId : "anonymous"),
                LogUtils.getClientIp(request));
    }
}
