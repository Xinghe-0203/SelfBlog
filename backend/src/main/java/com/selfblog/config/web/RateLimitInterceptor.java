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
        String matchedBase = null;
        for (Map.Entry<String, Integer> entry : allLimits.entrySet()) {
            String prefix = entry.getKey();
            String base = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            // 根路径和子路径均受保护，但不能把同名开头的另一个路径当作子路径。
            if ((path.equals(base) || path.startsWith(base + "/")) && base.length() > longestMatch) {
                maxRequests = entry.getValue();
                matched = true;
                longestMatch = base.length();
                matchedBase = base;
            }
        }

        if (!matched) {
            return true;
        }

        // 计数按**「命中的最长前缀 + 前缀之后的路径层数」**归桶，而不是按具体路径：
        // 按具体路径计数时 `/api/post/10`、`/api/post/11`… 各自一个桶，攻击者只要换 id
        // 就能让每个桶都停在阈值以下，前缀配额形同虚设（实测每个 id 都能各自刷满 10 次）。
        // 但只按前缀归桶会走向另一个极端：同一前缀下的读路径与写路径共用一份配额，
        // `/api/guest-comments/post/{id}` 的阅读会吃掉 `/api/guest-comments` 的 5/min
        // 创建配额（既有用例 readingCommentsDoesNotConsumeCreationQuota 明确要求互不消耗）。
        // 取「前缀 + 层数」后：同一层上的路径变量（换 id）共享一份配额，
        // 层数不同的路径各占一桶；最长前缀语义不变，`/api/post/list` 仍走自己的 60/min。
        String key = ip + ":" + matchedBase + "#" + pathDepthAfter(path, matchedBase);
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

    /**
     * 命中的最长前缀之后还剩多少层路径。`/api/post/` + `/api/post/10` → 1 层，
     * `/api/post/` + `/api/post/10/comments` → 2 层，`/api/guest-comments` + 它自身 → 0 层。
     * 同一层内的路径变量（各种 id）会归入同一个桶，换 id 无法绕过配额。
     */
    private int pathDepthAfter(String path, String base) {
        String rest = path.length() > base.length() ? path.substring(base.length()) : "";
        int depth = 0;
        for (String segment : rest.split("/")) {
            if (!segment.isEmpty()) {
                depth++;
            }
        }
        return depth;
    }

}