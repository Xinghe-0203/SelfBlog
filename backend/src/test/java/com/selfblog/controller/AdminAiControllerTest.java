package com.selfblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.controller.admin.AdminAiController;
import com.selfblog.dto.ai.AiHealthReportRequest;
import com.selfblog.dto.ai.AiImageAltRequest;
import com.selfblog.dto.ai.AiModerationCheckRequest;
import com.selfblog.dto.ai.AiModerationScanRequest;
import com.selfblog.dto.ai.AiPostRequest;
import com.selfblog.dto.ai.AiProofreadBatchRequest;
import com.selfblog.service.ai.AiHealthReportService;
import com.selfblog.service.ai.AiImageAltService;
import com.selfblog.service.ai.AiModerationService;
import com.selfblog.service.ai.AiProofreadService;
import com.selfblog.service.ai.AiWritingService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.vo.ai.AiHealthReportVO;
import com.selfblog.vo.ai.AiImageAltVO;
import com.selfblog.vo.ai.AiModerationScanVO;
import com.selfblog.vo.ai.AiModerationVO;
import com.selfblog.vo.ai.AiProofreadBatchVO;
import com.selfblog.vo.ai.AiProofreadVO;
import com.selfblog.vo.ai.AiSummaryVO;
import com.selfblog.vo.ai.AiTagsVO;
import com.selfblog.vo.ai.AiTitlesVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminAiController 契约测试。
 *
 * <p>路径不写 {@code /api}（测试 profile 的 {@code app.api-prefix} 为空）。
 * 必须 {@code @MockBean JwtUtils}，否则 {@code SecurityConfig} 的过滤器没有 Bean、上下文起不来；
 * 切片里方法级 {@code @PreAuthorize} 生效，所以非管理员必须 403。</p>
 *
 * <p>请求数量刻意控制在 20 条以内：{@code /api/admin/} 有 60 次/分钟的兜底限流。</p>
 */
