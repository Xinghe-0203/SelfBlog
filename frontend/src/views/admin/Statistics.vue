<template>
  <div class="admin-statistics">
    <div class="admin-container">
      <div class="page-header">
        <div>
          <h1>数据统计</h1>
          <p class="page-subtitle">平台运营数据概览与分析</p>
        </div>
        <div class="header-actions">
          <select v-model="timePeriod" class="period-select glass" @change="fetchStats">
            <option value="week">本周</option>
            <option value="month">本月</option>
            <option value="quarter">本季度</option>
            <option value="year">本年度</option>
          </select>
          <button class="btn btn-secondary btn-sm" @click="exportData">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            导出数据
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-skeleton">
        <div class="stats-grid">
          <div v-for="i in 6" :key="i" class="stat-card glass">
            <div class="sk-title"></div>
            <div class="stat-list">
              <div v-for="j in 3" :key="j" class="stat-item">
                <div class="sk-label"></div>
                <div class="sk-value"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="error" class="error-state glass">
        <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <h3>{{ error }}</h3>
        <button class="btn btn-primary" @click="fetchStats">重试</button>
      </div>

      <div v-else>
        <div class="stats-overview">
          <div class="stat-card glass stat-card-blue">
            <div class="stat-icon gradient-info">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="stat-main">
              <span class="stat-value">{{ stats.userCount || stats.userStats?.totalUsers || 0 }}</span>
              <span class="stat-label">总用户数</span>
            </div>
            <div class="stat-growth" :class="getGrowthClass(stats.userStats?.todayNewUsers)">
              <svg v-if="stats.userStats?.todayNewUsers > 0" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                <polyline points="17 6 23 6 23 12"/>
              </svg>
              <span>+{{ stats.userStats?.todayNewUsers || 0 }} 今日</span>
            </div>
          </div>

          <div class="stat-card glass stat-card-green">
            <div class="stat-icon gradient-success">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <div class="stat-main">
              <span class="stat-value">{{ stats.postCount || stats.postStats?.totalPosts || 0 }}</span>
              <span class="stat-label">总文章数</span>
            </div>
            <div class="stat-growth" :class="getGrowthClass(stats.postStats?.todayNewPosts)">
              <svg v-if="stats.postStats?.todayNewPosts > 0" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                <polyline points="17 6 23 6 23 12"/>
              </svg>
              <span>+{{ stats.postStats?.todayNewPosts || 0 }} 今日</span>
            </div>
          </div>

          <div class="stat-card glass stat-card-purple">
            <div class="stat-icon" style="background: linear-gradient(135deg, var(--purple), #7C3AED);">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <div class="stat-main">
              <span class="stat-value">{{ stats.commentCount || stats.postStats?.totalComments || 0 }}</span>
              <span class="stat-label">总评论数</span>
            </div>
            <div class="stat-growth neutral">
              <span>互动数据</span>
            </div>
          </div>

          <div class="stat-card glass stat-card-orange">
            <div class="stat-icon gradient-warning">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              </svg>
            </div>
            <div class="stat-main">
              <span class="stat-value">{{ stats.reportStats?.pendingReports || 0 }}</span>
              <span class="stat-label">待处理举报</span>
            </div>
            <div class="stat-growth" :class="stats.reportStats?.pendingReports > 0 ? 'warning' : 'success'">
              <span>{{ stats.reportStats?.pendingReports > 0 ? '需要处理' : '全部已处理' }}</span>
            </div>
          </div>
        </div>

        <div class="charts-grid">
          <div class="glass chart-card">
            <h3 class="chart-title">用户数据</h3>
            <div class="stat-details">
              <div class="detail-row">
                <span class="detail-label">总用户数</span>
                <span class="detail-value">{{ stats.userStats?.totalUsers || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">今日新增</span>
                <span class="detail-value text-success">+{{ stats.userStats?.todayNewUsers || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">本周新增</span>
                <span class="detail-value">+{{ stats.userStats?.weekNewUsers || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">本月新增</span>
                <span class="detail-value">+{{ stats.userStats?.monthNewUsers || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">活跃用户</span>
                <span class="detail-value">{{ stats.userStats?.activeUsers || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="glass chart-card">
            <h3 class="chart-title">文章数据</h3>
            <div class="stat-details">
              <div class="detail-row">
                <span class="detail-label">总文章数</span>
                <span class="detail-value">{{ stats.postStats?.totalPosts || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">今日新增</span>
                <span class="detail-value text-success">+{{ stats.postStats?.todayNewPosts || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">本周新增</span>
                <span class="detail-value">+{{ stats.postStats?.weekNewPosts || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">本月新增</span>
                <span class="detail-value">+{{ stats.postStats?.monthNewPosts || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">总评论数</span>
                <span class="detail-value">{{ stats.postStats?.totalComments || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">总点赞数</span>
                <span class="detail-value">{{ stats.postStats?.totalLikes || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">总收藏数</span>
                <span class="detail-value">{{ stats.postStats?.totalCollects || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="glass chart-card">
            <h3 class="chart-title">校友圈数据</h3>
            <div class="circle-stats">
              <div class="circle-stat-item">
                <div class="circle-stat-value">{{ stats.circleStats?.totalPosts || 0 }}</div>
                <div class="circle-stat-label">总动态</div>
                <div class="circle-stat-growth text-success">+{{ stats.circleStats?.todayNewPosts || 0 }} 今日</div>
              </div>
              <div class="circle-stat-divider"></div>
              <div class="circle-stat-item">
                <div class="circle-stat-value">{{ stats.circleStats?.totalComments || 0 }}</div>
                <div class="circle-stat-label">总评论</div>
                <div class="circle-stat-growth text-success">+{{ stats.circleStats?.todayNewComments || 0 }} 今日</div>
              </div>
              <div class="circle-stat-divider"></div>
              <div class="circle-stat-item">
                <div class="circle-stat-value">{{ stats.circleStats?.totalLikes || 0 }}</div>
                <div class="circle-stat-label">总点赞</div>
              </div>
              <div class="circle-stat-divider"></div>
              <div class="circle-stat-item">
                <div class="circle-stat-value">{{ stats.circleStats?.totalReposts || 0 }}</div>
                <div class="circle-stat-label">总转发</div>
              </div>
            </div>
          </div>

          <div class="glass chart-card">
            <h3 class="chart-title">话题与标签</h3>
            <div class="stat-details">
              <div class="detail-row">
                <span class="detail-label">总话题数</span>
                <span class="detail-value">{{ stats.topicStats?.totalTopics || stats.topicCount || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">今日新增话题</span>
                <span class="detail-value text-success">+{{ stats.topicStats?.todayNewTopics || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">活跃话题</span>
                <span class="detail-value">{{ stats.topicStats?.activeTopics || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">总标签数</span>
                <span class="detail-value" style="color: var(--purple);">{{ stats.tagCount || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="glass chart-card">
            <h3 class="chart-title">互动数据</h3>
            <div class="bar-chart">
              <div class="bar-item">
                <div class="bar-label">点赞</div>
                <div class="bar-wrapper">
                  <div class="bar-fill gradient-primary" :style="{ width: getBarWidth(stats.postStats?.totalLikes, maxEngagement) + '%' }">
                    <span class="bar-value">{{ stats.postStats?.totalLikes || 0 }}</span>
                  </div>
                </div>
              </div>
              <div class="bar-item">
                <div class="bar-label">收藏</div>
                <div class="bar-wrapper">
                  <div class="bar-fill gradient-success" :style="{ width: getBarWidth(stats.postStats?.totalCollects, maxEngagement) + '%' }">
                    <span class="bar-value">{{ stats.postStats?.totalCollects || 0 }}</span>
                  </div>
                </div>
              </div>
              <div class="bar-item">
                <div class="bar-label">关注</div>
                <div class="bar-wrapper">
                  <div class="bar-fill" style="background: linear-gradient(135deg, var(--purple), #7C3AED);" :style="{ width: getBarWidth(stats.engagementStats?.totalFollows, maxEngagement) + '%' }">
                    <span class="bar-value">{{ stats.engagementStats?.totalFollows || 0 }}</span>
                  </div>
                </div>
              </div>
              <div class="bar-item">
                <div class="bar-label">评论</div>
                <div class="bar-wrapper">
                  <div class="bar-fill gradient-warning" :style="{ width: getBarWidth(stats.commentCount || stats.postStats?.totalComments, maxEngagement) + '%' }">
                    <span class="bar-value">{{ stats.commentCount || stats.postStats?.totalComments || 0 }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="glass chart-card">
            <h3 class="chart-title">内容审核</h3>
            <div class="progress-list">
              <div class="progress-item">
                <div class="progress-header">
                  <span class="progress-label">待处理举报</span>
                  <span class="progress-value">{{ stats.reportStats?.pendingReports || 0 }}</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill gradient-warning" :style="{ width: getProgressPercent(stats.reportStats?.pendingReports, totalReports) + '%' }"></div>
                </div>
              </div>
              <div class="progress-item">
                <div class="progress-header">
                  <span class="progress-label">本月处理</span>
                  <span class="progress-value">{{ stats.reportStats?.monthHandledReports || 0 }}</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill gradient-success" :style="{ width: getProgressPercent(stats.reportStats?.monthHandledReports, totalReports) + '%' }"></div>
                </div>
              </div>
              <div class="progress-item">
                <div class="progress-header">
                  <span class="progress-label">总举报数</span>
                  <span class="progress-value">{{ stats.reportStats?.totalReports || 0 }}</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill gradient-error" :style="{ width: '100%' }"></div>
                </div>
              </div>
            </div>
            <div class="chart-summary">
              <div class="summary-item">
                <span class="summary-label">处理率</span>
                <span class="summary-value">{{ getHandleRate }}%</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">待处理</span>
                <span class="summary-value warning">{{ stats.reportStats?.pendingReports || 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="growth-section">
          <div class="glass growth-card">
            <h3 class="chart-title">用户增长趋势 (近30天)</h3>
            <div class="growth-chart">
              <div class="growth-bars">
                <div v-for="(day, idx) in userGrowthData" :key="idx" class="growth-bar-item" :title="`${day.date}: +${day.count}`">
                  <div class="growth-bar-fill gradient-info" :style="{ height: getGrowthBarHeight(day.count, maxUserGrowth) + '%' }"></div>
                </div>
              </div>
              <div class="growth-axis">
                <span>{{ userGrowthData[0]?.date || '' }}</span>
                <span>{{ userGrowthData[userGrowthData.length - 1]?.date || '' }}</span>
              </div>
            </div>
          </div>

          <div class="glass growth-card">
            <h3 class="chart-title">文章增长趋势 (近30天)</h3>
            <div class="growth-chart">
              <div class="growth-bars">
                <div v-for="(day, idx) in postGrowthData" :key="idx" class="growth-bar-item" :title="`${day.date}: +${day.count}`">
                  <div class="growth-bar-fill gradient-success" :style="{ height: getGrowthBarHeight(day.count, maxPostGrowth) + '%' }"></div>
                </div>
              </div>
              <div class="growth-axis">
                <span>{{ postGrowthData[0]?.date || '' }}</span>
                <span>{{ postGrowthData[postGrowthData.length - 1]?.date || '' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../../api/admin'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const logger = useLogger('AdminStatistics')
const stats = ref<Record<string, any>>({})
const loading = ref(true)
const error = ref('')
const timePeriod = ref('month')

const userGrowthData = ref<any[]>([])
const postGrowthData = ref<any[]>([])
const maxUserGrowth = ref(1)
const maxPostGrowth = ref(1)

const maxEngagement = computed(() => {
  const values = [
    stats.value.postStats?.totalLikes || 0,
    stats.value.postStats?.totalCollects || 0,
    stats.value.engagementStats?.totalFollows || 0,
    stats.value.commentCount || stats.value.postStats?.totalComments || 0
  ]
  return Math.max(...values, 1)
})

const totalReports = computed(() => {
  return stats.value.reportStats?.totalReports || 1
})

const getHandleRate = computed(() => {
  const total = stats.value.reportStats?.totalReports || 0
  const handled = stats.value.reportStats?.monthHandledReports || 0
  if (total === 0) return 0
  return Math.round((handled / total) * 100)
})

const getGrowthClass = (value: number | undefined) => {
  if (!value || value === 0) return 'neutral'
  return value > 0 ? 'success' : 'error'
}

const getBarWidth = (value: number | undefined, max: number) => {
  if (!max || max === 0 || !value) return 0
  return Math.max((value / max) * 100, 5)
}

const getProgressPercent = (value: number | undefined, total: number) => {
  if (!total || total === 0 || !value) return 0
  return Math.min((value / total) * 100, 100)
}

const getGrowthBarHeight = (value: number | undefined, max: number) => {
  if (!max || max === 0 || !value) return 0
  return Math.max((value / max) * 100, 2)
}

const fetchStats = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await adminApi.getStatistics()
    stats.value = response.data || {}

    if (response.data?.userGrowthTrend?.length) {
      userGrowthData.value = response.data.userGrowthTrend
      maxUserGrowth.value = Math.max(...response.data.userGrowthTrend.map(d => d.count || 0), 1)
    }

    if (response.data?.postGrowthTrend?.length) {
      postGrowthData.value = response.data.postGrowthTrend
      maxPostGrowth.value = Math.max(...response.data.postGrowthTrend.map(d => d.count || 0), 1)
    }
  } catch (err: any) {
    logger.error('Failed to fetch stats', { error: err.message })
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

const exportData = () => {
  const data = stats.value
  const jsonStr = JSON.stringify(data, null, 2)
  const blob = new Blob([jsonStr], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `statistics-${timePeriod.value}-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
  toast.success('数据导出成功')
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.admin-statistics {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.page-header h1 {
  font-size: 1.75rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-subtitle {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.header-actions {
  display: flex;
  gap: var(--spacing-sm);
  align-items: center;
}

.period-select {
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: 0.8125rem;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
}

.period-select:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-lg);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
  transition: all var(--transition-slow);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  pointer-events: none;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg), var(--glass-shadow-wet);
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: white;
}

.stat-main {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.stat-growth {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 0.75rem;
  font-weight: 500;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-full);
  width: fit-content;
}

.stat-growth.success {
  background: var(--success-light);
  color: var(--success);
}

.stat-growth.warning {
  background: var(--warning-light);
  color: var(--warning);
}

.stat-growth.error {
  background: var(--error-light);
  color: var(--error);
}

.stat-growth.neutral {
  background: var(--primary-light);
  color: var(--primary);
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.glass {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
  padding: var(--spacing-lg);
  position: relative;
  overflow: hidden;
}

.glass::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  pointer-events: none;
}

.chart-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--glass-border);
}

.stat-details {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-xs) 0;
  border-bottom: 1px solid rgba(0,0,0,0.03);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.detail-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.text-success { color: var(--success); }

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.bar-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.bar-label {
  width: 60px;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.bar-wrapper {
  flex: 1;
  height: 32px;
  background: var(--surface);
  border-radius: var(--radius);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: var(--spacing-sm);
  border-radius: var(--radius);
  transition: width 0.6s ease;
  min-width: 40px;
}

.bar-value {
  font-size: 0.75rem;
  font-weight: 600;
  color: white;
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.8125rem;
}

.progress-label {
  color: var(--text-secondary);
}

.progress-value {
  font-weight: 600;
  color: var(--text-primary);
}

.progress-bar {
  height: 8px;
  background: var(--surface);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.6s ease;
}

.chart-summary {
  display: flex;
  justify-content: space-around;
  padding: var(--spacing-md);
  background: var(--surface);
  border-radius: var(--radius);
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
}

.summary-label {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.summary-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
}

.summary-value.warning {
  color: var(--warning);
}

.circle-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.circle-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
}

.circle-stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.circle-stat-label {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.circle-stat-growth {
  font-size: 0.6875rem;
  font-weight: 500;
}

.circle-stat-divider {
  width: 1px;
  height: 60px;
  background: var(--glass-border);
}

.growth-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
}

.growth-chart {
  margin-top: var(--spacing-md);
}

.growth-bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 160px;
  padding: 0 var(--spacing-xs);
}

.growth-bar-item {
  flex: 1;
  display: flex;
  align-items: flex-end;
  height: 100%;
  cursor: pointer;
}

.growth-bar-fill {
  width: 100%;
  border-radius: 3px 3px 0 0;
  transition: height 0.4s ease;
  min-height: 2px;
}

.growth-bar-fill:hover {
  opacity: 0.8;
}

.growth-axis {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-xs) var(--spacing-xs) 0;
  font-size: 0.6875rem;
  color: var(--text-muted);
  border-top: 1px solid var(--glass-border);
  margin-top: var(--spacing-xs);
}

@media (max-width: 768px) {
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }

  .growth-section {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
  }

  .circle-stats {
    flex-wrap: wrap;
    gap: var(--spacing-md);
  }

  .circle-stat-divider {
    display: none;
  }
}

.loading-skeleton {
  padding: var(--spacing-lg) 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
}

.stat-card h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--border);
}

.stat-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sk-title {
  width: 80px;
  height: 16px;
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  margin-bottom: var(--spacing-md);
}

.sk-label {
  width: 60px;
  height: 14px;
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.sk-value {
  width: 50px;
  height: 20px;
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.error-state {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--text-secondary);
}

.error-state h3 {
  margin: var(--spacing-md) 0 var(--spacing-lg);
  font-size: 1rem;
}
</style>
