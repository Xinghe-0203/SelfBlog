<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSiteStore } from '@/stores/site'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useDialog } from '@/composables/useDialog'
import Icon from '@/components/common/Icon.vue'
import type { IconName } from '@/components/common/icons'

const route = useRoute()
const router = useRouter()
const siteStore = useSiteStore()
const userStore = useUserStore()
const themeStore = useThemeStore()
const mobileOpen = ref(false)
const isMobile = ref(window.innerWidth <= 860)
const modalOpen = computed(() => isMobile.value && mobileOpen.value)
// The drawer traps focus and Escape only while it is open over the workspace.
const drawer = ref<HTMLElement | null>(null)
const { layer } = useDialog(modalOpen, drawer, () => {
  mobileOpen.value = false
})

function updateViewport(): void {
  const wasOpen = modalOpen.value
  isMobile.value = window.innerWidth <= 860
  if (!isMobile.value) {
    mobileOpen.value = false
    // The mobile opener is hidden on desktop; restore to a visible destination.
    if (wasOpen) void nextTick(() => drawer.value?.querySelector<HTMLElement>('a.active')?.focus())
  }
}
onMounted(() => window.addEventListener('resize', updateViewport))
onBeforeUnmount(() => window.removeEventListener('resize', updateViewport))

const authorName = computed(() => userStore.nickname || userStore.username || '作者管理')
const navLinks: Array<{ segment: string; label: string; icon: IconName }> = [
  { segment: 'dashboard', label: '概览', icon: 'gauge' },
  { segment: 'posts', label: '文章管理', icon: 'file-text' },
  { segment: 'tags', label: '标签管理', icon: 'tag' },
  { segment: 'visitors', label: '访客记录', icon: 'users' },
  { segment: 'comments', label: '评论管理', icon: 'message' },
  { segment: 'albums', label: '相册管理', icon: 'image' },
  { segment: 'pages', label: '页面管理', icon: 'file-text' },
  { segment: 'seo', label: 'SEO 管理', icon: 'sparkle' },
  { segment: 'health', label: '内容体检', icon: 'gauge' },
  { segment: 'settings', label: '网站设置', icon: 'settings' }
]
const closeOnMobile = (): void => {
  mobileOpen.value = false
}

async function logout(): Promise<void> {
  await userStore.logout()
  await router.replace(siteStore.adminRoot)
}
watch(() => route.path, closeOnMobile)
</script>

