package com.selfblog.controller.admin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.entity.CirclePost;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.circle.CircleService;
import com.selfblog.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "管理员-校友圈管理", description = "管理员校友圈动态管理接口")
@RestController
@RequestMapping("/admin/circle")
@Validated
@RequiredArgsConstructor
public class AdminCircleController {

    private final CircleService circleService;

    private final CirclePostMapper circlePostMapper;

    private final SysUserMapper sysUserMapper;

    @Operation(summary = "获取动态列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<Map<String, Object>>> getCircleList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        LambdaQueryWrapper<CirclePost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePost::getIsDeleted, 0);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(CirclePost::getContent, keyword.trim());
        }
        if (userId != null) {
            wrapper.eq(CirclePost::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(CirclePost::getStatus, status);
        }
        wrapper.orderByDesc(CirclePost::getCreateTime);

        IPage<CirclePost> page = circlePostMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Long> userIds = page.getRecords().stream()
                .map(CirclePost::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(SysUser::getId, userIds);
            sysUserMapper.selectList(userWrapper)
                    .forEach(u -> userMap.put(u.getId(), u));
        }

        IPage<Map<String, Object>> result = page.convert(post -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", post.getId());
            map.put("userId", post.getUserId());
            SysUser user = userMap.get(post.getUserId());
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("nickname", user.getNickname());
                map.put("avatar", user.getAvatar());
            } else {
                map.put("username", null);
                map.put("nickname", null);
                map.put("avatar", null);
            }
            map.put("content", post.getContent());
            map.put("contentType", post.getContentType());
            map.put("imageUrls", StrUtil.isNotBlank(post.getImageUrls())
                    ? JSONUtil.toList(post.getImageUrls(), String.class) : null);
            map.put("videoUrls", StrUtil.isNotBlank(post.getVideoUrls())
                    ? JSONUtil.toList(post.getVideoUrls(), String.class) : null);
            map.put("location", post.getLocation());
            map.put("topicIds", post.getTopicIds());
            map.put("tags", StrUtil.isNotBlank(post.getTags())
                    ? JSONUtil.toList(post.getTags(), String.class) : null);
            map.put("viewCount", post.getViewCount());
            map.put("likeCount", post.getLikeCount());
            map.put("commentCount", post.getCommentCount());
            map.put("repostCount", post.getRepostCount());
            map.put("isTop", post.getIsTop());
            map.put("visibility", post.getVisibility());
            map.put("status", post.getStatus());
            map.put("createTime", post.getCreateTime());
            return map;
        });

        return Result.success(result);
    }

    @Operation(summary = "获取动态详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> getPostDetail(@PathVariable Long id) {
        CirclePost post = circlePostMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        SysUser user = sysUserMapper.selectById(post.getUserId());
        if (user != null) {
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("avatar", user.getAvatar());
        }
        map.put("content", post.getContent());
        map.put("contentType", post.getContentType());
        map.put("imageUrls", StrUtil.isNotBlank(post.getImageUrls())
                ? JSONUtil.toList(post.getImageUrls(), String.class) : null);
        map.put("videoUrls", StrUtil.isNotBlank(post.getVideoUrls())
                ? JSONUtil.toList(post.getVideoUrls(), String.class) : null);
        map.put("location", post.getLocation());
        map.put("topicIds", post.getTopicIds());
        map.put("tags", StrUtil.isNotBlank(post.getTags())
                ? JSONUtil.toList(post.getTags(), String.class) : null);
        map.put("viewCount", post.getViewCount());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        map.put("repostCount", post.getRepostCount());
        map.put("isTop", post.getIsTop());
        map.put("visibility", post.getVisibility());
        map.put("status", post.getStatus());
        map.put("createTime", post.getCreateTime());
        return Result.success(map);
    }

    @Operation(summary = "删除动态")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> deletePost(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        circleService.deletePost(id, adminId);
        return Result.success(null);
    }

    @Operation(summary = "切换动态状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> toggleStatus(@PathVariable Long id,
                                     @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值无效，只能为0（隐藏）或1（正常）");
        }
        CirclePost post = circlePostMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "动态不存在");
        }
        CirclePost update = new CirclePost();
        update.setId(id);
        update.setStatus(status);
        circlePostMapper.updateById(update);
        log.info("管理员 {} 切换动态 {} 状态为 {}", SecurityUtils.getCurrentUserId(), id, status);
        return Result.success(null);
    }
}
