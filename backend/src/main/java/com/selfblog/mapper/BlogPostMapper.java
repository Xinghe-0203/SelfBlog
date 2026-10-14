package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.BlogPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文章 Mapper 接口
 * 【数据库兼容】MySQL/SQLite - 自定义 SQL 已移至 BlogPostMapper.xml
 */
@Mapper
public interface BlogPostMapper extends BaseMapper<BlogPost> {

    Long countDistinctAuthorsSince(@Param("since") LocalDateTime since);

    void incrementViewCount(@Param("id") Long id);

    void incrementLikeCount(@Param("id") Long id);

    void decrementLikeCount(@Param("id") Long id);

    void incrementCommentCount(@Param("id") Long id);

    void decrementCommentCount(@Param("id") Long id, @Param("count") int count);

    void incrementCollectCount(@Param("id") Long id);

    void decrementCollectCount(@Param("id") Long id);

    void incrementShareCount(@Param("id") Long id);

    void decrementShareCount(@Param("id") Long id);

    List<Map<String, Object>> countPostsGroupByDate(@Param("since") LocalDateTime since);

    List<Long> fullTextSearch(@Param("keyword") String keyword,
                              @Param("status") Integer status,
                              @Param("isDeleted") Integer isDeleted,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);

    List<Long> fullTextSearchByView(@Param("keyword") String keyword,
                                    @Param("status") Integer status,
                                    @Param("isDeleted") Integer isDeleted,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    List<Long> fullTextSearchByLike(@Param("keyword") String keyword,
                                    @Param("status") Integer status,
                                    @Param("isDeleted") Integer isDeleted,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    Long countFullTextSearch(@Param("keyword") String keyword,
                             @Param("status") Integer status,
                             @Param("isDeleted") Integer isDeleted);
}
