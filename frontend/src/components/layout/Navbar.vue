<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useSiteStore } from '@/stores/site'
import { useMusicStore } from '@/stores/music'
import { useScrollState } from '@/composables/useScrollState'
import Icon from '@/components/common/Icon.vue'
import type { IconName } from '@/components/common/icons'
import NavbarMusicControl from './NavbarMusicControl.vue'
import { usePagesStore } from '@/stores/pages'
import { fitNavigation } from '@/utils/navigationFit'

const router = useRouter()
const route = useRoute()
const themeStore = useThemeStore()
const siteStore = useSiteStore()
const music = useMusicStore()
const pages = usePagesStore()
const { scrolled } = useScrollState(16)
const root = ref<HTMLElement | null>(null)
const searchQuery = ref('')
const panel = ref<'menu' | 'search' | 'music' | 'more' | null>(null)
let opener: HTMLElement | null = null
const pageIcons: Record<string, IconName> = {
  home: 'home',
  archives: 'archive',
  gallery: 'image',
  about: 'info'
}
const links = computed(() =>
  pages.navigation.map((link) => ({
    ...link,
    icon: pageIcons[link.pageId] || ('file-text' as IconName)
  }))
)
const visibleCount = ref(4)
const visibleLinks = computed(() => links.value.slice(0, visibleCount.value))
const overflowLinks = computed(() => links.value.slice(visibleCount.value))
let resizeObserver: ResizeObserver | undefined
function fitLinks(): void {
  const shell = root.value?.querySelector<HTMLElement>('.nav-shell')
  if (!shell || !shell.clientWidth) {
    visibleCount.value = Math.min(4, links.value.length)
    return
  }
  const style = getComputedStyle(shell)
  const brand = shell.querySelector<HTMLElement>('.brand')
  const search = shell.querySelector<HTMLElement>('.nav-search')
  const tools = shell.querySelector<HTMLElement>('.nav-actions')
  const gap = Number.parseFloat(style.columnGap) || 0
  const searchStyle = search ? getComputedStyle(search) : undefined
  const searchWidth =
    searchStyle?.display !== 'none'
      ? Number.parseFloat(searchStyle?.flexBasis || '0') ||
        search?.getBoundingClientRect().width ||
        0
      : 0
  const available =
    shell.clientWidth -
    Number.parseFloat(style.paddingLeft) -
    Number.parseFloat(style.paddingRight) -
    (brand?.getBoundingClientRect().width || 0) -
    searchWidth -
    (tools?.getBoundingClientRect().width || 0) -
    gap * (searchWidth ? 3 : 2)
  const widths = [...(root.value?.querySelectorAll<HTMLElement>('.nav-measure-link') || [])].map(
    (node) => node.getBoundingClientRect().width
  )
  const moreWidth =
    root.value?.querySelector<HTMLElement>('.nav-measure-more')?.getBoundingClientRect().width || 68
  visibleCount.value = fitNavigation(widths, available, moreWidth)
}
watch(
  links,
  async () => {
    await nextTick()
    fitLinks()
  },
  { deep: true }
)

function closePanel(restoreFocus = false): void {
  if (!panel.value) return
  panel.value = null
  if (restoreFocus && opener?.isConnected) opener.focus({ preventScroll: true })
}

async function togglePanel(
  next: NonNullable<typeof panel.value>,
  trigger: HTMLElement
): Promise<void> {
  if (panel.value === next) {
    closePanel(true)
    return
  }
  opener = trigger
  panel.value = next
  await nextTick()
  if (panel.value !== next) return
  const id = {
    menu: 'nav-mobile-panel',
    music: 'nav-music-panel',
    search: 'nav-search-panel',
    more: 'nav-more-panel'
  }[next]
  const activePanel = root.value?.querySelector<HTMLElement>(`#${id}:not([inert])`)
  activePanel?.querySelector<HTMLElement>('input, button, a')?.focus({ preventScroll: true })
}

