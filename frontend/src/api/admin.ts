/**
 * 管理后台接口
 */

import api from './index'
import type {
  ApiResponse,
  PaginatedResponse,
  AdminPostListParams,
  Post,
  Tag,
  CreateTagRequest,
  UpdateTagRequest
} from '@/types'

/**
 * 管理后台接口（文章 / 标签管理）。
 *
 * 用户、举报、审核、评论等管理面不在前端暴露 —— 后端保留这些端点作为
 * 管理员 API 面，前端不提供对应页面。
 */
export const adminApi = {
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
