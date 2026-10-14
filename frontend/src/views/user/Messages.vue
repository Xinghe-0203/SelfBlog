<template>
  <div class="messages-page">
    <button class="back-btn-floating" @click="goBack">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
    </button>

    <div class="messages-layout">
      <!-- Conversation list -->
      <aside class="conv-panel" :class="{ show: showMobileList }">
        <div class="conv-header">
          <h2>私信</h2>
          <button class="conv-close" @click="showMobileList = false" v-if="activeConversation">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="conv-scroll">
          <!-- Loading -->
          <div v-if="loadingConversations" class="conv-empty">
            <div class="spinner"></div>
            <span>加载中</span>
          </div>

          <!-- Error -->
          <div v-else-if="conversationsError" class="conv-empty">
            <p class="conv-error-text">{{ conversationsError }}</p>
            <button class="btn btn-xs btn-primary" @click="fetchConversations()">重试</button>
          </div>

          <!-- List -->
          <div v-else-if="conversations.length > 0">
            <div
              v-for="conv in conversations"
              :key="conv.conversationId || `virtual-${conv.user?.id}`"
              class="conv-item"
              :class="{ active: activeConversation?.conversationId != null ? activeConversation.conversationId === conv.conversationId : activeConversation?.user?.id === conv.user?.id }"
              @click="selectConversation(conv)"
            >
              <img :src="conv.user?.avatar || defaultAvatar" :alt="conv.user?.nickname || conv.user?.username" class="conv-avatar" @error="onAvatarError" />
              <div class="conv-body">
                <div class="conv-row">
                  <span class="conv-name">{{ conv.user?.nickname || conv.user?.username }}</span>
                  <span class="conv-time">{{ formatRelativeTime(conv.lastMessageTime) }}</span>
                </div>
                <div class="conv-row">
                  <span class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</span>
                  <span v-if="conv.unreadCount > 0" class="conv-unread">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty -->
          <div v-else class="conv-empty">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="conv-empty-icon"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            <p class="conv-empty-title">暂无私信</p>
            <p class="conv-empty-desc">开始与校园好友对话吧</p>
          </div>
        </div>
      </aside>

      <!-- Message area -->
      <main class="msg-panel">
        <template v-if="activeConversation">
          <!-- Chat header -->
          <div class="msg-header">
            <div class="msg-header-user">
              <img :src="activeConversation.user?.avatar || defaultAvatar" :alt="activeConversation.user?.nickname" class="msg-header-avatar" @error="onAvatarError" />
              <span class="msg-header-name">{{ activeConversation.user?.nickname || activeConversation.user?.username }}</span>
            </div>
          </div>

          <!-- Error -->
          <div v-if="messagesError" class="msg-empty">
            <p class="msg-error-text">{{ messagesError }}</p>
            <button class="btn btn-xs btn-primary" @click="selectConversation(activeConversation)">重试</button>
          </div>

          <!-- Loading -->
          <div v-else-if="loadingMessages" class="msg-empty">
            <div class="spinner"></div>
            <span>加载消息中</span>
          </div>

          <!-- Messages -->
          <div v-else class="msg-list" ref="messageList">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="msg-row"
              :class="{ mine: String(msg.sender?.id) === String(userStore.userId) }"
            >
              <img :src="msg.sender?.avatar || defaultAvatar" :alt="msg.sender?.nickname" class="msg-avatar" @error="onAvatarError" />
              <div class="msg-bubble">
                <p class="msg-text">{{ msg.content }}</p>
                <span class="msg-time">{{ formatMessageTime(msg.createTime) }}</span>
              </div>
            </div>
          </div>

          <!-- Input -->
          <div class="msg-input-bar">
            <input
              v-model="newMessage"
              type="text"
              maxlength="1000"
              placeholder="输入消息..."
              @keyup.enter="sendMessage"
              class="msg-input"
            />
            <button class="msg-send" @click="sendMessage" :disabled="!newMessage.trim() || sending">
              <span v-if="sending" class="sending-spinner"></span>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            </button>
          </div>
        </template>

        <!-- No conversation selected -->
        <div v-else class="msg-empty msg-placeholder">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="msg-placeholder-icon"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <p class="msg-placeholder-title">选择一个会话</p>
          <p class="msg-placeholder-desc">从左侧列表选择好友开始聊天</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { messageApi } from '../../api/message'
import { userApi } from '../../api/user'
import { formatRelativeTime } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('Messages')

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const defaultAvatar = '/default-avatar.png'

const onAvatarError = (e: Event) => {
  const target = e.target as HTMLImageElement
  if (target.src !== defaultAvatar) {
    target.src = defaultAvatar
  }
}

