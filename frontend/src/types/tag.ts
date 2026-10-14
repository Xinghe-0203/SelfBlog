/**
 * 标签相关类型定义
 */

import type { PaginationParams } from './common'

/** 标签信息 */
export interface Tag {
  id: number
  name: string
  description?: string
  color?: string
  icon?: string
  postCount?: number
  createdAt?: string
}

/** 创建标签请求 */
export interface CreateTagRequest {
  name: string
  description?: string
  color?: string
}

/** 更新标签请求 */
export type UpdateTagRequest = Partial<CreateTagRequest>

/** 标签搜索参数 */
export interface TagSearchParams extends PaginationParams {
  keyword: string
}
