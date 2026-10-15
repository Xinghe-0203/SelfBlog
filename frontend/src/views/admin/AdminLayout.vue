<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ 'sidebar-open': mobileOpen }">
      <div class="sidebar-brand">
        <span class="brand-mark" aria-hidden="true">
          <img
            v-if="siteStore.settings.siteLogoUrl"
            class="brand-mark--image"
            :src="siteStore.settings.siteLogoUrl"
            alt=""
          />
          <Icon v-else name="logo" :size="17" />
        </span>
        <div>
          <strong>{{ siteStore.settings.siteTitle }}</strong
          ><small>作者工作台</small>
        </div>
        <button class="close-menu" type="button" aria-label="关闭菜单" @click="mobileOpen = false">
          <Icon name="close" :size="18" />
        </button>
      </div>

      <nav class="sidebar-nav" aria-label="后台导航">
        <router-link :to="`${siteStore.adminRoot}/dashboard`" exact-active-class="active" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="gauge" :size="17" /></span>概览
        </router-link>
        <router-link :to="`${siteStore.adminRoot}/posts`" active-class="active" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="file-text" :size="17" /></span>文章管理
        </router-link>
        <router-link :to="`${siteStore.adminRoot}/tags`" active-class="active" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="tag" :size="17" /></span>标签管理
        </router-link>
        <router-link :to="`${siteStore.adminRoot}/visitors`" active-class="active" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="users" :size="17" /></span>访客记录
        </router-link>
        <router-link :to="`${siteStore.adminRoot}/comments`" active-class="active" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="message" :size="17" /></span>评论管理
        </router-link>
        <router-link :to="`${siteStore.adminRoot}/albums`" active-class="active" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="image" :size="17" /></span>相册管理
        </router-link>
        <router-link :to="`${siteStore.adminRoot}/settings`" active-class="active" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="settings" :size="17" /></span>网站设置
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" @click="closeOnMobile">
          <span class="nav-icon" aria-hidden="true"><Icon name="home" :size="15" /></span>返回博客
        </router-link>
      </div>
    </aside>

    <button
      v-if="mobileOpen"
      class="sidebar-overlay"
      type="button"
      aria-label="关闭导航菜单"
      @click="mobileOpen = false"
    ></button>

    <div class="workspace">
      <header class="workspace-header">
        <button
          class="menu-button"
          type="button"
          aria-label="打开导航菜单"
          @click="mobileOpen = true"
        >
          <Icon name="menu" :size="18" />
        </button>
        <span>{{ userStore.nickname || userStore.username || '作者管理' }}</span>
        <router-link to="/" class="public-link">打开博客 <Icon name="external" :size="13" /></router-link>
        <button class="logout-button" type="button" @click="logout">退出登录</button>
      </header>
      <div class="main-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSiteStore } from '@/stores/site'
import { useUserStore } from '@/stores/user'
import Icon from '@/components/common/Icon.vue'

const route = useRoute()
const router = useRouter()
const siteStore = useSiteStore()
const userStore = useUserStore()
const mobileOpen = ref(false)
const closeOnMobile = () => {
  mobileOpen.value = false
}
async function logout() {
  await userStore.logout()
  await router.replace(siteStore.adminRoot)
}
watch(() => route.path, closeOnMobile)
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
}
.sidebar {
  position: fixed;
  z-index: 100;
  inset: 0 auto 0 0;
  display: flex;
  flex-direction: column;
  width: 244px;
  padding: 22px 15px 16px;
  border-right: 1px solid var(--border-solid);
  background: var(--surface-primary);
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 52px;
  padding: 0 10px 20px;
  border-bottom: 1px solid var(--border-solid);
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 33px;
  height: 33px;
  overflow: hidden;
  border-radius: var(--radius);
  background: var(--primary);
  color: white;
  font-size: 1.1rem;
}
.brand-mark--image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.sidebar-brand div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.sidebar-brand strong {
  overflow: hidden;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 0.92rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sidebar-brand small {
  color: var(--text-muted);
  font-size: 0.7rem;
}
.close-menu {
  display: none;
  margin-left: auto;
  border: 0;
  background: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 1.5rem;
}
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding-top: 22px;
}
.sidebar-nav a,
.sidebar-footer a {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 43px;
  padding: 0 12px;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: 0.86rem;
  text-decoration: none;
  transition:
    color 150ms ease,
    background 150ms ease;
}
.sidebar-nav a:hover,
.sidebar-footer a:hover {
  background: var(--bg-secondary);
  color: var(--primary);
}
.sidebar-nav a.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}
.nav-icon {
  display: grid;
  place-items: center;
  width: 20px;
  color: currentColor;
  font-size: 1.1rem;
  text-align: center;
}
.sidebar-footer {
  margin-top: auto;
  padding-top: 15px;
  border-top: 1px solid var(--border-solid);
}
.workspace {
  min-height: 100vh;
  margin-left: 244px;
}
.workspace-header {
  position: sticky;
  z-index: 20;
  top: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 60px;
  padding: 0 30px;
  border-bottom: 1px solid var(--border-solid);
  background: var(--surface-primary);
  color: var(--text-secondary);
  font-size: 0.82rem;
}
.public-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}
.logout-button {
  padding: 7px 10px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.75rem;
  cursor: pointer;
}
.logout-button:hover { color: var(--error); border-color: var(--error); }
.menu-button {
  display: none;
  border: 0;
  background: none;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 1.2rem;
}
.main-content {
  max-width: 1220px;
  margin: 0 auto;
  padding: 34px 30px;
}
.sidebar-overlay {
  display: none;
}
@media (max-width: 760px) {
  .sidebar {
    transform: translateX(-105%);
    transition: transform 180ms ease;
    box-shadow: var(--shadow-lg);
  }
  .sidebar.sidebar-open {
    transform: translateX(0);
  }
  .close-menu,
  .menu-button {
    display: block;
  }
  .sidebar-overlay {
    position: fixed;
    z-index: 90;
    inset: 0;
    display: block;
    border: 0;
    background: rgba(12, 20, 25, 0.4);
  }
  .workspace {
    margin-left: 0;
  }
  .workspace-header {
    height: 54px;
    padding: 0 17px;
  }
  .main-content {
    padding: 23px 17px;
  }
}
</style>
