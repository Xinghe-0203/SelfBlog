/**
 * 应用常量定义
 */

/** 存储键前缀 */
export const STORAGE_KEY_PREFIX = 'edu_'

/** API 超时时间 (ms) */
export const API_TIMEOUT = 15000
export const UPLOAD_TIMEOUT = 300000

/** 分页默认值 */
export const DEFAULT_PAGE_SIZE = 10
export const DEFAULT_PAGE_NUM = 1

/** Token 相关 */
export const TOKEN_EXPIRY_BUFFER = 5 * 60 * 1000 // 5分钟

/** 重试配置 */
export const MAX_RETRIES = 2
export const RETRY_DELAY = 1000

/** Toast 持续时间 */
export const TOAST_DURATION = 3000
export const TOAST_ERROR_DURATION = 5000

/** 缓存时间 (ms) */
export const CACHE_TIME = {
  SHORT: 5 * 60 * 1000,      // 5分钟
  MEDIUM: 30 * 60 * 1000,    // 30分钟
  LONG: 24 * 60 * 60 * 1000  // 24小时
} as const

/** 文件上传限制 */
export const FILE_LIMITS = {
  MAX_IMAGE_SIZE: 10 * 1024 * 1024,   // 10MB
  MAX_VIDEO_SIZE: 100 * 1024 * 1024,  // 100MB
  MAX_FILES: 10,
  ALLOWED_IMAGE_TYPES: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
  ALLOWED_VIDEO_TYPES: ['video/mp4', 'video/webm']
} as const

/** 内容限制 */
export const CONTENT_LIMITS = {
  MAX_TITLE_LENGTH: 100,
  MAX_CONTENT_LENGTH: 50000,
  MAX_COMMENT_LENGTH: 1000,
  MAX_BIO_LENGTH: 200,
  MAX_SIGNATURE_LENGTH: 100
} as const

/** HTTP 状态码 */
export const HTTP_STATUS = {
  OK: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_ERROR: 500,
  TOO_MANY_REQUESTS: 429
} as const

/** 可重试的错误码 */
export const RETRYABLE_CODES = [
  'ECONNABORTED',
  'ERR_NETWORK',
  'ERR_CONNECTION_REFUSED',
  'ERR_CONNECTION_RESET'
] as const
