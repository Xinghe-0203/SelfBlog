/**
 * 校友圈相关类型定义
 */

import type { ID, PaginationParams } from './common'
import type { User } from './user'

/** 动态类型 */
export type CirclePostType = 'text' | 'image' | 'video'

/** 动态信息 */
export interface CirclePost {
  id: number
  userId: number
  content: string
  contentType?: string
  images?: string[]
  imageUrls?: string[]
  videoUrl?: string
  videoUrls?: string[]
  type: CirclePostType
  viewCount: number
  likeCount: number
  commentCount: number
  repostCount: number
  isLiked?: boolean
  isCollected?: boolean
  isTop?: boolean
  visibility?: string
  allowComment?: boolean
  allowRepost?: boolean
  status?: number
  location?: string
  tags?: string[]
  topicIds?: number[]
  topicNames?: string[]
  mentions?: number[]
  videos?: string[]
  repostId?: number
  repostUserId?: number
  repostContent?: string
  repostPost?: CirclePost
  originalPostHidden?: boolean
  likeAnim?: boolean
  _likeLoading?: boolean
  user?: User
  nickname?: string
  username?: string
  avatar?: string
  userNickname?: string
  userUsername?: string
  userAvatar?: string
  timeAgo?: string
  createdAt: string
  createTime?: string
  updatedAt?: string
  originalPost?: CirclePost // 转发的原动态
}

/** 创建动态请求 */
export interface CreateCirclePostRequest {
  content: string
  images?: string[]
  videos?: string[]
  videoUrl?: string
  type?: CirclePostType
  visibility?: number
  location?: string
  topicIds?: number[] | null
  allowComment?: number
  allowRepost?: number
}

/** 更新动态请求 */
export type UpdateCirclePostRequest = Partial<CreateCirclePostRequest>

/** 动态评论 */
export interface CircleComment {
  id: number
  postId: number
  userId: number
  content: string
  parentId?: number
  replyToUserId?: number
  likeCount?: number
  user?: User
  nickname?: string
  username?: string
  avatar?: string
  userNickname?: string
  userUsername?: string
  userAvatar?: string
  timeAgo?: string
  replies?: CircleComment[]
  createdAt: string
  createTime?: string
  updatedAt?: string
}

/** 创建动态评论请求 */
export interface CreateCircleCommentRequest {
  postId: ID
  content: string
  parentId?: number | null
  replyToUserId?: number | null
}

/** 动态列表参数 */
export interface CirclePostListParams extends PaginationParams {
  keyword?: string
  tab?: string
  userId?: number
  topicId?: number
}

/** 动态搜索参数 */
export interface CircleSearchParams extends PaginationParams {
  keyword: string
}
