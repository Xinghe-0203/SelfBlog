/**
 * 文章相关类型定义
 */

import type { ID, PaginationParams } from './common'
import type { Tag } from './tag'

/** 文章状态 */
export type PostStatus = 0 | 1 | 2 // 0=草稿, 1=已发布, 2=已下架

/** 文章排序方式 */
export type PostSortBy = 'latest' | 'hottest' | 'recommended'

/**
 * 文章基础信息 —— 字段以后端 PostListResponse / PostDetailResponse 为准：
 * 作者信息是 userId/username/nickname/avatar，时间是 createTime/updateTime，
 * 封面是 coverImage。后端没有 author* / createdAt / updatedAt / timeAgo / isLiked
 * 等字段，别给它们留位置。
 */
export interface Post {
  id: number
  title: string
  content: string
  summary?: string
  coverImage?: string
  /**
   * 遗留别名（后端不下发）：views/Home.vue 仍把它当封面兜底读取。
   * 那份兜底删掉后，这个字段一并删除。
   */
  coverUrl?: string
  category?: string
  userId?: number
  avatar?: string
  nickname?: string
  username?: string
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  /** 后端 PostListResponse 确实下发 shareCount */
  shareCount: number
  status: PostStatus
  tags?: Tag[]
  tagNames?: string[]
  tagIds?: number[]
  /**
   * 遗留别名（后端不下发，一律读 `createTime`）：views/Home.vue 仍兜底读取。
   * 那份兜底删掉后，这个字段一并删除。
   */
  createdAt?: string
  createTime?: string
  updateTime?: string
}

/** 归档条目：公开归档接口返回的精简文章行，按创建时间倒序 */
export interface PostArchiveItem {
  id: number
  title: string
  category: string
  createTime: string
  updateTime: string
  viewCount: number
}

/** 相关阅读条目：公开相关接口返回的精简文章行，按相关度排序（最多 5 条） */
export interface RelatedPostItem {
  id: number
  title: string
  summary: string
  coverUrl: string
  createTime: string
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
