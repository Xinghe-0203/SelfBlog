package com.selfblog.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 细粒度锁管理器
 * 用于解决同一用户对同一资源的并发操作问题（如点赞、收藏、关注等）
 *
 * 互斥完全依赖 {@code synchronized} 的监视器对象，因此同一个 lockKey 必须始终返回同一个监视器。
 * 旧实现会在条目数达到上限时淘汰“最久未访问”的条目，而被淘汰的条目可能正被另一个线程持有，
 * 随后的 compute 会为同一 key 创建新的监视器，两个线程于是同时进入临界区（互斥失效）。
 *
 * 这里刻意不做任何淘汰：lockKey 由数据库主键推导（userId-postId、followerId-followingId），
 * 在单作者博客中其规模受实际用户数与文章数约束，不会无界增长；而“永不淘汰”是保证监视器
 * 身份稳定的唯一简单办法。此前的 LOCK_EXPIRE_MS 常量从未被使用，已一并移除。
 */
public class FineGrainedLockManager {

    private final ConcurrentMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    private static class LockEntry {
        final Object lock = new Object();
    }

    /**
     * 获取锁对象
     * 同一个 lockKey 永远返回同一个监视器对象（computeIfAbsent 不会替换已有条目）。
     * @param lockKey 锁的key
     * @return 锁对象
     */
    public Object getLock(String lockKey) {
        return locks.computeIfAbsent(lockKey, key -> new LockEntry()).lock;
    }

    private static final FineGrainedLockManager INSTANCE = new FineGrainedLockManager();

    public static FineGrainedLockManager getInstance() {
        return INSTANCE;
    }
}