function search(): void {
  const query = searchQuery.value.trim()
  if (!query) return
  closePanel()
  searchQuery.value = ''
  void router.push({ path: '/search', query: { q: query } })
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Escape' || !panel.value) return
  event.preventDefault()
  closePanel(true)
}
function onOutside(event: Event): void {
  if (event.target instanceof Node && !root.value?.contains(event.target)) closePanel()
}
const onDialog = () => closePanel()
const onResize = () => {
  closePanel()
  fitLinks()
}
watch(
  () => route.fullPath,
  () => closePanel()
)
watch(
  () => music.available,
  (available) => {
    if (!available && panel.value === 'music') closePanel()
  }
)
onMounted(() => {
  document.addEventListener('pointerdown', onOutside)
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('selfblog:dialog-open', onDialog)
  window.addEventListener('resize', onResize)
  void nextTick(() => {
    fitLinks()
    if (typeof ResizeObserver !== 'undefined' && root.value) {
      resizeObserver = new ResizeObserver(fitLinks)
      root.value
        .querySelectorAll('.nav-shell, .brand, .nav-search, .nav-actions')
        .forEach((node) => resizeObserver?.observe(node))
    }
  })
  void document.fonts?.ready.then(() => {
    if (root.value) fitLinks()
  })
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onOutside)
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('selfblog:dialog-open', onDialog)
  window.removeEventListener('resize', onResize)
  resizeObserver?.disconnect()
})
</script>

<template>
  <header ref="root" class="site-nav-wrap" :class="{ 'is-scrolled': scrolled }">
    <nav class="site-nav functional-glass" aria-label="主导航">
      <div class="nav-shell">
        <router-link to="/" class="brand" :aria-label="`${siteStore.settings.siteTitle} 首页`">
          <img
            v-if="siteStore.settings.siteLogoUrl"
            class="brand-mark brand-mark--image"
            :src="siteStore.settings.siteLogoUrl"
            alt=""
          />
          <span v-else class="brand-mark" aria-hidden="true"
            ><Icon name="sparkle" :size="21"
          /></span>
          <span class="brand-name">{{ siteStore.settings.siteTitle }}</span>
        </router-link>

        <form class="nav-search" role="search" @submit.prevent="search" @focusin="closePanel()">
          <Icon name="search" :size="16" />
          <input v-model="searchQuery" type="search" placeholder="搜索文章" aria-label="搜索文章" />
          <kbd aria-hidden="true">↵</kbd>
        </form>

        <div class="nav-links" aria-label="博客导航">
          <router-link
            v-for="link in visibleLinks"
            :key="link.to"
            :to="link.to"
            active-class="active"
            exact-active-class="active"
          >
            <Icon :name="link.icon" :size="16" /><span>{{ link.label }}</span>
          </router-link>
          <button
            v-if="overflowLinks.length"
            class="nav-more"
            type="button"
            aria-label="更多页面"
            aria-controls="nav-more-panel"
            :aria-expanded="panel === 'more'"
            :class="{ active: overflowLinks.some((link) => link.to === route.path) }"
            @click="togglePanel('more', $event.currentTarget as HTMLElement)"
          >
            更多<Icon class="more-chevron" name="chevron-right" :size="14" />
          </button>
        </div>

        <div class="nav-actions">
          <button
            type="button"
            class="nav-tool search-toggle"
            aria-label="打开搜索"
            aria-controls="nav-search-panel"
            :aria-expanded="panel === 'search'"
            @click="togglePanel('search', $event.currentTarget as HTMLElement)"
          >
            <Icon name="search" :size="18" />
          </button>
          <NavbarMusicControl
            :open="panel === 'music'"
            @toggle="togglePanel('music', $event)"
            @close="closePanel(true)"
          />
          <button
            type="button"
            class="nav-tool theme-button"
            :aria-label="themeStore.isDarkMode ? '切换亮色模式' : '切换暗色模式'"
            @click="themeStore.toggleTheme"
          >
            <Icon :name="themeStore.isDarkMode ? 'sun' : 'moon'" :size="18" />
          </button>
          <button
            type="button"
            class="nav-tool menu-toggle"
            aria-controls="nav-mobile-panel"
            :aria-expanded="panel === 'menu'"
            aria-label="切换导航菜单"
            @click="togglePanel('menu', $event.currentTarget as HTMLElement)"
          >
            <Icon :name="panel === 'menu' ? 'close' : 'menu'" :size="19" />
          </button>
        </div>
      </div>

      <div class="nav-measure" aria-hidden="true" inert>
        <span v-for="link in links" :key="link.pageId" class="nav-measure-link"
          ><Icon :name="link.icon" :size="16" />{{ link.label }}</span
        ><span class="nav-measure-more nav-more"
          >更多<Icon class="more-chevron" name="chevron-right" :size="14"
        /></span>
      </div>
      <Transition name="nav-panel" @before-leave="$event.setAttribute('inert', '')"
        ><div
          v-if="panel === 'more'"
          id="nav-more-panel"
          class="more-panel nav-panel functional-glass"
          data-nav-panel
          role="region"
          aria-label="更多页面"
        >
          <router-link
            v-for="link in overflowLinks"
            :key="link.to"
            :to="link.to"
            active-class="active"
            exact-active-class="active"
            @click="closePanel()"
            ><Icon :name="link.icon" :size="18" />{{ link.label }}</router-link
          >
        </div></Transition
      >

      <Transition name="nav-panel" @before-leave="$event.setAttribute('inert', '')">
        <div
          v-if="panel === 'search'"
          id="nav-search-panel"
          class="search-panel nav-panel functional-glass"
          data-nav-panel
          role="region"
          aria-label="文章搜索"
        >
          <form class="panel-search" role="search" @submit.prevent="search">
            <input
              v-model="searchQuery"
              type="search"
              placeholder="搜索文章"
              aria-label="搜索文章"
            />
            <button type="submit" class="btn btn-primary">搜索</button>
          </form>
        </div>
      </Transition>
      <Transition name="nav-panel" @before-leave="$event.setAttribute('inert', '')">
        <div
          v-if="panel === 'menu'"
          id="nav-mobile-panel"
          class="mobile-panel nav-panel functional-glass"
          data-nav-panel
        >
          <form class="panel-search mobile-search" role="search" @submit.prevent="search">
            <input
              v-model="searchQuery"
              type="search"
              placeholder="搜索文章"
              aria-label="搜索文章"
            />
            <button type="submit" class="btn btn-primary">搜索</button>
          </form>
          <router-link
            v-for="link in links"
            :key="link.to"
            :to="link.to"
            active-class="active"
            exact-active-class="active"
            @click="closePanel()"
          >
            <Icon :name="link.icon" :size="18" />{{ link.label }}
          </router-link>
          <button type="button" class="menu-theme" @click="themeStore.toggleTheme">
            <Icon :name="themeStore.isDarkMode ? 'sun' : 'moon'" :size="18" />{{
              themeStore.isDarkMode ? '切换亮色模式' : '切换暗色模式'
            }}
          </button>
        </div>
      </Transition>
    </nav>
  </header>
