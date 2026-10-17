package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.dto.site.TagLastModified;
import com.selfblog.entity.BlogPostTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 帖子-标签关联 Mapper 接口
 * 【数据库兼容】MySQL/SQLite - 自定义 SQL 已移至 BlogPostTagMapper.xml
 */
@Mapper
public interface BlogPostTagMapper extends BaseMapper<BlogPostTag> {

    /**
     * 批量插入文章标签关联
     */
    void batchInsertPostTags(@Param("postId") Long postId, @Param("tagIds") List<Long> tagIds);

    /**
     * 站点地图用：每个标签下最新一篇已发布文章的更新时间（无已发布文章时不返回该行）。
     * 返回值为数据库原生时间类型（H2/MySQL 为 LocalDateTime，SQLite 为字符串），
     * 由调用方统一归一为 yyyy-MM-dd；SQL 为可移植语句，没有方言差异。
     */
    List<TagLastModified> selectTagLastModified(@Param("tagIds") List<Long> tagIds);
}
