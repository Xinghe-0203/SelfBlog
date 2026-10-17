/**
 * 文章路由模块
 */
import type { RouteRecordRaw } from 'vue-router'
import { lazyLoad } from '../helpers'

const postRoutes: RouteRecordRaw[] = [
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: lazyLoad(() => import('@/views/post/PostDetail.vue')),
    meta: { title: '文章详情' }
  }
]

export default postRoutes
