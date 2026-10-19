package com.selfblog.config.web;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.selfblog.utils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpRequestLoggingInterceptor 的日志注入与脱敏测试。
 *
 * 覆盖：
 * - 伪造 X-Trace-Id（CRLF / 超长 / 非法字符）不被采用，回落服务端 UUID
 * - 合法 X-Trace-Id 原样保留（回归）
 * - CRLF 的 UA / 查询串 / 异常消息被清洗成单行
 * - 敏感查询参数值被掩码，普通参数保持可读
 * - 既有日志格式（[REQUEST]/[RESPONSE] 前缀、字段名与顺序）未被破坏
 */
class HttpRequestLoggingInterceptorTest {

    private HttpRequestLoggingInterceptor interceptor;
    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger interceptorLogger;

    @BeforeEach
    void setUp() {
        // 同一 JVM 内其他测试类可能残留 MDC，先清空保证用例之间相互独立
        MDC.clear();
        interceptor = new HttpRequestLoggingInterceptor();
        interceptorLogger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(HttpRequestLoggingInterceptor.class);
        appender = new ListAppender<>();
        appender.start();
        interceptorLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        interceptorLogger.detachAppender(appender);
        MDC.clear();
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }

    // ==================== A. traceId 白名单 ====================

    @Test
    @DisplayName("伪造的 X-Trace-Id 含 CRLF 时不被采用，回落为服务端 UUID")
    void traceIdWithCrlfIsRejected() {
        var request = request("GET", "/api/post/list");
        request.addHeader("X-Trace-Id", "abc12345\r\n2026-01-01 00:00:00.000 [evil] INFO injected - fake line");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String traceId = MDC.get(LogUtils.TRACE_ID);
        assertNotNull(traceId);
        assertNotEquals("abc12345\r\n2026-01-01 00:00:00.000 [evil] INFO injected - fake line", traceId);
        assertFalse(traceId.contains("\r"), "traceId 不得含 CR，实际: " + traceId);
        assertFalse(traceId.contains("\n"), "traceId 不得含 LF，实际: " + traceId);
        // 回落值必须是服务端 UUID 形态（16 位十六进制）
        assertTrue(traceId.matches("^[0-9a-f]{16}$"), "应回落为服务端 UUID，实际: " + traceId);
    }

    @Test
    @DisplayName("伪造的 X-Trace-Id 超长（>64）时不被采用")
    void overlongTraceIdIsRejected() {
        var request = request("GET", "/api/post/list");
        request.addHeader("X-Trace-Id", "a".repeat(65));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String traceId = MDC.get(LogUtils.TRACE_ID);
        assertTrue(traceId.matches("^[0-9a-f]{16}$"), "应回落为服务端 UUID，实际: " + traceId);
    }

    @Test
    @DisplayName("伪造的 X-Trace-Id 含非法字符或过短时不被采用")
    void malformedTraceIdIsRejected() {
        for (String forged : new String[]{
                "short",                 // 7 位，短于下限 8
                "abc 12345",             // 含空格
                "abc12345!",             // 含感叹号
                "abc12345<script>",      // 含尖括号
                "中文追踪标识中文",        // 非 ASCII
                ""                       // 空串
        }) {
            MDC.clear();
            var request = request("GET", "/api/post/list");
            request.addHeader("X-Trace-Id", forged);

            assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

            String traceId = MDC.get(LogUtils.TRACE_ID);
            assertTrue(traceId.matches("^[0-9a-f]{16}$"),
                    "非法 traceId \"" + forged + "\" 必须回落为 UUID，实际: " + traceId);
        }
    }

    @Test
    @DisplayName("合法 X-Trace-Id 原样保留（回归）")
    void legitimateTraceIdIsPreserved() {
        String legit = "63a8a58fb1d34159b9413f92a7340a11";
        var request = request("GET", "/api/post/list");
        request.addHeader("X-Trace-Id", legit);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        assertEquals(legit, MDC.get(LogUtils.TRACE_ID));
    }

    @Test
    @DisplayName("边界：8 位与 64 位合法 traceId 被保留，下划线连字符允许")
    void boundaryLengthsAreAccepted() {
        for (String legit : new String[]{"abcdefgh", "a".repeat(64), "abc-def_12", "AbC_123-xY"}) {
            MDC.clear();
            var request = request("GET", "/api/post/list");
            request.addHeader("X-Trace-Id", legit);

            assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

            assertEquals(legit, MDC.get(LogUtils.TRACE_ID));
        }
    }

