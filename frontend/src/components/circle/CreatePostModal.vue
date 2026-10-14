<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { circleApi } from '../../api/circle'
import { topicApi } from '../../api/topic'
import { mediaApi } from '../../api/media'
import { userApi } from '../../api/user'
import { postApi } from '../../api/post'
import { useUserStore } from '../../stores/user'
import { debounce } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  close: []
  published: []
}>()

const userStore = useUserStore()
const logger = useLogger('CreatePostModal')

const publishing = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const showVisibilityPicker = ref(false)

const textareaRef = ref<HTMLTextAreaElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const videoInput = ref<HTMLInputElement | null>(null)

const visibilityOptions: Record<number, string> = { 0: '公开', 1: '关注者可见', 2: '仅自己' }

const newPost = reactive({
  content: '',
  images: [] as string[],
  videos: [] as string[],
  visibility: 0,
  location: ''
})

const showLocationInput = ref(false)

// @mention
const showMentionDropdown = ref(false)
const mentionKeyword = ref('')
const mentionSearchTimer = ref<any>(null)
const mentionLoading = ref(false)
const mentionResults = ref<any[]>([])

const handleTextareaInput = () => {
  autoResize()
  const textarea = textareaRef.value
  if (!textarea) return
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = newPost.content.substring(0, cursorPos)
  const match = textBeforeCursor.match(/@(\w*)$/)
  if (match) {
    const keyword: string = match[1] || ''
    mentionKeyword.value = keyword
    showMentionDropdown.value = true
    if (mentionSearchTimer.value) clearTimeout(mentionSearchTimer.value)
    if (keyword.length > 0) {
      mentionSearchTimer.value = setTimeout(() => searchMentionUsers(keyword), 300)
    } else {
      mentionResults.value = []
    }
  } else {
    showMentionDropdown.value = false
    mentionResults.value = []
  }
}

const searchMentionUsers = async (keyword: string) => {
  if (!keyword.trim()) { mentionResults.value = []; return }
  mentionLoading.value = true
  try {
    const res = await userApi.searchUsers({ keyword, pageNum: 1, pageSize: 10 })
    const data = res.data as any
    mentionResults.value = Array.isArray(data) ? data : (data?.records || [])
  } catch { mentionResults.value = [] }
  finally { mentionLoading.value = false }
}

const selectMention = (user: any) => {
  const textarea = textareaRef.value
  if (!textarea) return
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = newPost.content.substring(0, cursorPos)
  const textAfterCursor = newPost.content.substring(cursorPos)
  const matchPos = textBeforeCursor.lastIndexOf('@')
  if (matchPos === -1) return
  const newTextBefore = textBeforeCursor.substring(0, matchPos) + `@${user.nickname || user.username} `
  newPost.content = newTextBefore + textAfterCursor
  showMentionDropdown.value = false
  mentionResults.value = []
  nextTick(() => {
    textarea.focus()
    const newPos = newTextBefore.length
    textarea.setSelectionRange(newPos, newPos)
    autoResize()
  })
}

// Topics
const topicSearch = ref('')
const showTopicDropdown = ref(false)
const selectedTopics = ref<any[]>([])
const allTopics = ref<any[]>([])
const topicsLoading = ref(false)

const showCreateTopicModal = ref(false)
const creatingTopic = ref(false)
const newTopic = reactive({
  name: '',
  description: ''
})

const debouncedSearchTopics = debounce(() => {
  showTopicDropdown.value = true
}, 300)
void debouncedSearchTopics

const filteredTopics = computed(() => {
  if (!topicSearch.value) return allTopics.value
  const q = topicSearch.value.toLowerCase()
  return allTopics.value.filter(t => t.name.toLowerCase().includes(q))
})

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
    const createdTopic = {
      id: topicId,
      name: newTopic.name.trim(),
      description: newTopic.description.trim(),
      postCount: 0
    }
    allTopics.value.unshift(createdTopic)
    if (!selectedTopics.value.find(t => t.id === createdTopic.id)) {
      selectedTopics.value.push(createdTopic)
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

const autoResize = () => {
  const el = textareaRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = el.scrollHeight + 'px'
  }
}

