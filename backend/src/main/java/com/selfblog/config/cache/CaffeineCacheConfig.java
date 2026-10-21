package com.selfblog.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置 v2.0 - 性能优化版
 *
 * 缓存策略：
 * - hotTagsCache: 热门标签缓存，10分钟过期，最大1000条
 * - categoryCache: 分类缓存，30分钟过期，最大500条
 * - userCache: 用户缓存，10分钟过期，最大5000条
 * - trendingCache: 趋势缓存，5分钟过期，最大1000条
 * - statsCache: 统计缓存，15分钟过期，最大200条
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    public static final String HOT_TAGS_CACHE = "hotTagsCache";
    public static final String CATEGORY_CACHE = "categoryCache";
    public static final String USER_CACHE = "userCache";
    public static final String TRENDING_CACHE = "trendingCache";
    public static final String STATS_CACHE = "statsCache";
    public static final String STATUS_CACHE = "statusCache";
    public static final String SITE_SETTINGS_CACHE = "siteSettingsCache";
    public static final String VISITOR_SUMMARY_CACHE = "visitorSummaryCache";
    public static final String NOTIFICATION_LIST_CACHE = "notificationListCache";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new CaffeineCache(HOT_TAGS_CACHE, hotTagsCache().build()),
                new CaffeineCache(CATEGORY_CACHE, categoryCache().build()),
                new CaffeineCache(USER_CACHE, userCache().build()),
                new CaffeineCache(TRENDING_CACHE, trendingCache().build()),
                new CaffeineCache(STATS_CACHE, statsCache().build()),
                new CaffeineCache(STATUS_CACHE, statusCache().build()),
                new CaffeineCache(SITE_SETTINGS_CACHE, siteSettingsCache().build()),
                new CaffeineCache(VISITOR_SUMMARY_CACHE, visitorSummaryCache().build()),
                new CaffeineCache(NOTIFICATION_LIST_CACHE, notificationListCache().build())
        ));
        return cacheManager;
    }

    public Caffeine<Object, Object> hotTagsCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }

    public Caffeine<Object, Object> categoryCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }

    public Caffeine<Object, Object> userCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }

    public Caffeine<Object, Object> trendingCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }

    public Caffeine<Object, Object> statsCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats();
    }

    public Caffeine<Object, Object> statusCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .recordStats();
    }

    public Caffeine<Object, Object> siteSettingsCache() {
        return Caffeine.newBuilder().maximumSize(20).expireAfterWrite(10, TimeUnit.MINUTES);
    }

    public Caffeine<Object, Object> visitorSummaryCache() {
        return Caffeine.newBuilder().maximumSize(10).expireAfterWrite(30, TimeUnit.SECONDS);
    }

    /**
     * 通知列表缓存：按用户/分页维度缓存，发送通知与标记已读时整体失效。
     * 与 userCache 分开，避免为了失效列表而清掉用户数据行缓存。
     */
    public Caffeine<Object, Object> notificationListCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .recordStats();
    }
}
