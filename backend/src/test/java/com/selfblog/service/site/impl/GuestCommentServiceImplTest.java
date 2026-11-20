package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.dto.site.GuestCommentRequest;
import com.selfblog.service.ai.AiModerationService;
import com.selfblog.service.seo.SeoLlmConfigProvider;
import com.selfblog.service.site.GuestCommentService;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.site.GuestCommentVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GuestCommentServiceImpl 的行为回归（此前该服务没有专用集成测试）。
 *
 * <p>覆盖五组契约：</p>
 * <ol>
 *   <li>创建留言的校验规则（昵称/内容长度、访客标识 8-80 位 [A-Za-z0-9_-]、
 *       评论区开关 403、文章未发布 404）；</li>
 *   <li>访客伪名化哈希的确定性（同访客同哈希，不同访客不同哈希）；</li>
 *   <li>状态/删除语义（updateStatus 0-2、公开列表只看 status=1、物理删除）；</li>
 *   <li><b>C15 政策</b>：访客留言从不触碰 {@code blog_post.comment_count}；</li>
 *   <li>afterCommit 审核交接：总开关打开时注册事务同步器、关闭时不注册
 *       （照 AiCommentModerationWiringTest 的方式 mock 触发器下游 Bean：
 *       {@link AiModerationService} 与 {@link SeoLlmConfigProvider}，触发器保持真实；
 *       测试事务不会提交，afterCommit 永不执行，因此直接观察「是否注册了同步器」）。</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("访客留言服务（校验/哈希/状态/C15/审核交接）")
class GuestCommentServiceImplTest {

    private static final long POST_ID = 888001L;
    private static final long DRAFT_POST_ID = 888002L;
    private static final String VISITOR_ID = "visitor-junit-0001";
    private static final String OTHER_VISITOR_ID = "visitor-junit-0002";

    @Autowired
    private GuestCommentService guestCommentService;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    @MockBean
    private AiModerationService moderationService;
    @MockBean
    private SeoLlmConfigProvider configProvider;

    @BeforeEach
    void seed() {
        clearSettingsCache();
        jdbc.update("DELETE FROM guest_comment");
        jdbc.update("DELETE FROM blog_post");
        // comment_count=7 作为 C15 哨兵：访客留言全链路之后必须仍是 7
        insertPost(POST_ID, 1, 0, 7);
        insertPost(DRAFT_POST_ID, 0, 0, 0);
    }

    @AfterEach
    void cleanup() {
        // settings 开关若在本事务里被改过，回滚后缓存里可能残留改后值，统一清掉
        clearSettingsCache();
    }

