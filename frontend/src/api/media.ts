/**
 * 媒体文件上传接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, UploadType, Media, UploadResponse } from '@/types'
import type { AxiosProgressEvent } from 'axios'

export const mediaApi = {
  /**
   * 上传单个文件
   */
  uploadFile(
    file: File,
    type: UploadType = 'article',
    onProgress?: (event: AxiosProgressEvent) => void
  ): Promise<ApiResponse<UploadResponse>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)

    return api.post('/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: type === 'article' ? 300000 : 60000,
      onUploadProgress: onProgress
    })
  },

  /**
   * 批量上传文件（最多10个）
   */
  uploadMultipleFiles(
    files: File[],
    type: UploadType = 'article',
    onProgress?: (event: AxiosProgressEvent) => void
  ): Promise<ApiResponse<UploadResponse[]>> {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    formData.append('type', type)

    return api.post('/media/upload/multiple', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000,
      onUploadProgress: onProgress
    })
  },

  /**
   * 删除媒体文件
   */
  deleteMedia(mediaId: number | string): Promise<ApiResponse> {
    return api.delete(`/media/${mediaId}`)
  },

  /**
   * 获取媒体文件信息
   */
  getMediaInfo(mediaId: number | string): Promise<ApiResponse<Media>> {
    return api.get(`/media/${mediaId}`)
  },

  /**
   * 绑定媒体文件到文章
   */
  bindPostMedia(postId: number | string, mediaIds: number[]): Promise<ApiResponse> {
    return api.put(`/media/bind/${postId}`, mediaIds)
  },

  /**
   * 获取文章的媒体列表
   */
  getPostMedia(postId: number | string): Promise<ApiResponse<Media[]>> {
    return api.get(`/media/post/${postId}`)
  },

  /**
   * 获取用户媒体列表（分页）
   */
  getUserMediaList(params: { pageNum?: number; pageSize?: number }): Promise<PaginatedResponse<Media>> {
    return api.get('/media/list', { params })
  }
}
