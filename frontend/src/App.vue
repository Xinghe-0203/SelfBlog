<template>
  <div id="selfblog" :class="{ 'dark-mode': isDarkMode }">
    <div id="overlay-root" />
    <a href="#main-content" class="skip-link">跳转到主要内容</a>
    <Navbar v-if="!isAdminPage" />
    <main id="main-content" class="app-main" :class="{ 'app-main--admin': isAdminPage }">
      <router-view v-slot="{ Component, route }">
        <Transition :name="transitionName" mode="out-in" @before-enter="onBeforeEnter">
          <div :key="route.path" class="page-wrapper">
            <component :is="Component" />
          </div>
        </Transition>
      </router-view>
    </main>
    <Footer v-if="!isAdminPage" />
    <MusicPlayer />
    <BackToTop />
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { destroyLogger } from '@/utils/logger'
import { shouldOpenSearch } from '@/utils/searchShortcut'
import Navbar from '@/components/layout/Navbar.vue'
import Footer from '@/components/layout/Footer.vue'
import BackToTop from '@/components/common/BackToTop.vue'
import Toast from '@/components/common/Toast.vue'
import MusicPlayer from '@/components/layout/MusicPlayer.vue'
import { useSiteStore } from '@/stores/site'
import { siteApi } from '@/api/site'
import { applySeoMeta, setSeoSiteTitle } from '@/composables/useSeoMeta'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const siteStore = useSiteStore()

const isDarkMode = computed(() => themeStore.isDarkMode)
const isAdminPage = computed(() => route.meta.adminArea === true)

// Track transition direction based on route depth
const transitionName = ref('page')

const getRouteDepth = (path: string): number => {
  return path.split('/').filter(Boolean).length
}

watch(
  () => route.path,
  (to, from) => {
    if (!from) {
      transitionName.value = 'page'
      return
    }
    const toDepth = getRouteDepth(to)
    const fromDepth = getRouteDepth(from)
    transitionName.value = toDepth >= fromDepth ? 'page-forward' : 'page-back'
  }
)

// 路由切换时回到顶部：这里必须是 instant，滚动动画交给 `scroll-behavior` 与
// `prefers-reduced-motion` 决定，不要在运行时改写 documentElement 的行内样式
// （那会覆盖 a11y.css 的 reduced-motion 规则且永远不会被还原）。
const onBeforeEnter = () => {
  window.scrollTo({ top: 0, behavior: 'instant' })
}

// 匿名统计开关：作者关闭时服务端本来就会拒掉上报，前端再发就是一次注定失败的
// POST。只有显式 'false' 才跳过，其余取值（含脏数据）按开启处理，与服务端读法一致。
const canTrackVisitors = computed(() => siteStore.settings.visitorTrackingEnabled !== 'false')

/**
 * 全局搜索快捷键（文档类站点惯例）：Ctrl/Cmd+K 或 `/` 直达搜索页。
 * 是否命中由 utils/searchShortcut.ts 的纯函数判定（编辑态、长按重复都让路）；
 * 已在搜索页时只吞掉默认行为，不重复跳转。
 */
function onGlobalKeydown(event: KeyboardEvent): void {
  if (!shouldOpenSearch(event.target, event.key, event)) return
  event.preventDefault()
  if (route.path !== '/search') void router.push('/search')
}

onMounted(async () => {
  themeStore.initTheme()
  applySiteTheme()
  window.addEventListener('keydown', onGlobalKeydown)
  if (!isAdminPage.value && canTrackVisitors.value) void recordVisitor()
})

watch(
  () => route.path,
  () => {
    if (!isAdminPage.value && canTrackVisitors.value) void recordVisitor()
  }
)

watch(
  () => route.fullPath,
  () => {
    if (!isAdminPage.value) return
    setSeoSiteTitle(siteStore.settings.siteTitle)
    applySeoMeta({
      title: String(route.meta.title || '作者后台'),
      type: 'website',
      indexable: false,
      url: route.path
    })
  },
  { immediate: true }
)

watch(
  // isDarkMode 也在依赖里：深色画布要撤掉作者的浅色背景，否则浅底浅字不可读。
  // 外观深度自定义的枚举键同样要进依赖：它们由 applyAppearance() 写成
  // #selfblog 的 data-ap-* 属性，不进依赖数组的话后台保存后要刷新才看得到。
  // readingProgress、summaryCallout 与 postLicense 刻意不在依赖里：它们由 PostDetail 的
  // v-if 响应式消费，无需重放外观。
  () =>
    [
      siteStore.settings.themeSkin,
      siteStore.settings.themePrimaryColor,
      themeStore.isDarkMode,
      siteStore.settings.fontFamily,
      siteStore.settings.baseFontSize,
      siteStore.settings.bodyLineHeight,
      siteStore.settings.readerWidth,
      siteStore.settings.paragraphSpacing,
      siteStore.settings.radiusScale,
      siteStore.settings.shadowDepth,
      siteStore.settings.layoutDensity,
      siteStore.settings.homeHeroStyle,
      siteStore.settings.postCoverStyle,
      siteStore.settings.motionIntensity,
      siteStore.settings.blockquoteStyle,
      siteStore.settings.postRowStyle,
      siteStore.settings.contentImageStyle,
      siteStore.settings.tableStyle,
      siteStore.settings.linkStyle
    ] as const,
  applySiteTheme
)

