/**
 * 文章接口
 */

import api from './index'
import type {
  ApiResponse,
  PaginatedResponse,
  Post,
  CreatePostRequest,
  UpdatePostRequest,
  PostListParams,
  PostSearchParams,
  Draft
} from '@/types'

export const postApi = {
  /**
   * 创建文章
   */
  createPost(data: CreatePostRequest): Promise<ApiResponse<Post>> {
    return api.post('/post', data)
  },

  /**
   * 更新文章
   */
  updatePost(id: number | string, data: UpdatePostRequest): Promise<ApiResponse<Post>> {
    return api.put(`/post/${id}`, data)
  },

  /**
   * 删除文章
   */
  deletePost(id: number | string): Promise<ApiResponse> {
    return api.delete(`/post/${id}`)
  },

  /**
   * 获取文章详情
   */
  getPostById(id: number | string): Promise<ApiResponse<Post>> {
    return api.get(`/post/${id}`)
  },

  /**
   * 获取文章列表
   */
  getPostList(params: PostListParams): Promise<PaginatedResponse<Post>> {
    return api.get('/post/list', { params })
  },

  /**
   * 增加浏览量
   */
  incrementViewCount(id: number | string): Promise<ApiResponse> {
    return api.put(`/post/${id}/view`)
  },

  /**
   * 保存草稿
   */
  saveDraft(data: CreatePostRequest): Promise<ApiResponse<Draft>> {
    return api.post('/post/draft', data)
  },

  /**
   * 获取最新草稿
   */
  getLatestDraft(): Promise<ApiResponse<Draft>> {
    return api.get('/post/draft/latest')
  },

  /**
   * 获取指定草稿
   */
  getDraft(draftId: number | string): Promise<ApiResponse<Draft>> {
    return api.get(`/post/draft/${draftId}`)
  },

  /**
   * 删除草稿
   */
  deleteDraft(draftId: number | string): Promise<ApiResponse> {
    return api.delete(`/post/draft/${draftId}`)
  },

  /**
   * 高级搜索
   */
  advancedSearch(params: PostSearchParams): Promise<PaginatedResponse<Post>> {
    return api.get('/post/search/advanced', { params })
  },

  /**
   * 搜索建议
   */
  searchSuggest(keyword: string): Promise<ApiResponse<string[]>> {
    return api.get('/post/search/suggest', { params: { keyword } })
  },

  /**
   * 获取我的文章
   */
  getMyPosts(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Post>> {
    return api.get('/post/my', { params })
  },

  /**
   * 获取我的草稿
   */
  getMyDrafts(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Draft>> {
    return api.get('/post/draft/my', { params })
  }
}
