<template>
  <div class="gallery-page">
    <header class="gallery-hero">
      <p class="eyebrow">PHOTO JOURNAL</p>
      <h1>相册</h1>
      <p>把途中的光影，慢慢收进记忆。</p>
    </header>

    <div v-if="loading" class="gallery-state">正在整理相册…</div>
    <div v-else-if="error" class="gallery-state error" role="alert">
      {{ error }} <button type="button" @click="loadAlbums">重试</button>
    </div>
    <div v-else-if="albums.length" class="album-grid">
      <button v-for="album in albums" :key="album.id" class="album-card" type="button" @click="openAlbum(album)">
        <span class="album-cover">
          <img v-if="album.coverUrl" :src="album.coverUrl" :alt="album.title" loading="lazy" />
          <span v-else class="cover-empty" aria-hidden="true">✳</span>
          <span v-if="album.privateAlbum" class="lock-badge">⌑ 密码相册</span>
        </span>
        <span class="album-meta"><strong>{{ album.title }}</strong><small>{{ album.photoCount }} 张照片</small></span>
        <span v-if="album.description" class="album-description">{{ album.description }}</span>
      </button>
    </div>
    <div v-else class="gallery-state">相册还在准备中，欢迎稍后再来。</div>

    <div v-if="selectedAlbum" class="modal-backdrop" @click.self="closeAlbum" @keydown.esc="closeAlbum">
      <section class="album-modal" role="dialog" aria-modal="true" :aria-label="selectedAlbum.title">
        <header class="modal-header">
          <div><p class="eyebrow">ALBUM</p><h2>{{ selectedAlbum.title }}</h2><p>{{ selectedAlbum.description }}</p></div>
          <button class="close-button" type="button" aria-label="关闭相册" @click="closeAlbum">×</button>
        </header>
        <form v-if="selectedAlbum.locked" class="unlock-form" @submit.prevent="unlockAlbum">
          <span class="lock-icon" aria-hidden="true">⌑</span>
          <h3>这是一个私密相册</h3>
          <p>输入相册密码，解锁里面的照片。</p>
          <input v-model="password" type="password" autocomplete="current-password" placeholder="相册密码" required />
          <button type="submit" :disabled="unlocking">{{ unlocking ? '正在验证…' : '解锁相册' }}</button>
          <small v-if="unlockError" role="alert">{{ unlockError }}</small>
        </form>
        <div v-else-if="selectedAlbum.photos?.length" class="photo-grid">
          <button v-for="(photo, index) in selectedAlbum.photos" :key="photo.id" type="button" class="photo-tile" @click="lightboxIndex = index">
            <img :src="photo.imageUrl" :alt="photo.caption || selectedAlbum.title" loading="lazy" />
            <span v-if="photo.caption">{{ photo.caption }}</span>
          </button>
        </div>
        <div v-else class="gallery-state">这个相册暂时还没有照片。</div>
      </section>
    </div>

    <div v-if="lightboxPhoto" class="lightbox" @click.self="lightboxIndex = -1">
      <button class="lightbox-close" type="button" aria-label="关闭大图" @click="lightboxIndex = -1">×</button>
      <button v-if="selectedAlbum && lightboxIndex > 0" class="lightbox-nav prev" type="button" aria-label="上一张" @click="lightboxIndex--">‹</button>
      <figure><img :src="lightboxPhoto.imageUrl" :alt="lightboxPhoto.caption || selectedAlbum?.title" /><figcaption>{{ lightboxPhoto.caption }}</figcaption></figure>
      <button v-if="selectedAlbum && lightboxIndex < (selectedAlbum.photos?.length || 0) - 1" class="lightbox-nav next" type="button" aria-label="下一张" @click="lightboxIndex++">›</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { galleryApi, type PhotoAlbum } from '@/api/gallery'

const albums = ref<PhotoAlbum[]>([])
const selectedAlbum = ref<PhotoAlbum | null>(null)
const accessToken = ref('')
const password = ref('')
const unlockError = ref('')
const loading = ref(true)
const unlocking = ref(false)
const error = ref('')
const lightboxIndex = ref(-1)
const lightboxPhoto = computed(() => selectedAlbum.value?.photos?.[lightboxIndex.value] || null)

async function loadAlbums() {
  loading.value = true
  error.value = ''
  try {
    const result = await galleryApi.getAlbums()
    albums.value = result.data || []
  } catch {
    error.value = '相册暂时无法加载，请检查网络后重试。'
  } finally {
    loading.value = false
  }
}

async function openAlbum(album: PhotoAlbum) {
  selectedAlbum.value = { ...album, locked: album.privateAlbum }
  accessToken.value = ''
  password.value = ''
  unlockError.value = ''
  lightboxIndex.value = -1
  if (!album.privateAlbum) await loadAlbum(album.id)
}

async function loadAlbum(id: number) {
  try {
    const result = await galleryApi.getAlbum(id, accessToken.value || undefined)
    selectedAlbum.value = result.data || null
  } catch {
    unlockError.value = '相册暂时无法加载，请稍后重试。'
  }
}

async function unlockAlbum() {
  if (!selectedAlbum.value) return
  unlocking.value = true
  unlockError.value = ''
  try {
    const result = await galleryApi.unlock(selectedAlbum.value.id, password.value)
    accessToken.value = result.data?.accessToken || ''
    if (!accessToken.value) throw new Error('Missing album access token')
    await loadAlbum(selectedAlbum.value.id)
  } catch (err) {
    unlockError.value = (err as Error & { userMessage?: string }).userMessage || '密码不正确，请重新输入。'
  } finally {
    unlocking.value = false
  }
}

