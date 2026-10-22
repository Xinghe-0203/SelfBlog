package com.selfblog.dto.site;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单张相册照片的图注（alt / caption）更新表单。
 *
 * <p>{@code album_photo.caption} 列从建表起就存在，但此前<b>没有任何写入路径</b>：
 * 上传只写 file_path/mime_type/sort_order，于是 AI 图片描述（E 块）生成的文案
 * 只能停在浏览器草稿里，无法落库。这个表单补上那条写入路径。</p>
 *
 * <p>长度上限取 500，与 MySQL 建表语句 {@code caption VARCHAR(500)} 一致；
 * 空串在服务层归一为 {@code null}（= 清除图注），不是存空串。</p>
 */
@Data
public class PhotoCaptionForm {

    @Size(max = 500, message = "图注不能超过500个字符")
    private String caption;
}
