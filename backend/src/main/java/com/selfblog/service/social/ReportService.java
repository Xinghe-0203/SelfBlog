package com.selfblog.service.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.dto.social.HandleReportRequest;
import com.selfblog.dto.social.ReportRequest;
import com.selfblog.entity.BlogReport;
import com.selfblog.vo.social.ReportVO;

/**
 * 举报服务接口
 */
public interface ReportService extends IService<BlogReport> {

    /**
     * 创建举报
     * @param request 举报请求
     * @param reporterId 举报人ID
     * @return 举报ID
     */
    Long createReport(ReportRequest request, Long reporterId);

    /**
     * 获取当前用户的举报记录（分页）
     * @param page 页码
     * @param pageSize 每页大小
     * @param reporterId 举报人ID
     * @return 分页举报列表
     */
    IPage<ReportVO> getMyReports(Integer page, Integer pageSize, Long reporterId);

    /**
     * 获取待处理的举报列表（管理员）
     * @param page 页码
     * @param pageSize 每页大小
     * @return 待处理举报分页列表
     */
    IPage<ReportVO> getPendingReports(Integer page, Integer pageSize);

    /**
     * 按状态获取举报列表（管理员）
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 状态（0待处理/1已驳回/2已核实），null表示全部
     * @return 举报分页列表
     */
    IPage<ReportVO> getReportsByStatus(Integer page, Integer pageSize, Integer status);

    /**
     * 获取举报详情（管理员）
     * @param reportId 举报ID
     * @return 举报详情
     */
    ReportVO getReportDetail(Long reportId);

    /**
     * 处理举报（管理员）
     * @param reportId 举报ID
     * @param request 处理请求
     * @param handlerId 处理人ID
     */
    void handleReport(Long reportId, HandleReportRequest request, Long handlerId);
}