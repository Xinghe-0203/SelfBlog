<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <router-link to="/" class="footer-title">{{ siteStore.settings.siteTitle }}</router-link>
        <p>{{ siteStore.settings.footerText }}</p>
        <nav class="footer-subscribe" aria-label="订阅">
          <a href="/feed.xml"><Icon name="rss" :size="15" />RSS</a>
          <a href="/atom.xml"><Icon name="rss" :size="15" />Atom</a>
          <a href="/sitemap.xml"><Icon name="layers" :size="15" />站点地图</a>
        </nav>
      </div>
      <nav class="footer-links" aria-label="页脚导航">
        <router-link to="/">首页</router-link>
        <router-link to="/archives">归档</router-link>
        <router-link to="/tags">标签</router-link>
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
import Icon from '@/components/common/Icon.vue'

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
  margin-top: 84px;
  border-top: 1px solid var(--border-light);
  background: var(--surface-secondary);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  max-width: 1130px;
  margin: 0 auto;
  padding: 30px 30px 34px;
}

.footer-brand {
  min-width: 0;
}

.footer-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 700;
  letter-spacing: -0.01em;
  text-decoration: none;
}

.footer-brand p,
.footer-inner small {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
}

/* 订阅入口：RSS / Atom / 站点地图。给读者一个明确的「持续订阅」出口，
   也让搜索引擎更容易发现订阅源。 */
.footer-subscribe {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.footer-subscribe a {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 3px 11px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  background: var(--surface-primary);
  color: var(--text-secondary);
  font-size: var(--text-2xs);
  text-decoration: none;
  transition:
    color var(--transition-fast),
    border-color var(--transition-fast);
}
.footer-subscribe a:hover {
  color: var(--accent);
  border-color: var(--accent);
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
  .site-footer {
    margin-top: 56px;
  }
  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
    padding: 26px 20px 30px;
  }
  .footer-inner small {
    margin-top: 0;
  }
  .footer-links {
    flex-wrap: wrap;
    gap: 14px;
  }
}
</style>
