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

/**
 * 可重试的错误码。
 *
 * `ECONNABORTED` 同时是 axios 的**超时**错误码，它留在这张表里是刻意的：普通读请求
 * 超时值得重试。但「每次调用都真实花钱」的请求（LLM 生成）必须在自己 config 上设
 * `_noRetry: true` 退出重试，否则一次超时会放大成三次上游调用与三倍账单
 * （见 `api/index.ts` 的 `_noRetry` 与 `api/seo.ts`）。
 */
export const RETRYABLE_CODES = [
  'ECONNABORTED',
  'ERR_NETWORK',
  'ERR_CONNECTION_REFUSED',
  'ERR_CONNECTION_RESET'
] as const
