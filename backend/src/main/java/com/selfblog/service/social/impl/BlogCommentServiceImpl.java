package com.selfblog.service.social.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.social.CommentCreateRequest;
import com.selfblog.entity.BlogComment;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.SysUser;
import com.selfblog.event.CommentCreatedEvent;
import com.selfblog.mapper.BlogCommentMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.BlogCommentService;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.service.content.TrendingService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.post.CommentVO;
import com.selfblog.vo.post.CommentWithPostVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements BlogCommentService {

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TrendingService trendingService;

    @Autowired
    private BlogPostMapper blogPostMapper;

    private static final int MAX_RECURSION_DEPTH = 100; // 最大递归深度，防止栈溢出

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateRequest request, Long userId) {
        // 检查文章是否存在
        BlogPost post = blogPostService.getById(request.getPostId());
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getStatus() == null || post.getStatus() != 1) {
            throw new BusinessException(404, "文章不存在");
        }

        // 如果是回复，检查父评论是否存在
        if (request.getParentId() != null) {
            BlogComment parentComment = this.getById(request.getParentId());
            if (parentComment == null) {
                throw new BusinessException(404, "父评论不存在");
            }
            // 检查父评论是否属于同一篇文章
            if (!Objects.equals(request.getPostId(), parentComment.getPostId())) {
                throw new BusinessException(400, "父评论不属于该文章");
            }
        }

        // 创建评论
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(400, "评论内容不能超过2000字符");
        }
        // XSS 防护：评论使用严格策略，只保留纯文本，移除所有 HTML 标签
        String sanitizedContent = content != null ? htmlSanitizer.sanitizePlainText(content) : null;

        BlogComment comment = new BlogComment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(sanitizedContent);
        this.save(comment);

        // 更新文章评论数
        blogPostService.incrementCommentCount(request.getPostId());
        // 更新热门趋势数据
        trendingService.updatePostTrending(request.getPostId());

        // 发布评论创建事件，事务提交后异步发送通知
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new CommentCreatedEvent(
                        comment.getId(),
                        userId,
                        post.getUserId(),
                        request.getPostId(),
                        sanitizedContent,
                        request.getParentId() != null
                ));
            }
        });

        log.info("评论创建成功: commentId={}, postId={}, userId={}", comment.getId(), request.getPostId(), userId);
        return comment.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<CommentVO> getCommentsByPostId(Long postId, Integer pageNum, Integer pageSize) {
        // 先检查文章是否存在且已发布
        BlogPost post = blogPostService.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        // 只有已发布的文章才能查看评论
        if (post.getStatus() == null || post.getStatus() != 1) {
            throw new BusinessException(403, "文章未发布或已下架");
        }

        // C21：分页只针对根评论。旧实现按「全部评论」分页、再在 Java 里丢掉回复，
        // 于是 DB 的 total 把回复也算进页数（records.size() < total，深页返回空数组却仍宣称有数据）。
        // 把过滤条件下推到查询里，DB total 天然等于根评论数，records 与 total 同一谓词。
        Page<BlogComment> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogComment::getPostId, postId)
               .isNull(BlogComment::getParentId)
               .orderByDesc(BlogComment::getCreateTime);
        IPage<BlogComment> commentPage = this.page(pageParam, wrapper);

        List<BlogComment> pageRootComments = commentPage.getRecords();
        if (pageRootComments.isEmpty()) {
            return new Page<>(pageNum, pageSize, commentPage.getTotal());
        }

        // 回复不属于分页单位：单独取本页根评论下的直接回复，保证树形结构不因下推过滤而丢失。
        List<Long> pageRootIds = pageRootComments.stream()
                .map(BlogComment::getId)
                .collect(Collectors.toList());
        List<BlogComment> replies = this.list(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .in(BlogComment::getParentId, pageRootIds)
                .orderByAsc(BlogComment::getCreateTime));

        List<BlogComment> comments = new ArrayList<>(pageRootComments);
        comments.addAll(replies);

        // 获取所有评论者用户ID
        List<Long> userIds = comments.stream()
                .map(BlogComment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 先将评论列表转为 Map，ID -> Comment，用于 O(1) 查找父评论
        Map<Long, BlogComment> commentMapById = comments.stream()
                .collect(Collectors.toMap(BlogComment::getId, c -> c));

        // 转换为VO
        List<CommentVO> commentVOs = comments.stream()
                .map(comment -> {
                    CommentVO vo = new CommentVO();
                    BeanUtils.copyProperties(comment, vo);
                    SysUser user = userMap.get(comment.getUserId());
                    if (user != null) {
                        vo.setNickname(user.getNickname());
                        vo.setUsername(user.getUsername());
                        vo.setUserAvatar(user.getAvatar());
                    }
                    // 如果是回复，使用 Map O(1) 查找父评论
                    if (comment.getParentId() != null) {
                        BlogComment parentComment = commentMapById.get(comment.getParentId());
                        if (parentComment != null) {
                            SysUser parentUser = userMap.get(parentComment.getUserId());
                            if (parentUser != null) {
                                vo.setReplyToNickname(parentUser.getNickname());
                            }
                        }
                    }
                    vo.setReplies(new ArrayList<>());
                    return vo;
                })
                .collect(Collectors.toList());

        // 构建树形结构
        Map<Long, CommentVO> voMap = commentVOs.stream()
                .collect(Collectors.toMap(CommentVO::getId, c -> c));

        List<CommentVO> rootComments = new ArrayList<>();
        for (CommentVO vo : commentVOs) {
            if (vo.getParentId() == null) {
                rootComments.add(vo);
            } else {
                CommentVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    parent.getReplies().add(vo);
                }
            }
        }

        // C21：total 直接取根评论分页的 DB 计数（谓词已下推，与 records 同源），不再用本页数量冒充总数。
        Page<CommentVO> result = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        result.setRecords(rootComments);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CommentVO getCommentById(Long commentId) {
        BlogComment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        SysUser user = sysUserMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setUsername(user.getUsername());
            vo.setUserAvatar(user.getAvatar());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        BlogComment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }

        // 检查权限：作者本人或管理员可删除
        if (!Objects.equals(userId, comment.getUserId()) && !SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权删除此评论");
        }

        // 查找所有要删除的评论ID（包括当前评论及其所有子评论）
        List<Long> commentIdsToDelete = new ArrayList<>();
        commentIdsToDelete.add(commentId);
        collectChildCommentIdsBatch(commentId, commentIdsToDelete);

        // 批量删除所有相关评论（逻辑删除）
        this.removeByIds(commentIdsToDelete);

        log.info("[AUDIT] 评论删除: commentId={}, userId={}, deletedCount={}", commentId, userId, commentIdsToDelete.size());

        // C8: 用真实行数回写绝对值。原实现传 commentIdsToDelete.size() 走相对减法，且不检查
        // removeByIds 的实际影响行数——并发删除同一棵子树时两次都减，计数直接变负；历史漂移也无法自愈。
        blogPostService.syncCommentCount(comment.getPostId());
        // 更新热门趋势数据
        trendingService.updatePostTrending(comment.getPostId());
    }

    /**
     * 批量收集所有子评论ID（解决N+1问题）
     * 先一次性查询所有评论，然后在内存中构建树结构
     */
    private void collectChildCommentIdsBatch(Long rootCommentId, List<Long> result) {
        // 一次性查询该文章的所有评论
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogComment::getPostId, getPostIdForComment(rootCommentId));
        List<BlogComment> allComments = this.list(wrapper);

        // 构建 parentId -> 子评论列表 的映射
        Map<Long, List<BlogComment>> childrenMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(BlogComment::getParentId));

        // 使用栈实现非递归遍历
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootCommentId);

        int depth = 0;
        while (!stack.isEmpty() && depth <= MAX_RECURSION_DEPTH) {
            int levelSize = stack.size();
            for (int i = 0; i < levelSize; i++) {
                Long parentId = stack.pop();
                List<BlogComment> children = childrenMap.get(parentId);
                if (children != null) {
                    for (BlogComment child : children) {
                        result.add(child.getId());
                        stack.push(child.getId());
                    }
                }
            }
            depth++;
        }

        if (depth > MAX_RECURSION_DEPTH) {
            throw new BusinessException(400, "评论层级过深，请简化回复结构");
        }
    }

    private Long getPostIdForComment(Long commentId) {
        BlogComment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        return comment.getPostId();
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<CommentWithPostVO> getAllComments(Integer page, Integer pageSize) {
        Page<BlogComment> commentPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BlogComment::getCreateTime);

        IPage<BlogComment> commentResult = this.page(commentPage, wrapper);

        return buildCommentWithPostVOPage(commentResult, page, pageSize);
    }

    private IPage<CommentWithPostVO> buildCommentWithPostVOPage(IPage<BlogComment> commentResult, Integer page, Integer pageSize) {
        if (commentResult.getRecords().isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }

        List<Long> postIds = commentResult.getRecords().stream()
                .map(BlogComment::getPostId)
                .distinct()
                .collect(Collectors.toList());

        List<BlogPost> posts = blogPostMapper.selectBatchIds(postIds);
        Map<Long, BlogPost> postMap = posts.stream()
                .collect(Collectors.toMap(BlogPost::getId, p -> p, (a, b) -> a));

        IPage<CommentWithPostVO> resultPage = new Page<>(
                commentResult.getCurrent(),
                commentResult.getSize(),
                commentResult.getTotal()
        );

        List<CommentWithPostVO> items = commentResult.getRecords().stream()
                .map(comment -> {
                    CommentWithPostVO item = new CommentWithPostVO();
                    item.setId(comment.getId());
                    item.setPostId(comment.getPostId());
                    item.setParentId(comment.getParentId());
                    item.setContent(comment.getContent());
                    item.setCreateTime(comment.getCreateTime());

                    BlogPost post = postMap.get(comment.getPostId());
                    if (post != null) {
                        item.setPostTitle(post.getTitle());
                    }
                    return item;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(items);
        return resultPage;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<CommentWithPostVO> getMyComments(Long userId, Integer page, Integer pageSize) {
        Page<BlogComment> commentPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogComment::getUserId, userId)
                .orderByDesc(BlogComment::getCreateTime);

        IPage<BlogComment> commentResult = this.page(commentPage, wrapper);

        return buildCommentWithPostVOPage(commentResult, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteComment(Long commentId, Long adminId) {
        BlogComment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }

        // 查找所有要删除的评论ID（包括当前评论及其所有子评论）
        List<Long> commentIdsToDelete = new ArrayList<>();
        commentIdsToDelete.add(commentId);
        collectChildCommentIdsBatch(commentId, commentIdsToDelete);

        // 批量删除所有相关评论（逻辑删除）
        this.removeByIds(commentIdsToDelete);

        log.info("[AUDIT] 管理员删除评论: commentId={}, adminId={}, deletedCount={}", commentId, adminId, commentIdsToDelete.size());

        // C8: 同 deleteComment —— 写回真实行数，不再用相对减法（并发删除会重复减，历史漂移无法自愈）
        blogPostService.syncCommentCount(comment.getPostId());
        // 更新热门趋势数据
        trendingService.updatePostTrending(comment.getPostId());
    }
}
