package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.VisitorEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface VisitorEventMapper extends BaseMapper<VisitorEvent> {
    @Select("SELECT COUNT(DISTINCT visitor_hash) FROM visitor_event")
    Long countAllVisitors();

    @Select("SELECT COUNT(DISTINCT visitor_hash) FROM visitor_event WHERE create_time >= #{since} AND create_time < #{until}")
    Long countVisitorsBetween(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);
}
