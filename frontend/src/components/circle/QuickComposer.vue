<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { circleApi } from '../../api/circle'
import { mediaApi } from '../../api/media'
import { topicApi } from '../../api/topic'
import { useUserStore } from '../../stores/user'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const emit = defineEmits<{
  published: []
}>()

const userStore = useUserStore()
const logger = useLogger('QuickComposer')

const textareaRef = ref<HTMLTextAreaElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const videoInput = ref<HTMLInputElement | null>(null)
const content = ref('')
const images = ref<string[]>([])
const videos = ref<string[]>([])
const uploading = ref(false)
const uploadPercent = ref(0)
const publishing = ref(false)
const isFocused = ref(false)

const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const MAX_VIDEO_SIZE = 100 * 1024 * 1024

// ─── Topics ───
const topicSearch = ref('')
const showTopicDropdown = ref(false)
const selectedTopics = ref<any[]>([])
const allTopics = ref<any[]>([])
const topicsLoading = ref(false)

const showCreateTopicModal = ref(false)
const creatingTopic = ref(false)
const newTopic = reactive({ name: '', description: '' })

const filteredTopics = computed(() => {
  if (!topicSearch.value) return allTopics.value
  const q = topicSearch.value.toLowerCase()
  return allTopics.value.filter(t => t.name.toLowerCase().includes(q))
})

const loadTopics = () => {
  if (allTopics.value.length > 0) return
  topicsLoading.value = true
  topicApi.getTopicList({ pageNum: 1, pageSize: 100 })
    .then(res => {
      const data = res.data as any
      allTopics.value = Array.isArray(data) ? data : (data?.records || [])
    })
    .catch(() => {})
    .finally(() => { topicsLoading.value = false })
}

const selectTopic = (topic: any) => {
  if (!selectedTopics.value.find(t => t.id === topic.id)) {
    selectedTopics.value.push(topic)
  }
  topicSearch.value = ''
  showTopicDropdown.value = false
}

const removeTopic = (topic: any) => {
  selectedTopics.value = selectedTopics.value.filter(t => t.id !== topic.id)
}

const hideTopicDropdown = () => {
  setTimeout(() => { showTopicDropdown.value = false }, 200)
}

const openCreateTopicModal = () => {
  showTopicDropdown.value = false
  newTopic.name = topicSearch.value || ''
  newTopic.description = ''
  showCreateTopicModal.value = true
}

const confirmCreateTopic = async () => {
  if (!newTopic.name.trim()) return
  creatingTopic.value = true
  try {
    const res = await topicApi.createTopic({
      name: newTopic.name.trim(),
      description: newTopic.description.trim() || undefined
    })
    const topicId = res.data
    const created = { id: topicId, name: newTopic.name.trim(), description: newTopic.description.trim(), postCount: 0 }
    allTopics.value.unshift(created)
    if (!selectedTopics.value.find(t => t.id === created.id)) {
      selectedTopics.value.push(created)
    }
    topicSearch.value = ''
    showCreateTopicModal.value = false
    toast.success('话题创建成功')
  } catch (err: any) {
    logger.error('createTopic error', { error: err.message })
    toast.error(err.response?.data?.message || '话题创建失败')
  } finally {
    creatingTopic.value = false
  }
}

// ─── Location ───
const location = ref('')
const showLocationInput = ref(false)

// ─── Auto Resize ───
const autoResize = () => {
  const el = textareaRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }
}

const handleInput = () => {
  autoResize()
}

// ─── Image Upload ───
const handleImageUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = Array.from(target.files || [])
  if (files.length === 0) return
  uploading.value = true
  try {
    for (const file of files) {
      if (file.size > MAX_IMAGE_SIZE) {
        toast.warning(`图片 ${file.name} 超过10MB限制`)
        continue
      }
      if (images.value.length >= 9) break
      const res = await mediaApi.uploadFile(file, 'circle')
      images.value.push(res.data.fileUrl)
    }
  } catch (err: any) {
    logger.error('upload image error', { error: err.message })
    toast.error(err.response?.data?.message || '图片上传失败')
  } finally {
    uploading.value = false
    if (imageInput.value) imageInput.value.value = ''
  }
}

const removeImage = (index: number) => {
  images.value.splice(index, 1)
}

