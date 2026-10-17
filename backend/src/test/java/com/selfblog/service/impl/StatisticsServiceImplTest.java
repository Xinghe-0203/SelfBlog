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
}
