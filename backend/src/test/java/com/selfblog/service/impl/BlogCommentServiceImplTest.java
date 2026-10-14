package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.social.CommentCreateRequest;
import com.selfblog.entity.BlogComment;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogCommentMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.social.impl.BlogCommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * BlogCommentServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogCommentServiceImplTest {

    @Autowired
    private BlogCommentServiceImpl blogCommentService;

    @MockBean
    private BlogCommentMapper blogCommentMapper;

    @MockBean
    private BlogPostMapper blogPostMapper;

    private CommentCreateRequest validCommentRequest;
    private Long testUserId = 1L;
    private Long testPostId = 100L;
    private Long testCommentId = 200L;

    @BeforeEach
    void setUp() {
        validCommentRequest = new CommentCreateRequest();
        validCommentRequest.setPostId(testPostId);
        validCommentRequest.setContent("Test Comment Content");

        // Stub insert to simulate ID generation
        when(blogCommentMapper.insert(any(BlogComment.class))).thenAnswer(invocation -> {
            BlogComment comment = invocation.getArgument(0);
            if (comment.getId() == null) {
                comment.setId(System.currentTimeMillis());
            }
            return 1;
        });

        // Stub selectPage to return empty page
        when(blogCommentMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());
    }

    @Test
    @DisplayName("createComment_Success_ReturnsCommentId")
    void createComment_Success_ReturnsCommentId() {
        // Given
        validCommentRequest.setPostId(testPostId);
        validCommentRequest.setContent("Valid Comment Content");

        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setUserId(2L);
        post.setStatus(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(post);

        // When
        Long commentId = blogCommentService.createComment(validCommentRequest, testUserId);

        // Then
        assertNotNull(commentId);
        assertTrue(commentId > 0);
    }

    @Test
    @DisplayName("createComment_PostNotFound_ThrowsException")
    void createComment_PostNotFound_ThrowsException() {
        // Given
        validCommentRequest.setPostId(999L);
        when(blogPostMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.createComment(validCommentRequest, testUserId));
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("文章不存在"));
    }

    @Test
    @DisplayName("createComment_PostNotPublished_ThrowsException")
    void createComment_PostNotPublished_ThrowsException() {
        // Given
        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setStatus(0); // Not published

        when(blogPostMapper.selectById(testPostId)).thenReturn(post);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.createComment(validCommentRequest, testUserId));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("createComment_EmptyContent_ThrowsException")
    void createComment_EmptyContent_ThrowsException() {
        // Given
        validCommentRequest.setContent("");

        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setUserId(2L);
        post.setStatus(1);
        when(blogPostMapper.selectById(testPostId)).thenReturn(post);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.createComment(validCommentRequest, testUserId));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("评论内容不能为空"));
    }

    @Test
    @DisplayName("createComment_ContentTooLong_ThrowsException")
    void createComment_ContentTooLong_ThrowsException() {
        // Given
        validCommentRequest.setContent("a".repeat(2001));

        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setUserId(2L);
        post.setStatus(1);
        when(blogPostMapper.selectById(testPostId)).thenReturn(post);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.createComment(validCommentRequest, testUserId));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("评论内容不能超过2000字符"));
    }

    @Test
    @DisplayName("createComment_WithParent_Success")
    void createComment_WithParent_Success() {
        // Given
        validCommentRequest.setPostId(testPostId);
        validCommentRequest.setContent("Reply Comment");
        validCommentRequest.setParentId(testCommentId);

        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setUserId(2L);
        post.setStatus(1);

        BlogComment parentComment = new BlogComment();
        parentComment.setId(testCommentId);
        parentComment.setPostId(testPostId);
        parentComment.setUserId(3L);

        when(blogPostMapper.selectById(testPostId)).thenReturn(post);
        when(blogCommentMapper.selectById(testCommentId)).thenReturn(parentComment);

        // When
        Long commentId = blogCommentService.createComment(validCommentRequest, testUserId);

        // Then
        assertNotNull(commentId);
    }

    @Test
    @DisplayName("createComment_ParentNotFound_ThrowsException")
    void createComment_ParentNotFound_ThrowsException() {
        // Given
        validCommentRequest.setParentId(999L);

        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setUserId(2L);
        post.setStatus(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(post);
        when(blogCommentMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.createComment(validCommentRequest, testUserId));
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("父评论不存在"));
    }

    @Test
    @DisplayName("createComment_ParentBelongsToDifferentPost_ThrowsException")
    void createComment_ParentBelongsToDifferentPost_ThrowsException() {
        // Given
        validCommentRequest.setPostId(testPostId);
        validCommentRequest.setParentId(testCommentId);

        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setUserId(2L);
        post.setStatus(1);

        BlogComment parentComment = new BlogComment();
        parentComment.setId(testCommentId);
        parentComment.setPostId(999L); // Different post

        when(blogPostMapper.selectById(testPostId)).thenReturn(post);
        when(blogCommentMapper.selectById(testCommentId)).thenReturn(parentComment);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.createComment(validCommentRequest, testUserId));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("父评论不属于该文章"));
    }

    @Test
    @DisplayName("deleteComment_Success_NoException")
    void deleteComment_Success_NoException() {
        // Given
        BlogComment existingComment = new BlogComment();
        existingComment.setId(testCommentId);
        existingComment.setPostId(testPostId);
        existingComment.setUserId(testUserId);

        when(blogCommentMapper.selectById(testCommentId)).thenReturn(existingComment);

        // When & Then
        assertDoesNotThrow(() ->
                blogCommentService.deleteComment(testCommentId, testUserId));
    }

    @Test
    @DisplayName("deleteComment_CommentNotFound_ThrowsException")
    void deleteComment_CommentNotFound_ThrowsException() {
        // Given
        when(blogCommentMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.deleteComment(999L, testUserId));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("deleteComment_NotOwner_ThrowsException")
    void deleteComment_NotOwner_ThrowsException() {
        // Given
        BlogComment existingComment = new BlogComment();
        existingComment.setId(testCommentId);
        existingComment.setPostId(testPostId);
        existingComment.setUserId(999L); // Different user

        when(blogCommentMapper.selectById(testCommentId)).thenReturn(existingComment);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.deleteComment(testCommentId, testUserId));
        assertEquals(403, exception.getCode());
    }

    @Test
    @DisplayName("getCommentsByPostId_Success_ReturnsCommentList")
    void getCommentsByPostId_Success_ReturnsCommentList() {
        // Given
        BlogPost post = new BlogPost();
        post.setId(testPostId);
        post.setStatus(1);

        when(blogPostMapper.selectById(testPostId)).thenReturn(post);
        when(blogCommentMapper.selectList(any())).thenReturn(List.of());

        // When
        IPage<com.selfblog.vo.post.CommentVO> comments =
                blogCommentService.getCommentsByPostId(testPostId, 1, 10);

        // Then
        assertNotNull(comments);
    }

    @Test
    @DisplayName("getCommentsByPostId_PostNotFound_ThrowsException")
    void getCommentsByPostId_PostNotFound_ThrowsException() {
        // Given
        when(blogPostMapper.selectById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                blogCommentService.getCommentsByPostId(999L, 1, 10));
        assertEquals(404, exception.getCode());
    }
}