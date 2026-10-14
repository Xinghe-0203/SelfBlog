<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { formatRelativeTime, formatNumber } from '../../utils'

const props = defineProps<{
  post: any
  animationDelay?: number
}>()

const emit = defineEmits<{
  like: [post: any]
  delete: [post: any]
  repost: [post: any]
  preview: [images: string[], index: number]
}>()

const router = useRouter()
const userStore = useUserStore()

const isVideo = (url: string) => {
  if (!url) return false
  const exts = ['.mp4', '.webm', '.mov', '.avi', '.mkv']
  const lower = url.toLowerCase()
  return exts.some(e => lower.includes(e)) || lower.includes('video')
}

const getImageGridClass = (count: number) => {
  const n = Math.min(count, 9)
  if (n === 1) return 'grid-1'
  if (n === 2) return 'grid-2'
  if (n === 3) return 'grid-3'
  if (n === 4) return 'grid-4'
  return 'grid-multi'
}

const playVideo = (url: string) => {
  window.open(url, '_blank')
}

const handleLike = () => emit('like', props.post)
const handleDelete = () => emit('delete', props.post)
const handleRepost = () => emit('repost', props.post)
const handlePreview = (images: string[], index: number) => emit('preview', images, index)
</script>

<template>
  <article class="feed-card" :style="{ animationDelay: `${(animationDelay || 0) * 60}ms` }">
    <!-- Header -->
    <header class="feed-card__header">
      <router-link :to="`/user/${post.userId}`" class="feed-card__author">
        <img :src="post.userAvatar || '/default-avatar.png'" :alt="post.userUsername" class="feed-card__avatar" />
        <div class="feed-card__meta">
          <span class="feed-card__name">{{ post.userNickname || post.userUsername }}</span>
          <time class="feed-card__time">{{ formatRelativeTime(post.createTime) }}</time>
        </div>
      </router-link>
      <div class="feed-card__header-actions">
        <span v-if="post.visibility !== 0" class="feed-card__vis-dot" :title="post.visibility === 1 ? '关注者可见' : '仅自己可见'"></span>
        <div v-if="userStore.isLoggedIn && post.userId === userStore.userId" class="feed-card__owner-actions">
          <button class="feed-card__icon-btn" title="编辑" @click.stop="router.push(`/circle/post/edit/${post.id}`)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/></svg>
          </button>
          <button class="feed-card__icon-btn feed-card__icon-btn--danger" title="删除" @click.stop="handleDelete">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
      </div>
    </header>

    <!-- Content -->
    <div class="feed-card__content" @click="router.push(`/circle/${post.id}`)">
      <p class="feed-card__text">{{ post.content }}</p>

      <div v-if="post.topicNames && post.topicNames.length" class="feed-card__tags">
        <router-link v-for="(tn, idx) in post.topicNames" :key="tn" :to="`/topic/${post.topicIds?.[idx] || ''}`" class="feed-card__tag">#{{ tn }}</router-link>
      </div>

      <div v-if="post.location" class="feed-card__location">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
        <span>{{ post.location }}</span>
      </div>

      <!-- Images -->
      <div v-if="post.images && post.images.length" class="feed-card__images" :class="getImageGridClass(post.images.length)">
        <div v-for="(image, idx) in post.images" :key="idx" class="feed-card__img-wrap" :class="{ 'is-video': isVideo(image) }" @click.stop="handlePreview(post.images, Number(idx))">
          <img v-if="!isVideo(image)" :src="image" alt="动态图片" class="feed-card__image" loading="lazy" />
          <video v-else :src="image" class="feed-card__image" muted @click.stop.prevent="playVideo(image)"></video>
          <span v-if="isVideo(image)" class="feed-card__play-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"/></svg>
          </span>
        </div>
      </div>

      <div v-if="post.videos && post.videos.length" class="feed-card__videos">
        <video v-for="(video, idx) in post.videos" :key="'v-'+idx" :src="video" class="feed-card__video" controls muted preload="metadata"></video>
      </div>

      <!-- Repost -->
      <div v-if="post.repostPost" class="feed-card__repost" @click.stop="router.push(`/circle/${post.id}`)">
        <div class="feed-card__repost-header">
          <img :src="post.repostPost.userAvatar || '/default-avatar.png'" class="feed-card__repost-avatar" />
          <span class="feed-card__repost-name">{{ post.repostPost.userNickname || post.repostPost.userUsername }}</span>
        </div>
        <p v-if="post.repostContent" class="feed-card__repost-user-text">{{ post.repostContent }}</p>
        <p class="feed-card__repost-text">{{ post.repostPost.content }}</p>
        <div v-if="post.repostPost.images && post.repostPost.images.length" class="feed-card__repost-images">
          <img v-for="(img, idx) in post.repostPost.images.slice(0, 3)" :key="idx" :src="img" class="feed-card__repost-img" />
          <span v-if="post.repostPost.images.length > 3" class="feed-card__repost-more">+{{ post.repostPost.images.length - 3 }}</span>
        </div>
      </div>
      <div v-else-if="post.originalPostHidden" class="feed-card__hidden-notice">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
        <span>原内容已隐藏</span>
      </div>
    </div>

    <!-- Actions -->
    <footer class="feed-card__actions">
      <button class="feed-card__action" :class="{ 'is-liked': post.isLiked }" @click="handleLike">
        <svg class="feed-card__like-icon" :class="{ 'is-animating': post.likeAnim }" width="17" height="17" viewBox="0 0 24 24" :fill="post.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
        </svg>
        <span class="feed-card__action-count">{{ formatNumber(post.likeCount) }}</span>
      </button>
      <button class="feed-card__action" @click="router.push(`/circle/${post.id}`)">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <span class="feed-card__action-count">{{ formatNumber(post.commentCount) }}</span>
      </button>
      <button class="feed-card__action" @click="handleRepost">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/>
        </svg>
        <span class="feed-card__action-count">{{ formatNumber(post.repostCount) }}</span>
      </button>
      <span class="feed-card__views">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
        </svg>
        {{ formatNumber(post.viewCount || 0) }}
      </span>
    </footer>
  </article>
