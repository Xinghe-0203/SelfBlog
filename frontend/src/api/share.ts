/**
 * 分享接口
 */

import api from './index'
import type { ApiResponse } from '@/types'

export const shareApi = {
  /**
   * 记录分享
   */
  recordShare(postId: number | string, platform = 'web'): Promise<ApiResponse> {
    return api.post(`/share/${postId}`, null, { params: { platform } })
  },

  /**
   * 获取分享数
   */
  getShareCount(postId: number | string): Promise<ApiResponse<number>> {
    return api.get(`/share/count/${postId}`)
  }
}

export default shareApi
