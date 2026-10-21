<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import ArticleList from '@/components/post/ArticleList.vue'
import Icon from '@/components/common/Icon.vue'
import type { Post } from '@/types'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { usePageSeo } from '@/composables/usePageSeo'
import { useLatestRequest } from '@/composables/useLatestRequest'
const { config, text, module: showModule } = usePageConfig('search')
usePageSeo('search', () => ({
  title: text('title', '搜索文章'),
  description: text('description', '按标题、摘要或正文内容查找。'),
  url: '/search'
}))

const route = useRoute()
const router = useRouter()
const query = ref(String(route.query.q || ''))
const results = ref<Post[]>([])
const loading = ref(false)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const totalResults = ref(0)
const pageSize = 12
const beginSearchRequest = useLatestRequest().begin

const search = async (keyword: string, page = 1) => {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    results.value = []
    totalResults.value = 0
    return
  }

  // 翻页/改词都会并发发请求：先发后到的旧响应不许覆盖新结果。
  const isLatest = beginSearchRequest()
  loading.value = true
  error.value = ''
  currentPage.value = page
  try {
    const response = await postApi.advancedSearch({
      keyword: normalizedKeyword,
      pageNum: page,
      pageSize
    })
    if (!isLatest()) return
    const data = response.data || {}
    results.value = data.records || []
    totalPages.value = Math.max(1, data.pages || 1)
    totalResults.value = data.total || 0
  } catch {
    if (!isLatest()) return
    results.value = []
    totalResults.value = 0
    error.value = '搜索暂时不可用，请稍后重试。'
  } finally {
    if (isLatest()) loading.value = false
  }
}

const submitSearch = () => {
  const keyword = query.value.trim()
  if (!keyword) return
  if (String(route.query.q || '') === keyword) {
    search(keyword)
  } else {
    router.push({ path: '/search', query: { q: keyword } })
  }
}

const changePage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  search(String(route.query.q || query.value), page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(
  () => route.query.q,
  (value) => {
    query.value = String(value || '')
    search(query.value)
  }
)

onMounted(() => {
  if (route.query.q) search(String(route.query.q))
})
</script>

<template>
  <PageFrame :config="config" class="search-page">
    <header class="search-heading">
      <h1>{{ text('title', '搜索文章') }}</h1>
      <p v-if="text('description', '按标题、摘要或正文内容查找。')">
        {{ text('description', '按标题、摘要或正文内容查找。') }}
      </p>
    </header>

    <form class="search-form" role="search" @submit.prevent="submitSearch">
      <label class="sr-only" for="article-search">搜索文章</label>
      <input
        id="article-search"
        v-model="query"
        type="search"
        :placeholder="text('placeholder', '输入关键词…')"
        maxlength="200"
      />
      <button type="submit" :disabled="loading || !query.trim()">
        {{ loading ? '搜索中…' : '搜索' }}
      </button>
    </form>

    <section v-if="route.query.q" class="search-results" aria-live="polite">
      <div class="results-heading">
        <h2>搜索结果</h2>
        <span v-if="showModule('count') && !loading && !error">{{ totalResults }} 篇文章</span>
      </div>

      <div v-if="loading" class="state-panel" role="status">正在查找相关文章…</div>
      <div v-else-if="error" class="state-panel state-panel--error" role="alert">
        <p>{{ error }}</p>
        <button type="button" @click="search(String(route.query.q || query))">重新搜索</button>
      </div>
      <div v-else-if="results.length === 0" class="state-panel">
        <p>{{ text('empty', `没有找到与“${route.query.q}”相关的文章。`) }}</p>
        <span>试试更短的关键词，或检查拼写。</span>
      </div>
      <ArticleList v-else :posts="results" />

      <nav v-if="totalPages > 1 && !loading" class="pagination" aria-label="搜索结果分页">
        <button type="button" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
          <Icon name="chevron-left" :size="16" />上一页
        </button>
        <span>{{ currentPage }} / {{ totalPages }}</span>
        <button
          type="button"
          :disabled="currentPage >= totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页<Icon name="chevron-right" :size="16" />
        </button>
      </nav>
    </section>
    <section v-else class="search-prompt">
      <Icon name="search" :size="28" />
      <p>输入关键词，开始阅读。</p>
    </section>
  </PageFrame>
</template>

<style scoped>
.search-page {
  max-width: var(--page-width, 920px);
  min-height: 55vh;
  margin: 0 auto;
  padding: var(--page-padding-block, 24px) 24px 48px;
  color: var(--text-primary);
}
.search-heading {
  padding-bottom: 26px;
}
.search-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-3xl);
  letter-spacing: -0.045em;
}
.search-heading > p:last-child {
  margin: 12px 0 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.search-form {
  display: flex;
  gap: 9px;
  padding: 8px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-lg);
  background: var(--surface-solid);
  box-shadow: var(--shadow-sm);
}
.search-form input {
  flex: 1;
  min-width: 0;
  height: 43px;
  padding: 0 12px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-base);
}
.search-form input::placeholder {
  color: var(--text-muted);
}
.search-form:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}
.search-form button {
  min-width: 88px;
  padding: 0 15px;
  border: 0;
  border-radius: var(--radius);
  background: var(--primary);
  color: white;
  cursor: pointer;
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 600;
}
.search-form button:hover:not(:disabled) {
  background: var(--primary-hover);
}
.search-form button:disabled {
  cursor: default;
  opacity: 0.55;
}
.search-results {
  padding-top: 28px;
}
.results-heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-bottom: 12px;
  margin-bottom: 8px;
}
.results-heading h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-lg);
}
.results-heading > span {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.state-panel {
  margin-top: 20px;
  padding: 35px 20px;
  border: 1px dashed var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-muted);
  text-align: center;
}
.state-panel p {
  margin: 0 0 5px;
  color: var(--text-secondary);
}
.state-panel--error {
  color: var(--text-secondary);
}
.state-panel button {
  margin-top: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
}
.state-panel button:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding-top: 23px;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.pagination button {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 44px;
  border: 0;
  background: none;
  color: var(--primary);
  cursor: pointer;
  font: inherit;
}
.pagination button:disabled {
  color: var(--text-muted);
  cursor: default;
  opacity: 0.5;
}
.search-prompt {
  display: grid;
  place-items: center;
  gap: 9px;
  min-height: 190px;
  color: var(--text-muted);
}
.search-prompt p {
  margin: 0;
  font-size: var(--text-sm);
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
@media (max-width: 600px) {
  .search-page {
    padding: 8px 18px 36px;
  }
  .search-form button {
    min-width: 68px;
  }
}
</style>
