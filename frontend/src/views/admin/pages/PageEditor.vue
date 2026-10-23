<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useSiteStore } from '@/stores/site'
import { pageCapability } from '@/utils/pageCatalog'
import { safeBannerUrl } from '@/composables/usePageConfig'
import { usePageEditor } from './usePageEditor'
import PagePreview from './PagePreview.vue'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'
import type {
  BannerFit,
  BannerHeight,
  BannerLayout,
  BannerPosition,
  BannerTone,
  PageBanner,
  PageStyle
} from '@/types/page'
const site = useSiteStore()
const {
  editor,
  config,
  slug,
  loading,
  busy,
  error,
  success,
  dirty,
  hasUnpublishedChanges,
  reload,
  write,
  reset,
  ConfirmDialog
} = usePageEditor()
const tab = ref<'content' | 'style' | 'preview'>('content')
const capability = computed(() => pageCapability(editor.value?.id || 'about'))
function overridden(key: string) {
  return key === 'title' || key === 'description'
    ? config.value[key] != null
    : config.value.text?.[key] != null
}
function fieldValue(key: string): string {
  return (
    (key === 'title' || key === 'description' ? config.value[key] : config.value.text?.[key]) ?? ''
  )
}
function setField(key: string, value: string | null) {
  if (key === 'title' || key === 'description') config.value = { ...config.value, [key]: value }
  else {
    const text = { ...config.value.text }
    if (value === null) delete text[key]
    else text[key] = value
    config.value = { ...config.value, text }
  }
}
function setStyle(key: keyof PageStyle, value: string) {
  config.value = { ...config.value, style: { ...config.value.style, [key]: value } }
}
/* ---------------------------------------------------------------------------
   横幅背景
   ---------------------------------------------------------------------------
   banner 是一个对象，不能走 setStyle(key: keyof PageStyle, value: string) 那条
   字符串通道 —— 这里单独维护一个 setBanner(patch)，逐字段合并到 style.banner。
   背景图地址先过白名单（safeBannerUrl）：非法地址只停在输入框里，不写进草稿。 */
const FIT_OPTIONS: BannerFit[] = ['cover', 'contain']
const POSITION_OPTIONS: BannerPosition[] = ['center', 'top', 'bottom']
const HEIGHT_OPTIONS: BannerHeight[] = ['compact', 'standard', 'tall']
const TONE_OPTIONS: BannerTone[] = ['auto', 'light', 'dark']
const LAYOUT_LABELS: Record<BannerLayout, string> = {
  split: '左右（文字在左，背景图在右）',
  centered: '居中（背景图铺满，文字压在上面）',
  stacked: '上下（背景图在上，文字在下）'
}
/** 输入框里的原始文本：非法地址只停在这里，不进草稿。 */
const bannerImageDraft = ref('')
/** 背景图地址不合法时的提示。 */
const bannerError = ref('')
const banner = computed(() => config.value.style?.banner ?? null)
const bannerFit = computed(() => banner.value?.fit ?? 'cover')
const bannerPosition = computed(() => banner.value?.position ?? 'center')
const bannerHeight = computed(() => banner.value?.height ?? 'standard')
const bannerOverlay = computed(() => banner.value?.overlay ?? 0)
const bannerTone = computed(() => banner.value?.tone ?? 'auto')
const bannerLayout = computed(() => banner.value?.layout ?? 'split')
/** 该页面允许的排布；没有 banner 能力位时为空数组（横幅区块整体不显示）。 */
const bannerLayouts = computed(() => capability.value.banner?.layouts ?? [])
watch(
  () => config.value.style?.banner?.image ?? '',
  (value) => {
    if (value !== bannerImageDraft.value) bannerImageDraft.value = value
  },
  { immediate: true }
)
function setBanner(patch: Partial<PageBanner>) {
  const style = { ...config.value.style }
  style.banner = { ...(style.banner ?? {}), ...patch }
  config.value = { ...config.value, style }
}
/**
 * 背景图地址。
 *
 * 只接受 http(s):// 或 /uploads/ 开头、且不含空白/引号/圆括号/反斜杠的地址 ——
 * 与站点背景设置同一套白名单：这个值会被拼进 `url("…")`，校验必须在前端也做一次。
 */
