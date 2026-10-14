/**
 * 管理后台相关类型定义
 */

import type { PaginationParams } from './common'

/** 社区统计 */
export interface CommunityStats {
  totalUsers: number
  totalPosts: number
  totalComments: number
  totalCirclePosts: number
  todayNewUsers: number
  todayNewPosts: number
  onlineUsers: number
  userCount?: number
  postCount?: number
  commentCount?: number
  tagCount?: number
}

/** 用户增长趋势 */
export interface GrowthTrendItem {
  date: string
  count: number
}

/** 用户统计详情 */
export interface UserStats {
  totalUsers: number
  todayNewUsers: number
  activeUsers: number
}

/** 文章统计详情 */
export interface PostStats {
  totalPosts: number
  todayNewPosts: number
  totalComments: number
}

/** 校友圈统计详情 */
export interface CircleStats {
  totalCirclePosts: number
  todayNewCirclePosts: number
}

/** 管理后台统计 */
export interface AdminStatistics {
  userCount: number
  postCount: number
  commentCount: number
  circlePostCount: number
  tagCount: number
  topicCount: number
  reportCount: number
  pendingReportCount: number
  dailyActiveUsers: number
  weeklyActiveUsers: number
  monthlyActiveUsers: number
  userStats?: UserStats
  postStats?: PostStats
  circleStats?: CircleStats
  userGrowthTrend?: GrowthTrendItem[]
  postGrowthTrend?: GrowthTrendItem[]
}

/** 用户列表参数 */
export interface AdminUserListParams extends PaginationParams {
  keyword?: string
  status?: number
  role?: string
}

/** 文章列表参数 */
export interface AdminPostListParams extends PaginationParams {
  status?: string
  keyword?: string
}

/** 举报处理请求 */
export interface HandleReportRequest {
  status: 'approved' | 'rejected'
  handleResult: string
}

/** 用户状态处理请求 */
export interface HandleUserStatusRequest {
  status: number
  reason?: string
}

/** 校友圈列表参数 */
export interface AdminCircleListParams extends PaginationParams {
  keyword?: string
  status?: number
}
