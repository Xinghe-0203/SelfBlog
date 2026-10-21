<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import { useSiteStore } from '@/stores/site'
import { getSafeImageUrl } from '@/utils'
import ArticleList from '@/components/post/ArticleList.vue'
import Icon from '@/components/common/Icon.vue'
import type { Post, Tag } from '@/types'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { usePageSeo } from '@/composables/usePageSeo'
import { useLatestRequest } from '@/composables/useLatestRequest'

const route = useRoute()
const router = useRouter()
const siteStore = useSiteStore()
const { config, text, module: showModule } = usePageConfig('home')
const hasSidebar = computed(
  () => showModule('author') || showModule('tags') || showModule('archiveLink')
)
usePageSeo('home', () => ({
  title: text('title', siteStore.settings.siteTitle),
  description: text('description', siteStore.settings.siteDescription),
  url: '/'
}))
const posts = ref<Post[]>([])
const tags = ref<Tag[]>([])
const loading = ref(true)
const error = ref('')
const currentPage = ref(Math.max(1, Number(route.query.page) || 1))
const totalPages = ref(1)
const heroCover = ref('')
let heroChosen = false
const pageSize = 9
const beginPostsRequest = useLatestRequest().begin

async function fetchPosts(): Promise<void> {
  // 快速翻页会并发发请求：先发后到的旧响应不许覆盖新一页。
  const isLatest = beginPostsRequest()
  loading.value = true
  error.value = ''
  try {
    const response = await postApi.getPostList({
      pageNum: currentPage.value,
      pageSize,
      sort: 'latest'
    })
    if (!isLatest()) return
    const data = response.data || {}
    posts.value = data.records || []
    totalPages.value = Math.max(1, data.pages || 1)
    // One introduction image per visit; pagination never swaps the hero artwork.
    if (!heroChosen) {
      heroCover.value =
        posts.value
          .map((post) => getSafeImageUrl(post.coverImage || post.coverUrl, ''))
          .find(Boolean) || ''
      heroChosen = true
    }
  } catch {
    if (!isLatest()) return
    posts.value = []
    error.value = '文章暂时无法加载，请稍后重试。'
  } finally {
    if (isLatest()) loading.value = false
  }
}
async function fetchTags(): Promise<void> {
  try {
    tags.value = ((await tagApi.getTagList()).data || []).slice(0, 12)
  } catch {
    tags.value = []
  }
}
function changePage(page: number): void {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  void router.push({ query: { ...route.query, page: page === 1 ? undefined : String(page) } })
}
watch(
  () => route.query.page,
  (value) => {
    currentPage.value = Math.max(1, Number(value) || 1)
    void fetchPosts()
  }
)
onMounted(() => {
  void fetchPosts()
  void fetchTags()
})
</script>

<template>
  <PageFrame :config="config" class="blog-home">
    <section v-if="showModule('hero')" class="intro-shell">
      <div v-if="showModule('cover') && heroCover" class="intro-visual" aria-hidden="true">
        <img :src="heroCover" alt="" @error="heroCover = ''" />
      </div>
      <div class="intro-copy">
        <p class="intro-author">{{ text('eyebrow', siteStore.settings.authorName) }}</p>
        <h1>{{ text('title', siteStore.settings.siteDescription) }}</h1>
        <p v-if="text('description', siteStore.settings.authorBio)" class="intro-description">
          {{ text('description', siteStore.settings.authorBio) }}
        </p>
      </div>
    </section>
    <h1 v-else class="sr-only">{{ text('title', siteStore.settings.siteTitle) }}</h1>
    <div class="home-content" :class="{ 'home-content--full': !hasSidebar }">
      <section class="article-column" aria-labelledby="latest-heading">
        <div class="section-heading">
          <h2 id="latest-heading">{{ text('listTitle', '最近写作') }}</h2>
          <span v-if="posts.length" class="result-count">第 {{ currentPage }} 页</span>
        </div>
        <div v-if="loading" class="state-panel" role="status">正在加载文章…</div>
        <div v-else-if="error" class="state-panel" role="alert">
          <p>{{ error }}</p>
          <button class="btn btn-secondary" type="button" @click="fetchPosts">重新加载</button>
        </div>
        <div v-else-if="!posts.length" class="state-panel">
          <p>这里还没有发布文章。</p>
          <router-link to="/about">了解作者</router-link>
        </div>
        <template v-else>
          <ArticleList :posts="posts" :featured="currentPage === 1" />
          <nav v-if="totalPages > 1" class="home-pagination" aria-label="文章分页">
            <button type="button" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
              <Icon name="chevron-left" :size="16" />较新</button
            ><span>{{ currentPage }} / {{ totalPages }}</span
            ><button
              type="button"
              :disabled="currentPage >= totalPages"
              @click="changePage(currentPage + 1)"
            >
              较旧<Icon name="chevron-right" :size="16" />
            </button>
          </nav>
        </template>
      </section>
      <aside v-if="hasSidebar" class="home-sidebar" aria-label="作者与标签">
        <section v-if="showModule('author')" class="sidebar-block author-card">
          <div class="author-heading">
            <img
              v-if="siteStore.settings.authorAvatar"
              :src="getSafeImageUrl(siteStore.settings.authorAvatar)"
              alt=""
            /><span v-else class="author-initial" aria-hidden="true">{{
              siteStore.settings.authorName.slice(0, 1)
            }}</span>
            <h2>{{ siteStore.settings.authorName }}</h2>
          </div>
          <p>{{ siteStore.settings.authorBio }}</p>
          <router-link to="/about" class="text-link"
            >关于作者<Icon name="chevron-right" :size="14"
          /></router-link>
        </section>
        <section v-if="showModule('tags') && tags.length" class="sidebar-block">
          <h2>文章标签</h2>
          <div class="sidebar-tags">
            <router-link v-for="tag in tags" :key="tag.id" :to="`/tag/${tag.id}`">{{
              tag.name
            }}</router-link>
          </div>
        </section>
        <router-link v-if="showModule('archiveLink')" to="/archives" class="archive-callout"
          ><Icon name="archive" :size="19" /><span>按时间浏览文章</span
          ><Icon name="chevron-right" :size="15"
        /></router-link>
      </aside>
    </div>
  </PageFrame>
