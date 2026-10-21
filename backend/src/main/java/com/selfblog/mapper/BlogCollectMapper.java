package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BlogCollectMapper extends BaseMapper<BlogCollect> {
    /**
     * 逻辑删除记录（用于解决软删除+唯一约束冲突问题）
     */
    @Update("UPDATE blog_collect SET is_deleted = 1 WHERE id = #{id}")
    int logicalDeleteById(@Param("id") Long id);

    /**
     * 恢复已逻辑删除的记录（绕过 @TableLogic，直接设置 is_deleted = 0）
     */
    @Update("UPDATE blog_collect SET is_deleted = 0 WHERE id = #{id}")
    int logicalRestoreById(@Param("id") Long id);

    /**
     * 查询活跃收藏记录（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("SELECT * FROM blog_collect WHERE user_id = #{userId} AND post_id = #{postId} AND (is_deleted = 0 OR is_deleted IS NULL) LIMIT 1")
    BlogCollect selectActiveByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 绕过 @TableLogic 查询收藏记录（用于处理 is_deleted IS NULL 的历史数据）
     */
    @Select("SELECT * FROM blog_collect WHERE user_id = #{userId} AND post_id = #{postId} LIMIT 1")
    BlogCollect selectRawByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 批量查询用户已收藏的文章ID（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("<script>" +
            "SELECT DISTINCT post_id FROM blog_collect " +
            "WHERE user_id = #{userId} AND post_id IN " +
            "<foreach collection='postIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}</foreach> " +
            "AND (is_deleted = 0 OR is_deleted IS NULL)" +
            "</script>")
    List<Long> selectActivePostIdsByUserAndPosts(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    /**
     * 分页查询用户收藏记录（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("<script>" +
            "SELECT * FROM blog_collect " +
            "WHERE user_id = #{userId} AND (is_deleted = 0 OR is_deleted IS NULL) " +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<BlogCollect> selectPageByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户收藏记录总数（绕过 @TableLogic，兼容 is_deleted = 0 和 NULL）
     */
    @Select("SELECT COUNT(*) FROM blog_collect WHERE user_id = #{userId} AND (is_deleted = 0 OR is_deleted IS NULL)")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * C21：分页查询用户收藏记录，且只保留「已发布且未删除」的文章。
     *
     * <p>把文章可见性谓词下推到 SQL，records 与 total 使用同一个谓词，
     * 避免「本页条数 &lt; total」或深页返回空。手写 SQL 必须自己带 {@code is_deleted}。</p>
     */
    @Select("<script>" +
            "SELECT c.* FROM blog_collect c JOIN blog_post p ON p.id = c.post_id " +
            "WHERE c.user_id = #{userId} AND (c.is_deleted = 0 OR c.is_deleted IS NULL) " +
            "AND p.status = 1 AND (p.is_deleted = 0 OR p.is_deleted IS NULL) " +
            "ORDER BY c.create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<BlogCollect> selectPublishedPageByUserId(@Param("userId") Long userId,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    /**
     * C21：与 {@link #selectPublishedPageByUserId} 完全相同的谓词，用于取真实总数。
     */
    @Select("SELECT COUNT(*) FROM blog_collect c JOIN blog_post p ON p.id = c.post_id " +
            "WHERE c.user_id = #{userId} AND (c.is_deleted = 0 OR c.is_deleted IS NULL) " +
            "AND p.status = 1 AND (p.is_deleted = 0 OR p.is_deleted IS NULL)")
    Long countPublishedByUserId(@Param("userId") Long userId);
}
