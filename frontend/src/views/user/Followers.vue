<template>
  <div class="follow-page">
    <button class="back-btn" @click="router.back()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      返回
    </button>
    <div class="page-header">
      <h1>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="header-icon"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
        我的粉丝
      </h1>
      <span class="page-count">{{ total }} 人</span>
    </div>

    <div class="search-bar card">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
      <input v-model="searchQuery" type="text" class="search-input" placeholder="搜索粉丝..." />
      <button v-if="searchQuery" class="clear-btn" @click="searchQuery = ''">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>

    <div v-if="loading && followers.length === 0" class="skeleton-list">
      <div v-for="n in 3" :key="n" class="skeleton-user-item">
        <div class="skeleton skeleton-avatar"></div>
        <div class="skeleton-user-info">
          <div class="skeleton skeleton-user-name"></div>
          <div class="skeleton skeleton-user-bio"></div>
        </div>
      </div>
    </div>

    <div v-else-if="error" class="error-card card">
      <p>{{ error }}</p>
      <button class="btn btn-sm btn-primary" @click="fetchData">重试</button>
    </div>

    <div v-else-if="filteredFollowers.length === 0" class="empty-state card">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
      <p class="empty-title" v-if="searchQuery">未找到匹配的粉丝</p>
      <p class="empty-title" v-else>还没有粉丝</p>
      <p class="empty-text" v-if="!searchQuery">发布优质内容吸引更多关注吧</p>
      <router-link v-if="!searchQuery" to="/post-edit" class="btn btn-primary">写文章</router-link>
    </div>

    <div v-else class="user-list">
      <div v-for="user in filteredFollowers" :key="user.id" class="user-card card">
        <router-link :to="`/user/${user.id}`" class="user-card-left">
          <img :src="user.avatar || '/default-avatar.png'" :alt="user.nickname" class="user-avatar" @error="(e: Event) => (e.target as HTMLImageElement).src = '/default-avatar.png'" />
          <div class="user-info">
            <span class="user-name">{{ user.nickname || user.username }}</span>
            <span class="user-bio">{{ user.bio || '这个人很懒，什么都没写' }}</span>
          </div>
        </router-link>
        <div v-if="userStore.isLoggedIn" class="user-card-right">
          <button class="btn btn-sm" :class="user.isFollowing ? 'btn-secondary' : 'btn-primary'" @click="toggleFollow(user)" :disabled="followLoadingId === user.id">
            <template v-if="followLoadingId === user.id">
              <span class="loading-dots"></span>
            </template>
            <template v-else>
              <svg v-if="user.isFollowing" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="8" x2="23" y2="13"/><line x1="23" y1="8" x2="18" y2="13"/></svg>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
              {{ user.isFollowing ? '已关注' : '关注' }}
            </template>
          </button>
        </div>
      </div>

      <div v-if="totalPages > 1" class="pagination-section">
        <div class="pagination">
          <button class="pagination-btn" :disabled="page <= 1" @click="page--; fetchData()">上一页</button>
          <button v-for="p in totalPages" :key="p" class="pagination-btn" :class="{ active: p === page }" @click="page = p; fetchData()">{{ p }}</button>
          <button class="pagination-btn" :disabled="page >= totalPages" @click="page++; fetchData()">下一页</button>
        </div>
        <span class="pagination-info">共 {{ total }} 人</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { followApi } from '../../api/follow'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('Followers')
const followers = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const total = ref(0)
const totalPages = ref(1)
const pageSize = 10
const followLoadingId = ref<number | null>(null)
const searchQuery = ref('')

const filteredFollowers = computed(() => {
  if (!searchQuery.value.trim()) return followers.value
  const query = searchQuery.value.toLowerCase()
  return followers.value.filter(u =>
    (u.nickname || '').toLowerCase().includes(query) ||
    (u.username || '').toLowerCase().includes(query) ||
    (u.bio || '').toLowerCase().includes(query)
  )
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const response = await followApi.getFollowers(userStore.userId!, { pageNum: page.value, pageSize })
    const data = response.data || {}
    followers.value = (data.records || []).map(u => ({ ...u, isFollowing: u.isFollowing ?? false }))
    total.value = data.total || 0
    totalPages.value = data.pages || 1
  } catch (err: any) {
    logger.error('Failed to fetch followers', { error: err.message })
    error.value = '加载粉丝列表失败'
  } finally {
    loading.value = false
  }
}

async function toggleFollow(user: any) {
  followLoadingId.value = user.id
  const prevFollowing = user.isFollowing
  try {
    if (user.isFollowing) {
      await followApi.unfollow(user.id)
      user.isFollowing = false
    } else {
      await followApi.toggleFollow(user.id)
      user.isFollowing = true
    }
    toast.success(user.isFollowing ? '已关注' : '已取消关注')
  } catch (err: any) {
    user.isFollowing = prevFollowing
    logger.error('Failed to toggle follow', { error: err.message })
    toast.error(err.response?.data?.message || '操作失败')
  } finally {
    followLoadingId.value = null
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.follow-page {
  max-width: 800px;
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
  gap: 12px;
  margin-bottom: 20px;
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

.page-count {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  gap: 10px;
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
  transition: all var(--transition);
}

.search-bar:focus-within {
  border-color: var(--primary);
  box-shadow: var(--shadow-md), 0 0 0 3px var(--primary-light);
}

.search-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.9375rem;
  color: var(--text-primary);
  outline: none;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.clear-btn {
  padding: 4px;
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  transition: all var(--transition);
}

.clear-btn:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-user-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--surface);
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.skeleton-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-user-name {
  width: 120px;
  height: 16px;
}

.skeleton-user-bio {
  width: 200px;
  height: 13px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  gap: 12px;
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

.user-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  pointer-events: none;
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md), var(--glass-shadow-wet);
}

.user-card-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
  text-decoration: none;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--surface-solid);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition);
}

.user-card-left:hover .user-avatar {
  transform: scale(1.05);
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.user-bio {
  font-size: 0.8125rem;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-card-right {
  flex-shrink: 0;
}

.loading-dots {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: var(--radius-full);
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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
  .follow-page {
    padding: 16px;
  }

  .user-card {
    padding: 14px;
  }

  .user-avatar {
    width: 40px;
    height: 40px;
  }
}
</style>
