/**
 * 后端 VO 对应的前端类型定义
 * 与 backend/vo/ 下的 Java VO 一一对应
 */

import type { UserVO } from './user'

// ============================================================
// Post 模块
// ============================================================

/** 标签（文章/详情共用） */
export interface TagVO {
  id: number
  name: string
}

/** 文章列表返回 - 对应 PostListResponse */
export interface PostListResponse {
  id: number
  userId: number
  nickname?: string
  avatar?: string
  title: string
  summary?: string
  topicId?: number
  topicName?: string
  category?: string
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  shareCount: number
  createTime: string
  tags?: TagVO[]
  coverImage?: string
  username?: string
}

/** 文章详情返回 - 对应 PostDetailResponse */
export interface PostDetailResponse {
  id: number
  userId: number
  username?: string
  nickname?: string
  avatar?: string
  title: string
  summary?: string
  content?: string
  topicId?: number
  topicName?: string
  category?: string
  coverImage?: string
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  status: number
  reviewerId?: number
  reviewTime?: string
  rejectReason?: string
  createTime: string
  updateTime?: string
  tags?: TagVO[]
}

/** 评论响应（支持树形结构） - 对应 CommentVO */
export interface CommentVO {
  id: number
  postId: number
  userId: number
  nickname?: string
  username?: string
  userAvatar?: string
  parentId: number
  replyToNickname?: string
  content: string
  createTime: string
  replies?: CommentVO[]
}

// ============================================================
// Circle 模块
// ============================================================

/** 校友圈动态响应 - 对应 CirclePostVO */
export interface CirclePostVO {
  id: number
  userId: number
  userUsername?: string
  userNickname?: string
  userAvatar?: string
  content: string
  contentType: number
  images?: string[]
  videos?: string[]
  repostPost?: CirclePostVO
  originalPostHidden?: boolean
  location?: string
  topicNames?: string[]
  topicIds?: number[]
  likeCount: number
  commentCount: number
  repostCount: number
  viewCount: number
  isLiked?: boolean
  isReposted?: boolean
  isTop?: boolean
  visibility: number
  allowComment: number
  allowRepost: number
  createTime: string
  timeAgo?: string
}

// ============================================================
// Social 模块
// ============================================================

/** 通知响应 - 对应 NotificationVO */
export interface NotificationVO {
  id: number
  type: string
  title: string
  content: string
  fromUser?: UserVO
  targetType?: string
  targetId?: number
  isRead: number
  createTime: string
  timeAgo?: string
}

/** 会话列表项 - 对应 ConversationVO */
export interface ConversationVO {
  conversationId: number
  user?: UserVO
  lastMessage?: string
  lastMessageTime?: string
  timeAgo?: string
  unreadCount: number
  messageCount: number
}

/** 私信响应 - 对应 MessageVO */
export interface MessageVO {
  id: number
  senderId: number
  receiverId: number
  sender?: UserVO
  receiver?: UserVO
  content: string
  isRead: number
  createTime: string
  timeAgo?: string
}

// ============================================================
// Content 模块
// ============================================================

/** 热门内容（文章+动态统一） - 对应 HotContentVO */
export interface HotContentVO {
  id: number
  title?: string
  content?: string
  userId: number
  username?: string
  nickname?: string
  avatar?: string
  type: number
  likeCount: number
  commentCount: number
  shareCount: number
  viewCount: number
  images?: string[]
  tags?: string[]
  topics?: string[]
  createTime: string
  score?: number
}

/** 媒体文件响应 - 对应 MediaVO */
export interface MediaVO {
  id: number
  fileUrl: string
  thumbUrl?: string
  fileSize: number
  width?: number
  height?: number
}
