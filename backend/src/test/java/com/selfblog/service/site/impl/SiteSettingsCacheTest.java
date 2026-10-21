package com.selfblog.service.site.impl;

import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.mapper.SiteSettingMapper;
import com.selfblog.service.site.SiteSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * `getSettings()` 上的 `@Cacheable` 必须对**类内部**的读取也生效。
 *
 * <p>回归背景：`getPublicSettings()` / `isCommentsEnabled()` / `isVisitorTrackingEnabled()`
 * 原先直接写 `getSettings()`，属于自调用（self-invocation），不经过 Spring 的缓存代理，
 * 于是公开热路径每次都真的查库；其中 `isVisitorTrackingEnabled()` 会在**每一次访客埋点**上执行。
 * 修复后内部读取一律经注入的自身代理 {@code self.getSettings()}。</p>
 *
 * <p>断言用 `@SpyBean SiteSettingMapper` 数真实调用次数，而不是看返回值——返回值无论走不走
 * 缓存都一样，只有“查库次数”能区分代理是否生效。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SiteSettings 自调用缓存回归")
class SiteSettingsCacheTest {

    @Autowired
    private SiteSettingsService siteSettingsService;

    @Autowired
    private CacheManager cacheManager;

    @SpyBean
    private SiteSettingMapper settingMapper;

    /**
     * 缓存不参与事务回滚，且容器启动期可能已填充过，所以每次用例前清缓存再清调用记录，
     * 让“查库次数”的基线确定为零。
     */
    @BeforeEach
    void resetCacheAndInvocations() {
        Cache cache = cacheManager.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
        clearInvocations(settingMapper);
    }

    @Test
    @DisplayName("连续两次 getPublicSettings 只查库一次")
    void getPublicSettingsReadsTheDatabaseOnlyOnce() {
        Map<String, String> first = siteSettingsService.getPublicSettings();
        Map<String, String> second = siteSettingsService.getPublicSettings();

        assertNotNull(first.get("siteTitle"));
        assertEquals(first.get("siteTitle"), second.get("siteTitle"),
                "第二次调用必须返回与第一次一致的快照");
        verify(settingMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("缓存已填充时 isVisitorTrackingEnabled 不再查库")
    void isVisitorTrackingEnabledReusesTheCachedSnapshot() {
        // 先经公开 API 填充缓存，再清掉调用记录，隔离出内部读取路径本身的行为。
        siteSettingsService.getSettings();
        clearInvocations(settingMapper);

        // 埋点热路径上的两次判断都不应再触发查库。
        siteSettingsService.isVisitorTrackingEnabled();
        siteSettingsService.isVisitorTrackingEnabled();

        verify(settingMapper, times(0)).selectList(null);
    }
}
