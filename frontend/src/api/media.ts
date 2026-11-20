/**
 * 媒体文件上传接口
 */

import api from './index'
import type { ApiResponse, UploadType, UploadResponse } from '@/types'
import type { AxiosProgressEvent } from 'axios'

export const mediaApi = {
  /**
   * 上传单个文件
   *
   * `_noRetry`：上传有真实副作用、代价高（article 档 300s 超时），网络重试会把整个
   * 文件重新发一遍 —— 与 LLM 调用同一条「不自动重试」约定
   * （见 `constants/index.ts` 的 RETRYABLE_CODES 注释）。
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
      onUploadProgress: onProgress,
      _noRetry: true
    })
  }
}
