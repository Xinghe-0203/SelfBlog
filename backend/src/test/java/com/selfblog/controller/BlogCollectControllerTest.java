package com.selfblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.controller.post.BlogCollectController;
import com.selfblog.service.social.BlogCollectService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.post.CollectItemVO;
import com.selfblog.vo.post.CollectResultVO;
import com.selfblog.vo.post.CollectStatusVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BlogCollectController 单元测试
 */
@WebMvcTest(BlogCollectController.class)
class BlogCollectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogCollectService blogCollectService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Long userId) {
        UserContext ctx = new UserContext(userId, "user");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("收藏/取消收藏成功")
    void toggleCollect_Success() throws Exception {
        setUpSecurityContext(1L);
        CollectResultVO result = new CollectResultVO();
        result.setAction("collect");
        result.setCollectCount(1);
        when(blogCollectService.toggleCollect(eq(100L), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/collect/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.action").value("collect"));
    }

    @Test
    @DisplayName("收藏 - 未登录返回401")
    void toggleCollect_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(post("/collect/100"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("检查收藏状态")
    void checkCollectStatus_Success() throws Exception {
        CollectStatusVO status = new CollectStatusVO();
        status.setCollected(true);
        status.setCollectCount(5);
        when(blogCollectService.checkCollectStatus(eq(100L), eq(1L))).thenReturn(status);

        setUpSecurityContext(1L);

        mockMvc.perform(get("/collect/100/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.collected").value(true));
    }

    @Test
    @DisplayName("获取我的收藏列表")
    void getMyCollections_Success() throws Exception {
        setUpSecurityContext(1L);
        IPage<CollectItemVO> page = new Page<>(1, 10, 0);
        when(blogCollectService.getMyCollections(eq(1L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/collect/my")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取收藏列表 - 未登录返回401")
    void getMyCollections_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(get("/collect/my"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("批量检查收藏状态")
    void checkCollectStatusBatch_Success() throws Exception {
        setUpSecurityContext(1L);
        when(blogCollectService.checkCollectStatusBatch(anyList(), eq(1L)))
                .thenReturn(List.of(true, false, true));

        mockMvc.perform(post("/collect/check/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value(true));
    }

    @Test
    @DisplayName("批量检查 - 空列表返回400")
    void checkCollectStatusBatch_EmptyList_Returns400() throws Exception {
        setUpSecurityContext(1L);

        mockMvc.perform(post("/collect/check/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }
}
