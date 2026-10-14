/**
 * 关注接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, User, FollowCounts } from '@/types'

export const followApi = {
  /**
   * 关注/取消关注
   */
  toggleFollow(targetUserId: number | string): Promise<ApiResponse<{ followed: boolean }>> {
    return api.post('/follow', { targetUserId })
  },

  /**
   * 取消关注
   */
  unfollow(targetUserId: number | string): Promise<ApiResponse> {
    return api.delete(`/follow/${targetUserId}`)
  },

  /**
   * 检查关注状态
   */
  checkFollowStatus(targetUserId: number | string): Promise<ApiResponse<{ followed: boolean }>> {
    return api.get(`/follow/${targetUserId}/status`)
  },

  /**
   * 获取粉丝列表
   */
  getFollowers(
    userId: number | string,
    params: { pageNum?: number; pageSize?: number }
  ): Promise<PaginatedResponse<User>> {
    return api.get(`/follow/followers/${userId}`, { params })
  },

  /**
   * 获取关注列表
   */
  getFollowing(
    userId: number | string,
    params: { pageNum?: number; pageSize?: number }
  ): Promise<PaginatedResponse<User>> {
    return api.get(`/follow/following/${userId}`, { params })
  },

  /**
   * 获取关注/粉丝数量
   */
  getFollowCounts(userId: number | string): Promise<ApiResponse<FollowCounts>> {
    return api.get(`/follow/counts/${userId}`)
  }
}
