package com.selfblog.controller.content;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.content.TopicCreateRequest;
import com.selfblog.entity.Topic;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.service.circle.CircleService;
import com.selfblog.service.content.TopicService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.circle.CirclePostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 话题控制器
 */
@Slf4j
@Tag(name = "话题", description = "话题相关接口")
@RestController
@RequestMapping("/topic")
@Validated
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    private final CircleService circleService;

    private final CirclePostMapper circlePostMapper;

    private final com.selfblog.service.post.PostQueryService postQueryService;

    private Map<String, Object> toTopicMap(Topic topic, Map<Long, Long> postCountMap) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", topic.getId());
        map.put("name", topic.getName());
        map.put("description", topic.getDescription());
        map.put("postCount", postCountMap != null ? postCountMap.getOrDefault(topic.getId(), 0L) : circlePostMapper.countByTopicId(topic.getId()));
        map.put("trendingScore", topic.getTrendingScore());
        map.put("status", topic.getStatus());
        map.put("createTime", topic.getCreateTime());
        return map;
    }

    private Map<String, Object> toTopicMap(Topic topic) {
        return toTopicMap(topic, null);
    }

    private List<Map<String, Object>> toTopicMapList(List<Topic> topics) {
        if (topics.isEmpty()) return new ArrayList<>();
        List<Long> topicIds = topics.stream().map(Topic::getId).collect(Collectors.toList());
        Map<Long, Long> postCountMap = circlePostMapper.countByTopicIds(topicIds);
        return topics.stream().map(t -> toTopicMap(t, postCountMap)).collect(Collectors.toList());
    }

    /**
     * 创建话题（登录用户）
     */
    @RequiresAuth
    @Operation(summary = "创建话题")
    @PostMapping
    public Result<Long> createTopic(@Valid @RequestBody TopicCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Long topicId = topicService.createTopic(request.getName(), request.getDescription());
        return Result.success(topicId);
    }

    /**
     * 获取话题列表（分页）
     */
    @Operation(summary = "获取话题列表")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> getTopicList(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        IPage<Topic> topicPage = topicService.getTopicList(page, pageSize);
        List<Long> topicIds = topicPage.getRecords().stream().map(Topic::getId).collect(Collectors.toList());
        Map<Long, Long> postCountMap = topicIds.isEmpty() ? new HashMap<>() : circlePostMapper.countByTopicIds(topicIds);
        IPage<Map<String, Object>> result = topicPage.convert(t -> toTopicMap(t, postCountMap));
        return Result.success(result);
    }

    /**
     * 获取热门话题
     */
    @Operation(summary = "获取热门话题")
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotTopics(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        List<Topic> topics = topicService.getHotTopics(limit);
        List<Map<String, Object>> result = toTopicMapList(topics);
        return Result.success(result);
    }

    /**
     * 获取话题详情
     */
    @Operation(summary = "获取话题详情")
    @GetMapping("/{topicId}")
    public Result<Map<String, Object>> getTopicById(@PathVariable Long topicId) {
        Topic topic = topicService.getTopicById(topicId);
        if (topic == null) {
            throw new BusinessException(404, "话题不存在");
        }
        return Result.success(toTopicMap(topic));
    }

    /**
     * 获取话题下的动态列表
     */
    @Operation(summary = "获取话题下的动态列表")
    @GetMapping("/{topicId}/posts")
    public Result<List<CirclePostVO>> getTopicPosts(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<CirclePostVO> posts = circleService.getPostsByTopic(topicId, page, pageSize, userId);
        return Result.success(posts);
    }

    /**
     * 搜索话题
     */
    @Operation(summary = "搜索话题")
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchTopics(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }
        List<Topic> topics = topicService.searchTopics(keyword);
        List<Map<String, Object>> result = toTopicMapList(topics);
        return Result.success(result);
    }

    /**
     * 获取话题下的博客文章列表
     */
    @Operation(summary = "获取话题下的博客文章")
    @GetMapping("/{topicId}/blog-posts")
    public Result<IPage<com.selfblog.vo.post.PostListResponse>> getTopicBlogPosts(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        topicService.getTopicById(topicId);
        com.selfblog.dto.post.PostAdvancedSearchRequest searchRequest = new com.selfblog.dto.post.PostAdvancedSearchRequest();
        searchRequest.setPageNum(page);
        searchRequest.setPageSize(pageSize);
        searchRequest.setTopicId(topicId);
        IPage<com.selfblog.vo.post.PostListResponse> result = postQueryService.advancedSearch(searchRequest);
        return Result.success(result);
    }
}