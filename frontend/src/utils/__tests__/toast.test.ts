import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { addToast, removeToast, pauseToast, resumeToast, toast, toasts } from '../toast'

describe('Toast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    toasts.value = []
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should add a toast', () => {
    const id = addToast('Test message', 'info', 3000)
    expect(id).toBeGreaterThan(0)
    expect(toasts.value.length).toBe(1)
    expect(toasts.value[0]!.message).toBe('Test message')
    expect(toasts.value[0]!.type).toBe('info')
  })

  it('should remove a toast by id', () => {
    const id = addToast('Test', 'info', 0) // duration 0 = no auto-remove
    expect(toasts.value.length).toBe(1)
    removeToast(id)
    expect(toasts.value.length).toBe(0)
  })

  it('should auto-remove toast after duration', () => {
    addToast('Auto remove', 'info', 1000)
    expect(toasts.value.length).toBe(1)

    vi.advanceTimersByTime(1000)
    expect(toasts.value.length).toBe(0)
  })

  it('should not auto-remove toast with duration 0', () => {
    addToast('Persistent', 'info', 0)
    expect(toasts.value.length).toBe(1)

    vi.advanceTimersByTime(10000)
    expect(toasts.value.length).toBe(1)
  })

  it('should pause and resume toast', () => {
    const id = addToast('Pausable', 'info', 2000)
    expect(toasts.value.length).toBe(1)

    vi.advanceTimersByTime(500)
    pauseToast(id)
    expect(toasts.value[0]!.paused).toBe(true)

    vi.advanceTimersByTime(5000) // Should not trigger removal while paused
    expect(toasts.value.length).toBe(1)

    resumeToast(id)
    expect(toasts.value[0]!.paused).toBe(false)

    vi.advanceTimersByTime(1500) // Remaining time
    expect(toasts.value.length).toBe(0)
  })

  it('should create success toast via helper', () => {
    toast.success('Success!')
    expect(toasts.value.length).toBe(1)
    expect(toasts.value[0]!.type).toBe('success')
    expect(toasts.value[0]!.message).toBe('Success!')
  })

  it('should create error toast via helper', () => {
    toast.error('Error!')
    expect(toasts.value[0]!.type).toBe('error')
  })

  it('should create warning toast via helper', () => {
    toast.warning('Warning!')
    expect(toasts.value[0]!.type).toBe('warning')
  })

  it('should create info toast via helper', () => {
    toast.info('Info!')
    expect(toasts.value[0]!.type).toBe('info')
  })

  it('should handle multiple toasts', () => {
    addToast('First', 'info', 0)
    addToast('Second', 'success', 0)
    addToast('Third', 'error', 0)
    expect(toasts.value.length).toBe(3)
  })

  it('should handle removing non-existent toast', () => {
    removeToast(999)
    expect(toasts.value.length).toBe(0)
  })
})
