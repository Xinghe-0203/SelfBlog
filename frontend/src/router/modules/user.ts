/**
 * 用户路由模块
 */
import type { RouteRecordRaw } from 'vue-router'
import { lazyLoad } from '../helpers'

const userRoutes: RouteRecordRaw[] = [
  {
    path: '/profile',
    name: 'Profile',
    component: lazyLoad(() => import('@/views/user/Profile.vue')),
    meta: { title: '我的主页', requiresAuth: true }
  },
  {
    path: '/profile-edit',
    name: 'ProfileEdit',
    component: lazyLoad(() => import('@/views/user/ProfileEdit.vue')),
    meta: { title: '编辑资料', requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: lazyLoad(() => import('@/views/user/UserProfile.vue')),
    meta: { title: '用户主页' }
  },
  {
    path: '/password-change',
    name: 'PasswordChange',
    component: lazyLoad(() => import('@/views/user/PasswordChange.vue')),
    meta: { title: '修改密码', requiresAuth: true }
  },
  {
    path: '/drafts',
    name: 'Drafts',
    component: lazyLoad(() => import('@/views/user/Drafts.vue')),
    meta: { title: '我的草稿', requiresAuth: true }
  },
  {
    path: '/collections',
    name: 'Collections',
    component: lazyLoad(() => import('@/views/user/Collections.vue')),
    meta: { title: '我的收藏', requiresAuth: true }
  },
  {
    path: '/following',
    name: 'Following',
    component: lazyLoad(() => import('@/views/user/Following.vue')),
    meta: { title: '我的关注', requiresAuth: true }
  },
  {
    path: '/followers',
    name: 'Followers',
    component: lazyLoad(() => import('@/views/user/Followers.vue')),
    meta: { title: '我的粉丝', requiresAuth: true }
  },
  {
    path: '/my-reports',
    name: 'MyReports',
    component: lazyLoad(() => import('@/views/user/MyReports.vue')),
    meta: { title: '我的举报', requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: lazyLoad(() => import('@/views/user/Notifications.vue')),
    meta: { title: '消息通知', requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: lazyLoad(() => import('@/views/user/Messages.vue')),
    meta: { title: '私信', requiresAuth: true }
  }
]

export default userRoutes
