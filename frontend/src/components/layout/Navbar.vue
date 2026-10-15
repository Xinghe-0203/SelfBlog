<template>
  <header class="site-nav-wrap" :class="{ 'is-scrolled': scrolled, 'is-hidden': hidden }">
    <nav class="site-nav" aria-label="主导航">
      <div class="nav-shell">
        <router-link to="/" class="brand" :aria-label="`${siteStore.settings.siteTitle} 首页`">
          <img
            v-if="siteStore.settings.siteLogoUrl"
            class="brand-mark brand-mark--image"
            :src="siteStore.settings.siteLogoUrl"
            alt=""
            aria-hidden="true"
          />
          <span v-else class="brand-mark" aria-hidden="true">
            <Icon name="logo" :size="16" />
          </span>
          <span class="brand-name">{{ siteStore.settings.siteTitle }}</span>
        </router-link>

        <div class="nav-links" aria-label="博客导航">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/archives" active-class="active">归档</router-link>
          <router-link to="/gallery" active-class="active">相册</router-link>
          <router-link to="/about" active-class="active">关于</router-link>
        </div>

        <form class="nav-search" role="search" @submit.prevent="search">
          <Icon name="search" :size="17" />
          <input v-model="searchQuery" type="search" placeholder="搜索文章" aria-label="搜索文章" />
          <kbd>↵</kbd>
        </form>

        <div class="nav-actions">
          <button
            class="icon-button theme-button"
            type="button"
            :aria-label="themeStore.isDarkMode ? '切换亮色模式' : '切换暗色模式'"
            @click="themeStore.toggleTheme"
          >
            <Icon :name="themeStore.isDarkMode ? 'sun' : 'moon'" :size="17" />
          </button>

          <button
            class="menu-toggle"
            type="button"
            :aria-expanded="mobileOpen"
            aria-label="切换导航菜单"
            @click="mobileOpen = !mobileOpen"
          >
            <Icon :name="mobileOpen ? 'close' : 'menu'" :size="18" />
          </button>
        </div>
      </div>

      <Transition name="mobile-nav">
        <div v-if="mobileOpen" class="mobile-panel">
          <form class="mobile-search" role="search" @submit.prevent="search">
            <input
              v-model="searchQuery"
              type="search"
              placeholder="搜索文章"
              aria-label="搜索文章"
            />
            <button type="submit">搜索</button>
          </form>
          <router-link to="/" exact-active-class="active" @click="mobileOpen = false">
            首页
          </router-link>
          <router-link to="/archives" active-class="active" @click="mobileOpen = false">
            归档
          </router-link>
          <router-link to="/gallery" active-class="active" @click="mobileOpen = false">
            相册
          </router-link>
          <router-link to="/about" active-class="active" @click="mobileOpen = false">
            关于
          </router-link>
        </div>
      </Transition>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useSiteStore } from '@/stores/site'
import { useScrollState } from '@/composables/useScrollState'
import Icon from '@/components/common/Icon.vue'

const router = useRouter()
const themeStore = useThemeStore()
const siteStore = useSiteStore()
const { scrolled, direction } = useScrollState(16)

const searchQuery = ref('')
const mobileOpen = ref(false)

/**
 * 小屏下滚时把顶栏收走，上滚立刻归位。
 * 未滚过阈值前不隐藏 —— 否则首屏滑一下就找不到导航了。
 */
const hidden = computed(() => scrolled.value && direction.value === 'down')

const search = () => {
  const query = searchQuery.value.trim()
  if (!query) return
  mobileOpen.value = false
  searchQuery.value = ''
  router.push({ path: '/search', query: { q: query } })
}
</script>

<style scoped>
.site-nav-wrap {
  position: fixed;
  z-index: var(--z-sticky);
  top: 14px;
  left: 0;
  right: 0;
  padding: 0 20px;
  transition:
    transform var(--transition-slow),
    opacity var(--transition-slow);
}

