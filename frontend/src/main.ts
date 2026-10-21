/** Application entry point. */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { marked } from 'marked'
import App from './App.vue'
import router, { installAdminRoutes } from './router'
import { useLogger } from './utils/logger'
import { useSiteStore } from './stores/site'
import { useUserStore } from './stores/user'
import { usePagesStore } from './stores/pages'
import { vScrollReveal } from './composables/useScrollReveal'
import './styles/main.css'

marked.setOptions({ gfm: true, breaks: true })

const app = createApp(App)
const pinia = createPinia()
const logger = useLogger('App')
app.use(pinia)

app.directive('scroll-reveal', vScrollReveal)
app.config.errorHandler = (err, _instance, info) => {
  logger.error('Vue error', { error: (err as Error).message, info })
  console.error('[Vue Error]', err)
}

if (import.meta.env.DEV) {
  app.config.warnHandler = (msg, _instance, info) => {
    logger.warn('Vue warning', { message: msg, info })
  }
}

window.addEventListener('unhandledrejection', (event) => {
  logger.error('Unhandled Promise Rejection', { reason: event.reason?.message || event.reason })
})
window.onerror = (message, source, lineno, colno, error) => {
  logger.error('Global JS Error', { message, source, lineno, colno, error: error?.message })
}

async function bootstrap() {
  const siteStore = useSiteStore(pinia)
  await siteStore.loadPublicSettings()
  installAdminRoutes(siteStore.adminPath)

  // Restore admin identity before the router performs its first navigation.
  useUserStore(pinia).restoreFromStorage()
  app.use(router)
  app.mount('#app')
  // Optional published configuration must not delay identity restoration or initial navigation.
  void usePagesStore(pinia).loadManifest()
  logger.info('Application initialized', { version: '2.0.25', mode: import.meta.env.MODE })
}

void bootstrap().catch((error: unknown) => {
  logger.error('Application bootstrap failed', { error: (error as Error).message })
  // 兜底挂载也必须先装路由：App.vue 的 <router-view> 在缺少路由插件时会抛错，
  // 用户只会看到空白页而不是错误外壳。
  try {
    app.use(router)
  } catch (routerError: unknown) {
    logger.error('Router install failed during fallback mount', {
      error: (routerError as Error).message
    })
  }
  app.mount('#app')
})
