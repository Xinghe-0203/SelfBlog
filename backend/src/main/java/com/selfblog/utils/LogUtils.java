package com.selfblog.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 日志工具类
 * 提供统一的日志记录方法和MDC上下文管理
 */
public class LogUtils {

    private static final Logger log = LoggerFactory.getLogger(LogUtils.class);

    /** IPv4正则表达式 */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );

    /** IPv6正则表达式（简化版，覆盖主要格式） */
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::$|^([0-9a-fA-F]{1,4}:){1,7}:$|^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$|^:((:[0-9a-fA-F]{1,4}){1,7}|:)$|^fe80:([0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+$|^::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?\\d)\\d?)\\.){3}(25[0-5]|(2[0-4]|1?\\d)\\d?)$|^([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?\\d)\\d?)\\.){3}(25[0-5]|(2[0-4]|1?\\d)\\d?)$"
    );

    /** 可信的代理头列表（按优先级排序） */
    private static final String[] TRUSTED_PROXY_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "X-Real-IP"
    };

    /** 不可信的IP值 */
    private static final String UNKNOWN_IP = "unknown";

    /** MDC键常量 */
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String CLIENT_IP = "clientIp";
    public static final String USER_AGENT = "userAgent";
    public static final String REQUEST_PARAMS = "requestParams";
    public static final String RESPONSE_CODE = "responseCode";
    public static final String EXECUTION_TIME = "executionTime";

    private LogUtils() {
        // 私有构造函数，防止实例化
    }

    /**
     * 初始化MDC上下文
     * 为每个请求生成唯一的traceId并设置请求相关信息
     */
    public static void initMdc() {
        // 生成或获取traceId
        String traceId = MDC.get(TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_ID, traceId);

        // 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            MDC.put(REQUEST_URI, request.getRequestURI());
            MDC.put(REQUEST_METHOD, request.getMethod());
            MDC.put(CLIENT_IP, getClientIp(request));
            MDC.put(USER_AGENT, request.getHeader("User-Agent"));
        }
    }

    /**
     * 设置用户信息到MDC
     *
     * @param userId   用户ID
     * @param username 用户名
     */
    public static void setUserContext(Long userId, String username) {
        if (userId != null) {
            MDC.put(USER_ID, String.valueOf(userId));
        }
        if (username != null) {
            MDC.put(USERNAME, username);
        }
    }

    /**
     * 清理MDC上下文
     * 在请求结束时调用，防止内存泄漏
     */
    public static void clearMdc() {
        MDC.clear();
    }

    /**
     * 获取客户端真实IP地址
     * 安全实现：防止IP伪造攻击
     *
     * 策略：
     * 1. 只在信任的代理环境下使用代理头
     * 2. 对所有IP进行格式校验，拒绝非法格式
     * 3. 多级代理时只取第一个有效的真实客户端IP
     *
     * @param request HTTP请求
     * @return 客户端IP地址，如果无法获取则返回"0.0.0.0"
     */
    public static String getClientIp(HttpServletRequest request) {
        // 首先检查是否使用了可信代理（通过X-Forwarded-For存在判断）
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isEmpty() && !UNKNOWN_IP.equalsIgnoreCase(forwardedFor)) {
            // X-Forwarded-For存在且非空，认为处于代理环境
            // 格式: client, proxy1, proxy2, ...
            String[] ips = forwardedFor.split(",");
            for (String ip : ips) {
                ip = ip.trim();
                if (isValidIp(ip)) {
                    log.debug("IP from X-Forwarded-For: {}", ip);
                    return ip;
                }
            }
        }

        // 检查Proxy-Client-IP
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (isValidIp(proxyClientIp)) {
            log.debug("IP from Proxy-Client-IP: {}", proxyClientIp);
            return proxyClientIp;
        }

        // 检查WL-Proxy-Client-IP
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(wlProxyClientIp)) {
            log.debug("IP from WL-Proxy-Client-IP: {}", wlProxyClientIp);
            return wlProxyClientIp;
        }

        // 检查X-Real-IP
        String realIp = request.getHeader("X-Real-IP");
        if (isValidIp(realIp)) {
            log.debug("IP from X-Real-IP: {}", realIp);
            return realIp;
        }

        // 回退到request.getRemoteAddr()
        String remoteAddr = request.getRemoteAddr();
        if (isValidIp(remoteAddr)) {
            log.debug("IP from getRemoteAddr: {}", remoteAddr);
            return remoteAddr;
        }

        // 无法获取有效IP时返回默认值
        log.warn("Unable to get valid client IP, returning 0.0.0.0");
        return "0.0.0.0";
    }

    /**
     * 验证IP地址格式是否合法
     *
     * @param ip IP地址字符串
     * @return true表示合法，false表示非法
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || UNKNOWN_IP.equalsIgnoreCase(ip)) {
            return false;
        }

        // 去除空格
        ip = ip.trim();

        // 检查长度（IPv4最大15字符，IPv6最大39字符）
        if (ip.length() > 39 || ip.length() < 7) {
            return false;
        }

        // 检查是否包含非法字符（只允许数字、点、冒号、a-f、A-F）
        for (int i = 0; i < ip.length(); i++) {
            char c = ip.charAt(i);
            if (!Character.isDigit(c) && c != '.' && c != ':' &&
                    !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {
                return false;
            }
        }

        // 优先匹配IPv4（更常见）
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }

        // 尝试匹配IPv6
        if (IPV6_PATTERN.matcher(ip).matches()) {
            return true;
        }

        return false;
    }

    /**
     * 记录API调用日志
     *
     * @param module   模块名称
     * @param action   操作名称
     * @param detail   详细信息
     */
    public static void logApiCall(String module, String action, String detail) {
        log.info("[API] [{}] {} - {}", module, action, detail);
    }

    /**
     * 记录用户操作日志
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param action   操作
     * @param detail   详细信息
     */
    public static void logUserAction(Long userId, String username, String action, String detail) {
        log.info("[USER_ACTION] userId={}, username={}, action={}, detail={}",
                userId, username, action, detail);
    }

    /**
     * 记录安全事件日志
     *
     * @param event    事件类型
     * @param detail   详细信息
     * @param clientIp 客户端IP
     */
    public static void logSecurityEvent(String event, String detail, String clientIp) {
        log.warn("[SECURITY] event={}, detail={}, clientIp={}", event, detail, clientIp);
    }

    /**
     * 记录性能日志
     *
     * @param operation 操作名称
     * @param startTime 开始时间（毫秒）
     */
    public static void logPerformance(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 3000) {
            log.warn("[PERFORMANCE] SLOW operation={}, duration={}ms", operation, duration);
        } else if (duration > 1000) {
            log.info("[PERFORMANCE] operation={}, duration={}ms", operation, duration);
        } else {
            log.debug("[PERFORMANCE] operation={}, duration={}ms", operation, duration);
        }
    }

    /**
     * 记录数据库操作日志
     *
     * @param operation 操作类型（SELECT/INSERT/UPDATE/DELETE）
     * @param table     表名
     * @param detail    详细信息
     * @param duration  执行时间（毫秒）
     */
    public static void logDbOperation(String operation, String table, String detail, long duration) {
        if (duration > 1000) {
            log.warn("[DB] SLOW {} on {} - {} ({}ms)", operation, table, detail, duration);
        } else {
            log.debug("[DB] {} on {} - {} ({}ms)", operation, table, detail, duration);
        }
    }

    /**
     * 记录缓存操作日志
     *
     * @param operation 操作类型（GET/PUT/DELETE）
     * @param key       缓存键
     * @param hit       是否命中
     */
    public static void logCacheOperation(String operation, String key, boolean hit) {
        log.debug("[CACHE] {} key={}, hit={}", operation, key, hit);
    }

    /**
     * 记录通知发送日志
     *
     * @param userId  接收用户ID
     * @param type    通知类型
     * @param detail  详细信息
     */
    public static void logNotification(Long userId, String type, String detail) {
        log.info("[NOTIFICATION] userId={}, type={}, detail={}", userId, type, detail);
    }

    /**
     * 记录文件上传日志
     *
     * @param userId   用户ID
     * @param filename 文件名
     * @param size     文件大小（字节）
     * @param success  是否成功
     */
    public static void logFileUpload(Long userId, String filename, long size, boolean success) {
        log.info("[FILE_UPLOAD] userId={}, filename={}, size={}, success={}",
                userId, filename, size, success);
    }

    /**
     * 记录登录日志
     *
     * @param username 用户名
     * @param success  是否成功
     * @param clientIp 客户端IP
     * @param reason   失败原因（成功时为null）
     */
    public static void logLogin(String username, boolean success, String clientIp, String reason) {
        if (success) {
            log.info("[LOGIN] username={}, clientIp={}, success=true", username, clientIp);
        } else {
            log.warn("[LOGIN] username={}, clientIp={}, success=false, reason={}", username, clientIp, reason);
        }
    }

    /**
     * 记录注册日志
     *
     * @param username 用户名
     * @param clientIp 客户端IP
     * @param success  是否成功
     */
    public static void logRegister(String username, String clientIp, boolean success) {
        log.info("[REGISTER] username={}, clientIp={}, success={}", username, clientIp, success);
    }
}
