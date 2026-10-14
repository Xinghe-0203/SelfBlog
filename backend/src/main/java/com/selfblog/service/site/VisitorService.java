package com.selfblog.service.site;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.dto.site.TrackVisitorRequest;
import com.selfblog.vo.site.VisitorRecordVO;
import com.selfblog.vo.site.VisitorSummaryVO;
import jakarta.servlet.http.HttpServletRequest;

public interface VisitorService {
    void track(TrackVisitorRequest request, HttpServletRequest httpRequest);
    VisitorSummaryVO getSummary();
    IPage<VisitorRecordVO> getRecords(Integer page, Integer pageSize);
}
