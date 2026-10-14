package com.selfblog.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签创建请求
 */
@Data
@Schema(description = "标签创建请求")
public class TagCreateRequest {

    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 20, message = "标签名称长度必须在1-20字符之间")
    private String name;
}
