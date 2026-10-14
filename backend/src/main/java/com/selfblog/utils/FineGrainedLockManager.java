package com.selfblog.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 细粒度锁管理器
 * 用于解决同一用户对同一资源的并发操作问题（如点赞、收藏、关注等）
 * 使用带过期时间的LRU缓存，避免内存泄漏
 */
public class FineGrainedLockManager {

    private static final int MAX_LOCKS_SIZE = 10000;
    private static final long LOCK_EXPIRE_MS = 300000; // 5分钟

    private final ConcurrentMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    private static class LockEntry {
        final Object lock;
        volatile long lastAccessTime;

        LockEntry(Object lock) {
            this.lock = lock;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * 获取锁对象
     * 当锁数量超过MAX_LOCKS_SIZE时，触发主动清理移除最久未访问的锁
     * 注意：锁不会过期，因为过期可能导致synchronized互斥失效
     * @param lockKey 锁的key
     * @return 锁对象
     */
    public Object getLock(String lockKey) {
        // 主动清理过多的锁，防止内存泄漏
        if (locks.size() >= MAX_LOCKS_SIZE) {
            // 移除最久未访问的条目（LRU淘汰）
            locks.entrySet().stream()
                .min((a, b) -> Long.compare(a.getValue().lastAccessTime, b.getValue().lastAccessTime))
                .ifPresent(oldest -> locks.remove(oldest.getKey()));
        }

        // 使用compute原子操作确保线程安全
        LockEntry entry = locks.compute(lockKey, (key, existing) -> {
            if (existing == null) {
                return new LockEntry(new Object());
            }
            existing.touch();
            return existing;
        });
        return entry.lock;
    }

    private static final FineGrainedLockManager INSTANCE = new FineGrainedLockManager();

    public static FineGrainedLockManager getInstance() {
        return INSTANCE;
    }
}