const formatMessageTime = (time: string | null | undefined) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const showMobileList = ref(true)
const conversations = ref<any[]>([])
const activeConversation = ref<any>(null)
const messages = ref<any[]>([])
const newMessage = ref('')
const messageList = ref<HTMLElement | null>(null)
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const conversationsError = ref('')
const messagesError = ref('')
const sending = ref(false)
let pollingInterval: ReturnType<typeof setInterval> | null = null

const fetchConversations = async (silent = false) => {
  if (!silent) loadingConversations.value = true
  conversationsError.value = ''
  try {
    const response = await messageApi.getConversations()
    const newConversations = response.data || []
    conversations.value = newConversations

    if (!silent && route.query.userId && !activeConversation.value) {
      const targetUserId = parseInt(route.query.userId as string)
      const conv = newConversations.find((c: any) => c.user?.id === targetUserId)
      if (conv) {
        await selectConversation(conv)
      } else {
        try {
          const userRes = await userApi.getUserById(targetUserId)
          const targetUser = userRes.data || userRes
          if (targetUser) {
            const virtualConv = {
              conversationId: null,
              user: {
                id: targetUser.id,
                username: targetUser.username,
                nickname: targetUser.nickname,
                avatar: targetUser.avatar
              },
              lastMessage: '',
              lastMessageTime: null,
              unreadCount: 0
            }
            await selectConversation(virtualConv)
          }
        } catch (e: any) {
          logger.error('Failed to start conversation with user', { error: e.message, userId: targetUserId })
          toast.error('无法开始对话，用户不存在')
        }
      }
    }
  } catch (error: any) {
    logger.error('Failed to fetch conversations', { error: error.message })
    if (!silent) conversationsError.value = '加载会话失败，请稍后重试'
  } finally {
    if (!silent) loadingConversations.value = false
  }
}

const selectConversation = async (conv: any) => {
  showMobileList.value = false
  activeConversation.value = conv
  messagesError.value = ''
  messages.value = []

  const partnerId = conv.user?.id
  if (!partnerId) return

  loadingMessages.value = true
  try {
    const response = await messageApi.getConversationMessages(partnerId, { pageNum: 1, pageSize: 20 })
    messages.value = response.data?.records || []

    if (conv.unreadCount > 0) {
      conv.unreadCount = 0
      try {
        await messageApi.markConversationAsRead(partnerId)
      } catch (e: any) {
        logger.warn('Failed to mark conversation as read', { error: e.message })
      }
    }

    await scrollToBottom()
  } catch (error: any) {
    logger.error('Failed to fetch messages', { error: error.message })
    messagesError.value = '加载消息失败'
  } finally {
    loadingMessages.value = false
  }
}

const sendMessage = async () => {
  const content = newMessage.value.trim()
  if (!content || !activeConversation.value || sending.value) return
  if (content.length > 1000) {
    toast.warning('消息内容不能超过1000个字符')
    return
  }

  const receiverId = activeConversation.value.user?.id
  if (!receiverId) return

  sending.value = true
  try {
    await messageApi.sendMessage({ receiverId, content })

    if (activeConversation.value.conversationId == null) {
      await fetchConversations(true)
      const realConv = conversations.value.find(c => c.user?.id === receiverId)
      if (realConv) activeConversation.value = realConv
    }

    messages.value.push({
      id: Date.now(),
      sender: { id: userStore.userId, avatar: userStore.user?.avatar || userStore.avatar, nickname: userStore.nickname || userStore.username },
      content,
      createTime: new Date().toISOString().slice(0, 19),
      isRead: 1
    })

    newMessage.value = ''
    await scrollToBottom()
  } catch (error: any) {
    logger.error('Failed to send message', { error: error.message })
    toast.error('发送失败')
  } finally {
    sending.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight
  }
}

const startPolling = () => {
  pollingInterval = setInterval(async () => {
    await fetchConversations(true)
    if (activeConversation.value && activeConversation.value.user?.id) {
      const partnerId = activeConversation.value.user.id
      const updatedConv = conversations.value.find(c => c.user?.id === partnerId)
      if (updatedConv) {
        activeConversation.value = updatedConv
      }
      try {
        const response = await messageApi.getConversationMessages(partnerId, { pageNum: 1, pageSize: 20 })
        const records = response.data?.records || []
        const wasAtBottom = messageList.value && (messageList.value.scrollTop + messageList.value.clientHeight >= messageList.value.scrollHeight - 50)
        messages.value = records
        if (wasAtBottom) {
          await scrollToBottom()
        }
      } catch (e: any) {
        logger.warn('Failed to refresh messages', { error: e.message })
      }
    }
  }, 10000)
}

const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

