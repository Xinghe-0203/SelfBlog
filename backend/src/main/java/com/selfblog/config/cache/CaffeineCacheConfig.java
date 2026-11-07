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
    public static final String PAGE_CACHE = "pageCache";
    public static final String FEED_CACHE = "feedCache";

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
                new CaffeineCache(NOTIFICATION_LIST_CACHE, notificationListCache().build()),
                new CaffeineCache(PAGE_CACHE, pageCache().build()),
                new CaffeineCache(FEED_CACHE, feedCache().build())
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

    /**
     * 页面缓存：PageServiceImpl 的公开读（published:页面id / slug:地址 / manifest / nav）。
     * 每次公开页渲染原先要在 site_setting KV 表上多次 selectById/getBySlug；页面写路径
     * （建页/存稿/发布/下线/删除/导航）全部 allEntries 失效，TTL 5 分钟兜底
     * adminPath 等外部输入的变化。键数量 = 内置页面 + 内容页 + slug + 2，200 足够。
     */
    public Caffeine<Object, Object> pageCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 订阅源缓存：/feed.xml 与 /atom.xml 的整段渲染结果，键 = 解析后的站点源 + ":rss"/":atom"。
     *
     * <p>全文订阅源要把最近 100 篇文章的 Markdown 全部渲染一遍（阅读页同一套 SafeMarkdown），
     * 单次重建是百次详情查询加百次渲染，绝不能每请求都做。键必须带站点源：订阅正文内嵌
     * 绝对链接，SiteOrigin 在未显式配置时会回落到请求来源，同一实例可能按来源产出不同正文。
     * 文章写路径（create/update/delete/adminDelete/approve/reject）全部 allEntries 失效；
     * TTL 5 分钟兜底站点标题等设置变化（siteBaseUrl 变化因入键会自然产出新键）。
     * 键只有两种后缀 × 少量来源变体，20 足够。
     */
    public Caffeine<Object, Object> feedCache() {
        return Caffeine.newBuilder()
                .maximumSize(20)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }
}
