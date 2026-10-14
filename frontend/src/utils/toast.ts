/**
 * Toast 通知工具
 */

import { ref, reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastAction {
  text: string
  handler: () => void
}

export interface ToastItem {
  id: number
  message: string
  type: ToastType
  duration: number
  remaining: number
  paused: boolean
  action: ToastAction | null
  createdAt: number
  _originalDuration: number
  _activeMs: number
  _progress?: number
}

const toasts = ref<ToastItem[]>([])
let toastId = 0
const timers = new Map<number, ReturnType<typeof setTimeout>>()

export function addToast(
  message: string,
  type: ToastType = 'info',
  duration = 3000,
  action: ToastAction | null = null
): number {
  const id = ++toastId
  const item = reactive<ToastItem>({
    id,
    message,
    type,
    duration,
    remaining: duration,
    paused: false,
    action,
    createdAt: Date.now(),
    _originalDuration: duration,
    _activeMs: 0
  })

  toasts.value.push(item)

  if (duration > 0) {
    const timer = setTimeout(() => removeToast(id), duration)
    timers.set(id, timer)
  }

  return id
}

export function removeToast(id: number): void {
  const timer = timers.get(id)
  if (timer) {
    clearTimeout(timer)
    timers.delete(id)
  }
  const index = toasts.value.findIndex((t) => t.id === id)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

export function pauseToast(id: number): void {
  const item = toasts.value.find((t) => t.id === id)
  if (!item || item.paused) return
  item.paused = true
  const timer = timers.get(id)
  if (timer) {
    clearTimeout(timer)
    timers.delete(id)
  }
  const elapsed = Date.now() - item.createdAt
  item._activeMs += elapsed
  item.remaining = Math.max(0, item.remaining - elapsed)
}

export function resumeToast(id: number): void {
  const item = toasts.value.find((t) => t.id === id)
  if (!item || !item.paused) return
  item.paused = false
  if (item.remaining <= 0) {
    removeToast(id)
    return
  }
  item.createdAt = Date.now()
  const timer = setTimeout(() => removeToast(id), item.remaining)
  timers.set(id, timer)
}

export const toast = {
  success: (message: string, duration?: number, action?: ToastAction) =>
    addToast(message, 'success', duration, action ?? null),
  error: (message: string, duration?: number, action?: ToastAction) =>
    addToast(message, 'error', duration, action ?? null),
  warning: (message: string, duration?: number, action?: ToastAction) =>
    addToast(message, 'warning', duration, action ?? null),
  info: (message: string, duration?: number, action?: ToastAction) =>
    addToast(message, 'info', duration, action ?? null)
}

export { toasts }
