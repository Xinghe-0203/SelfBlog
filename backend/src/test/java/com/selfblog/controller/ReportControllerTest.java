package com.selfblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.controller.social.ReportController;
import com.selfblog.service.social.ReportService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.social.ReportVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReportController 单元测试
 */
@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Long userId) {
        UserContext ctx = new UserContext(userId, "user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList()));
    }

    @Test
    @DisplayName("创建举报成功")
    void createReport_Success() throws Exception {
        setUpSecurityContext(1L);
        when(reportService.createReport(any(), eq(1L))).thenReturn(100L);

        mockMvc.perform(post("/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"post\",\"targetId\":10,\"reason\":\"违规内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("创建举报 - 未登录返回401")
    void createReport_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(post("/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"post\",\"targetId\":10,\"reason\":\"违规\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("获取我的举报列表")
    void getMyReports_Success() throws Exception {
        setUpSecurityContext(1L);
        IPage<ReportVO> page = new Page<>(1, 10, 0);
        when(reportService.getMyReports(anyInt(), anyInt(), eq(1L))).thenReturn(page);

        mockMvc.perform(get("/report/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
