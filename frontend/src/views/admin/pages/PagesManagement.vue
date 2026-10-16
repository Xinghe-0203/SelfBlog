<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageApi } from '@/api/pages'
import { useSiteStore } from '@/stores/site'
import type { PageEditorDocument } from '@/types/page'
import NavigationEditor from './NavigationEditor.vue'
const router = useRouter()
const site = useSiteStore()
const pages = ref<PageEditorDocument[]>([])
const loading = ref(true)
const creating = ref(false)
const error = ref('')
const title = ref('')
const slug = ref('')
let active = true
let request = 0
const builtins = computed(() => pages.value.filter((page) => page.builtin))
const custom = computed(() => pages.value.filter((page) => !page.builtin))
async function load() {
  const current = ++request
  loading.value = true
  error.value = ''
  try {
    const response = await pageApi.list()
    if (active && current === request) pages.value = response.data
  } catch {
    if (active && current === request) error.value = '页面列表暂时无法加载，请重试。'
  } finally {
    if (active && current === request) loading.value = false
  }
}
async function create() {
  if (creating.value) return
  creating.value = true
  error.value = ''
  try {
    const response = await pageApi.create(slug.value.trim(), title.value.trim())
    if (!active) return
    await router.push(`${site.adminRoot}/pages/${response.data.id}`)
  } catch (reason) {
    if (active)
      error.value =
        (reason as { userMessage?: string }).userMessage || '创建失败，请检查地址后重试。'
  } finally {
    if (active) creating.value = false
  }
}
onMounted(load)
onBeforeUnmount(() => {
  active = false
})
</script>
<template>
  <div class="pages-management">
    <header class="page-heading">
      <h1>页面管理</h1>
      <p>逐页编辑内容、样式与SEO；先保存草稿，再发布给读者。</p>
    </header>
    <p v-if="error" class="pages-alert" role="alert">
      {{ error }} <button type="button" class="btn btn-ghost" @click="load">重新加载</button>
    </p>
    <form class="create-page-form" @submit.prevent="create">
      <h2>新增内容页</h2>
      <p>适合关于、项目、友链等独立介绍；新页面首先保存为不公开的草稿。</p>
      <div class="create-fields">
        <label for="new-page-title"
          >页面名称<input
            id="new-page-title"
            v-model="title"
            maxlength="120"
            required
            placeholder="例如：我的项目" /></label
        ><label for="new-page-slug"
          >页面地址
          <div class="slug-input">
            <span>/pages/</span
            ><input
              id="new-page-slug"
              v-model="slug"
              minlength="2"
              maxlength="60"
              pattern="[a-z0-9]+(-[a-z0-9]+)*"
              required
              placeholder="my-projects"
            /></div></label
        ><button
          class="btn btn-primary"
          type="submit"
          :disabled="creating || !title.trim() || !slug.trim()"
        >
          {{ creating ? '创建中…' : '创建草稿' }}
        </button>
      </div>
      <small>地址限小写字母、数字和单横线；首次发布后固定。</small>
    </form>
    <p v-if="loading" role="status">正在整理页面…</p>
    <template v-else
      ><section
        v-for="group in [
          { title: '内置页面与模板', pages: builtins },
          { title: '独立内容页', pages: custom }
        ]"
        :key="group.title"
        class="page-group"
      >
        <h2>{{ group.title }}</h2>
        <p v-if="!group.pages.length" class="empty-pages">还没有内容页，可以从上方创建草稿。</p>
        <ul v-else class="page-list">
          <li v-for="page in group.pages" :key="page.id">
            <div>
              <strong>{{ page.builtin ? page.label : page.draft.config.title }}</strong
              ><small>{{
                page.builtin ? '保留原功能，可恢复默认' : `/pages/${page.draft.slug}`
              }}</small>
            </div>
            <span>{{
              page.published
                ? JSON.stringify(page.draft.config) === JSON.stringify(page.published.config)
                  ? '已发布'
                  : '有未发布修改'
                : '草稿'
            }}</span
            ><router-link :to="`${site.adminRoot}/pages/${page.id}`" class="btn btn-secondary"
              >编辑页面</router-link
            >
          </li>
        </ul>
      </section></template
    >
    <NavigationEditor v-if="!loading" :pages="pages" />
  </div>
</template>
<style scoped>
.pages-management {
  display: grid;
  min-width: 0;
  gap: 24px;
}
.page-heading h1 {
  font-size: 24px;
}
.page-heading p {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 13px;
}
.create-page-form {
  padding: 22px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.create-page-form h2,
.page-group h2 {
  margin-bottom: 12px;
  font-size: 17px;
}
.create-page-form p,
.empty-pages {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
}
.create-fields {
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 18px;
}
.create-fields label {
  display: grid;
  flex: 1 1 220px;
  min-width: 0;
  gap: 8px;
  font-size: 13px;
}
.create-fields input {
  min-width: 0;
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: 16px;
}
.slug-input {
  display: flex;
  align-items: center;
  gap: 8px;
}
.slug-input span {
  flex-shrink: 0;
  color: var(--text-muted);
}
.create-page-form > small {
  display: block;
  margin-top: 12px;
  color: var(--text-muted);
  font-size: 12px;
}
.pages-alert {
  padding: 12px;
  color: var(--error);
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
}
.page-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  overflow: hidden;
}
.page-list li {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
}
.page-list li:last-child {
  border-bottom: 0;
}
.page-list li > div {
  flex: 1;
  min-width: 150px;
  display: grid;
  gap: 6px;
}
.page-list strong {
  font-size: 14px;
  overflow-wrap: anywhere;
}
.page-list small,
.page-list li > span {
  font-size: 12px;
  color: var(--text-muted);
  overflow-wrap: anywhere;
}
@media (max-width: 600px) {
  .create-page-form {
    padding: 18px;
  }
  .create-fields button {
    width: 100%;
  }
  .page-list li {
    padding: 16px;
  }
}
</style>
