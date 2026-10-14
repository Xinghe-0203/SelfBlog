<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { adminApi } from '@/api/admin'
import { formatDate } from '@/utils'
import { toast } from '@/utils/toast'
import { useConfirm } from '@/composables/useConfirm'
import { useSiteStore } from '@/stores/site'
import type { Post } from '@/types'

const { confirm, ConfirmDialog } = useConfirm()
const siteStore = useSiteStore()
const posts = ref<Post[]>([])
const keyword = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const loading = ref(true)
const error = ref('')
const pageSize = 20

const fetchPosts = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await adminApi.getPostList({
      pageNum: currentPage.value,
      pageSize,
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined
    })
    const data = response.data || {}
    posts.value = data.records || []
    totalPages.value = Math.max(1, data.pages || 1)
  } catch {
    posts.value = []
    error.value = '文章列表暂时无法加载。'
  } finally {
    loading.value = false
  }
}

const submitSearch = () => {
  currentPage.value = 1
  fetchPosts()
}

const changePage = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchPosts()
}

const statusLabel = (status: number) => {
  if (status === 1) return '公开'
  if (status === 2) return '已隐藏'
  return '未发布'
}

const deletePost = async (post: Post) => {
  const confirmed = await confirm(`确定删除《${post.title}》吗？此操作不可恢复。`, '删除文章')
  if (!confirmed) return
  try {
    await adminApi.deletePost(post.id)
    posts.value = posts.value.filter((item) => item.id !== post.id)
    toast.success('文章已删除')
    if (posts.value.length === 0 && currentPage.value > 1) {
      currentPage.value -= 1
      await fetchPosts()
    }
  } catch {
    toast.error('删除失败，请稍后重试')
  }
}

watch(statusFilter, () => {
  currentPage.value = 1
  fetchPosts()
})

onMounted(fetchPosts)
</script>

<template>
  <div class="admin-posts">
    <header class="page-heading">
      <div>
        <p class="eyebrow">CONTENT</p>
        <h1>文章管理</h1>
        <p>管理博客文章，编辑内容或检查前台展示。</p>
      </div>
      <router-link :to="`${siteStore.adminRoot}/write`" class="new-post">＋ 写文章</router-link>
    </header>

    <form class="toolbar" @submit.prevent="submitSearch">
      <label class="search-box">
        <span aria-hidden="true">⌕</span>
        <input v-model="keyword" type="search" placeholder="搜索文章标题或关键词" />
      </label>
      <select v-model="statusFilter" aria-label="筛选文章状态">
        <option value="">所有状态</option>
        <option value="1">公开</option>
        <option value="0">未发布</option>
        <option value="2">已隐藏</option>
      </select>
      <button type="submit" class="search-button">搜索</button>
    </form>

    <div v-if="loading" class="state-panel" role="status">正在加载文章…</div>
    <div v-else-if="error" class="state-panel state-panel--error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="fetchPosts">重新加载</button>
    </div>
    <div v-else-if="posts.length === 0" class="state-panel">
      没有找到文章。<router-link :to="`${siteStore.adminRoot}/write`">写一篇新文章</router-link>
    </div>

    <div v-else class="post-table-wrap">
      <table class="post-table">
        <thead>
          <tr>
            <th>文章</th>
            <th>状态</th>
            <th>发布时间</th>
            <th>阅读</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="post in posts" :key="post.id">
            <td class="title-cell">
              <router-link :to="`${siteStore.adminRoot}/edit/${post.id}`">{{ post.title }}</router-link>
              <span v-if="post.category" class="category">{{ post.category }}</span>
            </td>
            <td>
              <span class="status" :class="{ 'status-public': post.status === 1 }">{{
                statusLabel(post.status)
              }}</span>
            </td>
            <td class="muted">{{ formatDate(post.createTime || post.createdAt, 'YYYY-MM-DD') }}</td>
            <td class="muted">{{ post.viewCount || 0 }}</td>
            <td>
              <div class="row-actions">
                <router-link :to="`/post/${post.id}`">查看</router-link>
                <router-link :to="`${siteStore.adminRoot}/edit/${post.id}`">编辑</router-link>
                <button type="button" @click="deletePost(post)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav v-if="!loading && !error && totalPages > 1" class="pagination" aria-label="文章管理分页">
      <button type="button" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
        ← 上一页
      </button>
      <span>{{ currentPage }} / {{ totalPages }}</span>
      <button
        type="button"
        :disabled="currentPage >= totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页 →
      </button>
    </nav>
    <ConfirmDialog />
  </div>
