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
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnonymousLikeServiceImpl implements AnonymousLikeService {
    private final AnonymousPostLikeMapper likeMapper;
    private final BlogPostMapper postMapper;

    /**
     * 访客伪名化密钥（S7）。此前是 {@code ${JWT_SECRET:local-visitor-hash}}：既复用 JWT
     * 签名密钥，又在缺失时静默退化成一个人人可见的常量。现在由
     * {@code app.visitor-hash-secret}（默认回落 JWT_SECRET）提供，缺失时
     * {@code VisitorHashUtils} 会抛错而不是用公开常量哈希。
     */
    @Value("${app.visitor-hash-secret:}")
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
    public AnonymousLikeVO toggle(Long postId, String visitorId) {
        BlogPost post = getPublishedPost(postId);
        String visitorHash = hashVisitor(visitorId);
        AnonymousPostLike existing = findRow(postId, visitorHash);
        boolean liked;
        if (existing == null) {
            AnonymousPostLike like = new AnonymousPostLike();
            like.setPostId(postId);
            like.setVisitorHash(visitorHash);
            like.setIsDeleted(0);
            try {
                likeMapper.insert(like);
                liked = true;
            } catch (DataAccessException e) {
                // 并发下两个请求可能同时看到“无行”并各自插入，后者撞上
                // (post_id, visitor_hash) 唯一键。对方已插入成功，语义上等价于“行已存在”，
                // 因此改走 toggle 更新路径，而不是把 500 抛给用户。
                //
                // 捕获父类 DataAccessException：H2/MySQL 是 DuplicateKeyException，
                // SQLite（默认运行库）的约束错误是 UncategorizedSQLException，
                // 只有父类能同时覆盖两种方言（与 BlogTagServiceImpl 的既有做法一致）。
                existing = findRow(postId, visitorHash);
                if (existing == null) {
                    // 冲突行随后被物理删除等极端情况：退回原异常，不掩盖真实故障。
                    throw e;
                }
                liked = applyToggle(existing);
            }
        } else {
            liked = applyToggle(existing);
        }
        return result(post, postId, liked);
    }

    /**
     * 按 (post_id, visitor_hash) 读取任意状态的记录，绕过逻辑删除过滤。
     *
     * <p>不能用 LambdaQueryWrapper：实体字段 isDeleted 命中全局 logic-delete-field，
     * MP 会自动追加 is_deleted = 0，导致被逻辑删除的历史行永远查不到。
     */
    private AnonymousPostLike findRow(Long postId, String visitorHash) {
        return likeMapper.selectRawByPostAndVisitor(postId, visitorHash);
    }

    /**
     * 对已存在的行执行切换：当前“已点赞”则取消，否则（含被逻辑删除的行）恢复为已点赞。
     *
     * <p>必须用 mapper 的原生 @Update 写 is_deleted：MyBatis-Plus 的 updateById 会把
     * logic-delete-field 从 SET 子句剔除并在 WHERE 追加 is_deleted = 0，等于静默不生效
     * （取消点赞会失效、恢复分支永远走不到）。
     *
     * @return 切换后的点赞状态
     */
    private boolean applyToggle(AnonymousPostLike existing) {
        boolean wasActive = existing.getIsDeleted() != null && existing.getIsDeleted() == 0;
        if (wasActive) {
            likeMapper.logicalDeleteById(existing.getId());
        } else {
            likeMapper.logicalRestoreById(existing.getId());
        }
        return !wasActive;
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
