/**
 * 用户认证与资料接口
 */

import api, { getToken } from './index'
import { STORAGE_KEY_PREFIX } from '@/constants'
import type {
  ApiResponse,
  PaginatedResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ChangePasswordRequest,
  ResetPasswordRequest,
  UpdateProfileRequest,
  UserSearchParams,
  User
} from '@/types'

export const userApi = {
  /**
   * 注册
   */
  register(data: RegisterRequest): Promise<ApiResponse> {
    return api.post('/user/register', data)
  },

  /**
   * 登录
   */
  login(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    return api.post('/user/login', data)
  },

  /**
   * 刷新Token
   */
  refreshToken(): Promise<ApiResponse<LoginResponse>> {
    const refreshToken = getToken(STORAGE_KEY_PREFIX + 'refreshToken')
    return api.post('/user/refresh', null, {
      headers: { Authorization: 'Bearer ' + refreshToken }
    })
  },

  /**
   * 登出
   */
  logout(): Promise<ApiResponse> {
    return api.post('/user/logout')
  },

  /**
   * 获取用户信息
   */
  getUserById(id: number | string): Promise<ApiResponse<User>> {
    return api.get(`/user/${id}`)
  },

  /**
   * 修改密码
   */
  changePassword(data: ChangePasswordRequest): Promise<ApiResponse> {
    return api.put('/user/password', data)
  },

  /**
   * 搜索用户
   */
  searchUsers(params: UserSearchParams): Promise<PaginatedResponse<User>> {
    return api.get('/user/search', { params })
  },

  /**
   * 发送验证码
   */
  sendCode(email: string): Promise<ApiResponse> {
    return api.post('/auth/password/send-code', { email })
  },

  /**
   * 重置密码
   */
  resetPassword(data: ResetPasswordRequest): Promise<ApiResponse> {
    return api.put('/auth/password/reset-password', data)
  },

  /**
   * 更新个人资料
   */
  updateProfile(data: UpdateProfileRequest): Promise<ApiResponse> {
    return api.put('/user/profile', data)
  },

  /**
   * 更新头像
   */
  updateAvatar(avatarUrl: string): Promise<ApiResponse> {
    return api.put('/user/avatar', null, { params: { avatar: avatarUrl } })
  },

  /**
   * 更新封面图
   */
  updateCoverImage(coverImageUrl: string): Promise<ApiResponse> {
    return api.put('/user/cover-image', null, { params: { coverImage: coverImageUrl } })
  },

  /**
   * 发送注册验证码
   */
  sendRegisterCode(email: string, username: string): Promise<ApiResponse> {
    return api.post('/auth/register/send-code', { email, username })
  },

  /**
   * 验证注册验证码并完成注册
   */
  verifyRegisterCode(data: Record<string, unknown>): Promise<ApiResponse> {
    return api.post('/auth/register/verify', data)
  },

  /**
   * 登出其他设备
   */
  logoutOtherDevices(): Promise<ApiResponse> {
    return api.post('/user/logout-other-devices')
  },

  /**
   * 获取设备数量
   */
  getDevices(): Promise<ApiResponse<number>> {
    return api.get('/user/devices')
  }
}
