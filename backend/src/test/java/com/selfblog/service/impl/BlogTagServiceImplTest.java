package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogTag;
import com.selfblog.mapper.BlogTagMapper;
import com.selfblog.service.post.BlogTagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @BeforeEach
    void setUp() {
        blogTagMapper.delete(null);
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
}
