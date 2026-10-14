package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("album_photo")
public class AlbumPhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long albumId;
    private String filePath;
    private String mimeType;
    private String caption;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private Integer isDeleted;
}
