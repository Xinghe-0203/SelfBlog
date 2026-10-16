<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { pageApi } from '@/api/pages'
import { defaultNavigation, usePagesStore } from '@/stores/pages'
import type { NavigationItem, PageEditorDocument } from '@/types/page'
const props = withDefaults(defineProps<{ pages?: PageEditorDocument[] }>(), { pages: () => [] })
const published = usePagesStore()
const items = ref<NavigationItem[]>([])
const revision = ref(0)
const selected = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const saved = ref('')
const success = ref('')
const loaded = ref(false)
let active = true
const dirty = computed(
  () => loaded.value && !loading.value && JSON.stringify(items.value) !== saved.value
)
const options = computed(() =>
  [
    ...defaultNavigation.map((link) => ({ pageId: link.pageId, label: link.label })),
    ...props.pages
      .filter((page) => !page.builtin && page.published)
      .map((page) => ({ pageId: page.id, label: page.published?.config.title || '内容页' }))
  ].filter((page) => !items.value.some((item) => item.pageId === page.pageId))
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await pageApi.getNavigation()
    if (!active) return
    items.value = response.data.items.map((item) => ({ ...item }))
    revision.value = response.data.revision
    saved.value = JSON.stringify(items.value)
    loaded.value = true
  } catch {
    if (active) error.value = '导航设置暂时无法加载，请重试。'
  } finally {
    if (active) loading.value = false
  }
}
async function reload() {
  if (dirty.value && !window.confirm('重新加载会丢弃未保存的导航修改，确定继续吗？')) return
  await load()
}
function add() {
  const item = options.value.find((item) => item.pageId === selected.value)
  if (item) items.value.push({ ...item })
  selected.value = ''
}
function move(index: number, delta: number) {
  const next = index + delta
  if (next < 0 || next >= items.value.length) return
  const item = items.value.splice(index, 1)[0]
  if (item) items.value.splice(next, 0, item)
}
async function save() {
  if (saving.value || loading.value) return
  const payload = items.value.map((item) => ({ ...item }))
  const snapshot = JSON.stringify(payload)
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const response = await pageApi.saveNavigation(revision.value, payload)
    if (!active) return
    revision.value = response.data.revision
    saved.value = snapshot
    success.value = '导航已发布。'
    void published.loadManifest()
  } catch (reason) {
    if (active)
      error.value =
        (reason as { userMessage?: string }).userMessage || '导航保存失败，当前修改已保留。'
  } finally {
    if (active) saving.value = false
  }
}
onBeforeRouteLeave(() => !dirty.value || window.confirm('有未保存的导航修改，确定要离开吗？'))
function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => {
  window.addEventListener('beforeunload', beforeUnload)
  void load()
})
onBeforeUnmount(() => {
  active = false
  window.removeEventListener('beforeunload', beforeUnload)
})
</script>
<template>
  <section class="navigation-editor">
    <h2>公开导航</h2>
    <p>只显示已发布页面；首页入口保留。链接较多时自动放进「更多」，手机菜单保留全部。</p>
    <p v-if="error" role="alert">
      {{ error }}
      <button type="button" class="btn btn-ghost" :disabled="saving" @click="reload">
        重新加载
      </button>
    </p>
    <p v-if="success" role="status">{{ success }}</p>
    <p v-if="loading" role="status">正在加载导航…</p>
    <form v-else @submit.prevent="save">
      <ol>
        <li v-for="(item, index) in items" :key="item.pageId">
          <label class="sr-only" :for="`navigation-${item.pageId}`">导航名称</label
          ><input
            :id="`navigation-${item.pageId}`"
            v-model="item.label"
            :aria-label="`导航名称：${item.label}`"
            maxlength="20"
            required
          />
          <div class="navigation-buttons">
            <button
              type="button"
              :disabled="index === 0"
              :aria-label="`上移${item.label}`"
              @click="move(index, -1)"
            >
              上移</button
            ><button
              type="button"
              :disabled="index === items.length - 1"
              :aria-label="`下移${item.label}`"
              @click="move(index, 1)"
            >
              下移</button
            ><button
              type="button"
              :disabled="item.pageId === 'home'"
              :aria-label="`隐藏${item.label}`"
              @click="items.splice(index, 1)"
            >
              隐藏
            </button>
          </div>
        </li>
      </ol>
      <div v-if="options.length" class="navigation-add">
        <label class="sr-only" for="navigation-add">添加已发布页面</label
        ><select id="navigation-add" v-model="selected">
          <option value="">选择已发布页面</option>
          <option v-for="option in options" :key="option.pageId" :value="option.pageId">
            {{ option.label }}
          </option></select
        ><button class="btn btn-secondary" type="button" :disabled="!selected" @click="add">
          加入导航
        </button>
      </div>
      <button class="btn btn-primary" type="submit" :disabled="saving || !dirty">
        {{ saving ? '保存中…' : '保存并发布导航' }}
      </button>
    </form>
  </section>
</template>
<style scoped>
.navigation-editor {
  padding: 22px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  min-width: 0;
}
.navigation-editor h2 {
  margin-bottom: 12px;
  font-size: 17px;
}
.navigation-editor > p {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}
.navigation-editor [role='alert'] {
  color: var(--error);
}
.navigation-editor ol {
  display: grid;
  gap: 10px;
  margin: 20px 0;
  padding: 0;
  list-style: none;
}
.navigation-editor li {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.navigation-editor input,
.navigation-editor select {
  flex: 1 1 150px;
  min-width: 0;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: 16px;
}
.navigation-buttons {
  display: flex;
  gap: 4px;
}
.navigation-buttons button {
  min-height: 44px;
  padding-inline: 12px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  font-size: 13px;
}
.navigation-buttons button:hover:not(:disabled) {
  background: var(--accent-light);
  color: var(--accent);
}
.navigation-buttons button:disabled {
  opacity: 0.4;
}
.navigation-add {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}
@media (max-width: 600px) {
  .navigation-editor {
    padding: 18px;
  }
}
</style>
