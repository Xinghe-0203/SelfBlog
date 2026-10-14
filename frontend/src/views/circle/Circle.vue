<template>
  <div class="circle-page">
    <!-- Subtle background texture -->
    <div class="circle-bg-texture"></div>

    <div class="circle-layout">
      <!-- Left Sidebar -->
      <CircleSidebar />

      <!-- Main Content -->
      <main class="circle-main">
        <div class="circle-container">
          <!-- Back Button -->
          <button class="back-btn" @click="router.back()">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
            返回
          </button>

          <!-- Feed Tabs -->
          <div class="feed-tabs">
            <button
              class="feed-tab"
              :class="{ 'is-active': activeTab === 'recommend' }"
              @click="switchTab('recommend')"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
              推荐
            </button>
            <button
              v-if="userStore.isLoggedIn"
              class="feed-tab"
              :class="{ 'is-active': activeTab === 'following' }"
              @click="switchTab('following')"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
              关注
            </button>
          </div>

          <!-- Quick Composer (Twitter/X style) -->
          <QuickComposer
            v-if="userStore.isLoggedIn"
            @published="fetchPosts(true)"
          />

          <!-- Feed List -->
          <div class="feed-list" ref="feedListRef">
            <!-- Skeleton Loading -->
            <div v-if="loading && posts.length === 0" class="loading-container">
              <div v-for="i in 3" :key="i" class="feed-skeleton">
                <div class="skeleton-header">
                  <div class="skeleton-avatar"></div>
                  <div class="skeleton-info">
                    <div class="skeleton-name"></div>
                    <div class="skeleton-time"></div>
                  </div>
                </div>
                <div class="skeleton-content">
                  <div class="skeleton-text"></div>
                  <div class="skeleton-text skeleton-text--short"></div>
                  <div class="skeleton-images">
                    <div class="skeleton-img"></div>
                    <div class="skeleton-img"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Error State -->
            <div v-else-if="error" class="feed-state">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <h3>加载失败</h3>
              <p>{{ error }}</p>
              <button class="btn btn-primary btn-sm" @click="retryLoad">重新加载</button>
            </div>

            <!-- Empty State -->
            <div v-else-if="posts.length === 0" class="feed-state">
              <div class="feed-state__icon">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="0.8">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </div>
              <h3 class="feed-state__title">{{ activeTab === 'following' ? '还没有关注的内容' : '还没有动态' }}</h3>
              <p class="feed-state__text">{{ activeTab === 'following' ? '去发现更多有趣的人吧' : '快来说点什么吧' }}</p>
            </div>

            <!-- Posts -->
            <div v-else class="posts">
              <CirclePostCard
                v-for="(post, index) in posts"
                :key="post.id"
                :post="post"
                :animation-delay="index"
                @like="toggleLike"
                @delete="deletePost"
                @repost="openRepostModal"
                @preview="openImagePreview"
              />

              <div ref="sentinelRef" class="scroll-sentinel">
                <div v-if="loadingMore" class="loading-spinner-wrap">
                  <div class="spinner"></div>
                  <span>加载更多...</span>
                </div>
                <div v-else-if="!hasMore" class="no-more">
                  <span class="no-more-line"></span>
                  <span class="no-more-text">没有更多了</span>
                  <span class="no-more-line"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- Floating Create Button -->
    <button
      v-if="userStore.isLoggedIn"
      class="fab-create"
      @click="openCreateModal"
      title="发布动态"
      aria-label="发布动态"
    >
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
    </button>

    <!-- Create Post Modal -->
    <CreatePostModal
      :show="showCreateModal"
      @close="showCreateModal = false"
      @published="onPostPublished"
    />

    <!-- Repost Modal -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showRepostModal" class="modal-overlay" @click.self="closeRepostModal">
          <div class="modal-box">
            <div class="modal-box__header">
              <h3>转发动态</h3>
              <button class="modal-box__close" @click="closeRepostModal">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-box__body">
              <div v-if="currentRepostPost" class="repost-preview">
                <div class="repost-preview__header">
                  <img :src="currentRepostPost.userAvatar || '/default-avatar.png'" class="repost-preview__avatar" />
                  <span class="repost-preview__name">{{ currentRepostPost.userNickname || currentRepostPost.userUsername }}</span>
                </div>
                <p class="repost-preview__text">{{ currentRepostPost.content }}</p>
              </div>
              <textarea
                v-model="repostContent"
                class="modal-textarea"
                placeholder="说说你的看法..."
                rows="4"
                maxlength="2000"
              ></textarea>
              <div class="char-count" :class="{ 'is-warn': repostContent.length > 1800 }">{{ repostContent.length }}/2000</div>
            </div>
            <div class="modal-box__footer">
              <button class="btn btn-ghost" @click="closeRepostModal">取消</button>
              <button class="btn btn-primary" @click="confirmRepost" :disabled="reposting">
                {{ reposting ? '转发中...' : '转发' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- Image Preview -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showPreview" class="modal-overlay preview-overlay" @click.self="showPreview = false">
          <button class="preview-nav preview-nav--prev" @click="previewPrev" v-if="previewImages.length > 1">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          </button>
          <img :src="previewImages[previewIndex]" class="preview-image" @click.stop />
          <button class="preview-nav preview-nav--next" @click="previewNext" v-if="previewImages.length > 1">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
          <button class="preview-close" @click="showPreview = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
          <div class="preview-counter" v-if="previewImages.length > 1">{{ previewIndex + 1 }} / {{ previewImages.length }}</div>
        </div>
      </transition>
    </teleport>
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { circleApi } from '../../api/circle'
import { useConfirm } from '../../composables/useConfirm'
import { useUserStore } from '../../stores/user'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import CircleSidebar from '../../components/circle/CircleSidebar.vue'
import CirclePostCard from '../../components/circle/CirclePostCard.vue'
import CreatePostModal from '../../components/circle/CreatePostModal.vue'
import QuickComposer from '../../components/circle/QuickComposer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('Circle')

const loading = ref(false)
const loadingMore = ref(false)
const posts = ref<any[]>([])
const activeTab = ref('recommend')
const currentPage = ref(1)
const hasMore = ref(true)
const error = ref('')

const showCreateModal = ref(false)

const showRepostModal = ref(false)
const repostContent = ref('')
const currentRepostPost = ref<any>(null)
const reposting = ref(false)

const showPreview = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

const sentinelRef = ref<HTMLElement | null>(null)
const feedListRef = ref<HTMLElement | null>(null)
void feedListRef.value

const switchTab = (tab: string) => {
  activeTab.value = tab
}

const fetchPosts = async (reset = false) => {
  if (loading.value && !reset) return
  if (reset) {
    currentPage.value = 1
    hasMore.value = true
    error.value = ''
  }
  loading.value = true
  try {
    const fn = activeTab.value === 'following' ? circleApi.getFollowingFeed : circleApi.getRecommendFeed
    const res = await fn({ pageNum: currentPage.value, pageSize: 20 })
    const list = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    const mapped = list.map(p => ({ ...p, isLiked: p.isLiked || false, likeAnim: false }))
    posts.value = reset ? mapped : [...posts.value, ...mapped]
    hasMore.value = list.length >= 20
    return true
  } catch (err: any) {
    logger.error('fetchPosts error', { error: err.message })
    error.value = err.response?.data?.message || err.message || '加载失败'
    if (reset) posts.value = []
    return false
  } finally {
    loading.value = false
  }
}

const retryLoad = () => fetchPosts(true)

const loadMore = async () => {
  if (loadingMore.value || loading.value || !hasMore.value) return
  loadingMore.value = true
  currentPage.value++
  const ok = await fetchPosts()
  if (!ok) currentPage.value--
  loadingMore.value = false
}

watch(activeTab, () => fetchPosts(true))

let observer: IntersectionObserver | null = null
const setupObserver = () => {
  if (!sentinelRef.value) return
  observer = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) loadMore()
  }, { rootMargin: '200px' })
  observer.observe(sentinelRef.value)
}

