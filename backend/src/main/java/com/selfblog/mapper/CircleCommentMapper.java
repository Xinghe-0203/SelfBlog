package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.CircleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 校友圈评论 Mapper
 */
@Mapper
public interface CircleCommentMapper extends BaseMapper<CircleComment> {
    /**
     * 根据动态ID删除所有评论及子回复（逻辑删除）
     * 同时删除一级评论和二级回复（parent_id 在被删评论 ID 范围内）
     */
    @Update("UPDATE blog_circle_comment SET is_deleted = 1 " +
            "WHERE post_id = #{postId} OR parent_id IN " +
            "(SELECT temp.id FROM (SELECT id FROM blog_circle_comment WHERE post_id = #{postId}) AS temp)")
    int logicalDeleteByPostId(@Param("postId") Long postId);
}
