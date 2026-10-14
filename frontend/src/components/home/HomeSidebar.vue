<script setup lang="ts">
import { formatNumber } from '@/utils'
import type { Post, Tag, CommunityStats } from '@/types'

defineProps<{
  hotPosts?: Post[]
  hotTags?: Tag[]
  stats?: Partial<CommunityStats>
  loading?: boolean
}>()

const getTagSize = (count: number): string => {
  if (count > 20) return '0.95rem'
  if (count > 10) return '0.875rem'
  if (count > 5) return '0.8125rem'
  return '0.75rem'
}
</script>

<template>
  <aside class="sidebar">
    <!-- Hot Posts Card -->
    <div class="sidebar-card">
      <h3 class="sidebar-title">热门文章</h3>
      <div v-if="loading" class="sidebar-skeleton">
        <div v-for="i in 5" :key="i" class="sidebar-skeleton-item">
          <div class="skeleton skeleton-rank"></div>
          <div class="skeleton skeleton-line"></div>
        </div>
      </div>
      <div v-else-if="hotPosts && hotPosts.length > 0" class="hot-posts">
        <router-link
          v-for="(item, index) in hotPosts"
          :key="item.id"
          :to="`/post/${item.id}`"
          class="hot-post-item"
        >
          <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
          <span class="hot-post-title">{{ item.title }}</span>
          <span class="hot-views">{{ formatNumber(item.viewCount || 0) }}</span>
        </router-link>
      </div>
      <div v-else class="sidebar-empty">暂无热门文章</div>
    </div>

    <!-- Hot Tags Card -->
    <div class="sidebar-card">
      <h3 class="sidebar-title">热门标签</h3>
      <div v-if="loading" class="sidebar-skeleton">
        <div class="skeleton skeleton-tag-line"></div>
      </div>
      <div v-else-if="hotTags && hotTags.length > 0" class="tag-cloud">
        <router-link
          v-for="tag in hotTags"
          :key="tag.id"
          :to="`/search?keyword=${encodeURIComponent(tag.name)}`"
          class="tag-item"
          :style="{ fontSize: getTagSize(tag.postCount || 0) }"
        >
          {{ tag.name }}
          <sup class="tag-count">{{ tag.postCount }}</sup>
        </router-link>
      </div>
      <div v-else class="sidebar-empty">暂无热门标签</div>
    </div>

    <!-- Community Stats Card -->
    <div class="sidebar-card">
      <h3 class="sidebar-title">社区统计</h3>
      <div v-if="loading" class="sidebar-skeleton">
        <div class="stats-grid-skeleton">
          <div v-for="i in 4" :key="i" class="skeleton skeleton-stat-item"></div>
        </div>
      </div>
      <div v-else class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
            </svg>
          </div>
          <div class="stat-value">{{ formatNumber(stats?.postCount || 0) }}</div>
          <div class="stat-label">文章</div>
        </div>
        <div class="stat-item">
          <div class="stat-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <div class="stat-value">{{ formatNumber(stats?.userCount || 0) }}</div>
          <div class="stat-label">用户</div>
        </div>
        <div class="stat-item">
          <div class="stat-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <div class="stat-value">{{ formatNumber(stats?.commentCount || 0) }}</div>
          <div class="stat-label">评论</div>
        </div>
        <div class="stat-item">
          <div class="stat-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
              <line x1="7" y1="7" x2="7.01" y2="7"/>
            </svg>
          </div>
          <div class="stat-value">{{ formatNumber(stats?.tagCount || 0) }}</div>
          <div class="stat-label">标签</div>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: calc(var(--navbar-height) + 20px);
}

.sidebar-card {
  background: var(--surface-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
  margin-bottom: 16px;
  transition: all var(--transition-slow);
}

.sidebar-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.10);
  transform: translateY(-1px);
}

.sidebar-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--border-light);
  position: relative;
}

.sidebar-title::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 36px;
  height: 2px;
  background: linear-gradient(90deg, var(--purple), var(--purple-muted));
  border-radius: 1px;
}

.hot-posts {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hot-post-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.hot-post-item:hover {
  background: var(--primary-light);
}

.hot-post-item:hover .hot-post-title {
  color: var(--primary);
}

.hot-rank {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-muted);
  background: var(--purple-light);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.hot-rank.top {
  background: var(--purple);
  color: white;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(105, 65, 198, 0.25);
}

.hot-post-title {
  flex: 1;
  font-size: 0.8125rem;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color var(--transition-fast);
}

.hot-views {
  font-size: 0.6875rem;
  color: var(--text-muted);
  flex-shrink: 0;
  padding: 2px 8px;
  background: var(--glass-bg);
  border-radius: var(--radius-full);
  border: 1px solid var(--glass-border);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 6px 14px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--purple-light);
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  text-decoration: none;
  transition: all var(--transition-fast);
  line-height: 1.4;
}

.tag-item:hover {
  background: var(--purple);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(105, 65, 198, 0.25);
}

.tag-count {
  font-size: 0.625rem;
  color: var(--purple-muted);
  font-weight: 400;
  opacity: 0.7;
}

.tag-item:hover .tag-count {
  color: rgba(255, 255, 255, 0.8);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 16px 8px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  transition: all var(--transition-fast);
  box-shadow: var(--glass-shadow);
}

.stat-item:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary-light);
}

.stat-icon {
  color: var(--purple);
  margin-bottom: 8px;
  opacity: 0.85;
}

.stat-value {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--purple);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--text-secondary);
  color: var(--text-muted);
}

.sidebar-empty {
  text-align: center;
  padding: 16px;
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.sidebar-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-skeleton-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}

.skeleton-rank {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.skeleton-line {
  flex: 1;
  height: 14px;
  border-radius: var(--radius);
}

.skeleton-tag-line {
  width: 100%;
  height: 32px;
  border-radius: var(--radius);
}

.stats-grid-skeleton {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.skeleton-stat-item {
  height: 72px;
  border-radius: var(--radius);
}

@media (max-width: 992px) {
  .sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  .sidebar-card {
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .sidebar {
    grid-template-columns: 1fr;
  }
}
</style>