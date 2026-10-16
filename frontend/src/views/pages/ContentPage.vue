<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { usePagesStore } from '@/stores/pages'
import { usePageSeo } from '@/composables/usePageSeo'
import ContentPageView from '@/components/pages/ContentPageView.vue'
import NotFound from '@/views/common/NotFound.vue'
import type { PublicPage } from '@/types/page'

const route = useRoute()
const pages = usePagesStore()
const page = ref<PublicPage | null>(null)
const loading = ref(true)
const missing = ref(false)
const error = ref('')
let request = 0
usePageSeo(
  'custom',
  () => ({
    title: page.value?.config.title || (missing.value ? '页面不存在' : '内容页'),
    description: page.value?.config.description || '',
    url: `/pages/${route.params.slug}`,
    indexable: !!page.value
  }),
  () => page.value?.config || {}
)
async function load(): Promise<void> {
  const current = ++request
  loading.value = true
  missing.value = false
  error.value = ''
  page.value = null
  try {
    const result = await pages.loadSlug(String(route.params.slug))
    if (current === request) page.value = result
  } catch (reason) {
    if (current !== request) return
    if ((reason as { response?: { status?: number } }).response?.status === 404)
      missing.value = true
    else error.value = '页面暂时无法加载，请重试。'
  } finally {
    if (current === request) loading.value = false
  }
}
watch(() => route.params.slug, load, { immediate: true })
onBeforeUnmount(() => {
  request++
})
</script>
<template>
  <div v-if="loading" class="content-state" role="status">正在打开页面…</div>
  <NotFound v-else-if="missing" />
  <div v-else-if="error" class="content-state" role="alert">
    <p>{{ error }}</p>
    <button type="button" class="btn btn-secondary" @click="load">重新加载</button>
  </div>
  <ContentPageView v-else-if="page" :page="page" />
</template>
<style scoped>
.content-state {
  display: grid;
  justify-items: center;
  gap: 16px;
  padding: 48px 20px;
  color: var(--text-secondary);
}
</style>
