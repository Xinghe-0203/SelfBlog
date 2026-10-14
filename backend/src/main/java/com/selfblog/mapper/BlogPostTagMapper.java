package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
