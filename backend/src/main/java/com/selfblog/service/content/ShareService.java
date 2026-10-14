package com.selfblog.service.content;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.BlogShare;

/**
 * 分享服务接口
 */
public interface ShareService extends IService<BlogShare> {

    /**
     * 记录分享
     * @param postId 文章ID
     * @param userId 用户ID（可为空，表示未登录用户分享）
     * @param platform 分享平台
     */
    void recordShare(Long postId, Long userId, String platform);

    /**
     * 获取文章的分享数
     * @param postId 文章ID
     * @return 分享数
     */
    int getShareCount(Long postId);
}