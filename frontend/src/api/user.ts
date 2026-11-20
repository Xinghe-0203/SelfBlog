/**
 * 用户认证与资料接口
 */

import api from './index'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ChangePasswordRequest
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
   * 登出
   */
  logout(): Promise<ApiResponse> {
    return api.post('/user/logout')
  },

  /**
   * 修改密码
   */
  changePassword(data: ChangePasswordRequest): Promise<ApiResponse> {
    return api.put('/user/password', data)
  }
}