</template>

<style scoped>
.blog-home {
  max-width: var(--page-width, 1130px);
  margin-inline: auto;
  padding: var(--page-padding-block, 24px) 30px 48px;
}
.intro-shell {
  position: relative;
  display: flex;
  align-items: center;
  overflow: hidden;
  min-height: 213px;
  padding: 31px 34px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-2xl);
  background: var(--surface-secondary);
}
.intro-copy {
  position: relative;
  z-index: 1;
  max-width: 650px;
}
.intro-author {
  margin-bottom: 12px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.intro-copy h1 {
  max-width: 600px;
  font-size: clamp(28px, 3.2vw, 38px);
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.035em;
  overflow-wrap: anywhere;
}
.intro-description {
  margin-top: 15px;
  max-width: 410px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.8;
}
.intro-visual {
  position: absolute;
  inset: 0 0 0 auto;
  width: 43%;
  background: var(--bg-tertiary);
}
.intro-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.intro-shell::after {
  position: absolute;
  content: '';
  inset: 0;
  background: linear-gradient(
    90deg,
    var(--surface-secondary) 35%,
    color-mix(in srgb, var(--surface-secondary) 94%, transparent) 57%,
    color-mix(in srgb, var(--surface-secondary) 16%, transparent)
  );
}
.home-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 224px;
  gap: var(--page-section-gap, 24px);
  margin-top: var(--page-section-gap, 32px);
}
.home-content--full {
  grid-template-columns: minmax(0, 1fr);
}
.article-column {
  min-width: 0;
}
.section-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}
.section-heading h2 {
  font-size: var(--text-xl);
  letter-spacing: -0.025em;
}
.result-count {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.sidebar-block {
  padding: 21px;
  margin-bottom: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.sidebar-block h2 {
  font-size: var(--text-sm);
  overflow-wrap: anywhere;
}
.author-heading {
  display: flex;
  align-items: center;
  gap: 11px;
}
.author-heading img,
.author-initial {
  display: grid;
  flex: 0 0 39px;
  place-items: center;
  width: 39px;
  height: 39px;
  object-fit: cover;
  border-radius: var(--radius-full);
  background: var(--accent-light);
  color: var(--accent);
}
.author-card p {
  margin-block: 16px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.85;
  overflow-wrap: anywhere;
}
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  font-size: var(--text-xs);
}
.sidebar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 15px;
}
.sidebar-tags a {
  padding: 3px 8px;
  border-radius: var(--radius-xs);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: var(--text-2xs);
}
.sidebar-tags a:hover {
  background: var(--accent-light);
  color: var(--accent);
}
.archive-callout {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 16px;
  border: 1px dashed var(--border-light);
  border-radius: var(--radius-xl);
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.archive-callout span {
  flex: 1;
}
.archive-callout:hover {
  color: var(--accent);
  border-color: var(--accent);
}
.state-panel {
  display: grid;
  justify-items: start;
  gap: 16px;
  padding: 32px 24px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.state-panel p {
  font-size: var(--text-sm);
}
.home-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
  font-size: var(--text-xs);
  color: var(--text-muted);
}
.home-pagination button {
  display: flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  padding-inline: 10px;
  border-radius: var(--radius);
  color: var(--text-primary);
}
.home-pagination button:hover:not(:disabled) {
  background: var(--accent-light);
  color: var(--accent);
}
.home-pagination button:disabled {
  opacity: 0.4;
  cursor: default;
}
@media (max-width: 960px) {
  .home-content {
    grid-template-columns: minmax(0, 1fr);
  }
  .home-sidebar {
    display: none;
  }
}
@media (max-width: 600px) {
  .blog-home {
    padding: 8px 18px 36px;
  }
  .intro-shell {
    min-height: 217px;
    padding: 25px;
    border-radius: var(--radius-xl);
  }
  .intro-copy h1 {
    font-size: var(--text-3xl);
    max-width: 95%;
  }
  .intro-description {
    max-width: 220px;
  }
  .intro-visual {
    width: 69%;
    opacity: 0.6;
  }
  .home-content {
    margin-top: 27px;
  }
  .section-heading h2 {
    font-size: var(--text-lg);
  }
}
</style>
