<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { siteApi, type VisitorSummary } from '@/api/site'
import { useSiteStore } from '@/stores/site'
import CountUp from '@/components/common/CountUp.vue'
import Icon from '@/components/common/Icon.vue'
import type { IconName } from '@/components/common/icons'

const siteStore = useSiteStore()
const stats = ref<VisitorSummary | null>(null)
const statsError = ref(false)

/**
 * 概览的四张数字卡。
 *
 * 「今日」那两张额外带一句说明（tone=today），因为「今日 25 / 累计 163」
 * 单独看两个数字没有意义，旁边一句才说得清它们的关系。
 */
const statCards = computed(() => {
  const value = stats.value
  if (!value) return []
  return [
    { label: '累计访客', value: value.totalVisitors, hint: '自建站以来', tone: 'total' },
    { label: '今日访客', value: value.todayVisitors, hint: '今天', tone: 'today' },
    { label: '页面浏览', value: value.totalPageViews, hint: '含重复浏览', tone: 'total' },
    { label: '文章阅读', value: value.articleViews, hint: '正文页打开次数', tone: 'total' }
  ]
})
const workspaceLinks: Array<{
  segment: string
  label: string
  description: string
  action: string
  icon: IconName
}> = [
  {
    segment: 'posts',
    label: '文章管理',
    description: '查看、编辑和整理已发布的文章。',
    action: '管理文章',
    icon: 'file-text'
  },
  {
    segment: 'pages',
    label: '页面管理',
    description: '自定义首页、关于页与导航，以及站点其它独立页面。',
    action: '管理页面',
    icon: 'home'
  },
  {
    segment: 'health',
    label: '内容体检',
    description: '让模型通读最近的已发布文章，给出内容质量评分与可执行的改进建议。',
    action: '开始体检',
    icon: 'gauge'
  },
  {
    segment: 'visitors',
    label: '访客记录',
    description: '了解访问量、访客趋势和大家正在阅读的页面。',
    action: '查看访客',
    icon: 'users'
  },
  {
    segment: 'settings',
    label: '网站设置',
    description: '调整网站资料、评论开关、皮肤、相册和背景音乐。',
    action: '配置网站',
    icon: 'settings'
  }
]

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
      <div>
        <h1>欢迎回来</h1>
        <p>在这里发布文章、查看访客动态，管理 {{ siteStore.settings.siteTitle }} 的外观与内容。</p>
      </div>
      <router-link :to="`${siteStore.adminRoot}/write`" class="primary-link"
        ><Icon name="plus" :size="16" />开始写作</router-link
      >
    </header>

    <section class="workspace-grid" aria-label="管理入口">
      <router-link
        v-for="link in workspaceLinks"
        :key="link.segment"
        :to="`${siteStore.adminRoot}/${link.segment}`"
        class="workspace-card"
      >
        <span class="card-icon" aria-hidden="true"><Icon :name="link.icon" :size="19" /></span>
        <h2>{{ link.label }}</h2>
        <p>{{ link.description }}</p>
        <span class="card-link">{{ link.action }}<Icon name="chevron-right" :size="14" /></span>
      </router-link>
    </section>

    <section v-if="stats" class="summary-grid" aria-label="网站访问概况">
      <article v-for="stat in statCards" :key="stat.label">
        <small>{{ stat.label }}</small>
        <strong><CountUp :value="stat.value" /></strong>
        <span class="stat-trend" :class="`stat-trend--${stat.tone}`">{{ stat.hint }}</span>
      </article>
    </section>
    <p v-else-if="statsError" class="dashboard-note">
      访客统计暂时无法加载，可在“访客记录”页面重试。
    </p>

    <p class="dashboard-note">访客可以阅读和搜索公开文章；写作与内容管理仅对作者开放。</p>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1000px;
  margin: 0 auto;
}
.welcome-panel {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  padding: 26px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.welcome-panel h1 {
  margin: 0;
  font-size: var(--text-2xl);
}
.welcome-panel > div > p {
  max-width: 520px;
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.8;
}
.primary-link {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 44px;
  padding-inline: 14px;
  border-radius: var(--radius);
  background: var(--accent);
  color: var(--text-inverse);
  font-size: var(--text-sm);
  font-weight: 600;
}
.primary-link:hover {
  background: var(--accent-dark);
}
.workspace-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}
.workspace-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 176px;
  padding: 20px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  color: var(--text-primary);
}
.workspace-card {
  transition:
    border-color var(--transition-fast),
    transform var(--duration-slow) var(--ease-out),
    box-shadow var(--duration-slow) var(--ease-out);
}
.workspace-card:hover {
  border-color: var(--accent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
/* 图标在悬停时轻轻转一下，与前台的品牌图标是同一套小动作。 */
.card-icon {
  transition:
    background var(--transition-fast),
    transform var(--duration-slow) var(--ease-spring);
}
.workspace-card:hover .card-icon {
  transform: scale(1.06) rotate(-6deg);
}
.card-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  margin-bottom: 8px;
  border-radius: var(--radius);
  background: var(--accent-light);
  color: var(--accent);
}
.workspace-card h2 {
  font-size: var(--text-base);
}
.workspace-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.8;
}
.card-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: auto;
  padding-top: 14px;
  color: var(--accent);
  font-size: var(--text-xs);
  font-weight: 600;
}
.dashboard-note {
  margin: 18px 2px 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.summary-grid article {
  display: grid;
  gap: 7px;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
  transition:
    border-color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}
.summary-grid article:hover {
  border-color: color-mix(in srgb, var(--border-light) 55%, var(--accent));
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}
.summary-grid small {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.summary-grid strong {
  font-size: var(--text-2xl);
  font-variant-numeric: tabular-nums;
}
/* 数字下方的一句说明：让「累计」与「今日」的区别一眼可见。 */
.stat-trend {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.stat-trend--today {
  /* accent 压在（玻璃感的）半透明卡片上只有 4.47，深一档即可过 AA。 */
  color: var(--accent-dark);
  font-weight: 600;
}
@media (max-width: 860px) {
  .workspace-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .workspace-card {
    min-height: 0;
  }
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 600px) {
  .welcome-panel {
    align-items: stretch;
    flex-direction: column;
    padding: 20px;
  }
  .primary-link {
    justify-content: center;
  }
}
</style>
