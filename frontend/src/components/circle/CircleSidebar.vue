<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { circleApi } from '../../api/circle'
import { topicApi } from '../../api/topic'
import { formatNumber } from '../../utils'

const hotTopics = ref<any[]>([])
const hotTopicsLoading = ref(false)
const hotPosts = ref<any[]>([])
const hotPostsLoading = ref(false)

let sidebarTimer: ReturnType<typeof setInterval> | null = null

const refreshSidebarData = async () => {
  hotTopicsLoading.value = true
  hotPostsLoading.value = true
  try {
    const res = await topicApi.getHotTopics()
    const data = res.data
    hotTopics.value = Array.isArray(data) ? data : ((data as any)?.records || [])
  } catch { /* ignore */ } finally { hotTopicsLoading.value = false }
  try {
    const res = await circleApi.getRecommendFeed({ pageNum: 1, pageSize: 5 })
    const list = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    hotPosts.value = list.slice(0, 5)
  } catch { /* ignore */ } finally { hotPostsLoading.value = false }
}

onMounted(async () => {
  await refreshSidebarData()
  sidebarTimer = setInterval(refreshSidebarData, 60000)
})

onBeforeUnmount(() => {
  if (sidebarTimer) clearInterval(sidebarTimer)
})
</script>

<template>
  <aside class="sidebar hide-mobile">
    <!-- Hot Topics -->
    <section class="sidebar__section">
      <h3 class="sidebar__title">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 9h16M4 15h16M10 3l-2 6M14 15l-2 6M14 3l2 6M10 15l-2 6"/></svg>
        热门话题
      </h3>
      <div v-if="hotTopicsLoading" class="sidebar__loading">
        <span class="sidebar__spinner"></span>
        加载中...
      </div>
      <div v-else-if="hotTopics.length" class="sidebar__topic-cloud">
        <router-link
          v-for="(topic, idx) in hotTopics"
          :key="topic.id"
          :to="`/topic/${topic.id}`"
          class="sidebar__topic-chip"
          :class="{ 'sidebar__topic-chip--lg': idx < 2, 'sidebar__topic-chip--sm': idx >= 4 }"
        >
          #{{ topic.name }}
        </router-link>
      </div>
      <div v-else class="sidebar__empty">暂无热门话题</div>
    </section>

    <!-- Hot Posts -->
    <section class="sidebar__section">
      <h3 class="sidebar__title">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
        热门动态
      </h3>
      <div v-if="hotPostsLoading" class="sidebar__loading">
        <span class="sidebar__spinner"></span>
        加载中...
      </div>
      <div v-else-if="hotPosts.length" class="sidebar__post-list">
        <router-link
          v-for="hp in hotPosts"
          :key="hp.id"
          :to="`/circle/${hp.id}`"
          class="sidebar__post-item"
        >
          <div class="sidebar__post-row">
            <img :src="hp.userAvatar || '/default-avatar.png'" :alt="hp.userNickname" class="sidebar__post-avatar" />
            <span class="sidebar__post-name">{{ hp.userNickname || hp.userUsername }}</span>
          </div>
          <p class="sidebar__post-text">{{ hp.content?.substring(0, 48) }}{{ hp.content?.length > 48 ? '...' : '' }}</p>
          <div class="sidebar__post-stats">
            <span>{{ formatNumber(hp.likeCount || 0) }} 赞</span>
            <span>{{ formatNumber(hp.commentCount || 0) }} 评</span>
          </div>
        </router-link>
      </div>
      <div v-else class="sidebar__empty">暂无热门动态</div>
    </section>
  </aside>
</template>

<style scoped>
.sidebar__section {
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  position: sticky;
  top: calc(var(--navbar-height) + var(--spacing-md));
  margin-bottom: var(--spacing-md);
}

.sidebar__section:last-child {
  margin-bottom: 0;
}

.sidebar__title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
  letter-spacing: -0.01em;
}

.sidebar__title svg {
  color: var(--primary);
  opacity: 0.8;
}

/* Topic Cloud */
.sidebar__topic-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sidebar__topic-chip {
  display: inline-block;
  padding: 4px 12px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all var(--transition);
}

.sidebar__topic-chip:hover {
  background: var(--primary-subtle);
  border-color: var(--primary);
  color: var(--primary);
}

.sidebar__topic-chip--lg {
  font-size: 0.875rem;
  padding: 5px 14px;
  font-weight: 600;
  color: var(--primary);
  background: var(--primary-subtle);
}

.sidebar__topic-chip--sm {
  font-size: 0.75rem;
  padding: 3px 10px;
}

/* Post List */
.sidebar__post-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.sidebar__post-item {
  display: block;
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius);
  text-decoration: none;
  transition: all var(--transition);
  border: 1px solid transparent;
}

.sidebar__post-item:hover {
  background: var(--gray-50);
  border-color: var(--border);
}

.sidebar__post-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-0_5);
}

.sidebar__post-avatar {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.sidebar__post-name {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-primary);
}

.sidebar__post-text {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  line-height: 1.45;
  margin: 0 0 var(--spacing-0_5);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sidebar__post-stats {
  display: flex;
  gap: var(--spacing-sm);
  font-size: 0.6875rem;
  color: var(--text-muted);
}

/* Loading & Empty */
.sidebar__loading {
  text-align: center;
  padding: var(--spacing-md);
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.sidebar__empty {
  text-align: center;
  padding: var(--spacing-md);
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.sidebar__spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: var(--radius-full);
  animation: sidebarSpin 0.6s linear infinite;
  vertical-align: middle;
  margin-right: 4px;
}

@keyframes sidebarSpin {
  to { transform: rotate(360deg); }
}

@media (max-width: 640px) {
  .sidebar {
    display: none !important;
  }
}
</style>
