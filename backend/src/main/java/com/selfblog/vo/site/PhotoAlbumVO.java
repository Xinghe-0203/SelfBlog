package com.selfblog.vo.site;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PhotoAlbumVO {
    private Long id;
    private String title;
    private String description;
    private boolean privateAlbum;
    private boolean locked;
    private String coverUrl;
    private long photoCount;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private List<AlbumPhotoVO> photos;
}
