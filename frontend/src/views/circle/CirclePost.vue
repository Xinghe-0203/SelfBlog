<template>
  <div class="circle-post-page">
    <button class="back-btn glass" @click="router.back()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      返回
    </button>

    <div class="post-container">
      <div class="post-card glass">
        <div class="card-header">
          <h2>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="display:inline;vertical-align:middle;margin-right:8px"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            {{ isEdit ? '编辑动态' : '发布动态' }}
          </h2>
        </div>

        <div class="card-body">
          <div class="create-top">
            <img :src="userStore.avatar || '/default-avatar.png'" class="user-avatar" />
            <div class="create-info">
              <span class="create-nickname">{{ userStore.nickname }}</span>
              <button class="visibility-selector glass-chip" @click="showPicker = !showPicker">
                <svg v-if="form.visibility === 0" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                <svg v-else-if="form.visibility === 1" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6"/><path d="M23 11h-6"/></svg>
                <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                {{ visibilityOptions[form.visibility] }}
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
              </button>
              <transition name="dropdown">
                <div v-if="showPicker" class="visibility-dropdown glass">
                  <div v-for="(label, key) in visibilityOptions" :key="key" class="vis-item" :class="{ active: form.visibility === Number(key) }" @click="form.visibility = Number(key); showPicker = false">
                    <span class="vis-icon">
                      <svg v-if="Number(key) === 0" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                      <svg v-else-if="Number(key) === 1" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6"/><path d="M23 11h-6"/></svg>
                      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    </span>
                    <div>
                      <div class="vis-label">{{ label }}</div>
                      <div class="vis-desc">{{ ['所有人可见', '仅关注的粉丝可见', '只有自己可见'][key] }}</div>
                    </div>
                  </div>
                </div>
              </transition>
            </div>
          </div>

          <div class="textarea-wrapper">
            <textarea v-model="form.content" class="post-textarea" placeholder="分享你的校园生活... 使用 @ 提及用户" rows="6" maxlength="2000" @input="handleTextareaInput" ref="textareaRef"></textarea>
            <transition name="dropdown">
              <div v-if="showMentionDropdown" class="mention-dropdown glass">
                <div v-if="mentionLoading" class="mention-loading">
                  <span class="spinner-small"></span> 搜索中...
                </div>
                <div v-else-if="mentionResults.length === 0" class="mention-empty">未找到用户</div>
                <div v-else>
                  <div v-for="user in mentionResults" :key="user.id" class="mention-item" @mousedown.prevent="selectMention(user)">
                    <img :src="user.avatar || '/default-avatar.png'" class="mention-avatar" />
                    <span class="mention-name">{{ user.nickname || user.username }}</span>
                  </div>
                </div>
              </div>
            </transition>
          </div>
          <div class="char-count" :class="{ warn: form.content.length > 1800 }">{{ form.content.length }}/2000</div>

          <div class="location-input-wrapper">
            <button class="location-btn glass-chip" @click="showLocationInput = !showLocationInput">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              {{ form.location ? form.location : '添加位置' }}
            </button>
            <transition name="dropdown">
              <div v-if="showLocationInput" class="location-input-box glass">
                <input v-model="form.location" placeholder="输入位置信息..." maxlength="100" />
                <button class="clear-location" @click="form.location = ''; showLocationInput = false" v-if="form.location">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
            </transition>
          </div>

          <div class="toggle-options">
            <label class="toggle-item glass-chip">
              <span class="toggle-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                允许评论
              </span>
              <input type="checkbox" v-model="allowComment" class="toggle-checkbox" />
              <span class="toggle-slider"></span>
            </label>
            <label class="toggle-item glass-chip">
              <span class="toggle-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
                允许转发
              </span>
              <input type="checkbox" v-model="allowRepost" class="toggle-checkbox" />
              <span class="toggle-slider"></span>
            </label>
          </div>

          <div class="topic-selector">
            <div v-if="selectedTopics.length" class="selected-topics">
              <div v-for="topic in selectedTopics" :key="topic.id" class="selected-topic">
                <span class="topic-badge glass-chip">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
                  {{ topic.name }}
                </span>
                <button class="remove-topic" @click="removeTopic(topic)" title="移除话题">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
            </div>
            <div class="topic-input-wrapper">
              <div class="topic-search-box glass">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                <input v-model="topicSearch" placeholder="添加话题..." @focus="showTopicDropdown = true" @blur="hideTopicDropdown" />
              </div>
              <transition name="dropdown">
                <div v-if="showTopicDropdown && filteredTopics.length" class="topic-dropdown glass">
                  <div v-for="topic in filteredTopics" :key="topic.id" class="topic-dropdown-item" @mousedown.prevent="selectTopic(topic)">
                    <span class="topic-name">#{{ topic.name }}</span>
                    <span class="topic-count">{{ topic.postCount || 0 }} 篇</span>
                  </div>
                </div>
              </transition>
              <div v-if="topicSearch.trim() && !filteredTopics.length" class="no-topics">
                <button class="btn btn-text btn-xs" @click="createTopicAndAdd">创建「{{ topicSearch }}」话题</button>
              </div>
            </div>
          </div>

          <div v-if="form.images.length" class="uploaded-images">
            <div v-for="(img, idx) in form.images" :key="idx" class="image-item">
              <img :src="img" alt="" />
              <button class="remove-image" @click="form.images.splice(idx, 1)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>

          <div v-if="form.videos.length" class="uploaded-videos">
            <div v-for="(video, idx) in form.videos" :key="idx" class="video-item">
              <video :src="video" class="video-preview" muted preload="metadata"></video>
              <button class="remove-video" @click="form.videos.splice(idx, 1)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>

          <div v-if="uploading" class="upload-progress-container">
            <div class="upload-progress-bar">
              <div class="progress-fill" :style="{ width: uploadPercent + '%' }"></div>
            </div>
            <span class="upload-progress-text">上传中 {{ currentUploadIndex }}/{{ totalUploadCount }}... {{ uploadPercent }}%</span>
          </div>

          <div class="toolbar">
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple @change="handleUpload" ref="fileInput" hidden />
            <input type="file" accept="video/mp4,video/webm" @change="handleVideoUpload" ref="videoInput" hidden />
            <button class="tool-btn glass-chip" @click="fileInput?.click()" :disabled="form.images.length >= 9">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
              图片
            </button>
            <button class="tool-btn glass-chip" @click="videoInput?.click()">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2"/></svg>
              视频
            </button>
          </div>
        </div>

        <div class="card-footer">
          <button class="btn btn-ghost" @click="goBack">取消</button>
          <button class="btn btn-primary" @click="publishPost" :disabled="!form.content.trim() || publishing">
            {{ publishing ? (isEdit ? '保存中...' : '发布中...') : (isEdit ? '保存' : '发布') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { circleApi } from '../../api/circle'
import { topicApi } from '../../api/topic'
import { userApi } from '../../api/user'
import { mediaApi } from '../../api/media'
import { useUserStore } from '../../stores/user'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import type { Topic } from '../../types/topic'
import type { User } from '../../types/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('CirclePost')
const isEdit = computed(() => !!route.params.id)
const editingPostId = computed(() => route.params.id as string)

const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const MAX_VIDEO_SIZE = 100 * 1024 * 1024

const publishing = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const showPicker = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const videoInput = ref<HTMLInputElement | null>(null)
const currentUploadIndex = ref(0)
const totalUploadCount = ref(0)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const selectedTopics = ref<{ id: number; name: string }[]>([])
const showTopicDropdown = ref(false)
const topicSearch = ref('')
const allTopics = ref<Topic[]>([])

const filteredTopics = computed(() => {
  if (!topicSearch.value) return allTopics.value
  const q = topicSearch.value.toLowerCase()
  return allTopics.value.filter(t => t.name.toLowerCase().includes(q))
})

const selectTopic = (topic: { id: number; name: string }) => {
  if (!selectedTopics.value.find(t => t.id === topic.id)) {
    selectedTopics.value.push(topic)
  }
  topicSearch.value = ''
  showTopicDropdown.value = false
}

const removeTopic = (topic: { id: number; name: string }) => {
  selectedTopics.value = selectedTopics.value.filter(t => t.id !== topic.id)
}

async function createTopicAndAdd() {
  const name = topicSearch.value.trim()
  if (!name) return
  try {
    const res = await topicApi.createTopic({ name, description: '' })
    // 后端返回 Result.success(topicId)，res.data 是话题ID（Long类型）
    const newTopicId = (res.data as any)?.id ?? res.data
    if (newTopicId) {
      const newTopic: { id: number; name: string } = { id: Number(newTopicId), name }
      if (!selectedTopics.value.find(t => t.id === newTopic.id)) {
        selectedTopics.value.push(newTopic)
      }
    }
    topicApi.getTopicList({ pageNum: 1, pageSize: 100 }).then(res => {
      const data = res.data
      allTopics.value = Array.isArray(data) ? data : (data?.records || [])
    })
    toast.success('话题已创建')
    topicSearch.value = ''
  } catch (err: any) {
    logger.error('create topic error', { error: err.message })
    toast.error(err.response?.data?.message || '创建话题失败')
  }
}

const hideTopicDropdown = () => {
  setTimeout(() => { showTopicDropdown.value = false }, 200)
}

const handleTextareaInput = () => {
  autoResize()
  const textarea = textareaRef.value
  if (!textarea) return
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = form.content.substring(0, cursorPos)
  const match = textBeforeCursor.match(/@(\w*)$/)
  if (match) {
    const keyword: string = match[1] || ''
    mentionKeyword.value = keyword
    showMentionDropdown.value = true
    if (mentionSearchTimer.value) clearTimeout(mentionSearchTimer.value)
    if (keyword.length > 0) {
      mentionSearchTimer.value = setTimeout(() => searchUsers(keyword), 300)
    } else {
      mentionResults.value = []
    }
  } else {
    showMentionDropdown.value = false
    mentionResults.value = []
  }
}

const searchUsers = async (keyword: string) => {
  if (!keyword.trim()) {
    mentionResults.value = []
    return
  }
  mentionLoading.value = true
  try {
    const res = await userApi.searchUsers({ keyword, pageNum: 1, pageSize: 10 })
    mentionResults.value = Array.isArray(res.data) ? res.data : []
  } catch (err: any) {
    logger.error('searchUsers error', { error: err.message })
    mentionResults.value = []
  } finally {
    mentionLoading.value = false
  }
}

const selectMention = (user: User) => {
  const textarea = textareaRef.value
  if (!textarea) return
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = form.content.substring(0, cursorPos)
  const textAfterCursor = form.content.substring(cursorPos)
  const matchPos = textBeforeCursor.lastIndexOf('@')
  if (matchPos === -1) return
  const newTextBefore = textBeforeCursor.substring(0, matchPos) + `@${user.nickname || user.username} `
  form.content = newTextBefore + textAfterCursor
  showMentionDropdown.value = false
  mentionResults.value = []
  nextTick(() => {
    textarea.focus()
    const newPos = newTextBefore.length
    textarea.setSelectionRange(newPos, newPos)
    autoResize()
  })
}

const autoResize = () => {
  const el = textareaRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = el.scrollHeight + 'px'
  }
}

const visibilityOptions: Record<number, string> = { 0: '公开', 1: '关注者可见', 2: '仅自己' }

const showLocationInput = ref(false)
const showMentionDropdown = ref(false)
const mentionKeyword = ref('')
const mentionSearchTimer = ref<any>(null)
const mentionLoading = ref(false)
const mentionResults = ref<User[]>([])
const allowComment = ref(true)
const allowRepost = ref(true)

const form = reactive({
  content: '',
  images: [] as string[],
  videos: [] as string[],
  visibility: 0,
  location: ''
})

const handleUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const allFiles = Array.from(target.files || [])
  const oversized = allFiles.filter(f => f.size > MAX_IMAGE_SIZE)
  for (const f of oversized) toast.warning(`${f.name} 超过10MB限制`)
  const files = allFiles.filter(f => f.size <= MAX_IMAGE_SIZE)
  if (files.length === 0) return
  totalUploadCount.value = files.length
  currentUploadIndex.value = 0
  uploading.value = true
  uploadPercent.value = 0
  try {
    let totalBytes = 0
    for (const file of files) { totalBytes += file.size }
    let uploadedBytes = 0
    for (let i = 0; i < files.length; i++) {
      if (form.images.length >= 9) break
      currentUploadIndex.value = i + 1
      const file = files[i]!
      const beforeBytes = uploadedBytes
      const res = await mediaApi.uploadFile(file, 'circle', (pe) => {
        if (pe.total) {
          uploadedBytes = beforeBytes + pe.loaded
          uploadPercent.value = Math.round((uploadedBytes / totalBytes) * 100)
        }
      })
      uploadedBytes = beforeBytes + file.size
      form.images.push(res.data.fileUrl)
    }
    toast.success('上传完成')
  } catch (err: any) {
    logger.error('upload error', { error: err.message })
    toast.error('上传失败')
  } finally {
    uploading.value = false
    uploadPercent.value = 0
    currentUploadIndex.value = 0
    totalUploadCount.value = 0
    if (fileInput.value) fileInput.value.value = ''
  }
}

const handleVideoUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (file.size > MAX_VIDEO_SIZE) {
    toast.warning('视频文件不能超过100MB')
    return
  }
  uploading.value = true
  uploadPercent.value = 0
  try {
    const res = await mediaApi.uploadFile(file, 'circle', (pe) => {
      if (pe.total) uploadPercent.value = Math.round((pe.loaded / pe.total) * 100)
    })
    form.videos.push(res.data.fileUrl)
    toast.success('视频上传成功')
  } catch (err: any) {
    logger.error('upload video error', { error: err.message })
    toast.error('视频上传失败')
  } finally {
    uploading.value = false
    uploadPercent.value = 0
    if (videoInput.value) videoInput.value.value = ''
  }
}

const publishPost = async () => {
  if (!form.content.trim()) return
  publishing.value = true
  const payload = {
    content: form.content,
    images: form.images,
    videos: form.videos,
    location: form.location || undefined,
    topicIds: selectedTopics.value.map(t => t.id),
    visibility: form.visibility,
    allowComment: allowComment.value ? 1 : 0,
    allowRepost: allowRepost.value ? 1 : 0
  }
  try {
    if (isEdit.value) {
      await circleApi.updatePost(editingPostId.value, payload)
      toast.success('保存成功')
    } else {
      await circleApi.createPost(payload)
      toast.success('发布成功')
    }
    router.push('/circle')
  } catch (err: any) {
    logger.error('publish error', { error: err.message })
    toast.error(err.response?.data?.message || (isEdit.value ? '保存失败' : '发布失败'))
  } finally {
    publishing.value = false
  }
}

