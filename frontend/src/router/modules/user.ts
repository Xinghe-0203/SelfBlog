/**
 * 用户路由模块
 *
 * 单人作者博客已移除其余社区页面，仅保留作者使用的修改密码页。
 */
import type { RouteRecordRaw } from 'vue-router'
import { lazyLoad } from '../helpers'

const userRoutes: RouteRecordRaw[] = [
  {
    path: '/password-change',
    name: 'PasswordChange',
    component: lazyLoad(() => import('@/views/user/PasswordChange.vue')),
    meta: { title: '修改密码', requiresAuth: true }
  }
]

export default userRoutes
