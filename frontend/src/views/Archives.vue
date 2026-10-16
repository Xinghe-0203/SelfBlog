<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { postApi } from '@/api/post'
import { formatDate } from '@/utils'
import type { Post } from '@/types'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { usePageSeo } from '@/composables/usePageSeo'
const { config, text, module: showModule } = usePageConfig('archives')
usePageSeo('archives', () => ({
  title: text('title', '文章归档'),
  description: text('description', '按时间回看这里发布过的文章。'),
  url: '/archives'
}))

interface ArchiveGroup {
  key: string
  label: string
  posts: Post[]
}

const posts = ref<Post[]>([])
const loading = ref(true)
const error = ref('')
const pageSize = 100

const archiveGroups = computed<ArchiveGroup[]>(() => {
  const groups = new Map<string, Post[]>()
  for (const post of posts.value) {
    const date = post.createTime || post.createdAt || ''
    const key = date.slice(0, 7) || 'undated'
    const monthPosts = groups.get(key) || []
    monthPosts.push(post)
    groups.set(key, monthPosts)
  }

  return [...groups.entries()]
    .sort(([left], [right]) => right.localeCompare(left))
    .map(([key, groupedPosts]) => ({
      key,
      label: key === 'undated' ? '日期未记录' : formatDate(`${key}-01`, 'YYYY年 MM月'),
      posts: groupedPosts
    }))
})

const fetchArchives = async () => {
  loading.value = true
  error.value = ''
  try {
    const firstResponse = await postApi.getPostList({ pageNum: 1, pageSize, sort: 'latest' })
    const firstPage = firstResponse.data || {}
    const allPosts: Post[] = firstPage.records || []
    const totalPages = Math.max(1, firstPage.pages || 1)

    // Fetch older pages in small batches so larger archives do not create a request burst.
    for (let firstPageNumber = 2; firstPageNumber <= totalPages; firstPageNumber += 5) {
      const pageNumbers = Array.from(
        { length: Math.min(5, totalPages - firstPageNumber + 1) },
        (_, index) => firstPageNumber + index
      )
      const responses = await Promise.all(
        pageNumbers.map((pageNum) => postApi.getPostList({ pageNum, pageSize, sort: 'latest' }))
      )
      for (const response of responses) allPosts.push(...(response.data?.records || []))
    }

    posts.value = allPosts
  } catch {
    error.value = '归档暂时无法加载，请稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(fetchArchives)
</script>

<template>
  <PageFrame :config="config" class="archives-page">
    <header class="page-heading">
      <h1>{{ text('title', '文章归档') }}</h1>
      <p v-if="text('description', '按时间回看这里发布过的文章。')">
        {{ text('description', '按时间回看这里发布过的文章。') }}
      </p>
    </header>

    <div v-if="loading" class="state-panel" role="status">正在整理文章归档…</div>
    <div v-else-if="error" class="state-panel state-panel--error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="fetchArchives">重新加载</button>
    </div>
    <div v-else-if="archiveGroups.length === 0" class="state-panel">还没有发布文章。</div>

    <div v-else class="archive-list">
      <section v-for="group in archiveGroups" :key="group.key" class="archive-group">
        <h2>
          {{ group.label }}<span v-if="showModule('count')">{{ group.posts.length }}</span>
        </h2>
        <ol>
          <li v-for="post in group.posts" :key="post.id">
            <time :datetime="post.createTime || post.createdAt">
              {{ formatDate(post.createTime || post.createdAt, 'MM月DD日') }}
            </time>
            <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
            <span v-if="showModule('category') && post.category" class="category">{{
              post.category
            }}</span>
          </li>
        </ol>
      </section>
    </div>
  </PageFrame>
</template>

<style scoped>
.archives-page {
  max-width: var(--page-width, 920px);
  margin: 0 auto;
  padding: var(--page-padding-block, 24px) 24px 48px;
  color: var(--text-primary);
}
.page-heading {
  padding-bottom: 24px;
}
.page-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 30px;
  letter-spacing: -0.045em;
}
.page-heading > p:last-child {
  margin: 12px 0 0;
  color: var(--text-secondary);
  font-size: 0.93rem;
}
.archive-list {
  padding: 0 26px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.archive-group {
  display: grid;
  grid-template-columns: 145px minmax(0, 1fr);
  gap: var(--page-section-gap, 24px);
  padding: 27px 0;
  border-bottom: 1px solid var(--border-solid);
}
.archive-group:last-child {
  border-bottom: 0;
}
.archive-group h2 {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-size: 1.18rem;
}
.archive-group h2 span {
  color: var(--text-muted);
  font: 500 0.75rem var(--font-sans);
}
.archive-group ol {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.archive-group li {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 12px;
}
.archive-group time {
  color: var(--text-muted);
  font-size: 0.75rem;
  white-space: nowrap;
}
.archive-group a {
  color: var(--text-primary);
  font-size: 0.9rem;
  line-height: 1.55;
  text-decoration: none;
}
.archive-group a:hover {
  color: var(--primary);
}
.category {
  color: var(--text-muted);
  font-size: 0.72rem;
}
.state-panel {
  margin-top: 28px;
  padding: 35px 20px;
  border: 1px dashed var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-muted);
  text-align: center;
}
.state-panel--error {
  color: var(--text-secondary);
}
.state-panel button {
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
@media (max-width: 620px) {
  .archives-page {
    padding: 8px 18px 36px;
  }
  .archive-group {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .archive-list {
    padding-inline: 20px;
  }
  .archive-group h2 {
    margin: 0;
  }
  .archive-group li {
    grid-template-columns: 55px minmax(0, 1fr);
    gap: 8px;
  }
  .category {
    display: none;
  }
}
</style>
