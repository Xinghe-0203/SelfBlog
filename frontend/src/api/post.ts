/**
 * 文章接口
 */

import api from './index'
import { cachedGet, invalidateApiCache } from '@/utils/apiCache'
import type {
  ApiResponse,
  PaginatedResponse,
  Post,
  PostArchiveItem,
  RelatedPostItem,
  CreatePostRequest,
  UpdatePostRequest,
  PostListParams,
  PostSearchParams,
  Draft
} from '@/types'

/**
 * 文章列表的缓存键。
 *
 * 参数按**固定顺序**拼接，而不是 JSON.stringify(params)：对象的键序取决于写入顺序，
 * 同一个语义的请求可能因为参数对象构造顺序不同而算出两个键，缓存就失效了。
 */
function listCacheKey(params: PostListParams): string {
  return [
    'post:list',
    params.pageNum ?? 1,
    params.pageSize ?? 10,
    params.sort ?? '',
    params.sortBy ?? '',
    params.tagId ?? '',
    params.category ?? '',
    params.keyword ?? '',
    params.userId ?? '',
    params.status ?? ''
  ].join('|')
}

/** 公开只读列表的缓存时长（毫秒）。取短到「作者刚发的一篇文章不会被藏太久」。 */
const LIST_CACHE_TTL = 30_000
/** 归档与标签的缓存时长：这两份数据变动极少，可以多留一会儿。 */
const ARCHIVE_CACHE_TTL = 120_000

export const postApi = {
  /**
   * 创建文章
   *
   * 显式写出响应泛型：`.finally()` 会丢掉调用点的上下文类型，
   * axios 的 `post<T, R>` 里 R 就回落到默认的 AxiosResponse<T>，与本函数的返回类型不符。
   */
  createPost(data: CreatePostRequest): Promise<ApiResponse<Post>> {
    return api
      .post<unknown, ApiResponse<Post>>('/post', data)
      .finally(() => invalidateApiCache('post:'))
  },

  /**
   * 更新文章
   */
  updatePost(id: number | string, data: UpdatePostRequest): Promise<ApiResponse<Post>> {
    return api
      .put<unknown, ApiResponse<Post>>(`/post/${id}`, data)
      .finally(() => invalidateApiCache('post:'))
  },

  /**
   * 删除文章
   */
  deletePost(id: number | string): Promise<ApiResponse> {
    return api
      .delete<unknown, ApiResponse>(`/post/${id}`)
      .finally(() => invalidateApiCache('post:'))
  },

  /**
   * 获取文章详情
   */
  getPostById(id: number | string): Promise<ApiResponse<Post>> {
    return api.get(`/post/${id}`)
  },

  /**
   * 获取相邻文章（上一篇 / 下一篇），任一方向没有文章时对应值为 null
   */
  getAdjacentPosts(id: number | string): Promise<
    ApiResponse<{
      prev: { id: number; title: string } | null
      next: { id: number; title: string } | null
    }>
  > {
    return api.get(`/post/${id}/adjacent`)
  },

  /**
   * 获取文章列表
   *
   * 公开只读接口，走 30 秒的短期内存缓存：读者在文章页与首页之间来回切换时
   * 不必每次都等一次网络往返（见 utils/apiCache 的边界说明）。
   */
  getPostList(params: PostListParams): Promise<PaginatedResponse<Post>> {
    return cachedGet(listCacheKey(params), LIST_CACHE_TTL, () => api.get('/post/list', { params }))
  },

  /**
   * 获取全站文章归档（精简字段，按创建时间倒序）
   */
  getArchives(): Promise<ApiResponse<PostArchiveItem[]>> {
    return cachedGet('post:archives', ARCHIVE_CACHE_TTL, () => api.get('/post/archives'))
  },

  /**
   * 获取相关阅读（最多 5 条，按相关度排序；无相关文章时为空数组）
   */
  getRelated(postId: number): Promise<ApiResponse<RelatedPostItem[]>> {
    return api.get(`/post/${postId}/related`)
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
   * 搜索关键词联想（公开接口；空白/过短关键词返回空数组）。
   * 普通读请求：默认超时与网络重试，绝不加 _noRetry。
   */
  getSearchSuggestions(keyword: string): Promise<ApiResponse<string[]>> {
    return api.get('/post/search/suggest', { params: { keyword } })
  }
}