    private void clearSettingsCache() {
        var cache = caches.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) cache.clear();
    }

    private void insertPost(long id, int status, int deleted, int commentCount) {
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, status, "
                        + "comment_count, create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, "
                        + status + ", " + commentCount + ", "
                        + "TIMESTAMP '2026-10-16 09:00:00', TIMESTAMP '2026-10-16 09:00:00', " + deleted + ")",
                id, "访客留言测试文章", "摘要", "正文", "随笔");
    }

    private GuestCommentRequest request(String visitorId, String nickname, String content) {
        GuestCommentRequest request = new GuestCommentRequest();
        request.setPostId(POST_ID);
        request.setVisitorId(visitorId);
        request.setNickname(nickname);
        request.setContent(content);
        return request;
    }

    private int commentCountOf(long postId) {
        Integer count = jdbc.queryForObject(
                "SELECT comment_count FROM blog_post WHERE id = ?", Integer.class, postId);
        return count == null ? -1 : count;
    }

    // ==================== (a) 创建留言校验 ====================

    @Test
    @DisplayName("合法留言落库：status=1、返回 id")
    void validCommentIsStoredWithVisibleStatus() {
        Long id = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "正常的留言内容"));

        assertNotNull(id);
        Integer status = jdbc.queryForObject("SELECT status FROM guest_comment WHERE id = ?", Integer.class, id);
        assertEquals(1, status, "新留言默认对外可见");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM guest_comment", Integer.class));
    }

    @Test
    @DisplayName("昵称为空或超过80字符 → 400")
    void nicknameBoundsAreEnforced() {
        BusinessException blank = assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request(VISITOR_ID, "   ", "内容")));
        assertEquals(400, blank.getCode());

        BusinessException tooLong = assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request(VISITOR_ID, "昵".repeat(81), "内容")));
        assertEquals(400, tooLong.getCode());
    }

    @Test
    @DisplayName("内容为空或超过2000字符 → 400")
    void contentBoundsAreEnforced() {
        BusinessException blank = assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request(VISITOR_ID, "访客甲", " ")));
        assertEquals(400, blank.getCode());

        BusinessException tooLong = assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request(VISITOR_ID, "访客甲", "评".repeat(2001))));
        assertEquals(400, tooLong.getCode());
    }

    @Test
    @DisplayName("访客标识 8-80 位 [A-Za-z0-9_-]：过短/过长/非法字符 → 400，边界值放行")
    void visitorIdCharacterRulesAreEnforced() {
        assertEquals(400, assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request("short7!", "访客甲", "内容"))).getCode(),
                "过短且含非法字符必须拒绝");
        assertEquals(400, assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request("a".repeat(81), "访客甲", "内容"))).getCode(),
                "超过80字符必须拒绝");
        assertEquals(400, assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request("bad@visitor", "访客甲", "内容"))).getCode(),
                "非法字符必须拒绝");

        // 边界：恰好 8 位、恰好 80 位、连字符/下划线均可放行
        assertNotNull(guestCommentService.createComment(request("12345678", "访客甲", "内容")));
        assertNotNull(guestCommentService.createComment(request("v".repeat(80), "访客甲", "内容")));
        assertNotNull(guestCommentService.createComment(request("ok_id-okid2", "访客甲", "内容")));
    }

    @Test
    @DisplayName("文章未发布（草稿）→ 404，未登录访客看不到待发布内容")
    void commentOnUnpublishedPostIsRejected() {
        GuestCommentRequest bad = request(VISITOR_ID, "访客甲", "内容");
        bad.setPostId(DRAFT_POST_ID);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(bad));
        assertEquals(404, ex.getCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM guest_comment", Integer.class),
                "被拒留言不得落库");
    }

    @Test
    @DisplayName("站长关闭评论 → 403")
    void commentsCanBeDisabledBySiteOwner() {
        settings.updateSettings(Map.of("commentsEnabled", "false"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                guestCommentService.createComment(request(VISITOR_ID, "访客甲", "内容")));
        assertEquals(403, ex.getCode());
    }

    // ==================== (b) 伪名化哈希确定性 ====================

    @Test
    @DisplayName("同一访客标识两次留言落库的 visitor_hash 完全一致；不同访客哈希不同")
    void visitorHashIsDeterministicAndDistinguishesVisitors() {
        Long first = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "第一条"));
        Long second = guestCommentService.createComment(request(VISITOR_ID, "访客乙", "第二条"));
        Long other = guestCommentService.createComment(request(OTHER_VISITOR_ID, "访客甲", "第三条"));

        String hashFirst = jdbc.queryForObject("SELECT visitor_hash FROM guest_comment WHERE id = ?", String.class, first);
        String hashSecond = jdbc.queryForObject("SELECT visitor_hash FROM guest_comment WHERE id = ?", String.class, second);
        String hashOther = jdbc.queryForObject("SELECT visitor_hash FROM guest_comment WHERE id = ?", String.class, other);

        assertNotNull(hashFirst);
        assertEquals(hashFirst, hashSecond, "同一访客必须得到同一哈希（可识别重复访客）");
        assertNotEquals(hashFirst, hashOther, "不同访客的哈希必须不同");
        assertTrue(hashOther.matches("[0-9a-f]{64}"), "哈希必须是小写十六进制的 HMAC-SHA256");
    }

    // ==================== (c) 状态/删除语义 ====================

    @Test
    @DisplayName("updateStatus 隐藏留言：公开列表只返回 status=1 的行")
    void hiddenStatusDisappearsFromPublicListButRowRemains() {
        Long visible = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "保留的留言"));
        Long hidden = guestCommentService.createComment(request(VISITOR_ID, "访客乙", "要隐藏的留言"));

        guestCommentService.updateStatus(hidden, 0);

        List<GuestCommentVO> records = guestCommentService
                .getPublicComments(POST_ID, 1, 20).getRecords();
        assertEquals(1, records.size(), "公开列表只看 status=1");
        assertEquals(visible, records.get(0).getId());
        Integer rowStatus = jdbc.queryForObject("SELECT status FROM guest_comment WHERE id = ?", Integer.class, hidden);
        assertEquals(0, rowStatus, "行仍在，只是被隐藏");
    }

    @Test
    @DisplayName("updateStatus 校验：非法值 400，未知 id 404")
    void updateStatusValidation() {
        Long id = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "内容"));

        assertEquals(400, assertThrows(BusinessException.class, () ->
                guestCommentService.updateStatus(id, 3)).getCode(), "状态只允许 0-2");
        assertEquals(400, assertThrows(BusinessException.class, () ->
                guestCommentService.updateStatus(id, null)).getCode());
        assertEquals(404, assertThrows(BusinessException.class, () ->
                guestCommentService.updateStatus(999999L, 1)).getCode(), "未知留言 404");
    }

    @Test
    @DisplayName("delete 物理移除留言行，未知 id 404")
    void deleteRemovesTheRow() {
        Long id = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "要删除的留言"));

        guestCommentService.delete(id);

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM guest_comment WHERE id = ?",
                Integer.class, id), "删除后行不存在");
        assertEquals(404, assertThrows(BusinessException.class, () ->
                guestCommentService.delete(999999L)).getCode());
    }

    // ==================== (d) C15：blog_post.comment_count 永不被触碰 ====================

    @Test
    @DisplayName("C15：创建/隐藏/删除访客留言后 blog_post.comment_count 保持哨兵值不变")
    void guestCommentsNeverTouchBlogPostCommentCount() {
        assertEquals(7, commentCountOf(POST_ID), "种子哨兵值");

        Long visible = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "留言一"));
        Long toHide = guestCommentService.createComment(request(VISITOR_ID, "访客乙", "留言二"));
        Long toDelete = guestCommentService.createComment(request(OTHER_VISITOR_ID, "访客丙", "留言三"));
        assertEquals(7, commentCountOf(POST_ID), "创建留言不增 comment_count");

        guestCommentService.updateStatus(toHide, 0);
        assertEquals(7, commentCountOf(POST_ID), "隐藏留言不减 comment_count");

        guestCommentService.delete(toDelete);
        guestCommentService.delete(visible);
        assertEquals(7, commentCountOf(POST_ID), "删除留言也不减 comment_count");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM guest_comment", Integer.class),
                "sanity：只剩被隐藏的那条");
    }

    // ==================== (e) afterCommit 审核交接 ====================

    /**
     * 首个 mapper 操作会让 MyBatis-Spring 注册一条会话同步器，先触发它拿到稳定基线，
     * 之后 createComment 的同步器增量才纯粹对应「审核交接」本身。
     */
    private void primeSessionSynchronizationBaseline() {
        guestCommentService.getPublicComments(POST_ID, 1, 20);
    }

    @Test
    @DisplayName("总开关打开：createComment 注册 afterCommit 审核同步器")
    void moderationHandoffIsRegisteredWhenEnabled() {
        whenCommentModerationEnabled(true);
        primeSessionSynchronizationBaseline();
        int before = TransactionSynchronizationManager.getSynchronizations().size();

        Long id = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "开关打开的留言"));

        List<TransactionSynchronization> after = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(before + 1, after.size(), "开关打开时必须注册一条 afterCommit 同步器（交接给审核触发器）");
        assertNotNull(id);
    }

    @Test
    @DisplayName("总开关关闭：createComment 不注册任何审核同步器")
    void moderationHandoffIsNotRegisteredWhenDisabled() {
        whenCommentModerationEnabled(false);
        primeSessionSynchronizationBaseline();
        int before = TransactionSynchronizationManager.getSynchronizations().size();

        Long id = guestCommentService.createComment(request(VISITOR_ID, "访客甲", "开关关闭的留言"));

        assertNotNull(id, "留言照常落库");
        assertEquals(before, TransactionSynchronizationManager.getSynchronizations().size(),
                "开关关闭时不注册任何审核同步器");
    }

    private void whenCommentModerationEnabled(boolean enabled) {
        org.mockito.Mockito.when(configProvider.commentModerationEnabled()).thenReturn(enabled);
    }
}