watch(() => posts.value.length, () => {
  nextTick(() => {
    if (observer) observer.disconnect()
    setupObserver()
  })
})

const { confirm, ConfirmDialog } = useConfirm()

const deletePost = async (post: any) => {
  const ok = await confirm('确定要删除这条动态吗？删除后不可恢复。')
  if (!ok) return
  try {
    await circleApi.deletePost(post.id)
    posts.value = posts.value.filter(p => p.id !== post.id)
    toast.success('删除成功')
  } catch (err: any) {
    logger.error('deletePost error', { error: err.message })
    toast.error(err.response?.data?.message || '删除失败')
  }
}

const toggleLike = async (post: any) => {
  if (!userStore.isLoggedIn) { toast.warning('请先登录'); return }
  if (post._likeLoading) return
  post._likeLoading = true
  const prevLiked = post.isLiked
  const prevCount = post.likeCount || 0
  post.isLiked = !post.isLiked
  post.likeCount = prevCount + (post.isLiked ? 1 : -1)
  post.likeAnim = true
  setTimeout(() => post.likeAnim = false, 400)
  try {
    const res = await circleApi.toggleLike(post.id)
    const data = res.data as any
    if (data?.action) {
      post.isLiked = data.action === 'like'
    }
    if (data?.likeCount !== undefined) {
      post.likeCount = data.likeCount
    }
  } catch (err: any) {
    post.isLiked = prevLiked
    post.likeCount = prevCount
    logger.error('toggleLike error', { error: err.message })
    toast.error(err.response?.data?.message || '点赞失败，请稍后重试')
  } finally {
    post._likeLoading = false
  }
}

