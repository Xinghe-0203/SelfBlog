<template>
  <div class="trending-page">
    <div class="trending-container">
      <div class="page-header glass">
        <div class="header-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
          </svg>
        </div>
        <div>
          <h1>热搜榜单</h1>
          <p class="header-subtitle">发现校园中最热门的内容</p>
        </div>
      </div>

      <div class="trending-content">
        <div v-if="contentLoading" class="loading-section">
          <div v-for="i in 8" :key="i" class="trending-skeleton glass">
            <div class="skeleton-rank"></div>
            <div class="skeleton-info">
              <div class="skeleton-title"></div>
              <div class="skeleton-meta"></div>
            </div>
          </div>
        </div>

        <div v-else-if="error" class="error-state glass">
          <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <h3>{{ error }}</h3>
          <button class="btn btn-primary" @click="retryLoad">重新加载</button>
        </div>

        <div v-else-if="hotContent.length === 0" class="empty-state glass">
          <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
          </svg>
          <p class="empty-title">暂无热门内容</p>
          <p class="empty-text">精彩内容正在路上，请稍后再来</p>
        </div>

        <div v-if="hotContent.length > 0" class="rank-list">
          <transition-group name="rank">
            <div
              v-for="(item, index) in hotContent"
              :key="item.id"
              class="rank-item glass"
              :class="{ 'top-three': index < 3 }"
              @click="handleContentClick(item)"
            >
              <div class="rank-number" :class="getRankClass(index)">
                <template v-if="index === 0">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="#F59E0B" stroke="none"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                </template>
                <template v-else-if="index === 1">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="#94A3B8" stroke="none"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                </template>
                <template v-else-if="index === 2">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="#CD7F32" stroke="none"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                </template>
                <span v-else class="num">{{ index + 1 }}</span>
              </div>
              <div class="rank-content">
                <div class="rank-title">
                  <span class="type-badge" :class="item.type === 0 ? 'type-article' : 'type-post'">
                    {{ item.type === 0 ? '文章' : '动态' }}
                  </span>
                  <span v-if="item.type === 0" class="title-text">{{ item.title }}</span>
                  <span v-else class="content-text">{{ truncateContent(item.content) }}</span>
                  <span class="rank-badge hot" v-if="index < 3">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                    热
                  </span>
                  <span class="rank-badge new" v-else-if="index < 5">新</span>
                </div>
                <div class="rank-user">
                  <img
                    :src="item.avatar || '/default-avatar.png'"
                    class="user-avatar"
                    @error="e => (e.target as HTMLImageElement).src = '/default-avatar.png'"
                  />
                  <span class="user-nickname">{{ item.nickname || item.username }}</span>
                </div>
                <div v-if="item.type === 1 && item.topics && item.topics.length > 0" class="rank-topics">
                  <span v-for="topic in item.topics" :key="topic" class="topic-tag">#{{ topic }}</span>
                </div>
                <div v-if="item.type === 1 && item.images && item.images.length > 0" class="rank-images">
                  <div class="image-row">
                    <img
                      v-for="(img, i) in item.images.slice(0, 4)"
                      :key="i"
                      :src="img"
                      class="thumb-img"
                      @error="e => (e.target as HTMLImageElement).style.display = 'none'"
                    />
                    <span v-if="item.images.length > 4" class="more-images">+{{ item.images.length - 4 }}</span>
                  </div>
                </div>
                <div class="rank-meta">
                  <span class="rank-score">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                    {{ formatScore(item.score) }}
                  </span>
                  <span v-if="item.type === 0" class="rank-views">{{ item.viewCount || 0 }} 阅读</span>
                  <span class="rank-likes">{{ item.likeCount || 0 }} 赞</span>
                  <span class="rank-comments">{{ item.commentCount || 0 }} 评论</span>
                  <span v-if="item.type === 1" class="rank-shares">{{ item.shareCount || 0 }} 分享</span>
                </div>
              </div>
            </div>
          </transition-group>
          <div v-if="hasMoreContent" class="load-more">
            <button class="btn btn-secondary" @click="loadMoreContent" :disabled="loadingMore">
              <svg v-if="loadingMore" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { trendingApi } from '../../api/trending'
import { useLogger } from '../../utils/logger'

const router = useRouter()
const logger = useLogger('Trending')

const contentLoading = ref(true)
const loadingMore = ref(false)
const error = ref('')

const hotContent = ref<any[]>([])
const currentContentPage = ref(1)
const hasMoreContent = ref(true)
const PAGE_SIZE = 20

const getRankClass = (index: number) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

const formatScore = (score: number) => {
  if (!score) return '0'
  if (score >= 10000) return (score / 10000).toFixed(1) + 'w'
  if (score >= 1000) return (score / 1000).toFixed(1) + 'k'
  return Math.round(score).toString()
}

const truncateContent = (text: string, maxLength = 80) => {
  if (!text) return ''
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}

const handleContentClick = (item: any) => {
  if (item.type === 0) {
    router.push(`/post/${item.id}`)
  } else if (item.type === 1) {
    router.push(`/circle/${item.id}`)
  }
}

