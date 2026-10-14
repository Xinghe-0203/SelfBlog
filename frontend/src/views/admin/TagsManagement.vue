<template>
  <div class="admin-tags">
    <div class="admin-container">
      <div class="page-header">
        <h2>标签管理</h2>
      </div>

      <div class="create-form glass">
        <h3 class="form-title">新建标签</h3>
        <div class="form-row">
          <input v-model="newTagName" type="text" class="form-input" placeholder="输入标签名称..." @keyup.enter="createTag" />
          <button class="btn btn-primary" :disabled="!newTagName.trim() || creating" @click="createTag">
            {{ creating ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-skeleton">
        <div class="table-container glass">
          <table class="data-table">
            <thead>
              <tr><th>ID</th><th>名称</th><th>文章数</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="i in 5" :key="i">
                <td><div class="sk-line w-30"></div></td>
                <td><div class="sk-line w-60"></div></td>
                <td><div class="sk-line w-40"></div></td>
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
        <button class="btn btn-primary" @click="fetchTags">重试</button>
      </div>

      <div v-else class="table-container glass">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>名称</th><th>文章数</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-if="tags.length === 0">
              <td colspan="4" class="empty-cell">
                <div class="empty-content">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                    <line x1="7" y1="7" x2="7.01" y2="7"/>
                  </svg>
                  <span>暂无标签</span>
                </div>
              </td>
            </tr>
            <tr v-for="tag in tags" :key="tag.id">
              <td class="id-cell">{{ tag.id }}</td>
              <td class="tag-name">{{ tag.name }}</td>
              <td>
                <span class="count-badge">{{ tag.postCount || 0 }}</span>
              </td>
              <td>
                <div class="actions">
                  <button class="btn btn-xs btn-ghost" @click="startEdit(tag)">编辑</button>
                  <button class="btn btn-xs btn-ghost danger" @click="deleteTag(tag)">
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

    <Modal :show="editDialog" title="编辑标签" @close="editDialog = false">
      <div class="edit-form">
        <label class="edit-label">标签名称</label>
        <input v-model="editName" type="text" class="form-input" placeholder="输入标签名称" />
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
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { useConfirm } from '../../composables/useConfirm'
import Modal from '../../components/common/Modal.vue'

const logger = useLogger('AdminTags')
const { confirm, ConfirmDialog } = useConfirm()

const tags = ref<any[]>([])
const currentPage = ref(1)
const totalPages = ref(1)
const loading = ref(true)
const error = ref('')
const newTagName = ref('')
const creating = ref(false)
const editDialog = ref(false)
const editName = ref('')
const editingTag = ref<any | null>(null)

const fetchTags = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await adminApi.getTagList({ pageNum: currentPage.value, pageSize: 20 })
    const data = response.data || {}
    tags.value = data.records || []
    totalPages.value = data.pages || 1
  } catch (err: any) {
    logger.error('Failed to fetch tags', { error: err.message })
    error.value = '加载失败，请重试'
    tags.value = []
  } finally {
    loading.value = false
  }
}

const createTag = async () => {
  const name = newTagName.value.trim()
  if (!name) return
  creating.value = true
  try {
    await adminApi.createTag({ name })
    toast.success('标签创建成功')
    newTagName.value = ''
    fetchTags()
  } catch (err: any) {
    logger.error('Failed to create tag', { error: err.message })
    toast.error('创建失败')
  } finally {
    creating.value = false
  }
}

const startEdit = (tag: any) => {
  editingTag.value = tag
  editName.value = tag.name
  editDialog.value = true
}

const submitEdit = async () => {
  if (!editName.value.trim() || !editingTag.value) return
  try {
    await adminApi.updateTag(editingTag.value.id, { name: editName.value.trim() })
    toast.success('标签更新成功')
    editDialog.value = false
    editingTag.value.name = editName.value.trim()
  } catch (err: any) {
    logger.error('Failed to update tag', { error: err.message })
    toast.error('更新失败')
  }
}

const deleteTag = async (tag: any) => {
  const ok = await confirm(`确定删除标签「${tag.name}」吗？`, '删除标签')
  if (!ok) return
  try {
    await adminApi.deleteTag(tag.id)
    tags.value = tags.value.filter(t => t.id !== tag.id)
    toast.success('删除成功')
  } catch (err: any) {
    logger.error('Failed to delete tag', { error: err.message })
    toast.error('删除失败')
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  fetchTags()
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.admin-tags {
  padding: var(--spacing-lg);
  min-height: 100vh;
}

.admin-container {
  max-width: 1000px;
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

.form-row {
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

.tag-name {
  font-weight: 500;
  color: var(--text-primary);
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

.actions {
  display: flex;
  gap: var(--spacing-xs);
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

.edit-form {
  padding: var(--spacing-sm) 0;
}

.edit-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-sm);
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