onMounted(() => {
  fetchConversations()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.messages-page {
  height: calc(100vh - 80px);
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  position: relative;
}

.back-btn-floating {
  position: fixed;
  left: 20px;
  top: 100px;
  z-index: 100;
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: var(--surface-solid);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  box-shadow: var(--shadow-sm);
}

.back-btn-floating:hover {
  color: var(--primary);
  border-color: var(--primary);
}

/* Layout */
.messages-layout {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 1px;
  background: var(--border-solid);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-md);
  overflow: hidden;
}

/* Conversation panel */
.conv-panel {
  display: flex;
  flex-direction: column;
  background: var(--surface-solid);
  overflow: hidden;
}

.conv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--gray-100);
}

.conv-header h2 {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
}

.conv-close {
  display: none;
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
}

.conv-close:hover {
  background: var(--gray-100);
  color: var(--text-primary);
}

.conv-scroll {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-lg);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-default);
  border-bottom: 1px solid var(--gray-50);
}

.conv-item:hover {
  background: var(--gray-50);
}

.conv-item.active {
  background: rgba(13, 148, 136, 0.06);
  border-left: 2px solid var(--primary);
}

.conv-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--border);
}

.conv-body {
  flex: 1;
  min-width: 0;
}

.conv-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.conv-row + .conv-row {
  margin-top: 2px;
}

.conv-name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-time {
  font-size: 0.625rem;
  color: var(--text-muted);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.conv-preview {
  font-size: var(--text-xs);
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.conv-unread {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 0.625rem;
  font-weight: 700;
  color: white;
  background: var(--primary);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

/* Conversation empty / loading */
.conv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--spacing-3xl) var(--spacing-lg);
  gap: var(--spacing-sm);
  color: var(--text-muted);
  height: 100%;
}

.conv-empty-icon {
  opacity: 0.3;
}

.conv-empty-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.conv-empty-desc {
  font-size: var(--text-xs);
  color: var(--text-muted);
  margin: 0;
}

.conv-error-text {
  color: var(--error);
  font-size: var(--text-xs);
  margin: 0;
}

/* Message panel */
.msg-panel {
  display: flex;
  flex-direction: column;
  background: var(--surface-solid);
  overflow: hidden;
}

.msg-header {
  padding: var(--spacing-sm) var(--spacing-lg);
  border-bottom: 1px solid var(--gray-100);
}

.msg-header-user {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.msg-header-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 1px solid var(--border);
}

.msg-header-name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

/* Message list */
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  min-height: 200px;
}

.msg-row {
  display: flex;
  gap: var(--spacing-sm);
  max-width: 75%;
  animation: fadeIn 0.2s ease;
}

.msg-row.mine {
  margin-left: auto;
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  align-self: flex-end;
  border: 1px solid var(--border);
}

.msg-bubble {
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius);
  background: var(--gray-50);
  border: 1px solid var(--gray-100);
  max-width: 100%;
}

.msg-row.mine .msg-bubble {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.msg-text {
  font-size: var(--text-sm);
  line-height: 1.5;
  word-break: break-word;
  margin: 0;
}

.msg-time {
  display: block;
  font-size: 0.625rem;
  color: var(--text-muted);
  margin-top: 4px;
  text-align: right;
}

.msg-row.mine .msg-time {
  color: rgba(255, 255, 255, 0.7);
}

/* Input bar */
.msg-input-bar {
  display: flex;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-lg);
  border-top: 1px solid var(--gray-100);
}

.msg-input {
  flex: 1;
  padding: 10px 14px;
  font-size: var(--text-sm);
  font-family: var(--font-sans);
  background: var(--gray-50);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-primary);
  transition: all var(--duration-fast) var(--ease-default);
  outline: none;
}

.msg-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-light);
  background: var(--surface-solid);
}

.msg-input::placeholder {
  color: var(--text-muted);
}

.msg-send {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--primary);
  border: none;
  border-radius: var(--radius);
  color: white;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-fast) var(--ease-default);
}

.msg-send:hover:not(:disabled) {
  background: var(--primary-hover);
}

.msg-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.sending-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* No conversation */
.msg-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: var(--spacing-sm);
  text-align: center;
  color: var(--text-muted);
}

.msg-error-text {
  color: var(--error);
  font-size: var(--text-xs);
  margin: 0;
}

.msg-placeholder-icon {
  opacity: 0.2;
}

.msg-placeholder-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.msg-placeholder-desc {
  font-size: var(--text-xs);
  color: var(--text-muted);
  margin: 0;
}

/* Spinner */
.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-solid);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* Responsive */
@media (max-width: 768px) {
  .back-btn-floating {
    display: flex;
  }

  .messages-layout {
    grid-template-columns: 1fr;
  }

  .conv-panel {
    display: none;
  }

  .conv-panel.show {
    display: flex;
    position: fixed;
    inset: 0;
    z-index: 1000;
    border-radius: 0;
  }

  .conv-close {
    display: block;
  }

  .msg-row {
    max-width: 88%;
  }
}

@media (max-width: 1024px) {
  .back-btn-floating {
    display: flex;
  }
}
</style>
