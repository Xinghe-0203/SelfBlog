<template>
  <div class="albums-page">
    <header class="page-heading">
      <div>
        <h1>相册管理</h1>
        <p>创建公开或密码相册，上传照片并维护顺序。</p>
      </div>
      <button class="btn btn-primary" type="button" @click="newAlbum">
        <Icon name="plus" :size="16" />新建相册
      </button>
    </header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div v-if="loading" class="empty">正在加载相册…</div>
    <div v-else-if="!albums.length" class="empty">还没有相册，创建后即可在前台相册页展示。</div>
    <section v-else class="album-list">
      <article v-for="album in albums" :key="album.id" class="album-card">
        <header class="album-heading">
          <div>
            <h2>
              {{ album.title }}
              <span v-if="album.privateAlbum" class="private-label">密码保护</span>
            </h2>
            <p>{{ album.description || '暂无描述' }}</p>
          </div>
          <div class="album-actions">
            <button
              type="button"
              :disabled="altBusy || !album.photos?.length"
              data-action="ai-album-description"
              :title="album.photos?.length ? '用第一张照片生成相册简介' : '先上传照片才能生成描述'"
              @click="generateAlbumDescription(album)"
            >
              <Icon name="sparkle" :size="14" />AI 生成描述</button
            ><button type="button" @click="editAlbum(album)">编辑</button
            ><button class="danger" type="button" @click="removeAlbum(album)">删除</button>
          </div>
        </header>
        <div class="photo-strip">
          <figure v-for="photo in album.photos || []" :key="photo.id">
            <img
              :src="photo.imageUrl"
              :alt="altDrafts[photo.id]?.caption || photo.caption || album.title"
            /><button
              type="button"
              :aria-label="`删除照片 ${photo.id}`"
              @click="removePhoto(album, photo.id)"
            >
              <Icon name="close" :size="13" /></button
            ><button
              class="alt-tile"
              type="button"
              :aria-label="`AI 生成描述 ${photo.id}`"
              data-action="ai-photo-description"
              :disabled="altBusy"
              @click="generatePhotoDescription(album, photo.id)"
            >
              <Icon name="sparkle" :size="13" />
            </button>
          </figure>
          <label class="upload-tile"
            ><input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              multiple
              @change="uploadPhotos(album, $event)"
            /><Icon name="plus" :size="20" /><small>上传照片</small></label
          >
        </div>
        <div v-if="draftsOf(album).length" class="alt-drafts">
          <div
            v-for="draft in draftsOf(album)"
            :key="draft.photoId"
            class="alt-draft"
            :data-alt-photo="draft.photoId"
          >
            <div class="alt-draft-text">
              <p>{{ draft.caption || draft.alt }}</p>
              <small>替代文本：{{ draft.alt }}</small>
            </div>
            <div class="alt-draft-actions">
              <button
                type="button"
                data-action="save-alt-caption"
                :disabled="altBusy"
                @click="saveDraftAsCaption(album, draft.photoId)"
              >
                保存为照片图注</button
              ><button
                type="button"
                data-action="apply-alt-caption"
                @click="applyDraftToDescription(album, draft.photoId)"
              >
                应用到相册简介</button
              ><button type="button" @click="dismissDraft(draft.photoId)">忽略</button>
            </div>
          </div>
        </div>
        <footer>
          <span>{{ album.photoCount }} 张照片</span
          ><span>{{ album.privateAlbum ? '仅输入密码后可见' : '公开展示' }}</span>
        </footer>
      </article>
    </section>

    <Modal
      :show="editing"
      :title="currentId ? '编辑相册' : '新建相册'"
      width="480px"
      :closable="true"
      @close="editing = false"
    >
      <form id="album-form" class="album-dialog" @submit.prevent="saveAlbum">
        <label>相册名称<input v-model.trim="form.title" maxlength="160" required /></label>
        <label
          >相册简介<textarea v-model.trim="form.description" rows="3" maxlength="1000"></textarea>
        </label>
        <label class="check-row"
          ><input v-model="form.privateAlbum" type="checkbox" />设为密码相册</label
        >
        <label v-if="form.privateAlbum"
          >访问密码<input
            v-model="form.password"
            type="password"
            :required="!currentId"
            minlength="6"
            autocomplete="new-password"
            :placeholder="currentId ? '留空表示不修改密码' : '至少 6 位'"
        /></label>
        <label>排序值<input v-model.number="form.sortOrder" type="number" /></label>
        <p v-if="dialogError" class="dialog-error" role="alert">{{ dialogError }}</p>
      </form>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="editing = false">取消</button>
        <button class="btn btn-primary" type="submit" form="album-form" :disabled="saving">
          {{ saving ? '保存中…' : '保存相册' }}
        </button>
      </template>
    </Modal>
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { galleryApi, type AlbumForm, type PhotoAlbum } from '@/api/gallery'
import { llmApi } from '@/api/llm'
import type { ImageAltVO } from '@/types/llm'
import { toast } from '@/utils/toast'
import { useConfirm } from '@/composables/useConfirm'
import Modal from '@/components/common/Modal.vue'
import Icon from '@/components/common/Icon.vue'

