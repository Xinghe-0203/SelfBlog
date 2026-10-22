package com.selfblog.service.seo;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.dto.seo.SeoBatchRequest;
import com.selfblog.dto.seo.SeoUpdateRequest;
import com.selfblog.entity.PostSeo;
import com.selfblog.mapper.PostSeoMapper;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.seo.SeoBatchResultVO;
import com.selfblog.vo.seo.SeoDetailVO;
import com.selfblog.vo.seo.SeoLlmTestVO;
import com.selfblog.vo.seo.SeoOverviewVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEO 生成链路的集成测试：真实 H2 + 真实本地 HTTP 上游（{@link StubLlmServer}）。
 *
 * <p>覆盖任务要求的全部失败语义：happy path 落库、坏 JSON 一次修复重试后仍失败、
 * {@code response_format} 被 400 拒绝后重试成功、401 安全文案、超时、批量单条失败隔离。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SeoGenerationService")
class SeoGenerationServiceTest {

    private static final long POST_ID = 880001L;
    private static final long DRAFT_POST_ID = 880002L;
    private static final String API_KEY = "sk-integration-secret-abcdefghij";

    @Autowired
    private SeoGenerationService generationService;
    @Autowired
    private SeoMetadataService metadataService;
    @Autowired
    private SeoAuditService auditService;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private PostSeoMapper postSeoMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    private StubLlmServer server;

    private static String description(int length) {
        return "描".repeat(length);
    }

    private static String seoJson(String title) {
        return "{\"title\":\"" + title + "\",\"description\":\"" + description(140)
                + "\",\"keywords\":[\"Spring Boot\",\"事务\",\"SEO\"]}";
    }

