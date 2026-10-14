package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 媒体文件实体类 (blog_media)
 */
@Data
@TableName("blog_media")
public class Media implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 文件名称
     */
    @TableField("file_name")
    private String originalName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 存储路径
     */
    private String filePath;

    /**
     * 访问URL
     */
    private String fileUrl;

    /**
     * 缩略图URL
     */
    private String thumbUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型：image/jpeg, image/png, video/mp4 等
     */
    private String mimeType;

    /**
     * 图片宽度（仅图片类型）
     */
    private Integer width;

    /**
     * 图片高度（仅图片类型）
     */
    private Integer height;

    /**
     * 文件状态：1=正常，0=删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-正常，1-删除
     */
    @TableLogic
    private Integer isDeleted;
}