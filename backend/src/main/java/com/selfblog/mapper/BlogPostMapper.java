package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文章 Mapper 接口
 * 【数据库兼容】MySQL/SQLite - 自定义 SQL 已移至 BlogPostMapper.xml
 */
@Mapper
public interface BlogPostMapper extends BaseMapper<BlogPost> {

    Long countDistinctAuthorsSince(@Param("since") LocalDateTime since);

    void incrementViewCount(@Param("id") Long id);

    void incrementLikeCount(@Param("id") Long id);

    void decrementLikeCount(@Param("id") Long id);

    void incrementCommentCount(@Param("id") Long id);

    /**
     * 相对减法（C8 之前的形状）。带 {@code comment_count >= #{count}} 守卫，计数一旦漂低就静默
     * 不生效、越漂越高，无法自愈；删除评论的路径已改用 {@link #syncCommentCount}。
     *
     * @param id 文章ID
     * @param count 减少数量
     * @deprecated 使用 {@link #syncCommentCount(Long, int)} 写入绝对值
     */
    @Deprecated
    void decrementCommentCount(@Param("id") Long id, @Param("count") int count);

    /**
     * 把 {@code blog_post.comment_count} 直接写成绝对值（C8）。
     *
     * <p>相对减法只能"记账"，一旦历史数据已经漂移（重复删除、并发删除、早期缺守卫的写入），
     * 减多少都无法回到真实值。这里用真实行数覆盖，计数可以自我纠正。</p>
     *
     * @param id 文章ID
     * @param count 当前真实评论数（来自 {@code SELECT COUNT(*)}）
     */
    @Update("UPDATE blog_post SET comment_count = #{count} WHERE id = #{id} AND is_deleted = 0")
    void syncCommentCount(@Param("id") Long id, @Param("count") int count);

    void incrementCollectCount(@Param("id") Long id);

    void decrementCollectCount(@Param("id") Long id);

    void incrementShareCount(@Param("id") Long id);

    List<Map<String, Object>> countPostsGroupByDate(@Param("since") LocalDateTime since);
}
