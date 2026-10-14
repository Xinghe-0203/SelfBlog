/**
 * 标签接口
 */

import api from './index'
import type { ApiResponse, Tag, CreateTagRequest, UpdateTagRequest } from '@/types'

export const tagApi = {
  /**
   * 获取标签列表
   */
  getTagList(): Promise<ApiResponse<Tag[]>> {
    return api.get('/tag/list')
  },

  /**
   * 获取单个标签
   */
  getTagById(tagId: number | string): Promise<ApiResponse<Tag>> {
    return api.get(`/tag/${tagId}`)
  },

  /**
   * 创建标签
   */
  createTag(data: CreateTagRequest): Promise<ApiResponse<Tag>> {
    return api.post('/tag', data)
  },

  /**
   * 更新标签
   */
  updateTag(tagId: number | string, data: UpdateTagRequest): Promise<ApiResponse<Tag>> {
    return api.put(`/admin/tag/${tagId}`, data)
  },

  /**
   * 删除标签
   */
  deleteTag(tagId: number | string): Promise<ApiResponse> {
    return api.delete(`/tag/${tagId}`)
  },

  /**
   * 搜索标签
   */
  searchTags(keyword: string): Promise<ApiResponse<Tag[]>> {
    return api.get('/tag/search', { params: { keyword } })
  }
}