const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const MAX_VIDEO_SIZE = 100 * 1024 * 1024

const handleImageUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = Array.from(target.files || [])
  uploading.value = true
  uploadPercent.value = 0
  let uploadedCount = 0
  try {
    for (const file of files) {
      if (file.size > MAX_IMAGE_SIZE) {
        toast.warning(`图片 ${file.name} 超过10MB限制`)
        continue
      }
      if (newPost.images.length >= 9) break
      const res = await mediaApi.uploadFile(file, 'circle', (pe) => {
        if (pe.total) uploadPercent.value = Math.round((pe.loaded / pe.total) * 100)
      })
      newPost.images.push(res.data.fileUrl)
      uploadedCount++
    }
    if (uploadedCount > 0) toast.success('上传完成')
  } catch (err: any) {
    logger.error('upload image error', { error: err.message })
    toast.error(err.response?.data?.message || '图片上传失败')
  } finally {
    uploading.value = false
    uploadPercent.value = 0
    if (imageInput.value) imageInput.value.value = ''
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
    newPost.videos.push(res.data.fileUrl)
    toast.success('视频上传成功')
  } catch (err: any) {
    logger.error('upload video error', { error: err.message })
    toast.error(err.response?.data?.message || '视频上传失败')
  } finally {
    uploading.value = false
    uploadPercent.value = 0
    if (videoInput.value) videoInput.value.value = ''
  }
}