</template>

<style scoped>
.site-nav-wrap {
  position: fixed;
  z-index: var(--z-sticky);
  top: 16px;
  inset-inline: 16px;
}
.site-nav {
  position: relative;
  max-width: 1200px;
  margin-inline: auto;
  border-radius: var(--radius-full);
  box-shadow:
    var(--shadow-xs),
    inset 0 1px 0 var(--functional-glass-edge);
}
.is-scrolled .site-nav {
  box-shadow:
    var(--shadow-md),
    inset 0 1px 0 var(--functional-glass-edge);
}
.nav-shell {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 50px;
  padding-inline: 18px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-shrink: 1;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 650;
}
.brand-mark {
  display: grid;
  flex: 0 0 23px;
  place-items: center;
  width: 23px;
  height: 27px;
  color: var(--accent);
}
.brand-mark--image {
  object-fit: contain;
  border-radius: var(--radius-xs);
}
.brand-name {
  overflow: hidden;
  max-width: 128px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-search {
  display: flex;
  flex: 0 1 208px;
  align-items: center;
  gap: 8px;
  width: 208px;
  min-width: 128px;
  height: 34px;
  padding-inline: 11px;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  background: var(--bg-primary);
  color: var(--text-muted);
  transition: flex-basis var(--transition);
}
.nav-search:focus-within {
  flex-basis: 242px;
  border-color: var(--accent);
  outline: 3px solid var(--accent-light);
  background: var(--surface-primary);
}
.nav-search input,
.panel-search input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
}
.nav-search input::placeholder,
.panel-search input::placeholder {
  color: var(--text-muted);
}
.nav-search kbd {
  font: 10px var(--font-sans);
}
.nav-links {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
}
/* Keep the approved 34px capsule inside a full-height keyboard/touch target. */
.nav-links a,
.nav-measure-link {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 44px;
  padding-inline: 12px;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}
.nav-measure {
  position: absolute;
  left: -10000px;
  top: 0;
  display: flex;
  width: max-content;
  visibility: hidden;
  pointer-events: none;
}
.nav-more {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  min-width: 44px;
  height: 44px;
  padding-inline: 12px;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}
