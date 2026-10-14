package com.selfblog.service.content.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogShare;
import com.selfblog.mapper.BlogShareMapper;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.service.content.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分享服务实现类
 */
@Service
public class ShareServiceImpl extends ServiceImpl<BlogShareMapper, BlogShare> implements ShareService {

    @Autowired
    private BlogPostService blogPostService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordShare(Long postId, Long userId, String platform) {
        if (postId == null) {
            throw new BusinessException(400, "文章ID不能为空");
        }

        if (blogPostService.getById(postId) == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 记录分享
        BlogShare share = new BlogShare();
        share.setPostId(postId);
        share.setUserId(userId);
        share.setPlatform(platform != null ? platform : "web");
        this.save(share);

        // 增加文章分享数
        baseMapper.incrementShareCount(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getShareCount(Long postId) {
        if (postId == null) {
            return 0;
        }
        Long count = this.lambdaQuery()
                .eq(BlogShare::getPostId, postId)
                .eq(BlogShare::getIsDeleted, 0)
                .count();
        return count != null ? count.intValue() : 0;
    }
}