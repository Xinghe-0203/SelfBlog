package com.selfblog.controller.content;

import com.selfblog.common.result.Result;
import com.selfblog.service.content.StatisticsService;
import com.selfblog.vo.content.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据统计控制器（公开）
 */
@Tag(name = "数据统计", description = "公开数据统计接口")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取社区统计数据（公开接口）
     */
    @Operation(summary = "获取社区统计数据")
    @GetMapping("/community")
    public Result<StatisticsVO> getCommunityStats() {
        StatisticsVO statistics = statisticsService.getCommunityStats();
        return Result.success(statistics);
    }
}
