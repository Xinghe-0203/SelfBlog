package com.selfblog.service.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.dto.social.CommentCreateRequest;
import com.selfblog.entity.BlogComment;
import com.selfblog.vo.post.CommentVO;
import com.selfblog.vo.post.CommentWithPostVO;

/**
 * 评论服务接口
 */
public interface BlogCommentService extends IService<BlogComment> {

    /**
     * 发表评论
     * @param request 评论请求
     * @param userId 评论者ID
     * @return 评论ID
     */
    Long createComment(CommentCreateRequest request, Long userId);

    /**
     * 获取文章评论列表（树形结构，分页）
     * @param postId 文章ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页评论列表
     */
    IPage<CommentVO> getCommentsByPostId(Long postId, Integer pageNum, Integer pageSize);

    /**
     * 获取单个评论详情
     * @param commentId 评论ID
     * @return 评论详情
     */
    CommentVO getCommentById(Long commentId);

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 操作人ID
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 获取我的评论列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<CommentWithPostVO> getMyComments(Long userId, Integer page, Integer pageSize);

    /**
     * 获取所有评论列表（管理员用）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<CommentWithPostVO> getAllComments(Integer page, Integer pageSize);

    /**
     * 管理员删除评论（级联删除子评论，更新文章评论数）
     * @param commentId 评论ID
     * @param adminId 管理员ID
     */
    void adminDeleteComment(Long commentId, Long adminId);
}
