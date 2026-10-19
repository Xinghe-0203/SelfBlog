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

/**
 * HTTP请求日志拦截器
 * 记录所有HTTP请求的详细信息，包括请求处理时间
 *
 * 所有进入日志的外部可控值（X-Trace-Id、queryString、User-Agent、clientIp、异常消息）
 * 都先经 {@link LogUtils} 清洗：剥离 CRLF/控制字符并截断，保证一条日志恒为一行；
 * 敏感查询参数值另行掩码。日志格式（前缀、字段顺序、文案）保持不变。
 */
@Component
public class HttpRequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingInterceptor.class);

    /** 开始时间属性键 */
    private static final String START_TIME = "requestStartTime";

    /** 慢请求阈值（毫秒） */
    private static final long SLOW_REQUEST_THRESHOLD = 3000;

    /** 各字段的日志截断上限 */
    private static final int MAX_METHOD_LENGTH = 16;
    private static final int MAX_URI_LENGTH = 512;
    private static final int MAX_QUERY_LENGTH = 512;
    private static final int MAX_CLIENT_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 200;
    private static final int MAX_ERROR_LENGTH = 512;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 客户端自报的 X-Trace-Id 必须先过白名单（^[A-Za-z0-9_-]{8,64}$）：
        // 非法值一律不写入 MDC，改由 initMdc() 生成服务端 UUID，
        // 否则攻击者可用 CRLF/超长/伪造值污染每一行日志。
        String clientTraceId = LogUtils.trustedTraceId(request.getHeader("X-Trace-Id"));
        if (clientTraceId != null) {
            MDC.put(LogUtils.TRACE_ID, clientTraceId);
        }
        // initMdc 会再次校验 MDC 中的既有值（含过滤器预置路径），非法则替换为服务端 UUID
        LogUtils.initMdc();

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
                LogUtils.sanitizeLogValue(method, MAX_METHOD_LENGTH),
                LogUtils.sanitizeLogValue(uri, MAX_URI_LENGTH),
                queryString != null
                        ? "?" + LogUtils.maskSensitiveQuery(
                                LogUtils.sanitizeLogValue(queryString, MAX_QUERY_LENGTH))
                        : "",
                LogUtils.sanitizeLogValue(clientIp, MAX_CLIENT_IP_LENGTH),
                truncate(userAgent, MAX_USER_AGENT_LENGTH));

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

        String method = LogUtils.sanitizeLogValue(request.getMethod(), MAX_METHOD_LENGTH);
        String uri = LogUtils.sanitizeLogValue(request.getRequestURI(), MAX_URI_LENGTH);
        int statusCode = response.getStatus();
        String clientIp = LogUtils.sanitizeLogValue(LogUtils.getClientIp(request), MAX_CLIENT_IP_LENGTH);

        // 记录响应信息
        if (ex != null) {
            log.error("[RESPONSE] {} {} status={} duration={}ms clientIp={} error={}",
                    method, uri, statusCode, duration, clientIp,
                    LogUtils.sanitizeLogValue(ex.getMessage(), MAX_ERROR_LENGTH));
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
     * 截断字符串到指定长度，并清洗 CRLF/控制字符。
     * 保留原有的 "..." 后缀语义；入参为 null 时返回 "null"（与既有日志一致）。
     */
    private String truncate(String str, int maxLength) {
        String safe = LogUtils.sanitizeLogValue(str, 0);
        return safe.length() > maxLength ? safe.substring(0, maxLength) + "..." : safe;
    }
}
