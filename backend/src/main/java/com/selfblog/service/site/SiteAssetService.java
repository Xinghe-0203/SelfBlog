package com.selfblog.service.site;

import com.selfblog.vo.site.SiteAssetVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 站点品牌资产上传。
 */
public interface SiteAssetService {

    /**
     * 校验并原样保存一个品牌资产。
     *
     * @param file 上传文件
     * @param kind 资产种类，取值见 {@link com.selfblog.common.enums.SiteAssetKind}
     * @return 可公开访问的 URL 与元信息
     */
    SiteAssetVO store(MultipartFile file, String kind);
}
