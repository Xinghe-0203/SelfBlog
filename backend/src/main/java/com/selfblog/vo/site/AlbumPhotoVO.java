package com.selfblog.vo.site;

import lombok.Data;

@Data
public class AlbumPhotoVO {
    private Long id;
    private String imageUrl;
    private String caption;
    private Integer sortOrder;
}