const openRepostModal = (post: any) => {
  if (!userStore.isLoggedIn) { toast.warning('请先登录'); return }
  currentRepostPost.value = post
  repostContent.value = ''
  showRepostModal.value = true
}

const closeRepostModal = () => {
  showRepostModal.value = false
  currentRepostPost.value = null
  repostContent.value = ''
}

const confirmRepost = async () => {
  if (!currentRepostPost.value) return
  reposting.value = true
  try {
    await circleApi.repost(currentRepostPost.value.id, repostContent.value)
    closeRepostModal()
    toast.success('转发成功')
    fetchPosts(true)
  } catch (err: any) {
    logger.error('repost error', { error: err.message })
    toast.error(err.response?.data?.message || '转发失败')
  } finally {
    reposting.value = false
  }
}

const openImagePreview = (images: string[], idx: number) => {
  previewImages.value = images
  previewIndex.value = idx
  showPreview.value = true
}

const previewPrev = () => {
  previewIndex.value = (previewIndex.value - 1 + previewImages.value.length) % previewImages.value.length
}

const previewNext = () => {
  previewIndex.value = (previewIndex.value + 1) % previewImages.value.length
}

const openCreateModal = () => {
  if (!userStore.isLoggedIn) {
    toast.warning('请先登录')
    router.push('/login')
    return
  }
  showCreateModal.value = true
}

const onPostPublished = () => {
  showCreateModal.value = false
  fetchPosts(true)
}

onMounted(() => {
  fetchPosts(true)
  if (route.query.create === 'true') {
    nextTick(() => {
      openCreateModal()
      router.replace({ query: {} })
    })
  }
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.circle-page {
  position: relative;
  max-width: 1080px;
  margin: 0 auto;
  padding: var(--spacing-lg) var(--spacing-md);
  min-height: 100vh;
}

/* Subtle dot texture background */
.circle-bg-texture {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -1;
  opacity: 0.35;
  background-image: radial-gradient(circle, var(--gray-300) 0.5px, transparent 0.5px);
  background-size: 24px 24px;
}

.circle-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: var(--spacing-lg);
  align-items: start;
}

.circle-main {
  min-width: 0;
  max-width: 640px;
}

.circle-container {
  width: 100%;
}

/* Back Button */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 0.8125rem;
  font-weight: 500;
  transition: all var(--transition);
  margin-bottom: var(--spacing-md);
}

.back-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  transform: translateX(-2px);
}

/* Feed Tabs */
.feed-tabs {
  display: flex;
  gap: var(--spacing-0_5);
  margin-bottom: var(--spacing-md);
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: var(--spacing-1);
}

.feed-tab {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 0;
  font-size: 0.875rem;
  font-weight: 500;
  background: transparent;
  border: none;
  border-radius: var(--radius);
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition);
}

.feed-tab:hover {
  color: var(--text-secondary);
}

.feed-tab.is-active {
  background: var(--gray-50);
  color: var(--primary);
  font-weight: 600;
}

