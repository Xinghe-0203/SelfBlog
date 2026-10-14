package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor_event")
public class VisitorEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String visitorHash;
    private String ipHash;
    private String userAgent;
    private String pagePath;
    private String referrer;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
