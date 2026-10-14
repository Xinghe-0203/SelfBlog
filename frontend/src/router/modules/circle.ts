/**
 * 校友圈路由模块
 */
import type { RouteRecordRaw } from 'vue-router'
import { lazyLoad } from '../helpers'

const circleRoutes: RouteRecordRaw[] = [
  {
    path: '/circle',
    name: 'Circle',
    component: lazyLoad(() => import('@/views/circle/Circle.vue')),
    meta: { title: '校友圈' }
  },
  {
    path: '/circle/post',
    name: 'CirclePost',
    component: lazyLoad(() => import('@/views/circle/CirclePost.vue')),
    meta: { title: '发布动态', requiresAuth: true }
  },
  {
    path: '/circle/post/edit/:id',
    name: 'CirclePostEdit',
    component: lazyLoad(() => import('@/views/circle/CirclePost.vue')),
    meta: { title: '编辑动态', requiresAuth: true }
  },
  {
    path: '/circle/:id',
    name: 'CircleDetail',
    component: lazyLoad(() => import('@/views/circle/CircleDetail.vue')),
    meta: { title: '动态详情' }
  }
]

export default circleRoutes