/* 小屏下滚收起、上滚归位。桌面端不隐藏，避免鼠标用户找不到导航。 */
@media (max-width: 760px) {
  .site-nav-wrap.is-hidden {
    transform: translateY(-140%);
    opacity: 0;
    pointer-events: none;
  }
}

.site-nav {
  position: relative;
  max-width: 1120px;
  margin: 0 auto;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-2xl);
  background: var(--surface-primary);
  box-shadow: 0 8px 30px rgba(24, 31, 40, 0.07);
  transition:
    background var(--transition-slow),
    box-shadow var(--transition-slow),
    backdrop-filter var(--transition-slow),
    border-color var(--transition-slow);
}

/* 滚过后转成毛玻璃实心条：高度向下收，顶边不动，内容零位移。 */
.is-scrolled .site-nav {
  border-color: transparent;
  background: var(--navbar-bg);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  box-shadow: 0 10px 34px rgba(24, 31, 40, 0.12);
}

.nav-shell {
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 18px;
  transition: min-height var(--transition-slow);
}

.is-scrolled .nav-shell {
  min-height: 48px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-width: max-content;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 27px;
  height: 27px;
  border-radius: var(--radius-sm);
  color: white;
  background: var(--primary);
  font-size: 1rem;
  flex: 0 0 auto;
}

.brand-mark--image {
  display: block;
  object-fit: cover;
  background: transparent;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 胶囊选中态：选中带浅底，未选中只有 hover 才出底，避免导航变成一排实心色块。 */
.nav-links a,
.mobile-panel a {
  padding: 7px 14px;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.nav-links a:hover,
.mobile-panel a:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.nav-links a.active,
.mobile-panel a.active {
  background: var(--primary-light);
  color: var(--primary);
}

.nav-search {
  display: flex;
  align-items: center;
  gap: 9px;
  width: min(260px, 24vw);
  margin-left: auto;
  padding: 8px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-full);
  background: var(--bg-primary);
  color: var(--text-muted);
}

.nav-search input,
.mobile-search input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 0.8125rem;
}

.nav-search input::placeholder,
.mobile-search input::placeholder {
  color: var(--text-muted);
}

.nav-search kbd {
  min-width: 20px;
  text-align: center;
  color: var(--text-muted);
  font: 0.7rem var(--font-sans);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: max-content;
}

.icon-button,
.menu-toggle {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.icon-button:hover,
.menu-toggle:hover {
  background: var(--bg-secondary);
  color: var(--primary);
}

.menu-toggle {
  display: none;
}

.mobile-panel {
  display: none;
}

.mobile-search {
  display: flex;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-solid);
}

.mobile-search input {
  padding: 9px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
}

.mobile-search button {
  /* 输入框 width:100% 会挤扁按钮，导致"搜索"两个字竖着折行。 */
  flex: 0 0 auto;
  white-space: nowrap;
  border: 0;
  border-radius: var(--radius-full);
  padding: 0 18px;
  background: var(--primary);
  color: white;
  font-weight: 600;
}

.mobile-nav-enter-active,
.mobile-nav-leave-active {
  transition:
    opacity 150ms ease,
    transform 150ms ease;
}

.mobile-nav-enter-from,
.mobile-nav-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 760px) {
  .site-nav-wrap {
    top: 8px;
    padding: 0 10px;
  }
  .nav-shell {
    min-height: 52px;
    gap: 12px;
    padding: 0 12px;
  }
  .nav-links,
  .nav-search {
    display: none;
  }
  .nav-actions {
    margin-left: auto;
  }
  .menu-toggle {
    display: grid;
  }
  .mobile-panel {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 10px 12px 14px;
    border-top: 1px solid var(--border-solid);
  }
  .mobile-panel > a {
    text-align: left;
  }
}

@media (prefers-reduced-motion: reduce) {
  .site-nav-wrap,
  .site-nav,
  .nav-shell {
    transition: none;
  }
  .site-nav-wrap.is-hidden {
    transform: none;
    opacity: 0;
  }
}

@media (max-width: 420px) {
  .brand {
    font-size: 0.96rem;
  }
  .brand-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
