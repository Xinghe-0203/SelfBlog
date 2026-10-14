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
