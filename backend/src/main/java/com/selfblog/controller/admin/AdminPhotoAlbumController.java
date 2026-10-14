package com.selfblog.controller.admin;

import com.selfblog.common.result.Result;
import com.selfblog.dto.site.AlbumForm;
import com.selfblog.service.site.PhotoAlbumService;
import com.selfblog.vo.site.PhotoAlbumVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/albums")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminPhotoAlbumController {
    private final PhotoAlbumService albumService;

    @GetMapping
    public Result<List<PhotoAlbumVO>> list() {
        return Result.success(albumService.getAdminAlbums());
    }

    @PostMapping
    public Result<Long> create(@Valid @org.springframework.web.bind.annotation.RequestBody AlbumForm form) {
        return Result.success(albumService.createAlbum(form));
    }

    @PutMapping("/{albumId}")
    public Result<Void> update(@PathVariable Long albumId, @Valid @org.springframework.web.bind.annotation.RequestBody AlbumForm form) {
        albumService.updateAlbum(albumId, form);
        return Result.success(null);
    }

    @DeleteMapping("/{albumId}")
    public Result<Void> delete(@PathVariable Long albumId) {
        albumService.deleteAlbum(albumId);
        return Result.success(null);
    }

    @PostMapping("/{albumId}/photos")
    public Result<List<PhotoAlbumVO>> upload(@PathVariable Long albumId, @RequestParam("files") MultipartFile[] files) {
        return Result.success(albumService.uploadPhotos(albumId, files));
    }

    @DeleteMapping("/{albumId}/photos/{photoId}")
    public Result<Void> deletePhoto(@PathVariable Long albumId, @PathVariable Long photoId) {
        albumService.deletePhoto(albumId, photoId);
        return Result.success(null);
    }
}
