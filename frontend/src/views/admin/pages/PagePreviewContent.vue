<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, provide, ref, toRef } from 'vue'
import { pagePreviewKey } from '@/composables/usePageConfig'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import ContentPageView from '@/components/pages/ContentPageView.vue'
import type { PageConfig } from '@/types/page'
const props = defineProps<{
  id: string
  config: PageConfig
  slug?: string | null
  updatedAt?: string | null
}>()
const articleId = ref('')
const tagId = ref('')
const ready = ref(props.id !== 'post' && props.id !== 'tag')
const error = ref('')
provide(pagePreviewKey, { id: props.id, config: toRef(props, 'config'), articleId, tagId })
const views = {
  home: defineAsyncComponent(() => import('@/views/Home.vue')),
  about: defineAsyncComponent(() => import('@/views/About.vue')),
  archives: defineAsyncComponent(() => import('@/views/Archives.vue')),
  gallery: defineAsyncComponent(() => import('@/views/gallery/Gallery.vue')),
  search: defineAsyncComponent(() => import('@/views/search/Search.vue')),
  post: defineAsyncComponent(() => import('@/views/post/PostDetail.vue')),
  tag: defineAsyncComponent(() => import('@/views/tag/TagDetail.vue')),
  notFound: defineAsyncComponent(() => import('@/views/common/NotFound.vue'))
}
const view = computed(() => views[props.id as keyof typeof views])
async function loadExample() {
  error.value = ''
  try {
    if (props.id === 'post') {
      const post = (await postApi.getPostList({ pageNum: 1, pageSize: 1, sort: 'latest' })).data
        .records[0]
      if (!post) {
        error.value = '还没有公开文章，发布一篇文章后即可预览这个模板。'
        return
      }
      articleId.value = String(post.id)
    } else if (props.id === 'tag') {
      const tag = (await tagApi.getTagList()).data[0]
      if (!tag) {
        error.value = '还没有标签，创建标签后即可预览这个模板。'
        return
      }
      tagId.value = String(tag.id)
    }
    ready.value = true
  } catch {
    error.value = '真实预览数据暂时无法加载，请重试。'
  }
}
onMounted(() => {
  if (!ready.value) void loadExample()
})
</script>
<template>
  <div class="preview-content">
    <p v-if="error" class="preview-message" role="status">
      {{ error }} <button class="btn btn-ghost" type="button" @click="loadExample">重试</button>
    </p>
    <p v-else-if="!ready" class="preview-message" role="status">正在加载真实预览数据…</p>
    <div v-else inert>
      <component :is="view" v-if="view" /><ContentPageView
        v-else
        :page="{ id, config, slug, updatedAt }"
      />
    </div>
  </div>
</template>
<style scoped>
.preview-content {
  min-height: 300px;
}
.preview-message {
  padding: 24px;
  font-size: var(--text-sm);
}
</style>
