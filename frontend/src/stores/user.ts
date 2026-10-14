/**
 * 用户状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/api/notification'
import { messageApi } from '@/api/message'
import { userApi } from '@/api/user'
import { useLogger } from '@/utils/logger'
import { encodeData, decodeData } from '@/utils/crypto'
import { STORAGE_KEY_PREFIX } from '@/constants'
import type { User, LoginRequest, RegisterRequest, ChangePasswordRequest, UpdateProfileRequest } from '@/types'

const logger = useLogger('UserStore')

// ============================================================
// Auth Store - 认证状态 (登录/登出/Token)
// ============================================================

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<User | null>(null)
  const remember = ref(false)
  const loading = ref(false)

  try {
    remember.value = localStorage.getItem(STORAGE_KEY_PREFIX + 'remember') === 'true'
  } catch {
    // storage unavailable
  }

  // 计算属性
  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')
  const userId = computed(() => user.value?.id)
  const username = computed(() => user.value?.username)
  const avatar = computed(() => user.value?.avatar)
  const nickname = computed(() => user.value?.nickname || user.value?.username)

  // 保存到本地存储
  function saveToStorage(): void {
    try {
      const storage = remember.value ? localStorage : sessionStorage
      const otherStorage = remember.value ? sessionStorage : localStorage
      // 清除另一端存储的旧数据，防止切换"记住我"后残留旧token
      otherStorage.removeItem(STORAGE_KEY_PREFIX + 'token')
      otherStorage.removeItem(STORAGE_KEY_PREFIX + 'refreshToken')
      otherStorage.removeItem(STORAGE_KEY_PREFIX + 'user')
      if (token.value) {
        storage.setItem(STORAGE_KEY_PREFIX + 'token', encodeData(token.value)!)
      }
      if (refreshToken.value) {
        storage.setItem(STORAGE_KEY_PREFIX + 'refreshToken', encodeData(refreshToken.value)!)
      }
      if (user.value) {
        storage.setItem(STORAGE_KEY_PREFIX + 'user', encodeData(user.value)!)
      }
    } catch (error) {
      logger.warn('Failed to save to storage', { error: (error as Error).message })
    }
  }

  // 清除所有本地存储中的认证数据
  function clearStorage(): void {
    try {
      localStorage.removeItem(STORAGE_KEY_PREFIX + 'token')
      localStorage.removeItem(STORAGE_KEY_PREFIX + 'refreshToken')
      localStorage.removeItem(STORAGE_KEY_PREFIX + 'user')
      localStorage.removeItem(STORAGE_KEY_PREFIX + 'remember')
      localStorage.removeItem(STORAGE_KEY_PREFIX + 'token_fingerprint')
      localStorage.removeItem(STORAGE_KEY_PREFIX + 'xsrf_token')
      sessionStorage.removeItem(STORAGE_KEY_PREFIX + 'token')
      sessionStorage.removeItem(STORAGE_KEY_PREFIX + 'refreshToken')
      sessionStorage.removeItem(STORAGE_KEY_PREFIX + 'user')
    } catch (error) {
      logger.warn('Failed to clear storage', { error: (error as Error).message })
    }
  }

  // 从本地存储恢复用户状态
  function restoreFromStorage(): void {
    try {
      const savedToken =
        localStorage.getItem(STORAGE_KEY_PREFIX + 'token') ||
        sessionStorage.getItem(STORAGE_KEY_PREFIX + 'token')
      const savedRefreshToken =
        localStorage.getItem(STORAGE_KEY_PREFIX + 'refreshToken') ||
        sessionStorage.getItem(STORAGE_KEY_PREFIX + 'refreshToken')
      const savedUser =
        localStorage.getItem(STORAGE_KEY_PREFIX + 'user') ||
        sessionStorage.getItem(STORAGE_KEY_PREFIX + 'user')
      remember.value = localStorage.getItem(STORAGE_KEY_PREFIX + 'remember') === 'true'

      const decodedToken = decodeData<string>(savedToken)
      const decodedUser = decodeData<User>(savedUser)

      if (decodedToken && decodedUser) {
        token.value = decodedToken
        refreshToken.value = decodeData(savedRefreshToken)
        user.value = decodedUser
        logger.info('Auth state restored from storage', { userId: user.value?.id })
      } else {
        logger.warn('Failed to decode stored auth data, clearing storage')
        clearStorage()
      }
    } catch (error) {
      logger.error('Failed to restore auth state', { error: (error as Error).message })
      clearStorage()
    }
  }

  // 重置所有状态并清除存储
  function resetState(): void {
    user.value = null
    token.value = null
    refreshToken.value = null
    clearStorage()
  }

  // 验证Token是否有效
  async function validateToken(): Promise<boolean> {
    if (!token.value) {
      return false
    }

    try {
      const parts = token.value.split('.')
      if (parts.length === 3) {
        const payload = JSON.parse(atob(parts[1]!.replace(/-/g, '+').replace(/_/g, '/')))
        if (payload.exp && payload.exp * 1000 < Date.now()) {
          logger.warn('Token expired', { exp: new Date(payload.exp * 1000).toISOString() })
          return false
        }
        if (payload.exp && payload.exp * 1000 - Date.now() < 5 * 60 * 1000) {
          logger.info('Token expiring soon, will refresh on next request')
        }
        return true
      }
      return true
    } catch (error) {
      logger.warn('Token validation failed', { error: (error as Error).message })
      return false
    }
  }

  // 检查Token是否可能已被窃取
  function detectTokenTheft(): boolean {
    try {
      const savedLocalToken = localStorage.getItem(STORAGE_KEY_PREFIX + 'token')
      const savedSessionToken = sessionStorage.getItem(STORAGE_KEY_PREFIX + 'token')
      const currentToken = token.value
      const encodedCurrent = encodeData(currentToken)
      // 检查两端存储是否与内存token不一致
      if (savedLocalToken && currentToken && savedLocalToken !== encodedCurrent) {
        logger.error('Token mismatch in localStorage - possible theft attempt')
        return true
      }
      if (savedSessionToken && currentToken && savedSessionToken !== encodedCurrent) {
        logger.error('Token mismatch in sessionStorage - possible theft attempt')
        return true
      }
      return false
    } catch {
      return false
    }
  }

  // 用户登录
  async function login(credentials: LoginRequest): Promise<{ success: boolean; message?: string }> {
    loading.value = true
    try {
      const response = await userApi.login(credentials)
      const loginData = (response as { data?: unknown }).data || response

      let {
        token: newToken,
        refreshToken: newRefreshToken,
        id,
        username: loginUsername,
        nickname: loginNickname,
        avatar: loginAvatar,
        email,
        role
      } = loginData as {
        token: string
        refreshToken: string
        id: number
        username: string
        nickname: string
        avatar: string
        email: string
        role: 'user' | 'admin'
      }

      if (!id && (loginData as { data?: unknown }).data) {
        const nestedData = (loginData as { data: Record<string, unknown> }).data
        newToken = nestedData.token as string
        newRefreshToken = nestedData.refreshToken as string
        id = nestedData.id as number
        loginUsername = nestedData.username as string
        loginNickname = nestedData.nickname as string
        loginAvatar = nestedData.avatar as string
        email = nestedData.email as string
        role = nestedData.role as 'user' | 'admin'
      }

      if (!newToken || !id) {
        logger.error('Invalid login response structure', { loginData })
        return { success: false, message: '登录失败，服务器响应异常' }
      }

      token.value = newToken
      refreshToken.value = newRefreshToken
      user.value = {
        id,
        username: loginUsername || (loginData as Record<string, unknown>).username as string || ((loginData as Record<string, unknown>).data as Record<string, unknown>)?.username as string,
        nickname: loginNickname || (loginData as Record<string, unknown>).nickname as string || ((loginData as Record<string, unknown>).data as Record<string, unknown>)?.nickname as string,
        avatar: loginAvatar || (loginData as Record<string, unknown>).avatar as string || ((loginData as Record<string, unknown>).data as Record<string, unknown>)?.avatar as string,
        email: email || (loginData as Record<string, unknown>).email as string || ((loginData as Record<string, unknown>).data as Record<string, unknown>)?.email as string,
        role: role || (loginData as Record<string, unknown>).role as 'user' | 'admin' || ((loginData as Record<string, unknown>).data as Record<string, unknown>)?.role as 'user' | 'admin'
      }

      if (credentials.remember) {
        try {
          localStorage.setItem(STORAGE_KEY_PREFIX + 'remember', 'true')
        } catch (e) {
          logger.warn('Failed to save remember preference', { error: (e as Error).message })
        }
        remember.value = true
      } else {
        try {
          localStorage.removeItem(STORAGE_KEY_PREFIX + 'remember')
        } catch (e) {
          logger.warn('Failed to remove remember preference', { error: (e as Error).message })
        }
        remember.value = false
      }

      saveToStorage()
      logger.info('Login successful', { userId: id, username: user.value.username })

      return { success: true }
    } catch (error) {
      logger.error('Login failed', { error: (error as Error).message })
      return {
        success: false,
        message: (error as { response?: { data?: { message?: string } } }).response?.data?.message || '登录失败'
      }
    } finally {
      loading.value = false
    }
  }

  // 用户注册
  async function register(data: RegisterRequest): Promise<{ success: boolean; message?: string }> {
    loading.value = true
    try {
      await userApi.register(data)
      logger.info('Registration successful', { username: data.username })
      return { success: true }
    } catch (error) {
      logger.error('Registration failed', { error: (error as Error).message })
      return {
        success: false,
        message: (error as { response?: { data?: { message?: string } } }).response?.data?.message || '注册失败'
      }
    } finally {
      loading.value = false
    }
  }

  // 用户登出
  async function logout(): Promise<void> {
    const userId = user.value?.id
    try {
      await userApi.logout()
    } catch (e) {
      logger.warn('Backend logout failed', { error: (e as Error).message })
    }
    resetState()
    // 重置通知计数
    unreadNotifications.value = 0
    unreadMessages.value = 0
    logger.info('User logged out', { userId })
  }

  // 更新Token和刷新Token
  function updateTokens(newToken: string, newRefreshToken: string): void {
    token.value = newToken
    refreshToken.value = newRefreshToken
    saveToStorage()
  }

  // 更新用户信息
  function updateUserInfo(userData: Partial<User>): void {
    const allowedFields: (keyof User)[] = [
      'id', 'username', 'nickname', 'avatar', 'email', 'role',
      'coverImage', 'bio', 'gender', 'birthday', 'phone', 'school', 'major', 'grade',
      'signature', 'status', 'createdAt'
    ]
    const filteredData = {} as Partial<User>
    for (const key of allowedFields) {
      if (key in userData) {
        ;(filteredData as Record<string, unknown>)[key] = userData[key]
      }
    }
    user.value = { ...user.value!, ...filteredData }
    saveToStorage()
    logger.info('User info updated', { userId: user.value?.id })
  }

  // 更新用户头像
  function updateAvatar(avatarUrl: string): void {
    if (user.value) {
      user.value.avatar = avatarUrl
      saveToStorage()
    }
  }

  // 更新用户封面图
  function updateCoverImage(coverImageUrl: string): void {
    if (user.value) {
      user.value.coverImage = coverImageUrl
      saveToStorage()
    }
  }

  /**
   * 更新个人资料
   */
  async function updateProfile(profileData: UpdateProfileRequest): Promise<{ success: boolean; message?: string }> {
    loading.value = true
    try {
      await userApi.updateProfile(profileData)
      updateUserInfo(profileData as Partial<User>)
      return { success: true }
    } catch (error) {
      logger.error('Failed to update profile', { error: (error as Error).message })
      return {
        success: false,
        message: (error as { response?: { data?: { message?: string } } }).response?.data?.message || '更新失败'
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 修改密码
   */
  async function changePassword(passwordData: ChangePasswordRequest): Promise<{ success: boolean; message?: string }> {
    loading.value = true
    try {
      await userApi.changePassword(passwordData)
      return { success: true }
    } catch (error) {
      logger.error('Failed to change password', { error: (error as Error).message })
      return {
        success: false,
        message: (error as { response?: { data?: { message?: string } } }).response?.data?.message || '修改密码失败'
      }
    } finally {
      loading.value = false
    }
  }

  // 通知未读计数 (Navbar 等直接通过 userStore 访问)
  const unreadNotifications = ref(0)
  const unreadMessages = ref(0)
  const totalUnread = computed(() => unreadNotifications.value + unreadMessages.value)

  async function fetchUnreadCounts(): Promise<void> {
    if (!isLoggedIn.value) return
    try {
      const [notifResult, msgResult] = await Promise.allSettled([
        notificationApi.getUnreadCount(),
        messageApi.getUnreadCount()
      ])
      unreadNotifications.value =
        (notifResult.status === 'fulfilled' ? (notifResult.value as { data?: number }).data : 0) || 0
      unreadMessages.value =
        (msgResult.status === 'fulfilled' ? (msgResult.value as { data?: number }).data : 0) || 0
    } catch (error) {
      logger.warn('Failed to fetch unread counts', { error: (error as Error).message })
    }
  }

  function incrementNotifications(count = 1): void {
    unreadNotifications.value += count
  }
  function incrementMessages(count = 1): void {
    unreadMessages.value += count
  }

  return {
    token,
    refreshToken,
    user,
    remember,
    loading,

    isLoggedIn,
    isAdmin,
    userId,
    username,
    avatar,
    nickname,

    unreadNotifications,
    unreadMessages,
    totalUnread,

    saveToStorage,
    clearStorage,
    restoreFromStorage,
    resetState,
    validateToken,
    detectTokenTheft,
    login,
    register,
    logout,
    updateTokens,
    updateUserInfo,
    updateAvatar,
    updateCoverImage,
    updateProfile,
    changePassword,
    fetchUnreadCounts,
    incrementNotifications,
    incrementMessages
  }
})

// ============================================================
// User Store - 兼容别名 (与 useAuthStore 相同)
// ============================================================

export const useUserStore = useAuthStore

