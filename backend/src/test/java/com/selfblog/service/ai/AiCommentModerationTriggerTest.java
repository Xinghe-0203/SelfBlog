package com.selfblog.service.ai;

import com.selfblog.service.seo.SeoLlmConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 留言自动审核的派发语义。
 *
 * <p>只验证「什么时候派发、什么时候不派发」，模型调用本身被 mock 掉：</p>
 * <ul>
 *   <li>总开关关闭 → 连事务同步器都不注册，一次模型调用都不该有；</li>
 *   <li>有事务 → 必须等提交后才派发（否则异步线程读不到还没提交的留言）；</li>
 *   <li>派发必须异步（{@code @Async} 靠代理，本类独立 Bean 才有意义）；</li>
 *   <li>配置读取异常、审核异常都必须吞掉，绝不能让留言提交失败。</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AiCommentModerationTrigger")
class AiCommentModerationTriggerTest {

    private static final long COMMENT_ID = 886001L;

    @Autowired
    private AiCommentModerationTrigger trigger;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private AiModerationService moderationService;
    @MockBean
    private SeoLlmConfigProvider configProvider;

    @BeforeEach
    void setUp() {
        // 默认：总开关关闭
        when(configProvider.commentModerationEnabled()).thenReturn(false);
    }

    private void awaitModerate(long commentId) {
        verify(moderationService, timeout(5000)).moderateQuietly(commentId);
    }

    @Test
    @DisplayName("fireAfterCommit_SkipsWhenSwitchOff")
    void fireAfterCommit_SkipsWhenSwitchOff() throws Exception {
        trigger.fireAfterCommit(COMMENT_ID);
        Thread.sleep(300);

        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("fireAfterCommit_DispatchesWhenSwitchOn")
    void fireAfterCommit_DispatchesWhenSwitchOn() {
        when(configProvider.commentModerationEnabled()).thenReturn(true);

        trigger.fireAfterCommit(COMMENT_ID);

        awaitModerate(COMMENT_ID);
    }

    @Test
    @DisplayName("fireAfterCommit_IgnoresNullCommentId")
    void fireAfterCommit_IgnoresNullCommentId() throws Exception {
        when(configProvider.commentModerationEnabled()).thenReturn(true);

        trigger.fireAfterCommit(null);
        Thread.sleep(300);

        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("fireAfterCommit_ToleratesBrokenConfigProvider")
    void fireAfterCommit_ToleratesBrokenConfigProvider() throws Exception {
        when(configProvider.commentModerationEnabled()).thenThrow(new IllegalStateException("settings boom"));

        assertDoesNotThrow(() -> trigger.fireAfterCommit(COMMENT_ID));
        Thread.sleep(300);

        verifyNoInteractions(moderationService);
    }

    @Test
    @DisplayName("fireAfterCommit_DefersUntilTransactionCommits")
    void fireAfterCommit_DefersUntilTransactionCommits() {
        when(configProvider.commentModerationEnabled()).thenReturn(true);
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        template.execute(status -> {
            trigger.fireAfterCommit(COMMENT_ID);
            // 事务还没提交：此刻异步线程读不到这条留言，必须等到 afterCommit
            verify(moderationService, never()).moderateQuietly(anyLong());
            return null;
        });

        awaitModerate(COMMENT_ID);
    }

    @Test
    @DisplayName("fireAfterCommit_OutsideTransactionDispatchesImmediately")
    void fireAfterCommit_OutsideTransactionDispatchesImmediately() {
        when(configProvider.commentModerationEnabled()).thenReturn(true);

        trigger.fireAfterCommit(COMMENT_ID);

        awaitModerate(COMMENT_ID);
    }

    @Test
    @DisplayName("moderateQuietly_SwallowsModerationFailure")
    void moderateQuietly_SwallowsModerationFailure() {
        doThrow(new IllegalStateException("model exploded"))
                .when(moderationService).moderateQuietly(any());

        assertDoesNotThrow(() -> trigger.moderateQuietly(COMMENT_ID));

        verify(moderationService, timeout(5000)).moderateQuietly(COMMENT_ID);
    }
}
