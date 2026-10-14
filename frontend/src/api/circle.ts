/**
 * 校友圈接口
 */

import api from './index'
import type {
  ApiResponse,
  PaginatedResponse,
  CirclePost,
  CreateCirclePostRequest,
  UpdateCirclePostRequest,
  CircleComment,
  CreateCircleCommentRequest,
  CirclePostListParams
} from '@/types'

export const circleApi = {
  /**
   * 发布动态
   */
  createPost(data: CreateCirclePostRequest): Promise<ApiResponse<CirclePost>> {
    return api.post('/circle/post', data)
  },

  /**
   * 更新动态
   */
  updatePost(postId: number | string, data: UpdateCirclePostRequest): Promise<ApiResponse<CirclePost>> {
    return api.put(`/circle/post/${postId}`, data)
  },

  /**
   * 删除动态
   */
  deletePost(postId: number | string): Promise<ApiResponse> {
    return api.delete(`/circle/post/${postId}`)
  },

  /**
   * 搜索动态
   */
  searchPosts(params: CirclePostListParams): Promise<PaginatedResponse<CirclePost>> {
    return api.get('/circle/search', { params })
  },

  /**
   * 获取推荐动态
   */
  getRecommendFeed(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<PaginatedResponse<CirclePost>> {
    return api.get('/circle/feed/recommend', { params })
  },

  /**
   * 获取关注动态
   */
  getFollowingFeed(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<PaginatedResponse<CirclePost>> {
    return api.get('/circle/feed/following', { params })
  },

  /**
   * 获取动态详情
   */
  getPostById(postId: number | string): Promise<ApiResponse<CirclePost>> {
    return api.get(`/circle/post/${postId}`)
  },

  /**
   * 点赞/取消点赞
   */
  toggleLike(postId: number | string): Promise<ApiResponse<{ liked: boolean }>> {
    return api.post(`/circle/like/${postId}`)
  },

  /**
   * 检查点赞状态
   */
  checkLikeStatus(postId: number | string): Promise<ApiResponse<{ liked: boolean }>> {
    return api.get(`/circle/like/check/${postId}`)
  },

  /**
   * 获取评论
   */
  getComments(
    postId: number | string,
    params: { pageNum?: number; pageSize?: number }
  ): Promise<PaginatedResponse<CircleComment>> {
    return api.get(`/circle/comment/${postId}`, { params })
  },

  /**
   * 发表评论
   */
  createComment(data: CreateCircleCommentRequest): Promise<ApiResponse<CircleComment>> {
    return api.post('/circle/comment', data)
  },

  /**
   * 删除评论
   */
  deleteComment(commentId: number | string): Promise<ApiResponse> {
    return api.delete(`/circle/comment/${commentId}`)
  },

  /**
   * 转发
   */
  repost(postId: number | string, content: string): Promise<ApiResponse> {
    return api.post(`/circle/repost/${postId}`, null, { params: { content } })
  },

  /**
   * 获取用户动态列表
   */
  getUserPosts(
    userId: number | string,
    params: { pageNum?: number; pageSize?: number }
  ): Promise<PaginatedResponse<CirclePost>> {
    return api.get(`/circle/user/${userId}`, { params })
  }
}