const fetchHotContent = async (reset = false) => {
  if (reset) {
    currentContentPage.value = 1
    hasMoreContent.value = true
  }
  try {
    const res = await trendingApi.getHotContent({ pageNum: currentContentPage.value, pageSize: PAGE_SIZE })
    const data = res.data
    const records = data?.records || []
    hotContent.value = reset ? records : [...hotContent.value, ...records]
    hasMoreContent.value = (data?.pages || 0) > currentContentPage.value
  } catch (err: any) {
    logger.error('fetchHotContent error', { error: err.message })
    throw err
  }
}

const loadMoreContent = async () => {
  if (loadingMore.value || !hasMoreContent.value) return
  loadingMore.value = true
  currentContentPage.value++
  try {
    await fetchHotContent()
  } catch (err: any) {
    currentContentPage.value--
  } finally {
    loadingMore.value = false
  }
}

const retryLoad = () => {
  error.value = ''
  contentLoading.value = true
  initLoad()
}

const initLoad = async () => {
  contentLoading.value = true
  error.value = ''
  try {
    await fetchHotContent(true)
  } catch (err) {
    error.value = '加载失败'
  }
  contentLoading.value = false
}

onMounted(() => {
  initLoad()
})
</script>

<style scoped>
.trending-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

.trending-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  animation: fadeUp 0.4s ease both;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-radius: var(--radius-lg);
  transition: all var(--transition);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  box-shadow: var(--glass-shadow-wet);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  pointer-events: none;
}

.header-icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, var(--warning-light), rgba(245, 158, 11, 0.15));
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--warning);
}

.page-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  line-height: 1.3;
}

.header-subtitle {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin: 2px 0 0;
}

.loading-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.trending-skeleton {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-lg);
}

.skeleton-rank {
  width: 32px;
  height: 32px;
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.skeleton-info {
  flex: 1;
}

.skeleton-title {
  height: 16px;
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 8px;
  width: 70%;
}

.skeleton-meta {
  height: 12px;
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  width: 40%;
}

.error-state {
  text-align: center;
  padding: var(--spacing-3xl) var(--spacing-xl);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  color: var(--text-secondary);
}

.error-state h3 {
  margin: 0;
  font-size: 1rem;
}

.empty-state {
  text-align: center;
  padding: var(--spacing-3xl) var(--spacing-xl);
  border-radius: var(--radius-lg);
  color: var(--text-muted);
}

.empty-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: var(--spacing-md);
  margin-bottom: var(--spacing-xs);
}

.empty-text {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.rank-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition);
}

.rank-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.rank-item.top-three {
  background: linear-gradient(135deg, var(--glass-hover), rgba(245, 158, 11, 0.03));
}

.rank-number {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  font-size: 13px;
  border-radius: var(--radius);
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  color: var(--text-muted);
  transition: all var(--transition);
}

.rank-gold {
  background: linear-gradient(135deg, #FEF3C7, #FDE68A);
  border-color: rgba(245, 158, 11, 0.3);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
}

.rank-silver {
  background: linear-gradient(135deg, #F1F5F9, #E2E8F0);
  border-color: rgba(148, 163, 184, 0.3);
  box-shadow: 0 2px 8px rgba(148, 163, 184, 0.2);
}

.rank-bronze {
  background: linear-gradient(135deg, #FED7AA, #FDBA74);
  border-color: rgba(249, 115, 22, 0.3);
  box-shadow: 0 2px 8px rgba(249, 115, 22, 0.2);
}

.rank-content {
  flex: 1;
  min-width: 0;
}

.rank-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 6px;
  word-break: break-word;
  flex-wrap: wrap;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  color: #fff;
  flex-shrink: 0;
  line-height: 1.4;
}

.rank-badge.hot {
  background: linear-gradient(135deg, var(--warning), #DC2626);
  box-shadow: 0 2px 4px rgba(245, 158, 11, 0.3);
}

.rank-badge.new {
  background: linear-gradient(135deg, var(--info), #2563EB);
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
}

.rank-user {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}

.user-avatar {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
}

.user-nickname {
  font-size: 12px;
  color: var(--text-muted);
}

.rank-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.topic-tag {
  font-size: 11px;
  color: var(--primary);
  background: var(--primary-light);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}

.rank-images {
  margin-top: 6px;
}

.image-row {
  display: flex;
  gap: 4px;
  align-items: center;
}

.thumb-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border);
  transition: all var(--transition);
}

.thumb-img:hover {
  transform: scale(1.05);
}

.more-images {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  flex-wrap: wrap;
}

.rank-score {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--warning);
  font-weight: 600;
}

.rank-views,
.rank-likes,
.rank-comments,
.rank-shares {
  color: var(--text-muted);
}

.load-more {
  text-align: center;
  padding: var(--spacing-lg) 0;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.type-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  flex-shrink: 0;
  line-height: 1.5;
}

.type-article {
  background: var(--blue-light);
  color: var(--blue);
}

.type-post {
  background: var(--green-light);
  color: var(--green);
}

.title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
  font-weight: 400;
  font-size: 14px;
}

.rank-enter-active {
  transition: all var(--transition-slow);
}

.rank-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

@media (max-width: 600px) {
  .trending-page { padding: var(--spacing-md); }
  .page-header { padding: var(--spacing-md); }
  .page-header h1 { font-size: 1.25rem; }
  .header-icon { width: 44px; height: 44px; }
  .rank-item { padding: var(--spacing-md); }
  .rank-title { font-size: 14px; }
}
</style>
