import api from './index'
import { getVisitorId } from '@/utils/visitor'
import type { ApiResponse } from '@/types'
import type { PageResult } from './site'

export interface AnonymousLike {
  liked: boolean
  likeCount: number
}

export interface GuestComment {
  id: number
  postId: number
  postTitle?: string
  nickname: string
  content: string
  status: number
  createTime: string
}

export const publicInteractionApi = {
  getLikeStatus(postId: number): Promise<ApiResponse<AnonymousLike>> {
    return api.get(`/anonymous-likes/posts/${postId}`)
  },
  toggleLike(postId: number): Promise<ApiResponse<AnonymousLike>> {
    return api.post(`/anonymous-likes/posts/${postId}`)
  },
  getComments(postId: number, params: { pageNum?: number; pageSize?: number }): Promise<ApiResponse<PageResult<GuestComment>>> {
    return api.get(`/guest-comments/post/${postId}`, { params })
  },
  createComment(data: { postId: number; nickname: string; content: string }): Promise<ApiResponse<number>> {
    return api.post('/guest-comments', { ...data, visitorId: getVisitorId() })
  },
  getAdminComments(params: { pageNum?: number; pageSize?: number; status?: number }): Promise<ApiResponse<PageResult<GuestComment>>> {
    return api.get('/admin/guest-comments', { params })
  },
  updateCommentStatus(id: number, status: number): Promise<ApiResponse> {
    return api.patch(`/admin/guest-comments/${id}/status`, { status })
  },
  deleteComment(id: number): Promise<ApiResponse> {
    return api.delete(`/admin/guest-comments/${id}`)
  }
}
