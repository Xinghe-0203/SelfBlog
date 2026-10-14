package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.post.impl.BlogPostServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * BlogPostServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogPostServiceImplTest {

    @Autowired
    private BlogPostServiceImpl blogPostService;

    @MockBean
    private BlogPostMapper blogPostMapper;

    @MockBean
    private com.selfblog.mapper.BlogTagMapper blogTagMapper;

    private PostCreateRequest validPostRequest;
    private Long testUserId = 1L;
    private Long testPostId = 100L;

    @BeforeEach
    void setUp() {
        validPostRequest = new PostCreateRequest();
        validPostRequest.setTitle("Test Title");
        validPostRequest.setContent("Test Content");
        validPostRequest.setCategory("Test Category");

        // Stub insert to simulate ID generation
        when(blogPostMapper.insert(any(BlogPost.class))).thenAnswer(invocation -> {
            BlogPost post = invocation.getArgument(0);
            if (post.getId() == null) {
                post.setId(System.currentTimeMillis());
            }
            return 1;
        });

        // Stub tag validation to accept any tag IDs
        when(blogTagMapper.selectBatchIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> {
                com.selfblog.entity.BlogTag tag = new com.selfblog.entity.BlogTag();
                tag.setId(id);
                tag.setName("tag" + id);
                return tag;
            }).collect(java.util.stream.Collectors.toList());
        });
    }

    @Test
    @DisplayName("createPost_Success_ReturnsPostId")
    void createPost_Success_ReturnsPostId() {
        // Given
        validPostRequest.setTitle("Valid Title");
        validPostRequest.setContent("Valid Content");

        // When
        Long postId = blogPostService.createPost(validPostRequest, testUserId, false);

        // Then
        assertNotNull(postId);
        assertTrue(postId > 0);
    }

    @Test
    @DisplayName("createPost_EmptyTitle_ThrowsException")
    void createPost_EmptyTitle_ThrowsException() {
        // Given
        validPostRequest.setTitle("");
        validPostRequest.setContent("Valid Content");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.createPost(validPostRequest, testUserId, false));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("标题不能为空"));
    }

    @Test
    @DisplayName("createPost_EmptyContent_ThrowsException")
    void createPost_EmptyContent_ThrowsException() {
        // Given
        validPostRequest.setTitle("Valid Title");
        validPostRequest.setContent("");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.createPost(validPostRequest, testUserId, false));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("内容不能为空"));
    }

    @Test
    @DisplayName("createPost_TitleTooLong_ThrowsException")
    void createPost_TitleTooLong_ThrowsException() {
        // Given
        String longTitle = "a".repeat(201);
        validPostRequest.setTitle(longTitle);
        validPostRequest.setContent("Valid Content");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.createPost(validPostRequest, testUserId, false));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("标题不能超过200字符"));
    }

    @Test
    @DisplayName("createPost_ContentTooLong_ThrowsException")
    void createPost_ContentTooLong_ThrowsException() {
        // Given
        validPostRequest.setTitle("Valid Title");
        String longContent = "a".repeat(300001);
        validPostRequest.setContent(longContent);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.createPost(validPostRequest, testUserId, false));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("内容不能超过30万字符"));
    }

    @Test
    @DisplayName("createPost_WithTags_Success")
    void createPost_WithTags_Success() {
        // Given
        validPostRequest.setTitle("Valid Title");
        validPostRequest.setContent("Valid Content");
        validPostRequest.setTagIds(Arrays.asList(1L, 2L, 3L));

        // When
        Long postId = blogPostService.createPost(validPostRequest, testUserId, false);

        // Then
        assertNotNull(postId);
    }

    @Test
    @DisplayName("createPost_WithTagNames_Success")
    void createPost_WithTagNames_Success() {
        // Given
        validPostRequest.setTitle("Valid Title");
        validPostRequest.setContent("Valid Content");
        validPostRequest.setTagNames(Arrays.asList("tag1", "tag2"));

        // When
        Long postId = blogPostService.createPost(validPostRequest, testUserId, false);

        // Then
        assertNotNull(postId);
    }

    @Test
    @DisplayName("updatePost_Success_NoException")
    void updatePost_Success_NoException() {
        // Given
        validPostRequest.setId(testPostId);
        validPostRequest.setTitle("Updated Title");
        validPostRequest.setContent("Updated Content");

        // Mock the existing post
        BlogPost existingPost = new BlogPost();
        existingPost.setId(testPostId);
        existingPost.setUserId(testUserId);
        existingPost.setTitle("Original Title");
        existingPost.setContent("Original Content");
        existingPost.setStatus(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        assertDoesNotThrow(() ->
                blogPostService.updatePost(validPostRequest, testUserId, false));
    }

    @Test
    @DisplayName("updatePost_PostNotFound_ThrowsException")
    void updatePost_PostNotFound_ThrowsException() {
        // Given
        validPostRequest.setId(999L);
        validPostRequest.setTitle("Updated Title");
        validPostRequest.setContent("Updated Content");

        when(blogPostMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.updatePost(validPostRequest, testUserId, false));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("updatePost_NotOwner_ThrowsException")
    void updatePost_NotOwner_ThrowsException() {
        // Given
        validPostRequest.setId(testPostId);
        validPostRequest.setTitle("Updated Title");
        validPostRequest.setContent("Updated Content");

        BlogPost existingPost = new BlogPost();
        existingPost.setId(testPostId);
        existingPost.setUserId(999L); // Different user
        existingPost.setTitle("Original Title");
        existingPost.setContent("Original Content");

        when(blogPostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.updatePost(validPostRequest, testUserId, false));
        assertEquals(403, exception.getCode());
    }

    @Test
    @DisplayName("updatePost_AdminCanUpdateAnyPost")
    void updatePost_AdminCanUpdateAnyPost() {
        // Given
        validPostRequest.setId(testPostId);
        validPostRequest.setTitle("Admin Updated Title");
        validPostRequest.setContent("Admin Updated Content");

        BlogPost existingPost = new BlogPost();
        existingPost.setId(testPostId);
        existingPost.setUserId(999L); // Different user
        existingPost.setTitle("Original Title");
        existingPost.setContent("Original Content");
        existingPost.setStatus(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        assertDoesNotThrow(() ->
                blogPostService.updatePost(validPostRequest, 2L, true)); // isAdmin = true
    }

    @Test
    @DisplayName("updatePost_EmptyTitle_ThrowsException")
    void updatePost_EmptyTitle_ThrowsException() {
        // Given
        validPostRequest.setId(testPostId);
        validPostRequest.setTitle("");
        validPostRequest.setContent("Valid Content");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.updatePost(validPostRequest, testUserId, false));
        assertEquals(400, exception.getCode());
    }

    @Test
    @DisplayName("deletePost_Success_NoException")
    void deletePost_Success_NoException() {
        // Given
        BlogPost existingPost = new BlogPost();
        existingPost.setId(testPostId);
        existingPost.setUserId(testUserId);
        existingPost.setTitle("Test Post");
        existingPost.setStatus(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        assertDoesNotThrow(() ->
                blogPostService.deletePost(testPostId, testUserId, false));
    }

    @Test
    @DisplayName("deletePost_PostNotFound_ThrowsException")
    void deletePost_PostNotFound_ThrowsException() {
        // Given
        when(blogPostMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.deletePost(999L, testUserId, false));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("deletePost_NotOwner_ThrowsException")
    void deletePost_NotOwner_ThrowsException() {
        // Given
        BlogPost existingPost = new BlogPost();
        existingPost.setId(testPostId);
        existingPost.setUserId(999L); // Different user
        existingPost.setTitle("Test Post");

        when(blogPostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.deletePost(testPostId, testUserId, false));
        assertEquals(403, exception.getCode());
    }

    @Test
    @DisplayName("deletePost_AdminCanDeleteAnyPost")
    void deletePost_AdminCanDeleteAnyPost() {
        // Given
        BlogPost existingPost = new BlogPost();
        existingPost.setId(testPostId);
        existingPost.setUserId(999L); // Different user
        existingPost.setTitle("Test Post");
        existingPost.setStatus(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        assertDoesNotThrow(() ->
                blogPostService.deletePost(testPostId, 2L, true)); // isAdmin = true
    }

    @Test
    @DisplayName("deletePost_AlreadyDeleted_ThrowsException")
    void deletePost_AlreadyDeleted_ThrowsException() {
        // Given
        BlogPost existingPost = new BlogPost();
        existingPost.setId(testPostId);
        existingPost.setUserId(testUserId);
        existingPost.setTitle("Test Post");
        existingPost.setIsDeleted(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(existingPost);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogPostService.deletePost(testPostId, testUserId, false));
        assertEquals(404, exception.getCode());
    }
}