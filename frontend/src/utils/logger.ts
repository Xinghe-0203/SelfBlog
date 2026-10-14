/**
 * 前端日志系统
 * 提供统一的日志记录方法，支持发送到后端
 */

type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'

interface LogEntry {
  id: string
  timestamp: string
  level: LogLevel
  module: string
  message: string
  data?: unknown
  url: string
  userAgent: string
}

interface Logger {
  debug: (message: string, data?: unknown) => void
  info: (message: string, data?: unknown) => void
  warn: (message: string, data?: unknown) => void
  error: (message: string, data?: unknown) => void
}

// 日志级别
const LOG_LEVELS: Record<LogLevel, number> = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3
}

// 当前日志级别（可通过环境变量配置）
const currentLevel: number = import.meta.env.DEV ? LOG_LEVELS.DEBUG : LOG_LEVELS.INFO

let isInitialized = false

// 日志队列（用于批量发送到后端）
let logQueue: LogEntry[] = []
let flushTimer: ReturnType<typeof setInterval> | null = null
let isDestroyed = false

/**
 * 生成唯一ID
 */
function generateId(): string {
  return Math.random().toString(36).substring(2, 15)
}

/**
 * 格式化日志数据
 */
function formatLogData(level: LogLevel, module: string, message: string, data?: unknown): LogEntry {
  return {
    id: generateId(),
    timestamp: new Date().toISOString(),
    level,
    module,
    message,
    data,
    url: window.location.href,
    userAgent: navigator.userAgent
  }
}

/**
 * 发送日志到后端（批量）
 */
async function sendToBackend(logEntries: LogEntry[]): Promise<void> {
  try {
    await fetch('/api/log', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ logs: logEntries })
    })
  } catch {
    // 静默失败，不影响用户体验
  }
}

/**
 * 批量发送日志
 */
function flushLogs(): void {
  if (isDestroyed || logQueue.length === 0) return

  const logsToSend = [...logQueue]
  logQueue = []

  sendToBackend(logsToSend)
}

/**
 * 记录日志
 */
function log(level: LogLevel, module: string, message: string, data?: unknown): void {
  if (isDestroyed || LOG_LEVELS[level] < currentLevel) return

  const logEntry = formatLogData(level, module, message, data)

  // 控制台输出（开发环境）
  if (import.meta.env.DEV) {
    const style = getConsoleStyle(level)
    console.log(
      `%c[${level}]%c ${module}%c ${message}`,
      style,
      'color: #888;',
      'color: inherit;',
      data || ''
    )
    return
  }

  // 添加到队列（仅生产环境）
  logQueue.push(logEntry)

  // 错误日志立即发送
  if (level === 'ERROR') {
    flushLogs()
  } else if (logQueue.length >= 10) {
    flushLogs()
  }
}

/**
 * 获取控制台样式
 */
function getConsoleStyle(level: LogLevel): string {
  switch (level) {
    case 'DEBUG':
      return 'color: #888; font-weight: normal;'
    case 'INFO':
      return 'color: #2196F3; font-weight: bold;'
    case 'WARN':
      return 'color: #FF9800; font-weight: bold;'
    case 'ERROR':
      return 'color: #F44336; font-weight: bold;'
    default:
      return 'color: inherit;'
  }
}

/**
 * 创建日志器实例
 */
export function createLogger(module: string): Logger {
  return {
    debug: (message: string, data?: unknown) => log('DEBUG', module, message, data),
    info: (message: string, data?: unknown) => log('INFO', module, message, data),
    warn: (message: string, data?: unknown) => log('WARN', module, message, data),
    error: (message: string, data?: unknown) => log('ERROR', module, message, data)
  }
}

/**
 * Hook for Vue组件使用
 */
export function useLogger(module: string): Logger {
  return createLogger(module)
}

/**
 * 性能日志
 */
export function logPerformance(name: string, duration: number): void {
  const level: LogLevel = duration > 3000 ? 'WARN' : duration > 1000 ? 'INFO' : 'DEBUG'
  log(level, 'Performance', `${name}: ${duration}ms`)
}

/**
 * 用户行为日志
 */
export function logUserAction(action: string, data?: unknown): void {
  log('INFO', 'UserAction', action, data)
}

/**
 * API调用日志
 */
export function logApiCall(method: string, url: string, status: number, duration: number): void {
  const level: LogLevel = status >= 400 ? 'ERROR' : 'INFO'
  log(level, 'API', `${method} ${url} - ${status}`, { duration })
}

function handleBeforeUnload(): void {
  if (logQueue.length > 0) {
    const logsToSend = [...logQueue]
    logQueue = []
    try {
      navigator.sendBeacon('/api/log', JSON.stringify({ logs: logsToSend }))
    } catch {
      // sendBeacon 失败时静默处理
    }
  }
}

/**
 * 初始化日志系统
 */
export function initLogger(): void {
  if (isInitialized) return
  isInitialized = true

  // 仅生产环境启动定时刷新和卸载前上报
  if (!import.meta.env.DEV) {
    flushTimer = setInterval(flushLogs, 30000)
    window.addEventListener('beforeunload', handleBeforeUnload)
  }

  // 记录页面加载（开发环境仅控制台输出）
  log('INFO', 'Logger', 'Logger initialized', {
    mode: import.meta.env.MODE,
    level: currentLevel
  })
}

/**
 * 销毁日志系统，清理定时器和事件监听
 */
export function destroyLogger(): void {
  if (flushTimer) {
    clearInterval(flushTimer)
    flushTimer = null
  }
  window.removeEventListener('beforeunload', handleBeforeUnload)
  flushLogs()
  isDestroyed = true
}

/**
 * 重新初始化日志系统（用于 HMR 热更新后恢复）
 */
export function reinitLogger(): void {
  isDestroyed = false
  initLogger()
}

// 自动初始化
if (typeof window !== 'undefined') {
  initLogger()
}

export default {
  createLogger,
  useLogger,
  logPerformance,
  logUserAction,
  logApiCall,
  initLogger,
  destroyLogger
}