    @BeforeEach
    void setUp() {
        clearSiteCache();
        jdbc.update("DELETE FROM post_seo WHERE post_id IN (?, ?)", POST_ID, DRAFT_POST_ID);
        jdbc.update("DELETE FROM blog_post WHERE id IN (?, ?)", POST_ID, DRAFT_POST_ID);
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-15 10:00:00', TIMESTAMP '2026-10-15 10:00:00', 0)",
                POST_ID, "Spring Boot 事务传播行为详解", "事务传播行为的摘要",
                "## 事务传播\n\nREQUIRED 是默认行为，REQUIRES_NEW 会挂起外层事务。", "后端", "/uploads/cover.jpg");
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, 0, "
                        + "TIMESTAMP '2026-10-15 10:00:00', TIMESTAMP '2026-10-15 10:00:00', 0)",
                DRAFT_POST_ID, "草稿文章", "草稿摘要", "草稿正文", "后端", null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    /** 配置好 LLM 设置（走真实 service，顺带验证键名与校验都能通过）。 */
    private void configureLlm(String baseUrl, boolean autoOnPublish) {
        settings.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", baseUrl,
                "llmApiKey", API_KEY,
                "llmModel", "gpt-4o-mini",
                "llmTimeoutSeconds", "10",
                "llmAutoOnPublish", autoOnPublish ? "true" : "false"));
        clearSiteCache();
    }

    @Test
    @DisplayName("generate_HappyPath_PersistsLlmRow")
    void generate_HappyPath_PersistsLlmRow() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok(seoJson("Spring Boot 事务传播")));
        configureLlm(server.baseUrl(), false);

        SeoDetailVO detail = generationService.generate(POST_ID);

        assertEquals("llm", detail.getSource());
        assertEquals("Spring Boot 事务传播", detail.getSeoTitle());
        assertEquals(140, detail.getSeoDescription().length());
        assertEquals("Spring Boot,事务,SEO", detail.getSeoKeywords());
        assertTrue(detail.isHasSeo());
        assertNotNull(detail.getGeneratedAt());
        assertEquals(POST_ID, detail.getPostId());
        assertEquals("Spring Boot 事务传播行为详解", detail.getTitle());
        // effectiveTitle 优先取 SEO 标题，也就是前台实际渲染的标题
        assertEquals("Spring Boot 事务传播", detail.getEffectiveTitle());

        PostSeo stored = postSeoMapper.selectById(POST_ID);
        assertNotNull(stored);
        assertEquals("llm", stored.getSource());
        assertNotNull(stored.getGeneratedAt());
        assertEquals(1, server.callCount());
        assertTrue(server.bodies().get(0).contains("事务传播"));
    }

    @Test
    @DisplayName("generate_MalformedJson_RetriesOnceThenFails")
    void generate_MalformedJson_RetriesOnceThenFails() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("这不是 JSON"));
        configureLlm(server.baseUrl(), false);

        BusinessException error = assertThrows(BusinessException.class, () -> generationService.generate(POST_ID));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertEquals(2, server.callCount(), "解析失败必须只重试一次（共 2 次调用）");
        // 第二次请求要带上「上一次的输出」和修复指令
        assertTrue(server.bodies().get(1).contains("这不是 JSON"));
        assertNull(postSeoMapper.selectById(POST_ID), "失败时不能留下半成品 SEO 行");
    }

    @Test
    @DisplayName("generate_FirstBadThenRepairSucceeds")
    void generate_FirstBadThenRepairSucceeds() throws Exception {
        server = new StubLlmServer((index, body) ->
                index == 0 ? StubLlmServer.Reply.ok("{\"title\":\"太长的标题" + "标".repeat(70) + "\"}")
                        : StubLlmServer.Reply.ok(seoJson("修复后的标题")));
        configureLlm(server.baseUrl(), false);

        SeoDetailVO detail = generationService.generate(POST_ID);

        assertEquals("修复后的标题", detail.getSeoTitle());
        assertEquals(2, server.callCount());
    }

    @Test
    @DisplayName("generate_ResponseFormatRejected_RetriesWithoutIt")
    void generate_ResponseFormatRejected_RetriesWithoutIt() throws Exception {
        server = new StubLlmServer((index, body) ->
                index == 0
                        ? StubLlmServer.Reply.status(400, "{\"error\":\"unsupported response_format\"}")
                        : StubLlmServer.Reply.ok(seoJson("兼容标题")));
        configureLlm(server.baseUrl(), false);

        SeoDetailVO detail = generationService.generate(POST_ID);

        assertEquals("兼容标题", detail.getSeoTitle());
        assertEquals(2, server.callCount());
        assertFalse(server.bodies().get(1).contains("response_format"));
    }

    @Test
    @DisplayName("generate_Unauthorized_Throws502WithoutLeakingKey")
    void generate_Unauthorized_Throws502WithoutLeakingKey() throws Exception {
        server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(401, "{\"error\":\"bad key " + API_KEY + "\"}"));
        configureLlm(server.baseUrl(), false);

        BusinessException error = assertThrows(BusinessException.class, () -> generationService.generate(POST_ID));

        assertEquals(502, error.getCode());
        assertEquals("模型鉴权失败，请检查 API Key 是否有效", error.getMessage());
        assertFalse(error.getMessage().contains(API_KEY));
        assertFalse(error.getMessage().contains("bad key"));
    }

    @Test
    @DisplayName("generate_Timeout_ThrowsGenericFailure")
    void generate_Timeout_ThrowsGenericFailure() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.slow(3000, seoJson("迟到的标题")));
        settings.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", server.baseUrl(),
                "llmApiKey", API_KEY,
                "llmModel", "gpt-4o-mini",
                "llmTimeoutSeconds", "5",
                "llmAutoOnPublish", "false"));
        // 直接把超时压到 5 秒下限之外不可行（校验是 5..120），因此用 3 秒延迟 + 5 秒超时会成功；
        // 这里改成让上游永不返回内容 -> 用一个超出客户端超时的方式：把延迟设得比 timeout 更久。
        server.close();
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.slow(7000, seoJson("迟到的标题")));
        settings.updateSettings(Map.of("llmBaseUrl", server.baseUrl(), "llmTimeoutSeconds", "5"));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class, () -> generationService.generate(POST_ID));

        assertEquals(502, error.getCode());
        assertEquals("模型响应超时，请稍后重试", error.getMessage());
    }

    @Test
    @DisplayName("generate_NotConfigured_Throws400WithSetupHint")
    void generate_NotConfigured_Throws400WithSetupHint() {
        settings.updateSettings(Map.of("llmEnabled", "true", "llmBaseUrl", "", "llmApiKey", ""));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class, () -> generationService.generate(POST_ID));

        assertEquals(400, error.getCode());
        assertEquals("尚未配置模型，请先在设置中填写接口地址与 API Key", error.getMessage());
    }

    @Test
    @DisplayName("generate_Disabled_Throws400")
    void generate_Disabled_Throws400() {
        settings.updateSettings(Map.of("llmEnabled", "false", "llmBaseUrl", "http://127.0.0.1:9/v1",
                "llmApiKey", API_KEY));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class, () -> generationService.generate(POST_ID));

        assertEquals(400, error.getCode());
        assertEquals("AI 辅助功能已关闭，请先在设置中启用", error.getMessage());
    }

    @Test
    @DisplayName("generate_MissingPost_Throws404")
    void generate_MissingPost_Throws404() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok(seoJson("标题")));
        configureLlm(server.baseUrl(), false);

        BusinessException error = assertThrows(BusinessException.class, () -> generationService.generate(999_999_999L));

        assertEquals(404, error.getCode());
        assertEquals(0, server.callCount(), "文章不存在时不该浪费一次模型调用");
    }

    @Test
    @DisplayName("generate_DraftPost_StillGenerates")
    void generate_DraftPost_StillGenerates() throws Exception {
        // 手动点「生成」不要求已发布：作者常先写好 SEO 再发布。
        // 草稿只有作者本人可见，所以这里补一个作者身份（后台请求本来就是已登录管理员）。
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok(seoJson("草稿标题")));
        configureLlm(server.baseUrl(), false);
        authenticateAsAuthor();

        SeoDetailVO detail = generationService.generate(DRAFT_POST_ID);

        assertEquals("草稿标题", detail.getSeoTitle());
        assertEquals("llm", detail.getSource());
    }

    private void authenticateAsAuthor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserContext(1L, "admin"), null, Collections.emptyList()));
    }

    @Test
    @DisplayName("batch_OneFailure_OthersStillSucceed")
    void batch_OneFailure_OthersStillSucceed() throws Exception {
        long secondId = 880003L;
        jdbc.update("DELETE FROM blog_post WHERE id = ?", secondId);
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-15 10:00:00', TIMESTAMP '2026-10-15 10:00:00', 0)",
                secondId, "第二篇文章", "摘要", "正文内容", "后端", null);
        // 第一篇文章让上游 401，第二篇正常
        server = new StubLlmServer((index, body) -> index == 0
                ? StubLlmServer.Reply.status(401, "{\"error\":\"nope\"}")
                : StubLlmServer.Reply.ok(seoJson("第二篇标题")));
        configureLlm(server.baseUrl(), false);
        SeoBatchRequest request = new SeoBatchRequest();
        request.setPostIds(List.of(POST_ID, secondId));

        SeoBatchResultVO result = generationService.batch(request);

        assertEquals(2, result.getRequested());
        assertEquals(1, result.getSucceeded());
        assertEquals(1, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertEquals(1, result.getFailures().size());
        assertEquals(POST_ID, result.getFailures().get(0).getPostId());
        assertFalse(result.getFailures().get(0).getMessage().contains(API_KEY));
        assertNotNull(postSeoMapper.selectById(secondId));
        assertNull(postSeoMapper.selectById(POST_ID));
        jdbc.update("DELETE FROM post_seo WHERE post_id = ?", secondId);
        jdbc.update("DELETE FROM blog_post WHERE id = ?", secondId);
    }

    @Test
    @DisplayName("batch_SkipsMissingAndDrafts")
    void batch_SkipsMissingAndDrafts() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok(seoJson("标题")));
        configureLlm(server.baseUrl(), false);
        SeoBatchRequest request = new SeoBatchRequest();
        request.setPostIds(List.of(POST_ID, DRAFT_POST_ID, 999_999_998L));

        SeoBatchResultVO result = generationService.batch(request);

        assertEquals(1, result.getRequested());
        assertEquals(1, result.getSucceeded());
        assertEquals(2, result.getSkipped());
        assertEquals(0, result.getFailed());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    @DisplayName("batch_OnlyMissing_SkipsPostsThatAlreadyHaveSeo")
    void batch_OnlyMissing_SkipsPostsThatAlreadyHaveSeo() throws Exception {
        SeoUpdateRequest manual = new SeoUpdateRequest();
        manual.setSeoTitle("手工标题");
        manual.setSeoDescription(description(150));
        metadataService.save(POST_ID, manual);

        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok(seoJson("模型标题")));
        configureLlm(server.baseUrl(), false);
        SeoBatchRequest request = new SeoBatchRequest();
        request.setOnlyMissing(true);
        request.setLimit(10);

        SeoBatchResultVO result = generationService.batch(request);

        assertEquals(0, result.getRequested());
        assertEquals(0, server.callCount(), "已有 SEO 的文章不该再花一次模型调用");
        assertEquals("手工标题", postSeoMapper.selectById(POST_ID).getSeoTitle());
    }

    @Test
    @DisplayName("batch_ExplicitPostIds_GeneratesAll")
    void batch_ExplicitPostIds_GeneratesAll() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok(seoJson("批量标题")));
        configureLlm(server.baseUrl(), false);
        SeoBatchRequest request = new SeoBatchRequest();
        request.setPostIds(List.of(POST_ID));

        SeoBatchResultVO result = generationService.batch(request);

        assertEquals(1, result.getRequested());
        assertEquals(1, result.getSucceeded());
        assertEquals("llm", postSeoMapper.selectById(POST_ID).getSource());
    }

    @Test
    @DisplayName("testConnection_Ok")
    void testConnection_Ok() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("OK"));
        configureLlm(server.baseUrl(), false);

        SeoLlmTestVO result = generationService.testConnection();

        assertTrue(result.isOk());
        assertEquals("gpt-4o-mini", result.getModel());
        assertEquals("连接成功。", result.getMessage());
        assertTrue(result.getLatencyMs() >= 0);
    }

    @Test
    @DisplayName("testConnection_FailureReturnsOkFalseAndRedactsKey")
    void testConnection_FailureReturnsOkFalseAndRedactsKey() throws Exception {
        server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(401, "{\"error\":\"bad key " + API_KEY + "\"}"));
        configureLlm(server.baseUrl(), false);

        SeoLlmTestVO result = generationService.testConnection();

        assertFalse(result.isOk());
        assertEquals("模型鉴权失败，请检查 API Key 是否有效", result.getMessage());
        assertFalse(result.getMessage().contains(API_KEY));
        assertEquals("gpt-4o-mini", result.getModel());
    }

    @Test
    @DisplayName("testConnection_NotConfigured")
    void testConnection_NotConfigured() {
        settings.updateSettings(Map.of("llmBaseUrl", "", "llmApiKey", ""));
        clearSiteCache();

        SeoLlmTestVO result = generationService.testConnection();

        assertFalse(result.isOk());
        assertEquals("尚未配置接口地址或 API Key，请先在设置中填写。", result.getMessage());
    }

    @Test
    @DisplayName("overview_ReflectsGeneratedRow")
    void overview_ReflectsGeneratedRow() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok(seoJson("概览标题")));
        configureLlm(server.baseUrl(), true);

        generationService.generate(POST_ID);
        SeoOverviewVO overview = auditService.overview();

        assertTrue(overview.isLlmConfigured());
        assertTrue(overview.isLlmEnabled());
        assertTrue(overview.isAutoOnPublish());
        assertEquals("gpt-4o-mini", overview.getModel());
        assertTrue(overview.getTotalPosts() >= 1);
        assertTrue(overview.getWithSeo() >= 1);
        assertNotNull(overview.getCoveragePercent());
    }

    @Test
    @DisplayName("generateQuietly_SwallowsEverything")
    void generateQuietly_SwallowsEverything() throws Exception {
        server = new StubLlmServer((index, body) -> StubLlmServer.Reply.status(500, "boom"));
        configureLlm(server.baseUrl(), true);

        // 不抛异常就是通过：自动模式绝不能让发布流程感知失败
        generationService.generateQuietly(POST_ID);
        generationService.generateQuietly(null);
        generationService.generateQuietly(999_999_997L);
    }
}
