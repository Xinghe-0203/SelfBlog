package com.selfblog.controller.admin;

import com.selfblog.common.result.Result;
import com.selfblog.service.content.StatisticsService;
import com.selfblog.vo.content.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员数据统计控制器
 */
@Tag(name = "管理员-数据统计", description = "平台数据统计接口")
@RestController
@RequestMapping("/admin/statistics")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取平台完整统计数据
     */
    @Operation(summary = "获取平台统计数据")
    @GetMapping
    public Result<StatisticsVO> getPlatformStatistics() {
        StatisticsVO statistics = statisticsService.getPlatformStatistics();
        return Result.success(statistics);
    }
}
