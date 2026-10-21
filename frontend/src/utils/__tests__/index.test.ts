import { describe, it, expect, vi } from 'vitest'
import {
  formatRelativeTime,
  formatDate,
  truncateText,
  getSafeImageUrl,
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

  it('should count real calendar months instead of 30-day buckets', () => {
    vi.useFakeTimers()
    try {
      vi.setSystemTime(new Date(2026, 9, 20))
      // 2025-10-25 距 2026-10-20 是 360 天（还差 5 天才满一年）：旧算法 floor(360/30)=12 会
      // 谎报「1年前」，真实日历差只有 11 个月。
      expect(formatRelativeTime(new Date(2025, 9, 25))).toBe('11个月前')
      // 同年内满 30 天但不足一个完整日历月时，仍退回天数表述。
      vi.setSystemTime(new Date(2026, 2, 31))
      expect(formatRelativeTime(new Date(2026, 2, 1))).toBe('30天前')
    } finally {
      vi.useRealTimers()
    }
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
