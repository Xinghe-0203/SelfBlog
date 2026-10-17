/**
 * 热搜/热门接口
 */

import api from './index'
import type {
  ApiResponse,
  PaginatedResponse,
  Post,
  Tag,
  TrendingContent,
  TrendingParams
} from '@/types'

export const trendingApi = {
  /**
   * 获取热门文章
   */
  getHotPosts(params: TrendingParams): Promise<PaginatedResponse<Post>> {
    return api.get('/trending/posts', {
      params: {
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 10
      }
    })
  },

  /**
   * 获取热门标签
   */
  getHotTags(): Promise<PaginatedResponse<Tag>> {
    return api.get('/trending/hot-tags')
  },

  /**
   * 获取热门混排内容（文章+动态）
   */
  getHotContent(params: TrendingParams): Promise<PaginatedResponse<TrendingContent>> {
    return api.get('/trending/content', {
      params: {
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 10
      }
    })
  },

  /**
   * 更新热门状态
   */
  updateTrending(postId: number | string): Promise<ApiResponse> {
    return api.put(`/trending/${postId}`)
  }
}
