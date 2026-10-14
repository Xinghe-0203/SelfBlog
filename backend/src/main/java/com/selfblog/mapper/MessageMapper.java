package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 私信 Mapper 接口
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}