.nav-more:hover,
.nav-more[aria-expanded='true'],
.nav-more.active {
  color: var(--accent);
  background: var(--accent-light);
}
.more-chevron {
  transform: rotate(90deg);
}
.more-panel {
  display: grid;
  gap: 4px;
}
.more-panel > a {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding-inline: 12px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  font-size: 14px;
  overflow-wrap: anywhere;
}
.more-panel > a:hover,
.more-panel > a.active {
  color: var(--accent);
  background: var(--accent-light);
}
.nav-links a::before {
  position: absolute;
  content: '';
  z-index: -1;
  inset: 5px 0;
  border-radius: inherit;
  transition: background var(--transition-fast);
}
.nav-links a::after {
  position: absolute;
  content: '';
  inset-inline: 36%;
  bottom: 6px;
  height: 2px;
  border-radius: var(--radius-full);
  background: var(--accent);
  transform: scaleX(0);
  transition: transform var(--transition-fast);
}
.nav-links a .icon {
  opacity: 0.55;
}
.nav-links a.active,
.nav-links a:hover {
  color: var(--accent);
}
.nav-links a.active::before,
.nav-links a:hover::before {
  background: var(--accent-light);
}
.nav-links a.active .icon,
.nav-links a:hover .icon {
  opacity: 1;
}
.nav-links a.active::after,
.nav-links a:hover::after {
  transform: scaleX(1);
}
.nav-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
  margin-left: auto;
}
.nav-tool {
  display: grid;
  flex: 0 0 44px;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: var(--radius);
  color: var(--text-secondary);
}
.nav-tool:hover,
.nav-tool[aria-expanded='true'] {
  color: var(--accent);
  background: var(--accent-light);
}
.search-toggle,
.menu-toggle {
  display: none;
}
.nav-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: var(--z-nav-panel);
  width: min(340px, calc(100vw - 32px));
  padding: 14px;
  border-radius: var(--radius-xl);
  max-height: calc(100dvh - 90px);
  overflow-y: auto;
  overscroll-behavior: contain;
}
.panel-search {
  display: flex;
  gap: 8px;
}
.panel-search input {
  padding: 10px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  background: var(--surface-primary);
  font-size: 16px;
}
.panel-search input:focus-visible {
  outline: 2px solid var(--accent);
}
.panel-search button {
  flex-shrink: 0;
  min-height: 44px;
}
.mobile-panel {
  display: grid;
  gap: 4px;
}
.mobile-search {
  margin-bottom: 8px;
}
.mobile-panel > a,
.menu-theme {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding-inline: 12px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  font-size: 14px;
  text-align: left;
}
.mobile-panel > a.active,
.mobile-panel > a:hover,
.menu-theme:hover {
  background: var(--accent-light);
  color: var(--accent);
}
.menu-theme {
  border-top: 1px solid var(--border-light);
  margin-top: 6px;
}
.nav-panel-enter-active,
.nav-panel-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms var(--ease-out);
}
.nav-panel-enter-from,
.nav-panel-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
@media (max-width: 1100px) {
  .nav-search {
    flex-basis: 160px;
    width: 160px;
  }
  .nav-search:focus-within {
    flex-basis: 184px;
  }
  .nav-shell {
    gap: 8px;
  }
}
@media (max-width: 1020px) {
  .nav-search {
    display: none;
  }
  .search-toggle {
    display: grid;
  }
  .nav-links a,
  .nav-measure-link {
    padding-inline: 10px;
  }
}
@media (max-width: 720px) {
  .site-nav-wrap {
    top: 10px;
    inset-inline: 10px;
  }
  .nav-shell {
    height: 48px;
    padding-inline: 10px;
    gap: 6px;
  }
  .brand {
    flex: 1 1 auto;
    gap: 6px;
    font-size: 13px;
  }
  .brand-name {
    max-width: 95px;
  }
  .nav-links {
    display: none;
  }
  .nav-actions {
    gap: 0;
    margin-left: auto;
  }
  .menu-toggle {
    display: grid;
  }
  .nav-panel {
    width: min(340px, calc(100vw - 20px));
  }
}
@media (max-width: 360px) {
  .search-toggle,
  .theme-button {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .nav-panel-enter-from,
  .nav-panel-leave-to {
    transform: none;
  }
}
</style>
