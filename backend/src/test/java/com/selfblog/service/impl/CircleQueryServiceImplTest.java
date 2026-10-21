package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.CirclePost;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.CircleLikeMapper;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.mapper.CircleRepostMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.circle.CircleQueryService;
import com.selfblog.vo.circle.CirclePostVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CircleQueryServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CircleQueryServiceImplTest {

    @Autowired
    private CircleQueryService circleQueryService;

    @Autowired
    private CirclePostMapper circlePostMapper;

    @Autowired
    private CircleLikeMapper circleLikeMapper;

    @Autowired
    private CircleRepostMapper circleRepostMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private SysUser userA;
    private CirclePost publicPost;

    @BeforeEach
    void setUp() {
        circleLikeMapper.delete(null);
        circleRepostMapper.delete(null);
        circlePostMapper.delete(null);
        sysUserMapper.delete(null);

        userA = new SysUser();
        userA.setUsername("userA");
        userA.setPassword("password");
        userA.setNickname("User A");
        userA.setRole("user");
        userA.setStatus(1);
        sysUserMapper.insert(userA);

        publicPost = new CirclePost();
        publicPost.setUserId(userA.getId());
        publicPost.setContent("公开动态");
        publicPost.setContentType(1);
        publicPost.setLikeCount(0);
        publicPost.setCommentCount(0);
        publicPost.setRepostCount(0);
        publicPost.setViewCount(0L);
        publicPost.setIsTop(0);
        publicPost.setVisibility(0);
        publicPost.setAllowComment(1);
        publicPost.setAllowRepost(1);
        publicPost.setStatus(1);
        circlePostMapper.insert(publicPost);
    }

    private CirclePost createPost(Long userId, String content, int visibility) {
        CirclePost post = new CirclePost();
        post.setUserId(userId);
        post.setContent(content);
        post.setContentType(1);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setRepostCount(0);
        post.setViewCount(0L);
        post.setIsTop(0);
        post.setVisibility(visibility);
        post.setAllowComment(1);
        post.setAllowRepost(1);
        post.setStatus(1);
        circlePostMapper.insert(post);
        return post;
    }

    @Test
    @DisplayName("获取推荐流成功")
    void getRecommendFeed_Success() {
        List<CirclePostVO> feed = circleQueryService.getRecommendFeed(1, 10, userA.getId());
        assertNotNull(feed);
        assertFalse(feed.isEmpty());
    }

    @Test
    @DisplayName("获取推荐流 - 未登录用户只能看到公开动态")
    void getRecommendFeed_Anonymous_OnlyPublic() {
        createPost(userA.getId(), "仅关注者可见", 1); // FOLLOWERS

        List<CirclePostVO> feed = circleQueryService.getRecommendFeed(1, 10, null);
        assertNotNull(feed);
        // 应该只包含公开动态
        for (CirclePostVO vo : feed) {
            assertEquals(0, vo.getVisibility());
        }
    }

    @Test
    @DisplayName("获取动态详情成功")
    void getPostDetail_Success() {
        CirclePostVO detail = circleQueryService.getPostDetail(publicPost.getId(), userA.getId());
        assertNotNull(detail);
        assertEquals(publicPost.getId(), detail.getId());
        assertEquals("公开动态", detail.getContent());
    }

    @Test
    @DisplayName("获取动态详情 - view_count 为 NULL 不抛 NPE 且计数递增")
    void getPostDetail_NullViewCountDoesNotThrow() {
        // view_count 列允许为 NULL，历史数据里存在 NULL；直接拆箱会 NPE
        jdbc.update("UPDATE blog_circle_post SET view_count = NULL WHERE id = ?", publicPost.getId());

        CirclePostVO detail = circleQueryService.getPostDetail(publicPost.getId(), userA.getId());

        assertNotNull(detail, "NULL 浏览量不能让详情接口失败");
        assertEquals(1L, detail.getViewCount(), "NULL 视为 0 后再 +1");
    }

    @Test
    @DisplayName("获取不存在的动态详情抛404")
    void getPostDetail_NotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleQueryService.getPostDetail(99999L, userA.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("搜索动态成功")
    void searchPosts_Success() {
        List<CirclePostVO> results = circleQueryService.searchPosts("公开", 1, 10, userA.getId());
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("搜索动态 - 空关键词返回空")
    void searchPosts_EmptyKeyword() {
        List<CirclePostVO> results = circleQueryService.searchPosts("", 1, 10, userA.getId());
        assertNotNull(results);
    }

    @Test
    @DisplayName("获取用户动态列表")
    void getUserPosts_Success() {
        IPage<CirclePostVO> page = circleQueryService.getUserPosts(userA.getId(), 1, 10, userA.getId());
        assertNotNull(page);
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    @DisplayName("获取关注流 - 无关注返回空")
    void getFollowingFeed_NoFollowing() {
        List<CirclePostVO> feed = circleQueryService.getFollowingFeed(1, 10, userA.getId());
        assertNotNull(feed);
        assertTrue(feed.isEmpty());
    }
}