// ─── Video Upload ───
const handleVideoUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = (target.files || [])[0]
  if (!file) return
  if (!file.type.startsWith('video/')) {
    toast.warning('只能上传视频文件')
    return
  }
  if (file.size > MAX_VIDEO_SIZE) {
    toast.warning('视频大小不能超过100MB')
    return
  }
  uploading.value = true
  uploadPercent.value = 0
  try {
    const res = await mediaApi.uploadFile(file, 'circle', (event) => {
      uploadPercent.value = Math.round((event.loaded * 100) / (event.total || 1))
    })
    videos.value.push(res.data.fileUrl)
  } catch (err: any) {
    logger.error('upload video error', { error: err.message })
    toast.error(err.response?.data?.message || '视频上传失败')
  } finally {
    uploading.value = false
    uploadPercent.value = 0
    if (videoInput.value) videoInput.value.value = ''
  }
}

const removeVideo = (index: number) => {
  videos.value.splice(index, 1)
}

// ─── Submit ───
const handleSubmit = async () => {
  if (!content.value.trim() && images.value.length === 0) return
  if (publishing.value) return
  publishing.value = true
  try {
    await circleApi.createPost({
      content: content.value.trim(),
      images: images.value.length > 0 ? images.value : undefined,
      videos: videos.value.length > 0 ? videos.value : undefined,
      visibility: 0,
      topicIds: selectedTopics.value.length > 0 ? selectedTopics.value.map(t => t.id) : undefined,
      location: location.value.trim() || undefined
    })
    content.value = ''
    images.value = []
    videos.value = []
    selectedTopics.value = []
    location.value = ''
    isFocused.value = false
    showLocationInput.value = false
    await nextTick()
    if (textareaRef.value) textareaRef.value.style.height = 'auto'
    toast.success('发布成功')
    emit('published')
  } catch (err: any) {
    logger.error('publish error', { error: err.message })
    toast.error(err.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    handleSubmit()
  }
}

// ─── Lifecycle ───
onMounted(() => {
  loadTopics()
})

// Close topic dropdown on outside click
const handleOutsideClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.composer-topic-wrap')) {
    showTopicDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleOutsideClick, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick, true)
})

// ─── Watchers ───
watch(showCreateTopicModal, (val) => {
  if (val) document.body.style.overflow = 'hidden'
  else document.body.style.overflow = ''
})
</script>

