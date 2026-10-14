import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  formatRelativeTime,
  formatDate,
  formatNumber,
  truncateText,
  getSafeImageUrl,
  debounce,
  checkPasswordStrength
} from '../index'

describe('formatRelativeTime', () => {
  it('should return empty string for null/undefined', () => {
    expect(formatRelativeTime(null)).toBe('')
    expect(formatRelativeTime(undefined)).toBe('')
  })

  it('should return empty string for invalid date', () => {
    expect(formatRelativeTime('invalid')).toBe('')
  })

  it('should return "刚刚" for recent time', () => {
    const now = new Date()
    expect(formatRelativeTime(now)).toBe('刚刚')
  })

  it('should return minutes ago', () => {
    const date = new Date(Date.now() - 5 * 60 * 1000)
    expect(formatRelativeTime(date)).toBe('5分钟前')
  })

  it('should return hours ago', () => {
    const date = new Date(Date.now() - 3 * 60 * 60 * 1000)
    expect(formatRelativeTime(date)).toBe('3小时前')
  })

  it('should return days ago', () => {
    const date = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000)
    expect(formatRelativeTime(date)).toBe('5天前')
  })
})

describe('formatDate', () => {
  it('should return empty string for null/undefined', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate(undefined)).toBe('')
  })

  it('should format date with default format', () => {
    const date = new Date(2024, 0, 15, 10, 30, 45) // Jan 15, 2024 10:30:45
    const result = formatDate(date)
    expect(result).toContain('2024')
    expect(result).toContain('01')
    expect(result).toContain('15')
  })

  it('should format date with custom format', () => {
    const date = new Date(2024, 0, 15, 10, 30, 45)
    const result = formatDate(date, 'YYYY/MM/DD')
    expect(result).toBe('2024/01/15')
  })
})

describe('formatNumber', () => {
  it('should return "0" for null/undefined', () => {
    expect(formatNumber(null)).toBe('0')
    expect(formatNumber(undefined)).toBe('0')
  })

  it('should format small numbers', () => {
    expect(formatNumber(999)).toBe('999')
  })

  it('should format thousands as k', () => {
    expect(formatNumber(1500)).toBe('1.5k')
  })

  it('should format ten thousands as 万', () => {
    expect(formatNumber(15000)).toBe('1.5万')
  })

  it('should format hundred millions as 亿', () => {
    expect(formatNumber(150000000)).toBe('1.5亿')
  })
})

describe('truncateText', () => {
  it('should return empty string for null/undefined', () => {
    expect(truncateText(null)).toBe('')
    expect(truncateText(undefined)).toBe('')
  })

  it('should return original text if shorter than max', () => {
    expect(truncateText('hello', 10)).toBe('hello')
  })

  it('should truncate text exceeding max length', () => {
    expect(truncateText('hello world', 5)).toBe('hello...')
  })
})

describe('getSafeImageUrl', () => {
  it('should return fallback for null/undefined', () => {
    expect(getSafeImageUrl(null)).toBe('/default-image.png')
    expect(getSafeImageUrl(undefined)).toBe('/default-image.png')
  })

  it('should return relative URL as-is', () => {
    expect(getSafeImageUrl('/uploads/image.jpg')).toBe('/uploads/image.jpg')
  })

  it('should return valid HTTP URL as-is', () => {
    expect(getSafeImageUrl('https://example.com/image.jpg')).toBe('https://example.com/image.jpg')
  })

  it('should return fallback for invalid URL', () => {
    expect(getSafeImageUrl('not-a-url')).toBe('/default-image.png')
  })
})

describe('debounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should delay function execution', () => {
    const fn = vi.fn()
    const debounced = debounce(fn, 300)

    debounced()
    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(300)
    expect(fn).toHaveBeenCalledOnce()
  })

  it('should reset timer on subsequent calls', () => {
    const fn = vi.fn()
    const debounced = debounce(fn, 300)

    debounced()
    vi.advanceTimersByTime(200)
    debounced()
    vi.advanceTimersByTime(200)
    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledOnce()
  })

  it('should support cancel', () => {
    const fn = vi.fn()
    const debounced = debounce(fn, 300)

    debounced()
    debounced.cancel()
    vi.advanceTimersByTime(300)
    expect(fn).not.toHaveBeenCalled()
  })
})

describe('checkPasswordStrength', () => {
  it('should return "none" for null/undefined', () => {
    expect(checkPasswordStrength(null).level).toBe('none')
    expect(checkPasswordStrength(undefined).level).toBe('none')
  })

  it('should return "weak" for simple password', () => {
    expect(checkPasswordStrength('12345678').level).toBe('weak')
  })

  it('should return "medium" for moderate password', () => {
    expect(checkPasswordStrength('Password1').level).toBe('medium')
  })

  it('should return "strong" for complex password', () => {
    expect(checkPasswordStrength('P@ssw0rd!2024').level).toBe('strong')
  })
})
