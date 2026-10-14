<template>
  <div class="admin-topics">
    <div class="admin-container">
      <div class="page-header">
        <h2>话题管理</h2>
      </div>

      <div class="create-form glass">
        <h3 class="form-title">新建话题</h3>
        <div class="form-group">
          <input v-model="newTopicName" type="text" class="form-input" placeholder="话题名称..." />
          <input v-model="newTopicDescription" type="text" class="form-input" placeholder="话题描述..." />
          <button class="btn btn-primary" :disabled="!newTopicName.trim() || creating" @click="createTopic">
            {{ creating ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-skeleton">
        <div class="table-container glass">
          <table class="data-table">
            <thead>
              <tr><th>ID</th><th>名称</th><th>描述</th><th>文章数</th><th>热度</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="i in 5" :key="i">
                <td><div class="sk-line w-30"></div></td>
                <td><div class="sk-line w-60"></div></td>
                <td><div class="sk-line w-80"></div></td>
                <td><div class="sk-line w-40"></div></td>
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
        <button class="btn btn-primary" @click="fetchTopics">重试</button>
      </div>

      <div v-else class="table-container glass">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>名称</th><th>描述</th><th>文章数</th><th>热度</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-if="topics.length === 0">
              <td colspan="8" class="empty-cell">
                <div class="empty-content">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
                  </svg>
                  <span>暂无话题</span>
                </div>
              </td>
            </tr>
            <tr v-for="topic in topics" :key="topic.id">
              <td class="id-cell">{{ topic.id }}</td>
              <td class="topic-name">{{ topic.name }}</td>
              <td class="desc-cell">{{ topic.description || '-' }}</td>
              <td>
                <span class="count-badge">{{ topic.postCount || 0 }}</span>
              </td>
              <td>
                <span class="trend-score" :class="trendingClass(topic.trendingScore)">
                  {{ topic.trendingScore || 0 }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="topic.status === 1 ? 'status-active' : 'status-disabled'">
                  <span class="status-dot" :class="topic.status === 1 ? 'active' : 'disabled'"></span>
                  {{ topic.status === 1 ? '正常' : '禁用' }}
                </span>
              </td>
              <td class="time-cell">{{ formatDate(topic.createTime) }}</td>
              <td>
                <div class="actions">
                  <button class="btn btn-xs btn-ghost" @click="startEdit(topic)">编辑</button>
                  <button class="btn btn-xs btn-ghost" @click="toggleStatus(topic)">
                    {{ topic.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button class="btn btn-xs btn-ghost danger" @click="deleteTopic(topic)">
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

    <Modal :show="editDialog" title="编辑话题" @close="editDialog = false">
      <div class="edit-form">
        <div class="edit-field">
          <label class="edit-label">话题名称</label>
          <input v-model="editName" type="text" class="form-input" placeholder="话题名称" />
        </div>
        <div class="edit-field">
          <label class="edit-label">话题描述</label>
          <textarea v-model="editDescription" class="form-textarea" placeholder="话题描述" rows="3"></textarea>
        </div>
        <div class="edit-field">
          <label class="edit-label">状态</label>
          <select v-model="editStatus" class="form-select">
            <option :value="1">正常</option>
            <option :value="0">禁用</option>
          </select>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-secondary" @click="editDialog = false">取消</button>
        <button class="btn btn-primary" :disabled="!editName.trim()" @click="submitEdit">保存</button>
      </template>
    </Modal>
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
import Modal from '../../components/common/Modal.vue'

const logger = useLogger('AdminTopics')
const { confirm, ConfirmDialog } = useConfirm()

const topics = ref<any[]>([])
const currentPage = ref(1)
const totalPages = ref(1)
const loading = ref(true)
const error = ref('')
const newTopicName = ref('')
const newTopicDescription = ref('')
const creating = ref(false)
const editDialog = ref(false)
const editName = ref('')
const editDescription = ref('')
const editStatus = ref(1)
const editingTopic = ref<any | null>(null)

const fetchTopics = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await adminApi.getTopicList({ pageNum: currentPage.value, pageSize: 20 })
    const data = response.data || {}
    topics.value = data.records || []
    totalPages.value = data.pages || 1
  } catch (err: any) {
    logger.error('Failed to fetch topics', { error: err.message })
    error.value = '加载失败，请重试'
    topics.value = []
  } finally {
    loading.value = false
  }
}

const createTopic = async () => {
  const name = newTopicName.value.trim()
  if (!name) return
  creating.value = true
  try {
    await adminApi.createTopic({ name, description: newTopicDescription.value.trim() })
    toast.success('话题创建成功')
    newTopicName.value = ''
    newTopicDescription.value = ''
    fetchTopics()
  } catch (err: any) {
    logger.error('Failed to create topic', { error: err.message })
    toast.error('创建失败')
  } finally {
    creating.value = false
  }
}

const trendingClass = (score: number | undefined) => {
  if (!score) return 'trend-low'
  if (score >= 80) return 'trend-high'
  if (score >= 40) return 'trend-mid'
  return 'trend-low'
}

const startEdit = (topic: any) => {
  editingTopic.value = topic
  editName.value = topic.name
  editDescription.value = topic.description || ''
  editStatus.value = topic.status
  editDialog.value = true
}

const submitEdit = async () => {
  if (!editName.value.trim() || !editingTopic.value) return
  try {
    await adminApi.updateTopic(editingTopic.value.id, {
      name: editName.value.trim(),
      description: editDescription.value.trim(),
      status: editStatus.value
    } as any)
    toast.success('话题更新成功')
    editDialog.value = false
    Object.assign(editingTopic.value, {
      name: editName.value.trim(),
      description: editDescription.value.trim(),
      status: editStatus.value
    })
  } catch (err: any) {
    logger.error('Failed to update topic', { error: err.message })
    toast.error('更新失败')
  }
}

const toggleStatus = async (topic: any) => {
  const newStatus = topic.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  const ok = await confirm(`确定${action}该话题吗？`, `${action}话题`)
  if (!ok) return
  try {
    await adminApi.updateTopicStatus(topic.id, { status: newStatus })
    topic.status = newStatus
    toast.success(`${action}成功`)
  } catch (err: any) {
    logger.error('Failed to toggle topic status', { error: err.message })
    toast.error('操作失败')
  }
}

const deleteTopic = async (topic: any) => {
  const ok = await confirm(`确定删除话题「${topic.name}」吗？此操作不可恢复。`, '删除话题')
  if (!ok) return
  try {
    await adminApi.deleteTopic(topic.id)
    topics.value = topics.value.filter(t => t.id !== topic.id)
    toast.success('删除成功')
  } catch (err: any) {
    logger.error('Failed to delete topic', { error: err.message })
    toast.error('删除失败')
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  fetchTopics()
}

onMounted(() => {
  fetchTopics()
})
</script>

<style scoped>
.admin-topics {
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

.create-form {
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
}

.form-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
}

.form-group {
  display: flex;
  gap: var(--spacing-sm);
}

.form-input {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: 0.875rem;
  background: var(--surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  color: var(--text-primary);
  outline: none;
  transition: all var(--transition);
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.form-input::placeholder {
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

.id-cell {
  font-family: var(--font-mono);
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.topic-name {
  font-weight: 500;
  color: var(--text-primary);
}

.desc-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.count-badge {
  display: inline-flex;
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: var(--radius-full);
  background: var(--primary-light);
  color: var(--primary);
}

.trend-score {
  display: inline-flex;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: var(--radius-full);
}

.trend-high {
  background: var(--error-light);
  color: var(--error);
}

.trend-mid {
  background: var(--warning-light);
  color: var(--warning);
}

.trend-low {
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

.edit-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) 0;
}

.edit-field {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.edit-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-textarea {
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: 0.875rem;
  background: var(--surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  color: var(--text-primary);
  outline: none;
  transition: all var(--transition);
  resize: vertical;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.form-select {
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: 0.875rem;
  background: var(--surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  color: var(--text-primary);
  outline: none;
  transition: all var(--transition);
  cursor: pointer;
}

.form-select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

@media (max-width: 768px) {
  .form-group {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
