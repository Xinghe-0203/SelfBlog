package com.selfblog.controller.content;

import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.dto.social.MediaQueryRequest;
import com.selfblog.service.content.MediaService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.content.MediaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 媒体控制器
 */
@Tag(name = "媒体管理", description = "媒体文件上传相关接口")
@RestController
@RequestMapping("/media")
@Validated
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * 上传媒体文件
     */
    @RequiresAuth
    @Operation(summary = "上传媒体文件")
    @PostMapping("/upload")
    public Result<MediaVO> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "article") String type) {
        Long userId = SecurityUtils.getCurrentUserId();

        MediaVO media = mediaService.uploadMedia(file, userId, type);
        return Result.success(media);
    }

    /**
     * 删除媒体文件
     */
    @RequiresAuth
    @Operation(summary = "删除媒体文件")
    @DeleteMapping("/{mediaId}")
    public Result<Void> deleteMedia(@PathVariable Long mediaId) {
        Long userId = SecurityUtils.getCurrentUserId();

        mediaService.deleteMedia(mediaId, userId);
        return Result.success(null);
    }

    /**
     * 获取媒体信息
     */
    @RequiresAuth
    @Operation(summary = "获取媒体信息")
    @GetMapping("/{mediaId}")
    public Result<MediaVO> getMediaInfo(@PathVariable Long mediaId) {
        Long userId = SecurityUtils.getCurrentUserId();
        MediaVO media = mediaService.getMediaInfo(mediaId);
        return Result.success(media);
    }

    /**
     * 绑定文章媒体
     */
    @RequiresAuth
    @Operation(summary = "绑定文章媒体")
    @PutMapping("/bind/{postId}")
    public Result<Void> bindPostMedia(@PathVariable Long postId, @Valid @RequestBody @Size(max = 20, message = "最多绑定20个媒体") List<Long> mediaIds) {
        Long userId = SecurityUtils.getCurrentUserId();

        mediaService.bindPostMedia(postId, mediaIds);
        return Result.success(null);
    }

    /**
     * 获取文章的媒体列表（公开，已发布文章均可查看）
     */
    @Operation(summary = "获取文章的媒体列表")
    @GetMapping("/post/{postId}")
    public Result<List<MediaVO>> getPostMedia(@PathVariable Long postId) {
        List<MediaVO> mediaList = mediaService.getPostMedia(postId);
        return Result.success(mediaList);
    }

    /**
     * 批量上传媒体文件
     */
    @RequiresAuth
    @Operation(summary = "批量上传媒体文件（最多9个）")
    @PostMapping("/upload/multiple")
    public Result<List<MediaVO>> uploadMultipleMedia(
            @RequestParam("files") @Size(min = 1, max = 10, message = "请选择1-10个文件") MultipartFile[] files,
            @RequestParam(value = "type", defaultValue = "article") String type) {
        Long userId = SecurityUtils.getCurrentUserId();

        List<MediaVO> mediaList = mediaService.uploadFiles(files, userId, type);
        return Result.success(mediaList);
    }

    /**
     * 分页查询用户媒体列表
     */
    @RequiresAuth
    @Operation(summary = "分页查询用户媒体列表")
    @GetMapping("/list")
    public Result<List<MediaVO>> getUserMediaList(@Valid MediaQueryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        List<MediaVO> mediaList = mediaService.getUserMedia(request, userId);
        return Result.success(mediaList);
    }
}