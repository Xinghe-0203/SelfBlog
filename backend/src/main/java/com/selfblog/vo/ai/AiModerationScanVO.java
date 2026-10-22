package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量扫描结果。
 *
 * <p>{@code scanned} 是实际送审的条数，{@code flagged} 是结论不是 {@code pass} 的条数；
 * {@code items} 只列出被判有问题的那些，正常留言不占带宽。</p>
 */
@Data
@Schema(description = "评论批量审核结果")
public class AiModerationScanVO {

    @Schema(description = "实际扫描的评论条数")
    private Integer scanned;

    @Schema(description = "其中结论不是 pass 的条数")
    private Integer flagged;

    @Schema(description = "被判有问题的评论明细")
    private List<Item> items;

    @Data
    @Schema(description = "单条被判有问题的评论")
    public static class Item {

        @Schema(description = "评论 ID")
        private Long commentId;

        @Schema(description = "评论来源：guest 或 site")
        private String kind;

        @Schema(description = "留言昵称")
        private String nickname;

        @Schema(description = "留言内容节选")
        private String excerpt;

        @Schema(description = "结论：flag 或 block")
        private String verdict;

        @Schema(description = "判定理由")
        private String reason;

        @Schema(description = "命中的问题分类")
        private List<String> categories;
    }
}
