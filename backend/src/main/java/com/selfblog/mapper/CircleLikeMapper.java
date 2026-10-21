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

    /**
     * C19：原子地取消某用户对该动态的「活跃」点赞，返回受影响行数。
     *
     * <p>这是 toggle 的仲裁原语：先尝试取消，受影响行数为 1 才说明本次请求真的翻转了状态；
     * 为 0 说明当前没有活跃点赞，再走「恢复/插入」分支。这样输掉并发竞争的一方永远不会
     * 去改赢家刚提交的那一行（旧实现在 insert 冲突后调用 logicalDeleteById + decrement，
     * 等于把赢家的点赞悄悄取消了）。</p>
     */
    @Update("UPDATE blog_circle_like SET is_deleted = 1 WHERE user_id = #{userId} AND post_id = #{postId} AND is_deleted = 0")
    int logicalDeleteActiveByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * C19：原子地把已逻辑删除的点赞行恢复为活跃，返回受影响行数。
     *
     * <p>表上有 {@code UNIQUE(user_id, post_id)}，取消后再点赞只能复用同一行，
     * 因此「恢复」是点赞分支的第一选择，插入只用于该用户从未点过赞的情况。</p>
     */
    @Update("UPDATE blog_circle_like SET is_deleted = 0 WHERE user_id = #{userId} AND post_id = #{postId} AND is_deleted = 1")
    int logicalRestoreByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);
}
