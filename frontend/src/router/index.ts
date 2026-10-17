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
  // 用户路由仅保留修改密码页；其余社区页面已随单人博客精简移除。
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
