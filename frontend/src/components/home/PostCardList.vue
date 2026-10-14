<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { publicInteractionApi } from '@/api/public-interactions'
import { formatRelativeTime, getSafeImageUrl, copyToClipboard } from '@/utils'
import { useLogger } from '@/utils/logger'
import { toast } from '@/utils/toast'
import type { Post } from '@/types'

const props = defineProps<{
  posts: Post[]
  loading?: boolean
  error?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  retry: []
  'preview-image': [images: string[], index: number]
}>()

const router = useRouter()
const logger = useLogger('PostCardList')

const likeStates = ref<Record<string, { liked: boolean; likeCount: number }>>({})
const togglingLikes = ref(new Set<string>())

watch(
  () => props.posts.map((post) => String(post.id)).join(','),
  async () => {
    const result: Record<string, { liked: boolean; likeCount: number }> = {}
    await Promise.all(props.posts.map(async (post) => {
      try {
        const response = await publicInteractionApi.getLikeStatus(Number(post.id))
        if (response.data) result[String(post.id)] = response.data
      } catch {
        result[String(post.id)] = { liked: false, likeCount: post.likeCount || 0 }
      }
    }))
    likeStates.value = { ...likeStates.value, ...result }
  },
  { immediate: true }
)

const toggleLike = async (post: Post): Promise<void> => {
  const id = String(post.id)
  if (togglingLikes.value.has(id)) return
  togglingLikes.value.add(id)

  try {
    const response = await publicInteractionApi.toggleLike(Number(post.id))
    if (response.data) likeStates.value = { ...likeStates.value, [id]: response.data }
  } catch (err) {
    const error = err as Error & { userMessage?: string }
    logger.error('Failed to toggle like', { error: error.message })
    toast.error(error.userMessage || '点赞操作失败，请重试')
  } finally {
    togglingLikes.value.delete(id)
  }
}

const sharePost = async (post: Post): Promise<void> => {
  const url = `${window.location.origin}/post/${post.id}`
  const success = await copyToClipboard(url)
  if (success) {
    toast.success('链接已复制')
  } else {
    toast.error('复制失败，请手动复制链接')
  }
}

const goToPost = (post: Post): void => {
  router.push(`/post/${post.id}`)
}

const openImagePreview = (post: Post): void => {
  if (post.coverImage) {
    const images = [getSafeImageUrl(post.coverImage)]
    emit('preview-image', images, 0)
  }
}
</script>

<template>
  <div class="post-list-container">
    <Transition name="fade" mode="out-in">
      <!-- Loading State -->
      <div v-if="loading" key="loading" class="skeleton-list" role="status" aria-busy="true" aria-label="加载中">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <div class="skeleton-card-header">
            <div class="skeleton skeleton-avatar"></div>
            <div class="skeleton skeleton-name"></div>
          </div>
          <div class="skeleton skeleton-title"></div>
          <div class="skeleton skeleton-text"></div>
          <div class="skeleton skeleton-text short"></div>
          <div class="skeleton-card-footer">
            <div class="skeleton skeleton-stat"></div>
            <div class="skeleton skeleton-stat"></div>
            <div class="skeleton skeleton-stat"></div>
          </div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" key="error" class="error-state" role="alert">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <h3>加载失败</h3>
        <p>{{ errorMessage }}</p>
        <button class="btn btn-primary" @click="$emit('retry')">重新加载</button>
      </div>

      <!-- Empty State -->
      <div v-else-if="posts.length === 0" key="empty" class="empty-state">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="12" y1="18" x2="12" y2="12"/>
          <line x1="9" y1="15" x2="15" y2="15"/>
        </svg>
        <h3 class="empty-title">还没有文章</h3>
        <p class="empty-desc">新的故事很快就会在这里出现。</p>
      </div>

      <!-- Post List -->
      <div v-else key="list" class="post-list">
        <TransitionGroup name="stagger">
          <article
            v-for="(post, index) in posts"
            :key="post.id"
            class="post-card"
            :style="{ '--i': index }"
          >
            <div class="post-card-inner">
              <div class="post-author-row">
                <div class="post-author">
                  <img :src="post.avatar || '/default-avatar.png'" :alt="post.nickname || post.username || '用户头像'" class="author-avatar" loading="lazy" />
                  <span class="author-name">{{ post.nickname || post.username }}</span>
                </div>
                <span class="post-time">{{ formatRelativeTime(post.createTime) }}</span>
              </div>
              <img
                v-if="post.coverImage"
                :src="getSafeImageUrl(post.coverImage)"
                :alt="post.title + ' 封面图'"
                class="post-cover"
                style="cursor: pointer"
                @click="openImagePreview(post)"
              />
              <h3 class="post-title">
                <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
              </h3>
              <p class="post-excerpt" v-if="post.summary">{{ post.summary }}</p>
              <div class="post-tags" v-if="post.tags?.length">
                <span
                  v-for="tag in post.tags.slice(0, 3)"
                  :key="tag.id"
                  class="tag"
                >
                  {{ tag.name }}
                </span>
              </div>
              <div class="post-actions">
                <button
                  class="action-btn like-btn"
                  :class="{ active: likeStates[String(post.id)]?.liked }"
                  @click="toggleLike(post)"
                  :disabled="togglingLikes.has(String(post.id))"
                  :aria-label="'匿名点赞，当前 ' + (likeStates[String(post.id)]?.likeCount ?? post.likeCount ?? 0) + ' 人已赞'"
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" :fill="likeStates[String(post.id)]?.liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                  </svg>
                  <span>{{ likeStates[String(post.id)]?.likeCount ?? post.likeCount ?? 0 }}</span>
                </button>
                <button class="action-btn comment-btn" @click="goToPost(post)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                  </svg>
                  <span>{{ post.commentCount || 0 }}</span>
                </button>
                <button class="action-btn share-btn" @click="sharePost(post)" aria-label="分享文章">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="18" cy="5" r="3"/>
                    <circle cx="6" cy="12" r="3"/>
                    <circle cx="18" cy="19" r="3"/>
                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
                  </svg>
                  <span>{{ post.shareCount || 0 }}</span>
                </button>
              </div>
            </div>
          </article>
        </TransitionGroup>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.post-list-container {
  width: 100%;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--glass-shadow-wet);
}

