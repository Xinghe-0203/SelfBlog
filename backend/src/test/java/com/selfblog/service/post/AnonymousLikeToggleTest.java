package com.selfblog.service.post;

import com.selfblog.entity.AnonymousPostLike;
import com.selfblog.mapper.AnonymousPostLikeMapper;
import com.selfblog.vo.post.AnonymousLikeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 匿名点赞切换的回归测试。
 *
 * <p>背景一（切换必须真的落库）：{@code AnonymousPostLike.isDeleted} 命中全局
 * {@code logic-delete-field}，MyBatis-Plus 的 {@code updateById} 会把逻辑删除列从 SET 子句剔除、
 * 并在 WHERE 追加 {@code is_deleted = 0}，所以原先用 {@code setIsDeleted(...) + updateById}
 * 切换状态是**静默空操作**：取消点赞写不进去，重新读取仍然显示已点赞。修复后走 mapper 的原生
 * {@code @Update}。因此这里断言的不只是返回值，还有数据库里的 {@code is_deleted} 真实值。</p>
 *
 * <p>背景二（并发插入冲突）：方法上的事务代理在方法外层，{@code synchronized} 会在提交前释放，
 * 两个并发请求可能都走“无行 → 插入”分支，后者撞上 (post_id, visitor_hash) 唯一键。
 * 第二条用例用 spy 精确复现这一时序（第一次查不到行 → 插入抛 DuplicateKeyException →
 * 重新查到对方已插入的行 → 走切换更新路径），而不是把 500 抛给访客。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("匿名点赞切换与并发插入冲突")
class AnonymousLikeToggleTest {

    private static final long POST_ID = 881001L;
    private static final String VISITOR_ID = "visitor-abc-123";

    @Autowired
    private AnonymousLikeService anonymousLikeService;

    @Autowired
    private JdbcTemplate jdbc;

    /** 与被测服务同一来源的密钥，避免测试里硬编码派生哈希。 */
    @Value("${app.visitor-hash-secret:}")
    private String hashSecret;

    @SpyBean
    private AnonymousPostLikeMapper likeMapper;

    @BeforeEach
    void seedPublishedPost() {
        jdbc.update("DELETE FROM anonymous_post_like");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,view_count,like_count) "
                + "VALUES(?,42,'匿名点赞测试','正文',1,0,3,0)", POST_ID);
    }

    @Test
    @DisplayName("连续两次切换：第二次必须真正取消点赞并落库")
    void secondToggleReallyUnmarksTheRow() {
        AnonymousLikeVO first = anonymousLikeService.toggle(POST_ID, VISITOR_ID);
        assertTrue(first.isLiked(), "首次切换应标记为已点赞");

        AnonymousLikeVO second = anonymousLikeService.toggle(POST_ID, VISITOR_ID);
        assertFalse(second.isLiked(), "再次切换应取消点赞");

        // 关键回归：取消点赞必须真的写进 is_deleted，而不是被 updateById 静默丢弃。
        assertEquals(1, jdbc.queryForObject(
                "SELECT is_deleted FROM anonymous_post_like WHERE post_id = ? AND visitor_hash IS NOT NULL",
                Integer.class, POST_ID), "取消点赞必须把 is_deleted 置为 1");

        AnonymousLikeVO status = anonymousLikeService.getStatus(POST_ID, VISITOR_ID);
        assertFalse(status.isLiked(), "重新查询状态时不应再显示已点赞");
        assertEquals(0L, status.getLikeCount(), "取消后活跃点赞数应为 0");
    }

    @Test
    @DisplayName("第三次切换必须恢复已点赞：走逻辑删除行的恢复分支")
    void thirdToggleRestoresTheLogicallyDeletedRow() {
        anonymousLikeService.toggle(POST_ID, VISITOR_ID);
        anonymousLikeService.toggle(POST_ID, VISITOR_ID);
        assertEquals(1, jdbc.queryForObject(
                "SELECT is_deleted FROM anonymous_post_like WHERE post_id = ?", Integer.class, POST_ID),
                "第二次切换后行应处于逻辑删除状态");

        AnonymousLikeVO third = anonymousLikeService.toggle(POST_ID, VISITOR_ID);

        assertTrue(third.isLiked(), "第三次切换应重新点赞");
        assertEquals(0, jdbc.queryForObject(
                "SELECT is_deleted FROM anonymous_post_like WHERE post_id = ?", Integer.class, POST_ID),
                "恢复分支必须真的把 is_deleted 写回 0");
        // 同一行被复用（唯一键不含 is_deleted，物理删除后重建会占用新 id），因此行数仍为 1。
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM anonymous_post_like WHERE post_id = ?", Integer.class, POST_ID),
                "恢复应复用原行，而不是插入第二行");
        assertTrue(anonymousLikeService.getStatus(POST_ID, VISITOR_ID).isLiked(), "状态查询应同步为已点赞");
    }

    @Test
    @DisplayName("插入撞唯一键时改为切换已存在的行，而不是抛 500")
    void duplicateKeyOnInsertFallsBackToToggle() {
        // 模拟“对方请求已插入成功”：先直接写入一条活跃记录，并读出它的 id。
        jdbc.update("INSERT INTO anonymous_post_like(post_id,visitor_hash,is_deleted) VALUES(?,?,0)",
                POST_ID, hashOf(VISITOR_ID));
        Long rivalId = jdbc.queryForObject(
                "SELECT id FROM anonymous_post_like WHERE post_id = ?", Long.class, POST_ID);

        AnonymousPostLike rival = new AnonymousPostLike();
        rival.setId(rivalId);
        rival.setPostId(POST_ID);
        rival.setVisitorHash(hashOf(VISITOR_ID));
        rival.setIsDeleted(0);

        // 精确复现并发时序：本线程第一次读不到行（→ 走插入分支），
        // 插入撞唯一键后重新读取时，才看到对方已插入的那一行。
        // 用 spy 的连续返回值而不是 callRealMethod——mapper 是接口代理，接口抽象方法无法“调真实实现”。
        doReturn(null).doReturn(rival)
                .when(likeMapper).selectRawByPostAndVisitor(anyLong(), anyString());
        doThrow(new DuplicateKeyException("uk_anon_like_post_visitor"))
                .when(likeMapper).insert(any(AnonymousPostLike.class));

        AnonymousLikeVO result = anonymousLikeService.toggle(POST_ID, VISITOR_ID);

        assertFalse(result.isLiked(), "对方已点赞，本次切换应取消点赞");
        verify(likeMapper, times(2)).selectRawByPostAndVisitor(anyLong(), anyString());
        assertEquals(1, jdbc.queryForObject(
                "SELECT is_deleted FROM anonymous_post_like WHERE post_id = ?", Integer.class, POST_ID),
                "冲突回退路径同样必须真的落库");
    }

    /** 与 AnonymousLikeServiceImpl 使用同一套 JWT_SECRET 派生规则，避免测试里硬编码哈希。 */
    private String hashOf(String visitorId) {
        return com.selfblog.utils.VisitorHashUtils.hash(visitorId, hashSecret);
    }
}
