package com.selfblog.service.seo;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.PostSeo;
import com.selfblog.mapper.PostSeoMapper;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.seo.SeoDetailVO;
import com.selfblog.vo.seo.SeoOverviewVO;
import com.selfblog.vo.seo.SeoPostRowVO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 无 LLM 的 SEO 体检：概览计数、列表分页/过滤、逐篇问题标记。
 *
 * <p>所有数据都用本类专属的 88xxxx 号段，避免与其它测试类互相污染。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SeoAuditService")
class SeoAuditServiceTest {

    private static final long CLEAN_ID = 882001L;
    private static final long NO_SUMMARY_ID = 882002L;
    private static final long LONG_TITLE_ID = 882003L;
    private static final long DUP_A_ID = 882004L;
    private static final long DUP_B_ID = 882005L;
    private static final long DRAFT_ID = 882006L;
    private static final List<Long> ALL = List.of(
            CLEAN_ID, NO_SUMMARY_ID, LONG_TITLE_ID, DUP_A_ID, DUP_B_ID, DRAFT_ID);

    private static final String LONG_TITLE = "标".repeat(61);

    @Autowired
    private SeoAuditService auditService;
    @Autowired
    private PostSeoMapper postSeoMapper;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    @BeforeEach
    void setUp() {
        clearSiteCache();
        cleanUp();
        insertPost(CLEAN_ID, "干净的文章", "有摘要", "/uploads/cover.jpg", 1);
        insertPost(NO_SUMMARY_ID, "没有摘要的文章", null, null, 1);
        insertPost(LONG_TITLE_ID, LONG_TITLE, "有摘要", "/uploads/cover.jpg", 1);
        insertPost(DUP_A_ID, "重复标题", "有摘要", "/uploads/cover.jpg", 1);
        insertPost(DUP_B_ID, "重复标题", "有摘要", "/uploads/cover.jpg", 1);
        insertPost(DRAFT_ID, "草稿不参与体检", null, null, 0);
    }

    @AfterEach
    void tearDown() {
        clearSiteCache();
    }

    private void cleanUp() {
        jdbc.update("DELETE FROM post_seo WHERE post_id IN (?, ?, ?, ?, ?, ?)",
                ALL.get(0), ALL.get(1), ALL.get(2), ALL.get(3), ALL.get(4), ALL.get(5));
        jdbc.update("DELETE FROM blog_post WHERE id IN (?, ?, ?, ?, ?, ?)",
                ALL.get(0), ALL.get(1), ALL.get(2), ALL.get(3), ALL.get(4), ALL.get(5));
    }

