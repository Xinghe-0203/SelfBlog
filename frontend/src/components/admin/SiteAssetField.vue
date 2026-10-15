<script setup lang="ts">
/**
 * SiteAssetField.vue — 站点品牌资源字段
 *
 * 预览 + 手动 URL 输入 + 上传按钮。服务端才是校验权威（扩展名白名单 + 魔数 + 体积），
 * 这里只做"早失败"预检，让用户不必等网络往返就知道选错了文件。
 */
import { computed, ref } from 'vue'
import { siteApi, type SiteAssetKind } from '@/api/site'

interface SiteAssetRule {
  extensions: string[]
  maxBytes: number
}

/** 与后端 SiteAssetKind 保持一致；accept 直接由扩展名拼出。 */
const KIND_RULES: Record<SiteAssetKind, SiteAssetRule> = {
  logo: { extensions: ['.png', '.jpg', '.jpeg', '.webp', '.svg'], maxBytes: 2 * 1024 * 1024 },
  avatar: { extensions: ['.png', '.jpg', '.jpeg', '.webp', '.svg'], maxBytes: 2 * 1024 * 1024 },
  favicon: { extensions: ['.png', '.ico', '.svg'], maxBytes: 2 * 1024 * 1024 },
  music: { extensions: ['.mp3', '.ogg', '.wav', '.m4a', '.aac'], maxBytes: 20 * 1024 * 1024 }
}

const props = defineProps<{
  modelValue: string
  kind: SiteAssetKind
  label: string
  hint?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  uploaded: [value: string]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const progress = ref(0)
const error = ref('')
/** 上传成功时的原始文件名（URL 里是 UUID，对用户没有意义）。 */
const uploadedName = ref('')
const duration = ref('')

const rules = computed(() => KIND_RULES[props.kind])
const accept = computed(() => rules.value.extensions.join(','))

/** 音乐预览的说明行：文件名（优先原始名）+ 可选时长。 */
const audioMeta = computed(() => {
  const name = uploadedName.value || fileName(props.modelValue)
  return duration.value ? `${name} · ${duration.value}` : name
})

function extensionOf(filename: string): string {
  const dot = filename.lastIndexOf('.')
  return dot > 0 ? filename.slice(dot).toLowerCase() : ''
}

function fileName(url: string): string {
  const clean = url.replace(/[?#].*$/, '')
  const name = clean.slice(clean.lastIndexOf('/') + 1)
  try {
    return decodeURIComponent(name)
  } catch {
    return name
  }
}

function formatDuration(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 1) return ''
  const total = Math.round(seconds)
  const minutes = Math.floor(total / 60)
  const rest = total % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function handleInput(event: Event): void {
  // 手动改 URL：清掉上一次上传/预览留下的状态，避免旧文件名、时长或错误残留在新值下面。
  uploadedName.value = ''
  duration.value = ''
  error.value = ''
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}

function readDuration(event: Event): void {
  duration.value = formatDuration((event.target as HTMLAudioElement).duration)
}

async function handleFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // 清空 value，否则连续选择同一个文件不会再触发 change。
  input.value = ''
  if (!file || uploading.value) return

  error.value = ''
  const extension = extensionOf(file.name)
  if (!rules.value.extensions.includes(extension)) {
    error.value = `仅支持 ${rules.value.extensions.join('、')} 格式`
    return
  }
  const maxMegabytes = rules.value.maxBytes / (1024 * 1024)
  if (file.size > rules.value.maxBytes) {
    error.value = `文件大小不能超过 ${maxMegabytes}MB`
    return
  }

  uploading.value = true
  progress.value = 0
  try {
    const response = await siteApi.uploadSiteAsset(props.kind, file, (progressEvent) => {
      if (progressEvent.total) {
        progress.value = Math.round((progressEvent.loaded / progressEvent.total) * 100)
      }
    })
    if (!response.data?.url) throw new Error('empty upload response')
    uploadedName.value = response.data.originalName || file.name
    duration.value = ''
    emit('update:modelValue', response.data.url)
    // 与手动编辑区分：只有真实上传结果才让设置页同步到全局 store（Logo / favicon 立即换新）。
    emit('uploaded', response.data.url)
  } catch (err) {
    error.value = (err as Error & { userMessage?: string }).userMessage || '上传失败，请稍后重试。'
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="asset-field">
    <span class="asset-label">{{ label }}</span>

    <div v-if="kind === 'music' && modelValue" class="asset-audio">
      <audio
        class="asset-audio-player"
        controls
        preload="metadata"
        :src="modelValue"
        @loadedmetadata="readDuration"
      />
      <span class="asset-audio-meta">{{ audioMeta }}</span>
    </div>

    <div class="asset-row">
      <div v-if="kind !== 'music'" class="asset-thumb">
        <img v-if="modelValue" class="asset-thumb-image" :src="modelValue" :alt="label" />
        <span v-else class="asset-thumb-empty">未设置</span>
      </div>

      <div class="asset-controls">
        <input
          class="asset-input"
          type="text"
          :value="modelValue"
          :disabled="uploading"
          placeholder="/uploads/site-assets/… 或 https://…"
          :aria-label="`${label}地址`"
          @input="handleInput"
        />
        <button
          type="button"
          class="asset-upload-button"
          :disabled="uploading"
          @click="fileInput?.click()"
        >
          {{ uploading ? (progress > 0 ? `上传中 ${progress}%…` : '上传中…') : '上传文件' }}
        </button>
        <input
          ref="fileInput"
          class="asset-file-input"
          type="file"
          :accept="accept"
          @change="handleFileChange"
        />
      </div>
    </div>

    <p v-if="error" class="asset-error" role="alert">{{ error }}</p>
    <p v-else-if="hint" class="asset-hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.asset-field { display: grid; gap: 7px; }
.asset-label { color: var(--text-secondary); font-size: .78rem; }
.asset-row { display: flex; gap: 10px; align-items: stretch; }
.asset-thumb { display: grid; place-items: center; flex: 0 0 auto; width: 74px; min-height: 74px; overflow: hidden; border: 1px solid var(--border-solid); border-radius: var(--radius-sm); background: var(--bg-secondary); }
.asset-thumb-image { width: 100%; height: 100%; object-fit: cover; }
.asset-thumb-empty { color: var(--text-muted); font-size: .68rem; }
.asset-controls { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 8px; }
.asset-input { width: 100%; padding: 10px 11px; border: 1px solid var(--border-solid); border-radius: var(--radius-sm); background: var(--bg-primary); color: var(--text-primary); font: inherit; font-size: .84rem; }
.asset-input:disabled { opacity: .6; cursor: wait; }
.asset-upload-button { align-self: flex-start; padding: 8px 13px; border: 0; border-radius: var(--radius-sm); background: var(--primary); color: white; font-size: .78rem; font-weight: 700; cursor: pointer; }
.asset-upload-button:disabled { opacity: .6; cursor: wait; }
.asset-file-input { display: none; }
.asset-audio { display: grid; gap: 6px; }
.asset-audio-player { width: 100%; }
.asset-audio-meta { color: var(--text-muted); font-size: .72rem; }
.asset-error { margin: 0; color: var(--error); font-size: .72rem; }
.asset-hint { margin: 0; color: var(--text-muted); font-size: .7rem; line-height: 1.5; }
</style>
