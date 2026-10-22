package com.selfblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.controller.admin.AdminSeoController;
import com.selfblog.dto.seo.SeoBatchRequest;
import com.selfblog.dto.seo.SeoUpdateRequest;
import com.selfblog.service.seo.SeoAuditService;
import com.selfblog.service.seo.SeoGenerationService;
import com.selfblog.service.seo.SeoMetadataService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.vo.seo.SeoBatchResultVO;
import com.selfblog.vo.seo.SeoDetailVO;
import com.selfblog.vo.seo.SeoLlmModelVO;
import com.selfblog.vo.seo.SeoLlmModelsVO;
import com.selfblog.vo.seo.SeoLlmTestVO;
import com.selfblog.vo.seo.SeoOverviewVO;
import com.selfblog.vo.seo.SeoPostRowVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminSeoController 契约测试。
 *
 * <p>路径不写 {@code /api}（测试 profile 的 {@code app.api-prefix} 为空）。
 * 必须 {@code @MockBean JwtUtils}，否则 {@code SecurityConfig} 的过滤器没有 Bean、上下文起不来；
 * 切片里方法级 {@code @PreAuthorize} 是生效的，所以非管理员必须 403。</p>
 *
 * <p>请求数量刻意控制在 20 条以内：{@code /api/admin/} 有 60 次/分钟的兜底限流。</p>
 */
