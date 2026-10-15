# 前端站点资源上传控件 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在后台「网站设置」页接入四个站点资源字段（头像 / 站点 Logo / 网站图标 / 背景音乐）：预览 + 手动 URL 输入 + 上传按钮；上传成功立即生效（顶栏 Logo、favicon 换新），点「保存设置」才落库。

**Architecture:** 后端通道 `POST /api/admin/site-assets` 已在上一批次完成，本批次只动前端。三层：

1. `src/api/site.ts` 增加 `uploadSiteAsset()`（multipart，带进度回调）；
2. 新增通用组件 `src/components/admin/SiteAssetField.vue`（预览 + 输入 + 上传 + 预检 + 行内错误，props/emits 与仓库既有组件一致，不用 `defineModel`）；
3. `src/views/admin/Settings.vue` 四处接入，上传结果写入表单并 `siteStore.applySettings()` 乐观更新。

**Tech Stack:** Vue 3.5 `<script setup>` + TypeScript 6 + Pinia 2 + Vitest 4 + @vue/test-utils 2。不新增任何依赖。

## Global Constraints

- **不新增任何依赖**：`frontend/package.json` 不动。
- **Axios 拦截器返回业务 envelope**：`siteApi` 方法的返回值就是 `ApiResponse<T>`，`response.data` 才是载荷；失败时错误对象可能带 `userMessage`（`Settings.vue` 现有 `save()` 已是这个约定）。
- **服务端才是校验权威**：前端预检只做「扩展名白名单 + 体积上限」的早失败，规则必须与后端 `SiteAssetKind` 一致——logo/avatar `.png/.jpg/.jpeg/.webp/.svg`，favicon `.png/.ico/.svg`，music `.mp3/.ogg/.wav/.m4a/.aac`；图片上限 2MB、音乐上限 20MB。
- **`noUncheckedIndexedAccess: true`**（`frontend/tsconfig.json`）：索引访问结果带 `| undefined`；测试代码同样受 `vue-tsc` 检查，`mock.calls[0]` 之后要 `?.` 或显式类型标注。
- **命令一律在 `frontend/` 下执行**；Node 用仓库内已验证的 provisioned v24.15.0：
  ```powershell
  $env:PATH = "C:\Users\Zora\Downloads\SelfBlog\SelfBlog\backend\target\frontend\node;$env:PATH"
  ```
- **闸门**：`npm run lint:check`、`npm run type-check`、`npm run test:run` 必须通过；`npm run build` 在最后一步过。**不要**把 `format:check` 当闸门（存量失败），只对改过的文件跑 `npx prettier --check --end-of-line auto <files>`。
- 提交信息用英文 conventional commit 前缀。

---

### Task 1: `siteApi.uploadSiteAsset` 与类型

**Files:**
- Modify: `frontend/src/api/site.ts`
- Create: `frontend/src/api/__tests__/site.test.ts`

**Interfaces:**
- Produces:
  - `export type SiteAssetKind = 'logo' | 'favicon' | 'avatar' | 'music'`
  - `export interface SiteAssetUpload { url: string; kind: SiteAssetKind; size: number; originalName: string }`
  - `siteApi.uploadSiteAsset(kind: SiteAssetKind, file: File, onProgress?: (event: AxiosProgressEvent) => void): Promise<ApiResponse<SiteAssetUpload>>`

- [x] **Step 1: 写失败测试**

新建 `frontend/src/api/__tests__/site.test.ts`：

```ts
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { siteApi } from '../site'

// post 要被 mock 工厂引用，用 vi.hoisted 避免 TDZ（vi.mock 的工厂会提升到 import 之前）。
const { post } = vi.hoisted(() => ({ post: vi.fn() }))

vi.mock('../index', () => ({
  default: { post, get: vi.fn(), put: vi.fn(), delete: vi.fn() }
}))

describe('siteApi.uploadSiteAsset', () => {
  beforeEach(() => {
    post.mockReset()
  })

  it('posts file and kind to the admin asset endpoint', async () => {
    const envelope = {
      code: 200,
      message: 'OK',
      data: {
        url: '/uploads/site-assets/logo/a.png',
        kind: 'logo',
        size: 3,
        originalName: 'a.png'
      }
    }
    post.mockResolvedValue(envelope)

    const file = new File(['abc'], 'a.png', { type: 'image/png' })
    const onProgress = vi.fn()
    const result = await siteApi.uploadSiteAsset('logo', file, onProgress)

    // 拦截器返回的 envelope 原样透传
    expect(result).toBe(envelope)

    const call = post.mock.calls[0]
    expect(call?.[0]).toBe('/admin/site-assets')

    const body = call?.[1] as FormData
    expect(body).toBeInstanceOf(FormData)
    expect(body.get('kind')).toBe('logo')
    expect((body.get('file') as File).name).toBe('a.png')

    const config = call?.[2] as { onUploadProgress?: unknown; timeout?: number }
    expect(config.onUploadProgress).toBe(onProgress)
    expect(config.timeout).toBeGreaterThan(0)
  })
})
```

