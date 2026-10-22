package com.selfblog.dto.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量生成请求。
 *
 * <p>两种用法：显式传 {@code postIds}（后台勾选），或只传 {@code limit + onlyMissing}
 * （「补全所有缺失的」）。两者都为空时按 {@code limit} 取最近的未生成文章。</p>
 */
@Data
@Schema(description = "批量生成 SEO 元数据")
public class SeoBatchRequest {

    @Schema(description = "指定文章 ID；为空时按 limit 自动挑选")
    private List<Long> postIds;

    @Schema(description = "最多处理多少篇，默认 10，上限 50")
    private Integer limit;

    @Schema(description = "只处理还没有 SEO 元数据的文章")
    private Boolean onlyMissing;
}