function closeAlbum() {
  selectedAlbum.value = null
  accessToken.value = ''
  lightboxIndex.value = -1
}

onMounted(() => void loadAlbums())
</script>

<style scoped>
.gallery-page { max-width: 1120px; margin: 0 auto; padding: 45px 24px 60px; color: var(--text-primary); }
.gallery-hero { margin: 0 auto 38px; max-width: 660px; text-align: center; }
.eyebrow { color: var(--primary); font-size: .68rem; font-weight: 700; letter-spacing: .17em; }
.gallery-hero h1 { margin: 10px 0; font: 700 clamp(2.5rem, 7vw, 4.4rem)/1 var(--font-display); letter-spacing: -.06em; }
.gallery-hero > p:last-child { color: var(--text-secondary); }
.album-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; }
.album-card { display: flex; min-width: 0; flex-direction: column; gap: 12px; padding: 0 0 16px; overflow: hidden; border: 1px solid var(--border-solid); border-radius: var(--radius-lg); background: var(--surface-primary); color: inherit; text-align: left; cursor: pointer; transition: transform .2s, box-shadow .2s; }
.album-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
.album-cover { position: relative; display: grid; place-items: center; aspect-ratio: 1.3; overflow: hidden; background: var(--bg-secondary); }
.album-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform .35s; }
.album-card:hover .album-cover img { transform: scale(1.04); }
.cover-empty { color: var(--primary); font-size: 3rem; opacity: .3; }
.lock-badge { position: absolute; right: 12px; bottom: 12px; padding: 6px 10px; border-radius: 999px; background: rgb(16 24 40 / 70%); color: white; font-size: .7rem; backdrop-filter: blur(8px); }
.album-meta { display: flex; justify-content: space-between; gap: 12px; padding: 0 15px; }
.album-meta strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.album-meta small { flex: 0 0 auto; color: var(--text-muted); }
.album-description { padding: 0 15px; color: var(--text-secondary); font-size: .82rem; line-height: 1.6; }
.gallery-state { padding: 64px 20px; color: var(--text-muted); text-align: center; }
.gallery-state button { margin-left: 10px; border: 0; background: none; color: var(--primary); cursor: pointer; }
.error { color: var(--error); }
.modal-backdrop, .lightbox { position: fixed; z-index: 1000; inset: 0; display: grid; place-items: center; padding: 22px; background: rgb(8 15 25 / 72%); backdrop-filter: blur(7px); }
.album-modal { width: min(1040px, 100%); max-height: min(88vh, 900px); overflow: auto; padding: 24px; border: 1px solid var(--border-solid); border-radius: var(--radius-lg); background: var(--surface-primary); color: var(--text-primary); }
.modal-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 22px; }
.modal-header h2 { margin: 7px 0; font: 700 1.8rem var(--font-display); }
.modal-header p:last-child { margin: 0; color: var(--text-secondary); }
.close-button, .lightbox-close { width: 38px; height: 38px; border: 0; border-radius: 50%; background: var(--bg-secondary); color: var(--text-primary); font-size: 1.6rem; cursor: pointer; }
.unlock-form { display: grid; justify-items: center; gap: 12px; max-width: 360px; margin: 50px auto 65px; text-align: center; }
.lock-icon { color: var(--primary); font-size: 2.2rem; }
.unlock-form h3, .unlock-form p { margin: 0; }
.unlock-form p, .unlock-form small { color: var(--text-muted); font-size: .85rem; }
.unlock-form input { width: 100%; margin-top: 7px; padding: 12px 14px; border: 1px solid var(--border-solid); border-radius: var(--radius-sm); background: var(--bg-primary); color: var(--text-primary); }
.unlock-form button { width: 100%; padding: 12px; border: 0; border-radius: var(--radius-sm); background: var(--primary); color: white; font-weight: 700; cursor: pointer; }
.photo-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.photo-tile { position: relative; overflow: hidden; aspect-ratio: 1.2; padding: 0; border: 0; border-radius: var(--radius); background: var(--bg-secondary); cursor: zoom-in; }
.photo-tile img { width: 100%; height: 100%; object-fit: cover; }
.photo-tile span { position: absolute; inset: auto 0 0; padding: 25px 10px 9px; background: linear-gradient(transparent, rgb(0 0 0 / 65%)); color: white; font-size: .78rem; text-align: left; }
.lightbox { z-index: 1100; grid-template-columns: 54px minmax(0, 1fr) 54px; }
.lightbox figure { max-width: 100%; max-height: 88vh; margin: 0; text-align: center; }
.lightbox figure img { max-width: min(82vw, 1200px); max-height: 80vh; border-radius: var(--radius-sm); object-fit: contain; }
.lightbox figcaption { margin-top: 10px; color: white; }
.lightbox-close { position: absolute; top: 20px; right: 22px; }
.lightbox-nav { width: 45px; height: 45px; border: 1px solid rgb(255 255 255 / 35%); border-radius: 50%; background: rgb(0 0 0 / 28%); color: white; font-size: 2.3rem; cursor: pointer; }
.lightbox-nav.next { justify-self: end; }
@media (max-width: 760px) { .album-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .photo-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .gallery-page { padding: 30px 16px; } }
@media (max-width: 460px) { .album-grid { grid-template-columns: 1fr; } .album-meta { flex-direction: column; gap: 4px; } .album-modal { padding: 17px; } }
</style>
