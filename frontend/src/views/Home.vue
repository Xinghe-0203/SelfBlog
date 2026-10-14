<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import { useSiteStore } from '@/stores/site'
import { formatDate, getSafeImageUrl, truncateText } from '@/utils'
import type { Post, Tag } from '@/types'

const route = useRoute()
const router = useRouter()
const siteStore = useSiteStore()
const posts = ref<Post[]>([])
const tags = ref<Tag[]>([])
const loading = ref(true)
const error = ref('')
const currentPage = ref(Math.max(1, Number(route.query.page) || 1))
const totalPages = ref(1)
const pageSize = 9

const featuredPost = computed(() => posts.value[0])
const listPosts = computed(() => posts.value.slice(featuredPost.value ? 1 : 0))

const fetchPosts = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await postApi.getPostList({
      pageNum: currentPage.value,
      pageSize,
      sort: 'latest'
    })
    const data = response.data || {}
    posts.value = data.records || []
    totalPages.value = Math.max(1, data.pages || 1)
  } catch {
    posts.value = []
    error.value = '文章暂时无法加载，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const fetchTags = async () => {
  try {
    const response = await tagApi.getTagList()
    tags.value = (response.data || []).slice(0, 12)
  } catch {
    tags.value = []
  }
}

const changePage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  router.push({ query: page === 1 ? {} : { page: String(page) } })
}

watch(
  () => route.query.page,
  (value) => {
    currentPage.value = Math.max(1, Number(value) || 1)
    fetchPosts()
  }
)

onMounted(() => {
  fetchPosts()
  fetchTags()
})
</script>

