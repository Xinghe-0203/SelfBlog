package com.selfblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.controller.post.BlogCommentController;
import com.selfblog.dto.social.CommentCreateRequest;
import com.selfblog.service.social.BlogCommentService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.post.CommentVO;
import com.selfblog.vo.post.CommentWithPostVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BlogCommentController 单元测试
 */
@WebMvcTest(BlogCommentController.class)
class BlogCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogCommentService blogCommentService;

    @MockBean
    private JwtUtils jwtUtils;

    private CommentCreateRequest validCommentRequest;

    @BeforeEach
    void setUp() {
        validCommentRequest = new CommentCreateRequest();
        validCommentRequest.setPostId(1L);
        validCommentRequest.setContent("Test Comment Content");

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Long userId, boolean isAdmin) {
        UserContext userContext = new UserContext(userId, isAdmin ? "admin" : "user");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userContext, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("createComment_Success_ReturnsCommentId")
    void createComment_Success_ReturnsCommentId() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        when(blogCommentService.createComment(any(CommentCreateRequest.class), eq(1L)))
                .thenReturn(100L);

        // When & Then
        mockMvc.perform(post("/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":1,\"content\":\"Test Comment Content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(100L));
    }

    @Test
    @DisplayName("createComment_NotLoggedIn_Returns401")
    void createComment_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty

        // When & Then
        mockMvc.perform(post("/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":1,\"content\":\"Test Comment Content\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("createComment_EmptyContent_Returns400")
    void createComment_EmptyContent_Returns400() throws Exception {
        // Given
        setUpSecurityContext(1L, false);

        // When & Then
        mockMvc.perform(post("/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":1,\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getCommentsByPostId_Success_ReturnsCommentList")
    void getCommentsByPostId_Success_ReturnsCommentList() throws Exception {
        // Given
        CommentVO comment = new CommentVO();
        comment.setId(1L);
        comment.setContent("Test Comment");
        Page<CommentVO> page = new Page<>(1, 10);
        page.setRecords(List.of(comment));
        when(blogCommentService.getCommentsByPostId(eq(1L), anyInt(), anyInt()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/comment/post/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(1));
    }

    @Test
    @DisplayName("getCommentsByPostId_EmptyList_ReturnsEmptyArray")
    void getCommentsByPostId_EmptyList_ReturnsEmptyArray() throws Exception {
        // Given
        Page<CommentVO> emptyPage = new Page<>(1, 10);
        when(blogCommentService.getCommentsByPostId(eq(1L), anyInt(), anyInt()))
                .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/comment/post/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    @DisplayName("getCommentById_Success_ReturnsComment")
    void getCommentById_Success_ReturnsComment() throws Exception {
        // Given
        CommentVO comment = new CommentVO();
        comment.setId(1L);
        comment.setContent("Test Comment");
        when(blogCommentService.getCommentById(1L)).thenReturn(comment);

        // When & Then
        mockMvc.perform(get("/comment/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("getCommentById_NotFound_Returns404")
    void getCommentById_NotFound_Returns404() throws Exception {
        // Given
        when(blogCommentService.getCommentById(999L))
                .thenThrow(new BusinessException(404, "评论不存在"));

        // When & Then
        mockMvc.perform(get("/comment/999"))
                .andExpect(status().is(404));
    }

    @Test
    @DisplayName("deleteComment_Success_Returns200")
    void deleteComment_Success_Returns200() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        doNothing().when(blogCommentService).deleteComment(1L, 1L);

        // When & Then
        mockMvc.perform(delete("/comment/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("deleteComment_NotLoggedIn_Returns401")
    void deleteComment_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty

        // When & Then
        mockMvc.perform(delete("/comment/1"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("deleteComment_NotOwner_Returns403")
    void deleteComment_NotOwner_Returns403() throws Exception {
        // Given
        setUpSecurityContext(2L, false);
        doThrow(new BusinessException(403, "无权删除此评论"))
                .when(blogCommentService).deleteComment(1L, 2L);

        // When & Then
        mockMvc.perform(delete("/comment/1"))
                .andExpect(status().is(403));
    }

    @Test
    @DisplayName("getMyComments_Success_ReturnsPage")
    void getMyComments_Success_ReturnsPage() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        IPage<CommentWithPostVO> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(blogCommentService.getMyComments(eq(1L), anyInt(), anyInt()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/comment/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("getMyComments_NotLoggedIn_Returns401")
    void getMyComments_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty

        // When & Then
        mockMvc.perform(get("/comment/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().is(401));
    }
}