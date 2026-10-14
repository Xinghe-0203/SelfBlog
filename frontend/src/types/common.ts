/**
 * 通用类型定义
 */

/** API 统一响应格式 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PaginationParams {
  pageNum?: number
  pageSize?: number
}

/** 分页响应数据 */
export interface PaginatedData<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 分页 API 响应 */
export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>

/** 排序方式 */
export type SortOrder = 'asc' | 'desc'

/** 通用 ID 类型 */
export type ID = number | string

/** 通用键值对 */
export type Recordable<T = unknown> = Record<string, T>

/** 可为空 */
export type Nullable<T> = T | null

/** 可选 */
export type Optional<T> = T | undefined
