package com.selfblog.service.post;

import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.post.BlogTagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 标签的“逻辑删除 + 唯一名称”生命周期。
 *
 * <p>真实环境 blog_tag.name 唯一；删除标签是逻辑删除（is_deleted=1）。
 * 之后重建同名标签时，如果只捕获唯一键冲突再按“未删除”条件查询，就会拿到 null，
 * 表现为创建接口 500、发文章时标签丢失。这里要求重建必须恢复原来的行。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogTagLifecycleTest {
    @Autowired BlogTagService tags;
    @Autowired CacheManager caches;

    @BeforeEach void asAdmin() {
        // 管理员判定基于 UserContext.role，而不是 Long principal。
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new com.selfblog.utils.UserContext(1L, "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        clearCache();
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        clearCache();
    }

    private void clearCache() {
        var cache = caches.getCache(CaffeineCacheConfig.CATEGORY_CACHE);
        if (cache != null) cache.clear();
    }

    @Test
    void recreatingADeletedTagNameRestoresTheOriginalRow() {
        var created = tags.createTag("循环标签");
        assertNotNull(created);
        tags.deleteTag(created.getId());

        var recreated = tags.createTag("循环标签");

        assertNotNull(recreated, "同名重建不应返回 null");
        assertEquals(created.getId(), recreated.getId(), "应恢复原行而不是插入新行");
        assertTrue(tags.listAllTags().stream().anyMatch(tag -> "循环标签".equals(tag.getName())), "恢复后应重新可见");
    }

    @Test
    void publishingFlowCanReuseADeletedTagName() {
        Long originalId = tags.getOrCreateTag("发布标签");
        tags.deleteTag(originalId);

        Long restoredId = tags.getOrCreateTag("发布标签");

        assertEquals(originalId, restoredId, "发文章时输入已删除的标签名应恢复该标签");
    }

    @Test
    void listAllTagsNeverExposesDeletedNames() {
        var tag = tags.createTag("隐藏标签");
        tags.deleteTag(tag.getId());
        List<String> names = tags.listAllTags().stream().map(com.selfblog.entity.BlogTag::getName).toList();
        assertTrue(!names.contains("隐藏标签"));
    }
}
