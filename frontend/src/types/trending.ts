/**
 * 热搜/热门相关类型定义
 */

import type { PaginationParams } from './common'

/** 热门内容类型 */
export type TrendingContentType = 'post' | 'circle'

/** 热门内容 */
export interface TrendingContent {
  type: TrendingContentType
  id: number
  title: string
  content?: string
  heat: number
  createdAt: string
}

/** 热搜列表参数 */
export type TrendingParams = PaginationParams
