package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.site.PageService;
import com.selfblog.service.site.seo.SitemapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * C7 回归：读方法在"找不到"时抛 404，绝不能让共享事务被标记 rollback-only。
 *
 * <p>缺陷形状：{@code PageServiceImpl} / {@code SitemapService} 曾带类级
 * {@code @Transactional(readOnly = true)}。它们的读方法在目标不存在时抛
 * {@code BusinessException(404)}，异常穿过事务边界时 Spring 会把<b>正在参与的外层事务</b>
 * 标记为 rollback-only（{@code globalRollbackOnParticipationFailure} 默认为 true）。
 * 调用方捕获了这个 404、以为只是"没找到"，外层事务提交时却抛
 * {@code UnexpectedRollbackException}，对外表现为 500 —— 一个正常的 404 变成了服务器错误。
 *
 * <p>因此本类<b>刻意不加</b> {@code @Transactional}：必须真的提交一次外层事务，才能观察到
 * rollback-only 的后果。每个用例在一个 {@link TransactionTemplate} 里调用读方法、捕获 404，
 * 然后让模板正常提交；提交不抛异常即为通过。
 *
 * <p>修复前该用例在 {@code template.execute(...)} 处抛
 * {@code UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only}。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("站点读方法抛 404 不污染外层事务（C7）")
class NotFoundInsideTransactionTest {

    private static final String BASE = "https://blog.example.com";

    @Autowired PageService pages;
    @Autowired SitemapService sitemaps;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM site_setting WHERE setting_key LIKE 'page.%'");
        jdbc.update("DELETE FROM blog_post");
    }

    @Test
    @DisplayName("getBySlug 的 404 不会把外层事务标记为 rollback-only")
    void missingSlugDoesNotPoisonTheSurroundingTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        BusinessException thrown = assertDoesNotRollback(template,
                () -> pages.getBySlug("no-such-page"));

        assertEquals(404, thrown.getCode(), "未命中的 slug 必须是业务 404，而不是提交期 500");
    }

    @Test
    @DisplayName("getPublished 的 404 不会把外层事务标记为 rollback-only")
    void unknownPageIdDoesNotPoisonTheSurroundingTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        BusinessException thrown = assertDoesNotRollback(template,
                () -> pages.getPublished("custom-0123456789abcdef0123456789abcdef"));

        assertEquals(404, thrown.getCode());
    }

    @Test
    @DisplayName("站点地图分片越界的 404 不会把外层事务标记为 rollback-only")
    void missingSitemapShardDoesNotPoisonTheSurroundingTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        BusinessException thrown = assertDoesNotRollback(template,
                () -> sitemaps.shard("posts", 7, BASE));

        assertEquals(404, thrown.getCode());
    }

    @Test
    @DisplayName("控制器常见形态：捕获 404 后继续读取，外层事务仍能提交")
    void catchingTheNotFoundAndContinuingStillCommits() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        String manifest = template.execute(status -> {
            try {
                pages.getBySlug("still-missing");
            } catch (BusinessException notFound) {
                assertEquals(404, notFound.getCode());
            }
            return pages.getManifest().navigation().isEmpty() ? "empty" : "navigation";
        });

        assertNotNull(manifest, "外层事务必须提交成功并带回读结果");
    }

    /**
     * 在外层事务中执行一次会抛 404 的读调用，捕获异常后正常提交外层事务。
     * 提交若因 rollback-only 抛 {@code UnexpectedRollbackException}，本方法直接失败。
     */
    private BusinessException assertDoesNotRollback(TransactionTemplate template, Runnable action) {
        BusinessException[] captured = new BusinessException[1];
        template.execute(status -> {
            try {
                action.run();
            } catch (BusinessException e) {
                captured[0] = e;
            }
            return null;
        });
        assertNotNull(captured[0], "读方法本应抛业务异常，但什么都没抛");
        return captured[0];
    }
}
