package com.selfblog.service.site.impl;

import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.VisitorEventMapper;
import com.selfblog.service.site.VisitorService;
import com.selfblog.vo.site.VisitorSummaryVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 访客看板的 SQL 下推回归：热门页面（GROUP BY + LIMIT）、文章总浏览量（SUM）、
 * 14 天趋势（一条按天 GROUP BY）三个聚合原先分别用「LIMIT 20000 明细 + 内存 HashMap」、
 * 「全量已发布文章行内存求和」和「14 天 × 2 条 COUNT」实现。
 * 本测试用真实 H2 行数证明新 SQL 的取值与旧计算逐一相同，包括两个窗口语义：
 * 每日趋势是 [today-13 0 点, tomorrow 0 点) 的逐日分桶，热门页面是 30 天下界、无上界。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("访客看板聚合 SQL")
class VisitorDashboardSummaryTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired VisitorService visitorService;
    @Autowired VisitorEventMapper visitorEventMapper;
    @Autowired BlogPostMapper blogPostMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired CacheManager caches;

    private LocalDate today;

    /**
     * getSummary 上的 @Cacheable 在测试 profile 下同样是生效的（自定义 CacheManager
     * 覆盖了 spring.cache.type=none），且缓存不参与事务回滚——与 PageServiceImplTest
     * 的 siteSettingsCache 同一约定：测试前后手动清缓存。
     */
    private void clearDashboardCache() {
        var cache = caches.getCache(CaffeineCacheConfig.VISITOR_SUMMARY_CACHE);
        if (cache != null) cache.clear();
    }

    @BeforeEach
    void seed() {
        clearDashboardCache();
        today = LocalDate.now();
        jdbc.update("DELETE FROM visitor_event");
        jdbc.update("DELETE FROM blog_post");

        // 每日趋势窗口 = [today-13 0点, tomorrow 0点)；today-14 23:59 与 25 天前都落在
        // 趋势窗外，但仍在热门页面的 30 天（下界）窗口里——两个窗口各自保持原语义。
        LocalDateTime todayMorning = today.atTime(9, 0);
        LocalDateTime yesterday = today.minusDays(1).atTime(12, 0);
        LocalDateTime edgeIncluded = today.minusDays(13).atTime(8, 0);
        LocalDateTime edgeExcluded = today.minusDays(14).atTime(23, 59);
        LocalDateTime recent = today.minusDays(25).atTime(10, 0);

        Object[][] events = {
                {"v-visitor-1", "/a", todayMorning},   // 今日 /a 第 1 次
                {"v-visitor-1", "/b", todayMorning},   // 今日 /b（同访客第二路径）
                {"v-visitor-2", "/a", todayMorning},   // 今日 /a 第 2 次
                {"v-visitor-3", "/b", yesterday},
                {"v-visitor-4", "/c", edgeIncluded},
                {"v-visitor-5", "/b", edgeExcluded},   // 14 天窗外、30 天窗内
                {"v-visitor-6", "/d", recent}          // 25 天前：热门页面（30 天窗）仍统计
        };
        for (Object[] event : events) {
            // 必须用 LocalDateTime 绑定，不能用 Timestamp：生产写入走 MyBatis-Plus 的
            // LocalDateTime（字面量直发，无时区换算），而 setTimestamp 在 MySQL
            // serverTimezone != JVM 时区时会把值按连接时区平移存储——行整体偏移而
            // WHERE 边界（同样是 LocalDateTime 字面量）不偏移，边界行的分桶就会跨日。
            // 本地 H2 无换算所以假绿，只有 CI 的真实 MySQL 能暴露（2026-11-07 实录）。
            jdbc.update("INSERT INTO visitor_event(visitor_hash, ip_hash, user_agent, page_path, referrer, create_time) "
                    + "VALUES(?, ?, 'JUnit', ?, NULL, ?)", event[0], event[0], event[1], event[2]);
        }

        // 文章总浏览量只算 status=1 且未删除；草稿与已删除行带着大 view_count 作哨兵。
        Object[][] posts = {
                {910001L, 1, 0, 3L},
                {910002L, 1, 0, 4L},
                {910003L, 0, 0, 100L},  // 草稿：不计入
                {910004L, 1, 1, 100L}   // 已删除：不计入
        };
        for (Object[] post : posts) {
            jdbc.update("INSERT INTO blog_post(id, user_id, title, content, status, is_deleted, create_time, view_count) "
                    + "VALUES(?, 1, '看板文章', 'x', ?, ?, TIMESTAMP '2026-10-01 08:00:00', ?)",
                    post[0], post[1], post[2], post[3]);
        }
    }

    @AfterEach
    void cleanupCache() {
        clearDashboardCache();
    }

    @Test
    @DisplayName("热门页面：GROUP BY 计数与旧内存统计一致，30 天窗口无上界")
    void topPagesMatchTheOldInMemoryCounts() {
        VisitorSummaryVO summary = visitorService.getSummary();

        List<VisitorSummaryVO.TopPage> top = summary.getTopPages();
        assertEquals(4, top.size());
        assertEquals("/b", top.get(0).getPath());
        assertEquals(3, top.get(0).getViews());
        assertEquals("/a", top.get(1).getPath());
        assertEquals(2, top.get(1).getViews());
        // /c 与 /d 各 1 次：SQL 的并列顺序未定义（旧的 HashMap 迭代序同样未定义），
        // 只断言集合与计数。
        Map<String, Long> counts = top.stream()
                .collect(Collectors.toMap(VisitorSummaryVO.TopPage::getPath, VisitorSummaryVO.TopPage::getViews));
        assertEquals(1L, counts.get("/c"));
        assertEquals(1L, counts.get("/d"));
    }

    @Test
    @DisplayName("14 天趋势：逐日分桶与旧的逐日窗口一致，窗外日期补 0")
    void dailyBucketsMatchTheOldPerDayWindows() {
        VisitorSummaryVO summary = visitorService.getSummary();

        List<VisitorSummaryVO.Daily> daily = summary.getDaily();
        assertEquals(14, daily.size());
        for (int index = 0; index < daily.size(); index++) {
            assertEquals(today.minusDays(13 - index).format(ISO), daily.get(index).getDate(),
                    "日期轴必须是 offset 13..0 连续 14 天");
        }
        Map<String, VisitorSummaryVO.Daily> byDate = daily.stream()
                .collect(Collectors.toMap(VisitorSummaryVO.Daily::getDate, Function.identity()));
        assertEquals(2, byDate.get(today.format(ISO)).getVisitors(), "今日独立访客 = 2（v1 走了两条路径）");
        assertEquals(3, byDate.get(today.format(ISO)).getPageViews(), "今日浏览量 = 3");
        assertEquals(1, byDate.get(today.minusDays(1).format(ISO)).getPageViews());
        assertEquals(1, byDate.get(today.minusDays(13).format(ISO)).getPageViews(), "第 14 天（today-13）仍在窗口内");
        assertEquals(0, byDate.get(today.minusDays(12).format(ISO)).getPageViews(), "无事件的日期补 0");
    }

    @Test
    @DisplayName("文章总浏览量：SUM 只聚合 status=1 且未删除的行")
    void articleViewsSumOnlyPublishedUndeletedPosts() {
        assertEquals(7L, blogPostMapper.sumPublishedPostViews(),
                "3 + 4，草稿和已删除行的 100 必须被排除");
        VisitorSummaryVO summary = visitorService.getSummary();
        assertEquals(7L, summary.getArticleViews());
        assertEquals(7L, summary.getTotalPageViews(), "总浏览量 = 7 条事件， sanity check");
        assertTrue(summary.getTotalVisitors() >= 6);
    }
}