function setBannerImage(value: string) {
  bannerImageDraft.value = value
  const trimmed = value.trim()
  if (!trimmed) {
    bannerError.value = ''
    setBanner({ image: null })
    return
  }
  if (!safeBannerUrl(trimmed)) {
    bannerError.value = '背景图地址不合法'
    return
  }
  bannerError.value = ''
  setBanner({ image: trimmed })
}
function clearBanner() {
  bannerImageDraft.value = ''
  bannerError.value = ''
  const style = { ...config.value.style }
  delete style.banner
  config.value = { ...config.value, style }
}
function setBannerFit(value: string) {
  const fit = FIT_OPTIONS.find((item) => item === value)
  if (fit) setBanner({ fit })
}
function setBannerPosition(value: string) {
  const position = POSITION_OPTIONS.find((item) => item === value)
  if (position) setBanner({ position })
}
function setBannerHeight(value: string) {
  const height = HEIGHT_OPTIONS.find((item) => item === value)
  if (height) setBanner({ height })
}
function setBannerTone(value: string) {
  const tone = TONE_OPTIONS.find((item) => item === value)
  if (tone) setBanner({ tone })
}
function setBannerLayout(value: string) {
  const layout = bannerLayouts.value.find((item) => item === value)
  if (layout) setBanner({ layout })
}
function setBannerOverlay(value: string) {
  const parsed = Number.parseInt(value, 10)
  if (!Number.isFinite(parsed)) return
  setBanner({ overlay: Math.min(80, Math.max(0, parsed)) })
}
/** 保存前再校验一次背景图地址：非法的地址不落库。 */
function requestWrite(action: 'save' | 'publish') {
  const image = bannerImageDraft.value.trim()
  if (image && !safeBannerUrl(image)) {
    bannerError.value = '背景图地址不合法，请修正后再保存。'
    return
  }
  void write(action)
}
</script>
<template>
  <div class="page-editor">
    <header class="editor-heading">
      <div>
        <router-link :to="`${site.adminRoot}/pages`" class="back-link">返回页面管理</router-link>
        <h1>{{ editor?.builtin ? editor.label : editor?.draft.config.title || '编辑页面' }}</h1>
        <p>草稿与公开版本分开保存；预览不会改变线上内容。</p>
      </div>
      <div v-if="editor" class="editor-actions">
        <button
          class="btn btn-secondary"
          data-action="save"
          type="button"
          :disabled="busy || !dirty"
          @click="requestWrite('save')"
        >
          {{ busy ? '处理中…' : '保存草稿' }}</button
        ><button
          class="btn btn-primary"
          data-action="publish"
          type="button"
          :disabled="busy || dirty"
          :title="dirty ? '请先保存草稿' : '发布当前已保存版本'"
          @click="requestWrite('publish')"
        >
          发布页面
        </button>
      </div>
    </header>
    <p v-if="error" class="editor-alert" role="alert">
      {{ error }} <button type="button" :disabled="busy" @click="reload">重新加载</button>
    </p>
    <p v-if="bannerError" class="editor-alert" role="alert" data-banner-error>
      {{ bannerError }}
    </p>
    <p v-if="success" class="editor-success" role="status">{{ success }}</p>
    <p v-if="loading" role="status">正在加载页面配置…</p>
    <template v-else-if="editor">
      <div class="draft-status">
        <span>{{
          dirty
            ? '有未保存修改'
            : hasUnpublishedChanges
              ? '草稿有未发布修改'
              : editor.published
                ? '已发布'
                : '尚未发布'
        }}</span
        ><span>草稿版本 {{ editor.draft.revision }}</span
        ><time v-if="editor.draft.updatedAt">{{
          new Date(editor.draft.updatedAt).toLocaleString()
        }}</time>
      </div>
      <div class="editor-tabs" aria-label="页面设置分组">
        <button
          v-for="item in [
            { key: 'content', label: '内容' },
            { key: 'style', label: '样式' },
            { key: 'preview', label: '预览' }
          ] as const"
          :key="item.key"
          type="button"
          :aria-pressed="tab === item.key"
          @click="tab = item.key"
        >
          {{ item.label }}
        </button>
      </div>
      <section v-show="tab === 'content'" class="editor-panel" aria-label="页面内容">
        <label v-if="!editor.builtin" class="page-field"
          >页面地址
          <div class="slug-field">
            <span>/pages/</span
            ><input
              v-model="slug"
              maxlength="60"
              :disabled="editor.draft.everPublished"
              pattern="[a-z0-9]+(-[a-z0-9]+)*"
            />
          </div>
          <small>{{
            editor.draft.everPublished
              ? '首次发布后地址固定，避免原链接失效。'
              : '小写字母、数字和单横线，2–60位。'
          }}</small></label
        >
        <div v-for="field in capability.fields" :key="field.key" class="page-field">
          <label class="override-switch"
            ><input
              :data-override="field.key"
              type="checkbox"
              :checked="overridden(field.key)"
              :disabled="!editor.builtin && field.key === 'title'"
              @change="setField(field.key, ($event.target as HTMLInputElement).checked ? '' : null)"
            /><span>自定义{{ field.label }}</span
            ><small v-if="!overridden(field.key)">继承默认</small></label
          ><label v-if="overridden(field.key)" :for="`page-${field.key}`" class="sr-only">{{
            field.label
          }}</label
          ><textarea
            v-if="overridden(field.key) && field.multiline"
            :id="`page-${field.key}`"
            :value="fieldValue(field.key)"
            rows="3"
            :maxlength="field.limit"
            @input="setField(field.key, ($event.target as HTMLTextAreaElement).value)"
          /><input
            v-else-if="overridden(field.key)"
            :id="`page-${field.key}`"
            :value="fieldValue(field.key)"
            :maxlength="field.limit"
            @input="setField(field.key, ($event.target as HTMLInputElement).value)"
          /><small v-if="overridden(field.key)"
            >{{ fieldValue(field.key).length }} / {{ field.limit }}；介绍留空会隐藏该说明。</small
          >
        </div>
        <label v-if="capability.body" class="page-field" for="page-body"
          >独立正文（Markdown）<textarea
            id="page-body"
            :value="config.body ?? ''"
            rows="14"
            maxlength="10000"
            placeholder="使用 Markdown 编写正文，可在预览中查看效果。"
            @input="config = { ...config, body: ($event.target as HTMLTextAreaElement).value }"
          /><small>{{ (config.body || '').length }} / 10000；不执行脚本或自定义 CSS。</small></label
        >
        <fieldset class="module-fields">
          <legend>显示模块</legend>
          <label v-for="item in capability.modules" :key="item.key"
            ><input
              type="checkbox"
              :checked="config.modules?.[item.key] ?? true"
              @change="
                config = {
                  ...config,
                  modules: {
                    ...config.modules,
                    [item.key]: ($event.target as HTMLInputElement).checked
                  }
                }
              "
            />{{ item.label }}</label
          >
        </fieldset>
        <p v-if="capability.dynamic" class="field-hint">
          这里调整详情模板；真实文章和标签名称、正文继续通过原管理功能编辑。
        </p>
      </section>
      <section v-show="tab === 'style'" class="editor-panel style-fields" aria-label="页面样式">
        <p class="field-hint">仅作用于本页；未覆盖时保持原有全站主题和比例。</p>
        <label class="page-field"
          >强调色<input
            :value="config.style?.accent || ''"
            placeholder="继承全站，例如 #3E76CB"
            maxlength="7"
            @input="setStyle('accent', ($event.target as HTMLInputElement).value)" /></label
        ><label class="page-field"
          >背景风格<select
            :value="config.style?.background || 'inherit'"
            @change="setStyle('background', ($event.target as HTMLSelectElement).value)"
          >
            <option value="inherit">继承全站</option>
            <option value="plain">纯净</option>
            <option value="soft">柔和</option>
            <option value="paper">纸感</option>
          </select></label
        ><label class="page-field"
          >内容宽度<select
            :value="config.style?.width || 'inherit'"
            @change="setStyle('width', ($event.target as HTMLSelectElement).value)"
          >
            <option value="inherit">继承默认</option>
            <option value="narrow">窄</option>
            <option value="standard">标准</option>
            <option value="wide">宽</option>
          </select></label
        ><label class="page-field"
          >内容疏密<select
            :value="config.style?.density || 'inherit'"
            @change="setStyle('density', ($event.target as HTMLSelectElement).value)"
          >
            <option value="inherit">继承默认</option>
            <option value="compact">紧凑</option>
            <option value="comfortable">舒展</option>
          </select></label
        >
        <fieldset v-if="capability.banner" class="banner-fields" data-banner-fields>
          <legend>横幅背景</legend>
          <p class="field-hint">
            只影响{{ capability.label }}页的这块区域；横幅上的文字仍在「内容」标签里改。
          </p>
          <SiteAssetField
            :model-value="bannerImageDraft"
            kind="background"
            label="背景图"
            hint="可以上传图片，也可以直接填写地址；只接受 https://… 或 /uploads/… 开头的地址。"
            @update:model-value="setBannerImage"
          />
          <p v-if="bannerError" class="banner-error" role="alert" data-banner-error-inline>
            {{ bannerError }}
          </p>
          <div class="banner-grid">
            <label class="page-field"
              >图片填充方式<select
                data-banner-field="fit"
                :value="bannerFit"
                @change="setBannerFit(($event.target as HTMLSelectElement).value)"
              >
                <option value="cover">填满裁切</option>
                <option value="contain">完整显示</option>
              </select></label
            >
            <label class="page-field"
              >图片焦点<select
                data-banner-field="position"
                :value="bannerPosition"
                @change="setBannerPosition(($event.target as HTMLSelectElement).value)"
              >
                <option value="center">居中</option>
                <option value="top">靠上</option>
                <option value="bottom">靠下</option>
              </select></label
            >
            <label class="page-field"
              >横幅高度<select
                data-banner-field="height"
                :value="bannerHeight"
                @change="setBannerHeight(($event.target as HTMLSelectElement).value)"
              >
                <option value="compact">紧凑</option>
                <option value="standard">标准</option>
                <option value="tall">高</option>
              </select></label
            >
            <label class="page-field"
              >文字明暗<select
                data-banner-field="tone"
                :value="bannerTone"
                @change="setBannerTone(($event.target as HTMLSelectElement).value)"
              >
                <option value="auto">自动</option>
                <option value="light">浅色</option>
                <option value="dark">深色</option>
              </select></label
            >
            <label class="page-field"
              >内容排布<select
                data-banner-field="layout"
                :value="bannerLayout"
                @change="setBannerLayout(($event.target as HTMLSelectElement).value)"
              >
                <option v-for="item in bannerLayouts" :key="item" :value="item">
                  {{ LAYOUT_LABELS[item] }}
                </option>
              </select></label
            >
            <label class="page-field"
              >遮罩强度<input
                type="number"
                data-banner-field="overlay"
                min="0"
                max="80"
                step="1"
                :value="bannerOverlay"
                @input="setBannerOverlay(($event.target as HTMLInputElement).value)"
            /></label>
          </div>
          <p class="field-hint">
            「居中」排布时文字压在图上，文字明暗才起作用；「左右」「上下」排布的文字落在纸面上。
          </p>
          <button
            type="button"
            class="btn btn-ghost"
            data-action="clear-banner"
            :disabled="busy"
            @click="clearBanner"
          >
            清除横幅
          </button>
        </fieldset>
      </section>
      <PagePreview
        v-if="tab === 'preview'"
        :key="editor.id"
        :id="editor.id"
        :config="config"
        :slug="slug"
        :updated-at="editor.draft.updatedAt"
      />
      <footer class="editor-footer">
        <button
          v-if="editor.builtin"
          class="btn btn-ghost"
          data-action="reset"
          type="button"
          :disabled="busy"
          @click="reset"
        >
          恢复默认（仅草稿）</button
        ><template v-else
          ><button
            v-if="editor.published"
            class="btn btn-secondary"
            data-action="unpublish"
            type="button"
            :disabled="busy"
            @click="write('unpublish')"
          >
            下线页面</button
          ><button
            class="btn btn-ghost delete-button"
            data-action="delete"
            type="button"
            :disabled="busy"
            @click="write('delete')"
          >
            删除内容页
          </button></template
        ><small>修改后先保存草稿，再明确发布。</small>
      </footer>
    </template>
    <ConfirmDialog />
  </div>
