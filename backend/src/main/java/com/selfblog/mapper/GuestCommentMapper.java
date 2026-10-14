package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.GuestComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuestCommentMapper extends BaseMapper<GuestComment> {
}