- [x] **Step 2: 跑测试确认失败**

```powershell
npm run test:run -- src/api/__tests__/site.test.ts
```

Expected: 失败（`siteApi.uploadSiteAsset is not a function`）。

- [x] **Step 3: 实现**

`frontend/src/api/site.ts` 顶部补 `import type { AxiosProgressEvent } from 'axios'`，并新增类型：

```ts
export type SiteAssetKind = 'logo' | 'favicon' | 'avatar' | 'music'

export interface SiteAssetUpload {
  url: string
  kind: SiteAssetKind
  size: number
  originalName: string
}
```

在 `siteApi` 里 `updateSettings` 之后、`trackVisitor` 之前插入：

```ts
  /**
   * 上传站点品牌资源（头像 / logo / favicon / 音乐）。
   * 与 /media/upload 不同：这条通道原样落盘，不会把图片重编码成 jpg。
   */
  uploadSiteAsset(
    kind: SiteAssetKind,
    file: File,
    onProgress?: (event: AxiosProgressEvent) => void
  ): Promise<ApiResponse<SiteAssetUpload>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('kind', kind)
    return api.post('/admin/site-assets', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
      onUploadProgress: onProgress
    })
  },
```

- [x] **Step 4: 测试与类型检查**

```powershell
npm run test:run -- src/api/__tests__/site.test.ts
npm run type-check
```

Expected: 1 test passed；`vue-tsc` 无错误。

- [x] **Step 5: 提交**

```bash
git add frontend/src/api/site.ts frontend/src/api/__tests__/site.test.ts
git commit -m "feat: add the site asset upload API"
```

---

### Task 2: `SiteAssetField.vue`

**Files:**
- Create: `frontend/src/components/admin/SiteAssetField.vue`
- Create: `frontend/src/components/admin/__tests__/SiteAssetField.test.ts`

**Interfaces:**
- Consumes: `siteApi.uploadSiteAsset()`（Task 1）
- Produces: 组件 `SiteAssetField`
  - props：`modelValue: string`、`kind: SiteAssetKind`、`label: string`、`hint?: string`
  - emits：`update:modelValue: [value: string]`

- [x] **Step 1: 写失败测试**

新建 `frontend/src/components/admin/__tests__/SiteAssetField.test.ts`：

