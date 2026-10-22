import api from './index'
import type { ApiResponse } from '@/types'
import type { PageResult } from './site'
import type {
  SeoBatchRequest,
  SeoBatchResult,
  SeoDetail,
  SeoLlmTest,
  SeoOverview,
  SeoPostListParams,
  SeoPostRow,
  SeoUpdateRequest
} from '@/types/seo'

/**
 * 这些请求**必须**禁用自动重试（`_noRetry`）。
 *
 * <p>axios 的超时错误码是 `ECONNABORTED`，而它在 `RETRYABLE_CODES` 里（对普通请求
 * 是对的：网络抖动值得重试）。但模型生成每次都会真实消耗额度，超时又恰恰是它最常见
 * 的失败形态 —— 自动重试会把一次超时放大成三次上游调用、三倍等待与三倍账单，
 * 管理员看到的是「转圈很久然后还是失败」。失败就让它失败，由人决定是否重来。</p>
 */
const NO_RETRY = { _noRetry: true } as const

/**
 * 后台 SEO 自动化接口。
 *
 * 全部走标准信封 `Result<T>`：调用方拿到的是 `{ code, message, data }`（拦截器已解包
 * AxiosResponse），因此这里返回 `Promise<ApiResponse<T>>`，调用点读 `response.data`。
 */
export const seoApi = {
  getOverview(): Promise<ApiResponse<SeoOverview>> {
    return api.get('/admin/seo/overview')
  },

  getPosts(params: SeoPostListParams): Promise<ApiResponse<PageResult<SeoPostRow>>> {
    return api.get('/admin/seo/posts', {
      params: {
        pageNum: params.pageNum,
        pageSize: params.pageSize,
        keyword: params.keyword || undefined,
        issue: params.issue || undefined
      }
    })
  },

  getDetail(postId: number): Promise<ApiResponse<SeoDetail>> {
    return api.get(`/admin/seo/posts/${postId}`)
  },

  update(postId: number, data: SeoUpdateRequest): Promise<ApiResponse<SeoDetail>> {
    return api.put(`/admin/seo/posts/${postId}`, data)
  },

  /** 让模型为单篇生成 SEO 元数据并落库（source=llm） */
  generate(postId: number): Promise<ApiResponse<SeoDetail>> {
    // 生成要等上游模型，超时给足；后端自己还有 llmTimeoutSeconds 兜底。
    return api.post(`/admin/seo/posts/${postId}/generate`, null, {
      timeout: 130000,
      ...NO_RETRY
    })
  },

  batchGenerate(data: SeoBatchRequest): Promise<ApiResponse<SeoBatchResult>> {
    return api.post('/admin/seo/batch', data, { timeout: 130000, ...NO_RETRY })
  },

  testConnection(): Promise<ApiResponse<SeoLlmTest>> {
    return api.post('/admin/seo/test-connection', null, { timeout: 60000, ...NO_RETRY })
  },

  /** 删除该篇的 SEO 记录，前台回落为「由标题/摘要派生」 */
  remove(postId: number): Promise<ApiResponse<null>> {
    return api.delete(`/admin/seo/posts/${postId}`)
  }
}
