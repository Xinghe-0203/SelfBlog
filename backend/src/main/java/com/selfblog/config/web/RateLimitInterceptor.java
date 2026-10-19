package com.selfblog.config.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.selfblog.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

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
        // 与 MVC 查找路径保持一致：解码 URI、去掉应用上下文，编码同一路径不能绕过计数。
        String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
        // 与审计日志同源：两处必须给出同一个 clientIp，否则伪造 X-Forwarded-For 就能
        // 让审计记录的 IP 与限流计数归属的 IP 不一致。实现只在 LogUtils 里保留一份。
        String ip = LogUtils.getClientIp(request);

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
            // 限流命中此前完全无日志：生产 8 小时内 429 在 request.log / audit.log 里
            // 都看不到。这里写 WARN 到 LogUtils 的审计通道（audit.log）。
            LogUtils.logSecurityEvent("RATE_LIMIT_EXCEEDED",
                    "path=" + path + ", method=" + request.getMethod()
                            + ", limit=" + maxRequests + "/min, count=" + current,
                    ip);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"Too many requests, please try again later\"}");
            return false;
        }
        return true;
    }

}