@WebMvcTest(AdminAiController.class)
@DisplayName("AdminAiController")
class AdminAiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @MockBean
    private AiWritingService writingService;
    @MockBean
    private AiModerationService moderationService;
    @MockBean
    private AiProofreadService proofreadService;
    @MockBean
    private AiImageAltService imageAltService;
    @MockBean
    private AiHealthReportService healthReportService;
    @MockBean
    private JwtUtils jwtUtils;

    private static final String POST_BODY = "{\"postId\":12}";

    @Test
    @DisplayName("summary_ReturnsEnvelope")
    @WithMockUser(roles = "admin")
    void summary_ReturnsEnvelope() throws Exception {
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSummary("这是一段摘要");
        vo.setModel("deepseek-flash");
        vo.setLatencyMs(1234L);
        when(writingService.summary(12L)).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/writing/summary")
                        .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.summary").value("这是一段摘要"))
                .andExpect(jsonPath("$.data.model").value("deepseek-flash"))
                .andExpect(jsonPath("$.data.latencyMs").value(1234));
    }

    @Test
    @DisplayName("tags_ReturnsEnvelope")
    @WithMockUser(roles = "admin")
    void tags_ReturnsEnvelope() throws Exception {
        AiTagsVO vo = new AiTagsVO();
        vo.setTags(List.of("Spring Boot", "事务"));
        vo.setModel("deepseek-flash");
        vo.setLatencyMs(900L);
        when(writingService.tags(12L)).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/writing/tags")
                        .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags[0]").value("Spring Boot"))
                .andExpect(jsonPath("$.data.tags[1]").value("事务"));
    }

    @Test
    @DisplayName("titles_ReturnsThreeCandidates")
    @WithMockUser(roles = "admin")
    void titles_ReturnsThreeCandidates() throws Exception {
        AiTitlesVO vo = new AiTitlesVO();
        vo.setTitles(List.of("候选一", "候选二", "候选三"));
        vo.setModel("deepseek-flash");
        vo.setLatencyMs(1500L);
        when(writingService.titles(12L)).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/writing/titles")
                        .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titles.length()").value(3))
                .andExpect(jsonPath("$.data.titles[2]").value("候选三"));
    }

    @Test
    @DisplayName("writing_WithoutBody_UsesNullPostId")
    @WithMockUser(roles = "admin")
    void writing_WithoutBody_UsesNullPostId() throws Exception {
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSummary("摘要");
        when(writingService.summary(isNull())).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/writing/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(writingService).summary(isNull());
    }

    @Test
    @DisplayName("moderationCheck_PassesKind")
    @WithMockUser(roles = "admin")
    void moderationCheck_PassesKind() throws Exception {
        AiModerationVO vo = new AiModerationVO();
        vo.setVerdict("pass");
        vo.setReason("正常留言");
        vo.setCategories(List.of());
        vo.setModel("deepseek-flash");
        vo.setLatencyMs(900L);
        when(moderationService.check(5L, "guest")).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/moderation/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentId\":5,\"kind\":\"guest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verdict").value("pass"))
                .andExpect(jsonPath("$.data.categories.length()").value(0));
    }

    @Test
    @DisplayName("moderationScan_ReturnsFlaggedItems")
    @WithMockUser(roles = "admin")
    void moderationScan_ReturnsFlaggedItems() throws Exception {
        AiModerationScanVO.Item item = new AiModerationScanVO.Item();
        item.setCommentId(5L);
        item.setKind("guest");
        item.setNickname("张三");
        item.setExcerpt("加微信...");
        item.setVerdict("block");
        item.setReason("广告");
        item.setCategories(List.of("ad"));
        AiModerationScanVO vo = new AiModerationScanVO();
        vo.setScanned(20);
        vo.setFlagged(1);
        vo.setItems(List.of(item));
        when(moderationService.scan(20)).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/moderation/scan")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"limit\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scanned").value(20))
                .andExpect(jsonPath("$.data.flagged").value(1))
                .andExpect(jsonPath("$.data.items[0].verdict").value("block"))
                .andExpect(jsonPath("$.data.items[0].categories[0]").value("ad"));
    }

    @Test
    @DisplayName("proofread_ReturnsIssues")
    @WithMockUser(roles = "admin")
    void proofread_ReturnsIssues() throws Exception {
        AiProofreadVO.Issue issue = new AiProofreadVO.Issue();
        issue.setType("typo");
        issue.setExcerpt("帐号");
        issue.setSuggestion("账号");
        issue.setSeverity("low");
        AiProofreadVO vo = new AiProofreadVO();
        vo.setPostId(12L);
        vo.setIssues(List.of(issue));
        vo.setTruncated(false);
        vo.setModel("deepseek-flash");
        vo.setLatencyMs(4200L);
        when(proofreadService.proofread(12L)).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/proofread/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(12))
                .andExpect(jsonPath("$.data.truncated").value(false))
                .andExpect(jsonPath("$.data.issues[0].type").value("typo"))
                .andExpect(jsonPath("$.data.issues[0].excerpt").value("帐号"));
    }

    @Test
    @DisplayName("proofreadBatch_ReturnsCounts")
    @WithMockUser(roles = "admin")
    void proofreadBatch_ReturnsCounts() throws Exception {
        AiProofreadBatchVO.Failure failure = new AiProofreadBatchVO.Failure();
        failure.setPostId(3L);
        failure.setReason("模型返回的内容不符合要求，请重试或手动填写");
        AiProofreadBatchVO vo = new AiProofreadBatchVO();
        vo.setRequested(3);
        vo.setSucceeded(2);
        vo.setFailed(1);
        vo.setResults(List.of());
        vo.setFailures(List.of(failure));
        when(proofreadService.proofreadBatch(any(), eq(5))).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/proofread/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postIds\":[1,2,3],\"limit\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(3))
                .andExpect(jsonPath("$.data.succeeded").value(2))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.failures[0].postId").value(3));
    }

    @Test
    @DisplayName("imageAlt_ReturnsAltAndCaption")
    @WithMockUser(roles = "admin")
    void imageAlt_ReturnsAltAndCaption() throws Exception {
        AiImageAltVO vo = new AiImageAltVO();
        vo.setAlt("白色桌面上摊开的笔记本与咖啡杯");
        vo.setCaption("写作时的桌面");
        vo.setModel("deepseek-flash");
        vo.setLatencyMs(2600L);
        when(imageAltService.describe("cover", 12L, null)).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/images/alt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"cover\",\"postId\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alt").value("白色桌面上摊开的笔记本与咖啡杯"))
                .andExpect(jsonPath("$.data.caption").value("写作时的桌面"));
    }

    @Test
    @DisplayName("healthReport_ReturnsFindings")
    @WithMockUser(roles = "admin")
    void healthReport_ReturnsFindings() throws Exception {
        AiHealthReportVO.Finding finding = new AiHealthReportVO.Finding();
        finding.setTitle("摘要缺失");
        finding.setDetail("12 篇已发布文章没有摘要");
        finding.setSeverity("medium");
        finding.setAction("在文章编辑页补全摘要");
        finding.setCount(12);
        AiHealthReportVO vo = new AiHealthReportVO();
        vo.setGeneratedAt("2026-10-22T13:30:00");
        vo.setScore(72);
        vo.setSummary("站点内容整体健康");
        vo.setFindings(List.of(finding));
        vo.setSampledPosts(40);
        vo.setTotalPosts(15);
        vo.setModel("deepseek-flash");
        vo.setLatencyMs(5200L);
        when(healthReportService.report(40)).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/health/report")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"limit\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(72))
                .andExpect(jsonPath("$.data.generatedAt").value("2026-10-22T13:30:00"))
                .andExpect(jsonPath("$.data.findings[0].count").value(12))
                .andExpect(jsonPath("$.data.totalPosts").value(15));
    }

    @Test
    @DisplayName("nonAdmin_Forbidden")
    @WithMockUser(roles = "user")
    void nonAdmin_Forbidden() throws Exception {
        mockMvc.perform(post("/admin/ai/writing/summary")
                        .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("anonymous_Forbidden")
    void anonymous_Forbidden() throws Exception {
        mockMvc.perform(post("/admin/ai/writing/summary")
                        .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("businessError_MapsToHttpStatus")
    @WithMockUser(roles = "admin")
    void businessError_MapsToHttpStatus() throws Exception {
        when(writingService.summary(12L))
                .thenThrow(new BusinessException(400, "尚未配置可用的模型接口，请先在设置中填写"));

        mockMvc.perform(post("/admin/ai/writing/summary")
                        .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("尚未配置可用的模型接口，请先在设置中填写"));
    }

    @Test
    @DisplayName("upstreamError_MapsTo502")
    @WithMockUser(roles = "admin")
    void upstreamError_MapsTo502() throws Exception {
        when(writingService.tags(12L))
                .thenThrow(new BusinessException(502, "模型服务暂时不可用，请稍后重试"));

        mockMvc.perform(post("/admin/ai/writing/tags")
                        .contentType(MediaType.APPLICATION_JSON).content(POST_BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502));
    }

    @Test
    @DisplayName("healthReport_WithoutBody_UsesDefaultLimit")
    @WithMockUser(roles = "admin")
    void healthReport_WithoutBody_UsesDefaultLimit() throws Exception {
        AiHealthReportVO vo = new AiHealthReportVO();
        vo.setScore(80);
        when(healthReportService.report(isNull())).thenReturn(vo);

        mockMvc.perform(post("/admin/ai/health/report").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(healthReportService).report(isNull());
    }
}
