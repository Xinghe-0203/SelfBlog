<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import ArticleList from '@/components/post/ArticleList.vue'
import Icon from '@/components/common/Icon.vue'
import type { Post, Tag } from '@/types'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { usePageSeo } from '@/composables/usePageSeo'
const { config, text, module: showModule, preview } = usePageConfig('tag')

const route = useRoute()
const tag = ref<Tag | null>(null)
const posts = ref<Post[]>([])
const loading = ref(true)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const totalPosts = ref(0)
usePageSeo('tag', () => ({
  title: tag.value?.name || '标签详情',
  description: text('description', ''),
  url: `/tag/${tag.value?.id || route.params.id}`,
  indexable: !!tag.value
}))
const pageSize = 12

const hasMore = computed(() => currentPage.value < totalPages.value)

const fetchTag = async () => {
  loading.value = true
  error.value = ''
  try {
    const tagId = preview?.tagId?.value || String(route.params.id)
    const [tagResponse, postResponse] = await Promise.all([
      tagApi.getTagById(tagId),
      postApi.advancedSearch({ tagId, pageNum: 1, pageSize })
    ])
    tag.value = tagResponse.data || null
    const data = postResponse.data || {}
    posts.value = data.records || []
    currentPage.value = 1
    totalPages.value = Math.max(1, data.pages || 1)
    totalPosts.value = data.total || 0
  } catch {
    tag.value = null
    posts.value = []
    error.value = '标签不存在，或文章列表暂时无法加载。'
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!hasMore.value || loading.value) return
  loading.value = true
  try {
    const nextPage = currentPage.value + 1
    const response = await postApi.advancedSearch({
      tagId: String(route.params.id),
      pageNum: nextPage,
      pageSize
    })
    const data = response.data || {}
    posts.value.push(...(data.records || []))
    currentPage.value = nextPage
    totalPages.value = Math.max(1, data.pages || totalPages.value)
    totalPosts.value = data.total || totalPosts.value
  } catch {
    error.value = '更多文章暂时无法加载，请重试。'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, fetchTag, { immediate: true })
</script>

<template>
  <PageFrame :config="config" class="tag-page">
    <router-link to="/" class="back-link"
      ><Icon name="chevron-left" :size="16" />返回首页</router-link
    >

    <div v-if="loading && !tag" class="state-panel" role="status">正在加载标签…</div>
    <div v-else-if="error && !tag" class="state-panel state-panel--error" role="alert">
      {{ error }}
    </div>

    <template v-else-if="tag">
      <header class="tag-heading">
        <h1><span>#</span>{{ tag.name }}</h1>
        <p v-if="text('description', '')">{{ text('description', '') }}</p>
        <p v-if="showModule('count')">{{ totalPosts }} 篇相关文章</p>
      </header>

      <div v-if="posts.length === 0" class="state-panel">这个标签下还没有文章。</div>
      <ArticleList v-else :posts="posts" heading="h2" />

      <div v-if="error && posts.length" class="load-error" role="alert">{{ error }}</div>
      <div v-if="hasMore" class="load-more">
        <button type="button" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中…' : '加载更多文章' }}
        </button>
      </div>
      <p v-else-if="posts.length" class="end-note">已显示该标签下的全部文章</p>
    </template>
  </PageFrame>
</template>

<style scoped>
.tag-page {
  max-width: var(--page-width, 920px);
  margin: 0 auto;
  padding: var(--page-padding-block, 24px) 24px 48px;
  color: var(--text-primary);
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 44px;
  margin-bottom: 12px;
  color: var(--text-muted);
  font-size: var(--text-xs);
  text-decoration: none;
}
.back-link:hover {
  color: var(--primary);
}
.tag-heading {
  padding: 8px 0 25px;
}
.tag-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-3xl);
  letter-spacing: -0.045em;
  overflow-wrap: anywhere;
}
.tag-heading h1 span {
  margin-right: 5px;
  color: var(--primary);
}
.tag-heading > p:last-child {
  margin: 10px 0 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.state-panel {
  margin-top: 25px;
  padding: 35px 20px;
  border: 1px dashed var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-muted);
  text-align: center;
}
.state-panel--error {
  color: var(--text-secondary);
}
.load-more {
  display: flex;
  justify-content: center;
  padding-top: 22px;
}
.load-more button {
  min-height: 44px;
  padding: 9px 15px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font: inherit;
  font-size: var(--text-sm);
}
.load-more button:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}
.load-more button:disabled {
  cursor: default;
  opacity: 0.6;
}
.load-error,
.end-note {
  margin: 18px 0 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
  text-align: center;
}
@media (max-width: 600px) {
  .tag-page {
    padding: 8px 18px 36px;
  }
}
</style>
