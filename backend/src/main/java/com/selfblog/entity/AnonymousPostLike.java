package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("anonymous_post_like")
public class AnonymousPostLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private String visitorHash;
    private LocalDateTime createTime;
    private Integer isDeleted;
}
