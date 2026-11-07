package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.ai.AiInlineWritingVO;
import com.selfblog.vo.ai.AiLinkSuggestionVO;
import com.selfblog.vo.ai.AiLinkSuggestionsVO;
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
 *
 * <p>行内写作覆盖四种处理方式的提示词、选区校验（非法 action/空白/超长都 400 且不发上游）、
 * 修复往返与 502；内链建议覆盖候选外 postId 丢弃、标题取候选回填、空候选 400、
 * 修复往返与显式空数组的合法放行。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AiWritingService")
class AiWritingServiceTest {

    private static final long POST_ID = 881001L;
    private static final long LINK_POST_A = 881201L;
    private static final long LINK_POST_B = 881202L;
    private static final long LINK_POST_C = 881203L;
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
        jdbc.update("DELETE FROM blog_post WHERE id IN (?, ?, ?)", LINK_POST_A, LINK_POST_B, LINK_POST_C);
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

    /** 造一篇给内链候选用的已发布文章；时间用远期字面量，保证排在测试库其他行前面。 */
    private void seedPublishedPost(long id, String title, String summary, String content,
                                   String updateTimestamp) {
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, '后端', '/uploads/cover.jpg', 1, "
                        + "TIMESTAMP '2026-10-15 10:00:00', TIMESTAMP '" + updateTimestamp + "', 0)",
                id, title, summary, content);
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

    // ---- 行内写作（inline）：选区润色/续写/精简 ----

