<template>
  <div id="selfblog" :class="{ 'dark-mode': isDarkMode }">
    <a href="#main-content" class="skip-link">跳转到主要内容</a>
    <Navbar v-if="!isAdminPage" />
    <main id="main-content" class="app-main" :class="{ 'app-main--admin': isAdminPage }">
      <router-view v-slot="{ Component, route }">
        <Transition
          :name="transitionName"
          mode="out-in"
          @before-enter="onBeforeEnter"
          @after-enter="onAfterEnter"
        >
          <div :key="route.path" class="page-wrapper">
            <component :is="Component" />
          </div>
        </Transition>
      </router-view>
    </main>
    <Footer v-if="!isAdminPage" />
    <MusicPlayer v-if="!isAdminPage" />
    <BackToTop />
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { destroyLogger } from '@/utils/logger'
import Navbar from '@/components/layout/Navbar.vue'
import Footer from '@/components/layout/Footer.vue'
import BackToTop from '@/components/common/BackToTop.vue'
import Toast from '@/components/common/Toast.vue'
import MusicPlayer from '@/components/layout/MusicPlayer.vue'
import { useSiteStore } from '@/stores/site'
import { siteApi } from '@/api/site'

const route = useRoute()
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

// Scroll to top on route change (smooth for same-depth, instant for deep navigation)
const onBeforeEnter = () => {
  window.scrollTo({ top: 0, behavior: 'instant' })
}

const onAfterEnter = () => {
  // Restore smooth scroll after transition
  document.documentElement.style.scrollBehavior = 'smooth'
}

onMounted(async () => {
  themeStore.initTheme()
  applySiteTheme()
  if (!isAdminPage.value) void recordVisitor()
})

watch(() => route.path, () => {
  if (!isAdminPage.value) void recordVisitor()
})

watch(() => [siteStore.settings.themeSkin, siteStore.settings.themePrimaryColor] as const, applySiteTheme)

function applySiteTheme() {
  const root = document.getElementById('selfblog')
  if (!root) return
  root.dataset.skin = siteStore.settings.themeSkin || 'ocean'
  root.style.setProperty('--primary', siteStore.settings.themePrimaryColor || '#165DFF')
  root.style.setProperty('--accent', siteStore.settings.themePrimaryColor || '#165DFF')
}

function recordVisitor() {
  const referrer = document.referrer ? new URL(document.referrer).href : ''
  return siteApi.trackVisitor(route.fullPath, referrer).catch(() => undefined)
}

onUnmounted(() => {
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
  font-size: 0.875rem;
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

/* Standard page transition */
.page-enter-active {
  transition:
    opacity 280ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 280ms cubic-bezier(0.16, 1, 0.3, 1);
}

.page-leave-active {
  transition:
    opacity 160ms ease-out,
    transform 160ms ease-out;
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
    opacity 300ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 300ms cubic-bezier(0.16, 1, 0.3, 1);
}

.page-forward-leave-active {
  transition:
    opacity 200ms ease-out,
    transform 200ms ease-out;
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
    opacity 300ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 300ms cubic-bezier(0.16, 1, 0.3, 1);
}

.page-back-leave-active {
  transition:
    opacity 200ms ease-out,
    transform 200ms ease-out;
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
