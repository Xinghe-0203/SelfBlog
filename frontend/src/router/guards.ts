/**
 * 路由守卫
 */
import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useLogger } from '@/utils/logger'
import { toast } from '@/utils/toast'
import { useSiteStore } from '@/stores/site'

const logger = useLogger('Router')

export function setupGuards(router: Router): void {
  router.beforeEach(async (to, from) => {
    const userStore = useUserStore()
    const siteStore = useSiteStore()

    document.title = to.meta.title
      ? `${to.meta.title as string} - ${siteStore.settings.siteTitle}`
      : siteStore.settings.siteTitle

    logger.debug('Navigation', { from: from.path, to: to.path })

    if ((to.meta.requiresAuth || to.meta.requiresAdmin) && !userStore.isLoggedIn) {
      logger.info('Showing author login', { returnPath: to.fullPath })
      return { path: siteStore.adminRoot, query: { redirect: to.fullPath } }
    }

    if (userStore.isLoggedIn && to.meta.requiresAuth) {
      let isValid = false
      try {
        isValid = await userStore.validateToken()
      } catch (err) {
        logger.error('Token validation threw exception', { error: (err as Error).message })
        isValid = false
      }
      if (!isValid) {
        logger.warn('Token expired, redirecting to login', { path: to.fullPath })
        userStore.resetState()
        toast.warning('登录已过期，请重新登录')
        return { path: siteStore.adminRoot, query: { redirect: to.fullPath } }
      }
    }

    if (to.meta.requiresAdmin) {
      if (!userStore.isLoggedIn)
        return { path: siteStore.adminRoot, query: { redirect: to.fullPath } }
      if (!userStore.isAdmin) {
        logger.warn('Admin access denied', { userId: userStore.user?.id })
        toast.error('无管理员权限')
        return { name: 'Home' }
      }
    }

    if (to.meta.guest && userStore.isLoggedIn) {
      toast.info('您已登录')
      return { name: 'Home' }
    }
  })

  router.onError((error) => {
    logger.error('Router navigation error', { error: error.message })

    if (error.name === 'ChunkLoadError' || error.message?.includes('Loading chunk')) {
      toast.error('页面加载失败，请刷新重试')
      router.push({ name: 'Home' })
      return
    }

    toast.error('页面导航失败，请重试')
  })

  router.afterEach((to, from) => {
    logger.debug('Navigation completed', { to: to.path })

    if (from.name && to.path === from.path && to.hash !== from.hash) {
      logger.warn('Navigation aborted or failed', { from: from.fullPath, to: to.fullPath })
    }
  })
}