    @Test
    @DisplayName("inline_Polish_HappyPath_SendsInstructionAndSelection")
    void inline_Polish_HappyPath_SendsInstructionAndSelection() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"text\":\"润色后的句子\"}"));
        configureLlm(server.baseUrl());

        AiInlineWritingVO vo = writingService.inlineWrite("polish", "这个句子有点不通顺的。");

        assertEquals("polish", vo.action());
        assertEquals("润色后的句子", vo.text());
        assertEquals("deepseek-flash", vo.model());
        assertTrue(vo.latencyMs() >= 0);
        assertEquals(1, server.callCount());
        String body = server.bodies().get(0);
        assertTrue(body.contains(AiPromptBuilder.inlineInstruction("polish")), "请求体要带上润色指令");
        assertTrue(body.contains("这个句子有点不通顺的。"), "请求体要带上所选文字");
    }

    @Test
    @DisplayName("inline_Continue_HappyPath_ActionCaseInsensitive")
    void inline_Continue_HappyPath_ActionCaseInsensitive() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"text\":\"续写出来的一段正文。\"}"));
        configureLlm(server.baseUrl());

        AiInlineWritingVO vo = writingService.inlineWrite("CONTINUE", "REQUIRED 是默认行为，");

        assertEquals("continue", vo.action(), "action 大小写不敏感，返回归一化的小写值");
        assertEquals("续写出来的一段正文。", vo.text());
        String body = server.bodies().get(0);
        assertTrue(body.contains(AiPromptBuilder.inlineInstruction("continue")), "请求体要带上续写指令");
        assertTrue(body.contains("REQUIRED 是默认行为，"));
    }

    @Test
    @DisplayName("inline_Shorten_HappyPath_SendsShortenInstruction")
    void inline_Shorten_HappyPath_SendsShortenInstruction() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"text\":\"精简后的句子\"}"));
        configureLlm(server.baseUrl());

        AiInlineWritingVO vo = writingService.inlineWrite("shorten", "一段比较啰嗦的长句，可以砍掉一半。");

        assertEquals("shorten", vo.action());
        assertEquals("精简后的句子", vo.text());
        String body = server.bodies().get(0);
        assertTrue(body.contains(AiPromptBuilder.inlineInstruction("shorten")), "请求体要带上精简指令");
        assertTrue(body.contains("一段比较啰嗦的长句，可以砍掉一半。"));
    }

    @Test
    @DisplayName("inline_Translate_HappyPath_SendsInstructionAndSelection")
    void inline_Translate_HappyPath_SendsInstructionAndSelection() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"text\":\"This is an idiomatic English sentence.\"}"));
        configureLlm(server.baseUrl());

        AiInlineWritingVO vo = writingService.inlineWrite("translate", "这是一段需要翻译的中文。");

        assertEquals("translate", vo.action());
        assertEquals("This is an idiomatic English sentence.", vo.text());
        assertEquals("deepseek-flash", vo.model());
        assertEquals(1, server.callCount());
        String body = server.bodies().get(0);
        assertTrue(body.contains(AiPromptBuilder.inlineInstruction("translate")), "请求体要带上翻译指令");
        assertTrue(body.contains("这是一段需要翻译的中文。"), "请求体要带上所选文字");
    }

    @Test
    @DisplayName("inline_Translate_ActionCaseInsensitive")
    void inline_Translate_ActionCaseInsensitive() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"text\":\"翻译成的一段自然中文。\"}"));
        configureLlm(server.baseUrl());

        AiInlineWritingVO vo = writingService.inlineWrite("TRANSLATE", "An English sentence to translate.");

        assertEquals("translate", vo.action(), "action 大小写不敏感，返回归一化的小写值");
        assertEquals("翻译成的一段自然中文。", vo.text());
        assertTrue(server.bodies().get(0).contains(AiPromptBuilder.inlineInstruction("translate")));
    }

    @Test
    @DisplayName("inline_InvalidAction_Throws400WithoutUpstreamCall")
    void inline_InvalidAction_Throws400WithoutUpstreamCall() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"text\":\"x\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.inlineWrite("expand", "随便一段文字"));

        assertEquals(400, error.getCode());
        assertEquals("处理方式仅支持 polish、continue、shorten、translate", error.getMessage());
        assertEquals(0, server.callCount(), "非法 action 不该白花一次上游调用");
    }

    @Test
    @DisplayName("inline_BlankText_Throws400WithoutUpstreamCall")
    void inline_BlankText_Throws400WithoutUpstreamCall() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"text\":\"x\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.inlineWrite("polish", "   "));

        assertEquals(400, error.getCode());
        assertEquals("请提供需要处理的文本", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("inline_Over3000Chars_Throws400WithoutUpstreamCall")
    void inline_Over3000Chars_Throws400WithoutUpstreamCall() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"text\":\"x\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.inlineWrite("polish", "字".repeat(3001)));

        assertEquals(400, error.getCode());
        assertEquals("选区内容过长，请最多选择 3000 字符", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("inline_GarbageThenValid_RepairsOnce")
    void inline_GarbageThenValid_RepairsOnce() throws Exception {
        server = new AiStubLlmServer((index, body) -> index == 0
                ? AiStubLlmServer.Reply.ok("好的，这是润色结果，仅供参考")
                : AiStubLlmServer.Reply.ok("{\"text\":\"润色后的句子\"}"));
        configureLlm(server.baseUrl());

        AiInlineWritingVO vo = writingService.inlineWrite("polish", "原句");

        assertEquals("润色后的句子", vo.text());
        assertEquals(2, server.callCount(), "垃圾输出要触发一次修复往返");
        assertTrue(server.bodies().get(1).contains("好的，这是润色结果，仅供参考"),
                "修复请求要带上上一次的原始输出");
    }

    @Test
    @DisplayName("inline_PersistentGarbage_Throws502")
    void inline_PersistentGarbage_Throws502() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("两轮都不是 JSON 的输出"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.inlineWrite("polish", "原句"));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertFalse(error.getMessage().contains(API_KEY), "错误文案绝不回显密钥");
        assertFalse(error.getMessage().contains(server.baseUrl()), "错误文案绝不回显上游地址");
        assertEquals(2, server.callCount(), "结构不合规只重试一次");
    }

    @Test
    @DisplayName("inline_BlankOutputText_RepairsThenThrows502")
    void inline_BlankOutputText_RepairsThenThrows502() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"text\":\"   \"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.inlineWrite("shorten", "原句"));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertEquals(2, server.callCount(), "空白 text 也要回给模型修一次");
    }

    // ---- 内链建议（links）----

    @Test
    @DisplayName("links_HappyPath_KeepsOnlyValidCandidates_TitleFromCandidateMap")
    void links_HappyPath_KeepsOnlyValidCandidates_TitleFromCandidateMap() throws Exception {
        seedPublishedPost(LINK_POST_A, "MyBatis Plus 批量插入实践", "批量插入的正确姿势",
                "批量插入可以减少网络往返。", "2026-12-30 10:00:00");
        // B 无摘要：候选行退回用正文开头充数
        seedPublishedPost(LINK_POST_B, "H2 与 MySQL 的差异清单", null,
                "H2 与 MySQL 在函数与隐式转换上有不少差异。", "2026-12-31 10:00:00");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"links\":["
                + "{\"postId\":" + LINK_POST_A + ",\"anchor\":\"事务传播\","
                + "\"reason\":\"讲清楚了 REQUIRED 与 REQUIRES_NEW 的差异\",\"title\":\"模型编造的标题一\"},"
                + "{\"postId\":99999999,\"anchor\":\"不存在的文章\","
                + "\"reason\":\"候选里没有这篇，必须丢弃\",\"title\":\"模型编造的标题二\"},"
                + "{\"postId\":" + LINK_POST_B + ",\"anchor\":\"索引优化\","
                + "\"reason\":\"与查询性能直接相关\",\"title\":\"模型编造的标题三\"}]}"));
        configureLlm(server.baseUrl());

        AiLinkSuggestionsVO vo = writingService.suggestLinks(POST_ID);

        assertEquals(2, vo.suggestions().size(), "候选外的 postId 必须丢弃");
        assertEquals(LINK_POST_A, vo.suggestions().get(0).targetPostId());
        assertEquals("MyBatis Plus 批量插入实践", vo.suggestions().get(0).targetTitle(),
                "标题取候选列表回填，绝不采信模型输出的 title");
        assertEquals("事务传播", vo.suggestions().get(0).anchor());
        assertEquals("讲清楚了 REQUIRED 与 REQUIRES_NEW 的差异", vo.suggestions().get(0).reason());
        assertEquals(LINK_POST_B, vo.suggestions().get(1).targetPostId());
        assertEquals("H2 与 MySQL 的差异清单", vo.suggestions().get(1).targetTitle());
        assertEquals("deepseek-flash", vo.model());
        assertTrue(vo.latencyMs() >= 0);
        String prompt = server.bodies().get(0);
        assertTrue(prompt.contains("候选文章"));
        assertTrue(prompt.contains("id=" + LINK_POST_A));
        assertTrue(prompt.contains("id=" + LINK_POST_B));
        assertTrue(prompt.contains("MyBatis Plus 批量插入实践"));
        assertTrue(prompt.contains("H2 与 MySQL 在函数与隐式转换上有不少差异"),
                "没有摘要的候选要用正文开头充数");
        assertTrue(prompt.contains("REQUIRED 是默认行为"), "提示词要带上当前文章的正文节选");
        assertTrue(prompt.indexOf("H2 与 MySQL 的差异清单") < prompt.indexOf("MyBatis Plus 批量插入实践"),
                "候选按更新时间倒序排列");
        assertEquals(1, server.callCount());
    }

    @Test
    @DisplayName("links_CappedAtFive_BlankAnchorDropped")
    void links_CappedAtFive_BlankAnchorDropped() throws Exception {
        seedPublishedPost(LINK_POST_A, "候选甲", "摘要甲", "正文甲。", "2026-12-30 10:00:00");
        seedPublishedPost(LINK_POST_B, "候选乙", "摘要乙", "正文乙。", "2026-12-29 10:00:00");
        seedPublishedPost(LINK_POST_C, "候选丙", "摘要丙", "正文丙。", "2026-12-28 10:00:00");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"links\":["
                + "{\"postId\":" + LINK_POST_C + ",\"anchor\":\"   \",\"reason\":\"空白锚文本\"},"
                + "{\"postId\":" + LINK_POST_A + ",\"anchor\":\"锚一\",\"reason\":\"理由\"},"
                + "{\"postId\":" + LINK_POST_B + ",\"anchor\":\"锚二\",\"reason\":\"理由\"},"
                + "{\"postId\":" + LINK_POST_C + ",\"anchor\":\"锚三\",\"reason\":\"理由\"},"
                + "{\"postId\":" + LINK_POST_A + ",\"anchor\":\"锚四\",\"reason\":\"理由\"},"
                + "{\"postId\":" + LINK_POST_B + ",\"anchor\":\"锚五\",\"reason\":\"理由\"},"
                + "{\"postId\":" + LINK_POST_C + ",\"anchor\":\"锚六\",\"reason\":\"理由\"}]}"));
        configureLlm(server.baseUrl());

        AiLinkSuggestionsVO vo = writingService.suggestLinks(POST_ID);

        assertEquals(5, vo.suggestions().size(), "最多 5 条");
        assertEquals(List.of(LINK_POST_A, LINK_POST_B, LINK_POST_C, LINK_POST_A, LINK_POST_B),
                vo.suggestions().stream().map(AiLinkSuggestionVO::targetPostId).toList(),
                "空白锚文本整条丢弃，其余按给出顺序取前 5");
    }

    @Test
    @DisplayName("links_EmptyLinksArray_ReturnsEmptySuggestions")
    void links_EmptyLinksArray_ReturnsEmptySuggestions() throws Exception {
        seedPublishedPost(LINK_POST_A, "候选甲", "摘要甲", "正文甲。", "2026-12-30 10:00:00");
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"links\":[]}"));
        configureLlm(server.baseUrl());

        AiLinkSuggestionsVO vo = writingService.suggestLinks(POST_ID);

        assertTrue(vo.suggestions().isEmpty(), "模型明确返回空数组是合法结果，不能报错");
        assertEquals("deepseek-flash", vo.model());
        assertEquals(1, server.callCount());
    }

    @Test
    @DisplayName("links_NoOtherPublished_Throws400BeforeCallingModel")
    void links_NoOtherPublished_Throws400BeforeCallingModel() throws Exception {
        jdbc.update("UPDATE blog_post SET status = 0 WHERE id <> ?", POST_ID);
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"links\":[]}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.suggestLinks(POST_ID));

        assertEquals(400, error.getCode());
        assertEquals("暂无其他已发布文章，无法生成内链建议", error.getMessage());
        assertEquals(0, server.callCount(), "候选为空时不该白花一次上游调用");
    }

    @Test
    @DisplayName("links_MissingPost_Throws404")
    void links_MissingPost_Throws404() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"links\":[]}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.suggestLinks(99999999L));

        assertEquals(404, error.getCode());
        assertEquals("文章不存在", error.getMessage(),
                "与摘要/标签/标题端点同一套 404 语义");
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("links_NullPostId_Throws400")
    void links_NullPostId_Throws400() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"links\":[]}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> writingService.suggestLinks(null));

        assertEquals(400, error.getCode());
        assertEquals("请指定文章", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("links_UnparseableThenRepaired")
    void links_UnparseableThenRepaired() throws Exception {
        seedPublishedPost(LINK_POST_A, "候选甲", "摘要甲", "正文甲。", "2026-12-30 10:00:00");
        server = new AiStubLlmServer((index, body) -> index == 0
                ? AiStubLlmServer.Reply.ok("抱歉，这次输出里没有 JSON 对象")
                : AiStubLlmServer.Reply.ok("{\"links\":[{\"postId\":" + LINK_POST_A
                        + ",\"anchor\":\"事务传播\",\"reason\":\"理由\"}]}"));
        configureLlm(server.baseUrl());

        AiLinkSuggestionsVO vo = writingService.suggestLinks(POST_ID);

        assertEquals(1, vo.suggestions().size());
        assertEquals(LINK_POST_A, vo.suggestions().get(0).targetPostId());
        assertEquals("事务传播", vo.suggestions().get(0).anchor());
        assertEquals(2, server.callCount(), "解析失败要触发一次修复往返");
        assertTrue(server.bodies().get(1).contains("抱歉，这次输出里没有 JSON 对象"),
                "修复请求要带上上一次的原始输出");
    }
}