    @Test
    @DisplayName("MDC 中预置的非法 traceId（过滤器路径）在 initMdc 时被替换")
    void preexistingIllegalTraceIdInMdcIsReplaced() {
        // 模拟 JwtAuthenticationFilter 预置路径：未校验就把伪造值塞进 MDC
        MDC.put(LogUtils.TRACE_ID, "forged\r\ninjected line");

        LogUtils.initMdc();

        String traceId = MDC.get(LogUtils.TRACE_ID);
        assertTrue(traceId.matches("^[0-9a-f]{16}$"), "预置的非法 traceId 必须被替换，实际: " + traceId);
    }

    // ==================== B. CRLF / 控制字符清洗 ====================

    @Test
    @DisplayName("UA 含 CRLF 时被清洗成单行，日志行数不增加")
    void crlfUserAgentIsSanitized() {
        var request = request("GET", "/api/post/list");
        request.addHeader("User-Agent", "Mozilla/5.0\r\n2026-01-01 00:00:00.000 [evil] INFO injected - fake");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        List<String> messages = messages();
        assertEquals(1, messages.size(), "一条请求只能产生一条 REQUEST 日志，实际: " + messages);
        String line = messages.get(0);
        assertTrue(line.startsWith("[REQUEST] GET /api/post/list"), "前缀与字段顺序必须保持，实际: " + line);
        assertFalse(line.contains("\r"), "日志不得含 CR，实际: " + line);
        assertFalse(line.contains("\n"), "日志不得含 LF，实际: " + line);
        assertTrue(line.contains("Mozilla/5.0__2026-01-01"), "CRLF 应被替换为占位符，实际: " + line);
    }

    @Test
    @DisplayName("查询串含 CRLF 时被清洗成单行")
    void crlfQueryStringIsSanitized() {
        var request = new MockHttpServletRequest("GET", "/api/post/list");
        request.setRemoteAddr("203.0.113.10");
        request.setQueryString("tag=java\r\ninjected");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String line = messages().get(0);
        assertFalse(line.contains("\r"));
        assertFalse(line.contains("\n"));
        assertEquals(1, messages().size());
    }

    @Test
    @DisplayName("异常消息含 CRLF 时，RESPONSE error 日志仍是单行")
    void crlfExceptionMessageIsSanitized() {
        var request = request("GET", "/api/post/list");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(),
                new IllegalStateException("boom\r\n2026-01-01 00:00:00.000 [evil] INFO injected - fake"));

