package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BlogLikeMapper extends BaseMapper<BlogLike> {
    /**
     * 逻辑删除记录（用于解决软删除+唯一约束冲突问题）
     */
    @Update("UPDATE blog_like SET is_deleted = 1 WHERE id = #{id}")
    int logicalDeleteById(@Param("id") Long id);

    /**
     * 恢复已逻辑删除的记录（绕过 @TableLogic，直接设置 is_deleted = 0）
     */
    @Update("UPDATE blog_like SET is_deleted = 0 WHERE id = #{id}")
    int logicalRestoreById(@Param("id") Long id);

    /**
     * 绕过 @TableLogic 查询点赞记录（用于处理 is_deleted IS NULL 的历史数据）
     */
    @Select("SELECT * FROM blog_like WHERE user_id = #{userId} AND post_id = #{postId} LIMIT 1")
    BlogLike selectRawByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 查询活跃点赞记录（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("SELECT * FROM blog_like WHERE user_id = #{userId} AND post_id = #{postId} AND (is_deleted = 0 OR is_deleted IS NULL) LIMIT 1")
    BlogLike selectActiveByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 批量查询用户已点赞的文章ID（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("<script>" +
            "SELECT DISTINCT post_id FROM blog_like " +
            "WHERE user_id = #{userId} AND post_id IN " +
            "<foreach collection='postIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}</foreach> " +
            "AND (is_deleted = 0 OR is_deleted IS NULL)" +
            "</script>")
    List<Long> selectActivePostIdsByUserAndPosts(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    /**
     * 分页查询用户点赞记录（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("<script>" +
            "SELECT * FROM blog_like " +
            "WHERE user_id = #{userId} AND (is_deleted = 0 OR is_deleted IS NULL) " +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<BlogLike> selectPageByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户点赞记录总数（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("SELECT COUNT(*) FROM blog_like WHERE user_id = #{userId} AND (is_deleted = 0 OR is_deleted IS NULL)")
    Long countByUserId(@Param("userId") Long userId);
}
