package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 关注关系 Mapper 接口
 */
@Mapper
public interface BlogFollowMapper extends BaseMapper<BlogFollow> {
    /**
     * 逻辑删除记录（用于解决软删除+唯一约束冲突问题）
     */
    @Update("UPDATE blog_follow SET is_deleted = 1 WHERE id = #{id}")
    int logicalDeleteById(@Param("id") Long id);

    @Update("UPDATE blog_follow SET is_deleted = 0 WHERE id = #{id}")
    int logicalRestoreById(@Param("id") Long id);

    /**
     * 绕过 @TableLogic 查询任意状态的关注记录（兼容 is_deleted = 0、1 和 NULL）
     */
    @Select("SELECT * FROM blog_follow WHERE follower_id = #{followerId} AND following_id = #{followingId} LIMIT 1")
    BlogFollow selectRawByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}