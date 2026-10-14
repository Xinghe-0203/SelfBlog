/**
 * 收藏接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, Post } from '@/types'

export const collectApi = {
  /**
   * 切换收藏状态
   */
  toggleCollect(postId: number | string): Promise<ApiResponse<{ collected: boolean }>> {
    return api.post(`/collect/${postId}`)
  },

  /**
   * 检查是否已收藏
   */
  checkCollectStatus(postId: number | string): Promise<ApiResponse<{ collected: boolean }>> {
    return api.get(`/collect/${postId}/status`)
  },

  /**
   * 批量检查收藏状态
   */
  checkCollectStatusBatch(
    postIds: (number | string)[]
  ): Promise<ApiResponse<Record<string, boolean>>> {
    return api.post('/collect/check/batch', postIds)
  },

  /**
   * 获取我的收藏
   */
  getMyCollections(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Post>> {
    return api.get('/collect/my', { params })
  }
}
