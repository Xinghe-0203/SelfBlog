package com.selfblog.service.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文章删除的级联回归（F-14）。
 *
 * <p>旧实现里只有 {@code adminDeletePost} 会清理 {@code blog_comment}，作者自删的文章会留下
 * 指向已删除文章的活跃评论行；统计类查询会一直把它们算进去。作者删除与管理员删除现在
 * 共用同一条级联，本用例对两条路径都断言。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("文章删除级联清理子表")
class PostDeleteCascadeTest {

    private static final long AUTHOR_ID = 449900L;
    private static final long POST_ID = 883001L;
    private static final long OTHER_POST_ID = 883002L;

    @Autowired
    BlogPostService posts;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM blog_comment");
        jdbc.update("DELETE FROM blog_like");
        jdbc.update("DELETE FROM blog_collect");
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_post");

        insertPost(POST_ID);
        insertPost(OTHER_POST_ID);
        jdbc.update("INSERT INTO blog_comment(id,post_id,user_id,content,is_deleted) VALUES(?,?,?,?,0)",
                883101L, POST_ID, AUTHOR_ID, "要一起下线的评论");
        jdbc.update("INSERT INTO blog_comment(id,post_id,user_id,content,is_deleted) VALUES(?,?,?,?,0)",
                883102L, OTHER_POST_ID, AUTHOR_ID, "别的文章的评论");
        jdbc.update("INSERT INTO blog_like(id,user_id,post_id,is_deleted) VALUES(?,?,?,0)",
                883103L, AUTHOR_ID, POST_ID);
        jdbc.update("INSERT INTO blog_collect(id,user_id,post_id,is_deleted) VALUES(?,?,?,0)",
                883104L, AUTHOR_ID, POST_ID);
    }

    @Test
    @DisplayName("作者删除文章时评论、点赞、收藏一并软删除")
    void authorDeleteSoftDeletesCommentsAndInteractions() {
        posts.deletePost(POST_ID, AUTHOR_ID, false);

        assertEquals(1, isDeletedOf("blog_comment", 883101L), "作者删除后该文章的评论必须软删除");
        assertEquals(0, isDeletedOf("blog_comment", 883102L), "不得波及其他文章的评论");
        assertEquals(1, isDeletedOf("blog_like", 883103L));
        assertEquals(1, isDeletedOf("blog_collect", 883104L));
        assertEquals(1, isDeletedOf("blog_post", POST_ID));
    }

    @Test
    @DisplayName("管理员删除文章走同一条级联")
    void adminDeleteUsesTheSameCascade() {
        posts.adminDeletePost(POST_ID, 1L);

        assertEquals(1, isDeletedOf("blog_comment", 883101L), "管理员删除后评论同样必须软删除");
        assertEquals(1, isDeletedOf("blog_like", 883103L));
        assertEquals(1, isDeletedOf("blog_collect", 883104L));
    }

    @Test
    @DisplayName("删除后五个计数一并清零")
    void countersAreResetBeforeSoftDelete() {
        jdbc.update("UPDATE blog_post SET like_count=5, collect_count=4, comment_count=3, share_count=2, view_count=99 WHERE id=?",
                POST_ID);

        posts.deletePost(POST_ID, AUTHOR_ID, false);

        assertEquals(0, countOf(POST_ID, "like_count"));
        assertEquals(0, countOf(POST_ID, "collect_count"));
        assertEquals(0, countOf(POST_ID, "comment_count"));
        assertEquals(0, countOf(POST_ID, "share_count"));
        assertEquals(0, countOf(POST_ID, "view_count"));
    }

    private void insertPost(long id) {
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted) VALUES(?,?,?,?,1,0)",
                id, AUTHOR_ID, "级联测试" + id, "正文");
    }

    private int isDeletedOf(String table, long id) {
        Integer value = jdbc.queryForObject("SELECT is_deleted FROM " + table + " WHERE id = ?", Integer.class, id);
        return value == null ? -1 : value;
    }

    private int countOf(long postId, String column) {
        Integer value = jdbc.queryForObject(
                "SELECT " + column + " FROM blog_post WHERE id = ?", Integer.class, postId);
        return value == null ? -1 : value;
    }
}
