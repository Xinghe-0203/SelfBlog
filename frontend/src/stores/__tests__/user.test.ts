import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../user'
import type { User } from '@/types'

// Mock API modules
vi.mock('@/api/user', () => ({
  userApi: {
    login: vi.fn(),
    logout: vi.fn(),
    changePassword: vi.fn()
  }
}))

vi.mock('@/utils/logger', () => ({
  useLogger: () => ({
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
    debug: vi.fn()
  })
}))

/** 构造符合真实 `User` 形状的最小用户；测试只覆盖它关心的字段。 */
function makeUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    username: 'test',
    nickname: 'Test',
    avatar: '',
    email: '',
    role: 'user',
    ...overrides
  }
}

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should initialize with default state', () => {
    const store = useAuthStore()
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(store.isAdmin).toBe(false)
  })

  it('should compute isLoggedIn correctly', () => {
    const store = useAuthStore()
    store.token = 'test-token'
    store.user = makeUser()
    expect(store.isLoggedIn).toBe(true)
  })

  it('should compute isAdmin correctly', () => {
    const store = useAuthStore()
    store.user = makeUser({ username: 'admin', nickname: 'Admin', role: 'admin' })
    expect(store.isAdmin).toBe(true)
  })

  it('should compute nickname from user', () => {
    const store = useAuthStore()
    store.user = makeUser({ nickname: 'MyNick' })
    expect(store.nickname).toBe('MyNick')
  })

  it('should fallback to username for nickname', () => {
    const store = useAuthStore()
    store.user = makeUser({ username: 'testuser', nickname: '' })
    expect(store.nickname).toBe('testuser')
  })

  it('should reset state', () => {
    const store = useAuthStore()
    store.token = 'test-token'
    store.user = makeUser()
    store.resetState()
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
  })

  it('should update tokens', () => {
    const store = useAuthStore()
    store.updateTokens('new-token', 'new-refresh')
    expect(store.token).toBe('new-token')
    expect(store.refreshToken).toBe('new-refresh')
  })

  it('should validate expired token', async () => {
    const store = useAuthStore()
    // Create an expired JWT token (exp in the past)
    const header = btoa(JSON.stringify({ alg: 'HS256' }))
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 3600 }))
    const signature = 'fake-sig'
    store.token = `${header}.${payload}.${signature}`

    const isValid = await store.validateToken()
    expect(isValid).toBe(false)
  })

  it('should validate valid token', async () => {
    const store = useAuthStore()
    // Create a valid JWT token (exp in the future)
    const header = btoa(JSON.stringify({ alg: 'HS256' }))
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }))
    const signature = 'fake-sig'
    store.token = `${header}.${payload}.${signature}`

    const isValid = await store.validateToken()
    expect(isValid).toBe(true)
  })
})