function applySiteTheme() {
  siteStore.applyAppearance()
}

function recordVisitor() {
  // document.referrer 可能是非绝对地址（about:client、部分 WebView），new URL 会抛错。
  let referrer = ''
  try {
    referrer = document.referrer ? new URL(document.referrer).href : ''
  } catch {
    referrer = ''
  }
  return siteApi.trackVisitor(route.fullPath, referrer).catch(() => undefined)
}

onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
  themeStore.destroyTheme()
  destroyLogger()
})
</script>

<style>
/* ==================== Main Content ==================== */
.app-main {
  min-height: calc(100vh - 80px);
  padding-top: 80px;
  position: relative;
}

.app-main--admin {
  min-height: 100vh;
  padding-top: 0;
}

.page-wrapper {
  min-height: calc(100vh - 60px);
}

/* ==================== Skip Link ==================== */
.skip-link {
  position: absolute;
  top: -48px;
  left: var(--spacing-md);
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  color: white;
  padding: 0.5rem 1rem;
  font-size: var(--text-sm);
  font-weight: 500;
  border-radius: 0 0 var(--radius) var(--radius);
  z-index: 10000;
  transition: top 0.25s var(--ease-spring);
  text-decoration: none;
  box-shadow: var(--shadow-md);
}

.skip-link:focus {
  top: 0;
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

/* ==================== Page Transitions — Motion Choreography ====================
   Snap-decelerate easing (0.16, 1, 0.3, 1) for enters — fast start, gentle stop.
   Ease-out for exits — quicker, less prominent.
   Enter duration > exit duration so the eye catches the arriving page.
   ============================================================================ */

/* Standard page transition — 时长走 --ap-page-*，由后台「动效强度」写入
   （customize.css 在 #selfblog[data-ap-motion=…] 的 .app-main 上定义，
   转场元素是 .app-main 的后代，继承得到）。fallback 就是原来的 280/160ms。 */
.page-enter-active {
  transition:
    opacity var(--ap-page-enter, 280ms) cubic-bezier(0.16, 1, 0.3, 1),
    transform var(--ap-page-enter, 280ms) cubic-bezier(0.16, 1, 0.3, 1);
}

.page-leave-active {
  transition:
    opacity var(--ap-page-leave, 160ms) ease-out,
    transform var(--ap-page-leave, 160ms) ease-out;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Forward navigation (deeper) — slide from right */
.page-forward-enter-active {
  transition:
    opacity var(--ap-page-enter, 300ms) cubic-bezier(0.16, 1, 0.3, 1),
    transform var(--ap-page-enter, 300ms) cubic-bezier(0.16, 1, 0.3, 1);
}

.page-forward-leave-active {
  transition:
    opacity var(--ap-page-leave, 200ms) ease-out,
    transform var(--ap-page-leave, 200ms) ease-out;
}

.page-forward-enter-from {
  opacity: 0;
  transform: translateX(24px);
}

.page-forward-leave-to {
  opacity: 0;
  transform: translateX(-12px);
}

/* Back navigation (shallower) — slide from left */
.page-back-enter-active {
  transition:
    opacity var(--ap-page-enter, 300ms) cubic-bezier(0.16, 1, 0.3, 1),
    transform var(--ap-page-enter, 300ms) cubic-bezier(0.16, 1, 0.3, 1);
}

.page-back-leave-active {
  transition:
    opacity var(--ap-page-leave, 200ms) ease-out,
    transform var(--ap-page-leave, 200ms) ease-out;
}

.page-back-enter-from {
  opacity: 0;
  transform: translateX(-24px);
}

.page-back-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

/* Respect reduced motion preference */
@media (prefers-reduced-motion: reduce) {
  .page-enter-active,
  .page-leave-active,
  .page-forward-enter-active,
  .page-forward-leave-active,
  .page-back-enter-active,
  .page-back-leave-active {
    transition: opacity 100ms ease;
  }

  .page-enter-from,
  .page-forward-enter-from,
  .page-back-enter-from {
    transform: none;
  }

  .page-leave-to,
  .page-forward-leave-to,
  .page-back-leave-to {
    transform: none;
  }
}
</style>
