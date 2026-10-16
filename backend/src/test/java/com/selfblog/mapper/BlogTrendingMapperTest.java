package com.selfblog.mapper;

import com.selfblog.entity.BlogTrending;
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
 * 趋势表的每日 upsert 语义（每天每篇文章只有一行）。
 *
 * <p>与标签/媒体同类：唯一键 + 逻辑删除。H2 兜底语句用 MERGE … KEY 合并，
 * 因此测试库必须有和 MySQL/SQLite 相同的唯一键，否则这条语句在 H2 无法执行；
 * 查询也必须自己过滤 is_deleted。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogTrendingMapperTest {
    @Autowired BlogTrendingMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private static final LocalDate DAY = LocalDate.of(2026, 10, 17);

    @Test
    void upsertCreatesThenUpdatesTheSameDailyRow() {
        mapper.upsert(trending(800001L, 10.0, 10));
        mapper.upsert(trending(800001L, 25.0, 25));

        List<BlogTrending> rows = mapper.selectByPostIdAndDate(800001L, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay());
        assertEquals(1, rows.size(), "同一篇文章同一天只应有一行");
        assertEquals(25.0, rows.get(0).getScore());
    }

    @Test
    void upsertRevivesALogicallyDeletedDailyRow() {
        mapper.upsert(trending(800002L, 10.0, 10));
        jdbc.update("UPDATE blog_trending SET is_deleted = 1 WHERE post_id = ?", 800002L);

        mapper.upsert(trending(800002L, 30.0, 30));

        assertEquals(0, jdbc.queryForObject(
                "SELECT is_deleted FROM blog_trending WHERE post_id = ?", Integer.class, 800002L),
                "upsert 应把逻辑删除标记复位，否则这一天的数据永远不可见");
        List<BlogTrending> rows = mapper.selectByPostIdAndDate(800002L, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay());
        assertEquals(1, rows.size(), "恢复后仍应只有一行");
        assertEquals(30.0, rows.get(0).getScore());
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
