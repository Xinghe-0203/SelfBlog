package com.selfblog.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：{@code [PERFORMANCE]} 事件必须走专用 logger {@code com.selfblog.performance}，
 * 而<b>不再</b>跟着 {@link LogUtils} 自己的 logger 落进 audit.log。
 *
 * <p>背景：task-7 曾在 logback-spring.xml 的 dev/prod/default 三段配置了
 * {@code <logger name="com.selfblog.performance">}，但当时的 Java 侧没有任何代码使用该名字，
 * 所以那段配置是死配置，性能事件仍然经由 {@code LoggerFactory.getLogger(LogUtils.class)} 输出，
 * 被 ASYNC_AUDIT 收走 —— 即「注释声称已修、代码实际未生效」。
 *
 * <p>本测试同时挂两个 {@link ListAppender}：
 * <ol>
 *   <li>挂到 {@code com.selfblog.performance}：断言事件确实出现在这里；</li>
 *   <li>挂到 {@code com.selfblog.utils.LogUtils}：断言它<b>收不到</b> {@code [PERFORMANCE]}
 *       —— 这是核心断言，把 {@code perfLog} 改回 {@code log} 时它必然变红。</li>
 * </ol>
 *
 * <p>耗时档位不依赖真实等待：直接给 {@code startTime} 传一个过去的毫秒时间戳，
 * 精确落在 WARN(&gt;3000ms) / INFO(&gt;1000ms) / DEBUG(其余) 三档上。
 *
 * <p>注意：普通 JUnit 测试不经过 Spring Boot 的日志初始化，logback 走默认配置
 * （root=DEBUG），因此本测试<b>显式设置</b> perf logger 的 level 来模拟
 * logback-spring.xml 里配置的 INFO 阈值。
 */
class LogUtilsPerformanceLoggerTest {

    /** 专用 logger 名，必须与 logback-spring.xml 中的 &lt;logger name="..."/&gt; 完全一致。 */
    private static final String PERFORMANCE_LOGGER_NAME = "com.selfblog.performance";

    private static final Logger perfLogger =
            (Logger) LoggerFactory.getLogger(PERFORMANCE_LOGGER_NAME);
    private static final Logger logUtilsLogger =
            (Logger) LoggerFactory.getLogger(LogUtils.class);

    private ListAppender<ILoggingEvent> perfAppender;
    private ListAppender<ILoggingEvent> logUtilsAppender;
    private Level originalPerfLevel;

    /** 挂上两个 ListAppender，并记录/设置 perf logger 的 level。 */
    private void installAppenders(Level perfLevel) {
        originalPerfLevel = perfLogger.getLevel();
        perfLogger.setLevel(perfLevel);

        perfAppender = new ListAppender<>();
        perfAppender.start();
        perfLogger.addAppender(perfAppender);

        logUtilsAppender = new ListAppender<>();
        logUtilsAppender.start();
        logUtilsLogger.addAppender(logUtilsAppender);
    }

    @AfterEach
    void detachAppenders() {
        if (perfAppender != null) {
            perfLogger.detachAppender(perfAppender);
        }
        if (logUtilsAppender != null) {
            logUtilsLogger.detachAppender(logUtilsAppender);
        }
        perfLogger.setLevel(originalPerfLevel);
    }

