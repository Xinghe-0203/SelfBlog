/**
 * 统一从被拒绝的错误里挑「给用户看的那句话」。
 *
 * 拦截器（api/index.ts）会为常见 HTTP 状态把后端 message 写到 `error.userMessage`，
 * 但各 catch 点的挑选顺序并不一致（有的读 userMessage，有的直接翻
 * `response.data.message`）。这里按同一优先级收敛：
 * `userMessage` → `response.data.message` → `Error.message` → fallback。
 */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object') {
    const candidate = err as {
      userMessage?: unknown
      response?: { data?: { message?: unknown } }
      message?: unknown
    }
    if (typeof candidate.userMessage === 'string' && candidate.userMessage) {
      return candidate.userMessage
    }
    if (typeof candidate.response?.data?.message === 'string' && candidate.response.data.message) {
      return candidate.response.data.message
    }
    if (typeof candidate.message === 'string' && candidate.message) {
      return candidate.message
    }
  }
  return fallback
}
