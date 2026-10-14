<template>
  <div class="albums-page">
    <header class="page-heading"><div><p class="eyebrow">PHOTO LIBRARY</p><h1>相册管理</h1><p>创建公开或密码相册，上传照片并维护顺序。</p></div><button class="primary" type="button" @click="newAlbum">＋ 新建相册</button></header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div v-if="loading" class="empty">正在加载相册…</div>
    <div v-else-if="!albums.length" class="empty">还没有相册，创建后即可在前台相册页展示。</div>
    <section v-else class="album-list">
      <article v-for="album in albums" :key="album.id" class="album-card">
        <header class="album-heading"><div><h2>{{ album.title }} <span v-if="album.privateAlbum" class="private-label">密码保护</span></h2><p>{{ album.description || '暂无描述' }}</p></div><div class="album-actions"><button type="button" @click="editAlbum(album)">编辑</button><button class="danger" type="button" @click="removeAlbum(album)">删除</button></div></header>
        <div class="photo-strip">
          <figure v-for="photo in album.photos || []" :key="photo.id"><img :src="photo.imageUrl" :alt="photo.caption || album.title" /><button type="button" :aria-label="`删除照片 ${photo.id}`" @click="removePhoto(album, photo.id)">×</button></figure>
          <label class="upload-tile"><input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple @change="uploadPhotos(album, $event)" /><span aria-hidden="true">＋</span><small>上传照片</small></label>
        </div>
        <footer><span>{{ album.photoCount }} 张照片</span><span>{{ album.privateAlbum ? '仅输入密码后可见' : '公开展示' }}</span></footer>
      </article>
    </section>

    <div v-if="editing" class="dialog-backdrop" @click.self="editing = false">
      <form class="album-dialog" @submit.prevent="saveAlbum">
        <header><div><p class="eyebrow">ALBUM DETAILS</p><h2>{{ currentId ? '编辑相册' : '新建相册' }}</h2></div><button type="button" aria-label="关闭" @click="editing = false">×</button></header>
        <label>相册名称<input v-model.trim="form.title" maxlength="160" required /></label>
        <label>相册简介<textarea v-model.trim="form.description" rows="3" maxlength="1000"></textarea></label>
        <label class="check-row"><input v-model="form.privateAlbum" type="checkbox" />设为密码相册</label>
        <label v-if="form.privateAlbum">访问密码<input v-model="form.password" type="password" :required="!currentId" minlength="6" autocomplete="new-password" :placeholder="currentId ? '留空表示不修改密码' : '至少 6 位'" /></label>
        <label>排序值<input v-model.number="form.sortOrder" type="number" /></label>
        <p class="dialog-error" v-if="dialogError">{{ dialogError }}</p>
        <footer><button class="secondary" type="button" @click="editing = false">取消</button><button class="primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存相册' }}</button></footer>
      </form>
    </div>
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { galleryApi, type AlbumForm, type PhotoAlbum } from '@/api/gallery'
import { toast } from '@/utils/toast'
import { useConfirm } from '@/composables/useConfirm'

const albums = ref<PhotoAlbum[]>([])
const loading = ref(true)
const saving = ref(false)
const editing = ref(false)
const currentId = ref<number | null>(null)
const error = ref('')
const dialogError = ref('')
const form = reactive<AlbumForm>({ title: '', description: '', privateAlbum: false, password: '', sortOrder: 0 })
const { confirm, ConfirmDialog } = useConfirm()

async function load() {
  loading.value = true
  error.value = ''
  try { const response = await galleryApi.getAdminAlbums(); albums.value = response.data || [] }
  catch { error.value = '相册无法加载，请检查登录状态后重试。' }
  finally { loading.value = false }
}

function newAlbum() {
  currentId.value = null
  Object.assign(form, { title: '', description: '', privateAlbum: false, password: '', sortOrder: 0 })
  dialogError.value = ''
  editing.value = true
}

function editAlbum(album: PhotoAlbum) {
  currentId.value = album.id
  Object.assign(form, { title: album.title, description: album.description || '', privateAlbum: album.privateAlbum, password: '', sortOrder: album.sortOrder || 0 })
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
  } catch (err) { dialogError.value = (err as Error & { userMessage?: string }).userMessage || '保存失败，请重试。' }
  finally { saving.value = false }
}

async function uploadPhotos(album: PhotoAlbum, event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  try {
    await galleryApi.uploadPhotos(album.id, files)
    toast.success(`已上传 ${files.length} 张照片`)
    await load()
  } catch (err) { toast.error((err as Error & { userMessage?: string }).userMessage || '上传失败，请确认每张照片小于 15MB。') }
  finally { input.value = '' }
}

async function removePhoto(album: PhotoAlbum, photoId: number) {
  if (!(await confirm('确定删除这张照片吗？', '删除照片'))) return
  try { await galleryApi.deletePhoto(album.id, photoId); toast.success('照片已删除'); await load() }
  catch { toast.error('删除失败，请重试') }
}

