/**
 * 文章相关类型定义
 */

import type { ID, PaginationParams } from './common'
import type { User } from './user'
import type { Tag } from './tag'

/** 文章状态 */
export type PostStatus = 0 | 1 | 2 // 0=草稿, 1=已发布, 2=已下架

/** 文章排序方式 */
export type PostSortBy = 'latest' | 'hottest' | 'recommended'

/** 文章基础信息 */
export interface Post {
  id: number
  title: string
  content: string
  summary?: string
  coverImage?: string
  coverUrl?: string
  category?: string
  authorId: number
  authorName: string
  authorAvatar?: string
  author?: User
  userId?: number
  avatar?: string
  nickname?: string
  username?: string
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  shareCount: number
  status: PostStatus
  tags?: Tag[]
  tagNames?: string[]
  tagIds?: number[]
  topicIds?: number[]
  topicNames?: string[]
  isLiked?: boolean
  isCollected?: boolean
  isFollowing?: boolean
  timeAgo?: string
  createdAt: string
  createTime?: string
  updatedAt?: string
  updateTime?: string
}

/** 创建文章请求 */
export interface CreatePostRequest {
  title: string
  content: string
  summary?: string
  coverImage?: string
  tagIds?: number[]
  topicId?: number
  status?: PostStatus
}

/** 更新文章请求 */
export type UpdatePostRequest = Partial<CreatePostRequest>

/** 文章列表参数 */
export interface PostListParams extends PaginationParams {
  tagId?: ID
  category?: string
  sortBy?: PostSortBy
  sort?: string
  keyword?: string
  userId?: number
  status?: number
}

/** 文章搜索参数 */
export interface PostSearchParams extends PaginationParams {
  keyword?: string
  tagId?: ID
}

/** 草稿 */
export interface Draft {
  id: number
  draftId?: number
  title: string
  content: string
  summary?: string
  coverImage?: string
  tagIds?: number[]
  tags?: Tag[]
  topicId?: number
  postId?: number
  category?: string
  createTime?: string
  updateTime?: string
  createdAt: string
  updatedAt: string
}

/** 文章媒体 */
export interface PostMedia {
  id: number
  postId: number
  mediaId: number
  mediaType: string
  mediaUrl: string
  sortOrder: number
}
