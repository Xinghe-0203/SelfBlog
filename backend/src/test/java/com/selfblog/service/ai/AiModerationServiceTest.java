package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.ai.AiModerationScanVO;
import com.selfblog.vo.ai.AiModerationVO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C 评论审核的集成测试。
 *
 * <p>覆盖三条链路：单条判定、批量扫描（只扫已公开、只列有问题的）、以及留言落库后的
 * 自动审核（结论不是 pass 才改回待审，且任何异常都不外抛）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AiModerationService")
class AiModerationServiceTest {

    private static final long POST_ID = 882001L;
    private static final long COMMENT_ID = 882101L;
    private static final long SITE_COMMENT_ID = 882102L;
    private static final String API_KEY = "sk-ai-moderation-secret-abcdefgh";

    @Autowired
    private AiModerationService moderationService;
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
        jdbc.update("DELETE FROM guest_comment WHERE post_id = ?", POST_ID);
        jdbc.update("DELETE FROM blog_comment WHERE post_id = ?", POST_ID);
        jdbc.update("DELETE FROM blog_post WHERE id = ?", POST_ID);
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-10 09:00:00', TIMESTAMP '2026-10-10 09:00:00', 0)",
                POST_ID, "Spring Boot 事务传播行为详解", "摘要", "正文", "后端");
        insertComment(COMMENT_ID, "张三", "加微信 abc123 领资料", 1);
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
                "llmTimeoutSeconds", "10"));
        clearSiteCache();
    }

    private void insertComment(long id, String nickname, String content, int status) {
        jdbc.update("INSERT INTO guest_comment (id, post_id, visitor_hash, nickname, content, status, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, TIMESTAMP '2026-10-20 12:00:00')",
                id, POST_ID, "visitor-hash-" + id, nickname, content, status);
    }

    private int statusOf(long commentId) {
        Integer status = jdbc.queryForObject(
                "SELECT status FROM guest_comment WHERE id = ?", Integer.class, commentId);
        return status == null ? -1 : status;
    }

    @Test
    @DisplayName("check_Pass_ReturnsVerdictWithoutCategories")
    void check_Pass_ReturnsVerdictWithoutCategories() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"verdict\":\"pass\",\"reason\":\"正常提问\",\"categories\":[]}"));
        configureLlm(server.baseUrl());

        AiModerationVO vo = moderationService.check(COMMENT_ID, "guest");

        assertEquals("pass", vo.getVerdict());
        assertEquals("正常提问", vo.getReason());
        assertTrue(vo.getCategories().isEmpty());
        assertEquals("deepseek-flash", vo.getModel());
        assertTrue(server.bodies().get(0).contains("加微信"), "提示词要带上留言正文");
    }

    @Test
    @DisplayName("check_Block_KeepsOnlyAllowedCategories")
    void check_Block_KeepsOnlyAllowedCategories() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"verdict\":\"block\",\"reason\":\"广告引流\","
                        + "\"categories\":[\"ad\",\"spam\",\"不存在的分类\"]}"));
        configureLlm(server.baseUrl());

        AiModerationVO vo = moderationService.check(COMMENT_ID, null);

        assertEquals("block", vo.getVerdict());
        assertEquals(List.of("ad", "spam"), vo.getCategories(), "只保留契约允许的分类");
    }

    @Test
    @DisplayName("check_IllegalVerdict_RepairsThenFails502")
    void check_IllegalVerdict_RepairsThenFails502() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"verdict\":\"delete\",\"reason\":\"随便\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> moderationService.check(COMMENT_ID, "guest"));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertEquals(2, server.callCount());
    }

    @Test
    @DisplayName("check_MissingComment_Throws404")
    void check_MissingComment_Throws404() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"verdict\":\"pass\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> moderationService.check(99999999L, "guest"));

        assertEquals(404, error.getCode());
        assertEquals("评论不存在", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("check_SiteComment_ReadsBlogComment")
    void check_SiteComment_ReadsBlogComment() throws Exception {
        jdbc.update("INSERT INTO blog_comment (id, post_id, user_id, content, is_deleted) VALUES (?, ?, 1, ?, 0)",
                SITE_COMMENT_ID, POST_ID, "站内用户的一条评论");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"verdict\":\"flag\",\"reason\":\"需要人工确认\"}"));
        configureLlm(server.baseUrl());

        AiModerationVO vo = moderationService.check(SITE_COMMENT_ID, "site");

        assertEquals("flag", vo.getVerdict());
        assertTrue(server.bodies().get(0).contains("站内用户的一条评论"));
    }

    @Test
    @DisplayName("check_IllegalKind_Throws400")
    void check_IllegalKind_Throws400() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"verdict\":\"pass\"}"));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> moderationService.check(COMMENT_ID, "circle"));

        assertEquals(400, error.getCode());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("scan_OnlyPublicComments_ListsFlaggedOnly")
    void scan_OnlyPublicComments_ListsFlaggedOnly() throws Exception {
        insertComment(COMMENT_ID + 1, "李四", "写得很清楚，谢谢", 1);
        insertComment(COMMENT_ID + 2, "王五", "私信我代写论文", 1);
        insertComment(COMMENT_ID + 3, "赵六", "广告但已被隐藏", 0);
        server = new AiStubLlmServer((index, body) -> {
            String verdict = body.contains("代写") || body.contains("加微信") ? "block" : "pass";
            return AiStubLlmServer.Reply.ok("{\"verdict\":\"" + verdict + "\",\"reason\":\"判定\"}");
        });
        configureLlm(server.baseUrl());

        AiModerationScanVO vo = moderationService.scan(10);

        assertEquals(3, vo.getScanned(), "只扫 status=1 的三条（含最初的广告留言）");
        assertEquals(2, vo.getFlagged(), "广告留言与代写论文都该被标出");
        assertEquals(2, vo.getItems().size(), "items 只含有问题的");
        assertTrue(vo.getItems().stream().allMatch(item -> !"pass".equals(item.getVerdict())));
        assertTrue(vo.getItems().stream().anyMatch(item -> item.getExcerpt().contains("代写")));
        assertTrue(vo.getItems().stream().noneMatch(item -> "赵六".equals(item.getNickname())),
                "未公开的留言不该被扫到");
    }

    @Test
    @DisplayName("scan_ItemExcerptTruncatedTo120")
    void scan_ItemExcerptTruncatedTo120() throws Exception {
        insertComment(COMMENT_ID + 1, "长留言", "字".repeat(500), 1);
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"verdict\":\"flag\",\"reason\":\"过长\"}"));
        configureLlm(server.baseUrl());

        AiModerationScanVO vo = moderationService.scan(5);

        AiModerationScanVO.Item item = vo.getItems().get(0);
        assertEquals(AiModerationService.EXCERPT_MAX, item.getExcerpt().length());
    }

    @Test
    @DisplayName("scan_PartialFailure_SkipsBadComment")
    void scan_PartialFailure_SkipsBadComment() throws Exception {
        insertComment(COMMENT_ID + 1, "正常用户", "写得很清楚，谢谢", 1);
        // 最新的一条（id 更大）返回非法 verdict → 重试后仍失败，应跳过；另一条正常
        server = new AiStubLlmServer((index, body) -> {
            if (body.contains("写得很清楚")) {
                return AiStubLlmServer.Reply.ok("{\"verdict\":\"pass\",\"reason\":\"正常\"}");
            }
            return AiStubLlmServer.Reply.ok("{\"verdict\":\"???\"}");
        });
        configureLlm(server.baseUrl());

        AiModerationScanVO vo = moderationService.scan(10);

        assertEquals(1, vo.getScanned(), "失败的那条不计入 scanned");
        assertEquals(0, vo.getFlagged());
    }

    @Test
    @DisplayName("moderateQuietly_NonPass_SetsStatusPending")
    void moderateQuietly_NonPass_SetsStatusPending() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"verdict\":\"block\",\"reason\":\"广告\",\"categories\":[\"ad\"]}"));
        configureLlm(server.baseUrl());
        settings.updateSettings(Map.of("llmCommentModeration", "true"));
        clearSiteCache();

        moderationService.moderateQuietly(COMMENT_ID);

        assertEquals(0, statusOf(COMMENT_ID), "判定不是 pass 时要改回待审");
    }

    @Test
    @DisplayName("moderateQuietly_Pass_KeepsPublished")
    void moderateQuietly_Pass_KeepsPublished() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"verdict\":\"pass\",\"reason\":\"正常留言\"}"));
        configureLlm(server.baseUrl());
        settings.updateSettings(Map.of("llmCommentModeration", "true"));
        clearSiteCache();

        moderationService.moderateQuietly(COMMENT_ID);

        assertEquals(1, statusOf(COMMENT_ID));
    }

    @Test
    @DisplayName("moderateQuietly_SwitchOff_NeverCallsModel")
    void moderateQuietly_SwitchOff_NeverCallsModel() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"verdict\":\"block\"}"));
        configureLlm(server.baseUrl());
        settings.updateSettings(Map.of("llmCommentModeration", "false"));
        clearSiteCache();

        moderationService.moderateQuietly(COMMENT_ID);

        assertEquals(0, server.callCount());
        assertEquals(1, statusOf(COMMENT_ID));
    }

    @Test
    @DisplayName("moderateQuietly_UpstreamFailure_IsSwallowed")
    void moderateQuietly_UpstreamFailure_IsSwallowed() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.status(500, "{\"error\":\"boom\"}"));
        configureLlm(server.baseUrl());
        settings.updateSettings(Map.of("llmCommentModeration", "true"));
        clearSiteCache();

        // 不抛异常就是通过：自动审核绝不能影响留言本身
        moderationService.moderateQuietly(COMMENT_ID);

        assertEquals(1, statusOf(COMMENT_ID), "上游失败时留言保持原状");
    }

    @Test
    @DisplayName("moderateQuietly_UnknownComment_DoesNotThrow")
    void moderateQuietly_UnknownComment_DoesNotThrow() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"verdict\":\"block\"}"));
        configureLlm(server.baseUrl());
        settings.updateSettings(Map.of("llmCommentModeration", "true"));
        clearSiteCache();

        moderationService.moderateQuietly(99999999L);

        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("moderationDisabled_Throws400BeforeCallingModel")
    void moderationDisabled_Throws400BeforeCallingModel() throws Exception {
        settings.updateSettings(Map.of("llmEnabled", "false"));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class,
                () -> moderationService.check(COMMENT_ID, "guest"));

        assertEquals(400, error.getCode());
        assertEquals("AI 辅助功能已关闭，请先在设置中启用", error.getMessage());
    }

    @Test
    @DisplayName("scan_DefaultLimitIsTwenty")
    void scan_DefaultLimitIsTwenty() throws Exception {
        assertEquals(20, AiModerationService.normalizeScanLimit(null));
        assertEquals(20, AiModerationService.normalizeScanLimit(0));
        assertEquals(50, AiModerationService.normalizeScanLimit(500), "上限 50");
    }

    @Test
    @DisplayName("scan_EmptyTable_ReturnsZero")
    void scan_EmptyTable_ReturnsZero() throws Exception {
        jdbc.update("DELETE FROM guest_comment WHERE post_id = ?", POST_ID);
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"verdict\":\"pass\"}"));
        configureLlm(server.baseUrl());

        AiModerationScanVO vo = moderationService.scan(null);

        assertNotNull(vo.getItems());
        assertEquals(0, vo.getScanned());
        assertEquals(0, vo.getFlagged());
        assertEquals(0, server.callCount());
    }
}
