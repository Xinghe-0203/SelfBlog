package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogDraftTag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 草稿-标签关联 Mapper 接口
 */
@Mapper
public interface BlogDraftTagMapper extends BaseMapper<BlogDraftTag> {

    /**
     * 批量插入草稿标签关联
     * @param draftId 草稿ID
     * @param tagIds 标签ID列表
     */
    @Insert("<script>" +
            "INSERT INTO blog_draft_tag (draft_id, tag_id) VALUES " +
            "<foreach collection='tagIds' item='tagId' separator=','>" +
            "(#{draftId}, #{tagId})" +
            "</foreach>" +
            "</script>")
    void batchInsertDraftTags(@Param("draftId") Long draftId, @Param("tagIds") List<Long> tagIds);
}
