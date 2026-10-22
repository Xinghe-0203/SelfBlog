/**
 * 类型定义统一导出
 */

// 通用类型
export type {
  ApiResponse,
  PaginationParams,
  PaginatedData,
  PaginatedResponse,
  SortOrder,
  ID,
  Recordable,
  Nullable,
  Optional
} from './common'

// 用户类型
export type {
  UserRole,
  UserStatus,
  Gender,
  UserVO,
  User,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ChangePasswordRequest,
  ResetPasswordRequest,
  UpdateProfileRequest,
  UserSearchParams,
  DeviceInfo
} from './user'

// 文章类型
export type {
  PostStatus,
  PostSortBy,
  Post,
  CreatePostRequest,
  UpdatePostRequest,
  PostListParams,
  PostSearchParams,
  Draft,
  PostMedia
} from './post'

// 评论类型
export type { Comment, CreateCommentRequest, CommentListParams } from './comment'

// 标签类型
export type { Tag, CreateTagRequest, UpdateTagRequest, TagSearchParams } from './tag'

// 消息/通知类型
export type {
  NotificationType,
  Notification,
  Message,
  Conversation,
  SendMessageRequest,
  ReportTargetType,
  Report,
  CreateReportRequest
} from './message'

// 管理后台类型
export type {
  CommunityStats,
  AdminStatistics,
  AdminUserListParams,
  AdminPostListParams,
  HandleReportRequest,
  HandleUserStatusRequest
} from './admin'

// 媒体类型
export type { MediaType, UploadType, Media, UploadProgress, UploadResponse } from './media'

// 热搜类型
export type { TrendingContentType, TrendingContent, TrendingParams } from './trending'

// SEO 自动化类型
export type {
  SeoIssue,
  SeoSource,
  SeoOverview,
  SeoPostRow,
  SeoDetail,
  SeoUpdateRequest,
  SeoBatchResult,
  SeoLlmTest,
  SeoPostListParams,
  SeoBatchRequest
} from './seo'

// AI 能力扩展类型（A 设置 / B 写作 / C 评论审核 / D 校对 / E 图片描述 / F 体检）
export type {
  LlmProvider,
  LlmThinking,
  LlmReasoningEffort,
  LlmModel,
  LlmModelList,
  WritingRequest,
  WritingSummaryVO,
  WritingTagsVO,
  WritingTitlesVO,
  ModerationKind,
  ModerationVerdict,
  ModerationCategory,
  ModerationCheckRequest,
  ModerationCheckVO,
  ModerationScanRequest,
  ModerationScanItem,
  ModerationScanVO,
  ProofreadIssueType,
  ProofreadSeverity,
  ProofreadIssue,
  ProofreadVO,
  ProofreadBatchRequest,
  ProofreadBatchVO,
  ImageAltSource,
  ImageAltRequest,
  ImageAltVO,
  HealthReportRequest,
  HealthFinding,
  HealthReportVO
} from './llm'
