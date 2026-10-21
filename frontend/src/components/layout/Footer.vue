<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <router-link to="/" class="footer-title">{{ siteStore.settings.siteTitle }}</router-link>
        <p>{{ siteStore.settings.footerText }}</p>
      </div>
      <nav class="footer-links" aria-label="页脚导航">
        <router-link to="/">首页</router-link>
        <router-link to="/archives">归档</router-link>
        <router-link to="/gallery">相册</router-link>
        <router-link to="/about">关于</router-link>
        <router-link to="/search">搜索</router-link>
      </nav>
      <small>© {{ currentYear }} {{ siteStore.settings.siteTitle }}</small>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteStore } from '@/stores/site'

const route = useRoute()
// 按路由路径缓存：跨年时用户在页内跳转一次就会拿到新的年份，而不是一直显示打开页面时的旧值。
const currentYear = computed(() => {
  void route.path
  return new Date().getFullYear()
})
const siteStore = useSiteStore()
</script>

<style scoped>
.site-footer {
  margin-top: 72px;
  border-top: 1px solid var(--border-solid);
  background: var(--surface-primary);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  max-width: 1130px;
  margin: 0 auto;
  padding: 24px 30px 30px;
}

.footer-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-weight: 700;
  text-decoration: none;
}

.footer-brand p,
.footer-inner small {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
}

.footer-links {
  display: flex;
  gap: 18px;
}

.footer-links a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  text-decoration: none;
}

.footer-links a:hover {
  color: var(--primary);
}

@media (max-width: 680px) {
  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
    padding: 24px 20px;
  }
  .footer-inner small {
    margin-top: 0;
  }
  .footer-links {
    flex-wrap: wrap;
  }
}
</style>
