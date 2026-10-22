package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.ai.AiProofreadBatchVO;
import com.selfblog.vo.ai.AiProofreadVO;
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
 * D 校对的集成测试：单篇与批量。
 *
 * <p>重点是「只读、不改文章」、正文超 6000 字要置 {@code truncated}、
 * 以及批量里一篇失败不能拖垮其它篇。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AiProofreadService")
class AiProofreadServiceTest {

    private static final long POST_ID = 883001L;
    private static final long POST_ID_2 = 883002L;
    private static final String API_KEY = "sk-ai-proofread-secret-abcdefgh";

    @Autowired
    private AiProofreadService proofreadService;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    private AiStubLlmServer server;

    private static final String ONE_ISSUE = "{\"issues\":[{\"type\":\"typo\",\"excerpt\":\"帐号\","
            + "\"suggestion\":\"账号\",\"severity\":\"low\"}]}";

    @BeforeEach
    void setUp() {
        clearSiteCache();
        jdbc.update("DELETE FROM blog_post WHERE id IN (?, ?)", POST_ID, POST_ID_2);
        insertPost(POST_ID, "Spring Boot 事务传播行为详解", "请先登录你的帐号再重试。");
        insertPost(POST_ID_2, "第二篇文章", "这篇内容没有问题。");
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

    private void insertPost(long id, String title, String content) {
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-12 08:00:00', TIMESTAMP '2026-10-12 08:00:00', 0)",
                id, title, "摘要", content, "后端");
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

    @Test
    @DisplayName("proofread_ReturnsIssuesAndDoesNotModifyPost")
    void proofread_ReturnsIssuesAndDoesNotModifyPost() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ISSUE));
        configureLlm(server.baseUrl());

        AiProofreadVO vo = proofreadService.proofread(POST_ID);

        assertEquals(POST_ID, vo.getPostId());
        assertEquals(1, vo.getIssues().size());
        AiProofreadVO.Issue issue = vo.getIssues().get(0);
        assertEquals("typo", issue.getType());
        assertEquals("帐号", issue.getExcerpt());
        assertEquals("账号", issue.getSuggestion());
        assertEquals("low", issue.getSeverity());
        assertFalse(vo.getTruncated());
        assertEquals("deepseek-flash", vo.getModel());

        String content = jdbc.queryForObject("SELECT content FROM blog_post WHERE id = ?", String.class, POST_ID);
        assertTrue(content.contains("帐号"), "校对只读，绝不能改文章正文");
    }

    @Test
    @DisplayName("proofread_EmptyIssues_IsValid")
    void proofread_EmptyIssues_IsValid() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"issues\":[]}"));
        configureLlm(server.baseUrl());

        AiProofreadVO vo = proofreadService.proofread(POST_ID);

        assertTrue(vo.getIssues().isEmpty(), "没有问题时返回空数组，不是错误");
        assertEquals(1, server.callCount(), "空数组合法，不该触发修复重试");
    }

    @Test
    @DisplayName("proofread_LongBody_SetsTruncated")
    void proofread_LongBody_SetsTruncated() throws Exception {
        // setUp 已插入同 id 的文章，这里只改正文（再 INSERT 会撞主键）
        jdbc.update("UPDATE blog_post SET title = ?, content = ? WHERE id = ?",
                "超长文章", "字".repeat(6500), POST_ID);
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok("{\"issues\":[]}"));
        configureLlm(server.baseUrl());

        AiProofreadVO vo = proofreadService.proofread(POST_ID);

        assertTrue(vo.getTruncated(), "正文超过 6000 字必须标记 truncated");
        String prompt = server.bodies().get(0);
        assertTrue(prompt.length() < 20000, "截断后提示词不应无限膨胀");
    }

    @Test
    @DisplayName("proofread_IllegalTypeAndSeverity_NormalizedNotDropped")
    void proofread_IllegalTypeAndSeverity_NormalizedNotDropped() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"issues\":[{\"type\":\"style\",\"excerpt\":\"帐号\",\"suggestion\":\"账号\","
                        + "\"severity\":\"critical\"}]}"));
        configureLlm(server.baseUrl());

        AiProofreadVO.Issue issue = proofreadService.proofread(POST_ID).getIssues().get(0);

        assertEquals("wording", issue.getType(), "非法类型归到 wording");
        assertEquals("low", issue.getSeverity(), "非法严重程度归到 low");
    }

    @Test
    @DisplayName("proofread_MissingExcerptOrSuggestion_EntrySkipped")
    void proofread_MissingExcerptOrSuggestion_EntrySkipped() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"issues\":[{\"type\":\"typo\",\"excerpt\":\"帐号\"},"
                        + "{\"type\":\"typo\",\"excerpt\":\"帐号\",\"suggestion\":\"账号\"}]}"));
        configureLlm(server.baseUrl());

        assertEquals(1, proofreadService.proofread(POST_ID).getIssues().size());
    }

    @Test
    @DisplayName("proofread_OverlongFields_TruncatedToContractLimits")
    void proofread_OverlongFields_TruncatedToContractLimits() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"issues\":[{\"type\":\"typo\",\"excerpt\":\"" + "摘".repeat(300) + "\","
                        + "\"suggestion\":\"" + "改".repeat(400) + "\",\"severity\":\"high\"}]}"));
        configureLlm(server.baseUrl());

        AiProofreadVO.Issue issue = proofreadService.proofread(POST_ID).getIssues().get(0);

        assertEquals(AiProofreadService.EXCERPT_MAX, issue.getExcerpt().length());
        assertEquals(AiProofreadService.SUGGESTION_MAX, issue.getSuggestion().length());
    }

    @Test
    @DisplayName("proofread_MoreThanTwentyIssues_CappedAt20")
    void proofread_MoreThanTwentyIssues_CappedAt20() throws Exception {
        StringBuilder sb = new StringBuilder("{\"issues\":[");
        for (int i = 0; i < 30; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"type\":\"typo\",\"excerpt\":\"片").append(i)
                    .append("\",\"suggestion\":\"改\",\"severity\":\"low\"}");
        }
        sb.append("]}");
        String payload = sb.toString();
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(payload));
        configureLlm(server.baseUrl());

        assertEquals(AiProofreadService.ISSUES_MAX,
                proofreadService.proofread(POST_ID).getIssues().size());
    }

    @Test
    @DisplayName("proofread_MissingPost_Throws404")
    void proofread_MissingPost_Throws404() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ISSUE));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> proofreadService.proofread(99999999L));

        assertEquals(404, error.getCode());
        assertEquals("文章不存在", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("proofreadBatch_IsolatesPerPostFailure")
    void proofreadBatch_IsolatesPerPostFailure() throws Exception {
        // 第一篇输出不合规（重试两次都失败），第二篇正常
        server = new AiStubLlmServer((index, body) -> {
            if (index <= 1) {
                return AiStubLlmServer.Reply.ok("{\"issues\":\"不是数组\"}");
            }
            return AiStubLlmServer.Reply.ok(ONE_ISSUE);
        });
        configureLlm(server.baseUrl());

        AiProofreadBatchVO vo = proofreadService.proofreadBatch(
                List.of(POST_ID, POST_ID_2), 5);

        assertEquals(2, vo.getRequested());
        assertEquals(1, vo.getSucceeded());
        assertEquals(1, vo.getFailed());
        assertEquals(POST_ID, vo.getFailures().get(0).getPostId());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写",
                vo.getFailures().get(0).getReason());
        assertEquals(POST_ID_2, vo.getResults().get(0).getPostId());
    }

    @Test
    @DisplayName("proofreadBatch_DeduplicatesAndTruncatesByIdLimit")
    void proofreadBatch_DeduplicatesAndTruncatesByIdLimit() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ISSUE));
        configureLlm(server.baseUrl());

        AiProofreadBatchVO vo = proofreadService.proofreadBatch(
                List.of(POST_ID, POST_ID, POST_ID_2), 1);

        assertEquals(1, vo.getRequested(), "去重后按 limit 截断");
        assertEquals(1, vo.getSucceeded());
    }

    @Test
    @DisplayName("proofreadBatch_NoPostIds_UsesRecentPublished")
    void proofreadBatch_NoPostIds_UsesRecentPublished() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ISSUE));
        configureLlm(server.baseUrl());

        AiProofreadBatchVO vo = proofreadService.proofreadBatch(null, 5);

        assertTrue(vo.getRequested() >= 1);
        assertEquals(vo.getRequested(), vo.getSucceeded() + vo.getFailed());
    }

    @Test
    @DisplayName("proofreadBatch_NoPublishedPosts_Throws400")
    void proofreadBatch_NoPublishedPosts_Throws400() throws Exception {
        jdbc.update("UPDATE blog_post SET status = 0");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ISSUE));
        configureLlm(server.baseUrl());

        BusinessException error = assertThrows(BusinessException.class,
                () -> proofreadService.proofreadBatch(null, 5));

        assertEquals(400, error.getCode());
        assertEquals("还没有已发布的文章", error.getMessage());
    }

    @Test
    @DisplayName("normalizeBatchLimit_ClampsToDefaults")
    void normalizeBatchLimit_ClampsToDefaults() {
        assertEquals(5, AiProofreadService.normalizeBatchLimit(null));
        assertEquals(5, AiProofreadService.normalizeBatchLimit(0));
        assertEquals(10, AiProofreadService.normalizeBatchLimit(99), "上限 10");
        assertEquals(3, AiProofreadService.normalizeBatchLimit(3));
    }
}
