package com.selfblog.service.seo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 发布后自动生成 SEO 的异步触发器。
 *
 * <p><b>为什么必须是独立 Bean：</b>{@code @Async} 依赖 Spring 代理，若把
 * {@code @Async} 方法放在 {@link SeoGenerationService} 内部由自己调用，属于自调用，
 * 代理被绕过、任务会同步跑在发布线程里 —— 那样模型一慢，管理员保存文章就要等几十秒。
 * 因此这里单独成类，由发布流程注入并调用。</p>
 *
 * <p><b>为什么要在事务提交后再派发：</b>发布流程是 {@code @Transactional} 的，
 * 异步线程如果立刻开跑，很可能读到尚未提交的文章（看不到 → 直接放弃），
 * 或者读到旧内容。所以 {@link #fireAfterCommit(Long)} 在事务活跃时把异步派发挂到
 * {@code afterCommit} 回调上；这没有改变任何事务语义，只是把「异步开跑」推迟到提交之后。</p>
 *
 * <p><b>失败语义：</b>{@link #generateQuietly(Long)} 吞掉所有异常（内部只记 WARN）。
 * 自动生成是「锦上添花」，任何模型/网络/解析问题都不允许影响文章发布本身。</p>
 */
@Slf4j
@Component
public class SeoAutoGenerationTrigger {

    private final SeoGenerationService generationService;
    private final SeoLlmConfigProvider configProvider;
    private final SeoAutoGenerationTrigger self;

    public SeoAutoGenerationTrigger(SeoGenerationService generationService,
                                    SeoLlmConfigProvider configProvider,
                                    @Lazy SeoAutoGenerationTrigger self) {
        this.generationService = generationService;
        this.configProvider = configProvider;
        this.self = self;
    }

    /**
     * 发布流程调用入口：只有「已启用 + 开启发布后自动生成 + 接口地址与 Key 齐全」才派发。
     *
     * <p>任何配置读取失败都只记日志，绝不让发布流程感知。</p>
     */
    public void fireAfterCommit(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            if (!configProvider.current().autoEnabled()) {
                return;
            }
        } catch (Exception e) {
            log.warn("[SEO] 读取自动生成配置失败，跳过自动生成：postId={}, error={}", postId, e.getClass().getSimpleName());
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.generateQuietly(postId);
                }
            });
        } else {
            self.generateQuietly(postId);
        }
    }

    /**
     * 异步生成（{@code taskExecutor} 是项目既有的线程池 Bean）。
     *
     * <p>线程池队列满时 {@code CallerRunsPolicy} 会让任务回落到调用线程同步执行 ——
     * 这是 AsyncConfig 的既有策略，代价是极端情况下发布请求会多等一次模型调用，
     * 但换来的是「任务不丢」。生成失败仍然不影响保存。</p>
     */
    @Async("taskExecutor")
    public void generateQuietly(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            generationService.generateQuietly(postId);
        } catch (Exception e) {
            // generateQuietly 内部已兜底；这里再兜一层，防止意外异常打断线程池任务。
            log.warn("[SEO] 自动生成任务异常：postId={}, error={}", postId, e.getClass().getSimpleName());
        }
    }
}
