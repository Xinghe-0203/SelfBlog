<template>
  <div class="visitors-page">
    <header class="page-heading"><div><h1>访客记录</h1><p>匿名统计访问趋势，不保存访客的真实 IP 地址。</p></div><button class="refresh" type="button" @click="load"><Icon name="refresh" :size="16" />刷新数据</button></header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div v-if="summary" class="metrics">
      <article><small>累计访客</small><strong>{{ summary.totalVisitors.toLocaleString() }}</strong></article>
      <article><small>今日访客</small><strong>{{ summary.todayVisitors.toLocaleString() }}</strong></article>
      <article><small>总页面浏览</small><strong>{{ summary.totalPageViews.toLocaleString() }}</strong></article>
      <article><small>今日页面浏览</small><strong>{{ summary.todayPageViews.toLocaleString() }}</strong></article>
      <article><small>文章阅读量</small><strong>{{ summary.articleViews.toLocaleString() }}</strong></article>
    </div>
    <section v-if="summary?.daily?.length" class="panel trend-panel">
      <div class="section-heading"><div><h2>近 14 天访问趋势</h2><p>柱状图按每日独立访客数显示</p></div></div>
      <div class="chart" role="img" aria-label="近14天每日访客趋势">
        <div v-for="day in summary.daily" :key="day.date" class="chart-day" :title="`${day.date}：${day.visitors} 位访客，${day.pageViews} 次浏览`">
          <strong>{{ day.visitors }}</strong><span class="bar" :style="{ height: `${Math.max(4, (day.visitors / maxVisitors) * 100)}%` }"></span><small>{{ day.date.slice(5) }}</small>
        </div>
      </div>
    </section>
    <div class="data-grid">
      <section class="panel"><div class="section-heading"><div><h2>热门页面</h2><p>最近 30 天</p></div></div>
        <div v-if="summary?.topPages?.length" class="top-pages"><div v-for="page in summary.topPages" :key="page.path"><code>{{ page.path }}</code><strong>{{ page.views }}</strong></div></div><p v-else class="empty">暂无访问数据</p>
      </section>
      <section class="panel record-panel"><div class="section-heading"><div><h2>最近访问</h2><p>访客标识与网络标识均为不可逆摘要</p></div></div>
        <div class="table-wrap"><table><thead><tr><th>时间</th><th>页面</th><th>访客</th><th>来源</th></tr></thead><tbody><tr v-for="row in records" :key="row.id"><td>{{ formatDate(row.time, 'MM-DD HH:mm') }}</td><td class="path">{{ row.path }}</td><td><code>{{ row.visitor }}</code></td><td>{{ row.referrer || '直接访问' }}</td></tr><tr v-if="!records.length"><td colspan="4" class="empty">暂无记录</td></tr></tbody></table></div>
        <div class="pagination"><button type="button" :disabled="page <= 1" @click="changePage(page - 1)"><Icon name="chevron-left" :size="15" />上一页</button><span>第 {{ page }} 页 / 共 {{ pages }} 页</span><button type="button" :disabled="page >= pages" @click="changePage(page + 1)">下一页<Icon name="chevron-right" :size="15" /></button></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { formatDate } from '@/utils'
import { siteApi, type VisitorRecord, type VisitorSummary } from '@/api/site'
import Icon from '@/components/common/Icon.vue'

const summary = ref<VisitorSummary | null>(null)
const records = ref<VisitorRecord[]>([])
const page = ref(1)
const pages = ref(1)
const error = ref('')
const maxVisitors = computed(() => Math.max(1, ...(summary.value?.daily || []).map((day) => day.visitors)))

async function load() {
  error.value = ''
  try {
    const [summaryResponse, recordsResponse] = await Promise.all([
      siteApi.getVisitorSummary(),
      siteApi.getVisitorRecords({ pageNum: page.value, pageSize: 20 })
    ])
    summary.value = summaryResponse.data || null
    records.value = recordsResponse.data?.records || []
    pages.value = Math.max(1, recordsResponse.data?.pages || 1)
  } catch {
    error.value = '访客数据暂时无法加载，请稍后重试。'
  }
}

function changePage(next: number) {
  page.value = next
  void load()
}

onMounted(() => void load())
</script>

<style scoped>
.visitors-page { display: grid; gap: 18px; color: var(--text-primary); }
.page-heading, .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.page-heading h1 { margin: 0 0 8px; font-size: 24px; }
.page-heading p:not(.eyebrow), .section-heading p { margin: 0; color: var(--text-muted); font-size: 13px; }
.refresh, .pagination button { display: inline-flex; align-items: center; gap: 5px; min-height: 44px; padding-inline: 12px; border: 1px solid var(--border-light); border-radius: var(--radius); background: var(--surface-primary); color: var(--text-primary); font-size: 13px; cursor: pointer; }
.refresh:hover, .pagination button:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.metrics { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 10px; }
.metrics article, .panel { border: 1px solid var(--border-light); border-radius: var(--radius-lg); background: var(--surface-primary); }
.metrics article { display: grid; gap: 8px; padding: 16px; }
.metrics small { color: var(--text-muted); font-size: 12px; }
.metrics strong { font-size: 22px; font-variant-numeric: tabular-nums; }
.panel { min-width: 0; padding: 20px; }
.section-heading { margin-bottom: 18px; }
.section-heading h2 { margin: 0 0 5px; font-size: 15px; }
.chart { display: grid; height: 210px; grid-template-columns: repeat(14, minmax(16px, 1fr)); align-items: end; gap: 8px; padding-top: 12px; }
.chart-day { display: flex; height: 100%; flex-direction: column; justify-content: end; align-items: center; gap: 7px; color: var(--text-muted); }
.chart-day strong { font-size: 11px; font-weight: 500; }
.bar { width: min(100%, 27px); min-height: 3px; border-radius: var(--radius-xs) var(--radius-xs) 2px 2px; background: linear-gradient(180deg, var(--accent), color-mix(in srgb, var(--accent) 45%, white)); }
.chart-day small { font-size: 10px; white-space: nowrap; }
.data-grid { display: grid; grid-template-columns: minmax(230px, .7fr) minmax(0, 1.3fr); gap: 14px; }
.top-pages { display: grid; gap: 12px; }
.top-pages div { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
code { color: var(--accent); font-size: 12px; }
.top-pages code { overflow: hidden; text-overflow: ellipsis; }
.table-wrap { overflow-x: auto; overscroll-behavior-x: contain; }
table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; }
th { color: var(--text-muted); font-size: 11px; font-weight: 600; }
th, td { padding: 10px 9px; border-bottom: 1px solid var(--border-light); white-space: nowrap; }
td.path { max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 14px; padding-top: 15px; color: var(--text-muted); font-size: 12px; }
.pagination button:disabled { opacity: .45; cursor: default; }
.empty { padding: 24px 0; color: var(--text-muted); text-align: center; font-size: 13px; }
.error { margin: 0; color: var(--error); }
@media (max-width: 980px) { .metrics { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 720px) { .data-grid { grid-template-columns: 1fr; } .chart { gap: 3px; } .chart-day small { font-size: 9px; } }
@media (max-width: 460px) { .metrics { grid-template-columns: repeat(2, 1fr); } .panel { padding: 15px; } .page-heading { align-items: start; flex-direction: column; } }
</style>