const albums = ref<PhotoAlbum[]>([])
const loading = ref(true)
const saving = ref(false)
const editing = ref(false)
const currentId = ref<number | null>(null)
const error = ref('')
const dialogError = ref('')
const form = reactive<AlbumForm>({
  title: '',
  description: '',
  privateAlbum: false,
  password: '',
  sortOrder: 0
})
const { confirm, ConfirmDialog } = useConfirm()

/**
 * AI 图片描述（E 块）：只生成候选、不写库。
 *
 * <p>生成的文案有两个显式出口：**保存为照片图注**（写 `album_photo.caption`，
 * 即前台 `<img alt>` 与图注文字）或**应用到相册简介**（带进相册表单再显式保存）。
 * 两者都必须由作者点击——绝不静默改数据。</p>
 */
interface AltDraft extends ImageAltVO {
  photoId: number
}
const altBusy = ref(false)
const altDrafts = ref<AltDraft[]>([])

function draftsOf(album: PhotoAlbum): AltDraft[] {
  const ids = new Set((album.photos || []).map((photo) => photo.id))
  return altDrafts.value.filter((draft) => ids.has(draft.photoId))
}

function dismissDraft(photoId: number) {
  altDrafts.value = altDrafts.value.filter((draft) => draft.photoId !== photoId)
}

async function requestAlt(photoId: number) {
  altBusy.value = true
  try {
    const response = await llmApi.imageAlt({ source: 'album', photoId })
    const data = response.data
    if (!data) {
      toast.error('模型没有返回可用的描述，请重试')
      return
    }
    altDrafts.value = [
      ...altDrafts.value.filter((draft) => draft.photoId !== photoId),
      { ...data, photoId }
    ]
  } catch (err) {
    // 502（模型不支持视觉 / 内容不合规）不带 userMessage，要回落到信封里的 message。
    const detail = err as Error & {
      userMessage?: string
      response?: { data?: { message?: string } }
    }
    toast.error(
      detail.userMessage || detail.response?.data?.message || '生成图片描述失败，请稍后重试'
    )
  } finally {
    altBusy.value = false
  }
}

function generatePhotoDescription(_album: PhotoAlbum, photoId: number) {
  void requestAlt(photoId)
}

function generateAlbumDescription(album: PhotoAlbum) {
  const cover = album.photos?.[0]
  if (!cover) {
    toast.error('先上传照片才能生成描述')
    return
  }
  void requestAlt(cover.id)
}

async function saveDraftAsCaption(album: PhotoAlbum, photoId: number) {
  const draft = altDrafts.value.find((item) => item.photoId === photoId)
  if (!draft) return
  // 图注优先用 caption；模型只给了 alt 时退回 alt（总比空着强，且它同样是描述性文本）。
  const caption = draft.caption || draft.alt
  if (!caption) {
    toast.error('模型没有返回可用的图注')
    return
  }
  altBusy.value = true
  try {
    await galleryApi.updatePhotoCaption(album.id, photoId, caption)
    dismissDraft(photoId)
    toast.success('照片图注已保存')
    await load()
  } catch (err) {
    toast.error((err as Error & { userMessage?: string }).userMessage || '保存图注失败，请重试')
  } finally {
    altBusy.value = false
  }
}

function applyDraftToDescription(album: PhotoAlbum, photoId: number) {
  const draft = altDrafts.value.find((item) => item.photoId === photoId)
  if (!draft) return
  editAlbum(album)
  form.description = draft.caption || draft.alt
  dismissDraft(photoId)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await galleryApi.getAdminAlbums()
    albums.value = response.data || []
  } catch {
    error.value = '相册无法加载，请检查登录状态后重试。'
  } finally {
    loading.value = false
  }
}

function newAlbum() {
  currentId.value = null
  Object.assign(form, {
    title: '',
    description: '',
    privateAlbum: false,
    password: '',
    sortOrder: 0
  })
  dialogError.value = ''
  editing.value = true
}

function editAlbum(album: PhotoAlbum) {
  currentId.value = album.id
  Object.assign(form, {
    title: album.title,
    description: album.description || '',
    privateAlbum: album.privateAlbum,
    password: '',
    sortOrder: album.sortOrder || 0
  })
  dialogError.value = ''
  editing.value = true
}

