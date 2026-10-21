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

    // ==================== 管理员操作（事务边界已从控制器移到服务层） ====================

    @Test
    @DisplayName("更新话题 - 名称/描述/状态均生效")
    void updateTopic_Success() {
        Topic created = createTopic("Java", "描述", 10);

        Topic updated = topicService.updateTopic(created.getId(), "JavaSE", "新描述", 0);

        assertEquals("JavaSE", updated.getName());
        assertEquals("新描述", updated.getDescription());
        assertEquals(0, updated.getStatus());
        Topic reloaded = topicMapper.selectById(created.getId());
        assertEquals("JavaSE", reloaded.getName());
        assertEquals(0, reloaded.getStatus());
    }

    @Test
    @DisplayName("更新话题 - 重名抛400")
    void updateTopic_DuplicateName_ThrowsException() {
        createTopic("Java", "描述", 10);
        Topic other = createTopic("Vue", "描述", 20);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.updateTopic(other.getId(), "Java", null, null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("更新话题 - 不存在抛404")
    void updateTopic_NotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.updateTopic(99999L, "任意", null, null));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("更新话题 - 非法状态抛400")
    void updateTopic_InvalidStatus_ThrowsException() {
        Topic created = createTopic("Java", "描述", 10);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.updateTopic(created.getId(), null, null, 7));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("删除话题 - 成功且不存在时抛404")
    void deleteTopic_SuccessAndNotFound() {
        Topic created = createTopic("Java", "描述", 10);

        topicService.deleteTopic(created.getId());
        assertNull(topicMapper.selectById(created.getId()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.deleteTopic(created.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("切换话题状态 - 生效且非法值抛400")
    void toggleStatus_SuccessAndInvalid() {
        Topic created = createTopic("Java", "描述", 10);

        topicService.toggleStatus(created.getId(), 0);
        assertEquals(0, topicMapper.selectById(created.getId()).getStatus());

        topicService.toggleStatus(created.getId(), 1);
        assertEquals(1, topicMapper.selectById(created.getId()).getStatus());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicService.toggleStatus(created.getId(), null));
        assertEquals(400, ex.getCode());
        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> topicService.toggleStatus(99999L, 1));
        assertEquals(404, ex2.getCode());
    }
}
