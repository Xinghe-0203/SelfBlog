package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图片描述结果。
 *
 * <p>只返回建议值，<b>不写库</b>：管理员确认后由前端自己调原有的保存接口。</p>
 */
@Data
@Schema(description = "AI 生成的图片描述")
public class AiImageAltVO {

    @Schema(description = "无障碍替代文本，最多 120 字")
    private String alt;

    @Schema(description = "图片说明/图注，最多 80 字，可为空")
    private String caption;

    @Schema(description = "实际使用的模型名")
    private String model;

    @Schema(description = "模型调用耗时（毫秒）")
    private Long latencyMs;
}