/* Skeleton */
.skeleton-header {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--skeleton-base) 25%, var(--skeleton-highlight) 50%, var(--skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info { flex: 1; }
.skeleton-name { width: 100px; height: 14px; background: var(--skeleton-base); border-radius: var(--radius-xs); margin-bottom: var(--spacing-sm); animation: shimmer 1.5s infinite; background-size: 200% 100%; }
.skeleton-time { width: 60px; height: 12px; background: var(--skeleton-base); border-radius: var(--radius-xs); animation: shimmer 1.5s infinite; background-size: 200% 100%; }

.skeleton-content {
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
}

.skeleton-text { height: 14px; background: var(--skeleton-base); border-radius: var(--radius-xs); margin-bottom: var(--spacing-sm); animation: shimmer 1.5s infinite; background-size: 200% 100%; }
.skeleton-text--short { width: 60%; }
.skeleton-images { display: flex; gap: var(--spacing-xs); margin-top: var(--spacing-md); }
.skeleton-img { flex: 1; aspect-ratio: 1; background: var(--skeleton-base); border-radius: var(--radius-sm); animation: shimmer 1.5s infinite; background-size: 200% 100%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* States */
.feed-state {
  text-align: center;
  padding: var(--spacing-3xl) var(--spacing-lg);
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.feed-state__icon {
  color: var(--text-muted);
  opacity: 0.3;
  animation: stateFloat 4s ease-in-out infinite;
}

@keyframes stateFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.feed-state h3,
.feed-state__title {
  margin: var(--spacing-md) 0 var(--spacing-sm);
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.feed-state p,
.feed-state__text {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-bottom: var(--spacing-md);
}

.feed-state svg {
  color: var(--text-muted);
  opacity: 0.3;
}

/* Scroll Sentinel */
.scroll-sentinel {
  text-align: center;
  padding: var(--spacing-lg) 0;
}

.loading-spinner-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: var(--radius-full);
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.no-more {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: 0.75rem;
  color: var(--text-muted);
}

.no-more-line {
  flex: 1;
  height: 1px;
  background: var(--border);
}

.no-more-text {
  white-space: nowrap;
}

/* Floating Action Button */
.fab-create {
  position: fixed;
  bottom: var(--spacing-xl);
  right: var(--spacing-xl);
  width: 52px;
  height: 52px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(13, 148, 136, 0.35);
  transition: all var(--transition);
  z-index: var(--z-fixed);
}

.fab-create:hover {
  background: var(--primary-hover);
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(13, 148, 136, 0.45);
}

.fab-create:active {
  transform: scale(0.96);
}

/* Modal Overlay */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  padding: var(--spacing-lg);
}

/* Modal Box */
.modal-box {
  width: 100%;
  max-width: 500px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
}

.modal-box__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border);
}

.modal-box__header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-box__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition);
}

.modal-box__close:hover {
  background: var(--error-light);
  color: var(--error);
  border-color: var(--error);
}

.modal-box__body {
  padding: var(--spacing-lg);
  overflow-y: auto;
  flex: 1;
}

.modal-box__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--border);
}

.modal-textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: var(--spacing-md);
  font-size: 0.9375rem;
  line-height: 1.6;
  resize: none;
  color: var(--text-primary);
  background: var(--surface-solid);
  font-family: inherit;
  min-height: 100px;
  box-sizing: border-box;
  transition: all var(--transition);
}

.modal-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.modal-textarea::placeholder {
  color: var(--text-muted);
}

.char-count {
  text-align: right;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: var(--spacing-xs);
}

.char-count.is-warn {
  color: var(--warning);
}

/* Repost Preview */
.repost-preview {
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
  background: var(--gray-50);
  border: 1px solid var(--border);
}

.repost-preview__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.repost-preview__avatar {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.repost-preview__name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.repost-preview__text {
  font-size: 0.875rem;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Image Preview */
.preview-overlay {
  background: rgba(0, 0, 0, 0.92);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: var(--z-toast);
  cursor: zoom-out;
}

.preview-image {
  max-width: 90vw;
  max-height: 85vh;
  object-fit: contain;
  border-radius: var(--radius);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.4);
}

.preview-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
}

.preview-nav:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-50%) scale(1.08);
}

.preview-nav--prev { left: var(--spacing-lg); }
.preview-nav--next { right: var(--spacing-lg); }

.preview-close {
  position: absolute;
  top: var(--spacing-lg);
  right: var(--spacing-lg);
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
}

.preview-close:hover {
  background: var(--error);
  border-color: var(--error);
  transform: scale(1.08);
}

.preview-counter {
  position: absolute;
  bottom: var(--spacing-lg);
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  font-size: 0.875rem;
  background: rgba(0, 0, 0, 0.5);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-full);
}

/* Transitions */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s var(--ease-out);
}

.modal-fade-enter-active .modal-box,
.modal-fade-leave-active .modal-box {
  transition: transform 0.25s var(--ease-out);
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-box,
.modal-fade-leave-to .modal-box {
  transform: scale(0.96) translateY(8px);
}

/* Responsive */
@media (max-width: 768px) {
  .circle-page {
    padding: var(--spacing-md) var(--spacing-sm);
  }

  .circle-layout {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .circle-main {
    max-width: none;
  }

  .circle-container {
    max-width: 640px;
    margin: 0 auto;
  }

  .fab-create {
    bottom: var(--spacing-lg);
    right: var(--spacing-lg);
    width: 48px;
    height: 48px;
  }

  .modal-box {
    max-height: 95vh;
    margin: var(--spacing-sm);
    border-radius: var(--radius-lg);
  }

  .modal-box__body {
    padding: var(--spacing-md);
  }

  .preview-nav--prev { left: var(--spacing-sm); }
  .preview-nav--next { right: var(--spacing-sm); }
}
</style>
