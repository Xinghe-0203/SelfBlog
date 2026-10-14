/**
 * 话题相关类型定义
 */

import type { PaginationParams } from './common'

/** 话题信息 */
export interface Topic {
  id: number
  name: string
  description?: string
  postCount?: number
  status?: number
  trendingScore?: number
  createTime?: string
  createdAt?: string
  updatedAt?: string
}

/** 创建话题请求 */
export interface CreateTopicRequest {
  name: string
  description?: string
  status?: number
}

/** 更新话题请求 */
export type UpdateTopicRequest = Partial<CreateTopicRequest>

/** 话题列表参数 */
export type TopicListParams = PaginationParams

/** 话题搜索参数 */
export interface TopicSearchParams extends PaginationParams {
  keyword?: string
}
