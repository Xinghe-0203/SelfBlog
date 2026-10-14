/**
 * 用户相关类型定义
 */

/** 用户角色 */
export type UserRole = 'user' | 'admin'

/** 用户状态 */
export type UserStatus = 0 | 1 // 0=禁用, 1=正常

/** 性别 */
export type Gender = 0 | 1 | 2 // 0=未知, 1=男, 2=女

/** 用户信息 VO - 对应 UserVO.java（脱敏，后端返回用） */
export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar?: string
  coverImage?: string
  bio?: string
  email?: string
  role?: string
  status?: number
  createTime?: string
  updateTime?: string
  followerCount?: number
  followingCount?: number
  isFollowing?: boolean
}

/** 用户信息 */
export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  role: UserRole
  coverImage?: string
  bio?: string
  gender?: Gender
  birthday?: string
  phone?: string
  school?: string
  major?: string
  grade?: string
  signature?: string
  status?: UserStatus
  followerCount?: number
  followingCount?: number
  postCount?: number
  likeCount?: number
  isFollowing?: boolean
  createdAt?: string
  createTime?: string
  updatedAt?: string
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
  email?: string
  remember?: boolean
  loginType?: string
}

/** 登录响应 */
export interface LoginResponse {
  token: string
  refreshToken: string
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  role: UserRole
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  email?: string
  code?: string
}

/** 修改密码请求 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

/** 重置密码请求 */
export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

/** 更新个人资料请求 */
export interface UpdateProfileRequest {
  nickname?: string
  bio?: string
  gender?: Gender
  birthday?: string
  phone?: string
  school?: string
  major?: string
  grade?: string
  signature?: string
}

/** 用户搜索参数 */
export interface UserSearchParams extends PaginationParams {
  keyword: string
}

/** 设备信息 */
export interface DeviceInfo {
  deviceId: string
  deviceName: string
  lastActive: string
}

import type { PaginationParams } from './common'
