package com.selfblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.controller.post.BlogPostController;
import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.post.PostListResponse;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BlogPostController 单元测试
 */
@WebMvcTest(BlogPostController.class)
class BlogPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogPostService blogPostService;

    @MockBean
    private JwtUtils jwtUtils;

    private PostCreateRequest validPostRequest;
    private PostQueryRequest queryRequest;

    @BeforeEach
    void setUp() {
        validPostRequest = new PostCreateRequest();
        validPostRequest.setTitle("Test Title");
        validPostRequest.setContent("Test Content");
        validPostRequest.setCategory("Test Category");

        queryRequest = new PostQueryRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);

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
    @DisplayName("createPost_Success_ReturnsPostId")
    void createPost_Success_ReturnsPostId() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        when(blogPostService.createPost(any(PostCreateRequest.class), eq(1L), eq(false)))
                .thenReturn(100L);

        // When & Then
        mockMvc.perform(post("/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Title\",\"content\":\"Test Content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(100L));
    }

    @Test
    @DisplayName("createPost_NotLoggedIn_Returns401")
    void createPost_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty (not logged in)

        // When & Then
        mockMvc.perform(post("/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Title\",\"content\":\"Test Content\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("createPost_EmptyTitle_Returns400")
    void createPost_EmptyTitle_Returns400() throws Exception {
        // Given
        setUpSecurityContext(1L, false);

        // When & Then
        mockMvc.perform(post("/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"Test Content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createPost_OverlongCategoryOrCover_Returns400InsteadOfDatabaseFailure")
    void createPost_OverlongCategoryOrCover_Returns400InsteadOfDatabaseFailure() throws Exception {
        // blog_post.category 是 VARCHAR(50)、cover_url 是 VARCHAR(500)：
        // DTO 必须在这里挡住，否则 MySQL/H2 会以数据库错误返回 500（SQLite 甚至静默存下）。
        setUpSecurityContext(1L, false);
        String longCategory = "x".repeat(51);
        String longCover = "https://cdn.example.com/" + "a".repeat(500) + ".jpg";

        mockMvc.perform(post("/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"标题\",\"content\":\"正文\",\"category\":\"" + longCategory + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"标题\",\"content\":\"正文\",\"coverImage\":\"" + longCover + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updatePost_Success_Returns200")
    void updatePost_Success_Returns200() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        doNothing().when(blogPostService).updatePost(any(PostCreateRequest.class), eq(1L), eq(false));

        // When & Then
        mockMvc.perform(put("/post/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"content\":\"Updated Content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("updatePost_NotLoggedIn_Returns401")
    void updatePost_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty

        // When & Then
        mockMvc.perform(put("/post/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"content\":\"Updated Content\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("deletePost_Success_Returns200")
    void deletePost_Success_Returns200() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        doNothing().when(blogPostService).deletePost(eq(100L), eq(1L), eq(false));

        // When & Then
        mockMvc.perform(delete("/post/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("deletePost_NotLoggedIn_Returns401")
    void deletePost_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty

        // When & Then
        mockMvc.perform(delete("/post/100"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("getPostDetail_Success_ReturnsPostDetail")
    void getPostDetail_Success_ReturnsPostDetail() throws Exception {
        // Given
        PostDetailResponse response = new PostDetailResponse();
        response.setId(100L);
        response.setTitle("Test Post");
        when(blogPostService.getPostDetail(100L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/post/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("getPostList_Success_ReturnsPage")
    void getPostList_Success_ReturnsPage() throws Exception {
        // Given
        IPage<PostListResponse> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(blogPostService.getPostList(any(PostQueryRequest.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/post/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("getPostList_WithKeyword_Success_ReturnsFilteredResults")
    void getPostList_WithKeyword_Success_ReturnsFilteredResults() throws Exception {
        // Given
        IPage<PostListResponse> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(blogPostService.getPostList(any(PostQueryRequest.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/post/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("getMyPosts_NotLoggedIn_Returns401")
    void getMyPosts_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty

        // When & Then
        mockMvc.perform(get("/post/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("getMyPosts_Success_Returns200")
    void getMyPosts_Success_Returns200() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        IPage<PostListResponse> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(blogPostService.getMyPosts(eq(1L), anyInt(), anyInt())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/post/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("saveDraft_NotLoggedIn_Returns401")
    void saveDraft_NotLoggedIn_Returns401() throws Exception {
        // Given - SecurityContext is empty

        // When & Then
        mockMvc.perform(post("/post/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft Title\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("saveDraft_Success_ReturnsDraftId")
    void saveDraft_Success_ReturnsDraftId() throws Exception {
        // Given
        setUpSecurityContext(1L, false);
        when(blogPostService.saveDraft(eq(1L), any())).thenReturn(200L);

        // When & Then
        mockMvc.perform(post("/post/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Draft Title\",\"content\":\"Draft Content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(200L));
    }
}