const loading = ref(false)

const loadEditPost = async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await circleApi.getPostById(editingPostId.value)
    const post = res.data
    form.content = post.content || ''
    form.images = post.images || []
    form.videos = post.videoUrls || (post.videoUrl ? [post.videoUrl] : [])
    form.location = post.location || ''
    form.visibility = Number(post.visibility) || 0
    allowComment.value = post.allowComment !== false
    allowRepost.value = post.allowRepost !== false
    if (post.topicIds && post.topicNames) {
      selectedTopics.value = post.topicIds.map((id: number, idx: number) => ({
        id,
        name: post.topicNames![idx] || ''
      }))
    } else if (post.topicIds) {
      selectedTopics.value = post.topicIds.map((id: number) => ({ id, name: '' }))
    }
  } catch (err: any) {
    logger.error('load edit post error', { error: err.message })
    toast.error('加载动态失败')
    router.push('/circle')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    toast.warning('请先登录')
    router.push('/login')
    return
  }
  document.title = (isEdit.value ? '编辑动态' : '发布动态') + ' - 校友圈'
  if (isEdit.value) {
    loadEditPost()
  }
  topicApi.getTopicList({ pageNum: 1, pageSize: 100 }).then(res => {
    const data = res.data
    allTopics.value = Array.isArray(data) ? data : (data?.records || [])
  }).catch(() => {})
})
</script>

