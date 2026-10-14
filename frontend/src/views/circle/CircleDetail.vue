<template>
  <div class="circle-detail-page">
    <div class="detail-container">
      <button class="back-btn glass" @click="router.back()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>

      <div v-if="loading" class="loading-skeleton">
        <div class="skeleton-card glass">
          <div class="skeleton-header">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-info">
              <div class="skeleton-name"></div>
              <div class="skeleton-time"></div>
            </div>
          </div>
          <div class="skeleton-text"></div>
          <div class="skeleton-text short"></div>
        </div>
      </div>

      <div v-else-if="error" class="error-state glass">
        <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <h3>{{ error }}</h3>
        <button class="btn btn-primary" @click="fetchPost">重试</button>
      </div>

      <div v-else-if="post" class="detail-content">
        <div class="feed-card glass">
          <div class="feed-header">
            <router-link :to="`/user/${post.userId}`" class="feed-author">
              <img :src="post.userAvatar || '/default-avatar.png'" :alt="post.userUsername" class="author-avatar" />
              <div class="author-info">
                <span class="author-name">{{ post.userNickname || post.userUsername }}</span>
                <span class="feed-time">{{ formatRelativeTime(post.createTime) }}</span>
              </div>
            </router-link>
            <span class="visibility-badge glass-badge" :title="visibilityLabel(post.visibility)">
              <svg v-if="post.visibility === 0" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
              <svg v-else-if="post.visibility === 1" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6"/><path d="M23 11h-6"/></svg>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </span>
            <div v-if="userStore.isLoggedIn && post.userId === userStore.userId" class="post-owner-actions">
              <button class="action-icon-btn" title="编辑" @click="router.push(`/circle/post/edit/${post.id}`)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/></svg>
              </button>
              <button class="action-icon-btn" title="删除" @click="confirmDeletePost">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </div>
          </div>

          <div class="feed-content">
            <p class="feed-text">{{ post.content }}</p>

            <div v-if="post.topicNames && post.topicNames.length" class="topic-tags">
              <router-link v-for="(tn, idx) in post.topicNames" :key="tn" :to="`/topic/${post.topicIds?.[idx] || ''}`" class="topic-tag-link glass-chip">#{{ tn }}</router-link>
            </div>

            <div v-if="post.location" class="location-display">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              <span>{{ post.location }}</span>
            </div>

            <div v-if="post.images && post.images.length" class="feed-images" :class="getImageGridClass(post.images.length)">
              <div v-for="(image, idx) in post.images" :key="idx" class="img-wrap" :class="{ 'is-video': isVideo(image) }" @click="openImagePreview(post.images, Number(idx))">
                <img v-if="!isVideo(image)" :src="image" alt="" class="feed-image" loading="lazy" />
                <video v-else :src="image" class="feed-image" muted></video>
                <span v-if="isVideo(image)" class="play-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                </span>
              </div>
            </div>

            <div v-if="post.videos && post.videos.length" class="feed-videos">
              <video v-for="(video, idx) in post.videos" :key="'v-'+idx" :src="video" class="feed-video" controls preload="metadata"></video>
            </div>

            <div v-if="post.repostPost" class="repost-card glass-inner" @click="router.push(`/circle/${post.repostPost.id}`)">
              <div class="repost-header">
                <img :src="post.repostPost.userAvatar || '/default-avatar.png'" class="repost-avatar" />
                <span class="repost-author">{{ post.repostPost.userNickname || post.repostPost.userUsername }}</span>
              </div>
              <p v-if="post.repostContent" class="repost-user-content">{{ post.repostContent }}</p>
              <p class="repost-text">{{ post.repostPost.content }}</p>
              <div v-if="post.repostPost.images && post.repostPost.images.length" class="repost-images mini">
                <img v-for="(img, idx) in post.repostPost.images.slice(0, 3)" :key="idx" :src="img" class="repost-img" />
                <span v-if="post.repostPost.images.length > 3" class="repost-more">+{{ post.repostPost.images.length - 3 }}</span>
              </div>
            </div>
            <div v-else-if="post.originalPostHidden" class="repost-hidden-notice">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <span>原内容已隐藏</span>
            </div>
          </div>

          <div class="feed-stats">
            <span class="post-date">{{ formatDate(post.createTime) }}</span>
            <span v-if="post.likeCount || post.commentCount || post.repostCount || post.viewCount" class="post-stats-text">
              {{ formatNumber(post.likeCount) }} 赞 · {{ formatNumber(post.commentCount) }} 评论 · {{ formatNumber(post.repostCount) }} 转发 · {{ formatNumber(post.viewCount || 0) }} 浏览
            </span>
          </div>

          <div class="feed-actions">
            <button class="action-btn" :class="{ liked: isLiked }" @click="toggleLike">
              <svg class="like-icon" :class="{ 'animate-pop': likeAnim }" width="20" height="20" viewBox="0 0 24 24" :fill="isLiked ? 'var(--accent)' : 'none'" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              <span>{{ formatNumber(post.likeCount) }}</span>
            </button>
            <button class="action-btn active-comment">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              <span>{{ formatNumber(post.commentCount) }}</span>
            </button>
            <button class="action-btn" @click="openRepostModal">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/>
              </svg>
              <span>{{ formatNumber(post.repostCount) }}</span>
            </button>
            <button class="action-btn" @click="openReport" title="举报">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
            </button>
          </div>
        </div>

        <div class="comment-section glass">
          <h3 class="section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="display:inline;vertical-align:middle;margin-right:6px"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            评论 ({{ post.commentCount ?? comments.length }})
          </h3>

          <div v-if="userStore.isLoggedIn" class="comment-form">
            <img :src="userStore.avatar || '/default-avatar.png'" class="comment-form-avatar" />
            <div class="comment-form-body">
              <transition name="slide">
                <div v-if="replyTarget" class="reply-indicator glass-chip">
                  <span>回复 @{{ replyTarget.replyToUsername }}</span>
                  <button class="cancel-reply" @click="cancelReply">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </button>
                </div>
              </transition>
              <textarea v-model="commentContent" :placeholder="replyTarget ? `回复 @${replyTarget.replyToUsername}...` : '写下你的评论...'" rows="2" @input="autoResizeComment" maxlength="500" class="comment-textarea"></textarea>
              <div class="comment-form-bottom">
                <span class="char-count">{{ commentContent.length }}/500</span>
                <button class="btn btn-primary btn-sm" @click="submitComment" :disabled="!commentContent.trim() || submitting">
                  {{ submitting ? '发表中...' : replyTarget ? '回复' : '发表' }}
                </button>
              </div>
            </div>
          </div>
          <div v-else class="login-hint glass">
            <router-link to="/login">登录</router-link>后参与评论
          </div>

          <div v-if="commentsLoading" class="comment-skeleton">
            <div v-for="i in 3" :key="i" class="comment-skeleton-item">
              <div class="cs-avatar"></div>
              <div class="cs-body">
                <div class="cs-name"></div>
                <div class="cs-text"></div>
              </div>
            </div>
          </div>

          <div v-else-if="comments.length === 0" class="no-comments">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            <p>暂无评论，快来抢沙发吧~</p>
          </div>

          <div v-else class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item glass-inner">
              <img :src="comment.user?.avatar || '/default-avatar.png'" :alt="comment.user?.username" class="comment-avatar" />
              <div class="comment-body">
                <div class="comment-header">
                  <router-link :to="`/user/${comment.user?.id}`" class="comment-author">{{ comment.user?.nickname || comment.user?.username }}</router-link>
                  <span class="comment-time">{{ formatRelativeTime(comment.createTime) }}</span>
                </div>
                <div class="comment-content">{{ comment.content }}</div>
                <div class="comment-actions">
                  <button class="comment-action-btn" @click="startReply(comment)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="display:inline;vertical-align:middle;margin-right:4px"><polyline points="9 17 4 12 9 7"/><path d="M20 18v-2a4 4 0 0 0-4-4H4"/></svg>
                    回复
                  </button>
                  <button v-if="comment.user?.id === userStore.userId" class="comment-action-btn danger" @click="deleteComment(comment.id)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="display:inline;vertical-align:middle;margin-right:4px"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                    删除
                  </button>
                </div>

                <div v-if="comment.replies && comment.replies.length" class="nested-replies">
                  <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                    <img :src="reply.user?.avatar || '/default-avatar.png'" :alt="reply.user?.username" class="reply-avatar" />
                    <div class="reply-body">
                      <div class="comment-header">
                        <router-link :to="`/user/${reply.user?.id}`" class="comment-author">{{ reply.user?.nickname || reply.user?.username }}</router-link>
                        <span class="comment-time">{{ formatRelativeTime(reply.createTime) }}</span>
                      </div>
                      <div class="comment-content">{{ reply.content }}</div>
                      <div class="comment-actions">
                        <button class="comment-action-btn" @click="startReply(reply)">回复</button>
                        <button v-if="reply.user?.id === userStore.userId" class="comment-action-btn danger" @click="deleteComment(reply.id)">删除</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <teleport to="body">
      <transition name="modal">
        <div v-if="showRepostModal" class="modal-overlay" @click.self="closeRepostModal">
          <div class="modal-content glass">
            <div class="modal-header">
              <h3>转发动态</h3>
              <button class="close-btn" @click="closeRepostModal">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div v-if="post" class="repost-original-card glass-inner">
                <div class="repost-original-header">
                  <img :src="post.userAvatar || '/default-avatar.png'" class="repost-original-avatar" />
                  <span class="repost-original-name">{{ post.userNickname || post.userUsername }}</span>
                </div>
                <p class="repost-original-text">{{ post.content }}</p>
              </div>
              <textarea v-model="repostContent" class="post-textarea" placeholder="说说你的看法..." rows="4" maxlength="2000"></textarea>
              <div class="char-count" :class="{ warn: repostContent.length > 1800 }">{{ repostContent.length }}/2000</div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="closeRepostModal">取消</button>
              <button class="btn btn-primary" @click="confirmRepost" :disabled="reposting">
                {{ reposting ? '转发中...' : '转发' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <ImagePreview :images="previewImages" :initial-index="previewIndex" :show="showPreview" @close="showPreview = false" />
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { circleApi } from '../../api/circle'
import { useConfirm } from '../../composables/useConfirm'
import ImagePreview from '../../components/common/ImagePreview.vue'
import { useUserStore } from '../../stores/user'
import { formatRelativeTime, formatDate, formatNumber } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('CircleDetail')
const { confirm, ConfirmDialog } = useConfirm()

const post = ref<any>(null)
const comments = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const isLiked = ref(false)
const likeAnim = ref(false)
const commentContent = ref('')
const replyTarget = ref<any>(null)
const submitting = ref(false)
const commentsLoading = ref(true)
const showPreview = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)
const showRepostModal = ref(false)
const repostContent = ref('')
const reposting = ref(false)

const isVideo = (url: string) => {
  if (!url) return false
  const exts = ['.mp4', '.webm', '.mov', '.avi', '.mkv']
  const lower = url.toLowerCase()
  return exts.some(e => lower.includes(e)) || lower.includes('video')
}

const visibilityLabel = (v: number) => {
  if (v === 0) return '公开'
  if (v === 1) return '关注者可见'
  return '仅自己可见'
}

const getImageGridClass = (count: number) => {
  const n = Math.min(count, 9)
  if (n === 1) return 'grid-1'
  if (n === 2) return 'grid-2'
  if (n === 3) return 'grid-3'
  if (n === 4) return 'grid-4'
  return 'grid-multi'
}

const fetchPost = async () => {
  const postId = route.params.id as string
  loading.value = true
  error.value = ''
  try {
    const res = await circleApi.getPostById(postId)
    post.value = res.data
  } catch (err: any) {
    logger.error('fetchPost error', { error: err.message })
    error.value = err.response?.data?.message || '动态不存在或已删除'
  } finally {
    loading.value = false
  }
  if (userStore.isLoggedIn) {
    try {
      const likeRes = await circleApi.checkLikeStatus(postId)
      isLiked.value = likeRes.data?.liked ?? false
    } catch (err: any) {
      logger.warn('checkLikeStatus failed', { error: err.message })
    }
  }
}

const confirmDeletePost = async () => {
  const ok = await confirm('确定要删除这条动态吗？删除后不可恢复。')
  if (!ok) return
  try {
    await circleApi.deletePost(route.params.id as string)
    toast.success('删除成功')
    router.push('/circle')
  } catch (err: any) {
    logger.error('deletePost error', { error: err.message })
    toast.error(err.response?.data?.message || '删除失败')
  }
}

const fetchComments = async () => {
  commentsLoading.value = true
  try {
    const res = await circleApi.getComments(route.params.id as string, {})
    comments.value = Array.isArray(res.data) ? res.data : []
  } catch (err: any) {
    logger.error('fetchComments error', { error: err.message })
  } finally {
    commentsLoading.value = false
  }
}

const toggleLike = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  const prev = isLiked.value
  isLiked.value = !isLiked.value
  post.value.likeCount += isLiked.value ? 1 : -1
  likeAnim.value = true
  setTimeout(() => likeAnim.value = false, 400)
  try {
    await circleApi.toggleLike(route.params.id as string)
  } catch (err: any) {
    isLiked.value = prev
    post.value.likeCount += isLiked.value ? 1 : -1
    logger.error('toggleLike error', { error: err.message })
  }
}

