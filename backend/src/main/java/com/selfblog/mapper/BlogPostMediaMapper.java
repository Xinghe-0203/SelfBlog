package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogPostMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章媒体关联Mapper
 */
@Mapper
public interface BlogPostMediaMapper extends BaseMapper<BlogPostMedia> {

    /**
     * 批量插入文章媒体关联
     * @param postId 文章ID
     * @param mediaIds 媒体ID列表
     */
    void batchInsert(@Param("postId") Long postId, @Param("mediaIds") List<Long> mediaIds);

    /**
     * 根据文章ID查询所有媒体关联
     * @param postId 文章ID
     * @return 媒体关联列表
     */
    List<BlogPostMedia> selectByPostId(@Param("postId") Long postId);

    /**
     * 根据文章ID删除所有媒体关联
     * @param postId 文章ID
     */
    void deleteByPostId(@Param("postId") Long postId);
}
