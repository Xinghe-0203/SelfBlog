package com.selfblog.controller;

import com.selfblog.controller.post.BlogTagController;
import com.selfblog.entity.BlogTag;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
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
 * BlogTagController 单元测试
 */
@WebMvcTest(BlogTagController.class)
class BlogTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogTagService blogTagService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Long userId, boolean isAdmin) {
        UserContext ctx = new UserContext(userId, isAdmin ? "admin" : "user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList()));
    }

    private BlogTag createTag(Long id, String name) {
        BlogTag tag = new BlogTag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    @Test
    @DisplayName("获取所有标签")
    void listAllTags_Success() throws Exception {
        when(blogTagService.listAllTags()).thenReturn(List.of(createTag(1L, "Java"), createTag(2L, "Vue")));

        mockMvc.perform(get("/tag/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("根据ID获取标签")
    void getTagById_Success() throws Exception {
        when(blogTagService.getById(1L)).thenReturn(createTag(1L, "Java"));

        mockMvc.perform(get("/tag/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Java"));
    }

    @Test
    @DisplayName("根据ID获取标签 - 不存在抛404")
    void getTagById_NotFound_Returns404() throws Exception {
        when(blogTagService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/tag/999"))
                .andExpect(status().is(404));
    }

    @Test
    @DisplayName("创建标签成功")
    void createTag_Success() throws Exception {
        setUpSecurityContext(1L, false);
        BlogTag tag = createTag(1L, "Spring Boot");
        when(blogTagService.createTag("Spring Boot")).thenReturn(tag);

        mockMvc.perform(post("/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spring Boot\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Spring Boot"));
    }

    @Test
    @DisplayName("创建标签 - 未登录返回401")
    void createTag_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(post("/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("删除标签 - 非管理员返回403")
    void deleteTag_NotAdmin_Returns403() throws Exception {
        setUpSecurityContext(1L, false);

        mockMvc.perform(delete("/tag/1"))
                .andExpect(status().is(403));
    }

    @Test
    @DisplayName("搜索标签")
    void searchTags_Success() throws Exception {
        when(blogTagService.searchTags("Java")).thenReturn(List.of(createTag(1L, "Java")));

        mockMvc.perform(get("/tag/search").param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("搜索标签 - 空关键词返回空")
    void searchTags_EmptyKeyword() throws Exception {
        mockMvc.perform(get("/tag/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