    private void insertPost(long id, String title, String summary, String coverUrl, int status) {
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, ?, "
                        + "TIMESTAMP '2026-10-15 10:00:00', TIMESTAMP '2026-10-15 10:00:00', 0)",
                id, title, summary, "正文内容", "后端", coverUrl, status);
    }

    private void clearSiteCache() {
        var cache = caches.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private void insertSeo(long postId, String title, String description, String keywords, String image, String source) {
        PostSeo seo = new PostSeo();
        seo.setPostId(postId);
        seo.setSeoTitle(title);
        seo.setSeoDescription(description);
        seo.setSeoKeywords(keywords);
        seo.setOgImageUrl(image);
        seo.setSource(source);
        postSeoMapper.insert(seo);
    }

    private SeoPostRowVO rowFor(List<SeoPostRowVO> rows, long postId) {
        return rows.stream().filter(row -> row.getPostId() == postId).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("overview_CountsEveryIssueOnce")
    void overview_CountsEveryIssueOnce() {
        insertSeo(CLEAN_ID, "干净标题", "干净描述", "关键词", null, "manual");

        SeoOverviewVO overview = auditService.overview();

        // 体检范围是全部已发布文章，其它测试类可能也插了文章，所以这里用「至少」断言
        assertTrue(overview.getTotalPosts() >= 5, "草稿不该计入：" + overview.getTotalPosts());
        assertTrue(overview.getWithSeo() >= 1);
        assertTrue(overview.getMissingDescription() >= 1, "没有摘要也没有 SEO 描述的文章要计入");
        assertTrue(overview.getLongTitle() >= 1);
        assertTrue(overview.getDuplicateTitle() >= 2, "重复标题按文章数统计");
        assertTrue(overview.getMissingCover() >= 1);
        assertTrue(overview.getCoveragePercent() >= 0 && overview.getCoveragePercent() <= 100);
        assertFalse(overview.isLlmEnabled(), "测试环境默认未启用 LLM");
        assertFalse(overview.isLlmConfigured());
        assertEquals("", overview.getModel());
    }

    @Test
    @DisplayName("overview_ReflectsLlmSettings")
    void overview_ReflectsLlmSettings() {
        settings.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", "https://api.example.com/v1",
                "llmApiKey", "sk-audit-secret",
                "llmModel", "gpt-4o-mini",
                "llmAutoOnPublish", "true"));
        clearSiteCache();

        SeoOverviewVO overview = auditService.overview();

        assertTrue(overview.isLlmEnabled());
        assertTrue(overview.isLlmConfigured());
        assertTrue(overview.isAutoOnPublish());
        assertEquals("gpt-4o-mini", overview.getModel());
    }

    @Test
    @DisplayName("overview_NotConfiguredWhenKeyMissing")
    void overview_NotConfiguredWhenKeyMissing() {
        settings.updateSettings(Map.of("llmEnabled", "true", "llmBaseUrl", "https://api.example.com/v1"));
        clearSiteCache();

        SeoOverviewVO overview = auditService.overview();

        assertTrue(overview.isLlmEnabled());
        assertFalse(overview.isLlmConfigured(), "只有地址没有 Key 不算配置完成");
    }

    @Test
    @DisplayName("page_MarksIssuesPerRow")
    void page_MarksIssuesPerRow() {
        var rows = auditService.page(1, 100, "干净的文章", null).getRecords();

        assertEquals(1, rows.size());
        SeoPostRowVO row = rows.get(0);
        assertEquals(CLEAN_ID, row.getPostId());
        assertEquals("干净的文章", row.getTitle());
        assertFalse(row.isHasSeo());
        assertNull(row.getSource());
        assertTrue(row.getIssues().contains("noSeo"));
        assertFalse(row.getIssues().contains("missingDescription"), "有摘要就不算缺描述");
        assertFalse(row.getIssues().contains("missingCover"), "有封面就不算缺封面");
        assertFalse(row.getIssues().contains("longTitle"));
        assertFalse(row.getIssues().contains("duplicateTitle"));
    }

    @Test
    @DisplayName("page_MissingDescriptionAndCoverDetected")
    void page_MissingDescriptionAndCoverDetected() {
        var rows = auditService.page(1, 100, "没有摘要的文章", null).getRecords();

        assertEquals(1, rows.size());
        List<String> issues = rows.get(0).getIssues();
        assertTrue(issues.contains("missingDescription"));
        assertTrue(issues.contains("missingCover"));
        assertTrue(issues.contains("noSeo"));
    }

    @Test
    @DisplayName("page_LongTitleDetected")
    void page_LongTitleDetected() {
        var rows = auditService.page(1, 100, LONG_TITLE, null).getRecords();

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).getIssues().contains("longTitle"));
    }

    @Test
    @DisplayName("page_DuplicateTitleMarksBothPosts")
    void page_DuplicateTitleMarksBothPosts() {
        var rows = auditService.page(1, 100, "重复标题", null).getRecords();

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.getIssues().contains("duplicateTitle")));
    }

    @Test
    @DisplayName("page_DraftsExcluded")
    void page_DraftsExcluded() {
        var rows = auditService.page(1, 100, "草稿不参与体检", null).getRecords();

        assertTrue(rows.isEmpty(), "草稿不该出现在体检列表");
    }

    @Test
    @DisplayName("page_WithSeoRowIsCountedAsHasSeo")
    void page_WithSeoRowIsCountedAsHasSeo() {
        insertSeo(CLEAN_ID, "干净标题", "干净描述", "关键词甲,关键词乙", null, "manual");

        SeoPostRowVO row = rowFor(auditService.page(1, 100, "干净的文章", null).getRecords(), CLEAN_ID);

        assertTrue(row.isHasSeo());
        assertEquals("manual", row.getSource());
        assertEquals("干净标题", row.getSeoTitle());
        assertFalse(row.getIssues().contains("noSeo"));
        assertFalse(row.getIssues().contains("missingDescription"));
        // 没填 SEO 分享图，但文章本身有封面，所以不算缺分享图
        assertFalse(row.getIssues().contains("missingCover"));
    }

    @Test
    @DisplayName("page_IssueFilterNarrowsResults")
    void page_IssueFilterNarrowsResults() {
        insertSeo(CLEAN_ID, "干净标题", "干净描述", "关键词", "/uploads/share.png", "manual");

        var noSeo = auditService.page(1, 100, null, SeoIssue.NO_SEO).getRecords();
        var missingDescription = auditService.page(1, 100, null, SeoIssue.MISSING_DESCRIPTION).getRecords();
        var longTitle = auditService.page(1, 100, null, SeoIssue.LONG_TITLE).getRecords();

        assertTrue(noSeo.stream().noneMatch(row -> row.getPostId() == CLEAN_ID));
        assertTrue(missingDescription.stream().anyMatch(row -> row.getPostId() == NO_SUMMARY_ID));
        assertTrue(missingDescription.stream().noneMatch(row -> row.getPostId() == CLEAN_ID));
        assertTrue(longTitle.stream().anyMatch(row -> row.getPostId() == LONG_TITLE_ID));
        assertTrue(longTitle.stream().noneMatch(row -> row.getPostId() == CLEAN_ID));
    }

    @Test
    @DisplayName("page_UnknownIssueIsIgnored")
    void page_UnknownIssueIsIgnored() {
        // 未知 issue 串在 controller 层会被 fromCode 转成 null（不过滤），这里直接传 null 验证不过滤
        var filtered = auditService.page(1, 100, "重复标题", null).getRecords();

        assertEquals(2, filtered.size());
    }

    @Test
    @DisplayName("page_KeywordIsCaseInsensitiveAndEscaped")
    void page_KeywordIsCaseInsensitiveAndEscaped() {
        jdbc.update("UPDATE blog_post SET title = ? WHERE id = ?", "Spring Boot 实战", CLEAN_ID);

        assertEquals(1, auditService.page(1, 100, "spring boot", null).getRecords().size());
        assertEquals(1, auditService.page(1, 100, "SPRING", null).getRecords().size());
        // % 必须被转义成字面量，否则会命中全部
        assertTrue(auditService.page(1, 100, "%", null).getRecords().isEmpty());
    }

    @Test
    @DisplayName("page_PaginationKeepsTotalAndSlicesRecords")
    void page_PaginationKeepsTotalAndSlicesRecords() {
        var first = auditService.page(1, 2, "重复标题", null);
        var second = auditService.page(2, 2, "重复标题", null);

        assertEquals(2L, first.getTotal());
        assertEquals(2, first.getRecords().size());
        assertEquals(0, second.getRecords().size(), "第二页应已超出总数");
        assertEquals(2, first.getSize());
        assertEquals(1, first.getCurrent());
    }

    @Test
    @DisplayName("detail_ReturnsEffectiveValues")
    void detail_ReturnsEffectiveValues() {
        insertSeo(CLEAN_ID, "SEO 标题", "SEO 描述", "关键词", "/uploads/share.png", "llm");

        SeoDetailVO detail = auditService.detail(CLEAN_ID);

        assertEquals("SEO 标题", detail.getSeoTitle());
        assertEquals("SEO 标题", detail.getEffectiveTitle());
        assertEquals("SEO 描述", detail.getEffectiveDescription());
        assertEquals("llm", detail.getSource());
        assertEquals("有摘要", detail.getPostSummary());
        assertEquals(CLEAN_ID, detail.getPostId());
    }

    @Test
    @DisplayName("detail_NoSeoRowIs404")
    void detail_NoSeoRowIs404() {
        BusinessException error = assertThrows(BusinessException.class, () -> auditService.detail(CLEAN_ID));

        assertEquals(404, error.getCode());
    }

    @Test
    @DisplayName("detailOrDerived_NeverThrowsForMissingSeo")
    void detailOrDerived_NeverThrowsForMissingSeo() {
        SeoDetailVO detail = auditService.detailOrDerived(CLEAN_ID);

        assertFalse(detail.isHasSeo());
        assertEquals("干净的文章", detail.getEffectiveTitle());
        assertEquals("有摘要", detail.getEffectiveDescription());
    }

    @Test
    @DisplayName("detailOrDerived_MissingPostIs404")
    void detailOrDerived_MissingPostIs404() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> auditService.detailOrDerived(999_999_996L));

        assertEquals(404, error.getCode());
    }

    @Test
    @DisplayName("effectiveDescription_FallsBackToTruncatedBody")
    void effectiveDescription_FallsBackToTruncatedBody() {
        jdbc.update("UPDATE blog_post SET summary = NULL WHERE id = ?", NO_SUMMARY_ID);

        SeoDetailVO detail = auditService.detailOrDerived(NO_SUMMARY_ID);

        assertEquals("正文内容", detail.getEffectiveDescription());
    }
}
