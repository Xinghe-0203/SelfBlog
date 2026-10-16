<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import { formatDate, truncateText } from '@/utils'
import type { Post } from '@/types'

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

const search = async (keyword: string, page = 1) => {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    results.value = []
    totalResults.value = 0
    return
  }

  loading.value = true
  error.value = ''
  currentPage.value = page
  try {
    const response = await postApi.advancedSearch({
      keyword: normalizedKeyword,
      pageNum: page,
      pageSize
    })
    const data = response.data || {}
    results.value = data.records || []
    totalPages.value = Math.max(1, data.pages || 1)
    totalResults.value = data.total || 0
  } catch {
    results.value = []
    totalResults.value = 0
    error.value = '搜索暂时不可用，请稍后重试。'
  } finally {
    loading.value = false
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
  <div class="search-page">
    <header class="search-heading">
      <p class="eyebrow">SEARCH THE JOURNAL</p>
      <h1>搜索文章</h1>
      <p>按标题、摘要或正文内容查找。</p>
    </header>

    <form class="search-form" role="search" @submit.prevent="submitSearch">
      <label class="sr-only" for="article-search">搜索文章</label>
      <input
        id="article-search"
        v-model="query"
        type="search"
        placeholder="输入关键词…"
        maxlength="200"
      />
      <button type="submit" :disabled="loading || !query.trim()">
        {{ loading ? '搜索中…' : '搜索' }}
      </button>
    </form>

    <section v-if="route.query.q" class="search-results" aria-live="polite">
      <div class="results-heading">
        <h2>搜索结果</h2>
        <span v-if="!loading && !error">{{ totalResults }} 篇文章</span>
      </div>

      <div v-if="loading" class="state-panel" role="status">正在查找相关文章…</div>
      <div v-else-if="error" class="state-panel state-panel--error" role="alert">
        <p>{{ error }}</p>
        <button type="button" @click="search(String(route.query.q || query))">重新搜索</button>
      </div>
      <div v-else-if="results.length === 0" class="state-panel">
        <p>没有找到与“{{ route.query.q }}”相关的文章。</p>
        <span>试试更短的关键词，或检查拼写。</span>
      </div>
      <div v-else class="result-list">
        <article v-for="post in results" :key="post.id" class="result-item">
          <div class="result-meta">
            <time :datetime="post.createTime || post.createdAt">
              {{ formatDate(post.createTime || post.createdAt, 'YYYY年MM月DD日') }}
            </time>
            <span v-if="post.category">{{ post.category }}</span>
          </div>
          <h3>
            <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
          </h3>
          <p>{{ truncateText(post.summary || post.content, 200) }}</p>
          <div v-if="post.tags?.length" class="result-tags">
            <router-link v-for="tag in post.tags.slice(0, 4)" :key="tag.id" :to="`/tag/${tag.id}`"
              >#{{ tag.name }}</router-link
            >
          </div>
        </article>
      </div>

      <nav v-if="totalPages > 1 && !loading" class="pagination" aria-label="搜索结果分页">
        <button type="button" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
          ← 上一页
        </button>
        <span>{{ currentPage }} / {{ totalPages }}</span>
        <button
          type="button"
          :disabled="currentPage >= totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页 →
        </button>
      </nav>
    </section>
    <section v-else class="search-prompt">
      <span aria-hidden="true">⌕</span>
      <p>输入关键词，开始阅读。</p>
    </section>
  </div>
</template>

<style scoped>
.search-page {
  max-width: 790px;
  min-height: 55vh;
  margin: 0 auto;
  padding: 53px 24px 24px;
  color: var(--text-primary);
}
.search-heading {
  padding-bottom: 26px;
  text-align: center;
}
.eyebrow {
  margin: 0 0 10px;
  color: var(--primary);
  font-size: 0.69rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.search-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 5vw, 3rem);
  letter-spacing: -0.045em;
}
.search-heading > p:last-child {
  margin: 12px 0 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
}
.search-form {
  display: flex;
  gap: 9px;
  padding: 8px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
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
  font-size: 0.95rem;
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
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: white;
  cursor: pointer;
  font: inherit;
  font-size: 0.85rem;
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
  padding-top: 38px;
}
.results-heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-solid);
}
.results-heading h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.15rem;
}
.results-heading > span {
  color: var(--text-muted);
  font-size: 0.78rem;
}
.result-item {
  padding: 21px 2px 22px;
  border-bottom: 1px solid var(--border-solid);
}
.result-meta {
  display: flex;
  gap: 12px;
  color: var(--text-muted);
  font-size: 0.75rem;
}
.result-item h3 {
  margin: 8px 0 6px;
  font-family: var(--font-display);
  font-size: 1.22rem;
  line-height: 1.4;
}
.result-item h3 a {
  color: var(--text-primary);
  text-decoration: none;
}
.result-item h3 a:hover {
  color: var(--primary);
}
.result-item > p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.85rem;
  line-height: 1.75;
}
.result-tags {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
.result-tags a {
  color: var(--primary);
  font-size: 0.73rem;
  text-decoration: none;
}
.result-tags a:hover {
  text-decoration: underline;
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
  font-size: 0.8rem;
}
.pagination button {
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
.search-prompt > span {
  color: var(--primary);
  font-size: 2.8rem;
  line-height: 1;
}
.search-prompt p {
  margin: 0;
  font-size: 0.88rem;
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
    padding: 34px 18px 20px;
  }
  .search-form button {
    min-width: 68px;
  }
  .result-item h3 {
    font-size: 1.08rem;
  }
}
</style>
