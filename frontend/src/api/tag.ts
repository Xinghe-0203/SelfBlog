/**
 * 标签接口
 */

import api from './index'
import { cachedGet, invalidateApiCache } from '@/utils/apiCache'
import type { ApiResponse, Tag, TagWithCount, CreateTagRequest, UpdateTagRequest } from '@/types'

export const tagApi = {
  /**
   * 获取标签列表
   *
   * 公开只读接口，走 120 秒的短期缓存：标签几乎不变，但首页侧栏与筛选器
   * 每次渲染都要它。
   */
  getTagList(): Promise<ApiResponse<Tag[]>> {
    return cachedGet('tag:list', 120_000, () => api.get('/tag/list'))
  },

  /**
   * 获取标签及其已发布文章数（公开标签索引页用）。
   *
   * 计数只含已发布文章 —— 草稿不公开，也不该在公开计数里出现。
   */
  getTagsWithCounts(): Promise<ApiResponse<TagWithCount[]>> {
    return cachedGet('tag:with-counts', 120_000, () => api.get('/tag/with-counts'))
  },

  /**
   * 获取单个标签
   */
  getTagById(tagId: number | string): Promise<ApiResponse<Tag>> {
    return api.get(`/tag/${tagId}`)
  },

  /**
   * 创建标签
   *
   * 显式写出响应泛型：`.finally()` 会丢掉调用点的上下文类型，
   * axios 的 `post<T, R>` 里 R 就回落到默认的 AxiosResponse<T>，与本函数的返回类型不符。
   */
  createTag(data: CreateTagRequest): Promise<ApiResponse<Tag>> {
    return api
      .post<unknown, ApiResponse<Tag>>('/tag', data)
      .finally(() => invalidateApiCache('tag:'))
  },

  /**
   * 更新标签
   */
  updateTag(tagId: number | string, data: UpdateTagRequest): Promise<ApiResponse<Tag>> {
    return api
      .put<unknown, ApiResponse<Tag>>(`/admin/tag/${tagId}`, data)
      .finally(() => invalidateApiCache('tag:'))
  },

  /**
   * 删除标签
   */
  deleteTag(tagId: number | string): Promise<ApiResponse> {
    return api
      .delete<unknown, ApiResponse>(`/tag/${tagId}`)
      .finally(() => invalidateApiCache('tag:'))
  },

  /**
   * 搜索标签
   */
  searchTags(keyword: string): Promise<ApiResponse<Tag[]>> {
    return api.get('/tag/search', { params: { keyword } })
  }
}