const submitComment = async () => {
  if (!commentContent.value.trim()) return
  if (commentContent.value.length > 500) {
    toast.error('评论内容不能超过500个字符')
    return
  }
  submitting.value = true
  try {
    await circleApi.createComment({
      postId: Number(route.params.id),
      content: commentContent.value,
      parentId: replyTarget.value?.parentId || null,
      replyToUserId: replyTarget.value?.replyToUserId || null
    })
    commentContent.value = ''
    replyTarget.value = null
    await fetchComments()
    if (post.value) post.value.commentCount++
  } catch (err: any) {
    logger.error('submitComment error', { error: err.message })
    toast.error('评论失败')
  } finally {
    submitting.value = false
  }
}

const startReply = (comment: any) => {
  replyTarget.value = {
    parentId: comment.parentId || comment.id,
    replyToUserId: comment.user?.id,
    replyToUsername: comment.user?.nickname || comment.user?.username
  }
}

const cancelReply = () => {
  replyTarget.value = null
}

const deleteComment = async (commentId: number | string) => {
  try {
    await circleApi.deleteComment(commentId)
    toast.success('评论已删除')
    await fetchComments()
    if (post.value) post.value.commentCount = Math.max(0, post.value.commentCount - 1)
  } catch (err: any) {
    logger.error('deleteComment error', { error: err.message })
    toast.error('删除失败')
  }
}

