package com.selfblog.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.Topic;
import com.selfblog.mapper.TopicMapper;
import com.selfblog.service.content.TopicService;
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
 * TopicServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TopicServiceImplTest {

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicMapper topicMapper;

    @BeforeEach
    void setUp() {
        topicMapper.delete(null);
    }

    private Topic createTopic(String name, String desc, int score) {
        Topic t = new Topic();
        t.setName(name);
        t.setDescription(desc);
        t.setTrendingScore(score);
        t.setStatus(1);
        t.setPostCount(0);
        topicMapper.insert(t);
        return t;
    }

    @Test
    @DisplayName("创建话题成功")
    void createTopic_Success() {
        Long topicId = topicService.createTopic("Java", "Java编程语言");

        assertNotNull(topicId);
        Topic saved = topicMapper.selectById(topicId);
        assertEquals("Java", saved.getName());
    }

    @Test
    @DisplayName("创建话题 - 名称为空抛400")
    void createTopic_EmptyName_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.createTopic("", "描述"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("创建话题 - 名称超长抛400")
    void createTopic_NameTooLong_ThrowsException() {
        String longName = "a".repeat(51);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.createTopic(longName, "描述"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("创建重复话题抛400")
    void createTopic_Duplicate_ThrowsException() {
        topicService.createTopic("Java", "描述");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.createTopic("Java", "另一个描述"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("获取话题列表（分页）")
    void getTopicList_Success() {
        createTopic("Java", "描述1", 10);
        createTopic("Vue", "描述2", 20);

        Page<Topic> page = topicService.getTopicList(1, 10);
        assertNotNull(page);
        assertEquals(2, page.getTotal());
    }

    @Test
    @DisplayName("获取热门话题")
    void getHotTopics_Success() {
        createTopic("Java", "描述1", 10);
        createTopic("Vue", "描述2", 20);
        createTopic("React", "描述3", 30);

        List<Topic> hot = topicService.getHotTopics(2);
        assertNotNull(hot);
        assertEquals(2, hot.size());
        // 按热度降序
        assertEquals("React", hot.get(0).getName());
        assertEquals("Vue", hot.get(1).getName());
    }

    @Test
    @DisplayName("getOrCreateTopic - 已存在返回已有ID")
    void getOrCreateTopic_Existing() {
        Long existingId = topicService.createTopic("Java", "描述");
        Long foundId = topicService.getOrCreateTopic("Java");

        assertEquals(existingId, foundId);
    }

    @Test
    @DisplayName("getOrCreateTopic - 不存在则创建")
    void getOrCreateTopic_New() {
        Long topicId = topicService.getOrCreateTopic("Spring");

        assertNotNull(topicId);
        Topic saved = topicMapper.selectById(topicId);
        assertEquals("Spring", saved.getName());
    }

    @Test
    @DisplayName("getOrCreateTopic - 空名称返回null")
    void getOrCreateTopic_EmptyName() {
        assertNull(topicService.getOrCreateTopic(""));
        assertNull(topicService.getOrCreateTopic(null));
    }

    @Test
    @DisplayName("getOrCreateTopic - 去除#号前缀")
    void getOrCreateTopic_WithHash() {
        Long topicId = topicService.getOrCreateTopic("#Java");
        assertNotNull(topicId);
        Topic saved = topicMapper.selectById(topicId);
        assertEquals("Java", saved.getName());
    }

    @Test
    @DisplayName("获取话题详情")
    void getTopicById_Success() {
        Topic created = createTopic("Java", "描述", 10);
        Topic found = topicService.getTopicById(created.getId());

        assertNotNull(found);
        assertEquals("Java", found.getName());
    }

    @Test
    @DisplayName("获取不存在的话题抛404")
    void getTopicById_NotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.getTopicById(99999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("搜索话题")
    void searchTopics_Success() {
        createTopic("Java基础", "描述", 10);
        createTopic("JavaScript", "描述", 20);
        createTopic("Python", "描述", 5);

        List<Topic> results = topicService.searchTopics("Java");
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("搜索话题 - 空关键词返回空列表")
    void searchTopics_EmptyKeyword() {
        assertTrue(topicService.searchTopics("").isEmpty());
        assertTrue(topicService.searchTopics(null).isEmpty());
    }

    @Test
    @DisplayName("根据ID列表获取话题名称")
    void getTopicNamesByIds_Success() {
        Topic t1 = createTopic("Java", "描述", 10);
        Topic t2 = createTopic("Vue", "描述", 20);

        List<String> names = topicService.getTopicNamesByIds(List.of(t1.getId(), t2.getId()));
        assertNotNull(names);
        assertEquals(2, names.size());
        assertTrue(names.contains("Java"));
        assertTrue(names.contains("Vue"));
    }

    @Test
    @DisplayName("根据ID列表获取话题名称 - 空列表")
    void getTopicNamesByIds_Empty() {
        assertTrue(topicService.getTopicNamesByIds(List.of()).isEmpty());
        assertTrue(topicService.getTopicNamesByIds(null).isEmpty());
    }
}
