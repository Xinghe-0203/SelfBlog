package com.selfblog.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户注册响应")
public class UserRegisterResponse {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;
}