        List<String> messages = messages();
        String responseLine = messages.stream()
                .filter(m -> m.startsWith("[RESPONSE]"))
                .findFirst()
                .orElseThrow();
        assertFalse(responseLine.contains("\r"));
        assertFalse(responseLine.contains("\n"));
        assertTrue(responseLine.contains("error=boom__2026-01-01"), "实际: " + responseLine);
    }

    @Test
    @DisplayName("NUL、TAB、DEL 等控制字符被清洗，中文不被破坏")
    void controlCharactersAreSanitizedAndChinesePreserved() {
        assertEquals("a_b_c_", LogUtils.sanitizeLogValue("a\u0000b\tc\u007F", 0));
        assertEquals("教程-标签", LogUtils.sanitizeLogValue("教程-标签", 0));
    }

    @Test
    @DisplayName("超长字段被截断，且截断后仍不含 CRLF")
    void overlongValuesAreTruncated() {
        var request = request("GET", "/api/post/list");
        request.setQueryString("q=" + "x".repeat(2000));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String line = messages().get(0);
        assertTrue(line.length() < 900, "查询串必须被截断，实际长度: " + line.length());
    }

    // ==================== C. 敏感查询参数掩码 ====================

    @Test
    @DisplayName("token 参数值被掩码，参数名保留")
    void sensitiveTokenIsMasked() {
        var request = new MockHttpServletRequest("GET", "/api/gallery/photos/1/image");
        request.setRemoteAddr("203.0.113.10");
        request.setQueryString("token=super-secret-value");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String line = messages().get(0);
        assertTrue(line.contains("token=***"), "实际: " + line);
        assertFalse(line.contains("super-secret-value"), "敏感值不得入盘，实际: " + line);
    }

    @Test
    @DisplayName("大小写不敏感：TOKEN / Access_Token / Password 均被掩码")
    void maskingIsCaseInsensitive() {
        var request = new MockHttpServletRequest("GET", "/api/x");
        request.setRemoteAddr("203.0.113.10");
        request.setQueryString("TOKEN=abc&Access_Token=def&PASSWORD=ghi&secret=jkl&code=mno&key=pqr");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String line = messages().get(0);
        for (String leaked : new String[]{"abc", "def", "ghi", "jkl", "mno", "pqr"}) {
            assertFalse(line.contains("=" + leaked), "敏感值 " + leaked + " 不得入盘，实际: " + line);
        }
        assertTrue(line.contains("TOKEN=***"));
        assertTrue(line.contains("PASSWORD=***"));
    }

    @Test
    @DisplayName("普通查询串保持可读：tag/pageNum/pageSize 不被掩码")
    void nonSensitiveParamsStayReadable() {
        var request = new MockHttpServletRequest("GET", "/api/post/list");
        request.setRemoteAddr("203.0.113.10");
        request.setQueryString("pageNum=1&pageSize=20&tag=%E6%95%99%E7%A8%8B");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String line = messages().get(0);
        assertTrue(line.contains("pageNum=1"));
        assertTrue(line.contains("pageSize=20"));
        assertTrue(line.contains("tag=%E6%95%99%E7%A8%8B"), "中文标签查询串必须保持可读，实际: " + line);
    }

    @Test
    @DisplayName("混合查询串：只掩码敏感参数，其余原样")
    void mixedQueryStringMasksOnlySensitiveValues() {
        var request = new MockHttpServletRequest("GET", "/api/x");
        request.setRemoteAddr("203.0.113.10");
        request.setQueryString("tag=java&token=leak-me&pageNum=2");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        String line = messages().get(0);
        assertTrue(line.contains("tag=java"));
        assertTrue(line.contains("pageNum=2"));
        assertTrue(line.contains("token=***"));
        assertFalse(line.contains("leak-me"));
    }

    @Test
    @DisplayName("maskSensitiveQuery 直接调用：null/空串/无等号/数组式参数")
    void maskSensitiveQueryEdgeCases() {
        assertNull(LogUtils.maskSensitiveQuery(null));
        assertEquals("", LogUtils.maskSensitiveQuery(""));
        assertEquals("flag", LogUtils.maskSensitiveQuery("flag"));
        assertEquals("token=***", LogUtils.maskSensitiveQuery("token="));
        // 参数名原样保留，只掩码值
        assertEquals("token[]=***", LogUtils.maskSensitiveQuery("token[]=abc"));
        assertEquals("token[0]=***&tag=a", LogUtils.maskSensitiveQuery("token[0]=abc&tag=a"));
    }

    // ==================== D. 既有日志格式保持不变 ====================

    @Test
    @DisplayName("RESPONSE 日志前缀与字段顺序不变（含 400+ 走 warn 分支）")
    void responseLogFormatIsUnchanged() {
        var request = request("GET", "/api/not-found");
        var response = new MockHttpServletResponse();
        response.setStatus(404);
        assertTrue(interceptor.preHandle(request, response, new Object()));

        interceptor.afterCompletion(request, response, new Object(), null);

        String responseLine = messages().stream()
                .filter(m -> m.startsWith("[RESPONSE]"))
                .findFirst()
                .orElseThrow();
        assertTrue(responseLine.matches(
                        "^\\[RESPONSE] GET /api/not-found status=404 duration=\\d+ms clientIp=203\\.0\\.113\\.10$"),
                "既有格式必须保持，实际: " + responseLine);
    }

    @Test
    @DisplayName("REQUEST 日志格式不变：METHOD URI ?query from IP=... UA=...")
    void requestLogFormatIsUnchanged() {
        var request = new MockHttpServletRequest("GET", "/api/post/list");
        request.setRemoteAddr("203.0.113.10");
        request.setQueryString("pageNum=1");
        request.addHeader("User-Agent", "curl/8.0");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        assertEquals("[REQUEST] GET /api/post/list ?pageNum=1 from IP=203.0.113.10 UA=curl/8.0",
                messages().get(0));
    }

    @Test
    @DisplayName("无查询串与无 UA 时保持既有空串/null 语义")
    void absentQueryAndUserAgentKeepExistingSemantics() {
        var request = request("GET", "/api/post/list");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        assertEquals("[REQUEST] GET /api/post/list  from IP=203.0.113.10 UA=null", messages().get(0));
    }
}
