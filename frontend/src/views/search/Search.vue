<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
/** 搜索输入框：挂载后把光标放进这里，快捷键直达搜索页时可以直接开打。 */
const searchInput = ref<HTMLInputElement | null>(null)
const results = ref<Post[]>([])
const loading = ref(false)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const totalResults = ref(0)
const pageSize = 12
const beginSearchRequest = useLatestRequest().begin

// —— 关键词联想（typeahead）——
const SUGGEST_DEBOUNCE_MS = 300
const suggestions = ref<string[]>([])
const suggestOpen = ref(false)
/** 键盘高亮的联想项下标；-1 表示没有高亮，Enter 走原有提交。 */
const activeSuggestion = ref(-1)
let suggestTimer: ReturnType<typeof setTimeout> | undefined
/** 选中联想项后压掉 watch 对同一个词的下一次联想：搜索已经提交，下拉不该再弹回来。 */
let suppressNextSuggest = false
/** 联想请求单独一条令牌：关闭下拉也会让在途响应过期。 */
const beginSuggestRequest = useLatestRequest().begin

const closeSuggestions = () => {
  suggestOpen.value = false
  activeSuggestion.value = -1
  if (suggestTimer) {
    clearTimeout(suggestTimer)
    suggestTimer = undefined
  }
  beginSuggestRequest()
}

const fetchSuggestions = async (keyword: string) => {
  const isLatest = beginSuggestRequest()
  try {
    const response = await postApi.getSearchSuggestions(keyword)
    if (!isLatest()) return
    suggestions.value = response.data || []
    activeSuggestion.value = -1
    // 空结果不渲染下拉
    suggestOpen.value = suggestions.value.length > 0
  } catch {
    if (!isLatest()) return
    suggestions.value = []
    suggestOpen.value = false
  }
}

watch(query, (keyword) => {
  if (suggestTimer) {
    clearTimeout(suggestTimer)
    suggestTimer = undefined
  }
  if (suppressNextSuggest) {
    suppressNextSuggest = false
    return
  }
  const normalized = keyword.trim()
  if (!normalized) {
    suggestions.value = []
    closeSuggestions()
    return
  }
  suggestTimer = setTimeout(() => {
    suggestTimer = undefined
    void fetchSuggestions(normalized)
  }, SUGGEST_DEBOUNCE_MS)
})

const onSearchKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    if (suggestOpen.value) closeSuggestions()
    return
  }
  if (!suggestOpen.value || !suggestions.value.length) return
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    // 环形移动：末项再往下回到首项
    activeSuggestion.value = (activeSuggestion.value + 1) % suggestions.value.length
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeSuggestion.value =
      (activeSuggestion.value - 1 + suggestions.value.length) % suggestions.value.length
  } else if (event.key === 'Enter' && activeSuggestion.value >= 0) {
    // 有高亮联想项时 Enter 选用它；否则不拦截，走表单原生提交（submitSearch）
    event.preventDefault()
    chooseSuggestion(activeSuggestion.value)
  }
}

const chooseSuggestion = (index: number) => {
  const keyword = suggestions.value[index]
  if (!keyword) return
  if (query.value !== keyword) {
    // 换词会触发 watch，压掉对提交词的下一次联想；词没变时不压（否则会吞掉下一次真实输入）
    suppressNextSuggest = true
    query.value = keyword
  }
  closeSuggestions()
  submitSearch()
}

onBeforeUnmount(() => {
  if (suggestTimer) clearTimeout(suggestTimer)
})

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
  // 文档类站点的惯例：Ctrl/Cmd+K 或 `/` 直达搜索页后，光标应该已经在输入框里。
  // nextTick 等 PageFrame 渲染完再聚焦，jsdom 与真实浏览器下行为一致。
  void nextTick(() => searchInput.value?.focus())
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
      <div class="search-input-wrap">
        <input
          id="article-search"
          ref="searchInput"
          v-model="query"
          type="search"
          role="combobox"
          :aria-expanded="suggestOpen"
          aria-controls="article-search-suggestions"
          aria-autocomplete="list"
          :aria-activedescendant="
            suggestOpen && activeSuggestion >= 0
              ? `article-search-option-${activeSuggestion}`
              : undefined
          "
          :placeholder="text('placeholder', '输入关键词…')"
          maxlength="200"
          autocomplete="off"
          @keydown="onSearchKeydown"
          @blur="closeSuggestions"
        />
        <!-- 联想下拉：纯字符串，只用文本插值（绝不 v-html） -->
        <ul
          v-if="suggestOpen && suggestions.length"
          id="article-search-suggestions"
          class="suggest-list"
          role="listbox"
          aria-label="搜索联想"
        >
          <li
            v-for="(suggestion, index) in suggestions"
            :id="`article-search-option-${index}`"
            :key="`${index}-${suggestion}`"
            class="suggest-option"
            :class="{ active: index === activeSuggestion }"
            role="option"
            :aria-selected="index === activeSuggestion"
            @mousedown.prevent
            @click="chooseSuggestion(index)"
          >
            {{ suggestion }}
          </li>
        </ul>
      </div>
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
  padding-bottom: 24px;
}
.search-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-4xl);
  font-weight: 700;
  letter-spacing: -0.03em;
}
.search-heading > p:last-child {
  margin: 14px 0 0;
  max-width: 56ch;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.8;
}
.search-form {
  display: flex;
  gap: 8px;
  padding: 7px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-full);
  background: var(--surface-solid);
  box-shadow: var(--shadow-sm);
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}
.search-input-wrap {
  position: relative;
  display: flex;
  flex: 1;
  min-width: 0;
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
/* 联想下拉：挂在输入框正下方，宽度 = 输入框宽度；不传送门，z-index 与页面层级一致 */
.suggest-list {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: var(--z-dropdown);
  margin: 0;
  padding: 6px;
  list-style: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-solid);
  box-shadow: var(--shadow-md);
}
.suggest-option {
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}
.suggest-option:hover,
.suggest-option.active {
  background: var(--bg-secondary);
  color: var(--text-primary);
}
.search-form:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--accent-soft);
}
.search-form button {
  min-width: 92px;
  padding: 0 18px;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--primary);
  /* 深色主题下强调色是提亮过的，白字对比度不足；--contrast-fg 自适应 */
  color: var(--contrast-fg);
  cursor: pointer;
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 600;
  transition:
    background var(--transition-fast),
    transform var(--transition-fast);
}
.search-form button:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: translateY(-1px);
}
.search-form button:disabled {
  cursor: default;
  opacity: 0.55;
}
.search-results {
  padding-top: 30px;
}
.results-heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-bottom: 14px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--border-light);
}
.results-heading h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 650;
}
.results-heading > span {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.state-panel {
  margin-top: 20px;
  padding: 35px 20px;
  border: 1px dashed var(--border-solid);
  border-radius: var(--radius-lg);
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
