<template>
  <div class="admin-circle">
    <div class="admin-container">
      <div class="page-header">
        <h2>校友圈管理</h2>
        <div class="search-bar glass">
          <div class="search-input-wrapper">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input v-model="searchQuery" type="text" placeholder="搜索内容..." @keyup.enter="fetchCirclePosts" />
          </div>
          <button class="btn btn-primary btn-sm" @click="fetchCirclePosts">搜索</button>
        </div>
      </div>

      <div v-if="loading" class="loading-skeleton">
        <div class="table-container glass">
          <table class="data-table">
            <thead>
              <tr><th>ID</th><th>作者</th><th>内容</th><th>类型</th><th>统计</th><th>可见性</th><th>状态</th><th>时间</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="i in 5" :key="i">
                <td><div class="sk-line w-30"></div></td>
                <td><div class="sk-line w-50"></div></td>
                <td><div class="sk-line w-80"></div></td>
                <td><div class="sk-line w-40"></div></td>
                <td><div class="sk-line w-60"></div></td>
                <td><div class="sk-line w-40"></div></td>
                <td><div class="sk-line w-40"></div></td>
                <td><div class="sk-line w-60"></div></td>
                <td><div class="sk-line w-50"></div></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-else-if="error" class="error-state glass">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <h3>{{ error }}</h3>
        <button class="btn btn-primary" @click="fetchCirclePosts">重试</button>
      </div>

      <div v-else class="table-container glass">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>作者</th><th>内容</th><th>类型</th><th>统计</th><th>可见性</th><th>状态</th><th>时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-if="circlePosts.length === 0">
              <td colspan="9" class="empty-cell">
                <div class="empty-content">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                  </svg>
                  <span>暂无校友圈数据</span>
                </div>
              </td>
            </tr>
            <tr v-for="post in circlePosts" :key="post.id" class="clickable-row" @click="viewDetail(post)">
              <td class="id-cell">{{ post.id }}</td>
              <td>{{ post.nickname || post.username }}</td>
              <td class="content-cell">{{ truncateContent(post.content) }}</td>
              <td>
                <span class="type-badge" :class="'type-' + (post.type || 'text')">
                  {{ typeText(post.type) }}
                </span>
              </td>
              <td class="stats-cell">
                <span class="stat-item" title="浏览">{{ post.viewCount || 0 }}</span>
                <span class="stat-item" title="点赞">{{ post.likeCount || 0 }}</span>
                <span class="stat-item" title="评论">{{ post.commentCount || 0 }}</span>
                <span class="stat-item" title="转发">{{ post.repostCount || 0 }}</span>
              </td>
              <td>
                <span class="status-badge" :class="visibilityClass(post.visibility)">
                  {{ visibilityText(post.visibility) }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="post.status === 1 ? 'status-active' : 'status-disabled'">
                  <span class="status-dot" :class="post.status === 1 ? 'active' : 'disabled'"></span>
                  {{ post.status === 1 ? '正常' : '隐藏' }}
                </span>
              </td>
              <td class="time-cell">{{ formatDate(post.createTime) }}</td>
              <td @click.stop>
                <div class="actions">
                  <button class="btn btn-xs btn-ghost" @click="toggleStatus(post)">
                    {{ post.status === 1 ? '隐藏' : '显示' }}
                  </button>
                  <button class="btn btn-xs btn-ghost danger" @click="deletePost(post)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    </svg>
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
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

    <div v-if="detailDialog" class="dialog-overlay" @click.self="detailDialog = false">
      <div class="dialog glass">
        <div class="dialog-header">
          <h3>校友圈详情 #{{ detailData?.id }}</h3>
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
              <span class="detail-label">ID</span>
              <span class="detail-value mono">{{ detailData.id }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">作者</span>
              <span class="detail-value">{{ detailData.nickname || detailData.username }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">类型</span>
              <span class="detail-value">{{ typeText(detailData.type) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">可见性</span>
              <span class="detail-value">{{ visibilityText(detailData.visibility) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">状态</span>
              <span class="detail-value">
                <span class="status-badge" :class="detailData.status === 1 ? 'status-active' : 'status-disabled'">
                  <span class="status-dot" :class="detailData.status === 1 ? 'active' : 'disabled'"></span>
                  {{ detailData.status === 1 ? '正常' : '隐藏' }}
                </span>
              </span>
            </div>
            <div class="detail-row">
              <span class="detail-label">内容</span>
              <span class="detail-value content-text">{{ detailData.content || '无文本内容' }}</span>
            </div>
            <div class="detail-row" v-if="detailData.images && detailData.images.length">
              <span class="detail-label">图片</span>
              <div class="detail-value">
                <div class="image-list">
                  <img v-for="(img, idx) in detailData.images" :key="idx" :src="img" class="detail-image" @click="previewImage = img" />
                </div>
              </div>
            </div>
            <div class="detail-row">
              <span class="detail-label">统计</span>
              <div class="detail-value stats-detail">
                <span>浏览 {{ detailData.viewCount || 0 }}</span>
                <span>点赞 {{ detailData.likeCount || 0 }}</span>
                <span>评论 {{ detailData.commentCount || 0 }}</span>
                <span>转发 {{ detailData.repostCount || 0 }}</span>
              </div>
            </div>
            <div class="detail-row">
              <span class="detail-label">创建时间</span>
              <span class="detail-value time">{{ formatDate(detailData.createTime) }}</span>
            </div>
          </template>
        </div>
        <div class="dialog-footer">
          <button class="btn btn-ghost" @click="detailDialog = false">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="previewImage" class="image-preview-overlay" @click="previewImage = null">
      <img :src="previewImage" class="preview-img" @click.stop />
    </div>
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api/admin'
import { formatDate } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { useConfirm } from '../../composables/useConfirm'

const logger = useLogger('AdminCircle')
const { confirm, ConfirmDialog } = useConfirm()

const circlePosts = ref<any[]>([])
const searchQuery = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const loading = ref(true)
const error = ref('')
const detailDialog = ref(false)
const detailData = ref<any | null>(null)
const detailLoading = ref(false)
const previewImage = ref<string | null>(null)

const fetchCirclePosts = async () => {
  loading.value = true
  error.value = ''
  try {
    const params: Record<string, any> = { pageNum: currentPage.value, pageSize: 20 }
    if (searchQuery.value) params.keyword = searchQuery.value
    const response = await adminApi.getCircleList(params)
    const data = response.data || {}
    circlePosts.value = data.records || []
    totalPages.value = data.pages || 1
  } catch (err: any) {
    logger.error('Failed to fetch circle posts', { error: err.message })
    error.value = '加载失败，请重试'
    circlePosts.value = []
  } finally {
    loading.value = false
  }
}

const truncateContent = (content: string) => {
  if (!content) return '-'
  return content.length > 50 ? content.substring(0, 50) + '...' : content
}

const typeText = (type: string) => {
  const map: Record<string, string> = { text: '文字', image: '图片', video: '视频', repost: '转发' }
  return map[type] || '文字'
}

const visibilityText = (visibility: string) => {
  const map: Record<string, string> = { public: '公开', followers: '仅关注', private: '私密' }
  return map[visibility] || '公开'
}

const visibilityClass = (visibility: string) => {
  const map: Record<string, string> = { public: 'visibility-public', followers: 'visibility-followers', private: 'visibility-private' }
  return map[visibility] || 'visibility-public'
}

const viewDetail = async (post: any) => {
  detailLoading.value = true
  detailDialog.value = true
  detailData.value = null
  try {
    const response = await adminApi.getCircleDetail(post.id)
    detailData.value = response.data || post
  } catch (err: any) {
    logger.error('Failed to fetch circle detail', { error: err.message })
    toast.error('获取详情失败')
    detailData.value = post
  } finally {
    detailLoading.value = false
  }
}

const toggleStatus = async (post: any) => {
  const newStatus = post.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '显示' : '隐藏'
  const ok = await confirm(`确定${action}该校友圈动态吗？`, `${action}动态`)
  if (!ok) return
  try {
    await adminApi.updateCircleStatus(post.id, { status: newStatus })
    post.status = newStatus
    toast.success(`${action}成功`)
  } catch (err: any) {
    logger.error('Failed to toggle circle status', { error: err.message })
    toast.error('操作失败')
  }
}

const deletePost = async (post: any) => {
  const ok = await confirm('确定删除该校友圈动态吗？此操作不可恢复。', '删除动态')
  if (!ok) return
  try {
    await adminApi.deleteCirclePost(post.id)
    circlePosts.value = circlePosts.value.filter(p => p.id !== post.id)
    toast.success('删除成功')
  } catch (err: any) {
    logger.error('Failed to delete circle post', { error: err.message })
    toast.error('删除失败')
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  fetchCirclePosts()
}

onMounted(() => {
  fetchCirclePosts()
})
</script>

<style scoped>
.admin-circle {
  padding: var(--spacing-lg);
  min-height: 100vh;
}

.admin-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.page-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.search-bar {
  display: flex;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow);
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  transition: all var(--transition);
}

.search-input-wrapper:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.search-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-bar input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.875rem;
  color: var(--text-primary);
  outline: none;
}

.search-bar input::placeholder {
  color: var(--text-muted);
}

.table-container {
  overflow-x: auto;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
  position: relative;
}

.table-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  pointer-events: none;
  z-index: 1;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: var(--spacing-md);
  text-align: left;
  border-bottom: 1px solid var(--glass-border);
}

.data-table th {
  font-weight: 600;
  color: var(--text-primary);
  background: var(--surface);
  font-size: 0.8125rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.data-table tbody tr {
  transition: all var(--transition);
}

.data-table tbody tr:hover {
  background: var(--primary-light);
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.clickable-row {
  cursor: pointer;
}

.id-cell {
  font-family: var(--font-mono);
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.content-cell {
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}

.type-badge {
  display: inline-flex;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: var(--radius-full);
}

.type-text {
  background: var(--info-light);
  color: var(--info);
}

.type-image {
  background: var(--success-light);
  color: var(--success);
}

.type-video {
  background: var(--warning-light);
  color: var(--warning);
}

.type-repost {
  background: var(--primary-light);
  color: var(--primary);
}

.stats-cell {
  display: flex;
  gap: var(--spacing-xs);
  font-size: 0.75rem;
  color: var(--text-muted);
}

.stat-item {
  min-width: 24px;
  text-align: center;
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

.status-active {
  background: var(--success-light);
  color: var(--success);
}

.status-disabled {
  background: var(--error-light);
  color: var(--error);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
}

.status-dot.active { background: var(--success); }
.status-dot.disabled { background: var(--error); }

.visibility-public {
  background: var(--success-light);
  color: var(--success);
}

.visibility-followers {
  background: var(--warning-light);
  color: var(--warning);
}

.visibility-private {
  background: var(--error-light);
  color: var(--error);
}

.time-cell {
  font-size: 0.8125rem;
  color: var(--text-muted);
  white-space: nowrap;
}

.actions {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: nowrap;
}

.pagination-wrapper {
  margin-top: var(--spacing-lg);
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

.empty-cell {
  text-align: center;
  padding: var(--spacing-2xl);
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  color: var(--text-muted);
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

.loading-skeleton {
  padding: var(--spacing-lg) 0;
}

.sk-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-line.w-30 { width: 30%; }
.sk-line.w-40 { width: 40%; }
.sk-line.w-50 { width: 50%; }
.sk-line.w-60 { width: 60%; }
.sk-line.w-80 { width: 80%; }

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
  width: 560px;
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
  width: 100px;
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: var(--text-primary);
  word-break: break-word;
}

.mono {
  font-family: var(--font-mono);
}

.time {
  color: var(--text-muted);
}

.content-text {
  white-space: pre-wrap;
  line-height: 1.6;
}

.stats-detail {
  display: flex;
  gap: var(--spacing-md);
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.detail-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: transform var(--transition);
}

.detail-image:hover {
  transform: scale(1.05);
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

.image-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  cursor: pointer;
}

.preview-img {
  max-width: 80vw;
  max-height: 80vh;
  border-radius: var(--radius);
  object-fit: contain;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
