<template>
  <div class="notifications-page">
    <button class="back-btn" @click="router.back()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      返回
    </button>

    <header class="page-header">
      <div class="header-left">
        <h1>消息通知</h1>
        <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }} 条未读</span>
        <span v-else class="all-read">全部已读</span>
      </div>
      <button v-if="notifications.length > 0" class="btn btn-sm btn-ghost" @click="markAllAsRead()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
        全部已读
      </button>
    </header>

    <!-- Error -->
    <div v-if="error" class="error-block">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <p>{{ error }}</p>
      <button class="btn btn-sm btn-primary" @click="fetchNotifications()">重新加载</button>
    </div>

    <!-- Skeleton -->
    <div v-else-if="loading && notifications.length === 0" class="notif-list">
      <div v-for="n in 4" :key="n" class="notif-item skeleton-item">
        <div class="skel-circle"></div>
        <div class="skel-avatar"></div>
        <div class="skel-body">
          <div class="skel-line" style="width:60%"></div>
          <div class="skel-line" style="width:35%;height:10px;margin-top:6px"></div>
        </div>
      </div>
    </div>

    <!-- List -->
    <div v-else-if="notifications.length > 0" class="notif-list">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notif-item"
        :class="{ unread: !notification.isRead }"
        @click="handleNotification(notification)"
      >
        <span class="unread-dot" v-if="!notification.isRead"></span>
        <div v-else class="unread-dot-placeholder"></div>

        <img
          :src="notification.fromUser?.avatar || '/default-avatar.png'"
          :alt="notification.fromUser?.nickname || notification.fromUser?.username"
          class="notif-avatar"
          @error="onAvatarError"
        />

        <div class="notif-body">
          <p class="notif-text">
            <strong class="notif-sender">{{ notification.fromUser?.nickname || notification.fromUser?.username }}</strong>
            {{ getNotificationText(notification.type) }}
            <span v-if="notification.content" class="notif-target">{{ notification.content }}</span>
          </p>
          <span class="notif-time">{{ notification.timeAgo || formatRelativeTime(notification.createTime) }}</span>
        </div>

        <button class="notif-delete" @click.stop="deleteNotification(notification.id)" title="删除">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <div v-if="hasMore" class="load-more">
        <button class="btn btn-sm btn-ghost" @click="loadMore" :disabled="loading">
          <svg v-if="loading" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="empty-block">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
      <p class="empty-title">暂无通知</p>
      <p class="empty-desc">当有人与你互动时，通知会出现在这里</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '../../api/notification'
import { commentApi } from '../../api/comment'
import { formatRelativeTime } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const router = useRouter()
const logger = useLogger('Notifications')
const notifications = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const hasMore = ref(false)
const pageSize = 20
let pollingInterval: ReturnType<typeof setInterval> | null = null

const defaultAvatar = '/default-avatar.png'

const onAvatarError = (e: Event) => {
  const target = e.target as HTMLImageElement
  if (target.src !== defaultAvatar) {
    target.src = defaultAvatar
  }
}

const unreadCount = computed(() => notifications.value.filter(n => !n.isRead).length)

const fetchNotifications = async (silent = false) => {
  if (!silent) loading.value = true
  error.value = ''
  try {
    const response = await notificationApi.getNotifications({ pageNum: currentPage.value, pageSize: pageSize })
    const pageData = response.data || {}
    if (currentPage.value === 1) {
      notifications.value = pageData.records || []
    } else {
      notifications.value = [...notifications.value, ...(pageData.records || [])]
    }
    totalPages.value = pageData.pages || 1
    hasMore.value = currentPage.value < totalPages.value
  } catch (err: any) {
    logger.error('Failed to fetch notifications', { error: err.message })
    if (!silent) error.value = '加载通知失败'
  } finally {
    if (!silent) loading.value = false
  }
}

const loadMore = () => {
  if (currentPage.value < totalPages.value && !loading.value) {
    currentPage.value++
    fetchNotifications()
  }
}

const startPolling = () => {
  pollingInterval = setInterval(async () => {
    try {
      const response = await notificationApi.getNotifications({ pageNum: 1, pageSize: pageSize })
      const pageData = response.data || {}
      const newRecords = pageData.records || []
      const existingIds = new Set(notifications.value.map(n => n.id))
      const trulyNew = newRecords.filter(n => !existingIds.has(n.id))
      if (trulyNew.length > 0) {
        notifications.value = [...trulyNew, ...notifications.value]
      }
      totalPages.value = pageData.pages || 1
      hasMore.value = currentPage.value < totalPages.value
    } catch {
      // silent
    }
  }, 30000)
}

const getNotificationText = (type: string) => {
  const texts: Record<string, string> = {
    LIKE: '赞了你的文章',
    COMMENT: '评论了你的文章',
    REPLY: '回复了你的评论',
    FOLLOW: '关注了你',
    MENTION: '在评论中提到了你',
    SYSTEM: '系统通知',
    MESSAGE: '给你发了私信'
  }
  return texts[type] || '有新通知'
}

const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

