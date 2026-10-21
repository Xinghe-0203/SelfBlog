package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    /**
     * C21：分页查询某用户的粉丝关系，且只保留「未被封禁」的粉丝。
     *
     * <p>旧实现先用 MyBatis-Plus 分页拿到原始关注行、再在 Java 里丢掉被封禁用户，
     * 于是 total 仍按原始行数统计、records 却被过滤，出现「本页条数 &lt; total」和深页空数组。
     * 这里把封禁谓词（{@code status IS NULL OR status != 0}）与逻辑删除谓词一起下推到 SQL，
     * 让 records 与 total 使用同一个谓词。手写 SQL 必须自己带 {@code is_deleted} 条件。</p>
     */
    @Select("<script>" +
            "SELECT f.* FROM blog_follow f JOIN sys_user u ON u.id = f.follower_id " +
            "WHERE f.following_id = #{userId} AND f.is_deleted = 0 " +
            "AND u.is_deleted = 0 AND (u.status IS NULL OR u.status != 0) " +
            "ORDER BY f.create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<BlogFollow> selectActiveFollowerPage(@Param("userId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * C21：与 {@link #selectActiveFollowerPage} 完全相同的谓词，用于取真实总数。
     */
    @Select("SELECT COUNT(*) FROM blog_follow f JOIN sys_user u ON u.id = f.follower_id " +
            "WHERE f.following_id = #{userId} AND f.is_deleted = 0 " +
            "AND u.is_deleted = 0 AND (u.status IS NULL OR u.status != 0)")
    Long countActiveFollowers(@Param("userId") Long userId);

    /**
     * C21：分页查询某用户关注的人，且只保留「未被封禁」的目标用户；谓词说明见
     * {@link #selectActiveFollowerPage}。
     */
    @Select("<script>" +
            "SELECT f.* FROM blog_follow f JOIN sys_user u ON u.id = f.following_id " +
            "WHERE f.follower_id = #{userId} AND f.is_deleted = 0 " +
            "AND u.is_deleted = 0 AND (u.status IS NULL OR u.status != 0) " +
            "ORDER BY f.create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<BlogFollow> selectActiveFollowingPage(@Param("userId") Long userId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    /**
     * C21：与 {@link #selectActiveFollowingPage} 完全相同的谓词，用于取真实总数。
     */
    @Select("SELECT COUNT(*) FROM blog_follow f JOIN sys_user u ON u.id = f.following_id " +
            "WHERE f.follower_id = #{userId} AND f.is_deleted = 0 " +
            "AND u.is_deleted = 0 AND (u.status IS NULL OR u.status != 0)")
    Long countActiveFollowing(@Param("userId") Long userId);
}