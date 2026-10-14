package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogLike;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogLikeMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.social.BlogLikeService;
import com.selfblog.vo.post.LikeItemVO;
import com.selfblog.vo.post.LikeResultVO;
import com.selfblog.vo.post.LikeStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BlogLikeService 单元测试
 */
@SpringBootTest
@Transactional(rollbackFor = Exception.class)
public class BlogLikeServiceImplTest {

    @Autowired
    private BlogLikeService blogLikeService;

    @Autowired
    private BlogLikeMapper blogLikeMapper;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private BlogPost testPost;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        blogLikeMapper.delete(null);
        blogPostMapper.delete(null);

        // 创建测试文章
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
    @DisplayName("添加点赞成功")
    void testToggleLike_AddLike() {
        Long userId = 100L;
        Long postId = testPost.getId();

        LikeResultVO result = blogLikeService.toggleLike(postId, userId);

        assertNotNull(result);
        assertEquals("like", result.getAction());
        assertEquals(1, result.getLikeCount());

        // 验证点赞记录已存入数据库
        assertTrue(blogLikeService.hasLiked(postId, userId));
    }

    @Test
    @DisplayName("取消点赞成功")
    void testToggleLike_RemoveLike() {
        Long userId = 101L;
        Long postId = testPost.getId();

        // 先点赞
        blogLikeService.toggleLike(postId, userId);
        assertTrue(blogLikeService.hasLiked(postId, userId));

        // 再取消点赞
        LikeResultVO result = blogLikeService.toggleLike(postId, userId);

        assertNotNull(result);
        assertEquals("unlike", result.getAction());
        assertEquals(0, result.getLikeCount());

        // 验证点赞记录已删除
        assertFalse(blogLikeService.hasLiked(postId, userId));
    }

    @Test
    @DisplayName("检查点赞状态 - 已点赞")
    void testCheckLikeStatus_Liked() {
        Long userId = 102L;
        Long postId = testPost.getId();

        // 先点赞
        blogLikeService.toggleLike(postId, userId);

        // 检查状态
        LikeStatusVO status = blogLikeService.checkLikeStatus(postId, userId);

        assertNotNull(status);
        assertTrue(status.getLiked());
        assertEquals(1, status.getLikeCount());
    }

    @Test
    @DisplayName("检查点赞状态 - 未点赞")
    void testCheckLikeStatus_NotLiked() {
        Long userId = 103L;
        Long postId = testPost.getId();

        // 不点赞，直接检查状态
        LikeStatusVO status = blogLikeService.checkLikeStatus(postId, userId);

        assertNotNull(status);
        assertFalse(status.getLiked());
        assertEquals(0, status.getLikeCount());
    }

    @Test
    @DisplayName("点赞不存在的文章应抛出异常")
    void testToggleLike_PostNotFound() {
        Long userId = 104L;
        Long nonExistentPostId = 99999L;

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            blogLikeService.toggleLike(nonExistentPostId, userId);
        });

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("文章不存在"));
    }

    @Test
    @DisplayName("hasLiked 对空用户ID返回false")
    void testHasLiked_NullUserId() {
        Long postId = testPost.getId();

        assertFalse(blogLikeService.hasLiked(postId, null));
    }

    @Test
    @DisplayName("getMyLikes 应包含 is_deleted IS NULL 的历史数据")
    void testGetMyLikes_ShouldIncludeNullIsDeleted() {
        Long userId = 106L;
        Long postId = testPost.getId();

        // First, create a like (is_deleted = 0)
        blogLikeService.toggleLike(postId, userId);

        // Manually set is_deleted = NULL to simulate historical data
        BlogLike like = blogLikeMapper.selectActiveByUserAndPost(userId, postId);
        assertNotNull(like);
        LambdaUpdateWrapper<BlogLike> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogLike::getId, like.getId()).set(BlogLike::getIsDeleted, null);
        blogLikeMapper.update(null, updateWrapper);

        // Verify hasLiked still returns true (bypasses @TableLogic)
        assertTrue(blogLikeService.hasLiked(postId, userId));

        // Verify checkLikeStatus still returns liked
        LikeStatusVO status = blogLikeService.checkLikeStatus(postId, userId);
        assertTrue(status.getLiked());

        // Verify getMyLikes still includes the record with is_deleted = NULL
        IPage<LikeItemVO> myLikes = blogLikeService.getMyLikes(userId, 1, 10);
        assertTrue(myLikes.getTotal() > 0, "getMyLikes should include records with is_deleted IS NULL");
        assertEquals(1, myLikes.getRecords().size());
        assertEquals(postId, myLikes.getRecords().get(0).getPostId());
    }

    @Test
    @DisplayName("重复切换点赞状态")
    void testToggleLike_MultipleToggles() {
        Long userId = 105L;
        Long postId = testPost.getId();

        // 第一次：点赞
        LikeResultVO result1 = blogLikeService.toggleLike(postId, userId);
        assertEquals("like", result1.getAction());
        assertEquals(1, result1.getLikeCount());

        // 第二次：取消点赞
        LikeResultVO result2 = blogLikeService.toggleLike(postId, userId);
        assertEquals("unlike", result2.getAction());
        assertEquals(0, result2.getLikeCount());

        // 第三次：再次点赞
        LikeResultVO result3 = blogLikeService.toggleLike(postId, userId);
        assertEquals("like", result3.getAction());
        assertEquals(1, result3.getLikeCount());
    }
}