</template>

<style scoped>
.feed-card {
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-3);
  transition: border-color var(--transition), box-shadow var(--transition);
  animation: cardEnter 0.45s var(--ease-out) both;
}

.feed-card:hover {
  border-color: var(--gray-200);
  box-shadow: var(--shadow-sm);
}

@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Header */
.feed-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}

.feed-card__author {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  text-decoration: none;
}

.feed-card__avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  object-fit: cover;
  transition: transform var(--transition);
}

.feed-card__author:hover .feed-card__avatar {
  transform: scale(1.06);
}

.feed-card__meta {
  display: flex;
  flex-direction: column;
}

.feed-card__name {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.feed-card__time {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 1px;
}

.feed-card__header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.feed-card__vis-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--gray-300);
}

.feed-card__owner-actions {
  display: flex;
  gap: 2px;
  margin-left: var(--spacing-xs);
}

.feed-card__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition);
}

.feed-card__icon-btn:hover {
  background: var(--gray-100);
  color: var(--text-secondary);
}

.feed-card__icon-btn--danger:hover {
  background: var(--error-light);
  color: var(--error);
}

/* Content */
.feed-card__content {
  cursor: pointer;
}

.feed-card__text {
  font-size: 0.9375rem;
  color: var(--text-primary);
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.feed-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: var(--spacing-sm);
}

.feed-card__tag {
  display: inline-block;
  padding: 2px 10px;
  background: var(--primary-subtle);
  color: var(--primary);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 500;
  text-decoration: none;
  transition: all var(--transition);
}

.feed-card__tag:hover {
  background: var(--primary-light);
}

.feed-card__location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--spacing-sm);
  font-size: 0.75rem;
  color: var(--text-muted);
}

.feed-card__location svg {
  color: var(--primary);
  opacity: 0.7;
}

/* Images */
.feed-card__images {
  display: grid;
  gap: var(--spacing-1);
  margin-top: var(--spacing-md);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.grid-1 { grid-template-columns: 1fr; max-width: 400px; }
.grid-2 { grid-template-columns: 1fr 1fr; }
.grid-3 { grid-template-columns: 1fr 1fr 1fr; }
.grid-4 { grid-template-columns: 1fr 1fr; }
.grid-multi { grid-template-columns: repeat(3, 1fr); }

.feed-card__img-wrap {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  cursor: pointer;
}

.feed-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s var(--ease-out);
}

.feed-card__img-wrap:hover .feed-card__image {
  transform: scale(1.04);
}

.feed-card__play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 44px;
  height: 44px;
  background: rgba(0, 0, 0, 0.55);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  pointer-events: none;
  transition: all var(--transition);
}

.feed-card__img-wrap:hover .feed-card__play-icon {
  background: var(--primary);
  transform: translate(-50%, -50%) scale(1.08);
}

.feed-card__videos {
  margin-top: var(--spacing-md);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.feed-card__video {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
  background: #000;
  border-radius: var(--radius-md);
}

/* Repost */
.feed-card__repost {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--gray-50);
  transition: all var(--transition);
  cursor: pointer;
}

.feed-card__repost:hover {
  border-color: var(--primary);
  background: var(--primary-subtle);
}

.feed-card__repost-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.feed-card__repost-avatar {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.feed-card__repost-name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.feed-card__repost-user-text {
  font-size: 0.875rem;
  color: var(--text-primary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: var(--spacing-xs);
  padding-bottom: var(--spacing-xs);
  border-bottom: 1px dashed var(--border);
}

.feed-card__repost-text {
  font-size: 0.875rem;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.feed-card__repost-images {
  display: flex;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
}

.feed-card__repost-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.feed-card__repost-more {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.feed-card__hidden-notice {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  margin-top: var(--spacing-md);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.feed-card__hidden-notice svg {
  opacity: 0.5;
}

/* Actions */
.feed-card__actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding-top: var(--spacing-md);
  margin-top: var(--spacing-md);
  border-top: 1px solid var(--border);
}

.feed-card__action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: transparent;
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-muted);
  cursor: pointer;
  font-size: 0.8125rem;
  font-weight: 500;
  transition: all var(--transition);
}

.feed-card__action:hover {
  background: var(--gray-100);
  color: var(--text-secondary);
}

.feed-card__action.is-liked {
  color: var(--accent);
}

.feed-card__action.is-liked:hover {
  background: var(--accent-light);
}

.feed-card__like-icon.is-animating {
  animation: likePop 0.4s var(--ease-spring);
}

@keyframes likePop {
  0% { transform: scale(1); }
  25% { transform: scale(1.35); }
  50% { transform: scale(0.85); }
  75% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.feed-card__action-count {
  font-variant-numeric: tabular-nums;
}

.feed-card__views {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  color: var(--text-muted);
  font-size: 0.75rem;
}

.feed-card__views svg {
  opacity: 0.5;
}

@media (max-width: 640px) {
  .feed-card {
    padding: var(--spacing-md);
    border-radius: var(--radius-md);
  }
}
</style>
