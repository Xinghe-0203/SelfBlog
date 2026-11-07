package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相关文章推荐（文章详情页「相关阅读」板块）的精简信息。
 *
 * <p>只携带列表展示需要的元数据，绝不带正文；summary 截断到 100 字符，
 * coverUrl 按库中原样返回（可能为空串）。</p>
 */
@Data
@Schema(description = "相关文章推荐")
public class RelatedPostVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "摘要（≤100 字符，空白为空串）")
    private String summary;

    @Schema(description = "封面图URL（按存储原样返回）")
    private String coverUrl;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;
}
