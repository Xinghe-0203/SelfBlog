package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {

    @Select("SELECT COUNT(DISTINCT user_id) FROM blog_comment WHERE create_time >= #{since} AND is_deleted = 0")
    Long countDistinctAuthorsSince(@Param("since") LocalDateTime since);

    /**
     * 某篇文章当前有效的评论总数（C8 的绝对值来源）。
     *
     * <p>{@code blog_post.comment_count} 是用相对加减维护的派生计数，一旦漂移就再也回不来：
     * 减法带 {@code comment_count >= #{count}} 守卫，计数偏低时静默不生效，只会越漂越高。
     * 删除评论后用这里的真实行数回写，计数才可能自我纠正。</p>
     *
     * @param postId 文章ID
     * @return 有效评论行数（{@code is_deleted = 0}）
     */
    @Select("SELECT COUNT(*) FROM blog_comment WHERE post_id = #{postId} AND is_deleted = 0")
    Long countActiveByPostId(@Param("postId") Long postId);
}
