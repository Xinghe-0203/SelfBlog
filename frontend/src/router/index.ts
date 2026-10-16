/**
 * 路由配置 - 模块化入口
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './guards'

import authRoutes from './modules/auth'
import homeRoutes from './modules/home'
import postRoutes from './modules/post'
import discoverRoutes from './modules/discover'
import userRoutes from './modules/user'
import { createAdminRoutes } from './modules/admin'

export { lazyLoad } from './helpers'

const routes: RouteRecordRaw[] = [
  ...homeRoutes,
  ...authRoutes,
  ...postRoutes,
  ...discoverRoutes,
  // userRoutes 此前已定义但从未注册，导致 /profile、/drafts、/collections、
  // /messages、/notifications、/my-reports 等十多个页面全部落到 404。
  ...userRoutes,
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0 }
  }
})

setupGuards(router)

export function installAdminRoutes(adminPath: string): void {
  // Preserve a legacy /pages author entry instead of silently taking its namespace.
  if (adminPath !== 'pages' && !router.hasRoute('ContentPage')) {
    router.addRoute({
      path: '/pages/:slug',
      name: 'ContentPage',
      component: () => import('@/views/pages/ContentPage.vue'),
      meta: { title: '内容页' }
    })
  }
  for (const route of createAdminRoutes(adminPath)) {
    router.addRoute(route)
  }
}

export default router
