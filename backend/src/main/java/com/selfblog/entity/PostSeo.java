package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章 SEO 元数据 — 与 {@code blog_post} 一对一，主键即文章 ID。
 *
 * <p><b>为什么是独立表而不是给 blog_post 加列：</b>本项目没有 ALTER/列迁移执行器
 * （{@code src/main/resources/*.sql} 里零 {@code ALTER TABLE}），SQLite 只跑
 * {@code CREATE TABLE IF NOT EXISTS}。给已有表加列对现存数据库不会生效，而新表
 * 在三种方言下都能用 {@code CREATE TABLE IF NOT EXISTS} 安全补上。</p>
 *
 * <p>这里<b>刻意不加外键</b>：本仓库所有表都没有外键约束，文章软删除（{@code is_deleted}）
 * 也不触发级联；孤儿 SEO 行由读取端忽略，不影响任何查询。</p>
 *
 * <p>{@code source} 记录最后写入者：{@code manual}（后台手填）或 {@code llm}（模型生成）。
 * 自动生成在发布时不会覆盖手填内容。</p>
 */
@Data
@TableName("post_seo")
public class PostSeo {

    /** 主键 = blog_post.id，由调用方指定，不自增。 */
    @TableId(type = IdType.INPUT)
    private Long postId;

    /** 自定义 SEO 标题（≤200）；空表示沿用文章标题。 */
    private String seoTitle;

    /** 自定义 SEO 描述（≤400）；空表示沿用摘要/正文派生值。 */
    private String seoDescription;

    /** 逗号分隔的关键词，如 {@code Java,Spring Boot,后端}。 */
    private String seoKeywords;

    /** 社交分享封面图地址；空表示回落到文章封面 / 站点 Logo。 */
    private String ogImageUrl;

    /** {@code manual} 或 {@code llm}。 */
    private String source;

    /** 最近一次模型生成时间；手填保存不清除它，新建手填行为 null。 */
    private LocalDateTime generatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
