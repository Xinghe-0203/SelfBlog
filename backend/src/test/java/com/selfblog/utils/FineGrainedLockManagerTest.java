package com.selfblog.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 回归：细粒度锁管理器必须为同一个 key 永久返回同一个监视器对象。
 *
 * 历史缺陷：getLock 在条目数达到 MAX_LOCKS_SIZE(10000) 时会淘汰最旧的条目，
 * 而被淘汰的条目可能正被另一个线程持有在 synchronized 里。淘汰后 compute 会为
 * 同一个 key 新建一个 Object，第二个调用者于是拿到不同的监视器，
 * 两个线程同时进入同一段临界区（互斥失效）。
 *
 * 现在实现不再淘汰任何条目，本测试同时验证「同一 key 稳定」与
 * 「并发拿到的监视器一致」。
 */
@DisplayName("FineGrainedLockManager 监视器稳定性")
class FineGrainedLockManagerTest {

    @Test
    @DisplayName("同一个 key 永远返回同一个监视器")
    void sameKeyAlwaysReturnsSameMonitor() {
        FineGrainedLockManager manager = FineGrainedLockManager.getInstance();
        Object first = manager.getLock("lock-stability-key");
        Object second = manager.getLock("lock-stability-key");

        assertSame(first, second, "同一个 lockKey 必须返回同一个对象，否则 synchronized 无法互斥");
    }

    @Test
    @DisplayName("大量不同 key 之后，原有 key 的监视器仍然不变")
    void monitorSurvivesManyOtherKeys() {
        FineGrainedLockManager manager = FineGrainedLockManager.getInstance();
        Object before = manager.getLock("lock-eviction-probe");

        // 制造远超旧实现 MAX_LOCKS_SIZE(10000) 的 key 数量：旧实现会在这里淘汰掉 probe
        for (int i = 0; i < 20000; i++) {
            manager.getLock("lock-filler-" + i);
        }

        assertSame(before, manager.getLock("lock-eviction-probe"),
                "条目数量增长后，已有 key 的监视器不能被替换");
    }

    @Test
    @DisplayName("并发拿到的监视器一致且临界区互斥")
    void concurrentCallersShareTheSameMonitor() throws Exception {
        FineGrainedLockManager manager = FineGrainedLockManager.getInstance();
        String key = "lock-concurrency-key";

        int threads = 16;
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        AtomicReference<Object> seenMonitor = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Object> monitors = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        Object monitor = manager.getLock(key);
                        synchronized (monitor) {
                            synchronized (monitors) {
                                monitors.add(monitor);
                            }
                            int now = inside.incrementAndGet();
                            maxInside.accumulateAndGet(now, Math::max);
                            Thread.sleep(5);
                            inside.decrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "并发任务应在超时前完成");
        } finally {
            pool.shutdownNow();
        }

        assertFalse(monitors.isEmpty());
        Object expected = monitors.get(0);
        for (Object monitor : monitors) {
            assertSame(expected, monitor, "所有线程必须拿到同一个监视器");
        }
        assertEquals(1, maxInside.get(), "同一 key 的临界区必须互斥，最多一个线程在内部");
    }
}