const openRepostModal = () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  showRepostModal.value = true
  repostContent.value = ''
}

const openReport = () => {
  if (!post.value) return
  router.push(`/report/post/${post.value.id}`)
}

const closeRepostModal = () => {
  showRepostModal.value = false
  repostContent.value = ''
}

const confirmRepost = async () => {
  if (!userStore.isLoggedIn) { toast.warning('请先登录'); return }
  if (!post.value) return
  reposting.value = true
  try {
    await circleApi.repost(post.value.id, repostContent.value)
    closeRepostModal()
    post.value.repostCount = (post.value.repostCount || 0) + 1
    toast.success('转发成功')
  } catch (err: any) {
    logger.error('repost error', { error: err.message })
    toast.error('转发失败')
  } finally {
    reposting.value = false
  }
}

const openImagePreview = (images: string[], idx: number) => {
  previewImages.value = images
  previewIndex.value = idx
  showPreview.value = true
}

const autoResizeComment = (e: Event) => {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

onMounted(() => {
  fetchPost()
  fetchComments()
})

watch(() => route.params.id, () => {
  if (route.params.id) {
    fetchPost()
    fetchComments()
  }
})
</script>

<style scoped>
.circle-detail-page {
  max-width: 640px;
  margin: 0 auto;
  padding: var(--spacing-md);
  min-height: 100vh;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
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
  box-shadow: var(--glass-shadow);
  margin-bottom: var(--spacing-md);
}

.back-btn:hover {
  background: var(--glass-hover);
  color: var(--primary);
  border-color: var(--primary);
  transform: translateY(-1px);
}

.feed-card {
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
  border-radius: var(--radius-lg);
  transition: all var(--transition-slow);
}

.feed-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}

