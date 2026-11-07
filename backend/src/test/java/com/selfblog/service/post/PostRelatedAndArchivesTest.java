package com.selfblog.service.post;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.vo.post.ArchivePostVO;
import com.selfblog.vo.post.RelatedPostVO;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 相关文章推荐（getRelatedPosts）与全站归档（getArchives）的集成测试：真实 H2。
 *
 * <p>重点覆盖：共享标签数降序、同数 id 降序、排除当前篇/草稿/已删除、
 * 软删除的关联行不算共享标签、不足 5 篇按 createTime 倒序补齐、
 * 草稿当前篇返回空列表、缺失当前篇 404、摘要截断到 100 字符；
 * 归档只含已发布文章、按创建时间倒序、仅投影 6 个精简字段。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PostQueryService related & archives")
class PostRelatedAndArchivesTest {

    private static final long TAG_JIA = 883901L;
    private static final long TAG_YI = 883902L;

    @Autowired
    private PostQueryService queries;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    @BeforeEach
    void data() {
        // getTagsByPostId 带 @Cacheable：事务回滚不会清 Caffeine，
        // 上一个测试方法缓存的 tagsByPost:<id> 会污染本方法的断言
        var cache = caches.getCache(CaffeineCacheConfig.CATEGORY_CACHE);
        if (cache != null) {
            cache.clear();
        }
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_tag");
        jdbc.update("INSERT INTO blog_tag (id, name, is_deleted) VALUES (?, ?, 0)", TAG_JIA, "标签甲");
        jdbc.update("INSERT INTO blog_tag (id, name, is_deleted) VALUES (?, ?, 0)", TAG_YI, "标签乙");

        // 当前篇（排名测试用）：已发布，带 甲+乙
        insertPost(883000, 1, 0, "摘要883000", "/uploads/c-883000.jpg", 5L, "2026-01-01 10:00:00");
        linkTag(883000, TAG_JIA, 0);
        linkTag(883000, TAG_YI, 0);
        // 共享 2 个标签（甲+乙）→ 排第一
        insertPost(883001, 1, 0, "摘要883001", "/uploads/c-883001.jpg", 5L, "2026-01-02 10:00:00");
        linkTag(883001, TAG_JIA, 0);
        linkTag(883001, TAG_YI, 0);
        // 共享 1 个标签（甲），与 883003 同数但 id 更小 → 排在其后
        insertPost(883002, 1, 0, "摘要883002", null, 5L, "2026-01-02 10:00:00");
        linkTag(883002, TAG_JIA, 0);
        // 共享 1 个标签（甲），同数里 id 最大 → 同数倒序排在前
        insertPost(883003, 1, 0, "摘要883003", null, 5L, "2026-01-03 10:00:00");
        linkTag(883003, TAG_JIA, 0);
        // 草稿：即使共享标签也不能出现在推荐里
        insertPost(883004, 0, 0, "摘要883004", null, 5L, "2026-01-02 10:00:00");
        linkTag(883004, TAG_JIA, 0);
        // 已删除：即使 status=1 也要被 @TableLogic 过滤
        insertPost(883005, 1, 1, "摘要883005", null, 5L, "2026-01-02 10:00:00");
        linkTag(883005, TAG_JIA, 0);
        // 关联行被软删除：文章本身已发布，但不算共享标签
        insertPost(883008, 1, 0, "摘要883008", null, 5L, "2026-01-04 10:00:00");
        linkTag(883008, TAG_JIA, 1);
        // 无标签的已发布文章：只可能走兜底补位
        insertPost(883006, 1, 0, "摘要883006", null, 7L, "2026-02-01 10:00:00");
        insertPost(883007, 1, 0, "摘要883007", null, 3L, "2026-01-31 10:00:00");
        // view_count 为 NULL 的已发布文章：归档视图要兜底成 0
        insertPost(883015, 1, 0, "摘要883015", null, null, "2026-01-20 10:00:00");
    }

