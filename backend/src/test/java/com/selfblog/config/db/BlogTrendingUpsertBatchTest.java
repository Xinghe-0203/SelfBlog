package com.selfblog.config.db;

import com.selfblog.entity.BlogTrending;
import com.selfblog.mapper.BlogTrendingMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 批量趋势写入的语义等价性（task-9 要求 C 的验证）。
 *
 * <p>批量化把「循环内逐条 upsert」收敛成一次往返，但必须证明它和单条 {@code upsert} 的语义逐字一致：
 * <ol>
 *   <li>同一篇文章同一天只有一行；</li>
 *   <li>再次写入同一天的数据会更新既有行（而不是插出第二行）；</li>
 *   <li>命中唯一键时把 {@code is_deleted} 复位为 0，否则被逻辑删除过的那一天永远查不到；</li>
 *   <li>一批里不同文章互不干扰。</li>
 * </ol>
 *
 * <p>刻意分两次 {@code upsertBatch} 来测「同日重复」而不是在同一个 VALUES 列表里放两条相同键：
 * 真实调用方（每日定时任务重跑）走的就是「先一批、再一批」的路径，而 H2 的 MERGE 对
 * 同一条语句内的重复键行为与 MySQL/SQLite 未必一致，把结论建在跨方言不确定的行为上不可靠。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogTrendingUpsertBatchTest {

    @Autowired BlogTrendingMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private static final LocalDate DAY = LocalDate.of(2026, 10, 18);

    @Test
    void batchInsertsOneRowPerPostAndKeepsThemIndependent() {
        mapper.upsertBatch(List.of(
                trending(810001L, 10.0, 10),
                trending(810002L, 20.0, 20),
                trending(810003L, 30.0, 30)));

        assertEquals(3, countRows(810001L, 810002L, 810003L), "一批 3 篇不同文章应产生 3 行");
        assertEquals(10.0, scoreOf(810001L));
        assertEquals(20.0, scoreOf(810002L));
        assertEquals(30.0, scoreOf(810003L));
    }

    @Test
    void secondBatchUpdatesTheSameDailyRowInsteadOfInsertingAnother() {
        mapper.upsertBatch(List.of(trending(810011L, 10.0, 10)));
        mapper.upsertBatch(List.of(trending(810011L, 25.0, 25)));

        assertEquals(1, countRows(810011L), "同一篇文章同一天只应有一行");
        assertEquals(25.0, scoreOf(810011L), "第二次写入应更新既有行");
    }

    @Test
    void batchRevivesLogicallyDeletedDailyRows() {
        mapper.upsertBatch(List.of(
                trending(810021L, 10.0, 10),
                trending(810022L, 11.0, 11)));
        jdbc.update("UPDATE blog_trending SET is_deleted = 1 WHERE post_id IN (?, ?)", 810021L, 810022L);

        mapper.upsertBatch(List.of(
                trending(810021L, 30.0, 30),
                trending(810022L, 31.0, 31)));

        assertEquals(0, isDeletedOf(810021L), "upsertBatch 应把逻辑删除标记复位");
        assertEquals(0, isDeletedOf(810022L), "upsertBatch 应把逻辑删除标记复位");
        assertEquals(1, countRows(810021L), "恢复后每篇仍应只有一行");
        assertEquals(1, countRows(810022L), "恢复后每篇仍应只有一行");
        assertEquals(30.0, scoreOf(810021L));
    }

    @Test
    void batchMatchesSingleUpsertRowForRow() {
        // 同一份数据，一条路径走 upsertBatch、另一条走逐条 upsert，结果必须一致
        List<BlogTrending> batchInput = List.of(
                trending(810031L, 7.5, 7),
                trending(810032L, 8.5, 8));
        List<BlogTrending> singleInput = List.of(
                trending(810041L, 7.5, 7),
                trending(810042L, 8.5, 8));

        mapper.upsertBatch(batchInput);
        singleInput.forEach(mapper::upsert);

        for (int i = 0; i < batchInput.size(); i++) {
            assertEquals(countRows(singleInput.get(i).getPostId()),
                    countRows(batchInput.get(i).getPostId()),
                    "批量与单条写入的行数应一致");
            assertEquals(scoreOf(singleInput.get(i).getPostId()),
                    scoreOf(batchInput.get(i).getPostId()),
                    "批量与单条写入的分值应一致");
        }
    }

    private int countRows(Long... postIds) {
        String placeholders = String.join(",", java.util.Collections.nCopies(postIds.length, "?"));
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM blog_trending WHERE is_deleted = 0 AND post_id IN (" + placeholders + ")",
                Integer.class, (Object[]) postIds);
        return count == null ? 0 : count;
    }

    private double scoreOf(Long postId) {
        Double score = jdbc.queryForObject(
                "SELECT score FROM blog_trending WHERE post_id = ? AND is_deleted = 0", Double.class, postId);
        return score == null ? Double.NaN : score;
    }

    private int isDeletedOf(Long postId) {
        Integer flag = jdbc.queryForObject(
                "SELECT is_deleted FROM blog_trending WHERE post_id = ?", Integer.class, postId);
        return flag == null ? -1 : flag;
    }

    private BlogTrending trending(Long postId, double score, int views) {
        BlogTrending trending = new BlogTrending();
        trending.setPostId(postId);
        trending.setScore(score);
        trending.setViewCount(views);
        trending.setLikeCount(0);
        trending.setCommentCount(0);
        trending.setStatDate(DAY);
        return trending;
    }
}
