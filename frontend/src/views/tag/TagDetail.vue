<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import { formatDate, truncateText } from '@/utils'
import type { Post, Tag } from '@/types'

const route = useRoute()
const tag = ref<Tag | null>(null)
const posts = ref<Post[]>([])
const loading = ref(true)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const totalPosts = ref(0)
const pageSize = 12

const hasMore = computed(() => currentPage.value < totalPages.value)

const fetchTag = async () => {
  loading.value = true
  error.value = ''
  try {
    const tagId = String(route.params.id)
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
  <div class="tag-page">
    <router-link to="/" class="back-link">← 返回首页</router-link>

    <div v-if="loading && !tag" class="state-panel" role="status">正在加载标签…</div>
    <div v-else-if="error && !tag" class="state-panel state-panel--error" role="alert">
      {{ error }}
    </div>

    <template v-else-if="tag">
      <header class="tag-heading">
        <p class="eyebrow">ARTICLE TAG</p>
        <h1><span>#</span>{{ tag.name }}</h1>
        <p>{{ totalPosts }} 篇相关文章</p>
      </header>

      <div v-if="posts.length === 0" class="state-panel">这个标签下还没有文章。</div>
      <div v-else class="tag-posts">
        <article v-for="post in posts" :key="post.id" class="tag-post">
          <div class="post-meta">
            <time :datetime="post.createTime || post.createdAt">
              {{ formatDate(post.createTime || post.createdAt, 'YYYY年MM月DD日') }}
            </time>
            <span v-if="post.category">{{ post.category }}</span>
          </div>
          <h2>
            <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
          </h2>
          <p>{{ truncateText(post.summary || post.content, 180) }}</p>
          <div v-if="post.tags?.length" class="post-tags">
            <router-link v-for="item in post.tags" :key="item.id" :to="`/tag/${item.id}`"
              >#{{ item.name }}</router-link
            >
          </div>
        </article>
      </div>

      <div v-if="error && posts.length" class="load-error" role="alert">{{ error }}</div>
      <div v-if="hasMore" class="load-more">
        <button type="button" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中…' : '加载更多文章' }}
        </button>
      </div>
      <p v-else-if="posts.length" class="end-note">已显示该标签下的全部文章</p>
    </template>
  </div>
</template>

<style scoped>
.tag-page {
  max-width: 790px;
  margin: 0 auto;
  padding: 42px 24px 25px;
  color: var(--text-primary);
}
.back-link {
  display: inline-block;
  margin-bottom: 25px;
  color: var(--text-muted);
  font-size: 0.8rem;
  text-decoration: none;
}
.back-link:hover {
  color: var(--primary);
}
.tag-heading {
  padding: 8px 0 25px;
  border-bottom: 1px solid var(--border-solid);
}
.eyebrow {
  margin: 0 0 9px;
  color: var(--primary);
  font-size: 0.69rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.tag-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 5vw, 3.2rem);
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
  font-size: 0.84rem;
}
.tag-post {
  padding: 23px 2px 25px;
  border-bottom: 1px solid var(--border-solid);
}
.post-meta {
  display: flex;
  gap: 12px;
  color: var(--text-muted);
  font-size: 0.75rem;
}
.tag-post h2 {
  margin: 8px 0 6px;
  font-family: var(--font-display);
  font-size: 1.28rem;
  line-height: 1.4;
}
.tag-post h2 a {
  color: var(--text-primary);
  text-decoration: none;
}
.tag-post h2 a:hover {
  color: var(--primary);
}
.tag-post > p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
  line-height: 1.75;
}
.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 11px;
}
.post-tags a {
  color: var(--primary);
  font-size: 0.74rem;
  text-decoration: none;
}
.post-tags a:hover {
  text-decoration: underline;
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
  padding: 9px 15px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font: inherit;
  font-size: 0.82rem;
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
  font-size: 0.78rem;
  text-align: center;
}
@media (max-width: 600px) {
  .tag-page {
    padding: 30px 18px 20px;
  }
  .tag-post h2 {
    font-size: 1.12rem;
  }
}
</style>
