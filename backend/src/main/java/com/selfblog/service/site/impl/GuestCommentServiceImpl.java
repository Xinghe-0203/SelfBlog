package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.GuestCommentRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.GuestComment;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.GuestCommentMapper;
import com.selfblog.service.site.GuestCommentService;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.utils.VisitorHashUtils;
import com.selfblog.vo.site.GuestCommentVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GuestCommentServiceImpl implements GuestCommentService {
    private final GuestCommentMapper commentMapper;
    private final BlogPostMapper postMapper;
    private final SiteSettingsService settingsService;
    private final HtmlSanitizer htmlSanitizer;

    @Value("${JWT_SECRET:local-visitor-hash}")
    private String hashSecret;

    public GuestCommentServiceImpl(GuestCommentMapper commentMapper, BlogPostMapper postMapper,
                                  SiteSettingsService settingsService, HtmlSanitizer htmlSanitizer) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.settingsService = settingsService;
        this.htmlSanitizer = htmlSanitizer;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<GuestCommentVO> getPublicComments(Long postId, Integer page, Integer pageSize) {
        ensureCommentsEnabled();
        requirePublishedPost(postId);
        int current = normalizePage(page);
        int size = normalizeSize(pageSize);
        Page<GuestComment> queryPage = new Page<>(current, size);
        LambdaQueryWrapper<GuestComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GuestComment::getPostId, postId).eq(GuestComment::getStatus, 1)
                .orderByDesc(GuestComment::getCreateTime);
        IPage<GuestComment> result = commentMapper.selectPage(queryPage, wrapper);
        IPage<GuestCommentVO> converted = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        converted.setRecords(result.getRecords().stream().map(this::toView).collect(Collectors.toList()));
        return converted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(GuestCommentRequest request) {
        ensureCommentsEnabled();
        requirePublishedPost(request.getPostId());
        String nickname = htmlSanitizer.sanitizePlainText(request.getNickname().trim());
        String content = htmlSanitizer.sanitizePlainText(request.getContent().trim());
        if (nickname.isBlank() || nickname.length() > 80) throw new BusinessException(400, "昵称不能为空且不能超过80个字符");
        if (content.isBlank() || content.length() > 2000) throw new BusinessException(400, "评论内容不能为空且不能超过2000个字符");
        String visitorId = request.getVisitorId().trim();
        if (!visitorId.matches("[A-Za-z0-9_-]{8,80}")) throw new BusinessException(400, "访客标识无效");

        GuestComment comment = new GuestComment();
        comment.setPostId(request.getPostId());
        comment.setVisitorHash(VisitorHashUtils.hash(visitorId, hashSecret));
        comment.setNickname(nickname);
        comment.setContent(content);
        comment.setStatus(1);
        commentMapper.insert(comment);
        return comment.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<GuestCommentVO> getAdminComments(Integer page, Integer pageSize, Integer status) {
        int current = normalizePage(page);
        int size = normalizeSize(pageSize);
        Page<GuestComment> queryPage = new Page<>(current, size);
        LambdaQueryWrapper<GuestComment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(GuestComment::getStatus, status);
        wrapper.orderByDesc(GuestComment::getCreateTime);
        IPage<GuestComment> result = commentMapper.selectPage(queryPage, wrapper);
        List<Long> postIds = result.getRecords().stream().map(GuestComment::getPostId).distinct().toList();
        var posts = postIds.isEmpty() ? List.<BlogPost>of() : postMapper.selectBatchIds(postIds);
        return convertPage(result, posts);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 2) throw new BusinessException(400, "评论状态无效");
        GuestComment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(404, "评论不存在");
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (commentMapper.deleteById(id) == 0) throw new BusinessException(404, "评论不存在");
    }

    private IPage<GuestCommentVO> convertPage(IPage<GuestComment> source, List<BlogPost> posts) {
        var postTitles = posts.stream().collect(Collectors.toMap(BlogPost::getId, BlogPost::getTitle, (a, b) -> a));
        IPage<GuestCommentVO> converted = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        converted.setRecords(source.getRecords().stream().map(comment -> {
            GuestCommentVO vo = toView(comment);
            vo.setPostTitle(postTitles.get(comment.getPostId()));
            return vo;
        }).collect(Collectors.toList()));
        return converted;
    }

    private GuestCommentVO toView(GuestComment comment) {
        GuestCommentVO vo = new GuestCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setNickname(comment.getNickname());
        vo.setContent(comment.getContent());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    private BlogPost requirePublishedPost(Long postId) {
        BlogPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1
                || (post.getIsDeleted() != null && post.getIsDeleted() == 1)) {
            throw new BusinessException(404, "文章不存在");
        }
        return post;
    }

    private void ensureCommentsEnabled() {
        if (!settingsService.isCommentsEnabled()) throw new BusinessException(403, "站长暂时关闭了评论功能");
    }

    private int normalizePage(Integer page) { return page == null || page < 1 ? 1 : page; }
    private int normalizeSize(Integer size) { return size == null || size < 1 ? 20 : Math.min(size, 100); }
}
