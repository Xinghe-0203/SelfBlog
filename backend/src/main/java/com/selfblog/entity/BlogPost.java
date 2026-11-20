package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 文章/帖子实体类 (blog_post)
 */
@Data
@TableName("blog_post")
@Schema(description = "文章信息")
public class BlogPost implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "作者用户ID")
    private Long userId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章内容")
    private String content;

    /**
     * 话题 ID 数组，落库为 JSON 数组文本（如 {@code [884002]}）。
     *
     * <p><b>不要</b>给这个 String 字段挂 {@code JacksonTypeHandler}：写入时会把「已是 JSON
     * 的字符串」再编码一次，库里变成 {@code "[884002]"}（带引号的 JSON 字符串而非数组），
     * 导致 {@code JSON_CONTAINS} / {@code json_each} / {@code TopicMapper.recalculate*}
     * 全部失配——这正是 schema-fresh-install.sql「v2.0.16 topic_ids 格式修复」迁移注释
     * 所指的双重编码问题。String 字段不挂 typeHandler 即可按原文落库与读出（也无需
     * autoResultMap）；JSON 的生成由写入侧 hutool 完成，解析统一走
     * {@link #parseTopicIds(String)}（同时容忍上述遗留双重编码形态）。</p>
     */
    @Schema(description = "话题ID数组")
    private String topicIds;

    @Schema(description = "文章分类")
    private String category;

    @Schema(description = "封面图URL")
    @TableField("cover_url")
    private String coverUrl;

    @Schema(description = "阅读量")
    private Long viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer collectCount;

    @Schema(description = "分享数")
    private Integer shareCount;

    @Schema(description = "文章状态：0=待审核，1=已发布，2=已驳回")
    private Integer status;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除：0=正常，1=删除")
    @TableLogic
    private Integer isDeleted;

    /**
     * 兼容方法：返回第一个话题ID
     */
    public Long getTopicId() {
        List<Long> ids = parseTopicIds(topicIds);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /**
     * 解析 topic_ids 列文本为话题 ID 列表。
     *
     * <p>兼容两种历史存储形态：规范 JSON 数组文本（{@code [884002]}，现行写入形态）
     * 与遗留双重编码形态（{@code "[884002]"}——旧版实体挂 {@code JacksonTypeHandler} 后
     * createPost/updatePost 落库的带引号 JSON 字符串，见 schema-fresh-install.sql
     * 「v2.0.16 topic_ids 格式修复」迁移注释）。解析失败一律返回空列表，绝不抛异常：
     * 调用方是话题计数（编辑/删除/举报核实后的 decrement）与列表/详情展示路径。</p>
     */
    public static List<Long> parseTopicIds(String topicIdsJson) {
        if (topicIdsJson == null || topicIdsJson.isEmpty()) {
            return Collections.emptyList();
        }
        String text = topicIdsJson.trim();
        if (text.length() >= 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
            // 遗留双重编码形态：最外层是一对 JSON 引号，解掉一层得到规范数组文本
            text = text.substring(1, text.length() - 1);
        }
        try {
            List<Long> ids = cn.hutool.json.JSONUtil.toList(text, Long.class);
            return ids == null ? Collections.emptyList() : ids;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
