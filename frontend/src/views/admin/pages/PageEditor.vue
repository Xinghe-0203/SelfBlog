<script setup lang="ts">
import { computed, ref } from 'vue'
import { useSiteStore } from '@/stores/site'
import { pageCapability } from '@/utils/pageCatalog'
import { usePageEditor } from './usePageEditor'
import PagePreview from './PagePreview.vue'
import type { PageStyle, PageSeo } from '@/types/page'
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
const tab = ref<'content' | 'style' | 'seo' | 'preview'>('content')
const capability = computed(() => pageCapability(editor.value?.id || 'about'))
const forcedNoindex = computed(
  () => editor.value?.id === 'search' || editor.value?.id === 'notFound'
)
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
function setSeo(key: keyof PageSeo, value: string | boolean | null) {
  config.value = { ...config.value, seo: { ...config.value.seo, [key]: value } }
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
          @click="write('save')"
        >
          {{ busy ? '处理中…' : '保存草稿' }}</button
        ><button
          class="btn btn-primary"
          data-action="publish"
          type="button"
          :disabled="busy || dirty"
          :title="dirty ? '请先保存草稿' : '发布当前已保存版本'"
          @click="write('publish')"
        >
          发布页面
        </button>
      </div>
    </header>
    <p v-if="error" class="editor-alert" role="alert">
      {{ error }} <button type="button" :disabled="busy" @click="reload">重新加载</button>
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
            { key: 'seo', label: 'SEO' },
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
      </section>
      <section v-show="tab === 'seo'" class="editor-panel" aria-label="页面SEO">
        <p class="field-hint">
          留空会从真实页面信息生成。此处是页面元数据，不保证搜索引擎收录或排名。
        </p>
        <label class="page-field"
          >{{ capability.dynamic ? 'SEO标题后缀（保留真实标题）' : 'SEO标题'
          }}<input
            :value="config.seo?.title || ''"
            maxlength="160"
            @input="setSeo('title', ($event.target as HTMLInputElement).value)" /></label
        ><label class="page-field"
          >SEO描述<textarea
            :value="config.seo?.description || ''"
            rows="3"
            maxlength="400"
            @input="setSeo('description', ($event.target as HTMLTextAreaElement).value)"
          /></label
        ><label class="page-field"
          >分享图地址<input
            :value="config.seo?.image || ''"
            maxlength="2048"
            placeholder="https://… 或 /uploads/…"
            @input="setSeo('image', ($event.target as HTMLInputElement).value)" /></label
        ><label class="page-field"
          >搜索收录<select
            :value="
              forcedNoindex
                ? 'false'
                : config.seo?.indexable == null
                  ? 'inherit'
                  : String(config.seo.indexable)
            "
            :disabled="forcedNoindex"
            @change="
              setSeo(
                'indexable',
                ($event.target as HTMLSelectElement).value === 'inherit'
                  ? null
                  : ($event.target as HTMLSelectElement).value === 'true'
              )
            "
          >
            <option value="inherit">继承页面默认</option>
            <option value="true">允许收录</option>
            <option value="false">不允许收录</option></select
          ><small v-if="forcedNoindex">搜索和404页面始终不允许收录。</small></label
        >
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