    /** 收集 appender 里带 [PERFORMANCE] 标记的事件。 */
    private static List<ILoggingEvent> performanceEvents(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("[PERFORMANCE]"))
                .toList();
    }

    // ==================== 核心：事件改走专用 logger，且不再进 LogUtils 的 logger ====================

    @Test
    @DisplayName("INFO 档(>1000ms)：事件出现在 com.selfblog.performance，且 LogUtils logger 收不到")
    void infoTierGoesToPerformanceLoggerOnly() {
        installAppenders(Level.INFO);

        LogUtils.logPerformance("PostQueryService.list", System.currentTimeMillis() - 1500);

        List<ILoggingEvent> perfEvents = performanceEvents(perfAppender);
        assertEquals(1, perfEvents.size(), "INFO 档性能事件必须落在 com.selfblog.performance 上");
        assertEquals(Level.INFO, perfEvents.get(0).getLevel());
        assertTrue(perfEvents.get(0).getFormattedMessage().contains("PostQueryService.list"),
                "事件应保留被清洗后的 operation 文本");
        assertTrue(perfEvents.get(0).getFormattedMessage().contains("duration="),
                "事件应保留 duration= 格式串");

        assertTrue(performanceEvents(logUtilsAppender).isEmpty(),
                "LogUtils 自己的 logger 不得再收到 [PERFORMANCE]（否则仍会混入 audit.log）");
        assertTrue(logUtilsAppender.list.isEmpty(),
                "logPerformance 不应再向 LogUtils logger 写任何事件");
    }

    @Test
    @DisplayName("WARN 档(>3000ms)：带 SLOW 前缀的事件同样只走专用 logger")
    void warnTierGoesToPerformanceLoggerOnly() {
        installAppenders(Level.INFO);

        LogUtils.logPerformance("AlbumUpload.batch", System.currentTimeMillis() - 3500);

        List<ILoggingEvent> perfEvents = performanceEvents(perfAppender);
        assertEquals(1, perfEvents.size(), "WARN 档性能事件必须落在 com.selfblog.performance 上");
        assertEquals(Level.WARN, perfEvents.get(0).getLevel());
        assertTrue(perfEvents.get(0).getFormattedMessage().contains("SLOW"),
                "WARN 档应保留 SLOW 前缀");
        assertTrue(performanceEvents(logUtilsAppender).isEmpty(),
                "LogUtils 自己的 logger 不得再收到 [PERFORMANCE]");
    }

    // ==================== DEBUG 档：受 INFO 阈值压制，不落盘 ====================

    @Test
    @DisplayName("DEBUG 档(<1000ms)在 INFO 阈值下不产出事件（performance.log 噪声回归防护）")
    void debugTierIsSuppressedAtInfoLevel() {
        installAppenders(Level.INFO);

        LogUtils.logPerformance("FastQuery.byId", System.currentTimeMillis() - 50);

        assertTrue(performanceEvents(perfAppender).isEmpty(),
                "INFO 阈值下 <1000ms 的 DEBUG 档不得产出事件（否则 performance.log 会被正常请求淹没）");
        assertTrue(logUtilsAppender.list.isEmpty(),
                "被阈值压制的 DEBUG 档同样不应漏到 LogUtils logger");
    }

    @Test
    @DisplayName("DEBUG 档确实调用了专用 logger：level 放开到 DEBUG 时事件立即出现")
    void debugTierReachesPerformanceLoggerWhenLevelAllows() {
        installAppenders(Level.DEBUG);

        LogUtils.logPerformance("FastQuery.byId", System.currentTimeMillis() - 50);

        List<ILoggingEvent> perfEvents = performanceEvents(perfAppender);
        assertEquals(1, perfEvents.size(),
                "DEBUG 档必须指向专用 logger（证明压制来自 level/ThresholdFilter，而不是调用点缺失）");
        assertEquals(Level.DEBUG, perfEvents.get(0).getLevel());
        assertTrue(performanceEvents(logUtilsAppender).isEmpty(),
                "即使 level=DEBUG，LogUtils logger 也不应收到 [PERFORMANCE]");
    }

    // ==================== 防止 logger 名漂移（配置不再变回死配置） ====================

    @Test
    @DisplayName("LogUtils 的专用 logger 字段名与 logback 配置中的 logger 名一致")
    void perfLoggerFieldIsBoundToTheConfiguredName() throws Exception {
        Field field = LogUtils.class.getDeclaredField("perfLog");
        assertTrue(Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
                "perfLog 应为 static final");
        // 字段声明类型是 org.slf4j.Logger（接口）；运行时实例由 logback 提供
        assertTrue(org.slf4j.Logger.class.isAssignableFrom(field.getType()),
                "perfLog 应为 org.slf4j.Logger 类型");
        field.setAccessible(true);
        Object value = field.get(null);
        assertNotNull(value);
        assertTrue(value instanceof Logger,
                "运行时 perfLog 必须是 logback 的 Logger，否则 logback 无法把 appender 绑到该名字上");
        assertEquals(PERFORMANCE_LOGGER_NAME, ((Logger) value).getName(),
                "perfLog 的 logger 名必须与 logback-spring.xml 的 <logger name=\"com.selfblog.performance\"> 一致，"
                        + "否则配置会再次变成死配置");

        // 反向对照：LogUtils 自己的 logger 名不同，两者必须是两条独立通道
        assertFalse(PERFORMANCE_LOGGER_NAME.equals(logUtilsLogger.getName()),
                "perfLog 与 log 必须是不同 logger，否则事件仍会混入 audit.log");
    }
}
