/**
 * Axios 实例配置与拦截器
 */

import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/stores/user'
import { useLogger } from '@/utils/logger'
import { encodeData, decodeData, safeGetItem, safeSetItem, safeRemoveItem } from '@/utils/crypto'
import { STORAGE_KEY_PREFIX, MAX_RETRIES, RETRY_DELAY, RETRYABLE_CODES } from '@/constants'
import type { ApiResponse } from '@/types'
import { getVisitorId } from '@/utils/visitor'

const logger = useLogger('API')

function getToken(key: string): string | null {
  return decodeData(safeGetItem(key)) || decodeData(safeGetItem(key, 'sessionStorage'))
}

// 生成traceId
function generateTraceId(): string {
  return (
    crypto.randomUUID?.()?.replace(/-/g, '') ||
    Array.from(crypto.getRandomValues(new Uint8Array(16)))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('')
  )
}

// 存储Token指纹信息（用于检测Token是否被窃取）
function getTokenFingerprint(token: string): string | null {
  try {
    const hash = token.substring(0, 20) + token.substring(token.length - 20)
    return btoa(hash)
  } catch {
    return null
  }
}

// 检查Token指纹是否匹配（防止Token被窃取后在其他域名使用）
function verifyTokenFingerprint(token: string): boolean {
  try {
    const savedFingerprint =
      safeGetItem(STORAGE_KEY_PREFIX + 'token_fingerprint') ||
      safeGetItem(STORAGE_KEY_PREFIX + 'token_fingerprint', 'sessionStorage')
    if (!savedFingerprint) {
      const fingerprint = getTokenFingerprint(token)
      if (fingerprint) {
        safeSetItem(STORAGE_KEY_PREFIX + 'token_fingerprint', fingerprint)
      }
      return true
    }
    const currentFingerprint = getTokenFingerprint(token)
    return currentFingerprint === savedFingerprint
  } catch {
    return true
  }
}

// XSRF Token管理（防止CSRF攻击）
function getXsrfToken(): string | null {
  return safeGetItem(STORAGE_KEY_PREFIX + 'xsrf_token')
}

function generateXsrfToken(): string | null {
  try {
    const array = new Uint8Array(32)
    crypto.getRandomValues(array)
    const token = Array.from(array)
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('')
    safeSetItem(STORAGE_KEY_PREFIX + 'xsrf_token', token)
    return token
  } catch {
    return null
  }
}

// 清除安全相关存储
function clearSecurityStorage(): void {
  safeRemoveItem(STORAGE_KEY_PREFIX + 'token_fingerprint')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'xsrf_token')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'remember')
}

// 请求队列（用于Token刷新时暂存请求）
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

// 请求重试配置
function isRetryableError(error: { code?: string; response?: unknown; message?: string }): boolean {
  if (error.code === 'ERR_CANCELED') return false
  return (
    !error.response &&
    ((error.code && RETRYABLE_CODES.includes(error.code as (typeof RETRYABLE_CODES)[number])) ||
      error.message === 'Network Error')
  )
}

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

const processQueue = (error: unknown, token: string | null = null): void => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token!)
    }
  })
  failedQueue = []
}

