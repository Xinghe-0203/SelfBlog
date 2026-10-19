package com.selfblog.config.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 限流阈值表的缓存与覆盖测试。
 *
 * 回归背景：{@code getAllLimits()} 原先在**每个请求**的热路径上 new HashMap + 33 次 put；
 * 现在返回一次性构建的不可变快照，本测试锁定该缓存行为与新增读路径阈值。
 */
class RateLimitPropertiesTest {

    @Test
    @DisplayName("getAllLimits 重复调用返回同一个缓存实例，不再逐请求重建")
    void getAllLimitsIsCached() {
        RateLimitProperties properties = new RateLimitProperties();

        Map<String, Integer> first = properties.getAllLimits();
        Map<String, Integer> second = properties.getAllLimits();

        assertSame(first, second, "热路径必须复用同一个快照实例");
    }

    @Test
    @DisplayName("refreshLimits 在配置绑定/改值后重建快照")
    void refreshLimitsRebuildsSnapshot() {
        RateLimitProperties properties = new RateLimitProperties();
        Map<String, Integer> before = properties.getAllLimits();

        properties.setPostGeneral(11);
        properties.refreshLimits();

        Map<String, Integer> after = properties.getAllLimits();
        assertNotSame(before, after);
        assertEquals(11, after.get("/api/post/"));
    }

    @Test
    @DisplayName("返回的快照不可变，调用方无法篡改限流阈值")
    void limitsSnapshotIsImmutable() {
        Map<String, Integer> limits = new RateLimitProperties().getAllLimits();

        assertThrows(UnsupportedOperationException.class, () -> limits.put("/api/post/list", 100000));
    }

    @Test
    @DisplayName("新增 /api/post/list 读路径阈值为 60/min，且比写路径 /api/post/ 宽松")
    void postListReadPathThresholdIsRegistered() {
        Map<String, Integer> limits = new RateLimitProperties().getAllLimits();

        assertEquals(60, limits.get("/api/post/list"));
        assertEquals(10, limits.get("/api/post/"));
        assertTrue(limits.get("/api/post/list") > limits.get("/api/post/"),
                "读路径必须比写路径宽松，否则首页/归档页正常浏览会被 429 误伤");
    }

    @Test
    @DisplayName("/api/post/list 是 /api/post/ 的更长前缀，最长前缀匹配时优先命中")
    void postListIsTheLongerPrefix() {
        Map<String, Integer> limits = new RateLimitProperties().getAllLimits();

        assertTrue(limits.containsKey("/api/post/list"));
        assertTrue("/api/post/list".startsWith("/api/post/"),
                "最长前缀匹配依赖该包含关系；拦截器按 base 长度取最长者");
    }

    @Test
    @DisplayName("既有阈值未被改动（抽样核对）")
    void existingThresholdsAreUnchanged() {
        Map<String, Integer> limits = new RateLimitProperties().getAllLimits();

        assertEquals(10, limits.get("/api/user/login"));
        assertEquals(5, limits.get("/api/user/register"));
        assertEquals(3, limits.get("/api/user/send-code"));
        assertEquals(20, limits.get("/api/user/search"));
        assertEquals(30, limits.get("/api/user/"));
        assertEquals(60, limits.get("/api/visitor/"));
        assertEquals(5, limits.get("/api/guest-comments/"));
        assertEquals(120, limits.get("/api/site/pages"));
    }
}