```ts
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SiteAssetField from '../SiteAssetField.vue'

const { uploadSiteAsset } = vi.hoisted(() => ({ uploadSiteAsset: vi.fn() }))

vi.mock('@/api/site', () => ({
  siteApi: { uploadSiteAsset }
}))

const UPLOAD_RESPONSE = {
  code: 200,
  message: 'OK',
  data: {
    url: '/uploads/site-assets/logo/mock.png',
    kind: 'logo',
    size: 12,
    originalName: 'logo.png'
  }
}

function mountField(props: Record<string, unknown> = {}) {
  return mount(SiteAssetField, {
    props: { modelValue: '', kind: 'logo', label: '站点 Logo', ...props }
  })
}

/** jsdom 里 file input 的 files 是只读的，直接改属性再派发 change。 */
async function selectFile(wrapper: ReturnType<typeof mountField>, file: File) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', {
    value: { 0: file, length: 1 },
    configurable: true
  })
  await input.trigger('change')
}

describe('SiteAssetField', () => {
  beforeEach(() => {
    uploadSiteAsset.mockReset()
  })

  it('maps accept to the kind whitelist', () => {
    expect(mountField({ kind: 'favicon' }).find('input[type="file"]').attributes('accept')).toBe(
      '.png,.ico,.svg'
    )
    expect(mountField({ kind: 'music' }).find('input[type="file"]').attributes('accept')).toBe(
      '.mp3,.ogg,.wav,.m4a,.aac'
    )
  })

  it('rejects extensions outside the whitelist without a request', async () => {
    const wrapper = mountField()
    await selectFile(wrapper, new File(['x'], 'logo.gif', { type: 'image/gif' }))

    expect(wrapper.find('.asset-error').text()).toContain('仅支持')
    expect(uploadSiteAsset).not.toHaveBeenCalled()
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('rejects files over the image cap without a request', async () => {
    const wrapper = mountField()
    const oversized = new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'big.png')
    await selectFile(wrapper, oversized)

    expect(wrapper.find('.asset-error').text()).toContain('2MB')
    expect(uploadSiteAsset).not.toHaveBeenCalled()
  })

  it('uploads a valid file and emits the returned url', async () => {
    uploadSiteAsset.mockResolvedValue(UPLOAD_RESPONSE)
    const wrapper = mountField()
    const file = new File(['png-bytes'], 'logo.png', { type: 'image/png' })

    await selectFile(wrapper, file)
    await flushPromises()

    expect(uploadSiteAsset).toHaveBeenCalledTimes(1)
    const call = uploadSiteAsset.mock.calls[0]
    expect(call?.[0]).toBe('logo')
    expect(call?.[1]).toBe(file)
    expect(typeof call?.[2]).toBe('function')
    expect(wrapper.emitted('update:modelValue')).toEqual([
      ['/uploads/site-assets/logo/mock.png']
    ])
  })

  it('shows the API message inline when the upload fails', async () => {
    uploadSiteAsset.mockRejectedValue(
      Object.assign(new Error('bad'), { userMessage: '文件内容与扩展名不匹配，请上传真实文件' })
    )
    const wrapper = mountField()

    await selectFile(wrapper, new File(['x'], 'logo.png', { type: 'image/png' }))
    await flushPromises()

    expect(wrapper.find('.asset-error').text()).toContain('文件内容与扩展名不匹配')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('emits manual url edits', async () => {
    const wrapper = mountField({ modelValue: '/uploads/site-assets/logo/old.png' })
    await wrapper.find('.asset-input').setValue('https://cdn.example.com/new.png')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([
      'https://cdn.example.com/new.png'
    ])
  })

  it('renders a square preview for the current image value', () => {
    expect(mountField().find('.asset-thumb-empty').text()).toBe('未设置')

    const wrapper = mountField({ modelValue: '/uploads/site-assets/logo/current.png' })
    expect(wrapper.find('.asset-thumb-image').attributes('src')).toBe(
      '/uploads/site-assets/logo/current.png'
    )
  })

  it('renders the music filename and duration once metadata is read', async () => {
    const wrapper = mountField({
      kind: 'music',
      modelValue: '/uploads/site-assets/music/song.mp3'
    })
    const audio = wrapper.find('.asset-audio-player')
    expect(audio.attributes('src')).toBe('/uploads/site-assets/music/song.mp3')

    Object.defineProperty(audio.element, 'duration', { value: 204, configurable: true })
    await audio.trigger('loadedmetadata')

    const meta = wrapper.find('.asset-audio-meta').text()
    expect(meta).toContain('song.mp3')
    expect(meta).toContain('3:24')
  })
})
```

- [x] **Step 2: 跑测试确认失败**

```powershell
npm run test:run -- src/components/admin/__tests__/SiteAssetField.test.ts
```

Expected: 失败（模块不存在）。

- [x] **Step 3: 写组件**

新建 `frontend/src/components/admin/SiteAssetField.vue`：

```vue
<script setup lang="ts">
/**
 * SiteAssetField.vue — 站点品牌资源字段
 *
 * 预览 +手动 URL 输入 + 上传按钮。服务端才是校验权威（扩展名白名单 + 魔数 + 体积），
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
  if (!Number.isFinite(seconds) || seconds <= 0) return ''
  const total = Math.round(seconds)
  const minutes = Math.floor(total / 60)
  const rest = total % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function handleInput(event: Event): void {
  uploadedName.value = ''
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
  } catch (err) {
    error.value =
      (err as Error & { userMessage?: string }).userMessage || '上传失败，请稍后重试。'
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
      <span class="asset-audio-meta">{{ uploadedName || fileName(modelValue)
      }}<template v-if="duration"> · {{ duration }}</template></span>
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
.asset-upload-button { align-self: flex-start; padding: 8px 13px; border: 0; border-radius: var(--radius-sm); background: var(--primary); color: white; font-size: .78rem; font-weight: 700; cursor: pointer; }
.asset-upload-button:disabled { opacity: .6; cursor: wait; }
.asset-file-input { display: none; }
.asset-audio { display: grid; gap: 6px; }
.asset-audio-player { width: 100%; }
.asset-audio-meta { color: var(--text-muted); font-size: .72rem; }
.asset-error { margin: 0; color: var(--error); font-size: .72rem; }
.asset-hint { margin: 0; color: var(--text-muted); font-size: .7rem; line-height: 1.5; }
</style>
```