</template>
<style scoped>
.page-editor {
  display: grid;
  min-width: 0;
  gap: 18px;
  color: var(--text-primary);
}
.editor-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}
.back-link {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.editor-heading h1 {
  font-size: var(--text-2xl);
  overflow-wrap: anywhere;
}
.editor-heading p {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.editor-alert,
.editor-success {
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  font-size: var(--text-sm);
}
.editor-alert {
  color: var(--error);
}
.editor-alert button {
  min-height: 44px;
  padding-inline: 10px;
  text-decoration: underline;
}
.editor-success {
  color: var(--text-secondary);
  background: var(--surface-secondary);
}
.draft-status {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: var(--text-xs);
  color: var(--text-secondary);
}
.editor-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--border-light);
}
.editor-tabs button {
  min-width: 65px;
  min-height: 44px;
  padding: 10px 16px;
  border-bottom: 2px solid transparent;
  color: var(--text-secondary);
}
.editor-tabs [aria-pressed='true'] {
  border-color: var(--accent);
  color: var(--accent);
}
.editor-panel {
  display: grid;
  gap: 22px;
  padding: 24px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.page-field {
  display: grid;
  gap: 8px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.page-field input:not([type='checkbox']),
textarea,
select {
  width: 100%;
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-base);
}
.page-field textarea {
  resize: vertical;
  line-height: 1.7;
}
.page-field small,
.field-hint,
.editor-footer small {
  font-size: var(--text-xs);
  color: var(--text-muted);
  line-height: 1.7;
}
.override-switch {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
}
.override-switch input,
.module-fields input {
  width: 18px;
  height: 18px;
  accent-color: var(--accent);
}
.module-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 22px;
  border: 0;
  padding: 0;
}
.module-fields legend {
  margin-bottom: 12px;
  font-size: var(--text-sm);
}
.banner-fields {
  display: grid;
  gap: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px;
}
.banner-fields legend {
  padding-inline: 6px;
  color: var(--text-primary);
  font-size: var(--text-sm);
}
.banner-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px 18px;
}
.banner-error {
  margin: 0;
  color: var(--error);
  font-size: var(--text-xs);
}
.module-fields label {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  font-size: var(--text-sm);
}
.slug-field {
  display: flex;
  align-items: center;
  gap: 8px;
}
.slug-field span {
  flex-shrink: 0;
  font-size: var(--text-sm);
}
.editor-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.editor-footer small {
  margin-left: auto;
}
.delete-button {
  color: var(--error);
}
input:disabled,
select:disabled {
  opacity: 0.6;
}
@media (max-width: 600px) {
  .editor-panel {
    padding: 18px;
  }
  .editor-actions {
    width: 100%;
  }
  .editor-actions button {
    flex: 1;
  }
  .editor-footer small {
    margin-left: 0;
    width: 100%;
  }
}
</style>
