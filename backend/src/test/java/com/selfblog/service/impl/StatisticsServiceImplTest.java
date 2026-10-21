package com.selfblog.service.impl;

import com.selfblog.service.content.StatisticsService;
import com.selfblog.vo.content.StatisticsVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatisticsServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatisticsServiceImplTest {

    @Autowired
    private StatisticsService statisticsService;

    @Test
    @DisplayName("获取社区统计数据")
    void getCommunityStats_Success() {
        StatisticsVO stats = statisticsService.getCommunityStats();
        assertNotNull(stats);
    }

    /**
     * 增长趋势回归：H2 会把未加引号的列别名折叠成大写（DATE/COUNT），
     * 于是 {@code row.get("date")} 返回 null 并 NPE，后台统计接口在 H2 测试档 500。
     * 这里直接跑 {@code getPlatformStatistics()}，覆盖此前无人执行的趋势分支。
     */
    @Test
    @DisplayName("平台统计数据包含 30 天用户/文章增长趋势")
    void getPlatformStatistics_GrowthTrendsArePopulated() {
        StatisticsVO stats = statisticsService.getPlatformStatistics();
        assertNotNull(stats);

        assertNotNull(stats.getUserGrowthTrend(), "用户增长趋势不能为 null");
        assertEquals(30, stats.getUserGrowthTrend().size(), "用户增长趋势应为最近 30 天");
        assertNotNull(stats.getPostGrowthTrend(), "文章增长趋势不能为 null");
        assertEquals(30, stats.getPostGrowthTrend().size(), "文章增长趋势应为最近 30 天");

        for (StatisticsVO.DailyCount daily : stats.getUserGrowthTrend()) {
            assertNotNull(daily.getDate(), "趋势项的日期不能为 null");
            assertNotNull(daily.getCount(), "趋势项的计数不能为 null");
        }
    }
}
