package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.social.HandleReportRequest;
import com.selfblog.dto.social.ReportRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.BlogReport;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.BlogReportMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.ReportService;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.social.ReportVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReportServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceImplTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private BlogReportMapper blogReportMapper;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private SysUser reporter;
    private SysUser postAuthor;
    private SysUser admin;
    private BlogPost testPost;

    @BeforeEach
    void setUp() {
        blogReportMapper.delete(null);
        blogPostMapper.delete(null);
        sysUserMapper.delete(null);
        SecurityContextHolder.clearContext();

        reporter = new SysUser();
        reporter.setUsername("reporter");
        reporter.setPassword("password");
        reporter.setRole("user");
        reporter.setStatus(1);
        sysUserMapper.insert(reporter);

        postAuthor = new SysUser();
        postAuthor.setUsername("postauthor");
        postAuthor.setPassword("password");
        postAuthor.setRole("user");
        postAuthor.setStatus(1);
        sysUserMapper.insert(postAuthor);

        admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword("password");
        admin.setRole("admin");
        admin.setStatus(1);
        sysUserMapper.insert(admin);

        testPost = new BlogPost();
        testPost.setUserId(postAuthor.getId());
        testPost.setTitle("测试文章");
        testPost.setContent("测试内容");
        testPost.setStatus(1);
        blogPostMapper.insert(testPost);
    }

    private void setUpAdminContext() {
        UserContext ctx = new UserContext(admin.getId(), "admin");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ReportRequest createReportRequest(String targetType, Long targetId, String reason) {
        ReportRequest req = new ReportRequest();
        req.setTargetType(targetType);
        req.setTargetId(targetId);
        req.setReason(reason);
        return req;
    }

    @Test
    @DisplayName("创建举报成功")
    void createReport_Success() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规内容");
        Long reportId = reportService.createReport(req, reporter.getId());

        assertNotNull(reportId);
        BlogReport saved = blogReportMapper.selectById(reportId);
        assertEquals(0, saved.getStatus()); // 待处理
    }

    @Test
    @DisplayName("创建举报 - 原因为空抛400")
    void createReport_EmptyReason_ThrowsException() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.createReport(req, reporter.getId()));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("创建举报 - 无效目标类型抛400")
    void createReport_InvalidTargetType_ThrowsException() {
        ReportRequest req = createReportRequest("invalid", testPost.getId(), "违规");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.createReport(req, reporter.getId()));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("创建举报 - 目标不存在抛404")
    void createReport_TargetNotFound_ThrowsException() {
        ReportRequest req = createReportRequest("post", 99999L, "违规");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.createReport(req, reporter.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("创建举报 - 不能举报自己")
    void createReport_SelfReport_ThrowsException() {
        // postAuthor 举报自己的文章
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.createReport(req, postAuthor.getId()));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能举报自己"));
    }

    @Test
    @DisplayName("创建举报 - 重复举报抛400")
    void createReport_Duplicate_ThrowsException() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规");
        reportService.createReport(req, reporter.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.createReport(req, reporter.getId()));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("重复提交"));
    }

    @Test
    @DisplayName("获取我的举报列表")
    void getMyReports_Success() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规");
        reportService.createReport(req, reporter.getId());

        IPage<ReportVO> page = reportService.getMyReports(1, 10, reporter.getId());
        assertNotNull(page);
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("获取待处理举报列表 - 管理员")
    void getPendingReports_Admin_Success() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规");
        reportService.createReport(req, reporter.getId());

        setUpAdminContext();
        IPage<ReportVO> page = reportService.getPendingReports(1, 10);
        assertNotNull(page);
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("获取待处理举报列表 - 非管理员抛403")
    void getPendingReports_NotAdmin_ThrowsException() {
        UserContext ctx = new UserContext(reporter.getId(), "user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.getPendingReports(1, 10));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("处理举报 - 驳回")
    void handleReport_Reject_Success() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规");
        Long reportId = reportService.createReport(req, reporter.getId());

        setUpAdminContext();
        HandleReportRequest handleReq = new HandleReportRequest();
        handleReq.setStatus(1); // 已驳回
        handleReq.setHandlerResult("不构成违规");
        reportService.handleReport(reportId, handleReq, admin.getId());

        BlogReport updated = blogReportMapper.selectById(reportId);
        assertEquals(1, updated.getStatus());
    }

    @Test
    @DisplayName("处理举报 - 核实")
    void handleReport_Resolve_Success() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规");
        Long reportId = reportService.createReport(req, reporter.getId());

        setUpAdminContext();
        HandleReportRequest handleReq = new HandleReportRequest();
        handleReq.setStatus(2); // 已核实
        handleReq.setHandlerResult("确认违规");
        reportService.handleReport(reportId, handleReq, admin.getId());

        BlogReport updated = blogReportMapper.selectById(reportId);
        assertEquals(2, updated.getStatus());
        assertNotNull(updated.getHandleTime());
    }

    @Test
    @DisplayName("处理举报 - 已处理的举报不能重复处理")
    void handleReport_AlreadyHandled_ThrowsException() {
        ReportRequest req = createReportRequest("post", testPost.getId(), "违规");
        Long reportId = reportService.createReport(req, reporter.getId());

        setUpAdminContext();
        HandleReportRequest handleReq = new HandleReportRequest();
        handleReq.setStatus(1);
        reportService.handleReport(reportId, handleReq, admin.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.handleReport(reportId, handleReq, admin.getId()));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已被处理"));
    }

    @Test
    @DisplayName("处理举报 - 不存在的举报抛404")
    void handleReport_NotFound_ThrowsException() {
        setUpAdminContext();
        HandleReportRequest handleReq = new HandleReportRequest();
        handleReq.setStatus(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.handleReport(99999L, handleReq, admin.getId()));
        assertEquals(404, ex.getCode());
    }
}