.feed-author {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  text-decoration: none;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 2px solid var(--border-solid);
  transition: all var(--transition);
}

.feed-author:hover .author-avatar {
  transform: scale(1.05);
  box-shadow: var(--shadow-md);
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--text-primary);
}

.feed-time {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 2px;
}

.visibility-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  cursor: default;
}

.visibility-badge.glass-badge {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
}

.feed-text {
  font-size: 0.9375rem;
  color: var(--text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.topic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
}

.topic-tag-link {
  display: inline-block;
  padding: 2px 10px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 500;
  text-decoration: none;
  transition: all var(--transition);
}

.topic-tag-link:hover {
  background: var(--primary);
  color: var(--text-inverse);
  transform: translateY(-1px);
}

.topic-tag-link.glass-chip {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
}

.feed-images {
  display: grid;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-md);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.grid-1 { grid-template-columns: 1fr; max-width: 400px; }
.grid-2 { grid-template-columns: 1fr 1fr; }
.grid-3 { grid-template-columns: 1fr 1fr 1fr; }
.grid-4 { grid-template-columns: 1fr 1fr; }
.grid-multi { grid-template-columns: repeat(3, 1fr); }

.img-wrap {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  cursor: pointer;
}

.feed-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.img-wrap:hover .feed-image {
  transform: scale(1.05);
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 44px;
  height: 44px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  pointer-events: none;
  transition: all var(--transition);
}

.img-wrap:hover .play-icon {
  background: var(--primary);
  transform: translate(-50%, -50%) scale(1.1);
}

.feed-videos {
  margin-top: var(--spacing-md);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.feed-video {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
  background: #000;
  border-radius: var(--radius-md);
}

.repost-card {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition);
}

.repost-card.glass-inner {
  background: var(--surface);
  border: 1px solid var(--border);
}

.repost-card:hover {
  background: var(--primary-light);
  border-color: var(--primary);
}

.repost-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.repost-avatar {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.repost-author {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.repost-text {
  font-size: 0.875rem;
  color: var(--text-primary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.repost-images.mini {
  display: flex;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
}

.repost-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.repost-more {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--skeleton-base);
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.location-display {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
  font-size: 0.75rem;
  color: var(--text-muted);
}

.location-display svg {
  color: var(--primary);
}



.repost-user-content {
  font-size: 0.875rem;
  color: var(--text-primary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: var(--spacing-xs);
  padding-bottom: var(--spacing-xs);
  border-bottom: 1px dashed var(--glass-border);
}

.repost-hidden-notice {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  margin-top: var(--spacing-md);
  background: var(--skeleton-base);
  border-radius: var(--radius-md);
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.repost-hidden-notice svg {
  opacity: 0.5;
}

.feed-stats {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-sm) 0;
  font-size: 0.75rem;
  color: var(--text-muted);
  border-bottom: 1px solid var(--glass-border);
  margin-bottom: var(--spacing-sm);
}

.feed-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all var(--transition);
  flex: 1;
  box-shadow: var(--glass-shadow);
}

.action-btn:hover {
  background: var(--glass-hover);
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-1px);
}

.action-btn.liked {
  color: var(--accent);
  background: var(--accent-light);
  border-color: var(--accent);
}

.action-btn:hover[title="举报"] {
  color: var(--error);
  border-color: var(--error);
  background: var(--error-light);
}

.action-btn.active-comment {
  color: var(--primary);
  background: var(--primary-light);
  border-color: var(--primary);
}

.like-icon.animate-pop {
  animation: likePop 0.4s var(--transition-spring);
}

@keyframes likePop {
  0% { transform: scale(1); }
  25% { transform: scale(1.4); }
  50% { transform: scale(0.85); }
  75% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.comment-section {
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-md);
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: var(--spacing-lg);
  color: var(--text-primary);
  display: flex;
  align-items: center;
}

.comment-form {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}

.comment-form-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--border-solid);
}

.comment-form-body {
  flex: 1;
}

.reply-indicator {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--primary-light);
  border: 1px solid var(--primary);
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  color: var(--primary);
  margin-bottom: var(--spacing-xs);
}

.reply-indicator.glass-chip {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  box-shadow: var(--glass-shadow);
}

.cancel-reply {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px;
  border-radius: var(--radius-xs);
  transition: all var(--transition);
}

.cancel-reply:hover {
  color: var(--error);
  background: var(--error-light);
}

.comment-textarea {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  font-size: 0.875rem;
  font-family: inherit;
  resize: none;
  outline: none;
  transition: all var(--transition);
  box-sizing: border-box;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  box-shadow: var(--glass-shadow);
}

.comment-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light), var(--glass-shadow);
  background: var(--glass-hover);
}

