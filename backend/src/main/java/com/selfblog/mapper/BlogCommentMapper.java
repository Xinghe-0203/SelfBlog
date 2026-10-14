package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {

    @Select("SELECT COUNT(DISTINCT user_id) FROM blog_comment WHERE create_time >= #{since} AND is_deleted = 0")
    Long countDistinctAuthorsSince(@Param("since") LocalDateTime since);
}