<style scoped>
.circle-post-page {
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

.post-card {
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.card-header {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--glass-border);
}

.card-header h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.card-body {
  padding: var(--spacing-lg);
}

.create-top {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--border-solid);
  box-shadow: var(--shadow-sm);
}

.create-info {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.create-nickname {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.visibility-selector {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: 2px 10px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
}

.visibility-selector:hover {
  background: var(--glass-hover);
  border-color: var(--primary);
  color: var(--primary);
}

.visibility-dropdown {
  position: absolute;
  top: calc(100% + var(--spacing-xs));
  left: 0;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: var(--spacing-xs);
  z-index: 100;
  min-width: 220px;
}

.vis-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all var(--transition);
}

.vis-item:hover,
.vis-item.active {
  background: var(--primary-light);
}

.vis-item.active {
  color: var(--primary);
}

.vis-icon {
  display: inline-flex;
  color: var(--text-muted);
}

.vis-item:hover .vis-icon,
.vis-item.active .vis-icon {
  color: var(--primary);
}

.vis-label { font-size: 0.8125rem; font-weight: 500; }
.vis-desc { font-size: 0.6875rem; color: var(--text-muted); margin-top: 2px; }

.post-textarea {
  width: 100%;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  padding: var(--spacing-md);
  font-size: 0.9375rem;
  line-height: 1.6;
  font-family: inherit;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
  transition: all var(--transition);
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

.post-textarea::placeholder {
  color: var(--text-muted);
}

.char-count {
  text-align: right;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.char-count.warn {
  color: var(--warning);
}

.uploaded-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-sm);
  margin: var(--spacing-md) 0;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--bg-secondary);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image {
  position: absolute;
  top: var(--spacing-xs);
  right: var(--spacing-xs);
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
}

.remove-image:hover {
  background: var(--error);
  transform: scale(1.1);
}

.uploaded-videos {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  margin: var(--spacing-md) 0;
}

.video-item {
  position: relative;
  border-radius: var(--radius);
  overflow: hidden;
  background: #000;
  max-width: 100%;
}

.video-preview {
  width: 100%;
  max-height: 200px;
  object-fit: contain;
  display: block;
}

.remove-video {
  position: absolute;
  top: var(--spacing-xs);
  right: var(--spacing-xs);
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
}

.remove-video:hover {
  background: var(--error);
  transform: scale(1.1);
}

.upload-progress-bar {
  height: 4px;
  background: var(--skeleton-base);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: var(--spacing-xs);
}

.upload-progress-container { margin-bottom: var(--spacing-md); }
.upload-progress-text { display: block; font-size: 0.75rem; color: var(--text-muted); text-align: center; }

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-start), var(--primary-end));
  border-radius: var(--radius-full);
  transition: width var(--transition-slow);
}

