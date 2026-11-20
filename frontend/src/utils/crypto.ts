/**
 * 共享加密/编码工具函数
 */

/**
 * UTF-8 安全的 base64：`btoa` 只吃 Latin1 字符，中文/emoji 必须先过
 * `TextEncoder` 变成字节再逐位转 binary（替代已废弃的 escape/unescape 套路）。
 */
function bytesToBase64(bytes: Uint8Array): string {
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return btoa(binary)
}

function base64ToBytes(base64: string): Uint8Array {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes
}

export function encodeData(data: unknown): string | null {
  if (!data) return null
  try {
    return bytesToBase64(new TextEncoder().encode(JSON.stringify(data)))
  } catch {
    return null
  }
}

export function decodeData<T = unknown>(encoded: string | null): T | null {
  if (!encoded) return null
  try {
    return JSON.parse(new TextDecoder().decode(base64ToBytes(encoded))) as T
  } catch {
    return null
  }
}

export function safeGetItem(
  key: string,
  storage: 'localStorage' | 'sessionStorage' = 'localStorage'
): string | null {
  try {
    return window[storage].getItem(key)
  } catch {
    return null
  }
}

export function safeSetItem(
  key: string,
  value: string,
  storage: 'localStorage' | 'sessionStorage' = 'localStorage'
): void {
  try {
    window[storage].setItem(key, value)
  } catch {
    // storage unavailable
  }
}

export function safeRemoveItem(
  key: string,
  storage: 'localStorage' | 'sessionStorage' = 'localStorage'
): void {
  try {
    window[storage].removeItem(key)
  } catch {
    // storage unavailable
  }
}
