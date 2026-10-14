package com.selfblog.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.result.Result;
import com.selfblog.service.site.VisitorService;
import com.selfblog.vo.site.VisitorRecordVO;
import com.selfblog.vo.site.VisitorSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/visitors")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminVisitorController {
    private final VisitorService visitorService;

    @GetMapping("/summary")
    public Result<VisitorSummaryVO> summary() {
        return Result.success(visitorService.getSummary());
    }

    @GetMapping
    public Result<IPage<VisitorRecordVO>> records(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(visitorService.getRecords(pageNum, pageSize));
    }
}
