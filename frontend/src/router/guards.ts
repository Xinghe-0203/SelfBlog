/**
 * 路由守卫
 */
import type { RouteLocationNormalized, Router } from 'vue-router'
import { isNavigationFailure, NavigationFailureType } from 'vue-router'
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

    if (to.meta.requiresAdmin && !userStore.isAdmin) {
      logger.warn('Admin access denied', { userId: userStore.user?.id })
      toast.error('无管理员权限')
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

  // afterEach 的成功导航是常态；只有真正的导航失败（重复导航除外）才值得告警。
  router.afterEach((to, from, failure) => {
    logger.debug('Navigation completed', { to: to.path })
    if (failure && !isNavigationFailure(failure, NavigationFailureType.duplicated)) {
      logger.warn('Navigation failed', { to: to.fullPath, failure: String(failure) })
      return
    }
    if (failure) return
    // 键盘用户点完导航后，旧焦点会随旧页面一起消失（落在 <body> 上），
    // 下一次 Tab 会从头开始 —— 相当于把阅读位置丢了。这里把焦点搬到主内容容器。
    if (shouldMoveFocusToMain(to, from)) focusMainContent()
  })
}

/**
 * 一次成功的导航之后，是否应该把焦点移到 `<main>`。
 *
 * <p>三条排除理由：</p>
 * <ul>
 *   <li>**首次进入**（`from` 还没匹配到任何路由）：此时焦点本来就该从文档开头开始，
 *       抢焦点反而会打断读屏对页面标题的朗读；</li>
 *   <li>**带 hash 的导航**：浏览器要滚到目标锚点，焦点移走会打断它；</li>
 *   <li>**只有 query 变化**（翻页、切排序）：页面主体没换，焦点留在刚点的控件上更自然。</li>
 * </ul>
 */
export function shouldMoveFocusToMain(
  to: RouteLocationNormalized,
  from: RouteLocationNormalized
): boolean {
  if (from.matched.length === 0) return false
  if (to.hash) return false
  return to.path !== from.path
}

/** 把焦点交给主内容容器（`tabindex="-1"` 使其可编程聚焦，但不进 Tab 序）。 */
function focusMainContent(): void {
  const main = document.getElementById('main-content')
  // preventScroll：滚动位置由 router 的 scrollBehavior 决定，这里不许抢。
  main?.focus({ preventScroll: true })
}