.toolbar {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.tool-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 0.8125rem;
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
}

.tool-btn:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}

.tool-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--glass-border);
}

.topic-selector {
  margin-bottom: var(--spacing-md);
}

.selected-topic {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--primary-light);
  border-radius: var(--radius-full);
}

.topic-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--primary);
}

.remove-topic {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.1);
  border: none;
  border-radius: var(--radius-full);
  color: var(--primary);
  cursor: pointer;
  transition: all var(--transition);
}

.remove-topic:hover {
  background: var(--error);
  color: #fff;
}

.topic-input-wrapper {
  position: relative;
}

.topic-search-box {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
}

.topic-search-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light), var(--glass-shadow);
}

.topic-search-box svg {
  color: var(--text-muted);
  flex-shrink: 0;
}

.topic-search-box input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  outline: none;
  color: var(--text-primary);
  padding: 0;
}

.topic-search-box input::placeholder {
  color: var(--text-muted);
}

.topic-dropdown {
  position: absolute;
  top: calc(100% + var(--spacing-xs));
  left: 0;
  right: 0;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  max-height: 200px;
  overflow-y: auto;
  z-index: 100;
}

.topic-dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-sm) var(--spacing-md);
  cursor: pointer;
  transition: all var(--transition);
}

.topic-dropdown-item:hover {
  background: var(--primary-light);
}

