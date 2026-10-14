package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogCollectMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.social.BlogCollectService;
import com.selfblog.vo.post.CollectItemVO;
import com.selfblog.vo.post.CollectResultVO;
import com.selfblog.vo.post.CollectStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BlogCollectServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogCollectServiceImplTest {

    @Autowired
    private BlogCollectService blogCollectService;

    @Autowired
    private BlogCollectMapper blogCollectMapper;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private BlogPost testPost;
    private Long testUserId = 100L;

    @BeforeEach
    void setUp() {
        blogCollectMapper.delete(null);
        blogPostMapper.delete(null);

        testPost = new BlogPost();
        testPost.setUserId(1L);
        testPost.setTitle("测试文章");
        testPost.setSummary("测试摘要");
        testPost.setContent("测试内容");
        testPost.setStatus(1);
        testPost.setViewCount(0L);
        testPost.setLikeCount(0);
        testPost.setCommentCount(0);
        testPost.setCollectCount(0);
        blogPostMapper.insert(testPost);
    }

    @Test
    @DisplayName("收藏文章成功")
    void toggleCollect_Collect_Success() {
        CollectResultVO result = blogCollectService.toggleCollect(testPost.getId(), testUserId);

        assertNotNull(result);
        assertEquals("collect", result.getAction());
        assertEquals(1, result.getCollectCount());
    }

    @Test
    @DisplayName("取消收藏成功")
    void toggleCollect_Uncollect_Success() {
        blogCollectService.toggleCollect(testPost.getId(), testUserId);
        CollectResultVO result = blogCollectService.toggleCollect(testPost.getId(), testUserId);

        assertNotNull(result);
        assertEquals("uncollect", result.getAction());
        assertEquals(0, result.getCollectCount());
    }

    @Test
    @DisplayName("重复切换收藏状态")
    void toggleCollect_MultipleToggles() {
        // 收藏
        CollectResultVO r1 = blogCollectService.toggleCollect(testPost.getId(), testUserId);
        assertEquals("collect", r1.getAction());
        assertEquals(1, r1.getCollectCount());

        // 取消收藏
        CollectResultVO r2 = blogCollectService.toggleCollect(testPost.getId(), testUserId);
        assertEquals("uncollect", r2.getAction());
        assertEquals(0, r2.getCollectCount());

        // 再次收藏
        CollectResultVO r3 = blogCollectService.toggleCollect(testPost.getId(), testUserId);
        assertEquals("collect", r3.getAction());
        assertEquals(1, r3.getCollectCount());
    }

    @Test
    @DisplayName("收藏不存在的文章抛404")
    void toggleCollect_PostNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> blogCollectService.toggleCollect(99999L, testUserId));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("文章不存在"));
    }

    @Test
    @DisplayName("检查收藏状态 - 已收藏")
    void checkCollectStatus_Collected() {
        blogCollectService.toggleCollect(testPost.getId(), testUserId);

        CollectStatusVO status = blogCollectService.checkCollectStatus(testPost.getId(), testUserId);
        assertNotNull(status);
        assertTrue(status.getCollected());
        assertEquals(1, status.getCollectCount());
    }

    @Test
    @DisplayName("检查收藏状态 - 未收藏")
    void checkCollectStatus_NotCollected() {
        CollectStatusVO status = blogCollectService.checkCollectStatus(testPost.getId(), testUserId);
        assertNotNull(status);
        assertFalse(status.getCollected());
        assertEquals(0, status.getCollectCount());
    }

    @Test
    @DisplayName("检查收藏状态 - userId为null返回未收藏")
    void checkCollectStatus_NullUserId() {
        CollectStatusVO status = blogCollectService.checkCollectStatus(testPost.getId(), null);
        assertNotNull(status);
        assertFalse(status.getCollected());
    }

    @Test
    @DisplayName("获取我的收藏列表")
    void getMyCollections_Success() {
        blogCollectService.toggleCollect(testPost.getId(), testUserId);

        IPage<CollectItemVO> page = blogCollectService.getMyCollections(testUserId, 1, 10);
        assertNotNull(page);
        assertEquals(1, page.getTotal());
        assertEquals(1, page.getRecords().size());
        assertEquals(testPost.getId(), page.getRecords().get(0).getPostId());
        assertEquals(testPost.getTitle(), page.getRecords().get(0).getTitle());
    }

    @Test
    @DisplayName("获取我的收藏列表 - 空列表")
    void getMyCollections_Empty() {
        IPage<CollectItemVO> page = blogCollectService.getMyCollections(testUserId, 1, 10);
        assertNotNull(page);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    @DisplayName("批量检查收藏状态")
    void checkCollectStatusBatch_Success() {
        BlogPost post2 = new BlogPost();
        post2.setUserId(1L);
        post2.setTitle("文章2");
        post2.setContent("内容2");
        post2.setStatus(1);
        post2.setCollectCount(0);
        blogPostMapper.insert(post2);

        // 只收藏第一篇
        blogCollectService.toggleCollect(testPost.getId(), testUserId);

        List<Boolean> results = blogCollectService.checkCollectStatusBatch(
                List.of(testPost.getId(), post2.getId()), testUserId);
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.get(0));
        assertFalse(results.get(1));
    }

    @Test
    @DisplayName("批量检查收藏状态 - 空列表")
    void checkCollectStatusBatch_EmptyList() {
        List<Boolean> results = blogCollectService.checkCollectStatusBatch(List.of(), testUserId);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("收藏后文章收藏数正确更新")
    void toggleCollect_CollectCountUpdated() {
        blogCollectService.toggleCollect(testPost.getId(), testUserId);
        BlogPost updated = blogPostMapper.selectById(testPost.getId());
        assertEquals(1, updated.getCollectCount());

        blogCollectService.toggleCollect(testPost.getId(), testUserId);
        updated = blogPostMapper.selectById(testPost.getId());
        assertEquals(0, updated.getCollectCount());
    }
}
