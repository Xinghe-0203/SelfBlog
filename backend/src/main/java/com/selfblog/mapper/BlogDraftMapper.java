package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogDraft;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章草稿 Mapper 接口
 */
@Mapper
public interface BlogDraftMapper extends BaseMapper<BlogDraft> {
}
