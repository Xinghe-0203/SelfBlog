/**
 * 管理后台相关类型定义
 */

import type { PaginationParams } from './common'

/** 文章列表参数 */
export interface AdminPostListParams extends PaginationParams {
  status?: string
  keyword?: string
}
