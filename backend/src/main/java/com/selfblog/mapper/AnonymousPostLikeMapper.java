package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.AnonymousPostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AnonymousPostLikeMapper extends BaseMapper<AnonymousPostLike> {
    /**
     * 逻辑删除记录（绕过 MyBatis-Plus 的逻辑删除过滤，用于解决软删除+唯一约束冲突问题）
     */
    @Update("UPDATE anonymous_post_like SET is_deleted = 1 WHERE id = #{id}")
    int logicalDeleteById(@Param("id") Long id);

    /**
     * 恢复已逻辑删除的记录（绕过 MyBatis-Plus 的逻辑删除过滤，直接设置 is_deleted = 0）
     */
    @Update("UPDATE anonymous_post_like SET is_deleted = 0 WHERE id = #{id}")
    int logicalRestoreById(@Param("id") Long id);

    /**
     * 绕过 MyBatis-Plus 的逻辑删除过滤查询记录（兼容 is_deleted = 0 / 1 / NULL 的历史数据）。
     *
     * <p>必须用原生 SQL：实体字段 isDeleted 命中全局 logic-delete-field，updateById 会把
     * is_deleted 从 SET 子句里剔除、并在 WHERE 上追加 is_deleted = 0，因此切换点赞状态
     * （写 is_deleted）只能走本方法配套的两个 @Update。
     */
    @Select("SELECT * FROM anonymous_post_like WHERE post_id = #{postId} AND visitor_hash = #{visitorHash} LIMIT 1")
    AnonymousPostLike selectRawByPostAndVisitor(@Param("postId") Long postId, @Param("visitorHash") String visitorHash);
}