.comment-form-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-sm);
}

.comment-form-bottom .char-count {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.login-hint {
  text-align: center;
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-lg);
  font-size: 0.875rem;
  color: var(--text-muted);
}

.login-hint a {
  color: var(--primary);
  font-weight: 500;
  text-decoration: none;
}

.login-hint a:hover {
  text-decoration: underline;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.comment-item {
  display: flex;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  transition: all var(--transition);
}

.comment-item.glass-inner {
  background: var(--surface);
  border: 1px solid var(--border);
}

.comment-item:hover {
  transform: translateX(2px);
  box-shadow: var(--shadow-sm);
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--border-solid);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xs);
}

.comment-author {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  text-decoration: none;
}

.comment-author:hover {
  color: var(--primary);
}

.comment-time {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.comment-content {
  font-size: 0.875rem;
  color: var(--text-primary);
  line-height: 1.5;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-top: var(--spacing-xs);
}

.comment-action-btn {
  display: inline-flex;
  align-items: center;
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.75rem;
  cursor: pointer;
  padding: 2px 0;
  transition: all var(--transition);
}

.comment-action-btn:hover {
  color: var(--primary);
}

.comment-action-btn.danger:hover {
  color: var(--error);
}

.nested-replies {
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm) 0 0 var(--spacing-md);
  border-left: 2px solid var(--glass-border);
}

