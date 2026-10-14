package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("site_setting")
public class SiteSetting implements Serializable {
    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;
    private String settingValue;
    private LocalDateTime updateTime;
}
