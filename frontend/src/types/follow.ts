/**
 * 关注相关类型定义
 */

import type { ID, PaginationParams } from './common'
import type { User } from './user'

/** 关注信息 */
export interface Follow {
  id: number
  followerId: number
  followingId: number
  createdAt: string
  follower?: User
  following?: User
}

/** 关注计数 */
export interface FollowCounts {
  userId?: number
  followerCount: number
  followingCount: number
}

/** 关注列表参数 */
export interface FollowListParams extends PaginationParams {
  userId: ID
}
