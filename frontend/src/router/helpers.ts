/**
 * 路由辅助函数
 */
import { h } from 'vue'
import type { Router } from 'vue-router'
import { toast } from '@/utils/toast'

const ErrorFallback = () => import('@/views/common/NotFound.vue')

/**
 * 懒加载失败时的兜底组件。
 * 这里必须是已编译的组件（`h()` 渲染函数）：构建产物解析的是 runtime-only 的 vue，
 * 传 `{ template: '...' }` 只会在控制台告警并渲染空白。
 */
const FallbackView = {
  name: 'RouteErrorFallback',
  render() {
    return h('div', { style: 'text-align:center;padding:80px 20px;' }, [
      h('h2', '页面加载失败'),
      h('p', '请刷新页面重试')
    ])
  }
}

export const lazyLoad = (importFunc: () => Promise<unknown>) => {
  return () =>
    importFunc().catch((err: Error) => {
      console.error('路由加载失败:', err)
      toast.error('页面加载失败，请刷新重试')
      return ErrorFallback().catch(() => FallbackView) as Promise<unknown>
    }) as Promise<unknown>
}

/**
 * 校验登录后的 `?redirect=` 目标，只放行真实注册过的后台路由。
 *
 * 只做 `startsWith(adminRoot)` 是不够的：`/admin//evil.com` 同样以 `/admin` 开头，
 * 而 `router.resolve()` 不会把双斜杠折叠掉。所以先按站内绝对路径过滤掉
 * `//host`（协议相对）与 `https://…`（绝对 URL），再用解析结果是否命中
 * `adminArea` 路由来判定 —— 未注册的路径（含拼错的后台地址）一律回退。
 */
export function resolveAdminRedirect(
  router: Router,
  adminRoot: string,
  candidate: unknown,
  fallback: string
): string {
  if (typeof candidate !== 'string' || !candidate.startsWith('/') || candidate.startsWith('//')) {
    return fallback
  }
  const resolved = router.resolve(candidate)
  if (!resolved.matched.some((record) => record.meta.adminArea === true)) return fallback
  if (resolved.path !== adminRoot && !resolved.path.startsWith(`${adminRoot}/`)) return fallback
  return resolved.fullPath
}
