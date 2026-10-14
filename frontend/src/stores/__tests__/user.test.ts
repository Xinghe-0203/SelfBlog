import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../user'

// Mock API modules
vi.mock('@/api/user', () => ({
  userApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn()
  }
}))

vi.mock('@/api/notification', () => ({
  notificationApi: {
    getUnreadCount: vi.fn().mockResolvedValue({ data: 0 })
  }
}))

vi.mock('@/api/message', () => ({
  messageApi: {
    getUnreadCount: vi.fn().mockResolvedValue({ data: 0 })
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
    store.user = { id: 1, username: 'test', nickname: 'Test', role: 'user' } as any
    expect(store.isLoggedIn).toBe(true)
  })

  it('should compute isAdmin correctly', () => {
    const store = useAuthStore()
    store.user = { id: 1, username: 'admin', nickname: 'Admin', role: 'admin' } as any
    expect(store.isAdmin).toBe(true)
  })

  it('should compute nickname from user', () => {
    const store = useAuthStore()
    store.user = { id: 1, username: 'test', nickname: 'MyNick', role: 'user' } as any
    expect(store.nickname).toBe('MyNick')
  })

  it('should fallback to username for nickname', () => {
    const store = useAuthStore()
    store.user = { id: 1, username: 'testuser', nickname: '', role: 'user' } as any
    expect(store.nickname).toBe('testuser')
  })

  it('should reset state', () => {
    const store = useAuthStore()
    store.token = 'test-token'
    store.user = { id: 1, username: 'test', nickname: 'Test', role: 'user' } as any
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

  it('should update user info', () => {
    const store = useAuthStore()
    store.user = { id: 1, username: 'test', nickname: 'Test', role: 'user' } as any
    store.updateUserInfo({ nickname: 'Updated' })
    expect(store.user!.nickname).toBe('Updated')
  })

  it('should update avatar', () => {
    const store = useAuthStore()
    store.user = { id: 1, username: 'test', nickname: 'Test', role: 'user', avatar: '' } as any
    store.updateAvatar('http://example.com/avatar.jpg')
    expect(store.user!.avatar).toBe('http://example.com/avatar.jpg')
  })

  it('should track unread counts', () => {
    const store = useAuthStore()
    store.incrementNotifications(3)
    store.incrementMessages(2)
    expect(store.unreadNotifications).toBe(3)
    expect(store.unreadMessages).toBe(2)
    expect(store.totalUnread).toBe(5)
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
