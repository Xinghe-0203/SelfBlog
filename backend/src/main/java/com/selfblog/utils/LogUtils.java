package com.selfblog.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 日志工具类
 * 提供统一的日志记录方法和MDC上下文管理
 */
public class LogUtils {

    private static final Logger log = LoggerFactory.getLogger(LogUtils.class);

    /**
     * 性能日志专用 logger。
     * 使用独立 logger 名，使 logback 能把 {@code [PERFORMANCE]} 事件路由到 performance.log，
     * 而不是跟着 {@link #log} 落进 audit.log。绑定见 logback-spring.xml 的
     * {@code <logger name="com.selfblog.performance">}。
     */
    private static final Logger perfLog = LoggerFactory.getLogger("com.selfblog.performance");

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

    /**
     * 客户端可自报的追踪 ID 白名单字符集。
     * 只允许 8-64 位的字母/数字/下划线/连字符，因此不可能包含 CRLF、控制字符或日志分隔符，
     * 也就无法借 X-Trace-Id 伪造日志行。不匹配时一律改用服务端 UUID。
     */
    private static final Pattern TRUSTED_TRACE_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    /** 日志值清洗时替换控制字符的占位符 */
    private static final String SANITIZE_PLACEHOLDER = "_";

    /** 敏感查询参数名（小写比较）。命中即掩码，避免令牌/凭据整串入盘。 */
    private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "id_token",
            "password",
            "passwd",
            "pwd",
            "secret",
            "client_secret",
            "code",
            "key",
            "apikey",
            "api_key",
            "auth",
            "authorization",
            "credential",
            "credentials",
            "signature",
            "sign"
    );

    /** 敏感值的掩码 */
    private static final String MASK = "***";

    private LogUtils() {
        // 私有构造函数，防止实例化
    }

    /**
     * 初始化MDC上下文
     * 为每个请求生成唯一的traceId并设置请求相关信息
     */
    public static void initMdc() {
        // 生成或获取traceId：MDC 里已有的值也必须过白名单，因为它是外部可控的
        // X-Trace-Id 在过滤器里预置进来的；不校验就会把攻击者选定的字符串写进每一行日志。
        String traceId = trustedTraceId(MDC.get(TRACE_ID));
        if (traceId == null) {
            traceId = newTraceId();
        }
        MDC.put(TRACE_ID, traceId);

        // 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            MDC.put(REQUEST_URI, sanitizeLogValue(request.getRequestURI(), 512));
            MDC.put(REQUEST_METHOD, sanitizeLogValue(request.getMethod(), 16));
            MDC.put(CLIENT_IP, sanitizeLogValue(getClientIp(request), 64));
            MDC.put(USER_AGENT, sanitizeLogValue(request.getHeader("User-Agent"), 256));
        }
    }

    /**
     * 生成服务端追踪 ID（UUID 去掉连字符后取前 16 位）。
     *
     * @return 16 位十六进制追踪 ID
     */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 校验客户端自报的追踪 ID 是否可采信。
     * 只有完全匹配 {@code ^[A-Za-z0-9_-]{8,64}$} 的值才会被原样返回，否则返回 {@code null}
     * 由调用方改用服务端生成的 UUID。白名单字符集天然排除 CRLF 与控制字符，
     * 因此伪造的 X-Trace-Id 无法注入日志行。
     *
     * @param candidate 客户端自报的追踪 ID（可为 null）
     * @return 合法的追踪 ID，或 null 表示不可采信
     */
    public static String trustedTraceId(String candidate) {
        return candidate != null && TRUSTED_TRACE_ID.matcher(candidate).matches() ? candidate : null;
    }

    /**
     * 清洗进入日志的外部可控值：剥离 CRLF 与控制字符并截断，保证一条日志恒为一行。
     * 可打印字符（含中文）原样保留，不做转义，避免破坏既有日志文案与解析口径。
     *
     * @param value  原始值（可为 null）
     * @param maxLen 截断长度（&lt;=0 表示不截断）
     * @return 清洗后的值；入参为 null 时返回 "null"
     */
    public static String sanitizeLogValue(String value, int maxLen) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(Math.min(value.length(), Math.max(maxLen, 0)));
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t' || c < 0x20 || c == 0x7F) {
                sb.append(SANITIZE_PLACEHOLDER);
            } else {
                sb.append(c);
            }
            if (maxLen > 0 && sb.length() >= maxLen) {
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 对查询串里的敏感参数值做掩码，参数名与其余参数保持可读。
     * 例如 {@code token=abc123&tag=xx} → {@code token=***&tag=xx}。
     * 大小写不敏感；不含敏感参数时原样返回，因此 {@code ?tag=xx} 等正常查询串不受影响。
     *
     * @param queryString 原始查询串（可为 null）
     * @return 掩码后的查询串
     */
    public static String maskSensitiveQuery(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return queryString;
        }
        String[] pairs = queryString.split("&", -1);
        StringBuilder out = new StringBuilder(queryString.length());
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                out.append('&');
            }
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq < 0) {
                out.append(pair);
                continue;
            }
            String name = pair.substring(0, eq);
            if (isSensitiveParam(name)) {
                out.append(name).append('=').append(MASK);
            } else {
                out.append(pair);
            }
        }
        return out.toString();
    }

    /** 参数名是否为敏感参数（大小写不敏感，忽略 URL 编码或字面量的方括号后缀） */
    private static boolean isSensitiveParam(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        // 兼容 token[] / token[0] 这类数组式写法，以及 URL 编码的 token%5B%5D
        int bracket = normalized.indexOf('[');
        if (bracket < 0) {
            bracket = normalized.indexOf("%5b");
        }
        if (bracket > 0) {
            normalized = normalized.substring(0, bracket);
        }
        return SENSITIVE_QUERY_PARAMS.contains(normalized);
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
            MDC.put(USERNAME, sanitizeLogValue(username, 64));
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
        String remoteAddr = request.getRemoteAddr();

        // 只有直连对端本身是可信代理时才采用代理头：否则任何客户端都能靠伪造
        // X-Forwarded-For 污染审计日志与限流计数（同一个请求在两处得到不同 IP）。
        if (isTrustedProxy(remoteAddr)) {
            for (String header : TRUSTED_PROXY_HEADERS) {
                String headerValue = request.getHeader(header);
                if (headerValue == null || headerValue.isEmpty() || UNKNOWN_IP.equalsIgnoreCase(headerValue.trim())) {
                    continue;
                }
                // X-Forwarded-For 可能是 "client, proxy1, proxy2" 列表，取第一个合法 IP。
                for (String candidate : headerValue.split(",")) {
                    String ip = candidate.trim();
                    if (isValidIp(ip)) {
                        log.debug("IP from {}: {}", header, ip);
                        return ip;
                    }
                }
            }
        }

        // 回退到request.getRemoteAddr()
        if (isValidIp(remoteAddr)) {
            log.debug("IP from getRemoteAddr: {}", remoteAddr);
            return remoteAddr;
        }

        // 无法获取有效IP时返回默认值
        log.warn("Unable to get valid client IP, returning 0.0.0.0");
        return "0.0.0.0";
    }

    /**
     * 从当前请求上下文获取客户端 IP。
     * 供没有 {@link HttpServletRequest} 形参的服务层审计调用；不在请求线程内
     * （定时任务、直接调用服务方法的单元测试）时返回 {@link #UNKNOWN_IP}。
     *
     * @return 客户端IP，无法获取时返回 "unknown"
     */
    public static String getClientIpFromCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? getClientIp(attrs.getRequest()) : UNKNOWN_IP;
    }

    /**
     * 判断直连对端地址是否属于可信代理网段。
     * 语义与限流拦截器历史上的私有实现一致：10./192.168./127.0.0.1/::1/0:0:0:0:0:0:0:1/172.16-31。
     * 该判定是审计与限流共用的唯一来源，两处必须同源，避免同一请求得到不同 IP。
     *
     * @param addr 直连对端地址（request.getRemoteAddr()）
     * @return true 表示可信代理，可采用代理头
     */
    public static boolean isTrustedProxy(String addr) {
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
        log.info("[API] [{}] {} - {}",
                sanitizeLogValue(module, 64),
                sanitizeLogValue(action, 128),
                sanitizeLogValue(detail, 512));
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
                userId,
                sanitizeLogValue(username, 64),
                sanitizeLogValue(action, 128),
                sanitizeLogValue(detail, 512));
    }

    /**
     * 记录安全事件日志
     *
     * @param event    事件类型
     * @param detail   详细信息
     * @param clientIp 客户端IP
     */
    public static void logSecurityEvent(String event, String detail, String clientIp) {
        log.warn("[SECURITY] event={}, detail={}, clientIp={}",
                sanitizeLogValue(event, 64),
                sanitizeLogValue(detail, 512),
                sanitizeLogValue(clientIp, 64));
    }

    /**
     * 记录性能日志
     *
     * @param operation 操作名称
     * @param startTime 开始时间（毫秒）
     */
    public static void logPerformance(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        String safeOperation = sanitizeLogValue(operation, 512);
        if (duration > 3000) {
            perfLog.warn("[PERFORMANCE] SLOW operation={}, duration={}ms", safeOperation, duration);
        } else if (duration > 1000) {
            perfLog.info("[PERFORMANCE] operation={}, duration={}ms", safeOperation, duration);
        } else {
            perfLog.debug("[PERFORMANCE] operation={}, duration={}ms", safeOperation, duration);
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
        String safeOperation = sanitizeLogValue(operation, 32);
        String safeTable = sanitizeLogValue(table, 64);
        String safeDetail = sanitizeLogValue(detail, 512);
        if (duration > 1000) {
            log.warn("[DB] SLOW {} on {} - {} ({}ms)", safeOperation, safeTable, safeDetail, duration);
        } else {
            log.debug("[DB] {} on {} - {} ({}ms)", safeOperation, safeTable, safeDetail, duration);
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
        log.debug("[CACHE] {} key={}, hit={}",
                sanitizeLogValue(operation, 32), sanitizeLogValue(key, 256), hit);
    }

    /**
     * 记录通知发送日志
     *
     * @param userId  接收用户ID
     * @param type    通知类型
     * @param detail  详细信息
     */
    public static void logNotification(Long userId, String type, String detail) {
        log.info("[NOTIFICATION] userId={}, type={}, detail={}",
                userId, sanitizeLogValue(type, 64), sanitizeLogValue(detail, 512));
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
                userId, sanitizeLogValue(filename, 255), size, success);
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
        String safeUsername = sanitizeLogValue(username, 64);
        String safeClientIp = sanitizeLogValue(clientIp, 64);
        if (success) {
            log.info("[LOGIN] username={}, clientIp={}, success=true", safeUsername, safeClientIp);
        } else {
            log.warn("[LOGIN] username={}, clientIp={}, success=false, reason={}",
                    safeUsername, safeClientIp, sanitizeLogValue(reason, 256));
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
        log.info("[REGISTER] username={}, clientIp={}, success={}",
                sanitizeLogValue(username, 64), sanitizeLogValue(clientIp, 64), success);
    }
}
