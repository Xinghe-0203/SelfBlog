<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { adminApi } from '@/api/admin'
import { formatDate } from '@/utils'
import { toast } from '@/utils/toast'
import { useConfirm } from '@/composables/useConfirm'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { useSiteStore } from '@/stores/site'
import Icon from '@/components/common/Icon.vue'
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
/** 翻页、筛选与删除后重载会并发发请求：只有最后一次的响应可以写回列表与 loading。 */
const beginPostsRequest = useLatestRequest().begin

const fetchPosts = async () => {
  const isLatest = beginPostsRequest()
  loading.value = true
  error.value = ''
  try {
    const response = await adminApi.getPostList({
      pageNum: currentPage.value,
      pageSize,
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined
    })
    if (!isLatest()) return
    const data = response.data || {}
    posts.value = data.records || []
    totalPages.value = Math.max(1, data.pages || 1)
  } catch {
    if (!isLatest()) return
    posts.value = []
    error.value = '文章列表暂时无法加载。'
  } finally {
    if (isLatest()) loading.value = false
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
    // 本地过滤无法知道服务端剩余总数：回到上一页（若当前页已空）后重新拉取，totalPages 才会更新。
    if (posts.value.length === 0 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchPosts()
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
        <h1>文章管理</h1>
        <p>管理博客文章，编辑内容或检查前台展示。</p>
      </div>
      <router-link :to="`${siteStore.adminRoot}/write`" class="new-post"
        ><Icon name="plus" :size="16" />写文章</router-link
      >
    </header>

    <form class="toolbar" @submit.prevent="submitSearch">
      <label class="search-box">
        <Icon name="search" :size="16" />
        <input
          v-model="keyword"
          type="search"
          aria-label="搜索文章标题或关键词"
          placeholder="搜索文章标题或关键词"
        />
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
              <router-link :to="`${siteStore.adminRoot}/edit/${post.id}`">{{
                post.title
              }}</router-link>
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
        <Icon name="chevron-left" :size="16" />上一页
      </button>
      <span>{{ currentPage }} / {{ totalPages }}</span>
      <button
        type="button"
        :disabled="currentPage >= totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页<Icon name="chevron-right" :size="16" />
      </button>
    </nav>
    <ConfirmDialog />
  </div>
</template>

<style scoped>
.admin-posts {
  margin: 0 auto;
}
.page-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 22px;
}
.page-heading h1 {
  margin: 0;
  font-size: var(--text-2xl);
}
.page-heading p:last-child {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.new-post {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 44px;
  padding-inline: 14px;
  border-radius: var(--radius);
  background: var(--accent);
  color: var(--text-inverse);
  font-size: var(--text-sm);
  font-weight: 600;
}
.new-post:hover {
  background: var(--accent-dark);
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
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-muted);
}
.search-box:focus-within {
  border-color: var(--accent);
  outline: 2px solid var(--accent-light);
  outline-offset: 1px;
}
.search-box input {
  width: 100%;
  min-width: 0;
  height: 42px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
}
.toolbar select,
.search-button {
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
}
.search-button {
  border-color: var(--accent);
  background: var(--accent);
  color: var(--text-inverse);
  cursor: pointer;
}
.search-button:hover {
  background: var(--accent-dark);
}
.state-panel {
  display: grid;
  justify-items: start;
  gap: 14px;
  padding: 30px 22px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.state-panel--error {
  color: var(--text-secondary);
}
.state-panel button {
  min-height: 44px;
  padding-inline: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
}
.post-table-wrap {
  overflow-x: auto;
  overscroll-behavior-x: contain;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}
.post-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.post-table th,
.post-table td {
  padding: 13px 15px;
  border-bottom: 1px solid var(--border-light);
  white-space: nowrap;
}
.post-table th {
  color: var(--text-muted);
  background: var(--bg-secondary);
  font-size: var(--text-xs);
  font-weight: 600;
}
.post-table tbody tr:last-child td {
  border-bottom: 0;
}
.title-cell {
  min-width: 240px;
  white-space: normal !important;
  overflow-wrap: anywhere;
}
.title-cell > a {
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
}
.title-cell > a:hover {
  color: var(--accent);
}
.category {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.status {
  display: inline-block;
  padding: 3px 8px;
  border-radius: var(--radius-xs);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: var(--text-2xs);
}
.status-public {
  background: var(--accent-light);
  color: var(--accent);
}
.muted {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.row-actions {
  display: flex;
  gap: 4px;
}
.row-actions a,
.row-actions button {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding-inline: 9px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font: inherit;
  font-size: var(--text-xs);
}
.row-actions a:hover,
.row-actions button:hover {
  background: var(--bg-secondary);
  color: var(--accent);
}
.row-actions button:hover {
  color: var(--error);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 17px;
  padding: 18px 0 0;
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
  font: inherit;
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
@media (max-width: 660px) {
  .page-heading {
    align-items: start;
  }
  .new-post span {
    display: none;
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
}
</style>
