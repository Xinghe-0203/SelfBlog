package com.selfblog.config.web;

import com.selfblog.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

/**
 * HTTP请求日志拦截器
 * 记录所有HTTP请求的详细信息，包括请求处理时间
 */
@Component
public class HttpRequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingInterceptor.class);

    /** 开始时间属性键 */
    private static final String START_TIME = "requestStartTime";

    /** 慢请求阈值（毫秒） */
    private static final long SLOW_REQUEST_THRESHOLD = 3000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 初始化MDC上下文
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        LogUtils.initMdc();
        MDC.put(LogUtils.TRACE_ID, traceId);

        // 记录请求开始
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        // 记录请求详情
        String clientIp = LogUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        log.info("[REQUEST] {} {} {} from IP={} UA={}",
                method, uri,
                queryString != null ? "?" + queryString : "",
                clientIp,
                truncate(userAgent, 200));

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 请求处理完成后的逻辑（如果需要）
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        int statusCode = response.getStatus();
        String clientIp = LogUtils.getClientIp(request);

        // 记录响应信息
        if (ex != null) {
            log.error("[RESPONSE] {} {} status={} duration={}ms clientIp={} error={}",
                    method, uri, statusCode, duration, clientIp, ex.getMessage());
        } else if (duration > SLOW_REQUEST_THRESHOLD) {
            log.warn("[RESPONSE] SLOW {} {} status={} duration={}ms clientIp={}",
                    method, uri, statusCode, duration, clientIp);
        } else if (statusCode >= 400) {
            log.warn("[RESPONSE] {} {} status={} duration={}ms clientIp={}",
                    method, uri, statusCode, duration, clientIp);
        } else {
            log.info("[RESPONSE] {} {} status={} duration={}ms clientIp={}",
                    method, uri, statusCode, duration, clientIp);
        }

        // 记录性能日志
        LogUtils.logPerformance(method + " " + uri, startTime);

        // 清理MDC上下文
        LogUtils.clearMdc();
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