<template>
  <div class="admin-layout">
    <Transition name="drawer-scrim">
      <button
        v-if="mobileOpen"
        class="sidebar-overlay"
        type="button"
        aria-label="关闭导航菜单"
        :style="{ zIndex: 60 + layer }"
        @click="closeOnMobile"
      ></button>
    </Transition>

    <aside
      id="admin-sidebar"
      ref="drawer"
      class="sidebar functional-glass"
      :class="{ 'sidebar-open': mobileOpen }"
      :style="{ zIndex: 70 + layer }"
      :inert="isMobile && !mobileOpen ? true : undefined"
      :role="modalOpen ? 'dialog' : undefined"
      :aria-modal="modalOpen ? 'true' : undefined"
      aria-label="作者导航菜单"
    >
      <div class="sidebar-brand">
        <span class="brand-mark" aria-hidden="true"
          ><img
            v-if="siteStore.settings.siteLogoUrl"
            class="brand-mark--image"
            :src="siteStore.settings.siteLogoUrl"
            alt="" /><Icon v-else name="logo" :size="17"
        /></span>
        <div>
          <strong>{{ siteStore.settings.siteTitle }}</strong
          ><small>作者工作台</small>
        </div>
        <button class="close-menu" type="button" aria-label="关闭菜单" @click="closeOnMobile">
          <Icon name="close" :size="18" />
        </button>
      </div>

      <nav class="sidebar-nav" aria-label="后台导航">
        <router-link
          v-for="link in navLinks"
          :key="link.segment"
          :to="`${siteStore.adminRoot}/${link.segment}`"
          active-class="active"
          @click="closeOnMobile"
        >
          <Icon :name="link.icon" :size="17" />{{ link.label }}
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="admin-theme-toggle" type="button" @click="themeStore.toggleTheme">
          <Icon :name="themeStore.isDarkMode ? 'sun' : 'moon'" :size="16" />{{
            themeStore.isDarkMode ? '切换亮色模式' : '切换暗色模式'
          }}
        </button>
        <router-link to="/" @click="closeOnMobile"
          ><Icon name="home" :size="16" />返回博客</router-link
        >
      </div>
    </aside>

    <div class="workspace">
      <header class="workspace-header">
        <button
          class="menu-button"
          type="button"
          aria-label="打开导航菜单"
          aria-controls="admin-sidebar"
          :aria-expanded="mobileOpen"
          @click="mobileOpen = true"
        >
          <Icon name="menu" :size="18" />
        </button>
        <span class="workspace-author">{{ authorName }}</span>
        <router-link to="/" class="public-link"
          >打开博客<Icon name="external" :size="13"
        /></router-link>
        <button class="logout-button" type="button" @click="logout">退出登录</button>
      </header>
      <div class="main-content"><router-view /></div>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
}
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  display: flex;
  flex-direction: column;
  width: 236px;
  padding: 18px 14px 14px;
  border-right: 1px solid var(--border-light);
  border-radius: 0;
}
.sidebar::after {
  border-radius: 0;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 48px;
  padding: 0 8px 16px;
  border-bottom: 1px solid var(--border-light);
}
.brand-mark {
  display: grid;
  flex: 0 0 32px;
  place-items: center;
  width: 32px;
  height: 32px;
  overflow: hidden;
  border-radius: var(--radius);
  background: var(--accent-light);
  color: var(--accent);
}
.brand-mark--image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: transparent;
}
.sidebar-brand div {
  display: grid;
  gap: 3px;
  min-width: 0;
}
.sidebar-brand strong {
  overflow: hidden;
  font-size: var(--text-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sidebar-brand small {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.close-menu {
  display: none;
  flex: 0 0 44px;
  place-items: center;
  width: 44px;
  height: 44px;
  margin-left: auto;
  margin-right: -8px;
  border-radius: var(--radius);
  color: var(--text-secondary);
}
.close-menu:hover {
  background: var(--bg-secondary);
}
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-top: 14px;
}
.sidebar-nav a,
.sidebar-footer a,
.admin-theme-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding-inline: 11px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  text-align: left;
}
.sidebar-nav a:hover,
.sidebar-footer a:hover,
.admin-theme-toggle:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}
.sidebar-nav a.active {
  background: var(--accent-light);
  color: var(--accent);
  font-weight: 600;
}
.sidebar-footer {
  display: grid;
  gap: 3px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
.workspace {
  min-height: 100vh;
  margin-left: 236px;
}
.workspace-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  padding-inline: 24px;
  border-bottom: 1px solid var(--border-light);
  background: var(--functional-glass-bg);
  backdrop-filter: blur(12px) saturate(1.1);
  -webkit-backdrop-filter: blur(12px) saturate(1.1);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.workspace-author {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.public-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  margin-left: auto;
  color: var(--accent);
  font-weight: 600;
}
.logout-button {
  min-height: 44px;
  padding-inline: 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.logout-button:hover {
  color: var(--error);
  border-color: var(--error);
}
.menu-button {
  display: none;
  place-items: center;
  width: 44px;
  height: 44px;
  margin-left: -10px;
  border-radius: var(--radius);
  color: var(--text-primary);
}
.menu-button:hover {
  background: var(--bg-secondary);
}
.main-content {
  max-width: 1220px;
  margin-inline: auto;
  padding: 26px 24px 48px;
}
.sidebar-overlay {
  position: fixed;
  inset: 0;
  border: 0;
  background: var(--scrim);
}
.drawer-scrim-enter-active,
.drawer-scrim-leave-active {
  transition: opacity 180ms ease;
}
.drawer-scrim-enter-from,
.drawer-scrim-leave-to {
  opacity: 0;
}
@media (max-width: 860px) {
  .sidebar {
    transform: translateX(-105%);
    transition: transform 200ms var(--ease-out);
    box-shadow: var(--shadow-xl);
  }
  .sidebar.sidebar-open {
    transform: translateX(0);
  }
  .close-menu,
  .menu-button {
    display: grid;
  }
  .workspace {
    margin-left: 0;
  }
  .workspace-header {
    padding-inline: 16px;
  }
  .main-content {
    padding: 20px 16px 40px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .sidebar {
    transition: none;
  }
  .drawer-scrim-enter-from,
  .drawer-scrim-leave-to {
    opacity: 1;
  }
}
</style>
