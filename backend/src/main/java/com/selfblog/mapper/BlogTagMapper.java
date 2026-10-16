package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BlogTagMapper extends BaseMapper<BlogTag> {

    @Select("SELECT COUNT(*) FROM blog_tag WHERE is_deleted = 0")
    Long countTags();

    /**
     * 绕过 @TableLogic 按名称查任意状态的标签行。
     *
     * <p>blog_tag.name 有唯一约束，而删除是逻辑删除：删除后重建同名标签会命中唯一键，
     * 此时必须能找到那行已删除的记录并恢复它，否则只能拿到 null（创建接口 500、
     * 发文章时标签丢失）。这里是刻意不过滤 is_deleted 的原始查询。
     */
    @Select("SELECT * FROM blog_tag WHERE name = #{name} LIMIT 1")
    BlogTag selectRawByName(@Param("name") String name);

    /** 恢复被逻辑删除的标签行（is_deleted=1 → 0）。 */
    @Update("UPDATE blog_tag SET is_deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
