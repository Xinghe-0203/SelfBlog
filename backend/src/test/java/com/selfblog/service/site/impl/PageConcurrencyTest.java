package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.PageConfig;
import com.selfblog.service.site.PageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Real independent transactions, not a sequential stale-version simulation. H2 only. */
@SpringBootTest
@ActiveProfiles("test")
class PageConcurrencyTest {
    @Autowired PageService pages;

    @Test void twoWindowsCannotBothSaveTheSameRevision() throws Exception {
        var created = pages.createPage("race-" + UUID.randomUUID(), "并发测试");
        var start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> write = () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                try { pages.saveDraft(created.id(), created.draft().revision(), null, new PageConfig("来自窗口" + Thread.currentThread().threadId(), null, null, null, null, null, null)); return 200; }
                catch (BusinessException e) { return e.getCode(); }
            };
            var first = workers.submit(write); var second = workers.submit(write); start.countDown();
            var results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(code -> code == 200).count());
            assertEquals(1, results.stream().filter(code -> code == 409).count());
            assertEquals(created.draft().revision() + 1, pages.getEditor(created.id()).draft().revision());
        } finally {
            workers.shutdownNow();
            var current = pages.getEditor(created.id()); pages.deletePage(created.id(), current.draft().revision());
        }
    }

    @Test void simultaneousCustomPageCreationReservesTheSlugOnce() throws Exception {
        String slug = "unique-" + UUID.randomUUID();
        var start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        var ids = new ConcurrentLinkedQueue<String>();
        try {
            Callable<Integer> create = () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                try { ids.add(pages.createPage(slug, "唯一地址").id()); return 200; }
                catch (BusinessException e) { return e.getCode(); }
            };
            var first = workers.submit(create); var second = workers.submit(create); start.countDown();
            var results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(code -> code == 200).count());
            assertEquals(1, results.stream().filter(code -> code == 409).count());
            assertEquals(1, ids.size());
        } finally {
            workers.shutdownNow();
            for (String id : ids) { var current = pages.getEditor(id); pages.deletePage(id, current.draft().revision()); }
        }
    }
}
