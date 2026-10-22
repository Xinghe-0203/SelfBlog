package com.selfblog.service.seo;

import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 发布后自动生成 SEO 的派发语义。
 *
 * <p>上游服务与配置读取都被替换成 mock，只验证「什么时候派发、什么时候不派发」：
 * 开关关闭、配置不全、读取配置异常都不得调用模型；派发必须异步；
 * 有事务时必须等提交后才派发（否则异步线程读不到还没提交的文章）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SeoAutoGenerationTrigger")
class SeoAutoGenerationTriggerTest {

    private static final long POST_ID = 884001L;
    private static final long THROWING_ID = 884002L;

    @Autowired
    private SeoAutoGenerationTrigger trigger;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private SeoGenerationService generationService;
    @MockBean
    private SeoLlmConfigProvider configProvider;

    @BeforeEach
    void setUp() {
        // 默认：完全没配置，任何派发都该被跳过
        when(configProvider.current()).thenReturn(new SeoLlmConfig(false, false, "", "", "", 30));
    }

    @AfterEach
    void tearDown() {
        // @MockBean 的 mock 由 Spring 在用例间重置，这里只做显式说明，避免后续读者误以为会串味
    }

    private void configured(boolean enabled, boolean autoOnPublish, String apiKey) {
        when(configProvider.current()).thenReturn(
                new SeoLlmConfig(enabled, autoOnPublish, "http://127.0.0.1:9/v1", apiKey, "gpt-4o-mini", 10));
    }

    /** 等异步任务真的跑到 mock 上（@Async 走的是另一个线程）。 */
    private void awaitGenerate(long postId) {
        verify(generationService, timeout(5000)).generateQuietly(postId);
    }

    @Test
    @DisplayName("fireAfterCommit_SkipsWhenDisabled")
    void fireAfterCommit_SkipsWhenDisabled() throws Exception {
        configured(false, true, "sk-key");

        trigger.fireAfterCommit(POST_ID);
        Thread.sleep(300);

        verifyNoInteractions(generationService);
    }

    @Test
    @DisplayName("fireAfterCommit_SkipsWhenAutoOnPublishOff")
    void fireAfterCommit_SkipsWhenAutoOnPublishOff() throws Exception {
        configured(true, false, "sk-key");

        trigger.fireAfterCommit(POST_ID);
        Thread.sleep(300);

        verifyNoInteractions(generationService);
    }

    @Test
    @DisplayName("fireAfterCommit_SkipsWhenNotConfigured")
    void fireAfterCommit_SkipsWhenNotConfigured() throws Exception {
        // 开着自动生成，但没有 Key：模型调用必然失败，不该白白派发
        configured(true, true, "");

        trigger.fireAfterCommit(POST_ID);
        Thread.sleep(300);

        verifyNoInteractions(generationService);
    }

    @Test
    @DisplayName("fireAfterCommit_DispatchesWhenFullyEnabled")
    void fireAfterCommit_DispatchesWhenFullyEnabled() {
        configured(true, true, "sk-key");

        trigger.fireAfterCommit(POST_ID);

        awaitGenerate(POST_ID);
    }

    @Test
    @DisplayName("fireAfterCommit_IgnoresNullPostId")
    void fireAfterCommit_IgnoresNullPostId() throws Exception {
        configured(true, true, "sk-key");

        trigger.fireAfterCommit(null);
        Thread.sleep(300);

        verifyNoInteractions(generationService);
    }

    @Test
    @DisplayName("fireAfterCommit_ToleratesBrokenConfigProvider")
    void fireAfterCommit_ToleratesBrokenConfigProvider() throws Exception {
        when(configProvider.current()).thenThrow(new IllegalStateException("settings boom"));

        assertDoesNotThrow(() -> trigger.fireAfterCommit(POST_ID));
        Thread.sleep(300);
        verifyNoInteractions(generationService);
    }

    @Test
    @DisplayName("fireAfterCommit_DefersUntilTransactionCommits")
    void fireAfterCommit_DefersUntilTransactionCommits() {
        configured(true, true, "sk-key");
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        template.execute(status -> {
            trigger.fireAfterCommit(POST_ID);
            // 事务还没提交：异步线程此刻读不到这篇文章，必须等到 afterCommit
            verify(generationService, never()).generateQuietly(anyLong());
            return null;
        });

        awaitGenerate(POST_ID);
    }

    @Test
    @DisplayName("fireAfterCommit_OutsideTransactionDispatchesImmediately")
    void fireAfterCommit_OutsideTransactionDispatchesImmediately() {
        configured(true, true, "sk-key");

        // 没有事务时直接派发（例如未来在非事务路径调用）
        trigger.fireAfterCommit(POST_ID);

        awaitGenerate(POST_ID);
    }

    @Test
    @DisplayName("generateQuietly_SwallowsFailures")
    void generateQuietly_SwallowsFailures() {
        doThrow(new IllegalStateException("upstream exploded")).when(generationService).generateQuietly(THROWING_ID);

        assertDoesNotThrow(() -> trigger.generateQuietly(THROWING_ID));

        verify(generationService, timeout(5000)).generateQuietly(THROWING_ID);
    }

    @Test
    @DisplayName("generateQuietly_IgnoresNullPostId")
    void generateQuietly_IgnoresNullPostId() throws Exception {
        trigger.generateQuietly(null);
        Thread.sleep(300);

        verifyNoInteractions(generationService);
    }
}
