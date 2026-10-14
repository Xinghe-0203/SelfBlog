/**
 * 首页路由模块
 */
import type { RouteRecordRaw } from 'vue-router'
import { lazyLoad } from '../helpers'

const homeRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: lazyLoad(() => import('@/views/Home.vue')),
    meta: { title: '首页' }
  },
  {
    path: '/archives',
    name: 'Archives',
    component: lazyLoad(() => import('@/views/Archives.vue')),
    meta: { title: '归档' }
  },
  {
    path: '/about',
    name: 'About',
    component: lazyLoad(() => import('@/views/About.vue')),
    meta: { title: '关于' }
  },
  {
    path: '/gallery',
    name: 'Gallery',
    component: lazyLoad(() => import('@/views/gallery/Gallery.vue')),
    meta: { title: '相册' }
  }
]

export default homeRoutes
