package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogTag;
import com.selfblog.vo.post.TagWithCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    /**
     * 公开标签索引：每个标签 + 它的**已发布**文章数。
     *
     * <p>只数 status=1 且 is_deleted=0 的文章：草稿与已删除文章不公开，
     * 计数里也不该出现它们（否则公开页面会泄露「有几篇草稿」）。
     * 关联表 blog_post_tag 同样要过滤 is_deleted —— 逻辑删除的关联行不算。</p>
     *
     * <p>SQL 刻意只用 LEFT JOIN + GROUP BY + COUNT，不使用任何数据库专有函数：
     * H2 / MySQL / SQLite 三套 schema 共用这一条语句。</p>
     */
    @Select("SELECT t.id AS id, t.name AS name, COUNT(p.id) AS postCount "
            + "FROM blog_tag t "
            + "LEFT JOIN blog_post_tag pt ON pt.tag_id = t.id AND pt.is_deleted = 0 "
            + "LEFT JOIN blog_post p ON p.id = pt.post_id AND p.is_deleted = 0 AND p.status = 1 "
            + "WHERE t.is_deleted = 0 "
            + "GROUP BY t.id, t.name "
            + "ORDER BY postCount DESC, t.name ASC")
    List<TagWithCountVO> selectTagsWithPostCounts();
}
