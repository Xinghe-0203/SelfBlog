/**
 * 共享加密/编码工具函数
 */

export function encodeData(data: unknown): string | null {
  if (!data) return null
  try {
    return btoa(unescape(encodeURIComponent(JSON.stringify(data))))
  } catch {
    return null
  }
}

export function decodeData<T = unknown>(encoded: string | null): T | null {
  if (!encoded) return null
  try {
    return JSON.parse(decodeURIComponent(escape(atob(encoded)))) as T
  } catch {
    return null
  }
}

export function safeGetItem(key: string, storage: 'localStorage' | 'sessionStorage' = 'localStorage'): string | null {
  try {
    return window[storage].getItem(key)
  } catch {
    return null
  }
}

export function safeSetItem(key: string, value: string): void {
  try {
    localStorage.setItem(key, value)
  } catch {
    // storage unavailable
  }
}

export function safeRemoveItem(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch {
    // storage unavailable
  }
}
