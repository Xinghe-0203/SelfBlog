package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.ai.AiHealthReportVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F 站点体检的集成测试。
 *
 * <p>覆盖三件容易写错的事：素材只送元数据不送正文、单次调用不逐篇、
 * 以及模型给出的 score/summary/findings 越界时如何夹紧或修复重试。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AiHealthReportService")
class AiHealthReportServiceTest {

    private static final long POST_ID_1 = 885001L;
    private static final long POST_ID_2 = 885002L;
    private static final long POST_ID_3 = 885003L;
    private static final String API_KEY = "sk-ai-health-secret-abcdefgh";

    /** 正文里放一个只可能出现在正文的标记，用来断言它没被送进模型。 */
    private static final String BODY_MARKER = "正文内部标记只有本测试知道";

    private static final String LONG_SUMMARY =
            "站点内容整体健康，但摘要覆盖不足，部分文章缺少摘要或摘要过短，影响了搜索结果的展示效果，"
                    + "建议尽快补齐摘要并统一标签命名规则，同时为缺少封面的文章补上封面图片，"
                    + "以提升整体阅读体验和搜索引擎收录质量。此外还有少量标题过长与重复标题的问题需要处理。";

    private static final String REPORT_JSON = """
            {"score":72,"summary":"%s","findings":[
              {"title":"摘要缺失","detail":"12 篇已发布文章没有摘要","severity":"medium","action":"在文章编辑页补全摘要","count":12},
              {"title":"标签命名不统一","detail":"同一主题存在多个近义标签","severity":"low","action":"合并近义标签"},
              {"title":"封面缺失","detail":"3 篇文章没有封面图","severity":"high","action":"为文章补充封面","count":3}
            ]}
            """.formatted(LONG_SUMMARY);

    @Autowired
    private AiHealthReportService service;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    private AiStubLlmServer server;

