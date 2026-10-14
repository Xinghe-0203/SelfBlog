package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("guest_comment")
public class GuestComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private String visitorHash;
    private String nickname;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}
