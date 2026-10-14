package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BlogTagMapper extends BaseMapper<BlogTag> {

    @Select("SELECT COUNT(*) FROM blog_tag WHERE is_deleted = 0")
    Long countTags();
}
