package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单条评论审核请求。
 *
 * <p>{@code kind} 缺省为 {@code guest}：目前只有访客留言会被自动审核，
 * 登录用户的历史社区评论保留 {@code site} 入口以便日后复用。</p>
 */
@Data
@Schema(description = "评论审核请求")
public class AiModerationCheckRequest {

    @Schema(description = "评论 ID", example = "5")
    private Long commentId;

    @Schema(description = "评论来源：guest（访客留言，默认）或 site（站内评论）", example = "guest")
    private String kind;
}
