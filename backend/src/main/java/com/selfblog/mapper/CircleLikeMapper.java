package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.CircleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 校友圈点赞 Mapper
 */
@Mapper
public interface CircleLikeMapper extends BaseMapper<CircleLike> {
    /**
     * 逻辑删除记录（用于解决软删除+唯一约束冲突问题）
     */
    @Update("UPDATE blog_circle_like SET is_deleted = 1 WHERE id = #{id}")
    int logicalDeleteById(@Param("id") Long id);

    /**
     * 根据动态ID删除所有点赞记录（逻辑删除）
     */
    @Update("UPDATE blog_circle_like SET is_deleted = 1 WHERE post_id = #{postId}")
    int logicalDeleteByPostId(@Param("postId") Long postId);
}
