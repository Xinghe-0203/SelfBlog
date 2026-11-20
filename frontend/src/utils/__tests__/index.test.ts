import { describe, it, expect } from 'vitest'
import { formatDate, truncateText, getSafeImageUrl, checkPasswordStrength } from '../index'

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
  it('should return "empty" for null/undefined/blank input', () => {
    expect(checkPasswordStrength(null).level).toBe('empty')
    expect(checkPasswordStrength(undefined).level).toBe('empty')
    expect(checkPasswordStrength('').level).toBe('empty')
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

  it('should only report the level — colors live in component CSS, not here', () => {
    expect(Object.keys(checkPasswordStrength('P@ssw0rd!2024'))).toEqual(['level'])
  })
})
