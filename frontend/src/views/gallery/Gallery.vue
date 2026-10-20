<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { galleryApi, type PhotoAlbum } from '@/api/gallery'
import Modal from '@/components/common/Modal.vue'
import ImagePreview from '@/components/common/ImagePreview.vue'
import Icon from '@/components/common/Icon.vue'
import { getSafeImageUrl } from '@/utils'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { usePageSeo } from '@/composables/usePageSeo'
const { config, text, module: showModule } = usePageConfig('gallery')
usePageSeo('gallery', () => ({
  title: text('title', '相册'),
  description: text('description', '把途中的光影，慢慢收进记忆。'),
  url: '/gallery'
}))

const albums = ref<PhotoAlbum[]>([])
const selectedAlbum = ref<PhotoAlbum | null>(null)
const accessToken = ref('')
const password = ref('')
const unlockError = ref('')
const loading = ref(true)
const albumLoading = ref(false)
const unlocking = ref(false)
const error = ref('')
const albumError = ref('')
const lightboxIndex = ref(-1)
const failedCovers = ref(new Set<number>())
const photoUrls = computed(() =>
  (selectedAlbum.value?.photos || []).map((photo) => getSafeImageUrl(photo.imageUrl, ''))
)
const photoCaptions = computed(() =>
  (selectedAlbum.value?.photos || []).map((photo) => photo.caption || '')
)
let revision = 0

async function loadAlbums(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    albums.value = (await galleryApi.getAlbums()).data || []
  } catch {
    error.value = '相册暂时无法加载，请检查网络后重试。'
  } finally {
    loading.value = false
  }
}

function openAlbum(album: PhotoAlbum): void {
  revision++
  selectedAlbum.value = { ...album, locked: album.privateAlbum, photos: undefined }
  accessToken.value = ''
  password.value = ''
  unlockError.value = ''
  albumError.value = ''
  unlocking.value = false
  albumLoading.value = false
  lightboxIndex.value = -1
  if (!album.privateAlbum) void loadAlbum(album.id, revision)
}

async function loadAlbum(id: number, request = revision): Promise<void> {
  albumLoading.value = true
  albumError.value = ''
  try {
    const result = await galleryApi.getAlbum(id, accessToken.value || undefined)
    if (request !== revision || selectedAlbum.value?.id !== id) return
    if (!result.data) throw new Error('Missing album')
    selectedAlbum.value = result.data
  } catch {
    if (request === revision) albumError.value = '相册暂时无法加载，请稍后重试。'
  } finally {
    if (request === revision) albumLoading.value = false
  }
}

async function unlockAlbum(): Promise<void> {
  if (!selectedAlbum.value || unlocking.value) return
  const id = selectedAlbum.value.id
  const request = revision
  unlocking.value = true
  unlockError.value = ''
  try {
    const result = await galleryApi.unlock(id, password.value)
    if (request !== revision || selectedAlbum.value?.id !== id) return
    accessToken.value = result.data?.accessToken || ''
    if (!accessToken.value) throw new Error('Missing album access token')
    selectedAlbum.value.locked = false
    password.value = ''
    await loadAlbum(id, request)
  } catch (reason) {
    if (request === revision)
      unlockError.value =
        (reason as Error & { userMessage?: string }).userMessage || '密码不正确，请重新输入。'
  } finally {
    if (request === revision) unlocking.value = false
  }
}

function closeAlbum(): void {
  revision++
  selectedAlbum.value = null
  accessToken.value = ''
  password.value = ''
  lightboxIndex.value = -1
  unlocking.value = false
  albumLoading.value = false
}
onMounted(() => void loadAlbums())
onBeforeUnmount(() => {
  revision++
})
</script>

