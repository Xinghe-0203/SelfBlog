package com.selfblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.controller.content.TopicController;
import com.selfblog.entity.Topic;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.service.circle.CircleService;
import com.selfblog.service.content.TopicService;
import com.selfblog.service.post.PostQueryService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TopicController 单元测试
 */
@WebMvcTest(TopicController.class)
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopicService topicService;

    @MockBean
    private CircleService circleService;

    @MockBean
    private CirclePostMapper circlePostMapper;

    @MockBean
    private PostQueryService postQueryService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Long userId) {
        UserContext ctx = new UserContext(userId, "user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ctx, null, Collections.emptyList()));
    }

    private Topic createTopic(Long id, String name) {
        Topic t = new Topic();
        t.setId(id);
        t.setName(name);
        t.setDescription("描述");
        t.setTrendingScore(10);
        t.setStatus(1);
        return t;
    }

    @Test
    @DisplayName("创建话题成功")
    void createTopic_Success() throws Exception {
        setUpSecurityContext(1L);
        when(topicService.createTopic("Java", "Java编程")).thenReturn(1L);

        mockMvc.perform(post("/topic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\",\"description\":\"Java编程\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("创建话题 - 未登录返回401")
    void createTopic_NotLoggedIn_Returns401() throws Exception {
        mockMvc.perform(post("/topic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().is(401));
    }

    @Test
    @DisplayName("获取话题列表")
    void getTopicList_Success() throws Exception {
        Page<Topic> page = new Page<>(1, 20, 0);
        when(topicService.getTopicList(anyInt(), anyInt())).thenReturn(page);
        when(circlePostMapper.countByTopicIds(anyList())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/topic/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取热门话题")
    void getHotTopics_Success() throws Exception {
        when(topicService.getHotTopics(10)).thenReturn(List.of(createTopic(1L, "Java")));
        when(circlePostMapper.countByTopicIds(anyList())).thenReturn(Map.of(1L, 5L));

        mockMvc.perform(get("/topic/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取话题详情")
    void getTopicById_Success() throws Exception {
        when(topicService.getTopicById(1L)).thenReturn(createTopic(1L, "Java"));
        when(circlePostMapper.countByTopicId(1L)).thenReturn(5L);

        mockMvc.perform(get("/topic/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Java"));
    }

    @Test
    @DisplayName("获取话题详情 - 不存在抛404")
    void getTopicById_NotFound_Returns404() throws Exception {
        when(topicService.getTopicById(999L)).thenThrow(new com.selfblog.common.exception.BusinessException(404, "话题不存在"));

        mockMvc.perform(get("/topic/999"))
                .andExpect(status().is(404));
    }

    @Test
    @DisplayName("搜索话题")
    void searchTopics_Success() throws Exception {
        when(topicService.searchTopics("Java")).thenReturn(List.of(createTopic(1L, "Java")));
        when(circlePostMapper.countByTopicIds(anyList())).thenReturn(Map.of(1L, 5L));

        mockMvc.perform(get("/topic/search").param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("搜索话题 - 空关键词返回空")
    void searchTopics_EmptyKeyword() throws Exception {
        mockMvc.perform(get("/topic/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
