package com.selfblog.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.entity.Topic;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.mapper.TopicMapper;
import com.selfblog.service.content.TopicService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.utils.SqlLikeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "管理员-话题管理", description = "管理员话题管理接口")
@RestController
@RequestMapping("/admin/topic")
@Validated
@RequiredArgsConstructor
public class AdminTopicController {

    private final TopicService topicService;

    private final TopicMapper topicMapper;

    private final CirclePostMapper circlePostMapper;

    private final HtmlSanitizer htmlSanitizer;

    @Operation(summary = "获取话题列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<Map<String, Object>>> getTopicList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getIsDeleted, 0);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.apply("name LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE,
                    SqlLikeUtils.containsPattern(keyword.trim()));
        }
        if (status != null) {
            wrapper.eq(Topic::getStatus, status);
        }
        wrapper.orderByDesc(Topic::getCreateTime);

        IPage<Topic> page = topicMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Long> topicIds = page.getRecords().stream().map(Topic::getId).collect(Collectors.toList());
        Map<Long, Long> postCountMap = topicIds.isEmpty() ? new HashMap<>() : circlePostMapper.countByTopicIds(topicIds);

        IPage<Map<String, Object>> result = page.convert(topic -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", topic.getId());
            map.put("name", topic.getName());
            map.put("description", topic.getDescription());
            map.put("postCount", postCountMap.getOrDefault(topic.getId(), 0L));
            map.put("trendingScore", topic.getTrendingScore());
            map.put("status", topic.getStatus());
            map.put("createTime", topic.getCreateTime());
            map.put("updateTime", topic.getUpdateTime());
            return map;
        });

        return Result.success(result);
    }

    @Operation(summary = "创建话题")
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> createTopic(@Validated @RequestBody CreateTopicRequest request) {
        String sanitizedName = htmlSanitizer.sanitizePlainText(request.getName().trim());
        if (sanitizedName.length() > 50) {
            throw new BusinessException(400, "话题名称不能超过50个字符");
        }
        String description = request.getDescription() != null
                ? htmlSanitizer.sanitizePlainText(request.getDescription().trim())
                : null;
        if (description != null && description.length() > 500) {
            throw new BusinessException(400, "话题描述不能超过500个字符");
        }

        Long topicId = topicService.createTopic(sanitizedName, description);
        Topic topic = topicMapper.selectById(topicId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", topic.getId());
        map.put("name", topic.getName());
        map.put("description", topic.getDescription());
        map.put("postCount", 0);
        map.put("trendingScore", topic.getTrendingScore());
        map.put("status", topic.getStatus());
        map.put("createTime", topic.getCreateTime());
        map.put("updateTime", topic.getUpdateTime());
        return Result.success(map);
    }

    @Operation(summary = "更新话题")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> updateTopic(@PathVariable Long id,
                                                    @Validated @RequestBody UpdateTopicRequest request) {
        // 事务边界在 TopicServiceImpl.updateTopic：控制器只做参数校验与委托
        Topic topic = topicService.updateTopic(id, request.getName(), request.getDescription(), request.getStatus());

        Long postCount = circlePostMapper.countByTopicId(id);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", topic.getId());
        map.put("name", topic.getName());
        map.put("description", topic.getDescription());
        map.put("postCount", postCount);
        map.put("trendingScore", topic.getTrendingScore());
        map.put("status", topic.getStatus());
        map.put("createTime", topic.getCreateTime());
        map.put("updateTime", topic.getUpdateTime());
        return Result.success(map);
    }

    @Operation(summary = "删除话题")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> deleteTopic(@PathVariable Long id) {
        // 事务边界在 TopicServiceImpl.deleteTopic
        topicService.deleteTopic(id);
        log.info("管理员 {} 删除了话题 {}", com.selfblog.utils.SecurityUtils.getCurrentUserId(), id);
        return Result.success(null);
    }

    @Operation(summary = "切换话题状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> toggleStatus(@PathVariable Long id,
                                      @RequestBody Map<String, Integer> body) {
        // 事务边界在 TopicServiceImpl.toggleStatus
        Integer status = body.get("status");
        topicService.toggleStatus(id, status);
        log.info("管理员 {} 切换话题 {} 状态为 {}", com.selfblog.utils.SecurityUtils.getCurrentUserId(), id, status);
        return Result.success(null);
    }

    @Validated
    public static class CreateTopicRequest {
        @NotBlank(message = "话题名称不能为空")
        @Size(max = 50, message = "话题名称不能超过50个字符")
        private String name;

        @Size(max = 500, message = "话题描述不能超过500个字符")
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @Validated
    public static class UpdateTopicRequest {
        @Size(max = 50, message = "话题名称不能超过50个字符")
        private String name;

        @Size(max = 500, message = "话题描述不能超过500个字符")
        private String description;

        private Integer status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}