async function removeAlbum(album: PhotoAlbum) {
  if (!(await confirm(`删除相册「${album.title}」及其中全部照片？此操作不可恢复。`, '删除相册'))) return
  try { await galleryApi.deleteAlbum(album.id); toast.success('相册已删除'); await load() }
  catch { toast.error('删除失败，请重试') }
}

onMounted(() => void load())
</script>

<style scoped>
.albums-page { display: grid; gap: 16px; color: var(--text-primary); }
.page-heading { display: flex; align-items: end; justify-content: space-between; gap: 16px; }
.page-heading h1 { margin: 5px 0; font: 700 2rem var(--font-display); }.page-heading p:not(.eyebrow) { margin: 0; color: var(--text-muted); font-size: .8rem; }
.eyebrow { margin: 0; color: var(--primary); font-size: .68rem; font-weight: 700; letter-spacing: .16em; }
.primary, .secondary { padding: 9px 13px; border: 0; border-radius: 8px; background: var(--primary); color: white; font-size: .8rem; font-weight: 700; cursor: pointer; }.secondary { border: 1px solid var(--border-solid); background: var(--surface-primary); color: var(--text-secondary); }
.album-list { display: grid; gap: 13px; }.album-card { padding: 17px; border: 1px solid var(--border-solid); border-radius: 12px; background: var(--surface-primary); }
.album-heading { display: flex; justify-content: space-between; gap: 12px; }.album-heading h2 { display: flex; align-items: center; gap: 9px; margin: 0 0 6px; font-size: 1rem; }.album-heading p { margin: 0; color: var(--text-muted); font-size: .75rem; }
.private-label { padding: 4px 7px; border-radius: 999px; background: var(--primary-light); color: var(--primary); font-size: .65rem; font-weight: 500; }.album-actions { display: flex; gap: 7px; }.album-actions button { height: 31px; padding: 0 9px; border: 1px solid var(--border-solid); border-radius: 7px; background: transparent; color: var(--text-secondary); font-size: .72rem; cursor: pointer; }.album-actions .danger { color: var(--error); }
.photo-strip { display: flex; flex-wrap: wrap; gap: 9px; margin-top: 16px; }.photo-strip figure, .upload-tile { position: relative; display: grid; place-items: center; width: 92px; height: 76px; margin: 0; overflow: hidden; border: 1px solid var(--border-solid); border-radius: 8px; background: var(--bg-secondary); }.photo-strip figure img { width: 100%; height: 100%; object-fit: cover; }.photo-strip figure button { position: absolute; top: 4px; right: 4px; width: 22px; height: 22px; border: 0; border-radius: 50%; background: rgb(0 0 0 / 65%); color: white; cursor: pointer; }
.upload-tile { align-content: center; gap: 1px; border-style: dashed; color: var(--primary); cursor: pointer; }.upload-tile span { font-size: 1.5rem; }.upload-tile small { font-size: .62rem; }.upload-tile input { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }
.album-card footer { display: flex; justify-content: space-between; margin-top: 13px; padding-top: 10px; border-top: 1px solid var(--border-solid); color: var(--text-muted); font-size: .69rem; }
.empty { padding: 45px; border: 1px dashed var(--border-solid); border-radius: 12px; color: var(--text-muted); text-align: center; }.error { color: var(--error); font-size: .8rem; }
.dialog-backdrop { position: fixed; z-index: 600; inset: 0; display: grid; place-items: center; padding: 18px; background: rgb(8 15 25 / 60%); backdrop-filter: blur(5px); }.album-dialog { display: grid; gap: 14px; width: min(480px, 100%); max-height: 90vh; overflow: auto; padding: 21px; border: 1px solid var(--border-solid); border-radius: 14px; background: var(--surface-primary); }.album-dialog header, .album-dialog footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.album-dialog header h2 { margin: 5px 0 0; font-size: 1.25rem; }.album-dialog header > button { border: 0; background: none; color: var(--text-muted); font-size: 1.6rem; cursor: pointer; }.album-dialog label { display: grid; gap: 7px; color: var(--text-secondary); font-size: .77rem; }.album-dialog input:not([type=checkbox]), .album-dialog textarea { width: 100%; padding: 10px; border: 1px solid var(--border-solid); border-radius: 8px; background: var(--bg-primary); color: var(--text-primary); font: inherit; }.album-dialog .check-row { display: flex; align-items: center; gap: 8px; }.dialog-error { margin: 0; color: var(--error); font-size: .77rem; }.album-dialog footer { justify-content: end; }
@media (max-width: 580px) { .page-heading { align-items: start; flex-direction: column; }.album-heading { flex-direction: column; }.album-card footer { flex-direction: column; gap: 5px; } }
</style>
