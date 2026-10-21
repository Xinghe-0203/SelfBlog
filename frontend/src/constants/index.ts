/**
 * 应用常量定义
 */

/** 存储键前缀 */
export const STORAGE_KEY_PREFIX = 'edu_'

/** 分页默认值 */
export const DEFAULT_PAGE_NUM = 1

/** 重试配置 */
export const MAX_RETRIES = 2
export const RETRY_DELAY = 1000

/** Toast 持续时间 */
export const TOAST_DURATION = 3000
export const TOAST_ERROR_DURATION = 5000

/** 可重试的错误码 */
export const RETRYABLE_CODES = [
  'ECONNABORTED',
  'ERR_NETWORK',
  'ERR_CONNECTION_REFUSED',
  'ERR_CONNECTION_RESET'
] as const
