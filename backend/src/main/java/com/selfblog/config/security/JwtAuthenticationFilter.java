package com.selfblog.config.security;

import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.LogUtils;
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

/**
 * JWT 认证过滤器
 * 每次请求都检查 Authorization Header 中的 Token
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

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

                    // 必须先验证 Token 签名，再检查黑名单
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

            filterChain.doFilter(request, response);
        } finally {
            LogUtils.clearMdc();
        }
    }
}