.topic-dropdown-item:not(:last-child) {
  border-bottom: 1px solid var(--glass-border);
}

.topic-name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--primary);
}

.topic-count {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all var(--transition) ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.textarea-wrapper {
  position: relative;
}

.mention-dropdown {
  position: absolute;
  bottom: calc(100% + var(--spacing-xs));
  left: 0;
  right: 0;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  max-height: 200px;
  overflow-y: auto;
  z-index: 100;
}

.mention-loading,
.mention-empty {
  padding: var(--spacing-md);
  text-align: center;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.spinner-small {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: var(--radius-full);
  animation: spin 0.6s linear infinite;
  vertical-align: middle;
  margin-right: var(--spacing-xs);
}

.mention-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  cursor: pointer;
  transition: all var(--transition);
}

.mention-item:hover {
  background: var(--primary-light);
}

.mention-item:not(:last-child) {
  border-bottom: 1px solid var(--glass-border);
}

.mention-avatar {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.mention-name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-primary);
}

.location-input-wrapper {
  margin-bottom: var(--spacing-md);
}

.location-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 0.8125rem;
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
}

.location-btn:hover {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}

.location-input-box {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  margin-top: var(--spacing-xs);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
}

.location-input-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light), var(--glass-shadow);
}

.location-input-box input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  outline: none;
  color: var(--text-primary);
  padding: 0;
}

.location-input-box input::placeholder {
  color: var(--text-muted);
}

.clear-location {
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

.clear-location:hover {
  color: var(--error);
  background: var(--error-light);
}

/* 话题选择 */
.topic-selector {
  margin-bottom: var(--spacing-md);
}

.selected-topic {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm);
  background: var(--glass-bg);
  border-radius: var(--radius);
}

.topic-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--primary-light);
  color: var(--primary);
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  font-weight: 500;
}

.remove-topic {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  opacity: 0.6;
  transition: all var(--transition);
  border-radius: var(--radius-full);
}

.remove-topic:hover {
  opacity: 1;
  background: var(--primary);
  color: white;
}

.topic-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.topic-search-box {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  background: var(--glass-bg);
  transition: all var(--transition);
}

.topic-search-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.topic-search-box svg {
  width: 16px;
  height: 16px;
  color: var(--text-muted);
}

.topic-search-box input {
  flex: 1;
  border: none;
  font-size: 0.875rem;
  background: transparent;
  color: var(--text-primary);
  padding: 0 var(--spacing-sm);
}

.topic-search-box input::placeholder {
  color: var(--text-muted);
}

.topic-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: var(--spacing-xs);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  background: var(--glass-bg);
  box-shadow: var(--shadow-md);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.topic-dropdown-item {
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: 0.875rem;
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition);
}

.topic-dropdown-item:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.no-topics {
  padding: var(--spacing-sm) var(--spacing-md);
  text-align: center;
  color: var(--text-muted);
  font-size: 0.75rem;
}

.no-topics button {
  margin-top: var(--spacing-sm);
}

.toggle-options {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
  flex-wrap: wrap;
}

.toggle-item {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-md);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
  position: relative;
}

.toggle-item:hover {
  background: var(--glass-hover);
  border-color: var(--primary);
}

.toggle-label {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.toggle-checkbox {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  width: 36px;
  height: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-full);
  position: relative;
  transition: all var(--transition);
}

.toggle-slider::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  background: var(--text-muted);
  border-radius: var(--radius-full);
  transition: all var(--transition);
}

.toggle-checkbox:checked + .toggle-slider {
  background: var(--primary);
}

.toggle-checkbox:checked + .toggle-slider::after {
  transform: translateX(16px);
  background: #fff;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 640px) {
  .circle-post-page {
    padding: var(--spacing-sm);
  }
  .card-body {
    padding: var(--spacing-md);
  }
  .card-header,
  .card-footer {
    padding: var(--spacing-md);
  }
}
</style>