<template>
  <div class="blog-home">
    <section class="intro-shell">
      <div class="intro-copy">
        <p class="eyebrow">PERSONAL JOURNAL · NOTES & IDEAS</p>
        <h1>{{ siteStore.settings.siteTitle }}</h1>
        <p class="intro-description">{{ siteStore.settings.siteDescription }}</p>
        <div class="intro-links">
          <router-link to="/archives" class="text-link"
            >浏览全部文章 <span aria-hidden="true">↗</span></router-link
          >
          <router-link to="/about" class="text-link text-link--muted">关于本站</router-link>
        </div>
      </div>
      <div class="intro-mark" aria-hidden="true">
        <span>写</span>
        <i></i>
      </div>
    </section>

    <div class="home-content">
      <section class="article-column">
        <div class="section-heading">
          <div>
            <p class="eyebrow">THE LATEST</p>
            <h2>最近写作</h2>
          </div>
          <span v-if="posts.length" class="result-count">第 {{ currentPage }} 页</span>
        </div>

        <div v-if="loading" class="state-panel" role="status">正在加载文章…</div>
        <div v-else-if="error" class="state-panel state-panel--error" role="alert">
          <p>{{ error }}</p>
          <button class="quiet-button" type="button" @click="fetchPosts">重新加载</button>
        </div>
        <div v-else-if="posts.length === 0" class="state-panel">
          <p>这里还没有发布文章。</p>
          <p>新的故事很快就会在这里出现。</p>
        </div>

        <template v-else>
          <article v-if="featuredPost" class="featured-article">
            <router-link
              :to="`/post/${featuredPost.id}`"
              class="featured-cover"
              :class="{ 'is-empty': !featuredPost.coverImage }"
            >
              <img
                v-if="featuredPost.coverImage"
                :src="getSafeImageUrl(featuredPost.coverImage)"
                :alt="featuredPost.title"
              />
              <span v-else class="cover-placeholder" aria-hidden="true">✳</span>
              <span class="cover-label">最新文章</span>
            </router-link>
            <div class="featured-body">
              <div class="article-meta">
                <time :datetime="featuredPost.createTime || featuredPost.createdAt">
                  {{
                    formatDate(featuredPost.createTime || featuredPost.createdAt, 'YYYY年MM月DD日')
                  }}
                </time>
                <span v-if="featuredPost.category">{{ featuredPost.category }}</span>
              </div>
              <h3>
                <router-link :to="`/post/${featuredPost.id}`">{{ featuredPost.title }}</router-link>
              </h3>
              <p>{{ truncateText(featuredPost.summary || featuredPost.content, 190) }}</p>
              <div v-if="featuredPost.tags?.length" class="tag-row">
                <router-link
                  v-for="tag in featuredPost.tags.slice(0, 4)"
                  :key="tag.id"
                  :to="`/tag/${tag.id}`"
                  >#{{ tag.name }}</router-link
                >
              </div>
            </div>
          </article>

          <div v-if="listPosts.length" class="article-list">
            <article v-for="post in listPosts" :key="post.id" class="article-row">
              <div class="article-row-main">
                <div class="article-meta">
                  <time :datetime="post.createTime || post.createdAt">
                    {{ formatDate(post.createTime || post.createdAt, 'YYYY年MM月DD日') }}
                  </time>
                  <span v-if="post.category">{{ post.category }}</span>
                </div>
                <h3>
                  <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
                </h3>
                <p>{{ truncateText(post.summary || post.content, 150) }}</p>
                <div v-if="post.tags?.length" class="tag-row">
                  <router-link
                    v-for="tag in post.tags.slice(0, 3)"
                    :key="tag.id"
                    :to="`/tag/${tag.id}`"
                    >#{{ tag.name }}</router-link
                  >
                </div>
              </div>
              <router-link
                v-if="post.coverImage"
                :to="`/post/${post.id}`"
                class="row-cover"
                :aria-label="`阅读：${post.title}`"
              >
                <img :src="getSafeImageUrl(post.coverImage)" alt="" loading="lazy" />
              </router-link>
            </article>
          </div>

          <nav v-if="totalPages > 1" class="pagination" aria-label="文章分页">
            <button type="button" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
              ← 较新
            </button>
            <span>{{ currentPage }} / {{ totalPages }}</span>
            <button
              type="button"
              :disabled="currentPage >= totalPages"
              @click="changePage(currentPage + 1)"
            >
              较旧 →
            </button>
          </nav>
        </template>
      </section>

      <aside class="home-sidebar">
        <section class="sidebar-block">
          <p class="eyebrow">ABOUT</p>
          <h2>关于这个博客</h2>
          <p>{{ siteStore.settings.authorBio }}</p>
          <router-link to="/about" class="text-link"
            >继续了解 <span aria-hidden="true">↗</span></router-link
          >
        </section>
        <section v-if="tags.length" class="sidebar-block tag-block">
          <p class="eyebrow">TOPICS</p>
          <h2>文章标签</h2>
          <div class="sidebar-tags">
            <router-link v-for="tag in tags" :key="tag.id" :to="`/tag/${tag.id}`">{{
              tag.name
            }}</router-link>
          </div>
        </section>
        <router-link to="/archives" class="archive-callout">
          <span>按时间浏览</span>
          <strong>文章归档 <span aria-hidden="true">↗</span></strong>
        </router-link>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.blog-home {
  max-width: 1080px;
  margin: 0 auto;
  padding: 50px 24px 20px;
  color: var(--text-primary);
}
.intro-shell {
  min-height: 275px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  padding: 42px 0 48px;
  border-bottom: 1px solid var(--border-solid);
}
.intro-copy {
  max-width: 660px;
}
.eyebrow {
  margin: 0 0 12px;
  color: var(--primary);
  font-size: 0.69rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.intro-copy h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(2.4rem, 6vw, 4.3rem);
  line-height: 1.08;
  letter-spacing: -0.055em;
}
.intro-description {
  max-width: 520px;
  margin: 18px 0 0;
  color: var(--text-secondary);
  font-size: 1.03rem;
  line-height: 1.8;
}
.intro-links {
  display: flex;
  gap: 24px;
  margin-top: 24px;
}
.text-link {
  color: var(--primary);
  font-size: 0.86rem;
  font-weight: 600;
  text-decoration: none;
}
.text-link:hover {
  color: var(--primary-hover);
}
.text-link--muted {
  color: var(--text-muted);
}
.intro-mark {
  position: relative;
  display: grid;
  place-items: center;
  width: 178px;
  height: 178px;
  flex: 0 0 auto;
  border: 1px solid var(--border-solid);
  border-radius: 50%;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 4.5rem;
}
.intro-mark::before,
.intro-mark::after {
  position: absolute;
  content: '';
  border: 1px solid var(--border-solid);
  border-radius: 50%;
  inset: 13px;
}
.intro-mark::after {
  inset: 28px;
  border-color: color-mix(in srgb, var(--primary) 32%, transparent);
}
.intro-mark i {
  position: absolute;
  width: 8px;
  height: 8px;
  top: 14px;
  right: 27px;
  border-radius: 50%;
  background: var(--primary);
}
.home-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 66px;
  padding-top: 44px;
}
.article-column {
  min-width: 0;
}
.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin-bottom: 24px;
}
.section-heading .eyebrow {
  margin-bottom: 6px;
}
.section-heading h2,
.sidebar-block h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.55rem;
  letter-spacing: -0.03em;
}
.result-count {
  color: var(--text-muted);
  font-size: 0.78rem;
}
.featured-article {
  overflow: hidden;
  border: 1px solid var(--border-solid);
  border-radius: 14px;
  background: var(--surface-solid);
}
.featured-cover {
  position: relative;
  display: grid;
  place-items: center;
  height: 250px;
  overflow: hidden;
  background: linear-gradient(140deg, var(--primary-light), var(--bg-secondary));
  text-decoration: none;
}
.featured-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 350ms ease;
}
.featured-cover:hover img {
  transform: scale(1.025);
}
.cover-placeholder {
  color: var(--primary);
  font-size: 4rem;
  opacity: 0.65;
}
.cover-label {
  position: absolute;
  left: 17px;
  top: 16px;
  padding: 6px 10px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 20px;
  background: rgba(20, 30, 35, 0.52);
  color: white;
  font-size: 0.7rem;
  backdrop-filter: blur(8px);
}
.featured-body {
  padding: 22px 24px 25px;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--text-muted);
  font-size: 0.76rem;
}
.featured-body h3,
.article-row h3 {
  margin: 10px 0 8px;
  font-family: var(--font-display);
  font-size: 1.45rem;
  line-height: 1.35;
  letter-spacing: -0.025em;
}
.featured-body h3 a,
.article-row h3 a {
  color: var(--text-primary);
  text-decoration: none;
}
.featured-body h3 a:hover,
.article-row h3 a:hover {
  color: var(--primary);
}
.featured-body > p,
.article-row-main > p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.88rem;
  line-height: 1.75;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 14px;
}
.tag-row a {
  color: var(--primary);
  font-size: 0.75rem;
  text-decoration: none;
}
.tag-row a:hover {
  text-decoration: underline;
}
.article-list {
  margin-top: 9px;
}
.article-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  padding: 23px 0;
  border-bottom: 1px solid var(--border-solid);
}
.article-row-main {
  min-width: 0;
}
.article-row h3 {
  margin: 7px 0 5px;
  font-size: 1.15rem;
}
.article-row-main > p {
  font-size: 0.83rem;
}
.article-row .tag-row {
  margin-top: 9px;
}
.row-cover {
  width: 116px;
  height: 82px;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 8px;
  background: var(--bg-secondary);
}
.row-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.home-sidebar {
  padding-top: 4px;
}
.sidebar-block {
  padding-bottom: 24px;
  margin-bottom: 25px;
  border-bottom: 1px solid var(--border-solid);
}
.sidebar-block .eyebrow {
  margin-bottom: 9px;
}
.sidebar-block h2 {
  font-size: 1.08rem;
}
.sidebar-block > p:not(.eyebrow) {
  margin: 12px 0 14px;
  color: var(--text-secondary);
  font-size: 0.83rem;
  line-height: 1.75;
}
.sidebar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.sidebar-tags a {
  padding: 5px 9px;
  border: 1px solid var(--border-solid);
  border-radius: 20px;
  color: var(--text-secondary);
  font-size: 0.73rem;
  text-decoration: none;
}
.sidebar-tags a:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.archive-callout {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 15px;
  border-radius: 10px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 0.76rem;
  text-decoration: none;
}
.archive-callout strong {
  color: var(--text-primary);
  font-size: 0.95rem;
}
.archive-callout strong span {
  color: var(--primary);
}
.state-panel {
  padding: 38px 20px;
  border: 1px dashed var(--border-solid);
  border-radius: 12px;
  color: var(--text-muted);
  text-align: center;
}
.state-panel--error {
  color: var(--text-secondary);
}
.quiet-button {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 13px;
  border: 1px solid var(--border-solid);
  border-radius: 8px;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font: inherit;
  font-size: 0.82rem;
  text-decoration: none;
}
.quiet-button:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding-top: 25px;
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
@media (max-width: 760px) {
  .blog-home {
    padding: 27px 18px 10px;
  }
  .intro-shell {
    min-height: 0;
    padding: 36px 0;
  }
  .intro-mark {
    width: 108px;
    height: 108px;
    font-size: 2.7rem;
  }
  .intro-mark::before {
    inset: 8px;
  }
  .intro-mark::after {
    inset: 19px;
  }
  .intro-mark i {
    top: 9px;
    right: 16px;
  }
  .home-content {
    grid-template-columns: 1fr;
    gap: 38px;
    padding-top: 32px;
  }
  .home-sidebar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
  }
  .sidebar-block {
    margin: 0;
  }
  .archive-callout {
    align-self: start;
  }
}
@media (max-width: 520px) {
  .intro-mark {
    display: none;
  }
  .intro-copy h1 {
    font-size: 2.65rem;
  }
  .intro-description {
    font-size: 0.94rem;
  }
  .featured-cover {
    height: 195px;
  }
  .featured-body {
    padding: 18px;
  }
  .article-row {
    gap: 12px;
  }
  .row-cover {
    width: 82px;
    height: 70px;
  }
  .home-sidebar {
    grid-template-columns: 1fr;
  }
}
</style>