.reply-item {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--border-solid);
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.no-comments {
  text-align: center;
  padding: var(--spacing-xl);
  color: var(--text-muted);
  font-size: 0.875rem;
}

.no-comments svg {
  margin-bottom: var(--spacing-sm);
  opacity: 0.5;
}

.loading-skeleton {
  padding: var(--spacing-lg);
}

.skeleton-card {
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
}

.skeleton-header {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.skeleton-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info { flex: 1; }
.skeleton-name { width: 100px; height: 14px; background: var(--skeleton-base); border-radius: var(--radius-xs); margin-bottom: var(--spacing-sm); animation: shimmer 1.5s infinite; background-size: 200% 100%; }
.skeleton-time { width: 60px; height: 12px; background: var(--skeleton-base); border-radius: var(--radius-xs); animation: shimmer 1.5s infinite; background-size: 200% 100%; }
.skeleton-text { height: 14px; background: var(--skeleton-base); border-radius: var(--radius-xs); margin-bottom: var(--spacing-sm); animation: shimmer 1.5s infinite; background-size: 200% 100%; }
.skeleton-text.short { width: 60%; }

.comment-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) 0;
}

.comment-skeleton-item {
  display: flex;
  gap: var(--spacing-sm);
}

.cs-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: var(--skeleton-base);
  flex-shrink: 0;
  animation: shimmer 1.5s infinite;
  background-size: 200% 100%;
}

.cs-body { flex: 1; }
.cs-name { width: 80px; height: 12px; background: var(--skeleton-base); border-radius: var(--radius-xs); margin-bottom: var(--spacing-sm); animation: shimmer 1.5s infinite; background-size: 200% 100%; }
.cs-text { width: 100%; height: 12px; background: var(--skeleton-base); border-radius: var(--radius-xs); animation: shimmer 1.5s infinite; background-size: 200% 100%; }

.error-state {
  text-align: center;
  padding: var(--spacing-3xl) var(--spacing-lg);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-md);
}

.error-state h3 {
  margin: var(--spacing-md) 0 var(--spacing-lg);
  font-size: 1rem;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  padding: var(--spacing-lg);
}

.modal-content {
  width: 100%;
  max-width: 540px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--glass-border);
}

.modal-header h3 {
  font-size: 1rem;
  font-weight: 600;
}

.close-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition);
}

.close-btn:hover {
  background: var(--error-light);
  color: var(--error);
  border-color: var(--error);
}

.modal-body {
  padding: var(--spacing-lg);
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--glass-border);
}

.post-textarea {
  width: 100%;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  padding: var(--spacing-md);
  font-size: 0.9375rem;
  font-family: inherit;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
  transition: all var(--transition);
  min-height: 80px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  box-shadow: var(--glass-shadow);
}

.post-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light), var(--glass-shadow);
  background: var(--glass-hover);
}

.char-count {
  text-align: right;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: var(--spacing-sm);
}

.char-count.warn {
  color: var(--warning);
}

.repost-original-card {
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
}

.repost-original-card.glass-inner {
  background: var(--surface);
  border: 1px solid var(--border);
}

.repost-original-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.repost-original-avatar {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.repost-original-name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.repost-original-text {
  font-size: 0.875rem;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity var(--transition-slow) ease;
}

.modal-enter-active .modal-content,
.modal-leave-active .modal-content {
  transition: transform var(--transition-slow) ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: scale(0.95) translateY(10px);
}

.slide-enter-active,
.slide-leave-active {
  transition: all var(--transition) ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.post-owner-actions {
  display: flex;
  gap: 4px;
  margin-left: auto;
  align-items: center;
}

.action-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius);
  background: var(--glass-bg);
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition);
}

.action-icon-btn:hover {
  background: var(--primary-light);
  color: var(--primary);
}

@media (max-width: 640px) {
  .circle-detail-page {
    padding: var(--spacing-sm);
  }
  .feed-card,
  .comment-section {
    padding: var(--spacing-md);
  }
}
</style>
