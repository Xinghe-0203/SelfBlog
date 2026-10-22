package com.selfblog.vo.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模型连通性测试结果。
 *
 * <p>这个接口永远返回 HTTP 200 + 业务码 200，失败放在 {@code ok=false} 里，
 * 否则前端拿到 4xx/5xx 会走全局错误提示，看不出「是配置问题还是服务问题」。
 * {@code message} 已脱敏：不含 API Key、不含上游原始响应体。</p>
 */
@Data
@Schema(description = "模型连通性测试结果")
public class SeoLlmTestVO {

    @Schema(description = "是否连通且返回了内容")
    private boolean ok;

    @Schema(description = "面向管理员的一句话说明")
    private String message;

    @Schema(description = "端到端耗时（毫秒）")
    private long latencyMs;

    @Schema(description = "测试时使用的模型名")
    private String model;
}