const handleNotification = async (notification: any) => {
  if (!notification.isRead) {
    try {
      await notificationApi.markAsRead(notification.id)
      notification.isRead = true
    } catch (err: any) {
      logger.error('Failed to mark as read', { error: err.message })
    }
  }

  if (notification.type === 'FOLLOW' && notification.fromUser?.id) {
    router.push(`/user/${notification.fromUser.id}`)
  } else if (notification.type === 'MESSAGE') {
    router.push('/messages?userId=' + notification.fromUser?.id)
  } else if (notification.type === 'REPLY' || notification.type === 'MENTION') {
    if (notification.targetType === 'COMMENT' && notification.targetId) {
      try {
        const res = await commentApi.getCommentById(notification.targetId)
        const commentData = res.data as any
        const postId = commentData?.postId
        if (postId) {
          if (notification.targetType === 'COMMENT' && commentData?.circlePostId) {
            router.push(`/circle/${commentData.circlePostId}`)
          } else {
            router.push(`/post/${postId}`)
          }
        } else {
          router.push('/')
        }
      } catch {
        toast.info('该评论已被删除')
        router.push('/')
      }
    } else if (notification.targetType === 'CIRCLE_POST' && notification.targetId) {
      router.push(`/circle/${notification.targetId}`)
    } else if (notification.targetId) {
      router.push(`/post/${notification.targetId}`)
    }
  } else {
    if (notification.targetType === 'CIRCLE_POST') {
      router.push(`/circle/${notification.targetId}`)
    } else if (notification.targetType === 'COMMENT') {
      router.push(`/post/${notification.targetId}#comments`)
    } else if (notification.targetId) {
      router.push(`/post/${notification.targetId}`)
    }
  }
}

const markAllAsRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    toast.success('已全部标记为已读')
  } catch (error: any) {
    logger.error('Failed to mark all as read', { error: error.message })
    toast.error('操作失败')
  }
}

const deleteNotification = async (id: number) => {
  try {
    await notificationApi.deleteNotification(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
    toast.success('通知已删除')
  } catch (error: any) {
    logger.error('Failed to delete notification', { error: error.message })
    toast.error('删除失败')
  }
}

onMounted(() => {
  fetchNotifications()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.notifications-page {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

/* Back button */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: var(--text-sm);
  color: var(--text-muted);
  background: none;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  margin-bottom: var(--spacing-lg);
}

.back-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-sm);
}

.page-header h1 {
  font-family: var(--font-sans);
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: var(--tracking-tight);
}

.unread-badge {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--error);
  padding: 2px 8px;
  background: var(--error-light);
  border-radius: var(--radius-full);
}

.all-read {
  font-size: var(--text-xs);
  color: var(--text-muted);
}

/* Notification list */
.notif-list {
  display: flex;
  flex-direction: column;
}

.notif-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-sm);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-default);
  border-bottom: 1px solid var(--gray-100);
  position: relative;
}

.notif-item:first-child {
  border-top: 1px solid var(--gray-100);
}

.notif-item:hover {
  background: var(--gray-50);
}

.notif-item.unread {
  background: rgba(13, 148, 136, 0.03);
}

/* Unread dot */
.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: var(--primary);
  flex-shrink: 0;
}

.unread-dot-placeholder {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
}

/* Avatar */
.notif-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--border);
}

/* Body */
.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-text {
  font-size: var(--text-sm);
  color: var(--text-primary);
  line-height: 1.5;
  margin: 0;
}

.notif-sender {
  font-weight: 600;
  color: var(--text-primary);
}

.notif-target {
  color: var(--primary);
  font-weight: 500;
}

.notif-time {
  font-size: var(--text-xs);
  color: var(--text-muted);
  margin-top: 2px;
  display: block;
}

/* Delete button */
.notif-delete {
  opacity: 0;
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
  flex-shrink: 0;
}

.notif-item:hover .notif-delete {
  opacity: 1;
}

.notif-delete:hover {
  color: var(--error);
  background: var(--error-light);
}

/* Skeleton */
.skeleton-item {
  pointer-events: none;
}

.skel-circle {
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: var(--gray-200);
  flex-shrink: 0;
}

.skel-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: var(--gray-200);
  flex-shrink: 0;
  animation: pulse 1.5s ease-in-out infinite;
}

.skel-body {
  flex: 1;
}

.skel-line {
  height: 12px;
  border-radius: var(--radius-xs);
  background: var(--gray-200);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Load more */
.load-more {
  text-align: center;
  padding: var(--spacing-lg) 0;
}

/* Error */
.error-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-3xl) var(--spacing-lg);
  text-align: center;
  color: var(--text-muted);
}

.error-block p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* Empty */
.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-3xl) var(--spacing-lg);
  text-align: center;
}

.empty-icon {
  color: var(--text-muted);
  opacity: 0.3;
}

.empty-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--text-muted);
  margin: 0;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Responsive */
@media (max-width: 768px) {
  .notifications-page {
    padding: var(--spacing-md);
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-sm);
  }

  .notif-item {
    gap: var(--spacing-1);
    padding: var(--spacing-sm) 0;
  }

  .notif-avatar {
    width: 32px;
    height: 32px;
  }
}
</style>
