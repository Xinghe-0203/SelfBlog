/**
 * 认证路由模块
 *
 * 本站为单人作者博客：访客只读，唯一的登录入口是服务端下发的作者后台路径
 * （`siteStore.adminRoot`，默认 `/admin`，由后台“网站设置”决定）。
 *
 * 多个组件（LikeButton / CollectButton / FollowButton / Circle / CircleDetail 等）
 * 在访客执行需登录操作时会 `router.push('/login')`。此前 authRoutes 为空数组，
 * 这些跳转全部落到 404 页，因此这里补一个 `/login` 重定向到作者后台入口，
 * 与 `guards.ts` 中 `requiresAuth` 的跳转约定保持一致。
 */
import type { RouteRecordRaw } from 'vue-router'
import { useSiteStore } from '@/stores/site'

const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    redirect: () => {
      const siteStore = useSiteStore()
      const adminRoot = siteStore.adminRoot
      // 后台路径被配置成 /login 时不再重定向，避免无限重定向
      return adminRoot && adminRoot !== '/login' ? { path: adminRoot } : { path: '/' }
    }
  }
]

export default authRoutes
