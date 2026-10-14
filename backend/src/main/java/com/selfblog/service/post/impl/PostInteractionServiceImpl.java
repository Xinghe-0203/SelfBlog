package com.selfblog.service.post.impl;

import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.post.PostInteractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 帖子交互服务实现 — 负责点赞、收藏、浏览计数
 */
@Slf4j
@Service
public class PostInteractionServiceImpl implements PostInteractionService {

    @Autowired
    private BlogPostMapper blogPostMapper;

    private static final int MAX_VIEW_COUNT_CACHE_SIZE = 10000;
    private final ConcurrentMap<String, AtomicLong> viewCountCache = new ConcurrentHashMap<>();
    private static final long VIEW_COUNT_INTERVAL_MS = 60000;
    private static final long CACHE_EXPIRE_MS = 3600000;

    private String getUserIdentifier(Long userId, String ip, String userAgent) {
        if (userId != null) {
            return "user-" + userId;
        }
        if (ip == null || ip.isEmpty()) {
            ip = "unknown";
        }
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "unknown";
        }
        return "guest-" + ip + "-" + userAgent.hashCode();
    }

    private void cleanupViewCountCache() {
        long now = System.currentTimeMillis();
        viewCountCache.entrySet().removeIf(entry ->
            entry.getValue().get() < now - CACHE_EXPIRE_MS
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long postId, String userKey) {
        if (viewCountCache.size() >= MAX_VIEW_COUNT_CACHE_SIZE) {
            cleanupViewCountCache();
        }
        String cacheKey = userKey + "-" + postId;
        long now = System.currentTimeMillis();
        AtomicLong lastViewTime = viewCountCache.computeIfAbsent(cacheKey, k -> new AtomicLong(0));

        while (true) {
            long lastTime = lastViewTime.get();
            if (now - lastTime < VIEW_COUNT_INTERVAL_MS) {
                return;
            }
            if (lastViewTime.compareAndSet(lastTime, now)) {
                break;
            }
        }

        cleanupViewCountCache();
        blogPostMapper.incrementViewCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long postId) {
        blogPostMapper.incrementViewCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementLikeCount(Long postId) {
        blogPostMapper.incrementLikeCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementLikeCount(Long postId) {
        blogPostMapper.decrementLikeCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementCommentCount(Long postId) {
        blogPostMapper.incrementCommentCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementCommentCount(Long postId, int count) {
        blogPostMapper.decrementCommentCount(postId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementCollectCount(Long postId) {
        blogPostMapper.incrementCollectCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementCollectCount(Long postId) {
        blogPostMapper.decrementCollectCount(postId);
    }
}