.skeleton-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.skeleton-name {
  width: 100px;
  height: 14px;
  border-radius: var(--radius);
}

.skeleton-title {
  width: 65%;
  height: 20px;
  margin-bottom: 12px;
  border-radius: var(--radius);
}

.skeleton-text {
  width: 100%;
  height: 14px;
  margin-bottom: 8px;
  border-radius: var(--radius);
}

.skeleton-text.short {
  width: 45%;
}

.skeleton-card-footer {
  display: flex;
  gap: 24px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--glass-border);
}

.skeleton-stat {
  width: 60px;
  height: 14px;
  border-radius: var(--radius);
}

.skeleton {
  background: linear-gradient(90deg,
    var(--skeleton-base) 0%,
    var(--skeleton-highlight) 50%,
    var(--skeleton-base) 100%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: var(--text-muted);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
}

.error-state svg {
  margin-bottom: 16px;
  color: var(--error);
}

.error-state h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.error-state p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: var(--text-muted);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.4;
}

.empty-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow-wet);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: card-enter 0.4s ease both;
  animation-delay: calc(var(--i, 0) * 0.08s);
  overflow: hidden;
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg), 0 0 0 1px var(--primary-light);
  background: var(--glass-hover);
}

.post-card-inner {
  padding: 20px;
}

.post-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: var(--radius);
  margin-bottom: 14px;
  transition: transform var(--transition-slow);
}

.post-card:hover .post-cover {
  transform: scale(1.02);
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.post-author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
  transition: color var(--transition-fast);
}

.post-author:hover {
  color: var(--primary);
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  border: 2px solid var(--glass-border);
  flex-shrink: 0;
  transition: transform var(--transition-fast);
}

.post-author:hover .author-avatar {
  transform: scale(1.1);
}

.author-name {
  font-size: 0.875rem;
  font-weight: 500;
}

.post-time {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.post-title {
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 10px;
  line-height: 1.4;
}

.post-title a {
  color: var(--text-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.post-title a:hover {
  color: var(--primary);
}

.post-excerpt {
  font-size: 0.875rem;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.tag {
  padding: 4px 12px;
  font-size: 0.75rem;
  font-weight: 500;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: var(--radius-full);
  text-decoration: none;
  transition: all var(--transition-fast);
  border: 1px solid transparent;
}

.tag:hover {
  background: var(--primary);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px var(--primary-glow);
}

.post-actions {
  display: flex;
  gap: 4px;
  padding-top: 14px;
  border-top: 1px solid var(--glass-border);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-muted);
  background: transparent;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  transform: translateY(-1px);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.action-btn.active {
  color: var(--primary);
  background: var(--primary-light);
}

.like-btn.active {
  color: var(--accent);
  background: var(--accent-light);
}

.like-btn.active svg {
  animation: heart-pop 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.collect-btn.active {
  color: var(--warning);
  background: var(--warning-light);
}

.collect-btn.active svg {
  animation: collect-pop 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes heart-pop {
  0% { transform: scale(1); }
  30% { transform: scale(1.35); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

@keyframes collect-pop {
  0% { transform: scale(1) rotate(0deg); }
  30% { transform: scale(1.3) rotate(-5deg); }
  60% { transform: scale(0.9) rotate(3deg); }
  100% { transform: scale(1) rotate(0deg); }
}

.stagger-enter-active {
  transition: all 0.4s ease;
  transition-delay: calc(var(--i, 0) * 0.08s);
}

.stagger-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .post-card-inner {
    padding: 16px;
  }
  .post-actions {
    flex-wrap: wrap;
  }
  .post-cover {
    height: 140px;
  }
}
</style>
