<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { siteApi, type VisitorSummary } from '@/api/site'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
const stats = ref<VisitorSummary | null>(null)
const statsError = ref(false)

onMounted(async () => {
  try {
    const response = await siteApi.getVisitorSummary()
    stats.value = response.data || null
  } catch {
    statsError.value = true
  }
})
</script>

<template>
  <div class="dashboard">
    <header class="welcome-panel">
      <p class="eyebrow">AUTHOR WORKSPACE</p>
      <h1>欢迎回来</h1>
      <p>在这里发布文章、查看访客动态，管理 {{ siteStore.settings.siteTitle }} 的外观与内容。</p>
      <router-link :to="`${siteStore.adminRoot}/write`" class="primary-link"
        >开始写作 <span aria-hidden="true">↗</span></router-link
      >
      <span class="decorative-mark" aria-hidden="true">✳</span>
    </header>

    <section class="workspace-grid" aria-label="管理入口">
      <router-link :to="`${siteStore.adminRoot}/posts`" class="workspace-card">
        <span class="card-index">01</span>
        <span class="card-icon" aria-hidden="true">▤</span>
        <h2>文章管理</h2>
        <p>查看、编辑和整理已发布的文章。</p>
        <span class="card-link">管理文章 →</span>
      </router-link>
      <router-link :to="`${siteStore.adminRoot}/visitors`" class="workspace-card">
        <span class="card-index">02</span>
        <span class="card-icon" aria-hidden="true">◷</span>
        <h2>访客记录</h2>
        <p>了解访问量、访客趋势和大家正在阅读的页面。</p>
        <span class="card-link">查看访客 →</span>
      </router-link>
      <router-link :to="`${siteStore.adminRoot}/settings`" class="workspace-card workspace-card--subtle">
        <span class="card-index">03</span>
        <span class="card-icon" aria-hidden="true">⚙</span>
        <h2>网站设置</h2>
        <p>调整网站资料、评论开关、皮肤、相册和背景音乐。</p>
        <span class="card-link">配置网站 →</span>
      </router-link>
    </section>

    <section v-if="stats" class="summary-grid" aria-label="网站访问概况">
      <article><small>累计访客</small><strong>{{ stats.totalVisitors.toLocaleString() }}</strong></article>
      <article><small>今日访客</small><strong>{{ stats.todayVisitors.toLocaleString() }}</strong></article>
      <article><small>页面浏览</small><strong>{{ stats.totalPageViews.toLocaleString() }}</strong></article>
      <article><small>文章阅读</small><strong>{{ stats.articleViews.toLocaleString() }}</strong></article>
    </section>
    <p v-else-if="statsError" class="dashboard-note">访客统计暂时无法加载，可在“访客记录”页面重试。</p>

    <p class="dashboard-note">访客可以阅读和搜索公开文章；写作与内容管理仅对作者开放。</p>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1000px;
  margin: 0 auto;
}
.welcome-panel {
  position: relative;
  overflow: hidden;
  padding: clamp(27px, 5vw, 52px);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-lg);
  background: var(--surface-solid);
}
.eyebrow {
  margin: 0 0 10px;
  color: var(--primary);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.welcome-panel h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 2.8rem);
  letter-spacing: -0.045em;
}
.welcome-panel > p:not(.eyebrow) {
  max-width: 520px;
  margin: 12px 0 20px;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.75;
}
.primary-link {
  display: inline-flex;
  align-items: center;
  gap: 20px;
  min-height: 40px;
  padding: 0 15px;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: white;
  font-size: 0.82rem;
  font-weight: 600;
  text-decoration: none;
}
.primary-link:hover {
  background: var(--primary-hover);
}
.decorative-mark {
  position: absolute;
  right: 9%;
  top: 50%;
  transform: translateY(-50%);
  color: var(--primary);
  font-size: clamp(5rem, 14vw, 10rem);
  opacity: 0.08;
}
.workspace-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}
.workspace-card {
  position: relative;
  min-height: 198px;
  padding: 22px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  background: var(--surface-solid);
  color: var(--text-primary);
  text-decoration: none;
  transition:
    transform 150ms ease,
    border-color 150ms ease;
}
.workspace-card:hover {
  transform: translateY(-2px);
  border-color: var(--primary);
}
.workspace-card--subtle {
  background: var(--bg-secondary);
}
.card-index {
  position: absolute;
  top: 20px;
  right: 20px;
  color: var(--text-muted);
  font-size: 0.7rem;
}
.card-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius);
  background: var(--primary-light);
  color: var(--primary);
  font-size: 1.2rem;
}
.workspace-card h2 {
  margin: 18px 0 6px;
  font-family: var(--font-display);
  font-size: 1.1rem;
}
.workspace-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.65;
}
.card-link {
  display: inline-block;
  margin-top: 17px;
  color: var(--primary);
  font-size: 0.77rem;
  font-weight: 600;
}
.dashboard-note {
  margin: 20px 2px 0;
  color: var(--text-muted);
  font-size: 0.77rem;
}
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
.summary-grid article { display: grid; gap: 8px; padding: 18px; border: 1px solid var(--border-solid); border-radius: var(--radius); background: var(--surface-solid); }
.summary-grid small { color: var(--text-muted); font-size: .76rem; }
.summary-grid strong { font: 700 1.55rem var(--font-display); }
@media (max-width: 760px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
  .workspace-card {
    min-height: 0;
  }
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .decorative-mark {
    right: 4%;
  }
}
</style>
