import { describe, it, expect } from 'vitest'
import { encodeData, decodeData, safeGetItem, safeSetItem, safeRemoveItem } from '../crypto'

describe('crypto utils', () => {
  describe('encodeData', () => {
    it('should encode data to base64', () => {
      const data = { name: 'test', value: 123 }
      const encoded = encodeData(data)
      expect(typeof encoded).toBe('string')
      expect(encoded).not.toBe(data)
    })

    it('should return null for null input', () => {
      expect(encodeData(null)).toBeNull()
    })

    it('should return null for undefined input', () => {
      expect(encodeData(undefined)).toBeNull()
    })
  })

  describe('decodeData', () => {
    it('should decode base64 to original data', () => {
      const data = { name: 'test', value: 123 }
      const encoded = encodeData(data)
      const decoded = decodeData(encoded)
      expect(decoded).toEqual(data)
    })

    it('should round-trip non-ASCII payloads (UTF-8 safe)', () => {
      const data = { nickname: '张三', bio: '表情 🎉 与中文混排' }
      const encoded = encodeData(data)
      expect(decodeData(encoded)).toEqual(data)
    })

    it('should encode non-ASCII input as UTF-8 bytes, not lone surrogates', () => {
      // encodeData 先序列化成 JSON（带引号）再转字节：「中」→ '"中"' → 22 E4 B8 AD 22。
      // 旧 escape/unescape 实现与新实现必须逐字节一致，否则旧版本写入 localStorage 的会话数据会解不开。
      const bytes = new TextEncoder().encode(JSON.stringify('中'))
      expect(encodeData('中')).toBe(btoa(String.fromCharCode(...bytes)))
    })

    it('should return null for null input', () => {
      expect(decodeData(null)).toBeNull()
    })

    it('should return null for invalid base64', () => {
      expect(decodeData('invalid-base64')).toBeNull()
    })
  })

  describe('safeGetItem', () => {
    it('should get item from localStorage', () => {
      localStorage.setItem('test-key', 'test-value')
      expect(safeGetItem('test-key')).toBe('test-value')
    })

    it('should return null for non-existent key', () => {
      expect(safeGetItem('non-existent')).toBeNull()
    })
  })

  describe('safeSetItem', () => {
    it('should set item in localStorage', () => {
      safeSetItem('test-key', 'test-value')
      expect(localStorage.getItem('test-key')).toBe('test-value')
    })
  })

  describe('safeRemoveItem', () => {
    it('should remove item from localStorage', () => {
      localStorage.setItem('test-key', 'test-value')
      safeRemoveItem('test-key')
      expect(localStorage.getItem('test-key')).toBeNull()
    })
  })
})
