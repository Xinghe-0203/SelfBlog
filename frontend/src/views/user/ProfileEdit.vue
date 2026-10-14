<template>
  <div class="profile-edit-page">
    <div class="edit-hero card">
      <div class="edit-cover">
        <img :src="getSafeImageUrl(coverPreview, '/default-cover.jpg')" alt="" />
        <div class="cover-gradient"></div>
        <button class="cover-upload-btn" title="更换封面图" @click="triggerCoverUpload" :disabled="uploading">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
            <circle cx="12" cy="13" r="4"/>
          </svg>
        </button>
        <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" ref="coverInputRef" hidden @change="handleCoverUpload" />
      </div>
      <div class="edit-avatar-section">
        <div class="avatar-upload" @click="triggerAvatarUpload">
          <div class="avatar-preview-wrapper">
            <img :src="getSafeImageUrl(form.avatar, '/default-avatar.png')" alt="头像" class="avatar-preview" />
            <div class="avatar-overlay">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
              <span>{{ uploading ? '上传中...' : '更换头像' }}</span>
            </div>
          </div>
          <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="handleAvatarUpload" ref="avatarInput" hidden />
        </div>
        <p class="upload-tip">支持 jpg、png、gif、webp，最大 10MB</p>
      </div>
    </div>

    <div class="edit-card card">
      <div class="edit-header">
        <h2>编辑个人资料</h2>
        <router-link to="/profile" class="btn btn-ghost btn-sm">返回主页</router-link>
      </div>

      <form class="edit-form" @submit.prevent="handleSubmit">
        <div class="form-group float-label-group">
          <input v-model="form.nickname" type="text" class="form-input" :class="{ error: errors.nickname }" placeholder=" " maxlength="20" @input="validateNickname" />
          <label class="float-label">昵称</label>
          <span class="char-count-top" :class="{ warn: (form.nickname || '').length >= 18 }">{{ (form.nickname || '').length }}/20</span>
          <span v-if="errors.nickname" class="form-error">{{ errors.nickname }}</span>
        </div>

        <div class="form-group float-label-group">
          <textarea v-model="form.bio" class="form-input" :class="{ error: errors.bio }" placeholder=" " rows="4" maxlength="200" @input="validateBio"></textarea>
          <label class="float-label">个人简介</label>
          <span class="char-count-bottom" :class="{ warn: (form.bio || '').length >= 180 }">{{ (form.bio || '').length }}/200</span>
          <span v-if="errors.bio" class="form-error">{{ errors.bio }}</span>
        </div>

        <div class="form-group float-label-group">
          <input :value="userStore.user?.email" type="email" class="form-input" disabled placeholder=" " />
          <label class="float-label">邮箱</label>
          <span class="form-hint">邮箱不可修改</span>
        </div>

        <div class="form-actions">
          <router-link to="/profile" class="btn btn-ghost">取消</router-link>
          <button type="submit" class="btn btn-primary" :disabled="loading || uploading">
            <template v-if="loading">
              <span class="btn-spinner"></span>
              保存中...
            </template>
            <template v-else>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
              保存修改
            </template>
          </button>
        </div>
      </form>
    </div>
  </div>
  <ConfirmDialog />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { userApi } from '../../api/user'
import { mediaApi } from '../../api/media'
import { useLogger } from '../../utils/logger'
import { getSafeImageUrl } from '../../utils'
import { toast } from '../../utils/toast'
import { useConfirm } from '../../composables/useConfirm'

const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('ProfileEdit')
const { confirm, ConfirmDialog } = useConfirm()

const loading = ref(false)
const uploading = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const coverInputRef = ref<HTMLInputElement | null>(null)
const coverPreview = ref('')
const dirty = ref(false)

const form = reactive({
  nickname: '',
  bio: '',
  avatar: ''
})

const errors = reactive({
  nickname: '',
  bio: ''
})

watch([() => form.nickname, () => form.bio], () => {
  dirty.value = true
})

onBeforeRouteLeave(async (_to: any, _from: any) => {
  if (dirty.value) {
    const ok = await confirm('有未保存的修改，确定离开吗？')
    if (!ok) return false
  }
})

function triggerAvatarUpload() {
  if (!uploading.value) {
    avatarInput.value?.click()
  }
}

function triggerCoverUpload() {
  if (!uploading.value) {
    coverInputRef.value?.click()
  }
}

function validateNickname() {
  if (!form.nickname.trim()) {
    errors.nickname = '昵称不能为空'
  } else if (form.nickname.trim().length < 2) {
    errors.nickname = '昵称至少2个字符'
  } else {
    errors.nickname = ''
  }
}

function validateBio() {
  if (form.bio.length > 200) {
    errors.bio = '个人简介不能超过200个字符'
  } else {
    errors.bio = ''
  }
}

