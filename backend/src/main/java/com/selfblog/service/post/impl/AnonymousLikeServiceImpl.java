package com.selfblog.service.post.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.AnonymousPostLike;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.AnonymousPostLikeMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.post.AnonymousLikeService;
import com.selfblog.utils.VisitorHashUtils;
import com.selfblog.vo.post.AnonymousLikeVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnonymousLikeServiceImpl implements AnonymousLikeService {
    private final AnonymousPostLikeMapper likeMapper;
    private final BlogPostMapper postMapper;

    @Value("${JWT_SECRET:local-visitor-hash}")
    private String hashSecret;

    public AnonymousLikeServiceImpl(AnonymousPostLikeMapper likeMapper, BlogPostMapper postMapper) {
        this.likeMapper = likeMapper;
        this.postMapper = postMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AnonymousLikeVO getStatus(Long postId, String visitorId) {
        BlogPost post = getPublishedPost(postId);
        String visitorHash = hashVisitor(visitorId);
        LambdaQueryWrapper<AnonymousPostLike> active = new LambdaQueryWrapper<>();
        active.eq(AnonymousPostLike::getPostId, postId)
                .eq(AnonymousPostLike::getVisitorHash, visitorHash)
                .eq(AnonymousPostLike::getIsDeleted, 0);
        boolean liked = likeMapper.selectCount(active) > 0;
        return result(post, postId, liked);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized AnonymousLikeVO toggle(Long postId, String visitorId) {
        BlogPost post = getPublishedPost(postId);
        String visitorHash = hashVisitor(visitorId);
        LambdaQueryWrapper<AnonymousPostLike> query = new LambdaQueryWrapper<>();
        query.eq(AnonymousPostLike::getPostId, postId).eq(AnonymousPostLike::getVisitorHash, visitorHash);
        AnonymousPostLike existing = likeMapper.selectOne(query);
        boolean liked;
        if (existing == null) {
            AnonymousPostLike like = new AnonymousPostLike();
            like.setPostId(postId);
            like.setVisitorHash(visitorHash);
            like.setIsDeleted(0);
            likeMapper.insert(like);
            liked = true;
        } else if (existing.getIsDeleted() != null && existing.getIsDeleted() == 0) {
            existing.setIsDeleted(1);
            likeMapper.updateById(existing);
            liked = false;
        } else {
            existing.setIsDeleted(0);
            likeMapper.updateById(existing);
            liked = true;
        }
        return result(post, postId, liked);
    }

    private AnonymousLikeVO result(BlogPost post, Long postId, boolean liked) {
        LambdaQueryWrapper<AnonymousPostLike> countQuery = new LambdaQueryWrapper<>();
        countQuery.eq(AnonymousPostLike::getPostId, postId).eq(AnonymousPostLike::getIsDeleted, 0);
        AnonymousLikeVO result = new AnonymousLikeVO();
        result.setLiked(liked);
        result.setLikeCount((post.getLikeCount() == null ? 0 : post.getLikeCount()) + likeMapper.selectCount(countQuery));
        return result;
    }

    private BlogPost getPublishedPost(Long postId) {
        BlogPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1
                || (post.getIsDeleted() != null && post.getIsDeleted() == 1)) {
            throw new BusinessException(404, "文章不存在");
        }
        return post;
    }

    private String hashVisitor(String visitorId) {
        if (visitorId == null || !visitorId.trim().matches("[A-Za-z0-9_-]{8,80}")) {
            throw new BusinessException(400, "访客标识无效，请刷新页面后重试");
        }
        return VisitorHashUtils.hash(visitorId.trim(), hashSecret);
    }
}
