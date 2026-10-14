/**
 * 路由辅助函数
 */
import { toast } from '@/utils/toast'

const ErrorFallback = () => import('@/views/common/NotFound.vue')

export const lazyLoad = (importFunc: () => Promise<unknown>) => {
  return () =>
    importFunc().catch((err: Error) => {
      console.error('路由加载失败:', err)
      toast.error('页面加载失败，请刷新重试')
      return ErrorFallback().catch(
        () =>
          ({
            template:
              '<div style="text-align:center;padding:80px 20px;"><h2>页面加载失败</h2><p>请刷新页面重试</p></div>'
          }) as unknown
      )
    }) as Promise<unknown>
}
