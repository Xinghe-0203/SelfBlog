/**
 * 通知接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, Notification } from '@/types'

export const notificationApi = {
  /**
   * 获取通知列表
   */
  getNotifications(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<PaginatedResponse<Notification>> {
    return api.get('/notification/list', { params })
  },

  /**
   * 获取未读数量
   */
  getUnreadCount(): Promise<ApiResponse<number>> {
    return api.get('/notification/unread-count')
  },

  /**
   * 标记为已读
   */
  markAsRead(id: number | string): Promise<ApiResponse> {
    return api.put(`/notification/${id}/read`)
  },

  /**
   * 全部标记为已读
   */
  markAllAsRead(): Promise<ApiResponse> {
    return api.put('/notification/read-all')
  },

  /**
   * 删除通知
   */
  deleteNotification(id: number | string): Promise<ApiResponse> {
    return api.delete(`/notification/${id}`)
  }
}