onMounted(() => {
  if (userStore.user) {
    form.nickname = userStore.user.nickname || ''
    form.bio = userStore.user.bio || ''
    form.avatar = userStore.user.avatar || ''
    coverPreview.value = userStore.user.coverImage || ''
  }
})

async function handleAvatarUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const response = await mediaApi.uploadFile(file, 'avatar')
    form.avatar = (response.data as any).fileUrl
    toast.success('头像上传成功')
  } catch (error: any) {
    logger.error('Failed to upload avatar', { error: error.message })
    toast.error(error.response?.data?.message || '上传头像失败，请重试')
  } finally {
    uploading.value = false
  }
}

async function handleCoverUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const response = await mediaApi.uploadFile(file, 'cover')
    const uploadData = response.data as any
    coverPreview.value = uploadData.fileUrl
    await userApi.updateCoverImage(uploadData.fileUrl)
    if (userStore.user) userStore.user.coverImage = uploadData.fileUrl
    toast.success('封面图更新成功')
  } catch (error: any) {
    logger.error('Failed to upload cover', { error: error.message })
    toast.error(error.response?.data?.message || '上传封面图失败')
  } finally {
    uploading.value = false
  }
}

async function handleSubmit() {
  validateNickname()
  if (errors.nickname) {
    toast.warning('请修正错误后重试')
    return
  }

  loading.value = true
  try {
    await userStore.updateProfile({
      nickname: form.nickname,
      bio: form.bio
    })

    if (form.avatar && form.avatar !== (userStore.user?.avatar || '')) {
      await userApi.updateAvatar(form.avatar)
      userStore.updateAvatar(form.avatar)
    }

    toast.success('保存成功')
    dirty.value = false
    router.push('/profile')
  } catch (error: any) {
    logger.error('Failed to update profile', { error: error.message })
    toast.error(error.response?.data?.message || '保存失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-edit-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.edit-hero {
  overflow: visible;
  border-radius: var(--radius-xl);
}

.edit-cover {
  position: relative;
  height: 160px;
  overflow: hidden;
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}

.edit-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0,0,0,0.5));
}

.cover-upload-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all var(--transition);
  z-index: 2;
}

.edit-cover:hover .cover-upload-btn {
  opacity: 1;
}

.cover-upload-btn:hover {
  background: rgba(0,0,0,0.7);
  transform: scale(1.05);
}

.cover-upload-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.edit-avatar-section {
  padding: 0 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-upload {
  margin-top: -48px;
  cursor: pointer;
  position: relative;
  z-index: 2;
}

.avatar-preview-wrapper {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: var(--radius-full);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
  border: 4px solid var(--surface-solid);
  transition: transform var(--transition);
}

.avatar-preview-wrapper:hover {
  transform: scale(1.05);
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: white;
  opacity: 0;
  transition: opacity var(--transition);
}

.avatar-preview-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay svg {
  width: 22px;
  height: 22px;
}

.avatar-overlay span {
  font-size: 0.6875rem;
  font-weight: 500;
}

.upload-tip {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-align: center;
  margin: 8px 0 0;
}

.edit-card {
  padding: 28px;
  border-radius: var(--radius-xl);
}

.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.edit-header h2 {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.float-label-group {
  position: relative;
  margin-bottom: 20px;
}

.float-label-group .form-input {
  padding: 20px 14px 6px;
  border-radius: var(--radius);
}

.float-label-group textarea.form-input {
  padding: 22px 14px 10px;
  min-height: 100px;
}

.float-label {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.875rem;
  color: var(--text-muted);
  pointer-events: none;
  transition: all var(--transition);
  background: var(--surface);
  padding: 0 4px;
}

.float-label-group textarea ~ .float-label {
  top: 22px;
  transform: none;
}

.float-label-group .form-input:focus ~ .float-label,
.float-label-group .form-input:not(:placeholder-shown) ~ .float-label,
.float-label-group .form-input:disabled ~ .float-label {
  top: 6px;
  transform: none;
  font-size: 0.6875rem;
  color: var(--primary);
}

.float-label-group .form-input:disabled ~ .float-label {
  color: var(--text-muted);
}

.float-label-group .form-input.error ~ .float-label {
  color: var(--error);
}

.char-count-top {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.6875rem;
  color: var(--text-muted);
  pointer-events: none;
  transition: color var(--transition);
}

.char-count-top.warn {
  color: var(--warning);
}

.char-count-bottom {
  display: block;
  text-align: right;
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 4px;
  transition: color var(--transition);
}

.char-count-bottom.warn {
  color: var(--warning);
}

.form-hint {
  display: block;
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 4px;
}

.form-error {
  display: block;
  font-size: 0.6875rem;
  color: var(--error);
  margin-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--glass-border);
}

.btn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: var(--radius-full);
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .profile-edit-page {
    padding: 16px;
    gap: 16px;
  }

  .edit-card {
    padding: 20px;
  }

  .edit-cover {
    height: 120px;
  }
}
</style>
