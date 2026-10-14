package com.selfblog.controller.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.dto.social.ReportRequest;
import com.selfblog.service.social.ReportService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.social.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 举报控制器
 */
@Tag(name = "举报管理", description = "举报相关接口")
@RestController
@RequestMapping("/report")
@Validated
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 举报内容（需登录）
     */
    @RequiresAuth
    @Operation(summary = "举报内容")
    @PostMapping
    public Result<Long> createReport(@Valid @RequestBody ReportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long reportId = reportService.createReport(request, userId);
        return Result.success(reportId);
    }

    /**
     * 获取当前用户的举报记录（需登录）
     */
    @RequiresAuth
    @Operation(summary = "获取我的举报记录")
    @GetMapping("/my")
    public Result<IPage<ReportVO>> getMyReports(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<ReportVO> reports = reportService.getMyReports(page, pageSize, userId);
        return Result.success(reports);
    }
}