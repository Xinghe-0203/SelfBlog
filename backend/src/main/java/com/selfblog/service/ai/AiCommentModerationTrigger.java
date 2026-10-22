package com.selfblog.service.ai;

import com.selfblog.service.seo.SeoLlmConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 访客留言落库后的自动审核触发器。
 *
 * <p>照搬 {@code SeoAutoGenerationTrigger} 的两段式结构，原因同样承重：</p>
 * <ol>
 *   <li><b>必须等事务提交后再审核</b>：留言还在未提交事务里时另起线程去读，H2/MySQL 都可能
 *       读不到这一行，甚至（MySQL）在唯一索引或行锁上互相等待；</li>
 *   <li><b>必须是独立 Bean</b>：{@code @Async} 靠 Spring 代理生效，写在本类内部自调用会绕过
 *       代理，变成同步执行——那就等于让访客的提交去等一次模型调用。</li>
 * </ol>
 *
 * <p>所有异常都在 {@link AiModerationService#moderateQuietly(Long)} 内吞掉，这里再兜一层，
 * 保证审核永远不影响留言本身。</p>
 */
@Slf4j
@Component
public class AiCommentModerationTrigger {

    private final AiModerationService moderationService;
    private final SeoLlmConfigProvider configProvider;
    private final AiCommentModerationTrigger self;

    public AiCommentModerationTrigger(AiModerationService moderationService,
                                      SeoLlmConfigProvider configProvider,
                                      @Lazy AiCommentModerationTrigger self) {
        this.moderationService = moderationService;
        this.configProvider = configProvider;
        this.self = self;
    }

    /**
     * 在当前事务提交后异步审核；不在事务中时直接异步审核。
     *
     * <p>先做一次总开关判定：关闭时连事务同步器都不注册，省掉一次无用的回调。</p>
     */
    public void fireAfterCommit(Long commentId) {
        if (commentId == null) {
            return;
        }
        try {
            if (!configProvider.commentModerationEnabled()) {
                return;
            }
        } catch (Exception e) {
            log.warn("[AI] 读取留言自动审核开关失败，跳过审核：{}", e.getClass().getSimpleName());
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.moderateQuietly(commentId);
                }
            });
            return;
        }
        self.moderateQuietly(commentId);
    }

    /** 异步执行入口；异常只记日志。 */
    @Async("taskExecutor")
    public void moderateQuietly(Long commentId) {
        try {
            moderationService.moderateQuietly(commentId);
        } catch (Exception e) {
            log.warn("[AI] 留言 {} 自动审核异常（已忽略）：{}", commentId, e.getClass().getSimpleName());
        }
    }
}
