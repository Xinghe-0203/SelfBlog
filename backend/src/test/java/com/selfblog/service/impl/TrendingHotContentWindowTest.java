package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.entity.BlogTrending;
import com.selfblog.service.content.TrendingService;
import com.selfblog.vo.content.HotContentVO;
import com.selfblog.vo.content.HotPostVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 热门内容的抓取窗口契约（C22）。
 *
 * <p>缺陷回顾：{@code getHotContent} 在内存里合并「热门文章 + 热门动态」两个来源，
 * 只抓固定窗口再内存分页，却把窗口条数当成全库 total。深页于是返回空数组、
 * 同时宣称还有数据（幽灵页）；另外动态源曾用 {@code .last("LIMIT " + fetchSize)}
 * 拼接 SQL。</p>
 *
 * <p>修复后的契约：total 描述窗口内条数，pageNum 被钳制到窗口最后一页，
 * 因此任何页码都能拿到自洽的一页。同时 {@code getHotPosts} 的数据库 total
 * 与 records 使用同一谓词（JOIN blog_post 过滤未发布 / 已删除文章）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrendingHotContentWindowTest {

    @Autowired
    private TrendingService trendingService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long authorId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM blog_trending");
        jdbc.update("DELETE FROM blog_circle_post");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM sys_user");

        jdbc.update("INSERT INTO sys_user (username, password, nickname, role, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?)",
                "trending-author", "password", "trending-author", "user", 1, 0);
        authorId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, "trending-author");
    }

    @Test
    @DisplayName("超出窗口的页码被钳到最后一页，而不是返回空 records + 非零 total")
    void pageBeyondTheWindowIsClampedToTheLastPage() {
        insertCirclePost("热动态一", 9);
        insertCirclePost("热动态二", 5);
        insertCirclePost("热动态三", 1);

        IPage<HotContentVO> page = trendingService.getHotContent(99, 2);

        assertEquals(3L, page.getTotal(), "total 描述窗口内条数");
        assertEquals(2L, page.getCurrent(), "第 99 页应被钳制到最后一页（第 2 页）");
        assertEquals(1, page.getRecords().size(), "最后一页只应剩 1 条，不能返回幽灵页");
        assertEquals(2L, page.getPages());
    }

    @Test
    @DisplayName("窗口内正常分页：第一页满、第二页剩一条，且按热度降序")
    void firstPageIsFullAndOrderedByScore() {
        Long top = insertCirclePost("最高热度", 20);
        Long middle = insertCirclePost("中等热度", 10);
        insertCirclePost("最低热度", 1);

        IPage<HotContentVO> firstPage = trendingService.getHotContent(1, 2);

        assertEquals(3L, firstPage.getTotal());
        assertEquals(2, firstPage.getRecords().size());
        assertEquals(top, firstPage.getRecords().get(0).getId(), "第一页第一条应是热度最高的动态");
        assertEquals(middle, firstPage.getRecords().get(1).getId());

        IPage<HotContentVO> secondPage = trendingService.getHotContent(2, 2);
        assertEquals(1, secondPage.getRecords().size());
    }

    @Test
    @DisplayName("非正数 pageSize 落到默认值而不是抛异常")
    void nonPositivePageSizeFallsBackToTheDefault() {
        insertCirclePost("唯一动态", 3);

        IPage<HotContentVO> page = trendingService.getHotContent(0, 0);

        assertEquals(1L, page.getCurrent());
        assertEquals(10L, page.getSize(), "pageSize < 1 应回落到默认 10");
        assertEquals(1L, page.getTotal());
    }

    @Test
    @DisplayName("空窗口返回空页而不是 null")
    void emptyWindowReturnsAnEmptyPage() {
        IPage<HotContentVO> page = trendingService.getHotContent(1, 10);

        assertEquals(0L, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
        assertEquals(1L, page.getCurrent(), "空窗口的当前页仍是第 1 页");
        assertEquals(0L, page.getPages(), "total 为 0 时 MyBatis-Plus 的页数就是 0");
    }

    @Test
    @DisplayName("getHotPosts 的 total 只统计已发布且未删除的文章")
    void hotPostsTotalExcludesUnpublishedArticles() {
        Long published = insertPost("已发布文章", 1);
        Long draft = insertPost("草稿文章", 0);
        Long deleted = insertPost("已删除文章", 1);

        insertTrending(published, 30.0);
        insertTrending(draft, 20.0);
        insertTrending(deleted, 10.0);
        jdbc.update("UPDATE blog_post SET is_deleted = 1 WHERE id = ?", deleted);

        IPage<HotPostVO> page = trendingService.getHotPosts(1, 10);

        assertEquals(1L, page.getTotal(), "草稿与已删除文章的热度行不能计入 total");
        assertEquals(1, page.getRecords().size(), "records 与 total 必须同源");
        assertEquals(published, page.getRecords().get(0).getId());
    }

    // ==================== 测试数据 ====================

    private Long insertPost(String title, int status) {
        jdbc.update("INSERT INTO blog_post (user_id, title, summary, content, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?)",
                authorId, title, title + "摘要", "正文", status, 0);
        return jdbc.queryForObject("SELECT id FROM blog_post WHERE title = ?", Long.class, title);
    }

    private void insertTrending(Long postId, double score) {
        BlogTrending trending = new BlogTrending();
        trending.setPostId(postId);
        trending.setScore(score);
        trending.setViewCount(0);
        trending.setLikeCount(0);
        trending.setCommentCount(0);
        trending.setStatDate(LocalDate.now());
        jdbc.update("INSERT INTO blog_trending (post_id, score, view_count, like_count, comment_count, date, is_deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                trending.getPostId(), trending.getScore(), trending.getViewCount(), trending.getLikeCount(),
                trending.getCommentCount(), trending.getStatDate(), 0);
    }

    private Long insertCirclePost(String content, int likeCount) {
        jdbc.update("INSERT INTO blog_circle_post (user_id, content, like_count, comment_count, repost_count, view_count, "
                        + "visibility, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                authorId, content, likeCount, 0, 0, 0L, 0, 1, 0);
        return jdbc.queryForObject("SELECT id FROM blog_circle_post WHERE content = ?", Long.class, content);
    }
}