<template>
  <PageFrame :config="config" class="gallery-page">
    <header class="gallery-hero">
      <h1>{{ text('title', '相册') }}</h1>
      <p v-if="text('description', '把途中的光影，慢慢收进记忆。')">
        {{ text('description', '把途中的光影，慢慢收进记忆。') }}
      </p>
    </header>
    <div v-if="loading" class="gallery-state" role="status">正在整理相册…</div>
    <div v-else-if="error" class="gallery-state" role="alert">
      <p>{{ error }}</p>
      <button type="button" class="btn btn-secondary" @click="loadAlbums">重新加载</button>
    </div>
    <div v-else-if="albums.length" class="album-grid">
      <button
        v-for="album in albums"
        :key="album.id"
        class="album-card"
        type="button"
        @click="openAlbum(album)"
      >
        <span class="album-cover"
          ><img
            v-if="getSafeImageUrl(album.coverUrl, '') && !failedCovers.has(album.id)"
            :src="getSafeImageUrl(album.coverUrl, '')"
            alt=""
            loading="lazy"
            @error="failedCovers.add(album.id)"
          /><Icon v-else name="image" :size="30" /><span
            v-if="album.privateAlbum"
            class="lock-badge"
            ><Icon name="lock" :size="13" />密码相册</span
          ></span
        >
        <span class="album-meta"
          ><strong>{{ album.title }}</strong
          ><small v-if="showModule('count')">{{ album.photoCount }} 张照片</small></span
        ><span v-if="showModule('description') && album.description" class="album-description">{{
          album.description
        }}</span>
      </button>
    </div>
    <div v-else class="gallery-state">相册还在准备中，欢迎稍后再来。</div>

    <Modal
      :show="selectedAlbum !== null"
      :title="selectedAlbum?.title"
      width="1040px"
      :closable="true"
      @close="closeAlbum"
    >
      <template v-if="selectedAlbum">
        <p v-if="selectedAlbum.description" class="album-intro">{{ selectedAlbum.description }}</p>
        <form v-if="selectedAlbum.locked" class="unlock-form" @submit.prevent="unlockAlbum">
          <Icon name="lock" :size="28" />
          <h3>这是一个密码相册</h3>
          <p>输入相册密码，解锁里面的照片。</p>
          <label class="sr-only" for="album-password">相册密码</label
          ><input
            id="album-password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="相册密码"
            required
            :disabled="unlocking"
          />
          <button type="submit" class="btn btn-primary" :disabled="unlocking">
            {{ unlocking ? '正在验证…' : '解锁相册' }}</button
          ><small v-if="unlockError" role="alert">{{ unlockError }}</small>
        </form>
        <div v-else-if="albumLoading" class="gallery-state" role="status">正在加载照片…</div>
        <div v-else-if="albumError" class="gallery-state" role="alert">
          <p>{{ albumError }}</p>
          <button type="button" class="btn btn-secondary" @click="loadAlbum(selectedAlbum.id)">
            重试
          </button>
        </div>
        <div v-else-if="selectedAlbum.photos?.length" class="photo-grid">
          <button
            v-for="(photo, index) in selectedAlbum.photos"
            :key="photo.id"
            type="button"
            class="photo-tile"
            @click="lightboxIndex = index"
          >
            <img
              :src="photoUrls[index]"
              :alt="photo.caption || selectedAlbum.title"
              loading="lazy"
            /><span v-if="photo.caption">{{ photo.caption }}</span>
          </button>
        </div>
        <div v-else class="gallery-state">这个相册暂时还没有照片。</div>
      </template>
    </Modal>
    <ImagePreview
      :show="lightboxIndex >= 0"
      :images="photoUrls"
      :captions="photoCaptions"
      :initial-index="Math.max(0, lightboxIndex)"
      @close="lightboxIndex = -1"
    />
  </PageFrame>
</template>

<style scoped>
.gallery-page {
  max-width: var(--page-width, 1130px);
  margin-inline: auto;
  padding: var(--page-padding-block, 24px) 30px 48px;
}
.gallery-hero {
  margin-bottom: 28px;
}
.gallery-hero h1 {
  font-size: var(--text-3xl);
}
.gallery-hero p {
  margin-top: 12px;
  font-size: var(--text-sm);
}
.album-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--page-section-gap, 20px);
}
.album-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  padding-bottom: 18px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  color: var(--text-primary);
  text-align: left;
}
.album-card:hover {
  border-color: var(--accent);
}
.album-cover {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  aspect-ratio: 1.4;
  overflow: hidden;
  background: var(--bg-tertiary);
  color: var(--text-muted);
}
.album-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.lock-badge {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  border-radius: var(--radius-sm);
  background: var(--preview-scrim);
  color: white;
  font-size: var(--text-2xs);
}
.album-meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  padding-inline: 16px;
}
.album-meta strong {
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: var(--text-sm);
}
.album-meta small {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.album-description {
  padding-inline: 16px;
  font-size: var(--text-xs);
  line-height: 1.8;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}
.gallery-state {
  display: grid;
  justify-items: center;
  gap: 16px;
  padding: 48px 20px;
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  text-align: center;
}
.gallery-state p {
  font-size: var(--text-sm);
}
.album-intro {
  margin-bottom: 20px;
  font-size: var(--text-sm);
  overflow-wrap: anywhere;
}
.unlock-form {
  display: grid;
  justify-items: center;
  gap: 14px;
  max-width: 360px;
  margin: 24px auto 36px;
  text-align: center;
}
.unlock-form > .icon {
  color: var(--accent);
}
.unlock-form h3 {
  font-size: var(--text-lg);
}
.unlock-form p {
  font-size: var(--text-sm);
}
.unlock-form input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: var(--text-base);
}
.unlock-form .btn {
  width: 100%;
}
.unlock-form small {
  font-size: var(--text-sm);
  color: var(--error);
}
.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.photo-tile {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1.2;
  padding: 0;
  border-radius: var(--radius);
  background: var(--bg-secondary);
  cursor: zoom-in;
}
.photo-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.photo-tile span {
  position: absolute;
  inset: auto 0 0;
  padding: 25px 10px 9px;
  background: linear-gradient(transparent, var(--preview-scrim));
  color: white;
  font-size: var(--text-xs);
  text-align: left;
}
@media (max-width: 760px) {
  .album-grid,
  .photo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 600px) {
  .gallery-page {
    padding: 8px 18px 36px;
  }
}
@media (max-width: 460px) {
  .album-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
