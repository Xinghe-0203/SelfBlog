package com.selfblog.service.social;

import com.selfblog.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * C8 回归：删除评论后 {@code blog_post.comment_count} 必须写成<b>绝对值</b>，不能是相对减法。
 *
 * <p>旧实现的形状：{@code decrementCommentCount(id, commentIdsToDelete.size())} 执行
 * {@code SET comment_count = comment_count - #{count} WHERE ... AND comment_count >= #{count}}，
 * 而调用方既不检查 {@code removeByIds} 的实际影响行数，也不校对计数器本身。
 * 两个后果：计数一旦漂移（历史数据、并发删除、或访客留言从未计入）就<b>永远不会自愈</b>；
 * 守卫条件不满足时减法被静默跳过，计数停在错误值上，而删除本身成功了。
 *
 * <p>修复后 {@code BlogCommentServiceImpl} 调 {@code syncCommentCount(postId)}，
 * 由 {@code SELECT COUNT(*) FROM blog_comment WHERE post_id = ? AND is_deleted = 0} 回写绝对值。
 * 本类故意不 mock 任何 mapper —— 只有真实的 COUNT(*) 才能证明"绝对值"这件事。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("删除评论后回写评论计数绝对值（C8）")
class CommentCountSyncTest {

    private static final long AUTHOR_ID = 773300L;
    private static final long POST_ID = 884001L;

    @Autowired
    BlogCommentService comments;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM blog_comment");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted) VALUES(?,?,?,?,1,0)",
                POST_ID, AUTHOR_ID, "评论计数测试", "正文");
    }

    @Test
    @DisplayName("删除根评论时按实际剩余行数回写，而不是减去待删 id 数")
    void deleteWritesTheAbsoluteActiveCount() {
        long rootId = comment(884101L, null, "根评论");
        comment(884102L, rootId, "回复一");
        comment(884103L, null, "另一条根评论");
        setCommentCount(99);

        comments.deleteComment(rootId, AUTHOR_ID);

        assertEquals(1, commentCount(), "只剩一条根评论，计数必须写成 1（99-3 或 99-2 都是错的）");
    }

    @Test
    @DisplayName("计数偏低时向上自愈，而不是被守卫条件静默跳过")
    void driftedCounterIsHealedInsteadOfSilentlySkipped() {
        comment(884111L, null, "一");
        comment(884112L, null, "二");
        long rootId = comment(884113L, null, "三");
        setCommentCount(0);

        comments.deleteComment(rootId, AUTHOR_ID);

        assertEquals(2, commentCount(), "旧实现 0 >= 1 不成立，减法被跳过，计数会停在 0");
    }

    @Test
    @DisplayName("删掉全部评论后计数归零")
    void deletingEveryCommentLeavesZero() {
        long rootId = comment(884121L, null, "唯一一条");
        setCommentCount(500);

        comments.deleteComment(rootId, AUTHOR_ID);

        assertEquals(0, commentCount());
    }

    @Test
    @DisplayName("管理员删除走同一条绝对值回写")
    void adminDeleteAlsoSyncsTheAbsoluteCount() {
        long rootId = comment(884131L, null, "根");
        comment(884132L, rootId, "子");
        comment(884133L, null, "另一条");
        setCommentCount(77);

        comments.adminDeleteComment(rootId, 1L);

        assertEquals(1, commentCount(), "管理员路径同样只能写真实剩余行数");
    }

    @Test
    @DisplayName("已经软删除的评论不会被再次计入")
    void softDeletedCommentsAreNotCounted() {
        comment(884141L, null, "留下");
        long gone = comment(884142L, null, "已删");
        jdbc.update("UPDATE blog_comment SET is_deleted = 1 WHERE id = ?", gone);
        setCommentCount(2);
        long rootId = comment(884143L, null, "本次删除");

        comments.deleteComment(rootId, AUTHOR_ID);

        assertEquals(1, commentCount(), "COUNT(*) 只数 is_deleted = 0 的行");
    }

    @Test
    @DisplayName("删除不存在的评论仍是 404，不触碰计数")
    void missingCommentIsStillNotFound() {
        setCommentCount(3);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> comments.deleteComment(884999L, AUTHOR_ID));

        assertEquals(404, thrown.getCode());
        assertEquals(3, commentCount(), "未发生删除时不得改写计数");
    }

    private long comment(long id, Long parentId, String content) {
        jdbc.update("INSERT INTO blog_comment(id,post_id,user_id,parent_id,content,is_deleted) VALUES(?,?,?,?,?,0)",
                id, POST_ID, AUTHOR_ID, parentId, content);
        return id;
    }

    private void setCommentCount(int count) {
        jdbc.update("UPDATE blog_post SET comment_count = ? WHERE id = ?", count, POST_ID);
    }

    private int commentCount() {
        Integer value = jdbc.queryForObject("SELECT comment_count FROM blog_post WHERE id = ?", Integer.class, POST_ID);
        return value == null ? -1 : value;
    }
}
