<template>
  <div class="drafts-page">
    <button class="back-btn" @click="router.back()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      返回
    </button>
    <div class="page-header">
      <h1>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="header-icon"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
        我的草稿
      </h1>
      <div class="header-actions">
        <span class="page-count">{{ total }} 篇</span>
        <div class="view-toggle">
          <button class="toggle-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'" title="列表视图">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
          </button>
          <button class="toggle-btn" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'" title="网格视图">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
          </button>
        </div>
        <router-link to="/post-edit" class="btn btn-primary btn-sm">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          写文章
        </router-link>
      </div>
    </div>

    <div v-if="loading && drafts.length === 0" class="skeleton-list">
      <div v-for="n in 3" :key="n" class="skeleton-card-item">
        <div class="skeleton skeleton-card-title"></div>
        <div class="skeleton skeleton-card-text"></div>
        <div class="skeleton skeleton-card-text short"></div>
        <div class="skeleton skeleton-card-meta"></div>
      </div>
    </div>

    <div v-else-if="error" class="error-card card">
      <p>{{ error }}</p>
      <button class="btn btn-sm btn-primary" @click="fetchDrafts">重试</button>
    </div>

    <div v-else-if="drafts.length === 0" class="empty-state card">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
      <p class="empty-title">没有草稿</p>
      <p class="empty-text">开始写你的第一篇文章吧</p>
      <router-link to="/post-edit" class="btn btn-primary">写文章</router-link>
    </div>

    <div v-else :class="['draft-list', { 'grid-view': viewMode === 'grid' }]">
      <div v-for="draft in drafts" :key="draft.draftId" class="draft-card card">
        <div class="draft-card-body">
          <h3 class="draft-title">{{ draft.title || '无标题' }}</h3>
          <p class="draft-excerpt">{{ truncateText(draft.summary || draft.content, 120) }}</p>
          <div class="draft-meta">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            最后编辑于 {{ formatRelativeTime(draft.updateTime || draft.createTime || '') }}
          </div>
        </div>
        <div class="draft-card-actions">
          <router-link :to="`/post-edit?draft=${draft.draftId}`" class="btn btn-sm btn-primary">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            继续编辑
          </router-link>
          <button class="btn btn-sm btn-ghost" @click="confirmDeleteDraft(draft)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            删除
          </button>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination-section">
      <div class="pagination">
        <button class="pagination-btn" :disabled="page <= 1" @click="page--; fetchDrafts()">上一页</button>
        <button v-for="p in totalPages" :key="p" class="pagination-btn" :class="{ active: p === page }" @click="page = p; fetchDrafts()">{{ p }}</button>
        <button class="pagination-btn" :disabled="page >= totalPages" @click="page++; fetchDrafts()">下一页</button>
      </div>
      <span class="pagination-info">共 {{ total }} 篇草稿</span>
    </div>

    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { postApi } from '../../api/post'
import { useConfirm } from '../../composables/useConfirm'
import { formatRelativeTime, truncateText } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const router = useRouter()

const logger = useLogger('Drafts')
const { confirm, ConfirmDialog } = useConfirm()
const drafts = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const total = ref(0)
const totalPages = ref(1)
const pageSize = 10
const viewMode = ref<string>('list')

async function confirmDeleteDraft(draft: any) {
  const ok = await confirm(`确定要删除「${draft.title || '无标题'}」吗？`, '删除草稿')
  if (ok) deleteDraft(draft.draftId)
}

async function fetchDrafts() {
  loading.value = true
  error.value = ''
  try {
    const response = await postApi.getMyDrafts({ pageNum: page.value, pageSize })
    const data = response.data || {}
    drafts.value = data.records || []
    total.value = data.total || 0
    totalPages.value = data.pages || 1
  } catch (err: any) {
    logger.error('Failed to fetch drafts', { error: err.message })
    error.value = '加载草稿失败'
  } finally {
    loading.value = false
  }
}

async function deleteDraft(draftId: number | string) {
  try {
    await postApi.deleteDraft(draftId)
    drafts.value = drafts.value.filter(d => d.draftId !== draftId)
    total.value = Math.max(0, total.value - 1)
    toast.success('草稿已删除')
    if (drafts.value.length === 0 && page.value > 1) {
      page.value--
      fetchDrafts()
    }
  } catch (err: any) {
    logger.error('Failed to delete draft', { error: err.message })
    toast.error('删除失败')
  }
}

onMounted(() => {
  fetchDrafts()
})
</script>

<style scoped>
.drafts-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all var(--transition);
  width: fit-content;
  margin-bottom: 16px;
  box-shadow: var(--glass-shadow);
}

.back-btn:hover {
  background: var(--glass-hover);
  color: var(--primary);
  border-color: var(--primary);
  transform: translateY(-1px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-header h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.header-icon {
  color: var(--primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-count {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.view-toggle {
  display: flex;
  background: var(--surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  overflow: hidden;
}

.toggle-btn {
  padding: 6px 10px;
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition);
  display: flex;
  align-items: center;
}

.toggle-btn:hover {
  color: var(--primary);
}

.toggle-btn.active {
  background: var(--primary-light);
  color: var(--primary);
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card-item {
  padding: 20px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--surface);
}

.skeleton-card-title {
  width: 55%;
  height: 20px;
  margin-bottom: 10px;
}

.skeleton-card-text {
  width: 85%;
  height: 14px;
  margin-bottom: 6px;
}

.skeleton-card-text.short {
  width: 40%;
}

.skeleton-card-meta {
  width: 180px;
  height: 14px;
  margin-top: 12px;
}

.draft-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.draft-list.grid-view {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.draft-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  gap: 20px;
  transition: all var(--transition-slow);
  border-radius: var(--radius-lg);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  box-shadow: var(--glass-shadow-wet);
  position: relative;
  overflow: hidden;
}

.draft-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  pointer-events: none;
}

.draft-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md), var(--glass-shadow-wet);
  border-color: var(--primary-light);
}

.draft-card-body {
  flex: 1;
  min-width: 0;
}

.draft-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
  line-height: 1.4;
}

.draft-excerpt {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.draft-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--text-muted);
}

.draft-card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.error-card {
  padding: 40px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  border-radius: var(--radius-lg);
}

.error-card p {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.empty-state {
  padding: 80px 24px;
  text-align: center;
  border-radius: var(--radius-lg);
}

.empty-icon {
  color: var(--text-muted);
  opacity: 0.3;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-text {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.pagination-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding-top: 24px;
  margin-top: 8px;
}

.pagination-info {
  font-size: 0.75rem;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .drafts-page {
    padding: 16px;
  }

  .draft-list.grid-view {
    grid-template-columns: 1fr;
  }

  .draft-card {
    flex-direction: column;
    padding: 16px;
    gap: 12px;
  }

  .draft-card-actions {
    width: 100%;
    justify-content: flex-end;
    padding-top: 12px;
    border-top: 1px solid var(--glass-border);
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