    @BeforeEach
    void setUp() {
        clearSiteCache();
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_tag");
        jdbc.update("DELETE FROM blog_post WHERE id IN (?, ?, ?)", POST_ID_1, POST_ID_2, POST_ID_3);
        insertPost(POST_ID_1, "第一篇", 1);
        insertPost(POST_ID_2, "第二篇", 1);
        insertPost(POST_ID_3, "第三篇", 1);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
            server = null;
        }
        clearSiteCache();
    }

    private void clearSiteCache() {
        var cache = caches.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private void insertPost(long id, String title, int status) {
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, "
                        + "TIMESTAMP '2026-10-0" + (id % 10) + " 10:00:00', "
                        + "TIMESTAMP '2026-10-0" + (id % 10) + " 10:00:00', 0)",
                id, title, "这是" + title + "的摘要", BODY_MARKER + "，正文内容比较长", "技术", status);
    }

    private void configureLlm(String baseUrl) {
        settings.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", baseUrl,
                "llmApiKey", API_KEY,
                "llmModel", "deepseek-flash",
                "llmTimeoutSeconds", "10"));
        clearSiteCache();
    }

    private void configureLlm(String baseUrl, String model) {
        settings.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", baseUrl,
                "llmApiKey", API_KEY,
                "llmModel", model,
                "llmTimeoutSeconds", "10"));
        clearSiteCache();
    }

    @Test
    @DisplayName("report_HappyPath_ReturnsReportWithMeta")
    void report_HappyPath_ReturnsReportWithMeta() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(REPORT_JSON));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(null);

        assertEquals(72, vo.getScore());
        assertEquals(LONG_SUMMARY.length(), vo.getSummary().length());
        assertEquals(3, vo.getFindings().size());
        assertEquals(3, vo.getSampledPosts());
        assertEquals(3, vo.getTotalPosts());
        assertEquals("deepseek-flash", vo.getModel());
        assertTrue(vo.getLatencyMs() >= 0);
        assertNotNull(vo.getGeneratedAt());
        assertTrue(vo.getGeneratedAt().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"),
                "generatedAt 用 LocalDateTime.toString()，实际：" + vo.getGeneratedAt());
        assertEquals(1, server.callCount(), "单次调用体检，不逐篇");
    }

    @Test
    @DisplayName("report_MaterialHasMetadataButNoBody")
    void report_MaterialHasMetadataButNoBody() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(REPORT_JSON));
        configureLlm(server.baseUrl());

        service.report(null);

        String body = server.bodies().get(0);
        assertTrue(body.contains("第一篇"), "素材要有标题");
        assertTrue(body.contains("技术"), "素材要有分类");
        assertTrue(body.contains("字数："), "素材要带字数");
        assertFalse(body.contains(BODY_MARKER), "正文内容不得进体检素材");
    }

    @Test
    @DisplayName("report_CountsOnlyPublishedPosts")
    void report_CountsOnlyPublishedPosts() throws Exception {
        insertPost(885004L, "草稿篇", 0);
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(REPORT_JSON));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(null);

        assertEquals(3, vo.getTotalPosts(), "待审文章不计入已发布总数");
        assertEquals(3, vo.getSampledPosts());
        assertFalse(server.bodies().get(0).contains("草稿篇"));
    }

    @Test
    @DisplayName("report_LimitCapsSampledPosts")
    void report_LimitCapsSampledPosts() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(REPORT_JSON));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(1);

        assertEquals(1, vo.getSampledPosts());
        assertEquals(3, vo.getTotalPosts(), "总数仍是全部已发布文章");
    }

    @Test
    @DisplayName("normalizeLimit_DefaultsTo40AndCapsAt80")
    void normalizeLimit_DefaultsTo40AndCapsAt80() {
        assertEquals(AiHealthReportService.LIMIT_DEFAULT, AiHealthReportService.normalizeLimit(null));
        assertEquals(AiHealthReportService.LIMIT_DEFAULT, AiHealthReportService.normalizeLimit(0));
        assertEquals(AiHealthReportService.LIMIT_DEFAULT, AiHealthReportService.normalizeLimit(-5));
        assertEquals(7, AiHealthReportService.normalizeLimit(7));
        assertEquals(AiHealthReportService.LIMIT_MAX, AiHealthReportService.normalizeLimit(1000));
        assertEquals(40, AiHealthReportService.LIMIT_DEFAULT);
        assertEquals(80, AiHealthReportService.LIMIT_MAX);
    }

    @Test
    @DisplayName("report_NoPublishedPosts_Throws400WithoutCallingModel")
    void report_NoPublishedPosts_Throws400WithoutCallingModel() throws Exception {
        jdbc.update("UPDATE blog_post SET status = 0 WHERE id IN (?, ?, ?)",
                POST_ID_1, POST_ID_2, POST_ID_3);
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(REPORT_JSON));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(400, error.getCode());
        assertEquals("还没有已发布的文章", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("report_ScoreAbove100_ClampedTo100")
    void report_ScoreAbove100_ClampedTo100() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                REPORT_JSON.replace("\"score\":72", "\"score\":150")));
        configureLlm(server.baseUrl());

        assertEquals(100, service.report(null).getScore());
    }

    @Test
    @DisplayName("report_ScoreBelowZero_ClampedToZero")
    void report_ScoreBelowZero_ClampedToZero() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                REPORT_JSON.replace("\"score\":72", "\"score\":-20")));
        configureLlm(server.baseUrl());

        assertEquals(0, service.report(null).getScore());
    }

    @Test
    @DisplayName("report_SummaryTooShort_RepairsThenFails502")
    void report_SummaryTooShort_RepairsThenFails502() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                REPORT_JSON.replace(LONG_SUMMARY, "站点很健康")));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertEquals(2, server.callCount(), "不合规时先修复重试一次");
        assertTrue(server.bodies().get(1).contains("站点很健康"), "修复提示要带上一次的输出");
    }

    @Test
    @DisplayName("report_SummaryOver300_TruncatedTo300")
    void report_SummaryOver300_TruncatedTo300() throws Exception {
        String overlong = LONG_SUMMARY + "补".repeat(300);
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                REPORT_JSON.replace(LONG_SUMMARY, overlong)));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(null);

        assertEquals(AiHealthReportService.SUMMARY_MAX, vo.getSummary().length());
    }

    @Test
    @DisplayName("report_TooFewFindings_RepairsThenFails502")
    void report_TooFewFindings_RepairsThenFails502() throws Exception {
        String oneFinding = "{\"score\":60,\"summary\":\"" + LONG_SUMMARY
                + "\",\"findings\":[{\"title\":\"摘要缺失\",\"detail\":\"有些文章没摘要\","
                + "\"severity\":\"medium\",\"action\":\"补摘要\",\"count\":1}]}";
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(oneFinding));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertEquals(2, server.callCount());
    }

    @Test
    @DisplayName("report_MoreThanEightFindings_CappedAtEight")
    void report_MoreThanEightFindings_CappedAtEight() throws Exception {
        StringBuilder findings = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            if (i > 1) {
                findings.append(',');
            }
            findings.append("{\"title\":\"问题").append(i).append("\",\"detail\":\"细节")
                    .append(i).append("\",\"severity\":\"low\",\"action\":\"处理\"}");
        }
        String payload = "{\"score\":60,\"summary\":\"" + LONG_SUMMARY
                + "\",\"findings\":[" + findings + "]}";
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(payload));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(null);

        assertEquals(AiHealthReportService.FINDINGS_MAX, vo.getFindings().size());
        assertEquals(8, AiHealthReportService.FINDINGS_MAX);
    }

    @Test
    @DisplayName("report_UnknownSeverity_FallsBackToLow")
    void report_UnknownSeverity_FallsBackToLow() throws Exception {
        String payload = REPORT_JSON.replace("\"severity\":\"high\"", "\"severity\":\"critical\"");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(payload));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(null);

        assertEquals("low", vo.getFindings().get(2).getSeverity());
        assertEquals("medium", vo.getFindings().get(0).getSeverity());
    }

    @Test
    @DisplayName("report_FindingWithoutCount_KeepsNull")
    void report_FindingWithoutCount_KeepsNull() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(REPORT_JSON));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(null);

        assertEquals(12, vo.getFindings().get(0).getCount());
        assertNull(vo.getFindings().get(1).getCount(), "非计数类问题 count 为空");
        assertEquals("合并近义标签", vo.getFindings().get(1).getAction());
    }

    @Test
    @DisplayName("report_EntryMissingDetail_IsSkipped")
    void report_EntryMissingDetail_IsSkipped() throws Exception {
        String payload = REPORT_JSON.replace("\"detail\":\"3 篇文章没有封面图\",", "");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(payload));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(502, error.getCode(),
                "缺 detail 的条目被跳过，只剩 2 条 < 3 条下限 → 不合规");
    }

    @Test
    @DisplayName("report_AiDisabled_Throws400")
    void report_AiDisabled_Throws400() throws Exception {
        settings.updateSettings(Map.of("llmEnabled", "false"));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(400, error.getCode());
        assertEquals("AI 辅助功能已关闭，请先在设置中启用", error.getMessage());
    }

    @Test
    @DisplayName("report_NotConfigured_Throws400")
    void report_NotConfigured_Throws400() throws Exception {
        settings.updateSettings(Map.of("llmEnabled", "true", "llmBaseUrl", "", "llmApiKey", ""));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(400, error.getCode());
        assertEquals("尚未配置可用的模型接口，请先在设置中填写", error.getMessage());
    }

    @Test
    @DisplayName("report_UpstreamServerError_Throws502")
    void report_UpstreamServerError_Throws502() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.status(500, "{\"error\":\"boom\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(502, error.getCode());
        assertEquals("模型服务暂时不可用，请稍后重试", error.getMessage());
    }

    @Test
    @DisplayName("report_UpstreamUnauthorized_502WithoutApiKeyLeak")
    void report_UpstreamUnauthorized_502WithoutApiKeyLeak() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.status(401, "{\"error\":\"invalid api key\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> service.report(null));

        assertEquals(502, error.getCode());
        assertEquals("模型鉴权失败，请检查 API Key 是否有效", error.getMessage());
        assertFalse(error.getMessage().contains(API_KEY), "错误文案不得泄漏 API Key");
        assertEquals("Bearer " + API_KEY, server.authorizations().get(0));
    }

    @Test
    @DisplayName("report_ModelName_ComesFromSettings")
    void report_ModelName_ComesFromSettings() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(REPORT_JSON));
        configureLlm(server.baseUrl(), "deepseek-v4-pro");

        AiHealthReportVO vo = service.report(null);

        assertEquals("deepseek-v4-pro", vo.getModel());
    }

    @Test
    @DisplayName("report_MarkdownSummary_StrippedToPlainText")
    void report_MarkdownSummary_StrippedToPlainText() throws Exception {
        // 注意：替换进 JSON 字符串的值必须用转义换行，否则整个响应体不再是合法 JSON。
        String markdown = "## 体检结论\\n\\n" + LONG_SUMMARY;
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                REPORT_JSON.replace(LONG_SUMMARY, markdown)));
        configureLlm(server.baseUrl());

        AiHealthReportVO vo = service.report(null);

        assertFalse(vo.getSummary().contains("##"), "摘要必须是纯文本");
        assertFalse(vo.getSummary().contains("\n"));
    }
}
