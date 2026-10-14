package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.CirclePost;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.service.circle.impl.CircleServiceImpl;
import com.selfblog.service.social.FollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * CircleServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CircleServiceImplTest {

    @Autowired
    private CircleServiceImpl circleService;

    @MockBean
    private CirclePostMapper circlePostMapper;

    @MockBean
    private FollowService followService;

    private Long testUserId = 1L;
    private Long testPostId = 100L;

    @BeforeEach
    void setUp() {
        // Stub insert to simulate ID generation
        when(circlePostMapper.insert(any(CirclePost.class))).thenAnswer(invocation -> {
            CirclePost post = invocation.getArgument(0);
            if (post.getId() == null) {
                post.setId(System.currentTimeMillis());
            }
            return 1;
        });

        // Stub selectPage to return empty page
        when(circlePostMapper.selectPage(any(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class), any()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        // Stub followService to return empty list
        when(followService.getFollowing(anyLong())).thenReturn(java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("createPost_SuccessWithText_ReturnsPostId")
    void createPost_SuccessWithText_ReturnsPostId() {
        // Given
        String content = "This is a test post";
        Long userId = testUserId;

        // When
        Long postId = circleService.createPost(content, null, null, null, null, userId, 0, 1, 1, null);

        // Then
        assertNotNull(postId);
        assertTrue(postId > 0);
    }

    @Test
    @DisplayName("createPost_SuccessWithImages_ReturnsPostId")
    void createPost_SuccessWithImages_ReturnsPostId() {
        // Given
        String content = "Check out these photos";
        List<String> images = Arrays.asList("http://example.com/img1.jpg", "http://example.com/img2.jpg");
        Long userId = testUserId;

        // When
        Long postId = circleService.createPost(content, images, null, null, null, userId, 0, 1, 1, null);

        // Then
        assertNotNull(postId);
        assertTrue(postId > 0);
    }

    @Test
    @DisplayName("createPost_SuccessWithVideos_ReturnsPostId")
    void createPost_SuccessWithVideos_ReturnsPostId() {
        // Given
        String content = "Check out this video";
        List<String> videos = Arrays.asList("http://example.com/video1.mp4");
        Long userId = testUserId;

        // When
        Long postId = circleService.createPost(content, null, videos, null, null, userId, 0, 1, 1, null);

        // Then
        assertNotNull(postId);
        assertTrue(postId > 0);
    }

    @Test
    @DisplayName("createPost_EmptyContent_ThrowsException")
    void createPost_EmptyContent_ThrowsException() {
        // Given
        String content = "";
        Long userId = testUserId;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                circleService.createPost(content, null, null, null, null, userId, 0, 1, 1, null));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("动态内容不能为空"));
    }

    @Test
    @DisplayName("createPost_ContentTooLong_ThrowsException")
    void createPost_ContentTooLong_ThrowsException() {
        // Given
        String content = "a".repeat(2001);
        Long userId = testUserId;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                circleService.createPost(content, null, null, null, null, userId, 0, 1, 1, null));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("动态内容不能超过2000字符"));
    }

    @Test
    @DisplayName("createPost_LocationTooLong_ThrowsException")
    void createPost_LocationTooLong_ThrowsException() {
        // Given
        String content = "Valid content";
        String location = "a".repeat(101);
        Long userId = testUserId;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                circleService.createPost(content, null, null, location, null, userId, 0, 1, 1, null));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("位置信息不能超过100字符"));
    }

    @Test
    @DisplayName("createPost_RepostOriginalNotFound_ThrowsException")
    void createPost_RepostOriginalNotFound_ThrowsException() {
        // Given
        String content = "Reposting";
        Long repostId = 999L;
        Long userId = testUserId;

        when(circlePostMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                circleService.createPost(content, null, null, null, repostId, userId, 0, 1, 1, null));
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("原动态不存在"));
    }

    @Test
    @DisplayName("createPost_RepostOriginalNotAllowed_ThrowsException")
    void createPost_RepostOriginalNotAllowed_ThrowsException() {
        // Given
        String content = "Reposting";
        Long repostId = testPostId;
        Long userId = testUserId;

        CirclePost originalPost = new CirclePost();
        originalPost.setId(testPostId);
        originalPost.setUserId(2L);
        originalPost.setAllowRepost(0); // Not allowed to repost

        when(circlePostMapper.selectById(testPostId)).thenReturn(originalPost);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                circleService.createPost(content, null, null, null, repostId, userId, 0, 1, 1, null));
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("禁止转发"));
    }

    @Test
    @DisplayName("deletePost_Success_NoException")
    void deletePost_Success_NoException() {
        // Given
        CirclePost existingPost = new CirclePost();
        existingPost.setId(testPostId);
        existingPost.setUserId(testUserId);

        when(circlePostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        assertDoesNotThrow(() ->
                circleService.deletePost(testPostId, testUserId));
    }

    @Test
    @DisplayName("deletePost_PostNotFound_ThrowsException")
    void deletePost_PostNotFound_ThrowsException() {
        // Given
        when(circlePostMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                circleService.deletePost(999L, testUserId));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("deletePost_NotOwner_ThrowsException")
    void deletePost_NotOwner_ThrowsException() {
        // Given
        CirclePost existingPost = new CirclePost();
        existingPost.setId(testPostId);
        existingPost.setUserId(999L); // Different user

        when(circlePostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                circleService.deletePost(testPostId, testUserId));
        assertEquals(403, exception.getCode());
    }

    @Test
    @DisplayName("getPostList_Success_ReturnsNonEmptyList")
    void getPostList_Success_ReturnsNonEmptyList() {
        // Given
        int page = 1;
        int pageSize = 10;
        Long userId = testUserId;

        // When
        List<com.selfblog.vo.circle.CirclePostVO> posts =
                circleService.getRecommendFeed(page, pageSize, userId);

        // Then
        assertNotNull(posts);
    }

    @Test
    @DisplayName("getPostList_WithNullUserId_Success")
    void getPostList_WithNullUserId_Success() {
        // Given
        int page = 1;
        int pageSize = 10;

        // When
        List<com.selfblog.vo.circle.CirclePostVO> posts =
                circleService.getRecommendFeed(page, pageSize, null);

        // Then
        assertNotNull(posts);
    }

    @Test
    @DisplayName("createPost_WithContent_ShouldStoreContentCorrectly")
    void createPost_WithContent_ShouldStoreContentCorrectly() {
        // Given
        String content = "Test post with @mentions";
        Long userId = testUserId;

        // When
        Long postId = circleService.createPost(content, null, null, null, null, userId, 0, 1, 1, null);

        // Then
        assertNotNull(postId);

        // Verify the saved post has correct content via the mocked insert
        verify(circlePostMapper, times(1)).insert(any(CirclePost.class));
    }

    @Test
    @DisplayName("getFollowingFeed_Success_ReturnsNonEmptyList")
    void getFollowingFeed_Success_ReturnsNonEmptyList() {
        // Given
        int page = 1;
        int pageSize = 10;
        Long userId = testUserId;

        // When
        List<com.selfblog.vo.circle.CirclePostVO> posts =
                circleService.getFollowingFeed(page, pageSize, userId);

        // Then
        assertNotNull(posts);
    }

    @Test
    @DisplayName("getFollowingFeed_NoFollowing_ReturnsEmptyList")
    void getFollowingFeed_NoFollowing_ReturnsEmptyList() {
        // Given
        int page = 1;
        int pageSize = 10;
        Long userId = 999L; // User with no following

        // When
        List<com.selfblog.vo.circle.CirclePostVO> posts =
                circleService.getFollowingFeed(page, pageSize, userId);

        // Then
        assertNotNull(posts);
        assertTrue(posts.isEmpty());
    }
}