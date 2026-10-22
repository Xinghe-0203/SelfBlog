package com.selfblog.vo.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一个可选的模型候选。
 *
 * <p>{@code supportsImage} 决定「AI 图片描述」（E）能不能用这个模型：DeepSeek 只有
 * {@code deepseek-flash} 支持图像输入，{@code deepseek-v4-pro} 会以 400/422 拒绝。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型候选")
public class SeoLlmModelVO {

    @Schema(description = "模型 ID，保存进设置的就是它")
    private String id;

    @Schema(description = "展示名；上游没给名字时等于 id")
    private String name;

    @Schema(description = "是否支持图像输入")
    private boolean supportsImage;
}
