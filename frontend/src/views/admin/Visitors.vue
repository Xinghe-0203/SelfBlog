<template>
  <div class="visitors-page">
    <header class="page-heading"><div><p class="eyebrow">ANALYTICS</p><h1>访客记录</h1><p>匿名统计访问趋势，不保存访客的真实 IP 地址。</p></div><button class="refresh" type="button" @click="load">刷新数据</button></header>
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
        <div class="pagination"><button type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><span>第 {{ page }} 页 / 共 {{ pages }} 页</span><button type="button" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { formatDate } from '@/utils'
import { siteApi, type VisitorRecord, type VisitorSummary } from '@/api/site'

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
.page-heading h1 { margin: 4px 0 6px; font: 700 2rem var(--font-display); }
.page-heading p:not(.eyebrow), .section-heading p { margin: 0; color: var(--text-muted); font-size: .8rem; }
.eyebrow { margin: 0; color: var(--primary); font-size: .68rem; font-weight: 700; letter-spacing: .16em; }
.refresh, .pagination button { padding: 9px 13px; border: 1px solid var(--border-solid); border-radius: 8px; background: var(--surface-primary); color: var(--text-primary); cursor: pointer; }
.refresh:hover, .pagination button:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.metrics { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 10px; }
.metrics article, .panel { border: 1px solid var(--border-solid); border-radius: 12px; background: var(--surface-primary); }
.metrics article { display: grid; gap: 8px; padding: 17px; }
.metrics small { color: var(--text-muted); font-size: .75rem; }
.metrics strong { font: 700 1.55rem var(--font-display); }
.panel { min-width: 0; padding: 20px; }
.section-heading { margin-bottom: 18px; }
.section-heading h2 { margin: 0 0 5px; font-size: 1rem; }
.chart { display: grid; height: 210px; grid-template-columns: repeat(14, minmax(16px, 1fr)); align-items: end; gap: 8px; padding-top: 12px; }
.chart-day { display: flex; height: 100%; flex-direction: column; justify-content: end; align-items: center; gap: 7px; color: var(--text-muted); }
.chart-day strong { font-size: .65rem; font-weight: 500; }
.bar { width: min(100%, 27px); min-height: 3px; border-radius: 5px 5px 2px 2px; background: linear-gradient(180deg, var(--primary), color-mix(in srgb, var(--primary) 45%, white)); }
.chart-day small { font-size: .62rem; white-space: nowrap; }
.data-grid { display: grid; grid-template-columns: minmax(230px, .7fr) minmax(0, 1.3fr); gap: 14px; }
.top-pages { display: grid; gap: 12px; }
.top-pages div { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
code { color: var(--primary); font-size: .75rem; }
.top-pages code { overflow: hidden; text-overflow: ellipsis; }
.table-wrap { overflow: auto; }
table { width: 100%; border-collapse: collapse; font-size: .78rem; text-align: left; }
th { color: var(--text-muted); font-size: .69rem; font-weight: 600; }
th, td { padding: 10px 9px; border-bottom: 1px solid var(--border-solid); white-space: nowrap; }
td.path { max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 14px; padding-top: 15px; color: var(--text-muted); font-size: .75rem; }
.pagination button { padding: 7px 10px; }
.pagination button:disabled { opacity: .45; cursor: default; }
.empty { padding: 24px 0; color: var(--text-muted); text-align: center; font-size: .8rem; }
.error { margin: 0; color: var(--error); }
@media (max-width: 980px) { .metrics { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 720px) { .data-grid { grid-template-columns: 1fr; } .chart { gap: 3px; } .chart-day small { font-size: .55rem; } }
@media (max-width: 460px) { .metrics { grid-template-columns: repeat(2, 1fr); } .panel { padding: 15px; } .page-heading h1 { font-size: 1.65rem; } }
</style>