<template>
  <div class="quick-composer" :class="{ 'is-expanded': isFocused || content || images.length > 0 || videos.length > 0 }">
    <div class="composer-row">
      <img :src="userStore.avatar || '/default-avatar.png'" class="composer-avatar" alt="" />
      <div class="composer-input-wrap">
        <textarea
          ref="textareaRef"
          v-model="content"
          class="composer-textarea"
          placeholder="有什么新鲜事？"
          rows="1"
          maxlength="2000"
          @input="handleInput"
          @focus="isFocused = true"
          @blur="!content && images.length === 0 && videos.length === 0 && selectedTopics.length === 0 && !location && (isFocused = false)"
          @keydown="handleKeydown"
        ></textarea>
      </div>
    </div>

    <!-- Selected Topics -->
    <div v-if="selectedTopics.length > 0" class="composer-selected-topics">
      <span v-for="topic in selectedTopics" :key="topic.id" class="composer-topic-badge">
        #{{ topic.name }}
        <button class="composer-topic-remove" @click="removeTopic(topic)">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </span>
    </div>

    <!-- Topic Search Input (expanded only) -->
    <div v-if="isFocused" class="composer-topic-wrap">
      <div class="composer-topic-input">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          v-model="topicSearch"
          placeholder="添加话题..."
          @focus="showTopicDropdown = true"
          @blur="hideTopicDropdown"
        />
      </div>
      <transition name="qc-drop">
        <div v-if="showTopicDropdown" class="composer-topic-dropdown">
          <div v-if="topicsLoading" class="qc-dropdown-status">
            <span class="qc-spinner"></span> 加载中...
          </div>
          <template v-else>
            <div v-if="filteredTopics.length">
              <div
                v-for="topic in filteredTopics"
                :key="topic.id"
                class="qc-topic-option"
                @mousedown.prevent="selectTopic(topic)"
              >
                <span class="qc-topic-name">#{{ topic.name }}</span>
                <span class="qc-topic-count">{{ topic.postCount || 0 }} 篇</span>
              </div>
              <div class="qc-dropdown-divider"></div>
            </div>
            <div class="qc-topic-option qc-topic-create" @mousedown.prevent="openCreateTopicModal">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              <span>创建话题 "{{ topicSearch || '新话题' }}"</span>
            </div>
          </template>
        </div>
      </transition>
    </div>

    <!-- Location (expanded only) -->
    <div v-if="isFocused" class="composer-location">
      <button class="composer-loc-btn" @click="showLocationInput = !showLocationInput">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
        {{ location || '添加位置' }}
      </button>
      <transition name="qc-drop">
        <div v-if="showLocationInput" class="composer-loc-input">
          <input v-model="location" placeholder="输入位置信息..." maxlength="100" />
          <button v-if="location" class="composer-loc-clear" @click="location = ''; showLocationInput = false">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </transition>
    </div>

    <!-- Image Preview Strip -->
    <div v-if="images.length > 0" class="composer-images">
      <div v-for="(img, idx) in images" :key="idx" class="composer-image-item">
        <img :src="img" class="composer-image-thumb" alt="" />
        <button class="composer-image-remove" @click="removeImage(idx)" aria-label="移除图片">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
    </div>

    <!-- Video Preview Strip -->
    <div v-if="videos.length > 0" class="composer-videos">
      <div v-for="(vid, idx) in videos" :key="idx" class="composer-video-item">
        <video :src="vid" class="composer-video-thumb" preload="metadata"></video>
        <div class="composer-video-play-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="white"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        </div>
        <button class="composer-image-remove" @click="removeVideo(idx)" aria-label="移除视频">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
    </div>

    <!-- Upload Progress Bar -->
    <div v-if="uploading" class="composer-progress">
      <div class="composer-progress-bar">
        <div class="composer-progress-fill" :style="{ width: uploadPercent + '%' }"></div>
      </div>
      <span class="composer-progress-text">{{ uploadPercent }}%</span>
    </div>

    <!-- Action Bar -->
    <div class="composer-actions">
      <div class="composer-tools">
        <button class="tool-btn" @click="imageInput?.click()" :disabled="uploading || images.length >= 9" title="图片">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
        </button>
        <button class="tool-btn" @click="videoInput?.click()" :disabled="uploading || videos.length >= 1" title="视频">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
        </button>
        <button class="tool-btn" @click="isFocused = true; $nextTick(() => { showTopicDropdown = true })" title="话题">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><line x1="4" y1="9" x2="20" y2="9"/><line x1="4" y1="15" x2="20" y2="15"/><line x1="10" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="14" y2="21"/></svg>
        </button>
        <button class="tool-btn" @click="isFocused = true; showLocationInput = true" title="位置">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
        </button>
      </div>
      <div class="composer-submit">
        <span v-if="uploading" class="upload-hint">
          <span class="spinner-small"></span> 上传中...{{ uploadPercent > 0 ? ` ${uploadPercent}%` : '' }}
        </span>
        <span v-else-if="content.length > 0" class="char-hint" :class="{ warn: content.length > 1800 }">
          {{ content.length }}/2000
        </span>
        <button
          class="submit-btn"
          :disabled="(!content.trim() && images.length === 0 && videos.length === 0) || publishing || uploading"
          @click="handleSubmit"
        >
          {{ publishing ? '发送中...' : '发帖' }}
        </button>
      </div>
    </div>

    <input
      ref="imageInput"
      type="file"
      accept="image/*"
      multiple
      class="hidden-input"
      @change="handleImageUpload"
    />
    <input
      ref="videoInput"
      type="file"
      accept="video/*"
      class="hidden-input"
      @change="handleVideoUpload"
    />
  </div>

  <!-- Create Topic Modal -->
  <teleport to="body">
    <transition name="qc-modal-fade">
      <div v-if="showCreateTopicModal" class="qc-modal-overlay" @click.self="showCreateTopicModal = false">
        <div class="qc-modal">
          <div class="qc-modal-header">
            <h3>创建新话题</h3>
            <button class="qc-modal-close" @click="showCreateTopicModal = false">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="qc-modal-body">
            <div class="qc-field">
              <label class="qc-label">话题名称</label>
              <input v-model="newTopic.name" class="qc-input" placeholder="请输入话题名称" maxlength="20" />
              <div class="qc-field-char">{{ newTopic.name.length }}/20</div>
            </div>
            <div class="qc-field">
              <label class="qc-label">话题描述（可选）</label>
              <textarea v-model="newTopic.description" class="qc-textarea" placeholder="简单描述这个话题..." rows="3" maxlength="100"></textarea>
              <div class="qc-field-char">{{ newTopic.description.length }}/100</div>
            </div>
          </div>
          <div class="qc-modal-footer">
            <button class="qc-btn qc-btn--ghost" @click="showCreateTopicModal = false">取消</button>
            <button
              class="qc-btn qc-btn--primary"
              :disabled="!newTopic.name.trim() || creatingTopic"
              @click="confirmCreateTopic"
            >
              {{ creatingTopic ? '创建中...' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.quick-composer {
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  transition: all 0.25s var(--ease-out);
}

.quick-composer.is-expanded {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.composer-row {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
}

.composer-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--border);
}

.composer-input-wrap {
  flex: 1;
  min-width: 0;
}

.composer-textarea {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--text-primary);
  font-family: inherit;
  resize: none;
  min-height: 24px;
  max-height: 200px;
  padding: 6px 0;
  box-sizing: border-box;
}

