package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章分享 Mapper
 */
@Mapper
public interface BlogShareMapper extends BaseMapper<BlogShare> {

    /**
     * 增加文章分享数
     */
    @Update("UPDATE blog_post SET share_count = CASE WHEN share_count < 100000000 THEN share_count + 1 ELSE share_count END WHERE id = #{postId} AND is_deleted = 0")
    void incrementShareCount(@Param("postId") Long postId);
}