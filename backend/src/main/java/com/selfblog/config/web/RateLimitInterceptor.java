package com.selfblog.config.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting interceptor using Caffeine cache for in-memory rate limiting.
 * For production with multiple instances, use Redis + Lua script for distributed rate limiting.
 * Current implementation is per-instance; attackers could bypass by hitting different instances.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(50000)
            .build();

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();
        String ip = getClientIp(request);

        // 使用配置的限流阈值
        Map<String, Integer> allLimits = rateLimitProperties.getAllLimits();
        int maxRequests = 0;
        boolean matched = false;

        int longestMatch = -1;
        for (Map.Entry<String, Integer> entry : allLimits.entrySet()) {
            String prefix = entry.getKey();
            String base = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            // 根路径和子路径均受保护，但不能把同名开头的另一个路径当作子路径。
            if ((path.equals(base) || path.startsWith(base + "/")) && base.length() > longestMatch) {
                maxRequests = entry.getValue();
                matched = true;
                longestMatch = base.length();
            }
        }

        if (!matched) {
            return true;
        }

        String key = ip + ":" + path;
        AtomicInteger count = requestCounts.get(key, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();

        if (current > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"Too many requests, please try again later\"}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (isTrustedProxy(remoteAddr)) {
                return firstIp;
            }
        }

        if (xRealIp != null && !xRealIp.isEmpty()) {
            if (isTrustedProxy(remoteAddr)) {
                return xRealIp;
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String addr) {
        if (addr == null) {
            return false;
        }
        if (addr.startsWith("10.")) {
            return true;
        }
        if (addr.startsWith("192.168.")) {
            return true;
        }
        if (addr.equals("127.0.0.1") || addr.equals("0:0:0:0:0:0:0:1") || addr.equals("::1")) {
            return true;
        }
        if (addr.startsWith("172.")) {
            try {
                int secondOctet = Integer.parseInt(addr.split("\\.")[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}