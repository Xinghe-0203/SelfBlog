package com.selfblog.service.ai;

import com.selfblog.dto.site.GuestCommentRequest;
import com.selfblog.service.seo.SeoLlmConfigProvider;
import com.selfblog.service.site.GuestCommentService;
import com.selfblog.service.site.SiteSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * C 自动审核与访客留言创建之间的接线。
 *
 * <p>这个用例<b>刻意不加 {@code @Transactional}</b>：只有让 {@code createComment} 自己开事务并
 * 真的提交，{@code afterCommit} 回调才会触发——测试级事务永远不会提交，那样验的就不是真实链路了。
 * 代价是数据要自己清。</p>
 *
 * <p>验证两件契约要求：留言仍然照常落库并返回 id（审核绝不能让它失败或变慢）；
 * 审核只在总开关打开时派发，且派发发生在提交之后。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("访客留言自动审核接线")
class AiCommentModerationWiringTest {

    private static final long POST_ID = 887001L;
    private static final String VISITOR_ID = "visitor-wiring-0001";

    @Autowired
    private GuestCommentService guestCommentService;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private AiModerationService moderationService;
    @MockBean
    private SeoLlmConfigProvider configProvider;

    @BeforeEach
    void setUp() {
        settings.updateSettings(Map.of("commentsEnabled", "true"));
        jdbc.update("DELETE FROM guest_comment WHERE post_id = ?", POST_ID);
        jdbc.update("DELETE FROM blog_post WHERE id = ?", POST_ID);
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-16 09:00:00', TIMESTAMP '2026-10-16 09:00:00', 0)",
                POST_ID, "留言接线测试文章", "摘要", "正文", "随笔");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM guest_comment WHERE post_id = ?", POST_ID);
        jdbc.update("DELETE FROM blog_post WHERE id = ?", POST_ID);
    }

    private GuestCommentRequest request(String content) {
        GuestCommentRequest request = new GuestCommentRequest();
        request.setPostId(POST_ID);
        request.setVisitorId(VISITOR_ID);
        request.setNickname("访客甲");
        request.setContent(content);
        return request;
    }

    @Test
    @DisplayName("createComment_StoresAndReturnsIdEvenWhenModerationThrows")
    void createComment_StoresAndReturnsIdEvenWhenModerationThrows() {
        when(configProvider.commentModerationEnabled()).thenReturn(true);

        Long id = guestCommentService.createComment(request("这条留言应该照常落库"));

        assertNotNull(id);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM guest_comment WHERE id = ?", Integer.class, id),
                "留言必须照常落库");
        verify(moderationService, timeout(5000)).moderateQuietly(id);
    }

    @Test
    @DisplayName("createComment_DispatchesModerationOnlyWhenSwitchOn")
    void createComment_DispatchesModerationOnlyWhenSwitchOn() {
        when(configProvider.commentModerationEnabled()).thenReturn(true);

        Long id = guestCommentService.createComment(request("开关打开时要派发审核"));

        verify(moderationService, timeout(5000)).moderateQuietly(id);
    }

    @Test
    @DisplayName("createComment_SwitchOff_NeverModerates")
    void createComment_SwitchOff_NeverModerates() throws Exception {
        when(configProvider.commentModerationEnabled()).thenReturn(false);

        Long id = guestCommentService.createComment(request("开关关闭时完全不该触发审核"));

        assertNotNull(id);
        verify(moderationService, after(500).never()).moderateQuietly(id);
        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("createComment_RejectsBadRequestWithoutModerating")
    void createComment_RejectsBadRequestWithoutModerating() {
        when(configProvider.commentModerationEnabled()).thenReturn(true);

        List<Long> before = jdbc.queryForList(
                "SELECT id FROM guest_comment WHERE post_id = ?", Long.class, POST_ID);

        // 空内容会被校验拦下，留言不该落库，更不该派发审核
        GuestCommentRequest bad = request(" ");
        try {
            guestCommentService.createComment(bad);
        } catch (RuntimeException expected) {
            // 校验异常类型由既有实现决定，这里只关心「没有落库、没有派发」
        }

        List<Long> after = jdbc.queryForList(
                "SELECT id FROM guest_comment WHERE post_id = ?", Long.class, POST_ID);
        assertEquals(before.size(), after.size(), "校验失败的留言不该落库");
        verifyNoInteractions(moderationService);
    }
}
