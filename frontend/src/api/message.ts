/**
 * 私信接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, Message, Conversation, SendMessageRequest } from '@/types'

export const messageApi = {
  /**
   * 获取会话列表
   */
  getConversations(): Promise<ApiResponse<Conversation[]>> {
    return api.get('/message/conversations')
  },

  /**
   * 获取会话消息
   */
  getConversationMessages(
    partnerUserId: number | string,
    params: { pageNum?: number; pageSize?: number }
  ): Promise<PaginatedResponse<Message>> {
    return api.get(`/message/conversation/${partnerUserId}`, { params })
  },

  /**
   * 发送私信
   */
  sendMessage(data: SendMessageRequest): Promise<ApiResponse<Message>> {
    return api.post('/message', data)
  },

  /**
   * 获取收到的私信列表（分页）
   */
  getReceivedMessages(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<PaginatedResponse<Message>> {
    return api.get('/message/received', { params })
  },

  /**
   * 获取发送的私信列表（分页）
   */
  getSentMessages(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<PaginatedResponse<Message>> {
    return api.get('/message/sent', { params })
  },

  /**
   * 标记私信为已读
   */
  markAsRead(messageId: number | string): Promise<ApiResponse> {
    return api.put(`/message/${messageId}/read`)
  },

  /**
   * 删除私信
   */
  deleteMessage(messageId: number | string): Promise<ApiResponse> {
    return api.delete(`/message/${messageId}`)
  },

  /**
   * 标记会话中所有消息为已读
   */
  markConversationAsRead(partnerUserId: number | string): Promise<ApiResponse> {
    return api.put(`/message/conversation/${partnerUserId}/read`)
  },

  /**
   * 获取未读数量
   */
  getUnreadCount(): Promise<ApiResponse<number>> {
    return api.get('/message/unread-count')
  }
}
