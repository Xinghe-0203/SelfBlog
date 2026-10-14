package com.selfblog.controller.site;

import com.selfblog.common.result.Result;
import com.selfblog.service.site.PhotoAlbumService;
import com.selfblog.vo.site.AlbumAccessVO;
import com.selfblog.vo.site.AlbumImage;
import com.selfblog.vo.site.PhotoAlbumVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gallery")
@RequiredArgsConstructor
public class PhotoGalleryController {
    private final PhotoAlbumService albumService;

    @GetMapping("/albums")
    public Result<List<PhotoAlbumVO>> albums() {
        return Result.success(albumService.getPublicAlbums());
    }

    @GetMapping("/albums/{albumId}")
    public Result<PhotoAlbumVO> album(@PathVariable Long albumId,
                                      @RequestParam(required = false) String token) {
        return Result.success(albumService.getPublicAlbum(albumId, token));
    }

    @PostMapping("/albums/{albumId}/unlock")
    public Result<AlbumAccessVO> unlock(@PathVariable Long albumId, @RequestBody Map<String, String> body) {
        return Result.success(albumService.unlock(albumId, body.get("password")));
    }

    @GetMapping("/photos/{photoId}/image")
    public ResponseEntity<Resource> image(@PathVariable Long photoId,
                                          @RequestParam(required = false) String token) {
        AlbumImage image = albumService.getImage(photoId, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(image.getPath()));
    }
}
