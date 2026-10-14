package com.selfblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.controller.social.FollowController;
import com.selfblog.service.social.FollowService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.social.FollowStatusVO;
import com.selfblog.vo.user.UserVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FollowController 单元测试
 */
@WebMvcTest(FollowController.class)
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FollowService followService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Long userId, boolean isAdmin) {
        UserContext ctx = new UserContext(userId, isAdmin ? "admin" : "user");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("关注用户成功")
    void follow_Success() throws Exception {
        setUpSecurityContext(1L, false);
        FollowStatusVO result = new FollowStatusVO();
        result.setFollowing(true);
        result.setAction("follow");
        when(followService.follow(eq(2L), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.following").value(true));
    }

    @Test
    @DisplayName("关注 - 未登录返回401")
    void follow_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(post("/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("取消关注成功")
    void unfollow_Success() throws Exception {
        setUpSecurityContext(1L, false);
        FollowStatusVO result = new FollowStatusVO();
        result.setFollowing(false);
        result.setAction("unfollow");
        when(followService.unfollow(eq(2L), eq(1L))).thenReturn(result);

        mockMvc.perform(delete("/follow/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.following").value(false));
    }

    @Test
    @DisplayName("取消关注 - 未登录返回401")
    void unfollow_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(delete("/follow/2"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("检查关注状态")
    void checkFollow_Success() throws Exception {
        when(followService.isFollowing(eq(2L), eq(1L))).thenReturn(true);
        FollowService.FollowCountsVO counts = new FollowService.FollowCountsVO();
        counts.setFollowerCount(10);
        counts.setFollowingCount(5);
        when(followService.getCounts(eq(2L))).thenReturn(counts);

        setUpSecurityContext(1L, false);

        mockMvc.perform(get("/follow/2/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.following").value(true));
    }

    @Test
    @DisplayName("获取粉丝列表")
    void getFollowers_Success() throws Exception {
        IPage<UserVO> page = new Page<>(1, 10, 0);
        when(followService.getFollowers(eq(1L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/follow/followers/1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取关注列表")
    void getFollowing_Success() throws Exception {
        IPage<UserVO> page = new Page<>(1, 10, 0);
        when(followService.getFollowing(eq(1L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/follow/following/1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取关注计数")
    void getCounts_Success() throws Exception {
        FollowService.FollowCountsVO counts = new FollowService.FollowCountsVO();
        counts.setFollowerCount(100);
        counts.setFollowingCount(50);
        when(followService.getCounts(eq(1L))).thenReturn(counts);

        mockMvc.perform(get("/follow/counts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.followerCount").value(100));
    }
}