// 请求拦截器
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 优先从 store 读取 token，保持与 reactive state 同步
    let token: string | null = null
    try {
      const userStore = useUserStore()
      token = userStore.token
    } catch {
      // store 未初始化时回退到 storage
      token = getToken(STORAGE_KEY_PREFIX + 'token')
    }

    // 安全检查：验证Token指纹防止被窃取后在其他域名使用
    if (token && !verifyTokenFingerprint(token)) {
      logger.error('Token fingerprint mismatch - possible theft attempt')
      try {
        const userStore = useUserStore()
        userStore.resetState()
      } catch {
        // store not available
      }
      throw new Error('Token security violation')
    }

    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加XSRF token
    const xsrfToken = getXsrfToken() || generateXsrfToken()
    if (xsrfToken) {
      config.headers['X-XSRF-TOKEN'] = xsrfToken
    }

    config.headers['X-Visitor-Id'] = getVisitorId()

    // 添加traceId用于日志追踪
    const traceId = generateTraceId()
    config.headers['X-Trace-Id'] = traceId

    logger.debug('Request', {
      method: config.method?.toUpperCase(),
      url: config.url,
      traceId
    })

    return config
  },
  (error) => {
    logger.error('Request error', { error: (error as Error).message })
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    logger.debug('Response success', {
      url: response.config.url,
      status: response.status
    })
    return response.data
  },
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
      _networkRetryCount?: number
    }

    logger.error('Response error', {
      url: originalRequest?.url,
      status: error.response?.status,
      message: error.response?.data?.message || error.message
    })

    // 网络错误重试（无响应、超时等）
    if (isRetryableError(error)) {
      originalRequest._networkRetryCount = (originalRequest._networkRetryCount || 0) + 1
      if (originalRequest._networkRetryCount <= MAX_RETRIES) {
        logger.warn('Retrying request', {
          url: originalRequest.url,
          attempt: originalRequest._networkRetryCount,
          maxRetries: MAX_RETRIES
        })
        await new Promise((resolve) =>
          setTimeout(resolve, RETRY_DELAY * originalRequest._networkRetryCount!)
        )
        return api(originalRequest)
      }
    }

    // 400 - 请求参数错误
    if (error.response?.status === 400) {
      const message = error.response?.data?.message || '请求参数错误'
      logger.warn('Bad request', { url: originalRequest?.url, message })
      error.userMessage = message
      return Promise.reject(error)
    }

    // 429 - 请求过于频繁
    if (error.response?.status === 429) {
      const message = error.response?.data?.message || '操作过于频繁，请稍后再试'
      logger.warn('Rate limited', { url: originalRequest?.url, message })
      error.userMessage = message
      return Promise.reject(error)
    }

    // 403 - 无权限
    if (error.response?.status === 403) {
      const message = error.response?.data?.message || '没有访问权限'
      logger.warn('Forbidden', { url: originalRequest?.url, message })
      error.userMessage = message
      return Promise.reject(error)
    }

    // 404 - 资源不存在
    if (error.response?.status === 404) {
      const message = error.response?.data?.message || '请求的资源不存在'
      logger.warn('Not found', { url: originalRequest?.url, message })
      error.userMessage = message
      return Promise.reject(error)
    }

    // 500 - 服务器错误
    if (error.response?.status === 500) {
      const message = error.response?.data?.message || '服务器内部错误，请稍后再试'
      logger.error('Server error', { url: originalRequest?.url, message })
      error.userMessage = message
      return Promise.reject(error)
    }

    // 无响应的网络错误
    if (
      !error.response &&
      originalRequest._networkRetryCount &&
      originalRequest._networkRetryCount > MAX_RETRIES
    ) {
      const message = error.message || '网络异常，请检查网络连接'
      logger.error('Network error after retries exhausted', {
        url: originalRequest?.url,
        message,
        retries: originalRequest._networkRetryCount
      })
      error.userMessage = message
      return Promise.reject(error)
    }

    // 兜底: 未知错误
    if (error.response?.data?.message) {
      error.userMessage = error.response.data.message
    } else if (!error.userMessage) {
      error.userMessage = error.message || '请求失败'
    }

    // 401错误处理（Token过期）
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 如果是刷新Token的请求失败，直接跳转登录
      if (originalRequest.url === '/user/refresh') {
        await handleAuthError()
        return Promise.reject(error)
      }

      // 如果正在刷新Token，将请求加入队列
      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 优先从 store 读取 refreshToken
        let refreshTokenValue: string | null = null
        try {
          const userStore = useUserStore()
          refreshTokenValue = userStore.refreshToken
        } catch {
          refreshTokenValue = getToken(STORAGE_KEY_PREFIX + 'refreshToken')
        }
        if (!refreshTokenValue) {
          throw new Error('No refresh token')
        }

        const response = await api.post('/user/refresh', null, {
          headers: { Authorization: `Bearer ${refreshTokenValue}` }
        })

        const refreshData = (response as unknown as ApiResponse<{ token: string; refreshToken: string }>)?.data
        if (!refreshData?.token || !refreshData?.refreshToken) {
          throw new Error('Invalid refresh response')
        }
        const { token: newToken, refreshToken: newRefreshToken } = refreshData

        const userStore = useUserStore()
        const storage = userStore.remember ? localStorage : sessionStorage
        try {
          storage.setItem(STORAGE_KEY_PREFIX + 'token', encodeData(newToken)!)
          storage.setItem(STORAGE_KEY_PREFIX + 'refreshToken', encodeData(newRefreshToken)!)
        } catch {
          // storage unavailable
        }
        userStore.updateTokens(newToken, newRefreshToken)

        const fingerprint = getTokenFingerprint(newToken)
        if (fingerprint) {
          safeSetItem(STORAGE_KEY_PREFIX + 'token_fingerprint', fingerprint)
        }

        processQueue(null, newToken)

        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        await handleAuthError()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

// 处理认证错误
async function handleAuthError(): Promise<void> {
  safeRemoveItem(STORAGE_KEY_PREFIX + 'token')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'refreshToken')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'user')
  try {
    sessionStorage.removeItem(STORAGE_KEY_PREFIX + 'token')
  } catch {
    // ignore
  }
  try {
    sessionStorage.removeItem(STORAGE_KEY_PREFIX + 'refreshToken')
  } catch {
    // ignore
  }
  try {
    sessionStorage.removeItem(STORAGE_KEY_PREFIX + 'user')
  } catch {
    // ignore
  }
  clearSecurityStorage()

  try {
    const userStore = useUserStore()
    userStore.resetState()
  } catch {
    // store may not be available during initialization
  }

  const adminRoot = `/${(await import('@/stores/site')).useSiteStore().adminPath}`
  if (window.location.pathname !== adminRoot) {
    try {
      const { default: router } = await import('@/router')
      const redirectPath = window.location.pathname + window.location.search
      router.push({ path: adminRoot, query: { redirect: redirectPath !== adminRoot ? redirectPath : undefined } })
    } catch {
      window.location.href = adminRoot
    }
  }
}

export { getToken }
export default api
