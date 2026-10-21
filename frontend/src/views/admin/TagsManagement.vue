<template>
  <div class="admin-tags">
    <div class="admin-container">
      <header class="page-heading">
        <div>
          <h1>标签管理</h1>
          <p>为文章建立标签，方便读者按主题查找。</p>
        </div>
      </header>

      <form class="create-form" @submit.prevent="createTag">
        <h2 class="form-title">新建标签</h2>
        <div class="form-row">
          <input
            v-model="newTagName"
            type="text"
            class="form-input"
            aria-label="标签名称"
            placeholder="输入标签名称（最多20字）"
            maxlength="20"
          />
          <button class="btn btn-primary" type="submit" :disabled="!newTagName.trim() || creating">
            {{ creating ? '创建中…' : '创建' }}
          </button>
        </div>
      </form>

      <div v-if="loading" class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>文章数</th>
              <th>操作</th>
            </tr>
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

      <div v-else-if="error" class="state-panel" role="alert">
        <Icon name="info" :size="20" />
        <p>{{ error }}</p>
        <button class="btn btn-secondary" type="button" @click="fetchTags">重新加载</button>
      </div>

      <div v-else class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>文章数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tags.length === 0">
              <td colspan="4" class="empty-cell">
                <div class="empty-content"><Icon name="tag" :size="22" /><span>暂无标签</span></div>
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
                  <button class="btn btn-ghost" type="button" @click="startEdit(tag)">编辑</button>
                  <button class="btn btn-ghost danger" type="button" @click="deleteTag(tag)">
                    <Icon name="trash" :size="14" />删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <nav v-if="totalPages > 1" class="pagination" aria-label="标签分页">
        <button type="button" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
          <Icon name="chevron-left" :size="16" />上一页
        </button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button
          type="button"
          :disabled="currentPage >= totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页<Icon name="chevron-right" :size="16" />
        </button>
      </nav>
    </div>

    <Modal :show="editDialog" title="编辑标签" @close="editDialog = false">
      <div class="edit-form">
        <label class="edit-label" for="tag-edit-name">标签名称</label>
        <input
          id="tag-edit-name"
          v-model="editName"
          type="text"
          class="form-input"
          maxlength="20"
          placeholder="输入标签名称（最多20字）"
        />
      </div>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="editDialog = false">取消</button>
        <button
          class="btn btn-primary"
          type="button"
          :disabled="!editName.trim()"
          @click="submitEdit"
        >
          保存
        </button>
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
import Icon from '../../components/common/Icon.vue'

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
  if (name.length > 20) {
    toast.warning('标签名称不能超过20个字符')
    return
  }
  creating.value = true
  try {
    await adminApi.createTag({ name })
    toast.success('标签创建成功')
    newTagName.value = ''
    fetchTags()
  } catch (err: any) {
    logger.error('Failed to create tag', { error: err.message })
    toast.error(err.userMessage || '创建失败，请重试')
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
  if (editName.value.trim().length > 20) {
    toast.warning('标签名称不能超过20个字符')
    return
  }
  try {
    await adminApi.updateTag(editingTag.value.id, { name: editName.value.trim() })
    toast.success('标签更新成功')
    editDialog.value = false
    editingTag.value.name = editName.value.trim()
  } catch (err: any) {
    logger.error('Failed to update tag', { error: err.message })
    toast.error(err.userMessage || '更新失败，请重试')
  }
}

const deleteTag = async (tag: any) => {
  const ok = await confirm(`确定删除标签「${tag.name}」吗？`, '删除标签')
  if (!ok) return
  try {
    await adminApi.deleteTag(tag.id)
    tags.value = tags.value.filter((t) => t.id !== tag.id)
    toast.success('删除成功')
    // 删掉当前页最后一条后必须回到上一页并重新拉取，否则分页器会停在一个已不存在的页码上。
    if (tags.value.length === 0 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchTags()
  } catch (err: any) {
    logger.error('Failed to delete tag', { error: err.message })
    toast.error('删除失败')
  }
}

const changePage = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchTags()
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.admin-tags {
  min-height: 0;
}

.admin-container {
  max-width: 1000px;
  margin: 0 auto;
}

.page-heading {
  margin-bottom: 18px;
}

.page-heading h1 {
  margin: 0;
  font-size: var(--text-2xl);
}

.page-heading p {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.create-form {
  padding: 18px;
  margin-bottom: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}

.form-title {
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: var(--text-base);
  font-weight: 600;
}

.form-row {
  display: flex;
  gap: 8px;
}

.form-input {
  flex: 1;
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: var(--text-base);
  outline: none;
}

.form-input:focus-visible {
  border-color: var(--accent);
  outline: 2px solid var(--accent-light);
  outline-offset: 1px;
}

.form-input::placeholder {
  color: var(--text-muted);
}

.table-container {
  overflow-x: auto;
  overscroll-behavior-x: contain;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th,
.data-table td {
  padding: 13px 15px;
  border-bottom: 1px solid var(--border-light);
  white-space: nowrap;
}

.data-table th {
  background: var(--bg-secondary);
  color: var(--text-muted);
  font-size: var(--text-xs);
  font-weight: 600;
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.id-cell {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.tag-name {
  min-width: 160px;
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 500;
  overflow-wrap: anywhere;
  white-space: normal;
}

.count-badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  background: var(--accent-light);
  color: var(--accent);
  font-size: var(--text-xs);
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 4px;
}

.actions .btn {
  min-height: 36px;
  padding-inline: 10px;
  font-size: var(--text-xs);
}

.actions .danger {
  color: var(--error);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding-top: 18px;
}

.page-info {
  color: var(--text-muted);
  font-size: var(--text-xs);
}

.pagination button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 44px;
  padding-inline: 10px;
  border-radius: var(--radius);
  color: var(--accent);
  font-size: var(--text-xs);
}

.pagination button:hover:not(:disabled) {
  background: var(--accent-light);
}

.pagination button:disabled {
  color: var(--text-muted);
  cursor: default;
  opacity: 0.5;
}

.empty-cell {
  padding: 40px 20px;
  text-align: center;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.state-panel {
  display: grid;
  justify-items: start;
  gap: 14px;
  padding: 28px 22px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
  color: var(--text-secondary);
}

.state-panel p {
  margin: 0;
  font-size: var(--text-sm);
}

.sk-line {
  height: 14px;
  border-radius: var(--radius-xs);
  background: linear-gradient(
    90deg,
    var(--skeleton-base) 25%,
    var(--skeleton-highlight) 50%,
    var(--skeleton-base) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-line.w-30 {
  width: 30%;
}
.sk-line.w-40 {
  width: 40%;
}
.sk-line.w-50 {
  width: 50%;
}
.sk-line.w-60 {
  width: 60%;
}

.edit-form {
  padding: 4px 0;
}

.edit-label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  font-weight: 500;
}

@media (max-width: 640px) {
  .form-row {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
