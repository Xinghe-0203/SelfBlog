/**
 * 管理后台接口
 */

import api from './index'
import { useLogger } from '@/utils/logger'
import type {
  ApiResponse,
  PaginatedResponse,
  CommunityStats,
  AdminStatistics,
  AdminUserListParams,
  AdminPostListParams,
  HandleReportRequest,
  HandleUserStatusRequest,
  User,
  Post,
  Report,
  Tag,
  Comment,
  CreateTagRequest,
  UpdateTagRequest
} from '@/types'

const logger = useLogger('AdminApi')

/**
 * 社区统计接口
 */
export const statsApi = {
  /**
   * 获取社区统计
   */
  getCommunityStats(): Promise<ApiResponse<CommunityStats>> {
    return api.get('/statistics/community')
  }
}

/**
 * 管理后台接口
 */
export const adminApi = {
  /**
   * 获取待处理举报
   */
  getPendingReports(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<PaginatedResponse<Report>> {
    return api.get('/admin/reports/pending', { params })
  },

  /**
   * 获取举报详情
   */
  getReportById(reportId: number | string): Promise<ApiResponse<Report>> {
    return api.get(`/admin/reports/${reportId}`)
  },

  /**
   * 处理举报
   */
  handleReport(reportId: number | string, data: HandleReportRequest): Promise<ApiResponse> {
    return api.put(`/admin/reports/${reportId}`, data)
  },

  /**
   * 处理用户状态
   */
  handleUserStatus(userId: number | string, data: HandleUserStatusRequest): Promise<ApiResponse> {
    return api.put(`/admin/user/${userId}/status`, data)
  },

  /**
   * 获取用户列表
   */
  getUserList(params: AdminUserListParams): Promise<PaginatedResponse<User>> {
    return api.get('/admin/user/list', { params })
  },

  /**
   * 重置用户密码
   */
  resetUserPassword(userId: number | string): Promise<ApiResponse> {
    logger.info('Admin password reset initiated', { targetUserId: userId })
    return api.put(`/admin/user/${userId}/reset-password`)
  },

  /**
   * 封禁/解封用户
   */
  banUser(userId: number | string, ban: boolean): Promise<ApiResponse> {
    return api.put(`/admin/user/${userId}/ban`, { ban })
  },

  /**
   * 获取文章列表
   */
  getPostList(params: AdminPostListParams): Promise<PaginatedResponse<Post>> {
    return api.get('/admin/post/list', { params })
  },

  /**
   * 删除文章
   */
  deletePost(postId: number | string): Promise<ApiResponse> {
    return api.delete(`/admin/post/${postId}`)
  },

  /**
   * 获取待审核文章
   */
  getReviewList(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Post>> {
    return api.get('/admin/post/review-list', { params })
  },

  /**
   * 审核通过文章
   */
  approvePost(postId: number | string): Promise<ApiResponse> {
    return api.put(`/admin/post/${postId}/approve`)
  },

  /**
   * 审核拒绝文章
   */
  rejectPost(postId: number | string, reason: string): Promise<ApiResponse> {
    return api.put(`/admin/post/${postId}/reject`, { reason })
  },

  /**
   * 获取统计数据
   */
  getStatistics(): Promise<ApiResponse<AdminStatistics>> {
    return api.get('/admin/statistics')
  },

  // Comment management
  getCommentList(params: {
    pageNum?: number
    pageSize?: number
    postId?: number
  }): Promise<PaginatedResponse<Comment>> {
    return api.get('/admin/comment/list', { params })
  },

  deleteComment(commentId: number | string): Promise<ApiResponse> {
    return api.delete(`/admin/comment/${commentId}`)
  },

  // Tag management
  getTagList(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Tag>> {
    return api.get('/admin/tag/list', { params })
  },

  createTag(data: CreateTagRequest): Promise<ApiResponse<Tag>> {
    return api.post('/admin/tag', data)
  },

  updateTag(id: number | string, data: UpdateTagRequest): Promise<ApiResponse<Tag>> {
    return api.put(`/admin/tag/${id}`, data)
  },

  deleteTag(id: number | string): Promise<ApiResponse> {
    return api.delete(`/admin/tag/${id}`)
  }
}