async function saveAlbum() {
  saving.value = true
  dialogError.value = ''
  try {
    if (currentId.value) await galleryApi.updateAlbum(currentId.value, { ...form })
    else await galleryApi.createAlbum({ ...form })
    editing.value = false
    toast.success('相册已保存')
    await load()
  } catch (err) {
    dialogError.value =
      (err as Error & { userMessage?: string }).userMessage || '保存失败，请重试。'
  } finally {
    saving.value = false
  }
}

async function uploadPhotos(album: PhotoAlbum, event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  try {
    await galleryApi.uploadPhotos(album.id, files)
    toast.success(`已上传 ${files.length} 张照片`)
    await load()
  } catch (err) {
    toast.error(
      (err as Error & { userMessage?: string }).userMessage || '上传失败，请确认每张照片小于 15MB。'
    )
  } finally {
    input.value = ''
  }
}

async function removePhoto(album: PhotoAlbum, photoId: number) {
  if (!(await confirm('确定删除这张照片吗？', '删除照片'))) return
  try {
    await galleryApi.deletePhoto(album.id, photoId)
    toast.success('照片已删除')
    await load()
  } catch {
    toast.error('删除失败，请重试')
  }
}

async function removeAlbum(album: PhotoAlbum) {
  if (!(await confirm(`删除相册「${album.title}」及其中全部照片？此操作不可恢复。`, '删除相册')))
    return
  try {
    await galleryApi.deleteAlbum(album.id)
    toast.success('相册已删除')
    await load()
  } catch {
    toast.error('删除失败，请重试')
  }
}

onMounted(() => void load())
</script>

<style scoped>
.albums-page {
  display: grid;
  gap: 16px;
  color: var(--text-primary);
}
.page-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}
.page-heading h1 {
  margin: 0 0 8px;
  font-size: var(--text-2xl);
}
.page-heading p:not(.eyebrow) {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.album-list {
  display: grid;
  gap: 13px;
}
.album-card {
  padding: 17px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}
.album-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.album-heading h2 {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 6px;
  font-size: var(--text-base);
}
.album-heading p {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.private-label {
  padding: 3px 7px;
  border-radius: var(--radius-xs);
  background: var(--accent-light);
  color: var(--accent);
  font-size: var(--text-2xs);
  font-weight: 500;
}
.album-actions {
  display: flex;
  gap: 6px;
}
.album-actions button {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding-inline: 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  cursor: pointer;
}
.album-actions button:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.album-actions .danger {
  color: var(--error);
}
.photo-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 16px;
}
.photo-strip figure,
.upload-tile {
  position: relative;
  display: grid;
  place-items: center;
  width: 92px;
  height: 76px;
  margin: 0;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--bg-secondary);
}
.photo-strip figure img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.photo-strip figure button {
  position: absolute;
  top: 4px;
  right: 4px;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  background: var(--preview-scrim);
  color: white;
  cursor: pointer;
}
.upload-tile {
  align-content: center;
  gap: 2px;
  border-style: dashed;
  color: var(--accent);
  cursor: pointer;
}
.photo-strip figure .alt-tile {
  top: auto;
  right: auto;
  bottom: 4px;
  left: 4px;
  width: 26px;
  height: 26px;
  border: 0;
}
.photo-strip figure .alt-tile:disabled {
  cursor: progress;
  opacity: 0.55;
}
.alt-drafts {
  display: grid;
  gap: 8px;
  margin-top: 13px;
}
.alt-draft {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--accent-light);
  border-radius: var(--radius);
  background: var(--accent-light);
}
.alt-draft-text p {
  margin: 0 0 3px;
  color: var(--text-primary);
  font-size: var(--text-sm);
}
.alt-draft-text small {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.alt-draft-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 6px;
}
.alt-draft-actions button {
  min-height: 32px;
  padding-inline: 9px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-secondary);
  font-size: var(--text-xs);
  cursor: pointer;
}
.alt-draft-actions button:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.upload-tile small {
  font-size: var(--text-2xs);
}
.upload-tile input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}
.album-card footer {
  display: flex;
  justify-content: space-between;
  margin-top: 13px;
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.empty {
  padding: 40px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  color: var(--text-muted);
  text-align: center;
  font-size: var(--text-sm);
}
.error {
  color: var(--error);
  font-size: var(--text-sm);
}
.album-dialog {
  display: grid;
  gap: 14px;
}
.album-dialog label {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.album-dialog input:not([type='checkbox']),
.album-dialog textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: var(--text-base);
}
.album-dialog .check-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dialog-error {
  margin: 0;
  color: var(--error);
  font-size: var(--text-sm);
}
@media (max-width: 640px) {
  .page-heading {
    align-items: start;
    flex-direction: column;
  }
  .album-heading {
    flex-direction: column;
  }
  .album-card footer {
    flex-direction: column;
    gap: 5px;
  }
}
</style>