</template>

<style scoped>
.admin-posts {
  max-width: 1100px;
  margin: 0 auto;
}
.page-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 23px;
}
.eyebrow {
  margin: 0 0 7px;
  color: var(--primary);
  font-size: 0.67rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.page-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.8rem;
  letter-spacing: -0.04em;
}
.page-heading p:last-child {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 0.83rem;
}
.new-post {
  flex: 0 0 auto;
  padding: 10px 13px;
  border-radius: 8px;
  background: var(--primary);
  color: white;
  font-size: 0.8rem;
  font-weight: 600;
  text-decoration: none;
}
.new-post:hover {
  background: var(--primary-hover);
}
.toolbar {
  display: flex;
  gap: 9px;
  margin-bottom: 16px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 9px;
  flex: 1;
  min-width: 0;
  padding: 0 11px;
  border: 1px solid var(--border-solid);
  border-radius: 8px;
  background: var(--surface-solid);
  color: var(--text-muted);
}
.search-box:focus-within {
  border-color: var(--primary);
}
.search-box input {
  width: 100%;
  min-width: 0;
  height: 40px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 0.82rem;
}
.toolbar select,
.search-button {
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--border-solid);
  border-radius: 8px;
  background: var(--surface-solid);
  color: var(--text-primary);
  font: inherit;
  font-size: 0.8rem;
}
.search-button {
  background: var(--text-primary);
  color: var(--surface-primary);
  cursor: pointer;
}
.state-panel {
  padding: 38px 20px;
  border: 1px dashed var(--border-solid);
  border-radius: 11px;
  color: var(--text-muted);
  text-align: center;
}
.state-panel a {
  margin-left: 6px;
  color: var(--primary);
  text-decoration: none;
}
.state-panel--error {
  color: var(--text-secondary);
}
.state-panel button {
  margin-left: 9px;
  padding: 7px 10px;
  border: 1px solid var(--border-solid);
  border-radius: 7px;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
}
.post-table-wrap {
  overflow: auto;
  border: 1px solid var(--border-solid);
  border-radius: 10px;
  background: var(--surface-solid);
}
.post-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.post-table th,
.post-table td {
  padding: 14px 15px;
  border-bottom: 1px solid var(--border-solid);
  white-space: nowrap;
}
.post-table th {
  background: var(--bg-secondary);
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 600;
}
.post-table tbody tr:last-child td {
  border-bottom: 0;
}
.title-cell {
  min-width: 260px;
  white-space: normal !important;
}
.title-cell > a {
  color: var(--text-primary);
  font-size: 0.84rem;
  font-weight: 600;
  text-decoration: none;
}
.title-cell > a:hover {
  color: var(--primary);
}
.category {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 0.7rem;
}
.status {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 20px;
  background: var(--bg-secondary);
  color: var(--text-muted);
  font-size: 0.7rem;
}
.status-public {
  background: var(--primary-light);
  color: var(--primary);
}
.muted {
  color: var(--text-muted);
  font-size: 0.76rem;
}
.row-actions {
  display: flex;
  gap: 10px;
}
.row-actions a,
.row-actions button {
  padding: 0;
  border: 0;
  background: none;
  color: var(--text-secondary);
  cursor: pointer;
  font: inherit;
  font-size: 0.75rem;
  text-decoration: none;
}
.row-actions a:hover {
  color: var(--primary);
}
.row-actions button:hover {
  color: var(--error);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 17px;
  padding: 19px 0;
  color: var(--text-muted);
  font-size: 0.78rem;
}
.pagination button {
  border: 0;
  background: none;
  color: var(--primary);
  cursor: pointer;
  font: inherit;
}
.pagination button:disabled {
  color: var(--text-muted);
  cursor: default;
  opacity: 0.5;
}
@media (max-width: 660px) {
  .page-heading {
    align-items: start;
  }
  .toolbar {
    flex-wrap: wrap;
  }
  .search-box {
    flex-basis: 100%;
  }
  .toolbar select {
    flex: 1;
  }
  .post-table th,
  .post-table td {
    padding: 11px 10px;
  }
  .title-cell {
    min-width: 190px;
  }
  .row-actions {
    gap: 8px;
  }
}
</style>
