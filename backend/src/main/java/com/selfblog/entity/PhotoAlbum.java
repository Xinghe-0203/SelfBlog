package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("photo_album")
public class PhotoAlbum {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private Integer isPrivate;
    private String passwordHash;
    private Long coverPhotoId;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
