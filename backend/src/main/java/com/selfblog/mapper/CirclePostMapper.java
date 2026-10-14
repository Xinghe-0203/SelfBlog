package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.CirclePost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校友圈动态 Mapper 接口
 * 【数据库兼容】MySQL/SQLite - 自定义 SQL 已移至 CirclePostMapper.xml
 */
@Mapper
public interface CirclePostMapper extends BaseMapper<CirclePost> {

    Long countDistinctAuthorsSince(@Param("since") LocalDateTime since);

    void incrementViewCount(@Param("id") Long id);

    void incrementLikeCount(@Param("id") Long id);

    void decrementLikeCount(@Param("id") Long id);

    void incrementCommentCount(@Param("id") Long id);

    void decrementCommentCount(@Param("id") Long id, @Param("count") int count);

    void incrementRepostCount(@Param("id") Long id);

    void decrementRepostCount(@Param("id") Long id);

    Long countByTopicId(@Param("topicId") Long topicId);

    List<Map<String, Object>> countByTopicIdsRaw(@Param("topicIds") List<Long> topicIds);

    default Map<Long, Long> countByTopicIds(List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return new HashMap<>();
        return countByTopicIdsRaw(topicIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        m -> ((Number) m.get("topicId")).longValue(),
                        m -> ((Number) m.get("cnt")).longValue(),
                        (a, b) -> a
                ));
    }
}