const publishPost = async () => {
  if (!newPost.content.trim()) return
  publishing.value = true
  try {
    await circleApi.createPost({
      content: newPost.content,
      images: newPost.images,
      videos: newPost.videos,
      visibility: newPost.visibility,
      location: newPost.location || undefined,
      topicIds: selectedTopics.value.length > 0 ? selectedTopics.value.map(t => t.id) : null,
      allowComment: 1,
      allowRepost: 1
    })
    // Clear circle draft
    try {
      const draftRes = await postApi.getLatestDraft()
      const draft = draftRes.data
      const draftId = draft?.id || draft?.draftId
      if (draftId && draft?.title === '[校友圈]') {
        await postApi.deleteDraft(draftId)
      }
    } catch { /* ignore */ }
    toast.success('发布成功')
    resetForm()
    emit('published')
  } catch (err: any) {
    logger.error('publish error', { error: err.message })
    toast.error(err.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

const resetForm = () => {
  newPost.content = ''
  newPost.images = []
  newPost.videos = []
  newPost.visibility = 0
  newPost.location = ''
  showLocationInput.value = false
  selectedTopics.value = []
}

// Draft
const saveCircleDraft = async () => {
  if (!newPost.content.trim() && newPost.images.length === 0) return
  try {
    await postApi.saveDraft({
      title: '[校友圈]',
      content: newPost.content,
      coverImage: newPost.images[0] || undefined,
      tagNames: selectedTopics.value.map(t => t.name)
    } as any)
  } catch { /* ignore */ }
}

const loadCircleDraft = async () => {
  try {
    const res = await postApi.getLatestDraft()
    if (res.data && res.data.title === '[校友圈]') {
      newPost.content = res.data.content || ''
      if (res.data.coverImage) {
        newPost.images = [res.data.coverImage]
      }
      if (res.data.tags && res.data.tags.length > 0) {
        selectedTopics.value = res.data.tags.map((t: any) => ({ id: t.id, name: t.name }))
      }
    }
  } catch { /* ignore */ }
}

const closeModal = async () => {
  await saveCircleDraft()
  showCreateTopicModal.value = false
  showVisibilityPicker.value = false
  showTopicDropdown.value = false
  showMentionDropdown.value = false
  showLocationInput.value = false
  emit('close')
}

const loadTopics = () => {
  if (allTopics.value.length === 0) {
    topicsLoading.value = true
    topicApi.getTopicList({ pageNum: 1, pageSize: 100 })
      .then(res => {
        const data = res.data
        allTopics.value = Array.isArray(data) ? data : (data?.records || [])
      })
      .catch(() => {})
      .finally(() => {
        topicsLoading.value = false
      })
  }
}

const visibilityIcon = (v: number) => {
  if (v === 0) return 'globe'
  if (v === 1) return 'users'
  return 'lock'
}

watch(() => props.show, (val) => {
  if (val) {
    nextTick(() => autoResize())
    loadCircleDraft()
    loadTopics()
  }
})
</script>

<template>
  <teleport to="body">
    <transition name="cpm-fade">
      <div v-if="show" class="cpm-overlay" @click.self="closeModal">
        <div class="cpm-modal">
          <div class="cpm-header">
            <h3>发布动态</h3>
            <button class="cpm-close" @click="closeModal">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <div class="cpm-body">
            <!-- Author Row -->
            <div class="cpm-author">
              <img :src="userStore.avatar || '/default-avatar.png'" class="cpm-avatar" />
              <div class="cpm-author-meta">
                <span class="cpm-nickname">{{ userStore.nickname }}</span>
                <div class="cpm-vis-wrap">
                  <button class="cpm-vis-btn" @click="showVisibilityPicker = !showVisibilityPicker">
                    <svg v-if="visibilityIcon(newPost.visibility) === 'globe'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                    <svg v-else-if="visibilityIcon(newPost.visibility) === 'users'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6"/><path d="M23 11h-6"/></svg>
                    <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    {{ visibilityOptions[newPost.visibility] }}
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
                  </button>
                  <transition name="cpm-drop">
                    <div v-if="showVisibilityPicker" class="cpm-vis-dropdown">
                      <div v-for="(label, key) in visibilityOptions" :key="key" class="cpm-vis-option" :class="{ 'is-active': newPost.visibility === Number(key) }" @click="newPost.visibility = Number(key); showVisibilityPicker = false">
                        <svg v-if="Number(key) === 0" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                        <svg v-else-if="Number(key) === 1" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6"/><path d="M23 11h-6"/></svg>
                        <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                        <span>{{ label }}</span>
                      </div>
                    </div>
                  </transition>
                </div>
              </div>
            </div>

            <!-- Textarea -->
            <div class="cpm-textarea-wrap">
              <textarea
                v-model="newPost.content"
                class="cpm-textarea"
                placeholder="分享你的校园生活... 使用 @ 提及用户"
                rows="4"
                @input="handleTextareaInput"
                ref="textareaRef"
                maxlength="2000"
              ></textarea>
              <transition name="cpm-drop">
                <div v-if="showMentionDropdown" class="cpm-mention-dropdown">
                  <div v-if="mentionLoading" class="cpm-dropdown-status"><span class="cpm-spinner"></span> 搜索中...</div>
                  <div v-else-if="mentionResults.length">
                    <div v-for="user in mentionResults" :key="user.id" class="cpm-mention-item" @mousedown.prevent="selectMention(user)">
                      <img :src="user.avatar || '/default-avatar.png'" class="cpm-mention-avatar" />
                      <span class="cpm-mention-name">{{ user.nickname || user.username }}</span>
                    </div>
                  </div>
                  <div v-else class="cpm-dropdown-status">未找到用户</div>
                </div>
              </transition>
            </div>
            <div class="cpm-char" :class="{ 'is-warn': newPost.content.length > 1800 }">{{ newPost.content.length }}/2000</div>

            <!-- Topics -->
            <div class="cpm-topics">
              <div v-if="selectedTopics.length > 0" class="cpm-selected-topics">
                <span v-for="topic in selectedTopics" :key="topic.id" class="cpm-topic-badge">
                  #{{ topic.name }}
                  <button class="cpm-topic-remove" @click="removeTopic(topic)">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </button>
                </span>
              </div>
              <div class="cpm-topic-input-wrap">
                <div class="cpm-topic-input">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  <input v-model="topicSearch" placeholder="添加话题..." @focus="showTopicDropdown = true" @blur="hideTopicDropdown" />
                </div>
                <transition name="cpm-drop">
                  <div v-if="showTopicDropdown" class="cpm-topic-dropdown">
                    <div v-if="topicsLoading" class="cpm-dropdown-status"><span class="cpm-spinner"></span> 加载中...</div>
                    <div v-else-if="filteredTopics.length">
                      <div v-for="topic in filteredTopics" :key="topic.id" class="cpm-topic-option" @mousedown.prevent="selectTopic(topic)">
                        <span class="cpm-topic-option-name">#{{ topic.name }}</span>
                        <span class="cpm-topic-option-count">{{ topic.postCount || 0 }} 篇</span>
                      </div>
                      <div class="cpm-dropdown-divider"></div>
                      <div class="cpm-topic-option cpm-topic-create" @mousedown.prevent="openCreateTopicModal">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                        <span>创建话题 "{{ topicSearch || '新话题' }}"</span>
                      </div>
                    </div>
                    <div v-else class="cpm-topic-create" @mousedown.prevent="openCreateTopicModal">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                      <span>创建话题 "{{ topicSearch || '新话题' }}"</span>
                    </div>
                  </div>
                </transition>
              </div>
            </div>

            <!-- Location -->
            <div class="cpm-location">
              <button class="cpm-loc-btn" @click="showLocationInput = !showLocationInput">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                {{ newPost.location || '添加位置' }}
              </button>
              <transition name="cpm-drop">
                <div v-if="showLocationInput" class="cpm-loc-input">
                  <input v-model="newPost.location" placeholder="输入位置信息..." maxlength="100" />
                  <button v-if="newPost.location" class="cpm-loc-clear" @click="newPost.location = ''; showLocationInput = false">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </button>
                </div>
              </transition>
            </div>

            <!-- Uploaded Images -->
            <div v-if="newPost.images.length" class="cpm-images">
              <div v-for="(img, idx) in newPost.images" :key="idx" class="cpm-image-item">
                <img :src="img" alt="" />
                <button class="cpm-image-remove" @click="newPost.images.splice(idx, 1)">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
            </div>

            <!-- Uploaded Videos -->
            <div v-if="newPost.videos.length" class="cpm-videos">
              <div v-for="(video, idx) in newPost.videos" :key="'v-'+idx" class="cpm-video-item">
                <video :src="video" class="cpm-video-preview" muted preload="metadata"></video>
                <button class="cpm-image-remove" @click="newPost.videos.splice(idx, 1)">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
            </div>

            <!-- Upload Progress -->
            <div v-if="uploading" class="cpm-progress">
              <div class="cpm-progress-bar" :style="{ width: uploadPercent + '%' }"></div>
            </div>
          </div>

          <!-- Toolbar & Footer -->
          <div class="cpm-footer">
            <div class="cpm-tools">
              <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple @change="handleImageUpload" ref="imageInput" hidden />
              <input type="file" accept="video/mp4,video/webm" @change="handleVideoUpload" ref="videoInput" hidden />
              <button class="cpm-tool" title="图片" @click="(imageInput as HTMLInputElement)?.click()" :disabled="newPost.images.length >= 9">
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
              </button>
              <button class="cpm-tool" title="视频" @click="(videoInput as HTMLInputElement)?.click()">
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2"/></svg>
              </button>
            </div>
            <button class="btn btn-primary btn-sm" @click="publishPost" :disabled="!newPost.content.trim() || publishing">
              {{ publishing ? '发布中...' : '发布' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>

  <!-- Create Topic Modal -->
  <teleport to="body">
    <transition name="cpm-fade">
      <div v-if="showCreateTopicModal" class="cpm-overlay" @click.self="showCreateTopicModal = false">
        <div class="cpm-modal cpm-modal--sm">
          <div class="cpm-header">
            <h3>创建新话题</h3>
            <button class="cpm-close" @click="showCreateTopicModal = false">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="cpm-body">
            <div class="cpm-field">
              <label class="cpm-label">话题名称</label>
              <input v-model="newTopic.name" class="cpm-input" placeholder="请输入话题名称" maxlength="20" />
              <div class="cpm-char">{{ newTopic.name.length }}/20</div>
            </div>
            <div class="cpm-field">
              <label class="cpm-label">话题描述（可选）</label>
              <textarea v-model="newTopic.description" class="cpm-textarea-sm" placeholder="简单描述一下这个话题..." rows="3" maxlength="200"></textarea>
              <div class="cpm-char">{{ newTopic.description.length }}/200</div>
            </div>
          </div>
          <div class="cpm-footer cpm-footer--end">
            <button class="btn btn-ghost" @click="showCreateTopicModal = false">取消</button>
            <button class="btn btn-primary" @click="confirmCreateTopic" :disabled="!newTopic.name.trim() || creatingTopic">
              {{ creatingTopic ? '创建中...' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.cpm-overlay {
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

.cpm-modal {
  width: 100%;
  max-width: 520px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
}

.cpm-modal--sm {
  max-width: 440px;
}

.cpm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border);
}

.cpm-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.cpm-close {
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

.cpm-close:hover {
  background: var(--error-light);
  color: var(--error);
  border-color: var(--error);
}

.cpm-body {
  padding: var(--spacing-lg);
  overflow-y: auto;
  flex: 1;
}

.cpm-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--border);
}

.cpm-footer--end {
  justify-content: flex-end;
}

/* Author */
.cpm-author {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.cpm-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
}

.cpm-author-meta {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cpm-nickname {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.cpm-vis-wrap {
  position: relative;
}

.cpm-vis-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition);
}

.cpm-vis-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.cpm-vis-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: var(--spacing-0_5);
  z-index: 100;
  min-width: 180px;
}

.cpm-vis-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 6px 10px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all var(--transition);
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.cpm-vis-option:hover,
.cpm-vis-option.is-active {
  background: var(--primary-subtle);
  color: var(--primary);
}

/* Textarea */
.cpm-textarea-wrap {
  position: relative;
}

.cpm-textarea {
  width: 100%;
  border: none;
  border-radius: 0;
  padding: 0;
  font-size: 0.9375rem;
  line-height: 1.7;
  resize: none;
  color: var(--text-primary);
  background: transparent;
  font-family: inherit;
  min-height: 80px;
  box-sizing: border-box;
}

.cpm-textarea:focus {
  outline: none;
}

.cpm-textarea::placeholder {
  color: var(--text-muted);
}

.cpm-char {
  text-align: right;
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: var(--spacing-0_5);
}

.cpm-char.is-warn {
  color: var(--warning);
}

/* Mention Dropdown */
.cpm-mention-dropdown {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  max-height: 180px;
  overflow-y: auto;
  z-index: 100;
}

.cpm-mention-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 6px 10px;
  cursor: pointer;
  transition: background var(--transition);
}

.cpm-mention-item:hover {
  background: var(--primary-subtle);
}

.cpm-mention-avatar {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.cpm-mention-name {
  font-size: 0.8125rem;
  color: var(--text-primary);
}

.cpm-dropdown-status {
  padding: var(--spacing-md);
  text-align: center;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.cpm-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: var(--radius-full);
  animation: cpmSpin 0.6s linear infinite;
  vertical-align: middle;
  margin-right: 4px;
}

@keyframes cpmSpin {
  to { transform: rotate(360deg); }
}

/* Topics */
.cpm-topics {
  margin-top: var(--spacing-md);
}

.cpm-selected-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: var(--spacing-sm);
}

.cpm-topic-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: var(--primary-subtle);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--primary);
}

.cpm-topic-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: rgba(0, 0, 0, 0.08);
  border: none;
  border-radius: var(--radius-full);
  color: var(--primary);
  cursor: pointer;
  transition: all var(--transition);
}

