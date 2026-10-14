/** Public reading routes: article search and tag archives. */
import type { RouteRecordRaw } from 'vue-router'
import { lazyLoad } from '../helpers'

const discoverRoutes: RouteRecordRaw[] = [
  {
    path: '/search',
    name: 'Search',
    component: lazyLoad(() => import('@/views/search/Search.vue')),
    meta: { title: '搜索' }
  },
  {
    path: '/tag/:id',
    name: 'TagDetail',
    component: lazyLoad(() => import('@/views/tag/TagDetail.vue')),
    meta: { title: '标签详情' }
  }
]

export default discoverRoutes
