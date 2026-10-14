<template>
  <div class="admin-users">
    <div class="admin-container">
      <div class="page-header">
        <h2>用户管理</h2>
        <div class="search-bar glass">
          <div class="search-input-wrapper">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input v-model="searchQuery" type="text" placeholder="搜索用户名、邮箱..." @keyup.enter="fetchUsers" />
          </div>
          <button class="btn btn-primary btn-sm" @click="fetchUsers">搜索</button>
        </div>
      </div>

      <div v-if="loading" class="loading-skeleton">
        <div class="table-container glass">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户</th>
                <th>邮箱</th>
                <th>角色</th>
                <th>状态</th>
                <th>封禁</th>
                <th>注册时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in 5" :key="i">
                <td><div class="sk-line w-30"></div></td>
                <td><div class="sk-line w-60"></div></td>
                <td><div class="sk-line w-70"></div></td>
                <td><div class="sk-line w-40"></div></td>
                <td><div class="sk-line w-40"></div></td>
                <td><div class="sk-line w-60"></div></td>
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
        <button class="btn btn-primary" @click="fetchUsers">重试</button>
      </div>

      <div v-else class="table-container glass">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>状态</th>
              <th>封禁</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="users.length === 0">
              <td colspan="8" class="empty-cell">
                <div class="empty-content">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                  </svg>
                  <span>暂无用户数据</span>
                </div>
              </td>
            </tr>
            <tr v-for="user in users" :key="user.id">
              <td class="id-cell">{{ user.id }}</td>
              <td>
                <div class="user-cell">
                  <img :src="user.avatar || '/default-avatar.png'" :alt="user.username" class="user-avatar" />
                  <span class="username">{{ user.nickname || user.username }}</span>
                </div>
              </td>
              <td class="email-cell">{{ user.email }}</td>
              <td>
                <span class="badge" :class="user.role === 'admin' ? 'badge-warning' : 'badge-info'">
                  <span class="badge-dot" :class="user.role === 'admin' ? 'warning' : 'info'"></span>
                  {{ user.role === 'admin' ? '管理员' : '用户' }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="user.status === 1 ? 'status-active' : 'status-disabled'">
                  <span class="status-dot" :class="user.status === 1 ? 'active' : 'disabled'"></span>
                  {{ user.status === 1 ? '正常' : '禁用' }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="user.banned ? 'status-banned' : 'status-normal'">
                  <span class="status-dot" :class="user.banned ? 'banned' : 'normal'"></span>
                  {{ user.banned ? '已封禁' : '正常' }}
                </span>
              </td>
              <td class="time-cell">{{ formatDate(user.createTime) }}</td>
              <td>
                <div class="actions">
                  <button class="btn btn-xs btn-ghost" @click="toggleUserStatus(user)" :title="user.status === 1 ? '禁用' : '启用'">
                    {{ user.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button class="btn btn-xs btn-ghost" :class="user.banned ? 'btn-success-outline' : 'btn-danger-outline'" @click="toggleBan(user)">
                    {{ user.banned ? '解封' : '封禁' }}
                  </button>
                  <button class="btn btn-xs btn-ghost" @click="resetPassword(user)">重置密码</button>
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
    <ConfirmDialog />
    <Modal :show="showPasswordDialog" title="新密码" @close="showPasswordDialog = false">
      <div class="password-display">
        <code class="password-code">{{ newPassword }}</code>
        <button class="btn btn-sm btn-outline" @click="copyPassword">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
          </svg>
          复制
        </button>
      </div>
      <p class="password-warning">请将密码安全地发送给用户，关闭后将无法再次查看</p>
      <template #footer>
        <button class="btn btn-primary" @click="showPasswordDialog = false">关闭</button>
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api/admin'
import { formatDate, copyToClipboard } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { useConfirm } from '../../composables/useConfirm'
import Modal from '../../components/common/Modal.vue'

const logger = useLogger('AdminUsers')
const { confirm, ConfirmDialog } = useConfirm()

const users = ref<any[]>([])
const searchQuery = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const loading = ref(true)
const error = ref('')
const showPasswordDialog = ref(false)
const newPassword = ref('')

const fetchUsers = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await adminApi.getUserList({
      pageNum: currentPage.value,
      pageSize: 20,
      keyword: searchQuery.value
    })
    const pageData = response.data || {}
    users.value = pageData.records || []
    totalPages.value = pageData.pages || 1
  } catch (err: any) {
    logger.error('Failed to fetch users', { error: err.message })
    error.value = '加载失败，请重试'
    users.value = []
  } finally {
    loading.value = false
  }
}

const toggleUserStatus = async (user: any) => {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  const ok = await confirm(`确定${action}该用户吗？`, `${action}用户`)
  if (!ok) return
  try {
    await adminApi.handleUserStatus(user.id, { status: newStatus })
    user.status = newStatus
    toast.success(`${action}成功`)
  } catch (err: any) {
    logger.error('Failed to toggle user status', { error: err.message })
    toast.error('操作失败')
  }
}

const resetPassword = async (user: any) => {
  const ok = await confirm('确定重置该用户密码吗？重置后的密码将显示一次，请注意保管。', '重置密码')
  if (!ok) return
  try {
    const res = await adminApi.resetUserPassword(user.id)
    newPassword.value = res.data as string
    showPasswordDialog.value = true
    toast.success('密码已重置成功')
  } catch (err: any) {
    logger.error('Failed to reset password', { error: err.message })
    toast.error('操作失败')
  }
}

const copyPassword = async () => {
  const ok = await copyToClipboard(newPassword.value)
  if (ok) {
    toast.success('密码已复制到剪贴板')
  } else {
    toast.error('复制失败，请手动复制')
  }
}

const toggleBan = async (user: any) => {
  const action = user.banned ? '解封' : '封禁'
  const ok = await confirm(`确定${action}该用户吗？`, `${action}用户`)
  if (!ok) return
  try {
    await adminApi.banUser(user.id, !user.banned)
    user.banned = !user.banned
    toast.success(`${action}成功`)
  } catch (err: any) {
    logger.error('Failed to toggle ban', { error: err.message })
    toast.error('操作失败')
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  fetchUsers()
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.admin-users {
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

.id-cell {
  font-family: var(--font-mono);
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 2px solid var(--glass-border);
}

.username {
  font-weight: 500;
  color: var(--text-primary);
}

.email-cell {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.time-cell {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: var(--radius-full);
  background: var(--info-light);
  color: var(--info);
}

.badge-warning {
  background: var(--warning-light);
  color: var(--warning);
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
}

.badge-dot.warning { background: var(--warning); }
.badge-dot.info { background: var(--info); }

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

.status-banned {
  background: var(--error-light);
  color: var(--error);
}

.status-normal {
  background: var(--success-light);
  color: var(--success);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
}

.status-dot.active,
.status-dot.normal { background: var(--success); }

.status-dot.disabled,
.status-dot.banned { background: var(--error); }

.actions {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.btn-danger-outline {
  color: var(--error) !important;
}

.btn-danger-outline:hover {
  background: var(--error-light) !important;
  color: var(--error) !important;
}

.btn-success-outline {
  color: var(--success) !important;
}

.btn-success-outline:hover {
  background: var(--success-light) !important;
  color: var(--success) !important;
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
.sk-line.w-70 { width: 70%; }

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

.password-display {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  margin-bottom: var(--spacing-md);
}

.password-code {
  flex: 1;
  font-family: var(--font-mono);
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--primary);
  word-break: break-all;
  user-select: all;
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--glass-border);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.btn-outline:hover {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}

.password-warning {
  font-size: 0.8125rem;
  color: var(--warning);
  line-height: 1.5;
}
</style>
