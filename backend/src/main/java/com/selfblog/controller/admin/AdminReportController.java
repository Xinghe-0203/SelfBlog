package com.selfblog.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.result.Result;
import com.selfblog.dto.social.HandleReportRequest;
import com.selfblog.service.social.ReportService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.social.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员举报管理控制器
 */
@Tag(name = "管理员-举报管理", description = "管理员举报管理接口")
@RestController
@RequestMapping("/admin/reports")
@Validated
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * 获取举报列表（仅管理员），支持按状态筛选
     */
    @Operation(summary = "获取举报列表")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<ReportVO>> getPendingReports(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
            @RequestParam(required = false) Integer status) {
        IPage<ReportVO> reports = reportService.getReportsByStatus(page, pageSize, status);
        return Result.success(reports);
    }

    /**
     * 获取举报详情（仅管理员）
     */
    @Operation(summary = "获取举报详情")
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('admin')")
    public Result<ReportVO> getReportDetail(@PathVariable Long reportId) {
        ReportVO report = reportService.getReportDetail(reportId);
        return Result.success(report);
    }

    /**
     * 处理举报（仅管理员）
     */
    @Operation(summary = "处理举报")
    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> handleReport(
            @PathVariable Long reportId,
            @Valid @RequestBody HandleReportRequest request) {
        Long handlerId = SecurityUtils.getCurrentUserId();
        reportService.handleReport(reportId, request, handlerId);
        return Result.success(null);
    }
}