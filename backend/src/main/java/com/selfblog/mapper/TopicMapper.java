package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 话题表 Mapper 接口
 * 【数据库兼容】MySQL/SQLite - 自定义 SQL 已移至 TopicMapper.xml
 */
@Mapper
public interface TopicMapper extends BaseMapper<Topic> {

    int incrementPostCount(@Param("topicId") Long topicId);

    int decrementPostCount(@Param("topicId") Long topicId);

    int incrementTrendingScore(@Param("topicId") Long topicId, @Param("score") int score);

    void recalculateAllTrendingScore();

    void recalculateAllPostCount();
}
