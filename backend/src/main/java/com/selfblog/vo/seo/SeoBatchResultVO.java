package com.selfblog.vo.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量生成结果。
 *
 * <p>口径：{@code requested} = 真正送进模型的篇数（不超过 limit）；
 * {@code skipped} = 在任何模型调用之前就被跳过的篇数（文章不存在、未发布、
 * 或 {@code onlyMissing} 下已有 SEO）；{@code succeeded + failed == requested}。</p>
 */
@Data
@Schema(description = "批量生成结果")
public class SeoBatchResultVO {

    @Schema(description = "实际请求模型的篇数")
    private int requested;

    @Schema(description = "成功篇数")
    private int succeeded;

    @Schema(description = "失败篇数")
    private int failed;

    @Schema(description = "被跳过的篇数（未调用模型）")
    private int skipped;

    @Schema(description = "失败明细")
    private List<Failure> failures = new ArrayList<>();

    @Data
    @Schema(description = "单篇失败原因")
    public static class Failure {

        private Long postId;

        @Schema(description = "面向管理员的原因，不含密钥与上游原文")
        private String message;

        public Failure() {
        }

        public Failure(Long postId, String message) {
            this.postId = postId;
            this.message = message;
        }
    }
}
