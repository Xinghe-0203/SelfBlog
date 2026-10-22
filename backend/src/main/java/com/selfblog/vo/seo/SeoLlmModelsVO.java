package com.selfblog.vo.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型候选清单。
 *
 * <p>{@code source} 说明清单从哪来：{@code upstream} = 真的问了上游 {@code GET /models}；
 * {@code fallback} = 上游不支持或不可达，用了内置候选。前端据此提示「列表来自内置候选」，
 * 但两种情况下拉框都能用 —— 所以接口<b>恒返回 200</b>，不把探测失败当错误。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型候选清单")
public class SeoLlmModelsVO {

    @Schema(description = "候选列表，至少两项")
    private List<SeoLlmModelVO> models;

    @Schema(description = "清单来源：upstream 或 fallback")
    private String source;

    @Schema(description = "回落到内置候选时的原因；source=upstream 时为空")
    private String reason;
}
