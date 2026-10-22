package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.ai.AiSummaryVO;
import com.selfblog.vo.ai.AiTagsVO;
import com.selfblog.vo.ai.AiTitlesVO;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B 写作辅助的集成测试：真实 H2 + 真实本地 HTTP 上游（{@link AiStubLlmServer}）。
 *
 * <p>重点覆盖契约里那些「错了会静默」的约束：摘要长度区间、标签只能出自已有池、
 * 池外值必须丢弃、全丢弃报 502、池空报 400、标题恰好 3 个且互不相同。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AiWritingService")
class AiWritingServiceTest {

    private static final long POST_ID = 881001L;
    private static final String API_KEY = "sk-ai-writing-secret-abcdefgh";

    @Autowired
    private AiWritingService writingService;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    private AiStubLlmServer server;

    private static String summary(int length) {
        return "摘".repeat(length);
    }

    @BeforeEach
    void setUp() {
        clearSiteCache();
        jdbc.update("DELETE FROM blog_post_tag WHERE post_id = ?", POST_ID);
        jdbc.update("DELETE FROM blog_post WHERE id = ?", POST_ID);
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-15 10:00:00', TIMESTAMP '2026-10-15 10:00:00', 0)",
                POST_ID, "Spring Boot 事务传播行为详解", "旧摘要",
                "REQUIRED 是默认行为，REQUIRES_NEW 会挂起外层事务。", "后端", "/uploads/cover.jpg");
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

