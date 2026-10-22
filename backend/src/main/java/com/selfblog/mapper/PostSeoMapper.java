package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.PostSeo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章 SEO 元数据 Mapper。
 *
 * <p>只用 MyBatis-Plus 的通用方法（{@code selectById}/{@code selectList}/{@code insert}/
 * {@code updateById}/{@code deleteById}），<b>不写 XML</b>：审核统计需要的计数、标题长度、
 * 重复标题判断全部在 Java 侧基于一次性加载的元数据计算，避免 {@code CHAR_LENGTH}（MySQL 用
 * 字符、{@code LENGTH} 用字节）这类方言差异，也避免 H2 没有 {@code databaseId} 映射时
 * 共享语句不可移植的问题。</p>
 */
@Mapper
public interface PostSeoMapper extends BaseMapper<PostSeo> {
}
