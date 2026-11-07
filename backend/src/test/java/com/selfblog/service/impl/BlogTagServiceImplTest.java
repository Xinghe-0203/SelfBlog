package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.BlogTag;
import com.selfblog.mapper.BlogTagMapper;
import com.selfblog.service.post.BlogTagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * BlogTagServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogTagServiceImplTest {

    @Autowired
    private BlogTagService blogTagService;

    @Autowired
    private BlogTagMapper blogTagMapper;

    @Autowired
    private CacheManager caches;

    @BeforeEach
    void setUp() {
        blogTagMapper.delete(null);
        clearTagCaches();
    }

    @AfterEach
    void tearDown() {
        clearTagCaches();
    }

    /**
     * listAllTags 现在带 @Cacheable(hotTagsCache)，标签写路径带 @CacheEvict。
     * 测试 profile 下自定义 CacheManager 依然生效（覆盖 spring.cache.type=none）
     * 且缓存不参与事务回滚，所以标签相关的两级缓存都要手动清——这也顺带验证了
     * 「写路径的 allEntries 失效注解 + 读路径缓存键」的装配在真实代理下工作正常
     * （上面的 createThenListReflectsTheNewTag 用例就是经真实代理跑通的）。
     */
    private void clearTagCaches() {
        for (String name : new String[] {CaffeineCacheConfig.HOT_TAGS_CACHE, CaffeineCacheConfig.CATEGORY_CACHE}) {
            var cache = caches.getCache(name);
            if (cache != null) cache.clear();
        }
    }

    private BlogTag createTag(String name) {
        BlogTag tag = new BlogTag();
        tag.setName(name);
        blogTagMapper.insert(tag);
        return tag;
    }

    @Test
    @DisplayName("获取所有标签列表")
    void listAllTags_Success() {
        createTag("Java");
        createTag("Vue");

        List<BlogTag> tags = blogTagService.listAllTags();
        assertNotNull(tags);
        assertEquals(2, tags.size());
    }

    @Test
    @DisplayName("获取所有标签 - 空列表")
    void listAllTags_Empty() {
        List<BlogTag> tags = blogTagService.listAllTags();
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("根据ID获取标签")
    void getById_Success() {
        BlogTag created = createTag("Java");
        BlogTag found = blogTagService.getById(created.getId());

        assertNotNull(found);
        assertEquals("Java", found.getName());
    }

    @Test
    @DisplayName("搜索标签")
    void searchTags_Success() {
        createTag("Java基础");
        createTag("JavaScript");
        createTag("Python");

        List<BlogTag> results = blogTagService.searchTags("Java");
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("搜索标签 - 无匹配返回空")
    void searchTags_NoMatch() {
        createTag("Python");

        List<BlogTag> results = blogTagService.searchTags("Java");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("创建标签成功")
    void createTag_Success() {
        BlogTag tag = blogTagService.createTag("Spring Boot");
        assertNotNull(tag);
        assertNotNull(tag.getId());
        assertEquals("Spring Boot", tag.getName());
    }

    @Test
    @DisplayName("创建重复标签抛异常")
    void createTag_Duplicate_ThrowsException() {
        blogTagService.createTag("Java");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> blogTagService.createTag("Java"));
        assertTrue(ex.getCode() >= 400);
    }

    @Test
    @DisplayName("创建后列表立刻可见（写路径失效缓存，读路径回填）")
    void createThenListReflectsTheNewTag() {
        blogTagService.createTag("缓存验证标签");

        List<String> names = blogTagService.listAllTags().stream().map(BlogTag::getName).toList();

        assertTrue(names.contains("缓存验证标签"),
                "createTag 必须失效 hotTagsCache，listAllTags 才能立刻看到新标签");
    }

    @Test
    @DisplayName("软删除后列表不再包含该标签（deleteTag 失效缓存）")
    void deleteThenListNoLongerContainsTheTag() {
        // deleteTag 要求管理员身份（与 BlogTagLifecycleTest 同一构造方式）。
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new com.selfblog.utils.UserContext(1L, "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        try {
            BlogTag tag = blogTagService.createTag("待删除标签");

            blogTagService.deleteTag(tag.getId());

            List<String> names = blogTagService.listAllTags().stream().map(BlogTag::getName).toList();
            assertFalse(names.contains("待删除标签"),
                    "deleteTag（软删除）必须失效 hotTagsCache，否则列表在 TTL 内仍显示已删标签");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
