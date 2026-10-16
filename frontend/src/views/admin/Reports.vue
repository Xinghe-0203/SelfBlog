<template>
  <div class="admin-reports">
    <div class="admin-container">
      <div class="page-header">
        <h2>举报管理</h2>
      </div>

      <div class="tabs glass">
        <button class="tab-btn" :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          待处理
          <span v-if="pendingCount > 0" class="tab-badge warning">{{ pendingCount }}</span>
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'verified' }" @click="activeTab = 'verified'">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          已核实
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'rejected' }" @click="activeTab = 'rejected'">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          已驳回
        </button>
      </div>

      <div v-if="loading" class="loading-skeleton">
        <div class="reports-grid">
          <div v-for="i in 4" :key="i" class="report-card glass">
            <div class="sk-line w-40"></div>
            <div class="sk-line w-80 mt-2"></div>
            <div class="sk-line w-60 mt-2"></div>
          </div>
        </div>
      </div>

      <div v-else-if="error" class="error-state glass">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <h3>{{ error }}</h3>
        <button class="btn btn-primary" @click="fetchReports">重试</button>
      </div>

      <div v-else>
        <div v-if="reports.length === 0" class="empty-state glass">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          <h3 class="empty-title">暂无举报数据</h3>
          <p class="empty-text">当前分类下没有举报记录</p>
        </div>

        <div v-else class="reports-grid">
          <div v-for="report in reports" :key="report.id" class="report-card glass">
            <div class="report-header">
              <div class="report-id">
                <span class="id-label">#{{ report.id }}</span>
                <span class="priority-badge" :class="getPriorityClass(report)">{{ getPriorityText(report) }}</span>
              </div>
              <span class="status-badge" :class="getStatusClass(report.status)">
                <span class="status-dot" :class="getStatusDotClass(report.status)"></span>
                {{ getStatusText(report.status) }}
              </span>
            </div>

            <div class="report-body">
              <div class="report-row">
                <span class="report-label">举报类型</span>
                <span class="report-value">{{ report.targetType }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">举报原因</span>
                <span class="report-value reason-text">{{ report.reason }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">举报人</span>
                <span class="report-value">{{ report.reporterName }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">目标ID</span>
                <span class="report-value mono">{{ report.targetId }}</span>
              </div>
              <div class="report-row">
                <span class="report-label">举报时间</span>
                <span class="report-value time">{{ formatDate(report.createTime) }}</span>
              </div>
            </div>

            <div class="report-actions" v-if="report.status === 0">
              <button class="btn btn-sm btn-success-outline" @click="handleReport(report, 2)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                核实
              </button>
              <button class="btn btn-sm btn-danger-outline" @click="handleReport(report, 1)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
                </svg>
                驳回
              </button>
              <button class="btn btn-sm btn-ghost" @click="viewDetail(report)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
                详情
              </button>
            </div>
            <div class="report-actions" v-else>
              <button class="btn btn-sm btn-ghost" @click="viewDetail(report)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
                查看详情
              </button>
            </div>
          </div>
        </div>

        <div v-if="totalPages > 1" class="pagination-wrapper">
          <div class="pagination glass">
            <button class="pagination-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
              上一页
            </button>
            <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
            <button class="pagination-btn" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">
              下一页
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="detailDialog" class="dialog-overlay" @click.self="detailDialog = false">
      <div class="dialog glass">
        <div class="dialog-header">
          <h3>举报详情 #{{ detailData?.id }}</h3>
          <button class="dialog-close" @click="detailDialog = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="dialog-body">
          <div v-if="detailLoading" class="dialog-loading">
            <span class="loading-spinner"></span>
            <p>加载中...</p>
          </div>
          <template v-else-if="detailData">
            <div class="detail-row">
              <span class="detail-label">举报类型</span>
              <span class="detail-value">{{ detailData.targetType }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">举报原因</span>
              <span class="detail-value">{{ detailData.reason }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">详细说明</span>
              <span class="detail-value">{{ detailData.description || '无' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">举报人</span>
              <span class="detail-value">{{ detailData.reporterName }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">被举报内容ID</span>
              <span class="detail-value mono">{{ detailData.targetId }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">状态</span>
              <span class="detail-value">
                <span class="status-badge" :class="getStatusClass(detailData.status)">
                  <span class="status-dot" :class="getStatusDotClass(detailData.status)"></span>
                  {{ getStatusText(detailData.status) }}
                </span>
              </span>
            </div>
            <div class="detail-row">
              <span class="detail-label">举报时间</span>
              <span class="detail-value time">{{ formatDate(detailData.createTime) }}</span>
            </div>
          </template>
        </div>
        <div class="dialog-footer">
          <button class="btn btn-ghost" @click="detailDialog = false">关闭</button>
        </div>
      </div>
    </div>
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { adminApi } from '../../api/admin'
import { formatDate } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { useConfirm } from '../../composables/useConfirm'

const logger = useLogger('AdminReports')
const { confirm, ConfirmDialog } = useConfirm()

const reports = ref<any[]>([])
const activeTab = ref('pending')
const currentPage = ref(1)
const totalPages = ref(1)
const pendingCount = ref(0)
const detailDialog = ref(false)
const detailData = ref<any | null>(null)
const detailLoading = ref(false)
const loading = ref(true)
const error = ref('')

const statusMap: Record<string, number> = { pending: 0, verified: 2, rejected: 1 }

const fetchReports = async () => {
  loading.value = true
  error.value = ''
  try {
    const status = statusMap[activeTab.value]
    const response = await adminApi.getPendingReports({
      pageNum: currentPage.value,
      pageSize: 20,
      status: status !== undefined ? status : undefined
    } as any)
    let records = response.data?.records || []
    // 后端已按 status 筛选，此处无需再过滤
    reports.value = records
    totalPages.value = response.data?.pages || 1
    if (activeTab.value === 'pending') {
      pendingCount.value = response.data?.total || records.length
    }
  } catch (err: any) {
    logger.error('Failed to fetch reports', { error: err.message })
    error.value = '加载失败，请重试'
    reports.value = []
  } finally {
    loading.value = false
  }
}

const getPriorityClass = (report: any) => {
  const reason = (report.reason || '').toLowerCase()
  if (reason.includes('违法') || reason.includes('暴力') || reason.includes('色情')) return 'priority-high'
  return 'priority-normal'
}

const getPriorityText = (report: any) => {
  const reason = (report.reason || '').toLowerCase()
  if (reason.includes('违法') || reason.includes('暴力') || reason.includes('色情')) return '高优先级'
  return '普通'
}

const getStatusClass = (status: number) => {
  return { 0: 'status-pending', 1: 'status-rejected', 2: 'status-verified' }[status] || ''
}

const getStatusDotClass = (status: number) => {
  return { 0: 'pending', 1: 'rejected', 2: 'verified' }[status] || ''
}

const getStatusText = (status: number) => {
  return { 0: '待处理', 1: '已驳回', 2: '已核实' }[status] || '未知'
}

const handleReport = async (report: any, status: number) => {
  const action = status === 2 ? '核实' : '驳回'
  const ok = await confirm(`确定${action}该举报吗？`, `${action}举报`)
  if (!ok) return
  try {
    await adminApi.handleReport(report.id, { status: status === 2 ? 'approved' : 'rejected', handleResult: '' } as any)
    report.status = status
    toast.success(`已${action}`)
    fetchReports()
  } catch (err: any) {
    logger.error('Failed to handle report', { error: err.message })
    toast.error('操作失败')
  }
}

const viewDetail = async (report: any) => {
  detailLoading.value = true
  detailDialog.value = true
  detailData.value = null
  try {
    const response = await adminApi.getReportById(report.id)
    detailData.value = response.data || report
  } catch (err: any) {
    logger.error('Failed to fetch report detail', { error: err.message })
    toast.error('获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  fetchReports()
}

watch(activeTab, () => {
  currentPage.value = 1
  fetchReports()
})

onMounted(() => {
  fetchReports()
  // 单独获取待处理数量用于 badge 显示
  adminApi.getPendingReports({ pageNum: 1, pageSize: 1, status: 0 } as any).then(res => {
    pendingCount.value = res.data?.total || 0
  }).catch(() => {})
})
</script>

<style scoped>
.admin-reports {
  padding: var(--spacing-lg);
  min-height: 100vh;
}

.admin-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--spacing-lg);
}

.page-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.tabs {
  display: flex;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs);
  margin-bottom: var(--spacing-lg);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: 0.8125rem;
  background: none;
  border: none;
  border-radius: var(--radius);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition);
  position: relative;
}

.tab-btn:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.tab-btn.active {
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  color: var(--text-inverse);
  box-shadow: var(--shadow-sm);
}

.tab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  font-size: 0.6875rem;
  font-weight: 600;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-badge.warning {
  background: var(--warning);
  color: white;
}

.reports-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.report-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
  padding: var(--spacing-lg);
  transition: all var(--transition-slow);
  position: relative;
  overflow: hidden;
}

.report-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  pointer-events: none;
}

.report-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg), var(--glass-shadow-wet);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--glass-border);
}

.report-id {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.id-label {
  font-family: var(--font-mono);
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.priority-badge {
  padding: 0.125rem 0.5rem;
  font-size: 0.6875rem;
  font-weight: 600;
  border-radius: var(--radius-full);
}

.priority-high {
  background: var(--error-light);
  color: var(--error);
}

.priority-normal {
  background: var(--info-light);
  color: var(--info);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: var(--radius-full);
}

.status-pending {
  background: var(--warning-light);
  color: var(--warning);
}

.status-verified {
  background: var(--success-light);
  color: var(--success);
}

.status-rejected {
  background: var(--error-light);
  color: var(--error);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
}

.status-dot.pending { background: var(--warning); }
.status-dot.verified { background: var(--success); }
.status-dot.rejected { background: var(--error); }

.report-body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.report-row {
  display: flex;
  gap: var(--spacing-sm);
  font-size: 0.8125rem;
}

.report-label {
  width: 80px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.report-value {
  flex: 1;
  color: var(--text-primary);
}

.reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mono {
  font-family: var(--font-mono);
}

.time {
  color: var(--text-muted);
}

.report-actions {
  display: flex;
  gap: var(--spacing-xs);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--glass-border);
}

.btn-success-outline {
  color: var(--success) !important;
}

.btn-success-outline:hover {
  background: var(--success-light) !important;
  color: var(--success) !important;
}

.btn-danger-outline {
  color: var(--error) !important;
}

.btn-danger-outline:hover {
  background: var(--error-light) !important;
  color: var(--error) !important;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
}

.pagination {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow);
}

.page-info {
  font-size: 0.8125rem;
  color: var(--text-muted);
  padding: 0 var(--spacing-md);
}

.empty-state {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--text-secondary);
}

.empty-state svg {
  color: var(--text-muted);
  opacity: 0.5;
  margin-bottom: var(--spacing-md);
}

.empty-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.empty-text {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: 500px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  padding: var(--spacing-lg);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--glass-border);
}

.dialog-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
}

.dialog-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: var(--spacing-xs);
  border-radius: var(--radius);
  transition: all var(--transition);
}

.dialog-close:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.detail-row {
  display: flex;
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--glass-border);
  font-size: 0.875rem;
}

.detail-label {
  width: 120px;
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: var(--text-primary);
}

.dialog-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl) 0;
  gap: var(--spacing-md);
  color: var(--text-muted);
  font-size: 0.875rem;
}

.dialog-footer {
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--glass-border);
  text-align: right;
}

.loading-skeleton {
  padding: var(--spacing-lg) 0;
}

.sk-line {
  height: 14px;
  border-radius: var(--radius-xs);
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-line.w-40 { width: 40%; }
.sk-line.w-60 { width: 60%; }
.sk-line.w-80 { width: 80%; }

@media (max-width: 768px) {
  .reports-grid {
    grid-template-columns: 1fr;
  }

  .tabs {
    flex-wrap: wrap;
  }
}
</style>