    private void configureLlm(String baseUrl) {
        settings.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", baseUrl,
                "llmApiKey", API_KEY,
                "llmModel", "deepseek-flash",
                "llmTimeoutSeconds", "10",
                "llmAutoOnPublish", "false"));
        clearSiteCache();
    }

    /** 造一个干净的标签池：先清空关联与标签，再插入指定名字的标签。 */
    private void seedTags(String... names) {
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_tag");
        long id = 881100L;
        for (String name : names) {
            jdbc.update("INSERT INTO blog_tag (id, name, is_deleted) VALUES (?, ?, 0)", id++, name);
        }
    }

    @Test
    @DisplayName("summary_HappyPath_ReturnsPlainTextInRange")
    void summary_HappyPath_ReturnsPlainTextInRange() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"summary\":\"" + summary(140) + "\"}"));
        configureLlm(server.baseUrl());

        AiSummaryVO vo = writingService.summary(POST_ID);

        assertEquals(140, vo.getSummary().length());
        assertEquals("deepseek-flash", vo.getModel());
        assertTrue(vo.getLatencyMs() >= 0);
        assertEquals(1, server.callCount());
        assertTrue(server.bodies().get(0).contains("事务传播"), "提示词要带上正文");
    }

    @Test
    @DisplayName("summary_TooShort_RepairsOnceThenSucceeds")
    void summary_TooShort_RepairsOnceThenSucceeds() throws Exception {
        server = new AiStubLlmServer((index, body) -> index == 0
                ? AiStubLlmServer.Reply.ok("{\"summary\":\"太短\"}")
                : AiStubLlmServer.Reply.ok("{\"summary\":\"" + summary(120) + "\"}"));
        configureLlm(server.baseUrl());

        AiSummaryVO vo = writingService.summary(POST_ID);

        assertEquals(120, vo.getSummary().length());
        assertEquals(2, server.callCount());
        assertTrue(server.bodies().get(1).contains("太短"), "修复请求要带上上一次的输出");
    }

    @Test
    @DisplayName("summary_Overlong_TruncatedTo200")
    void summary_Overlong_TruncatedTo200() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"summary\":\"" + summary(500) + "\"}"));
        configureLlm(server.baseUrl());

        assertEquals(200, writingService.summary(POST_ID).getSummary().length());
    }

    @Test
    @DisplayName("summary_MarkdownStrippedToPlainText")
    void summary_MarkdownStrippedToPlainText() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "```json\n{\"summary\":\"## 标题\\n\\n**加粗** " + summary(100) + "\"}\n```"));
        configureLlm(server.baseUrl());

        String summary = writingService.summary(POST_ID).getSummary();

        assertFalse(summary.contains("**"));
        assertFalse(summary.contains("##"));
        assertFalse(summary.contains("\n"));
    }

    @Test
    @DisplayName("tags_OnlyPicksFromPool_DropsUnknown")
    void tags_OnlyPicksFromPool_DropsUnknown() throws Exception {
        seedTags("Spring Boot", "事务", "数据库", "缓存");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"tags\":[\"Spring Boot\",\"事务\",\"这个标签不存在\",\"缓存\"]}"));
        configureLlm(server.baseUrl());

        AiTagsVO vo = writingService.tags(POST_ID);

        assertEquals(List.of("Spring Boot", "事务", "缓存"), vo.getTags());
        assertTrue(server.bodies().get(0).contains("可选标签"));
    }

    @Test
    @DisplayName("tags_AllOutOfPool_Throws502")
    void tags_AllOutOfPool_Throws502() throws Exception {
        seedTags("Spring Boot", "事务", "数据库");
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"tags\":[\"胡编的标签一\",\"胡编的标签二\"]}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> writingService.tags(POST_ID));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertEquals(2, server.callCount(), "结构不合规只重试一次");
    }

    @Test
    @DisplayName("tags_EmptyPool_Throws400BeforeCallingModel")
    void tags_EmptyPool_Throws400BeforeCallingModel() throws Exception {
        seedTags();
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"tags\":[]}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> writingService.tags(POST_ID));

        assertEquals(400, error.getCode());
        assertEquals("还没有任何标签，请先创建标签", error.getMessage());
        assertEquals(0, server.callCount(), "池空时不该白花一次上游调用");
    }

    @Test
    @DisplayName("tags_PoolOrderedByUsageFrequency")
    void tags_PoolOrderedByUsageFrequency() throws Exception {
        seedTags("低频", "高频", "中频");
        // 高频 3 次、中频 1 次、低频 0 次
        jdbc.update("INSERT INTO blog_post_tag (post_id, tag_id) SELECT ?, id FROM blog_tag WHERE name = ?",
                POST_ID, "高频");
        jdbc.update("INSERT INTO blog_post_tag (post_id, tag_id) SELECT ?, id FROM blog_tag WHERE name = ?",
                POST_ID + 1, "高频");
        jdbc.update("INSERT INTO blog_post_tag (post_id, tag_id) SELECT ?, id FROM blog_tag WHERE name = ?",
                POST_ID + 2, "高频");
        jdbc.update("INSERT INTO blog_post_tag (post_id, tag_id) SELECT ?, id FROM blog_tag WHERE name = ?",
                POST_ID, "中频");
        // 池里只有 3 个标签，契约要求至少 3 个，所以模型必须把三个都选上
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"tags\":[\"高频\",\"中频\",\"低频\"]}"));
        configureLlm(server.baseUrl());

        writingService.tags(POST_ID);

        String prompt = server.bodies().get(0);
        int high = prompt.indexOf("高频");
        int middle = prompt.indexOf("中频");
        int low = prompt.indexOf("低频");
        assertTrue(high > 0 && middle > 0 && low > 0, "池里三个标签都要出现在提示词里");
        assertTrue(high < middle && middle < low, "池必须按使用频次降序排列");
    }

    @Test
    @DisplayName("titles_HappyPath_ReturnsExactlyThree")
    void titles_HappyPath_ReturnsExactlyThree() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"titles\":[\"候选一\",\"候选二\",\"候选三\",\"多余的第四个\"]}"));
        configureLlm(server.baseUrl());

        AiTitlesVO vo = writingService.titles(POST_ID);

        assertEquals(List.of("候选一", "候选二", "候选三"), vo.getTitles());
    }

    @Test
    @DisplayName("titles_TooFewOrDuplicated_RepairsThenFails")
    void titles_TooFewOrDuplicated_RepairsThenFails() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"titles\":[\"同一个标题\",\"同一个标题\"]}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> writingService.titles(POST_ID));

        assertEquals(502, error.getCode());
        assertEquals(2, server.callCount());
    }

    @Test
    @DisplayName("titles_TooLong_RepairsThenFails")
    void titles_TooLong_RepairsThenFails() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"titles\":[\"" + "长".repeat(70) + "\",\"候选二\",\"候选三\"]}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class, () -> writingService.titles(POST_ID));

        assertEquals(502, error.getCode());
    }

    @Test
    @DisplayName("postMissing_Throws404")
    void postMissing_Throws404() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"summary\":\"x\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.summary(99999999L));

        assertEquals(404, error.getCode());
        assertEquals("文章不存在", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("disabled_Throws400BeforeTouchingModel")
    void disabled_Throws400BeforeTouchingModel() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"summary\":\"x\"}"));
        settings.updateSettings(Map.of("llmEnabled", "false"));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.summary(POST_ID));

        assertEquals(400, error.getCode());
        assertEquals("AI 辅助功能已关闭，请先在设置中启用", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("notConfigured_Throws400WithSetupHint")
    void notConfigured_Throws400WithSetupHint() throws Exception {
        settings.updateSettings(Map.of("llmEnabled", "true", "llmBaseUrl", "", "llmApiKey", ""));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.summary(POST_ID));

        assertEquals(400, error.getCode());
        assertEquals("尚未配置可用的模型接口，请先在设置中填写", error.getMessage());
    }

    @Test
    @DisplayName("upstreamUnauthorized_Throws502WithoutLeakingKey")
    void upstreamUnauthorized_Throws502WithoutLeakingKey() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.status(401, "{\"error\":\"bad key " + API_KEY + "\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.summary(POST_ID));

        assertEquals(502, error.getCode());
        assertEquals("模型鉴权失败，请检查 API Key 是否有效", error.getMessage());
        assertFalse(error.getMessage().contains(API_KEY));
    }
}