.cpm-topic-remove:hover {
  background: var(--error);
  color: #fff;
}

.cpm-topic-input-wrap {
  position: relative;
}

.cpm-topic-input {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: all var(--transition);
}

.cpm-topic-input:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.cpm-topic-input svg {
  color: var(--text-muted);
  flex-shrink: 0;
}

.cpm-topic-input input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  outline: none;
  color: var(--text-primary);
  padding: 0;
}

.cpm-topic-input input::placeholder {
  color: var(--text-muted);
}

.cpm-topic-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--surface-solid);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  max-height: 180px;
  overflow-y: auto;
  z-index: 100;
}

.cpm-topic-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  cursor: pointer;
  transition: background var(--transition);
}

.cpm-topic-option:hover {
  background: var(--primary-subtle);
}

.cpm-topic-option-name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--primary);
}

.cpm-topic-option-count {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.cpm-dropdown-divider {
  height: 1px;
  background: var(--border);
  margin: var(--spacing-0_5) 0;
}

.cpm-topic-create {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  transition: background var(--transition);
  color: var(--primary);
  font-size: 0.8125rem;
  font-weight: 500;
}

.cpm-topic-create:hover {
  background: var(--primary-subtle);
}

/* Location */
.cpm-location {
  margin-top: var(--spacing-sm);
}

.cpm-loc-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition);
}

