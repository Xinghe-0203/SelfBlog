/**
 * 评论相关类型定义
 */

import type { ID, PaginationParams } from './common'
import type { User } from './user'

/** 评论信息 */
export interface Comment {
  id: number
  postId: number
  userId: number
  content: string
  parentId?: number
  replyToUserId?: number
  replyToUsername?: string
  user?: User
  likeCount: number
  createdAt: string
  children?: Comment[]
}

/** 创建评论请求 */
export interface CreateCommentRequest {
  postId: ID
  content: string
  parentId?: ID
  replyToUserId?: ID
}

/** 评论列表参数 */
export interface CommentListParams extends PaginationParams {
  postId: ID
}
