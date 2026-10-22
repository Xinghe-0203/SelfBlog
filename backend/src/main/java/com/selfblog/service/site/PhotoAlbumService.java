package com.selfblog.service.site;

import com.selfblog.dto.site.AlbumForm;
import com.selfblog.vo.site.AlbumAccessVO;
import com.selfblog.vo.site.AlbumImage;
import com.selfblog.vo.site.PhotoAlbumVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PhotoAlbumService {
    List<PhotoAlbumVO> getPublicAlbums();
    List<PhotoAlbumVO> getAdminAlbums();
    PhotoAlbumVO getPublicAlbum(Long albumId, String accessToken);
    AlbumAccessVO unlock(Long albumId, String password);
    Long createAlbum(AlbumForm form);
    void updateAlbum(Long albumId, AlbumForm form);
    void deleteAlbum(Long albumId);
    List<PhotoAlbumVO> uploadPhotos(Long albumId, MultipartFile[] files);
    void deletePhoto(Long albumId, Long photoId);

    /**
     * 更新单张照片的图注（alt 文本）。{@code null} 或空白表示清除。
     *
     * <p>只改 caption，不动文件与排序；照片必须属于该相册，否则 404。</p>
     */
    void updatePhotoCaption(Long albumId, Long photoId, String caption);
    AlbumImage getImage(Long photoId, String accessToken);
}
