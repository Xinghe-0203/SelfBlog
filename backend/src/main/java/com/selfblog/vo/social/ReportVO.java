package com.selfblog.vo.social;

import com.selfblog.vo.user.UserVO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 举报信息 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "举报信息")
public class ReportVO {

    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "举报人信息")
    private UserVO reporter;

    @Schema(description = "被举报用户信息")
    private UserVO reportedUser;

    @Schema(description = "举报目标类型：post=文章, comment=评论, user=用户")
    private String targetType;

    @Schema(description = "举报目标ID")
    private Long targetId;

    @Schema(description = "举报原因")
    private String reason;

    @Schema(description = "举报状态：0=待处理，1=已驳回，2=已核实")
    private Integer status;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理人信息")
    private UserVO handler;

    @Schema(description = "处理结果说明")
    private String handlerResult;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}