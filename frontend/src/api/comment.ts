/**
 * 评论接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, Comment, CreateCommentRequest, CommentListParams } from '@/types'

export const commentApi = {
  /**
   * 发表评论
   */
  createComment(data: CreateCommentRequest): Promise<ApiResponse<Comment>> {
    return api.post('/comment', data)
  },

  /**
   * 获取文章评论
   */
  getCommentsByPostId(
    postId: number | string,
    params: CommentListParams
  ): Promise<PaginatedResponse<Comment>> {
    return api.get(`/comment/post/${postId}`, { params })
  },

  /**
   * 删除评论
   */
  deleteComment(commentId: number | string): Promise<ApiResponse> {
    return api.delete(`/comment/${commentId}`)
  },

  /**
   * 获取单个评论详情
   */
  getCommentById(commentId: number | string): Promise<ApiResponse<Comment>> {
    return api.get(`/comment/${commentId}`)
  },

  /**
   * 获取我的评论
   */
  getMyComments(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Comment>> {
    return api.get('/comment/my', { params })
  }
}
