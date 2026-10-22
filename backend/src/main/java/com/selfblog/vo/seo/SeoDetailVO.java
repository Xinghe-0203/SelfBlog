package com.selfblog.vo.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单篇 SEO 详情：列表行 + 编辑面板需要的上下文。
 *
 * <p>{@code effectiveTitle}/{@code effectiveDescription} 与前台阅读页使用
 * <b>同一个</b> {@link com.selfblog.service.seo.SeoFallback} 计算，因此后台预览
 * 不会与真实渲染漂移。注意它们是「站点名后缀拼接之前」的值 —— 60/160 字符
 * 限制针对的正是这一段。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SEO 详情")
public class SeoDetailVO extends SeoPostRowVO {

    @Schema(description = "文章摘要，作为生成素材展示；可能为 null")
    private String postSummary;

    @Schema(description = "前台实际会渲染的标题")
    private String effectiveTitle;

    @Schema(description = "前台实际会渲染的描述")
    private String effectiveDescription;
}
