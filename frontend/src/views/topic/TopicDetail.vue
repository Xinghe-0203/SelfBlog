<template>
  <div class="topic-detail-page">
    <button class="back-btn" @click="router.back()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      返回
    </button>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="fetchTopicDetail">重试</button>
    </div>

    <template v-else-if="topic">
      <div class="topic-header glass">
        <div class="topic-icon-large">#</div>
        <div class="topic-info">
          <h1 class="topic-name">{{ topic.name }}</h1>
          <p v-if="topic.description" class="topic-desc">{{ topic.description }}</p>
          <div class="topic-stats">
            <span>{{ topic.postCount || 0 }} 动态</span>
            <span v-if="topic.trendingScore">热度 {{ topic.trendingScore }}</span>
          </div>
        </div>
      </div>

      <div class="topic-posts">
        <div v-if="postsLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载动态中...</p>
        </div>
        <div v-else-if="posts.length === 0" class="empty-state">
          <p>暂无相关动态</p>
        </div>
        <div v-else class="posts-list">
          <div v-for="item in posts" :key="item.id" class="post-item glass">
            <div class="post-header">
              <img v-if="item.userAvatar" :src="item.userAvatar" class="user-avatar" @error="onAvatarError" />
              <div class="user-info">
                <router-link :to="`/user/${item.userId}`" class="user-name">
                  {{ item.userNickname || item.userUsername }}
                </router-link>
                <span class="post-time">{{ item.timeAgo || formatRelativeTime(item.createTime) }}</span>
              </div>
            </div>
            <div class="post-content">
              <p>{{ truncateText(item.content, 300) }}</p>
            </div>
            <div v-if="item.topicNames && item.topicNames.length > 0" class="post-topics">
              <router-link v-for="(t, idx) in item.topicNames" :key="t" :to="`/topic/${item.topicIds?.[idx] || ''}`" class="topic-tag">#{{ t }}</router-link>
            </div>
            <div v-if="item.images && item.images.length > 0" class="post-images">
              <img v-for="(img, idx) in item.images.slice(0, 4)" :key="idx" :src="img" class="post-image" @click="previewImage(img)" />
            </div>
            <div class="post-actions">
              <span>❤️ {{ item.likeCount || 0 }}</span>
              <span>💬 {{ item.commentCount || 0 }}</span>
            </div>
          </div>
        </div>

        <div v-if="hasMore" class="load-more">
          <button class="btn btn-secondary" @click="loadMore" :disabled="postsLoading">
            {{ postsLoading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>
    </template>
    <ImagePreview
      :images="previewImages"
      :initial-index="0"
      @close="previewImages = []"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { topicApi } from '../../api/topic'
import { formatRelativeTime, truncateText } from '../../utils'
import { useLogger } from '../../utils/logger'
import ImagePreview from '@/components/common/ImagePreview.vue'

const route = useRoute()
const router = useRouter()
const logger = useLogger('TopicDetail')

const topic = ref<any>(null)
const loading = ref(false)
const error = ref('')
const posts = ref<any[]>([])
const postsLoading = ref(false)
const currentPage = ref(1)
const hasMore = ref(false)
const pageSize = 20
const previewImages = ref<string[]>([])

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48"><rect width="48" height="48" rx="24" fill="#e0e0e0"/><text x="24" y="30" text-anchor="middle" fill="#999" font-size="20" font-family="sans-serif">?</text></svg>')

const onAvatarError = (e: Event) => {
  const target = e.target as HTMLImageElement
  if (target.src !== defaultAvatar) {
    target.src = defaultAvatar
  }
}

const fetchTopicDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    const topicId = String(route.params.id)
    const [topicRes] = await Promise.all([
      topicApi.getTopicById(topicId)
    ])
    topic.value = topicRes.data
  } catch (err: any) {
    logger.error('Failed to fetch topic', { error: err.message })
    error.value = '话题不存在或已被删除'
  } finally {
    loading.value = false
  }
}

const fetchTopicPosts = async () => {
  postsLoading.value = true
  try {
    const topicId = String(route.params.id)
    const res = await topicApi.getTopicPosts(topicId, { pageNum: currentPage.value, pageSize })
    const data = (res.data as any) || []
    if (currentPage.value === 1) {
      posts.value = data
    } else {
      posts.value.push(...data)
    }
    hasMore.value = data.length >= pageSize
  } catch (err: any) {
    logger.error('Failed to fetch posts', { error: err.message })
  } finally {
    postsLoading.value = false
  }
}

const loadMore = () => {
  currentPage.value++
  fetchTopicPosts()
}

const previewImage = (src: string) => {
  previewImages.value = [src]
}

onMounted(() => {
  fetchTopicDetail()
  fetchTopicPosts()
})
</script>

<style scoped>
.topic-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  color: var(--text-secondary);
  cursor: pointer;
  margin-bottom: var(--spacing-md);
  transition: all var(--transition);
}

.back-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.loading-state, .error-state {
  text-align: center;
  padding: var(--spacing-3xl);
}

.error-state {
  color: var(--error);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--glass-border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto var(--spacing-md);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.topic-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-xl);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-lg);
}

.topic-icon-large {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  color: white;
  border-radius: var(--radius);
  font-size: 2rem;
  font-weight: 700;
  box-shadow: var(--shadow-glow-primary);
}

.topic-info {
  flex: 1;
}

.topic-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.topic-desc {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-sm);
}

.topic-stats {
  display: flex;
  gap: var(--spacing-md);
  font-size: 0.85rem;
  color: var(--text-muted);
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.post-item {
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
}

.post-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 600;
  color: var(--text-primary);
}

.user-name:hover {
  color: var(--primary);
}

.post-time {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.post-content {
  font-size: 0.95rem;
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: var(--spacing-sm);
}

.post-topics {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.topic-tag {
  font-size: 0.8rem;
  color: var(--primary);
  background: var(--primary-light);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  text-decoration: none;
  display: inline-block;
}
.topic-tag:hover {
  background: var(--primary);
  color: white;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.post-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: var(--radius);
  cursor: pointer;
}

.post-image:hover {
  opacity: 0.8;
}

.post-actions {
  display: flex;
  gap: var(--spacing-md);
  font-size: 0.85rem;
  color: var(--text-muted);
}

.load-more {
  text-align: center;
  padding: var(--spacing-lg);
}

.empty-state {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .topic-detail-page { padding: var(--spacing-md); }
  .topic-header { flex-direction: column; text-align: center; }
}
</style>