- [x] **Step 4: 跑测试与类型检查**

```powershell
npm run test:run -- src/components/admin/__tests__/SiteAssetField.test.ts
npm run type-check
```

Expected: 8 tests passed；`vue-tsc` 无错误。

- [x] **Step 5: 提交**

```bash
git add frontend/src/components/admin/SiteAssetField.vue frontend/src/components/admin/__tests__/SiteAssetField.test.ts
git commit -m "feat: add the site asset upload field component"
```

---

### Task 3: `Settings.vue` 接入四个字段

**Files:**
- Modify: `frontend/src/views/admin/Settings.vue`
- Create: `frontend/src/views/admin/__tests__/Settings.test.ts`

**Interfaces:**
- Consumes: `SiteAssetField`（Task 2）、`useSiteStore().applySettings()`、`siteApi.getSettings()/updateSettings()`
- Produces: 设置页四个资源字段的绑定与乐观更新行为

- [x] **Step 1: 写失败测试**

新建 `frontend/src/views/admin/__tests__/Settings.test.ts`：

```ts
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Settings from '../Settings.vue'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'
import { useSiteStore } from '@/stores/site'

const { getSettings, updateSettings } = vi.hoisted(() => ({
  getSettings: vi.fn(),
  updateSettings: vi.fn()
}))

vi.mock('@/api/site', () => ({
  siteApi: { getSettings, updateSettings, uploadSiteAsset: vi.fn() }
}))

const STATIC_FAVICON = '<link rel="icon" type="image/svg+xml" href="/favicon.svg">'

let pinia: ReturnType<typeof createPinia>

async function mountSettings() {
  const wrapper = mount(Settings, { global: { plugins: [pinia] } })
  await flushPromises()
  return wrapper
}

function requireField(wrapper: Awaited<ReturnType<typeof mountSettings>>, kind: string) {
  const field = wrapper
    .findAllComponents(SiteAssetField)
    .find((candidate) => candidate.props('kind') === kind)
  if (!field) throw new Error(`missing SiteAssetField: ${kind}`)
  return field
}

describe('Settings brand asset fields', () => {
  beforeEach(() => {
    document.head.innerHTML = STATIC_FAVICON
    pinia = createPinia()
    setActivePinia(pinia)
    getSettings.mockReset()
    updateSettings.mockReset()
    getSettings.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ...useSiteStore().settings }
    })
  })

  afterEach(() => {
    document.head.innerHTML = ''
  })

  it('binds all four asset fields to the loaded settings', async () => {
    getSettings.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ...useSiteStore().settings, authorAvatar: '/uploads/site-assets/avatar/me.png' }
    })
    const wrapper = await mountSettings()

    const kinds = wrapper.findAllComponents(SiteAssetField).map((field) => field.props('kind'))
    expect(kinds).toEqual(expect.arrayContaining(['avatar', 'logo', 'favicon', 'music']))
    expect(requireField(wrapper, 'avatar').props('modelValue')).toBe(
      '/uploads/site-assets/avatar/me.png'
    )
    expect(wrapper.text()).toContain('站点 Logo')
    expect(wrapper.text()).toContain('网站图标')
  })

  it('applies an uploaded url optimistically, then persists it on save', async () => {
    const wrapper = await mountSettings()
    const favicon = requireField(wrapper, 'favicon')

    favicon.vm.$emit('update:modelValue', '/uploads/site-assets/favicon/new.ico')
    await nextTick()

    // 乐观更新：store 立刻换新，favicon link 也立刻注入
    const store = useSiteStore()
    expect(store.settings.faviconUrl).toBe('/uploads/site-assets/favicon/new.ico')
    const injected = document.head.querySelector<HTMLLinkElement>('link[data-runtime]')
    expect(injected?.getAttribute('href')).toBe('/uploads/site-assets/favicon/new.ico')

    updateSettings.mockResolvedValue({ code: 200, message: 'OK', data: { ...store.settings } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(updateSettings).toHaveBeenCalledWith(
      expect.objectContaining({ faviconUrl: '/uploads/site-assets/favicon/new.ico' })
    )
  })
})
```

- [x] **Step 2: 跑测试确认失败**