.composer-textarea::placeholder {
  color: var(--text-muted);
}

/* ─── Selected Topics ─── */
.composer-selected-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: var(--spacing-sm);
  padding-left: 48px;
}

.composer-topic-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: var(--radius-full);
  font-size: 0.8125rem;
  font-weight: 500;
}

.composer-topic-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: var(--radius-full);
  background: transparent;
  border: none;
  color: var(--primary);
  cursor: pointer;
  padding: 0;
  transition: all var(--transition);
}

.composer-topic-remove:hover {
  background: var(--primary);
  color: #fff;
}

/* ─── Topic Search ─── */
.composer-topic-wrap {
  position: relative;
  margin-top: var(--spacing-sm);
  padding-left: 48px;
}

.composer-topic-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface-solid);
  transition: all var(--transition);
}

.composer-topic-input:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.composer-topic-input svg {
  color: var(--text-muted);
  flex-shrink: 0;
}

.composer-topic-input input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.8125rem;
  color: var(--text-primary);
  font-family: inherit;
  padding: 2px 0;
}

.composer-topic-input input::placeholder {
  color: var(--text-muted);
}

.composer-topic-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: 100%;
  margin-top: 4px;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: var(--z-dropdown);
  max-height: 240px;
  overflow-y: auto;
}

.qc-dropdown-status {
  padding: var(--spacing-md);
  text-align: center;
  font-size: 0.8125rem;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.qc-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: var(--radius-full);
  animation: qc-spin 0.6s linear infinite;
}

.qc-topic-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  cursor: pointer;
  transition: background var(--transition);
}

.qc-topic-option:hover {
  background: var(--gray-50);
}

.qc-topic-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
}

.qc-topic-count {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.qc-dropdown-divider {
  height: 1px;
  background: var(--border);
  margin: 2px 0;
}

.qc-topic-create {
  color: var(--primary);
  font-weight: 500;
}

.qc-topic-create svg {
  flex-shrink: 0;
}

/* ─── Location ─── */
.composer-location {
  margin-top: var(--spacing-sm);
  padding-left: 48px;
  position: relative;
}

.composer-loc-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: 0.8125rem;
  cursor: pointer;
  transition: all var(--transition);
}

.composer-loc-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.composer-loc-btn svg {
  flex-shrink: 0;
}

.composer-loc-input {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface-solid);
  transition: all var(--transition);
}

.composer-loc-input:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.composer-loc-input input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.8125rem;
  color: var(--text-primary);
  font-family: inherit;
  padding: 2px 0;
}

.composer-loc-input input::placeholder {
  color: var(--text-muted);
}

.composer-loc-clear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0;
  transition: all var(--transition);
}

.composer-loc-clear:hover {
  background: var(--error-light);
  color: var(--error);
}

/* ─── Image Preview ─── */
.composer-images {
  display: flex;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
  flex-wrap: wrap;
}

.composer-image-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--border);
}

.composer-image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.composer-image-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition);
}

.composer-image-remove:hover {
  background: var(--error);
}