    @AfterEach
    void clearCacheAgain() {
        var cache = caches.getCache(CaffeineCacheConfig.CATEGORY_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private void insertPost(long id, int status, int deleted, String summary,
                            String coverUrl, Long viewCount, String created) {
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted, view_count) VALUES (?, 42, ?, ?, '正文内容" + id
                        + "', '随笔', ?, ?, TIMESTAMP '" + created + "', TIMESTAMP '" + created + "', ?, ?)",
                id, "标题" + id, summary, coverUrl, status, deleted, viewCount);
    }

    private void linkTag(long postId, long tagId, int deleted) {
        jdbc.update("INSERT INTO blog_post_tag (post_id, tag_id, is_deleted) VALUES (?, ?, ?)", postId, tagId, deleted);
    }

    @Test
    @DisplayName("related_RankedBySharedTagCountDescThenIdDesc_ExcludesSelfDraftDeleted")
    void related_RankedBySharedTagCountDescThenIdDesc_ExcludesSelfDraftDeleted() {
        List<RelatedPostVO> related = queries.getRelatedPosts(883000L);

        // 883001 共享 2 个标签排第一；883003 与 883002 同为 1 个，id 降序 883003 在前；
        // 剩两席走兜底：883006 (02-01) 与 883007 (01-31) 比 883015/883008 更新
        assertEquals(List.of(883001L, 883003L, 883002L, 883006L, 883007L),
                related.stream().map(RelatedPostVO::getId).toList());
        // 当前篇自身、草稿、已删除、关联行软删除的文章都不出现
        assertTrue(related.stream().noneMatch(vo -> vo.getId() == 883000L), "不能推荐自己");
        assertTrue(related.stream().noneMatch(vo -> vo.getId() == 883004L), "草稿不能出现在推荐里");
        assertTrue(related.stream().noneMatch(vo -> vo.getId() == 883005L), "已删除文章不能出现在推荐里");
        assertTrue(related.stream().noneMatch(vo -> vo.getId() == 883008L),
                "关联行软删除不算共享标签，且其创建时间也排不进兜底");
    }

    @Test
    @DisplayName("related_NoTagsOnCurrent_FallsBackToLatestPublished")
    void related_NoTagsOnCurrent_FallsBackToLatestPublished() {
        // 当前篇已发布但没有任何标签：全部 5 席由最新已发布文章补齐
        insertPost(883010, 1, 0, "摘要883010", null, 1L, "2026-03-01 10:00:00");

        List<RelatedPostVO> related = queries.getRelatedPosts(883010L);

        // createTime 倒序：883006 (02-01) → 883007 (01-31) → 883015 (01-20) → 883008 (01-04) → 883003 (01-03)
        assertEquals(List.of(883006L, 883007L, 883015L, 883008L, 883003L),
                related.stream().map(RelatedPostVO::getId).toList());
        assertTrue(related.stream().noneMatch(vo -> vo.getId() == 883010L), "兜底也要排除当前篇");
    }

    @Test
    @DisplayName("related_DraftCurrent_ReturnsEmptyList")
    void related_DraftCurrent_ReturnsEmptyList() {
        // 作者预览草稿时：不推荐任何文章（公开页只对已发布文章调用）
        insertPost(883011, 0, 0, "草稿摘要", null, 0L, "2026-03-01 10:00:00");
        linkTag(883011, TAG_JIA, 0);

        List<RelatedPostVO> related = queries.getRelatedPosts(883011L);

        assertTrue(related.isEmpty(), "未发布的当前篇返回空推荐");
    }

    @Test
    @DisplayName("related_MissingPost_Throws404")
    void related_MissingPost_Throws404() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> queries.getRelatedPosts(99999999L));

        assertEquals(404, error.getCode(), "与详情接口同一套 404 语义");
        assertEquals("文章不存在", error.getMessage());
    }

    @Test
    @DisplayName("related_SummaryTruncatedTo100_BlankBecomesEmpty_CoverAsStored")
    void related_SummaryTruncatedTo100_BlankBecomesEmpty_CoverAsStored() {
        // 当前篇带标签 甲；两篇候选共享 甲，一篇摘要超长、一篇摘要空白
        insertPost(883012, 1, 0, "当前篇摘要", null, 0L, "2026-01-01 10:00:00");
        linkTag(883012, TAG_JIA, 0);
        String longSummary = "摘".repeat(300);
        insertPost(883013, 1, 0, longSummary, "/uploads/cover-a.jpg", 0L, "2026-01-05 10:00:00");
        linkTag(883013, TAG_JIA, 0);
        insertPost(883014, 1, 0, "   ", "", 0L, "2026-01-06 10:00:00");
        linkTag(883014, TAG_JIA, 0);

        List<RelatedPostVO> related = queries.getRelatedPosts(883012L);

        // 所有候选同数（1 个共享标签），id 降序：883014 → 883013 → 883003 → 883002 → 883001
        assertEquals(List.of(883014L, 883013L, 883003L, 883002L, 883001L),
                related.stream().map(RelatedPostVO::getId).toList());
        RelatedPostVO blank = related.get(0);
        assertEquals("", blank.getSummary(), "空白摘要归一为空串");
        assertEquals("", blank.getCoverUrl(), "coverUrl 按存储原样返回（空串）");
        RelatedPostVO longOne = related.get(1);
        assertEquals(100, longOne.getSummary().length(), "摘要截断到 100 字符");
        assertEquals("摘".repeat(100), longOne.getSummary());
        assertEquals("/uploads/cover-a.jpg", longOne.getCoverUrl());
        assertEquals("标题883013", longOne.getTitle());
        assertEquals(LocalDateTime.of(2026, 1, 5, 10, 0, 0), longOne.getCreateTime());
    }

    @Test
    @DisplayName("archives_PublishedOnly_OrderedByCreateTimeDesc_SixFieldProjection")
    void archives_PublishedOnly_OrderedByCreateTimeDesc_SixFieldProjection() {
        List<ArchivePostVO> archives = queries.getArchives();

        // 草稿 883004 与已删除 883005 不出现；createTime 倒序、同时间戳（883002/883001）id 降序
        assertEquals(List.of(883006L, 883007L, 883015L, 883008L, 883003L, 883002L, 883001L, 883000L),
                archives.stream().map(ArchivePostVO::getId).toList());

        ArchivePostVO newest = archives.get(0);
        assertEquals("标题883006", newest.getTitle());
        assertEquals("随笔", newest.getCategory());
        assertEquals(7L, newest.getViewCount());
        assertEquals(LocalDateTime.of(2026, 2, 1, 10, 0, 0), newest.getCreateTime());
        assertEquals(LocalDateTime.of(2026, 2, 1, 10, 0, 0), newest.getUpdateTime());
        for (ArchivePostVO vo : archives) {
            assertNotNull(vo.getId());
            assertNotNull(vo.getTitle());
            assertNotNull(vo.getCreateTime());
            assertNotNull(vo.getUpdateTime());
            assertTrue(vo.getViewCount() >= 0);
        }
        // view_count 为 NULL 的行兜底为 0，而不是把 null 丢给前端
        assertEquals(0L, archives.stream().filter(vo -> vo.getId() == 883015L)
                .findFirst().orElseThrow().getViewCount());
    }
}