```powershell
npm run test:run -- src/views/admin/__tests__/Settings.test.ts
```

Expected: 失败（页面里还没有 `SiteAssetField`）。

- [x] **Step 3: 改 `Settings.vue`**

模板部分：

1. 第 01 面板里，把现有的：

```html
        <label>头像地址<input v-model.trim="form.authorAvatar" placeholder="https://… 或 /uploads/…" /></label>
```

替换为：

```html
        <SiteAssetField :model-value="form.authorAvatar" kind="avatar" label="作者头像" hint="支持上传或手填外链地址。" @update:model-value="applyAsset('authorAvatar', $event)" />
        <SiteAssetField :model-value="form.siteLogoUrl" kind="logo" label="站点 Logo" hint="公开页顶栏与后台侧边栏的品牌图标；留空使用内置图标。" @update:model-value="applyAsset('siteLogoUrl', $event)" />
        <SiteAssetField :model-value="form.faviconUrl" kind="favicon" label="网站图标" hint="浏览器标签页图标；留空使用默认图标。" @update:model-value="applyAsset('faviconUrl', $event)" />
```

2. 第 03 面板里，把现有的：

```html
        <label>音乐文件 URL<input v-model.trim="form.musicUrl" placeholder="https://… 或 /uploads/music.mp3" /><small>音频请使用可公开访问的 MP3/OGG 地址。</small></label>
```

替换为：

```html
        <SiteAssetField :model-value="form.musicUrl" kind="music" label="背景音乐文件" hint="支持 MP3/OGG/WAV/M4A/AAC，最大 20MB；留空则播放器不出现。" @update:model-value="applyAsset('musicUrl', $event)" />
```

脚本部分：

```ts
import SiteAssetField from '@/components/admin/SiteAssetField.vue'

type AssetSettingKey = 'authorAvatar' | 'siteLogoUrl' | 'faviconUrl' | 'musicUrl'

/**
 * 上传成功先写表单 + 同步 store（导航栏 Logo、favicon 立即换新），
 * 真正落库要等「保存设置」——与 spec 的乐观更新约定一致。
 */
function applyAsset(key: AssetSettingKey, url: string): void {
  const patch: Partial<SiteSettings> = { [key]: url.trim() }
  Object.assign(form, patch)
  siteStore.applySettings(patch)
}
```

- [x] **Step 4: 跑测试与类型检查**

```powershell
npm run test:run -- src/views/admin/__tests__/Settings.test.ts
npm run type-check
```

Expected: 2 tests passed；`vue-tsc` 无错误。

- [x] **Step 5: 提交**

```bash
git add frontend/src/views/admin/Settings.vue frontend/src/views/admin/__tests__/Settings.test.ts
git commit -m "feat: wire brand asset uploads into site settings"
```

---

### Task 4: 全量校验与文档

- [x] **Step 1: 全量校验**

```powershell
npm run lint:check
npm run type-check
npm run test:run
npm run build
```

Expected: 全绿；`build` 会先跑 `vue-tsc` 再打包。

- [x] **Step 2: 更新 `CHANGELOG.md`**

在文件顶部新增一节（沿用既有中文分节）：

