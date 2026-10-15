package com.selfblog.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站点品牌资产上传结果。
 */
@Data
@Schema(description = "站点品牌资产")
public class SiteAssetVO {

    @Schema(description = "文件访问URL，形如 /uploads/site-assets/logo/xxx.png")
    private String url;

    @Schema(description = "资产种类：logo / favicon / avatar / music")
    private String kind;

    @Schema(description = "落盘后的字节数")
    private Long size;

    @Schema(description = "上传时的原始文件名")
    private String originalName;
}
