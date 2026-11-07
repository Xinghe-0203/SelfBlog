package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全站归档条目（归档页时间轴）的精简文章行。
 *
 * <p>只投影归档页需要的 6 个字段，绝不拉取 content 正文；已发布文章按创建时间倒序。</p>
 */
@Data
@Schema(description = "全站归档条目")
public class ArchivePostVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章分类")
    private String category;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "阅读量")
    private Long viewCount;
}