```markdown
## 未发布 — 站点资源上传控件（前端）(2026-10-15)

承接「资源上传通道（后端）」：后台「网站设置」页新增四个品牌资源字段，支持上传与预览。

### 新增

- **`SiteAssetField.vue` 通用资源字段**：预览 + 手动 URL 输入 + 上传按钮；按 kind 限定 `accept` 与体积上限（图片 2MB、音乐 20MB），预检失败不发请求，错误行内展示
- **上传 API** `siteApi.uploadSiteAsset()`：multipart 提交到 `POST /api/admin/site-assets`，带上传进度回调

### 说明

- 上传成功即写入表单并同步 `siteStore`（顶栏 Logo、favicon 立即换新），点「保存设置」才通过 `PUT /api/admin/site-settings` 落库
- 音乐字段预览显示文件名与时长；图片字段显示方形缩略图；远端 CDN 手填 URL 的能力保留

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/api/site.ts` | 新增 `SiteAssetKind` / `SiteAssetUpload` 类型与 `uploadSiteAsset()` |
| `frontend/src/components/admin/SiteAssetField.vue` | 新增：站点资源上传字段组件 |
| `frontend/src/views/admin/Settings.vue` | 四个字段接入 + 乐观更新 |
| `frontend/src/api/__tests__/site.test.ts` | 新增：上传 API 单测 |
| `frontend/src/components/admin/__tests__/SiteAssetField.test.ts` | 新增：字段组件单测 |
| `frontend/src/views/admin/__tests__/Settings.test.ts` | 新增：设置页集成测试 |
```

- [x] **Step 3: 检查 `frontend/README.md`**

看「Project Structure」里 `components/` 与 `views/admin` 的清单是否提到设置页资源字段；若有对应描述就补一行，没有就不硬造。

- [x] **Step 4: （可选）浏览器端到端验证**

仓库有 `backend/.env` 与 SQLite 库，可真实起服务验证：

```powershell
# 终端 1（backend/）
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
& $mvn spring-boot:run
# 终端 2（frontend/）
npm run dev
```

登录后台 → 网站设置 → 上传 PNG 头像、SVG logo、ICO favicon、MP3 音乐 → 验证即时预览、顶栏 logo 与 favicon 立即换新 → 保存后刷新仍在。

- [x] **Step 5: 提交**

```bash
git add CHANGELOG.md frontend/README.md
git commit -m "docs: describe the frontend site asset upload controls"
```

---

## 完成后

- 后台「网站设置」四个资源字段可用：上传、预览、手填 URL、行内错误、保存后持久化
- 上传成功到保存之间，公开页顶栏 Logo 与 favicon 已即时生效
- 前端 `lint:check` / `type-check` / `test:run` / `build` 全绿

**下一批（E）**：文章页阅读体验——代码复制/折叠、标题锚点、移动端目录抽屉、图片灯箱、字数统计与「最后更新」。计划另起。

---

## 执行修订

### 实施记录（2026-10-15）

- Task 1–3 由主会话按 TDD 直接实施（子代理渠道限流），每个任务独立提交：`f7d9c69`（上传 API）、`a9df493`（字段组件）、`b643963`（设置页接入）。
- Task 2 实现偏离一处：音乐预览的"文件名 + 时长"改用 `audioMeta` computed 拼装，避免模板里跨行插值；行为与测试目标不变。
- Task 2 测试偏离一处：`tsconfig` 的 lib 不含 ES2022，`Array.prototype.at` 过不了 `vue-tsc`，改为取 emit 数组最后一条索引。
- Task 3 的 `applyAsset` 用 `Partial<SiteSettings>` patch + `Object.assign` 写表单，规避联合键直接赋值在类型系统里的歧义。
- 全量验证：`npm run lint:check`、`npm run type-check`、`npm run test:run`（**167/167**）、`npm run build` 全部通过。
- 端到端（真实后端 + Chrome）：管理员登录 → 设置页上传四种资源 → 上传后 favicon 即时换新 → 保存成功 → 刷新后四个字段全部持久化 → 公开页顶栏 Logo 与 favicon 生效，无控制台错误。
- 验证后已删除数据库中本次写入的四个设置行与 `backend/uploads/site-assets` 下的测试文件（经比对主库 WAL 时间戳确认测试前这些行与文件均不存在），本地环境已还原。

### 审查修复（2026-10-15，`6f1950f`）

第一轮实现后做了只读代码审查，两处重要项与若干小项已修复：

- **上传在途锁**：`SiteAssetField` 的 URL 输入在 `uploading` 期间禁用，避免慢上传的响应覆盖用户刚输入的内容；另补"在途忽略二次选择"的用例。
- **上传与手填语义分离**：新增 `uploaded` 事件，设置页只在真实上传结果上同步 `siteStore`（Logo/favicon 立即生效）；手填 URL 仍只写表单、保存时随其余设置落库，避免逐字输入污染全局 store。
- **陈旧状态清理**：手动编辑会同时清掉 `error` 与旧 `duration`；`formatDuration` 对 <1 秒的值返回空串。
- **补齐测试**：新增 8 个用例（在途去重、上传锁、音乐 20MB 档、大小写扩展名、空响应、原始文件名展示、手工编辑清理、四键路由 + 手填不同步），前端全量 **175/175**。
- 未采纳的一条 Minor：`Content-Type: multipart/form-data` 头保留 —— 与 `media.ts` / `gallery.ts` 的既有上传调用保持一致，axios 会自行覆盖 boundary。

### 复核补充（2026-10-15）

- 审查会话复核确认两个 Important 均已真实修复、`uploaded` 事件切分无重复写入；两个残留 Minor 一并处理：四字段的手填路由全部纳入集成测试（此前只覆盖 favicon）；禁用态 URL 输入框补 `opacity` + `cursor` 视觉反馈。