.cpm-loc-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.cpm-loc-input {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  margin-top: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: all var(--transition);
}

.cpm-loc-input:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.cpm-loc-input input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  outline: none;
  color: var(--text-primary);
  padding: 0;
}

.cpm-loc-input input::placeholder {
  color: var(--text-muted);
}

.cpm-loc-clear {
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

.cpm-loc-clear:hover {
  color: var(--error);
}

/* Uploaded Images */
.cpm-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

.cpm-image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--bg-secondary);
}

.cpm-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cpm-image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
}

.cpm-image-remove:hover {
  background: var(--error);
  transform: scale(1.1);
}

/* Uploaded Videos */
.cpm-videos {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

.cpm-video-item {
  position: relative;
  border-radius: var(--radius);
  overflow: hidden;
  background: #000;
  max-width: 100%;
}

.cpm-video-preview {
  width: 100%;
  max-height: 180px;
  object-fit: contain;
  display: block;
}

/* Progress */
.cpm-progress {
  margin-top: var(--spacing-sm);
  height: 3px;
  background: var(--bg-secondary);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.cpm-progress-bar {
  height: 100%;
  background: var(--primary);
  border-radius: var(--radius-full);
  transition: width var(--transition-slow);
}

/* Tools */
.cpm-tools {
  display: flex;
  gap: var(--spacing-0_5);
}

.cpm-tool {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition);
}

.cpm-tool:hover:not(:disabled) {
  background: var(--primary-subtle);
  border-color: var(--primary);
  color: var(--primary);
}

.cpm-tool:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* Form Fields (Create Topic Modal) */
.cpm-field {
  margin-bottom: var(--spacing-md);
}

.cpm-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.cpm-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface-solid);
  color: var(--text-primary);
  font-size: 0.9375rem;
  transition: all var(--transition);
  box-sizing: border-box;
}

.cpm-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.cpm-textarea-sm {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface-solid);
  color: var(--text-primary);
  font-size: 0.9375rem;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  transition: all var(--transition);
  box-sizing: border-box;
}

.cpm-textarea-sm:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

/* Transitions */
.cpm-fade-enter-active,
.cpm-fade-leave-active {
  transition: opacity 0.25s var(--ease-out);
}

.cpm-fade-enter-active .cpm-modal,
.cpm-fade-leave-active .cpm-modal {
  transition: transform 0.25s var(--ease-out);
}

.cpm-fade-enter-from,
.cpm-fade-leave-to {
  opacity: 0;
}

.cpm-fade-enter-from .cpm-modal,
.cpm-fade-leave-to .cpm-modal {
  transform: scale(0.96) translateY(8px);
}

.cpm-drop-enter-active,
.cpm-drop-leave-active {
  transition: all 0.2s var(--ease-out);
}

.cpm-drop-enter-from,
.cpm-drop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 640px) {
  .cpm-modal {
    max-height: 95vh;
    margin: var(--spacing-sm);
    border-radius: var(--radius-lg);
  }

  .cpm-body {
    padding: var(--spacing-md);
  }
}
</style>
