<template>
  <header class="site-nav-wrap">
    <nav class="site-nav" aria-label="主导航">
      <div class="nav-shell">
        <router-link to="/" class="brand" :aria-label="`${siteStore.settings.siteTitle} 首页`">
          <span class="brand-mark" aria-hidden="true">✳</span>
          <span>{{ siteStore.settings.siteTitle }}</span>
        </router-link>

        <div class="nav-links" aria-label="博客导航">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/archives" active-class="active">归档</router-link>
          <router-link to="/gallery" active-class="active">相册</router-link>
          <router-link to="/about" active-class="active">关于</router-link>
        </div>

        <form class="nav-search" role="search" @submit.prevent="search">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m16 16 4 4" />
          </svg>
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
            <svg v-if="!themeStore.isDarkMode" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20.5 15.5A8.5 8.5 0 0 1 8.5 3.5 8.5 8.5 0 1 0 20.5 15.5Z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="4" />
              <path
                d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"
              />
            </svg>
          </button>

          <button
            class="menu-toggle"
            type="button"
            :aria-expanded="mobileOpen"
            aria-label="切换导航菜单"
            @click="mobileOpen = !mobileOpen"
          >
            <span></span><span></span>
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
          <router-link to="/" @click="mobileOpen = false">首页</router-link>
          <router-link to="/archives" @click="mobileOpen = false">归档</router-link>
          <router-link to="/gallery" @click="mobileOpen = false">相册</router-link>
          <router-link to="/about" @click="mobileOpen = false">关于</router-link>
        </div>
      </Transition>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useSiteStore } from '@/stores/site'

const router = useRouter()
const themeStore = useThemeStore()
const siteStore = useSiteStore()
const searchQuery = ref('')
const mobileOpen = ref(false)

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
}

.site-nav {
  position: relative;
  max-width: 1120px;
  margin: 0 auto;
  border: 1px solid var(--border-solid);
  border-radius: 16px;
  background: var(--surface-primary);
  box-shadow: 0 8px 30px rgba(24, 31, 40, 0.07);
}

.nav-shell {
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 18px;
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
  border-radius: 9px;
  color: white;
  background: var(--primary);
  font-size: 1rem;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 22px;
}

.nav-links a,
.mobile-panel a,
.mobile-panel > button {
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
  transition: color 160ms ease;
}

.nav-links a:hover,
.nav-links a.active,
.mobile-panel a:hover {
  color: var(--primary);
}

.nav-search {
  display: flex;
  align-items: center;
  gap: 9px;
  width: min(260px, 24vw);
  margin-left: auto;
  padding: 8px 10px;
  border: 1px solid var(--border-solid);
  border-radius: 9px;
  background: var(--bg-primary);
  color: var(--text-muted);
}

.nav-search svg,
.icon-button svg {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex: 0 0 auto;
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
  gap: 12px;
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
  border-radius: 9px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

.icon-button:hover,
.menu-toggle:hover {
  background: var(--bg-secondary);
  color: var(--primary);
}

.write-link,
.admin-link,
.login-link,
.logout-link {
  color: var(--text-secondary);
  font-size: 0.8125rem;
  font-weight: 600;
  text-decoration: none;
}

.write-link,
.login-link {
  color: var(--primary);
}

.write-link:hover,
.admin-link:hover,
.login-link:hover,
.logout-link:hover {
  color: var(--primary-hover);
}

.logout-link {
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
  font-family: inherit;
}

.menu-toggle {
  display: none;
  align-content: center;
  gap: 5px;
}

.menu-toggle span {
  width: 17px;
  height: 1.5px;
  background: currentColor;
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
  padding: 9px 10px;
  border: 1px solid var(--border-solid);
  border-radius: 8px;
}

.mobile-search button {
  border: 0;
  border-radius: 8px;
  padding: 0 12px;
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
  .nav-search,
  .admin-link,
  .logout-link {
    display: none;
  }
  .nav-actions {
    gap: 6px;
    margin-left: auto;
  }
  .menu-toggle {
    display: grid;
  }
  .mobile-panel {
    display: flex;
    flex-direction: column;
    gap: 15px;
    padding: 14px 16px 17px;
    border-top: 1px solid var(--border-solid);
  }
  .mobile-panel > a,
  .mobile-panel > button {
    padding: 2px 0;
    text-align: left;
  }
  .mobile-panel > button {
    border: 0;
    background: transparent;
    cursor: pointer;
    font-family: inherit;
  }
}

@media (max-width: 420px) {
  .brand {
    font-size: 0.96rem;
  }
  .write-link {
    font-size: 0.75rem;
  }
}
</style>