@WebMvcTest(AdminSeoController.class)
@DisplayName("AdminSeoController")
class AdminSeoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @MockBean
    private SeoAuditService auditService;
    @MockBean
    private SeoGenerationService generationService;
    @MockBean
    private SeoMetadataService metadataService;
    @MockBean
    private JwtUtils jwtUtils;

    private SeoOverviewVO overview() {
        SeoOverviewVO vo = new SeoOverviewVO();
        vo.setTotalPosts(12);
        vo.setWithSeo(5);
        vo.setCoveragePercent(42);
        vo.setMissingDescription(3);
        vo.setLongTitle(1);
        vo.setDuplicateTitle(2);
        vo.setMissingCover(4);
        vo.setLlmEnabled(true);
        vo.setLlmConfigured(true);
        vo.setAutoOnPublish(false);
        vo.setModel("gpt-4o-mini");
        return vo;
    }

    private SeoPostRowVO row(long postId) {
        SeoPostRowVO row = new SeoPostRowVO();
        row.setPostId(postId);
        row.setTitle("文章标题");
        row.setStatus(1);
        row.setHasSeo(true);
        row.setSeoTitle("SEO 标题");
        row.setSeoDescription("SEO 描述");
        row.setSeoKeywords("关键词甲,关键词乙");
        row.setOgImageUrl("/uploads/share.png");
        row.setSource("llm");
        row.setGeneratedAt("2026-10-21T14:03:11");
        row.setUpdateTime("2026-10-21T14:03:11");
        row.setIssues(List.of("missingCover"));
        return row;
    }

    private SeoDetailVO detail(long postId) {
        SeoDetailVO vo = new SeoDetailVO();
        vo.setPostId(postId);
        vo.setTitle("文章标题");
        vo.setStatus(1);
        vo.setHasSeo(true);
        vo.setSeoTitle("SEO 标题");
        vo.setSeoDescription("SEO 描述");
        vo.setSeoKeywords("关键词甲");
        vo.setOgImageUrl("/uploads/share.png");
        vo.setSource("manual");
        vo.setUpdateTime("2026-10-21T14:03:11");
        vo.setPostSummary("文章摘要");
        vo.setEffectiveTitle("SEO 标题");
        vo.setEffectiveDescription("SEO 描述");
        vo.setIssues(List.of());
        return vo;
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("overview_ReturnsEnvelopeWithAllCounters")
    void overview_ReturnsEnvelopeWithAllCounters() throws Exception {
        when(auditService.overview()).thenReturn(overview());

        mockMvc.perform(get("/admin/seo/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalPosts").value(12))
                .andExpect(jsonPath("$.data.withSeo").value(5))
                .andExpect(jsonPath("$.data.coveragePercent").value(42))
                .andExpect(jsonPath("$.data.missingDescription").value(3))
                .andExpect(jsonPath("$.data.longTitle").value(1))
                .andExpect(jsonPath("$.data.duplicateTitle").value(2))
                .andExpect(jsonPath("$.data.missingCover").value(4))
                .andExpect(jsonPath("$.data.llmEnabled").value(true))
                .andExpect(jsonPath("$.data.llmConfigured").value(true))
                .andExpect(jsonPath("$.data.autoOnPublish").value(false))
                .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("posts_ReturnsMyBatisPageShape")
    void posts_ReturnsMyBatisPageShape() throws Exception {
        Page<SeoPostRowVO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(row(100L)));
        when(auditService.page(eq(1), eq(10), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/admin/seo/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].postId").value(100))
                .andExpect(jsonPath("$.data.records[0].title").value("文章标题"))
                .andExpect(jsonPath("$.data.records[0].hasSeo").value(true))
                .andExpect(jsonPath("$.data.records[0].source").value("llm"))
                .andExpect(jsonPath("$.data.records[0].issues[0]").value("missingCover"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.pages").value(1));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("posts_PassesKeywordAndIssueThrough")
    void posts_PassesKeywordAndIssueThrough() throws Exception {
        Page<SeoPostRowVO> page = new Page<>(2, 20, 0);
        when(auditService.page(eq(2), eq(20), eq("事务"), eq(com.selfblog.service.seo.SeoIssue.LONG_TITLE)))
                .thenReturn(page);

        mockMvc.perform(get("/admin/seo/posts")
                        .param("pageNum", "2")
                        .param("pageSize", "20")
                        .param("keyword", "事务")
                        .param("issue", "longTitle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("posts_UnknownIssueIsTreatedAsNoFilter")
    void posts_UnknownIssueIsTreatedAsNoFilter() throws Exception {
        Page<SeoPostRowVO> page = new Page<>(1, 10, 0);
        when(auditService.page(eq(1), eq(10), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/admin/seo/posts").param("issue", "not-an-issue"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("detail_ReturnsEffectivePreviewValues")
    void detail_ReturnsEffectivePreviewValues() throws Exception {
        when(auditService.detail(100L)).thenReturn(detail(100L));

        mockMvc.perform(get("/admin/seo/posts/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(100))
                .andExpect(jsonPath("$.data.seoTitle").value("SEO 标题"))
                .andExpect(jsonPath("$.data.postSummary").value("文章摘要"))
                .andExpect(jsonPath("$.data.effectiveTitle").value("SEO 标题"))
                .andExpect(jsonPath("$.data.effectiveDescription").value("SEO 描述"))
                .andExpect(jsonPath("$.data.source").value("manual"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("detail_NoSeoRowMaps404")
    void detail_NoSeoRowMaps404() throws Exception {
        when(auditService.detail(100L)).thenThrow(new BusinessException(404, "该文章还没有 SEO 元数据"));

        mockMvc.perform(get("/admin/seo/posts/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("该文章还没有 SEO 元数据"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("save_ReturnsFullDetailWithManualSource")
    void save_ReturnsFullDetailWithManualSource() throws Exception {
        when(metadataService.save(eq(100L), any(SeoUpdateRequest.class))).thenReturn(detail(100L));

        SeoUpdateRequest body = new SeoUpdateRequest();
        body.setSeoTitle("SEO 标题");
        body.setSeoDescription("SEO 描述");
        body.setSeoKeywords("关键词甲");
        body.setOgImageUrl("/uploads/share.png");

        mockMvc.perform(put("/admin/seo/posts/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("manual"))
                .andExpect(jsonPath("$.data.effectiveTitle").value("SEO 标题"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("save_ValidationFailureMaps400")
    void save_ValidationFailureMaps400() throws Exception {
        when(metadataService.save(eq(100L), any(SeoUpdateRequest.class)))
                .thenThrow(new BusinessException(400, "SEO 标题不能超过 200 个字符"));

        mockMvc.perform(put("/admin/seo/posts/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seoTitle\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("delete_ReturnsNullData")
    void delete_ReturnsNullData() throws Exception {
        mockMvc.perform(delete("/admin/seo/posts/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(metadataService).delete(100L);
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("generate_ReturnsLlmDetail")
    void generate_ReturnsLlmDetail() throws Exception {
        SeoDetailVO generated = detail(100L);
        generated.setSource("llm");
        when(generationService.generate(100L)).thenReturn(generated);

        mockMvc.perform(post("/admin/seo/posts/100/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("llm"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("generate_NotConfiguredMaps400")
    void generate_NotConfiguredMaps400() throws Exception {
        when(generationService.generate(100L))
                .thenThrow(new BusinessException(400, "尚未配置模型，请先在设置中填写接口地址与 API Key"));

        mockMvc.perform(post("/admin/seo/posts/100/generate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("generate_UpstreamFailureMaps502")
    void generate_UpstreamFailureMaps502() throws Exception {
        when(generationService.generate(100L))
                .thenThrow(new BusinessException(502, "模型鉴权失败，请检查 API Key 是否有效"));

        mockMvc.perform(post("/admin/seo/posts/100/generate"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("模型鉴权失败，请检查 API Key 是否有效"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("batch_ReturnsCounters")
    void batch_ReturnsCounters() throws Exception {
        SeoBatchResultVO result = new SeoBatchResultVO();
        result.setRequested(3);
        result.setSucceeded(2);
        result.setFailed(1);
        result.setSkipped(1);
        result.getFailures().add(new SeoBatchResultVO.Failure(101L, "模型响应超时，请稍后重试"));
        when(generationService.batch(any(SeoBatchRequest.class))).thenReturn(result);

        mockMvc.perform(post("/admin/seo/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":10,\"onlyMissing\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(3))
                .andExpect(jsonPath("$.data.succeeded").value(2))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.skipped").value(1))
                .andExpect(jsonPath("$.data.failures[0].postId").value(101))
                .andExpect(jsonPath("$.data.failures[0].message").value("模型响应超时，请稍后重试"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("batch_EmptyBodyUsesDefaults")
    void batch_EmptyBodyUsesDefaults() throws Exception {
        when(generationService.batch(any(SeoBatchRequest.class))).thenReturn(new SeoBatchResultVO());

        mockMvc.perform(post("/admin/seo/batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(0));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("testConnection_AlwaysReturnsEnvelope200")
    void testConnection_AlwaysReturnsEnvelope200() throws Exception {
        SeoLlmTestVO vo = new SeoLlmTestVO();
        vo.setOk(true);
        vo.setMessage("连接成功。");
        vo.setLatencyMs(120);
        vo.setModel("gpt-4o-mini");
        when(generationService.testConnection()).thenReturn(vo);

        mockMvc.perform(post("/admin/seo/test-connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.message").value("连接成功。"))
                .andExpect(jsonPath("$.data.latencyMs").value(120))
                .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("models_ReturnsUpstreamListWithSourceFlag")
    void models_ReturnsUpstreamListWithSourceFlag() throws Exception {
        SeoLlmModelsVO vo = new SeoLlmModelsVO(List.of(
                new SeoLlmModelVO("deepseek-flash", "DeepSeek-V4.1-Flash", true),
                new SeoLlmModelVO("deepseek-v4-pro", "DeepSeek-V4-Pro", false)), "upstream", null);
        when(generationService.listModels()).thenReturn(vo);

        mockMvc.perform(get("/admin/seo/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.source").value("upstream"))
                .andExpect(jsonPath("$.data.models[0].id").value("deepseek-flash"))
                .andExpect(jsonPath("$.data.models[0].supportsImage").value(true))
                .andExpect(jsonPath("$.data.models[1].supportsImage").value(false));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("models_FallbackStillReturnsEnvelope200")
    void models_FallbackStillReturnsEnvelope200() throws Exception {
        // 探测失败不是错误：拉框必须照样能用，所以恒 200 + source=fallback + 原因文案。
        SeoLlmModelsVO vo = new SeoLlmModelsVO(
                List.of(new SeoLlmModelVO("deepseek-flash", "DeepSeek Flash（支持图像）", true)),
                "fallback", "模型鉴权失败，请检查 API Key 是否有效");
        when(generationService.listModels()).thenReturn(vo);

        mockMvc.perform(get("/admin/seo/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("fallback"))
                .andExpect(jsonPath("$.data.reason").value("模型鉴权失败，请检查 API Key 是否有效"))
                .andExpect(jsonPath("$.data.models[0].id").value("deepseek-flash"));
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("nonAdmin_IsForbiddenOnEveryEndpoint")
    void nonAdmin_IsForbiddenOnEveryEndpoint() throws Exception {
        mockMvc.perform(get("/admin/seo/overview")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/seo/posts")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/seo/models")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/seo/test-connection")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/admin/seo/posts/100")).andExpect(status().isForbidden());
    }
}