/* ─── Video Preview ─── */
.composer-videos {
  display: flex;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
  flex-wrap: wrap;
}

.composer-video-item {
  position: relative;
  width: 100px;
  height: 72px;
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--border);
}

.composer-video-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.composer-video-play-icon {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
  pointer-events: none;
}

/* ─── Upload Progress ─── */
.composer-progress {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
  padding-left: 48px;
}

.composer-progress-bar {
  flex: 1;
  height: 4px;
  background: var(--gray-100);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.composer-progress-fill {
  height: 100%;
  background: var(--primary);
  border-radius: var(--radius-full);
  transition: width 0.3s var(--ease-out);
}

.composer-progress-text {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  min-width: 36px;
  text-align: right;
}

/* ─── Action Bar ─── */
.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--border);
}

.composer-tools {
  display: flex;
  gap: 2px;
}

.tool-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: transparent;
  border: none;
  color: var(--primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
}

.tool-btn:hover:not(:disabled) {
  background: var(--primary-light);
}

.tool-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.composer-submit {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.upload-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.spinner-small {
  width: 14px;
  height: 14px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: var(--radius-full);
  animation: qc-spin 0.6s linear infinite;
}

@keyframes qc-spin {
  to { transform: rotate(360deg); }
}

.char-hint {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.char-hint.warn {
  color: var(--warning);
}

.submit-btn {
  padding: 7px 20px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: #fff;
  border: none;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
  white-space: nowrap;
}

.submit-btn:hover:not(:disabled) {
  background: var(--primary-hover);
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.3);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hidden-input {
  display: none;
}

/* ─── Dropdown Transition ─── */
.qc-drop-enter-active,
.qc-drop-leave-active {
  transition: all 0.2s var(--ease-out);
}

.qc-drop-enter-from,
.qc-drop-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ─── Create Topic Modal ─── */
.qc-modal-overlay {
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

.qc-modal {
  width: 100%;
  max-width: 420px;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
}

.qc-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border);
}

.qc-modal-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.qc-modal-close {
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

.qc-modal-close:hover {
  background: var(--error-light);
  color: var(--error);
  border-color: var(--error);
}

.qc-modal-body {
  padding: var(--spacing-lg);
}

.qc-field {
  margin-bottom: var(--spacing-md);
}

.qc-field:last-child {
  margin-bottom: 0;
}

.qc-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.qc-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 0.875rem;
  color: var(--text-primary);
  background: var(--surface-solid);
  font-family: inherit;
  box-sizing: border-box;
  transition: all var(--transition);
}

.qc-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.qc-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--text-primary);
  background: var(--surface-solid);
  font-family: inherit;
  resize: vertical;
  min-height: 60px;
  box-sizing: border-box;
  transition: all var(--transition);
}

.qc-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.qc-field-char {
  text-align: right;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 4px;
}

.qc-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--border);
}

.qc-btn {
  padding: 7px 18px;
  border-radius: var(--radius);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition);
  border: 1px solid var(--border);
}

.qc-btn--ghost {
  background: transparent;
  color: var(--text-secondary);
}

.qc-btn--ghost:hover {
  background: var(--gray-50);
}

.qc-btn--primary {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.qc-btn--primary:hover:not(:disabled) {
  background: var(--primary-hover);
}

.qc-btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ─── Modal Transition ─── */
.qc-modal-fade-enter-active,
.qc-modal-fade-leave-active {
  transition: opacity 0.25s var(--ease-out);
}

.qc-modal-fade-enter-active .qc-modal,
.qc-modal-fade-leave-active .qc-modal {
  transition: transform 0.25s var(--ease-out);
}

.qc-modal-fade-enter-from,
.qc-modal-fade-leave-to {
  opacity: 0;
}

.qc-modal-fade-enter-from .qc-modal,
.qc-modal-fade-leave-to .qc-modal {
  transform: scale(0.96) translateY(8px);
}

/* ─── Responsive ─── */
@media (max-width: 480px) {
  .quick-composer {
    padding: var(--spacing-sm);
  }

  .composer-avatar {
    width: 36px;
    height: 36px;
  }

  .composer-selected-topics,
  .composer-topic-wrap,
  .composer-location {
    padding-left: 44px;
  }

  .submit-btn {
    padding: 6px 16px;
    font-size: 0.8125rem;
  }
}
</style>
