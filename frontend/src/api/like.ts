/**
 * 点赞接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, Post } from '@/types'

export const likeApi = {
  /**
   * 切换点赞状态
   */
  toggleLike(postId: number | string): Promise<ApiResponse<{ liked: boolean }>> {
    return api.post(`/like/${postId}`)
  },

  /**
   * 检查是否已点赞
   */
  checkLikeStatus(postId: number | string): Promise<ApiResponse<{ liked: boolean }>> {
    return api.get(`/like/${postId}/status`)
  },

  /**
   * 批量检查点赞状态
   */
  checkLikeStatusBatch(postIds: (number | string)[]): Promise<ApiResponse<Record<string, boolean>>> {
    return api.post('/like/check/batch', postIds)
  },

  /**
   * 获取我的点赞
   */
  getMyLikes(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Post>> {
    return api.get('/like/my', { params })
  }
}
