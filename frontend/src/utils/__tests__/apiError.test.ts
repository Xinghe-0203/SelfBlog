import { describe, it, expect } from 'vitest'
import { apiErrorMessage } from '../apiError'

/**
 * 各 catch 点共用同一套「给用户看的那句话」挑选顺序：
 * `userMessage` → `response.data.message` → `Error.message` → fallback。
 */
describe('apiErrorMessage', () => {
  it('prefers the interceptor-provided userMessage first', () => {
    const err = {
      userMessage: '操作过于频繁，请稍后再试',
      response: { data: { message: '后端原文' } },
      message: 'Request failed'
    }
    expect(apiErrorMessage(err, '兜底文案')).toBe('操作过于频繁，请稍后再试')
  })

  it('falls back to response.data.message when userMessage is missing', () => {
    const err = { response: { data: { message: 'AI 辅助功能已关闭，请先在设置中启用' } } }
    expect(apiErrorMessage(err, '兜底文案')).toBe('AI 辅助功能已关闭，请先在设置中启用')
  })

  it('falls back to Error.message for plain errors', () => {
    expect(apiErrorMessage(new Error('offline'), '兜底文案')).toBe('offline')
  })

  it('returns the fallback for null/undefined/primitive errors and empty strings', () => {
    expect(apiErrorMessage(null, '兜底文案')).toBe('兜底文案')
    expect(apiErrorMessage(undefined, '兜底文案')).toBe('兜底文案')
    expect(apiErrorMessage('boom', '兜底文案')).toBe('兜底文案')
    expect(apiErrorMessage({ userMessage: '', message: '' }, '兜底文案')).toBe('兜底文案')
  })
})
