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
const { config, text, module: showModule, banner, bannerVars } = usePageConfig('home')
const hasSidebar = computed(
  () => showModule('author') || showModule('tags') || showModule('archiveLink')
)
/**
 * 横幅的版式 class。
 *
 * 没有横幅时返回空对象 —— `.intro-shell` 的 class 与改造前逐字一致，
 * 介绍区保持原有外观（右侧继续用第一篇文章封面）。
 */
const bannerClass = computed(() => {
  const current = banner.value
  if (!current) return {}
  return {
    'intro-shell--banner': true,
    [`intro-shell--banner-${current.layout}`]: true
  }
})
/** 文字明暗：tone=light，或 auto 且遮罩够深（≥40）时用浅色。 */
const bannerTone = computed(() => {
  const current = banner.value
  if (!current) return undefined
  return current.light ? 'light' : 'dark'
})
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
    <section
      v-if="showModule('hero')"
      class="intro-shell"
      :class="bannerClass"
      :style="bannerVars"
      :data-banner-tone="bannerTone"
    >
      <div v-if="banner" class="intro-banner" aria-hidden="true">
        <div class="intro-banner-media"></div>
        <div class="intro-banner-scrim"></div>
      </div>
      <div
        v-if="showModule('cover') && heroCover && !banner"
        class="intro-visual"
        aria-hidden="true"
      >
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
/* 遮罩渐变由**纸面**那一档构成，不是 --surface-secondary：壁纸模式下纸面是实心白，
   用 --surface-secondary 会在「文案底下的渐变」与「它身后的纸面」之间留一道可见色带。
   appearance.css 里已有同名规则做同一件事；这里保留一份是为了让 scoped 样式自洽，
   且只在壁纸模式生效（纯色画布逐像素不变）。 */
html[data-wallpaper] #selfblog .intro-shell::after {
  background-image: linear-gradient(
    90deg,
    var(--surface-paper) 35%,
    color-mix(in srgb, var(--surface-paper) 94%, transparent) 57%,
    color-mix(in srgb, var(--surface-paper) 16%, transparent)
  );
}
/* ---------------------------------------------------------------------------
   横幅背景（作者在「页面管理 → 首页」里设置）
   ---------------------------------------------------------------------------
   没有横幅时下面每一条规则都不生效：`.intro-shell` 的 class、style 属性与 DOM
   结构都和改造前完全一致 —— 右侧继续用第一篇文章封面做 `.intro-visual`。
   变量由 usePageConfig 的 pageBannerVars() 拼好，地址已经过白名单校验。 */
.intro-shell--banner {
  min-height: var(--banner-height, 250px);
  isolation: isolate;
}
.intro-banner {
  position: absolute;
  z-index: 0;
  inset: 0;
  overflow: hidden;
  border-radius: inherit;
  pointer-events: none;
}
.intro-banner-media {
  position: absolute;
  inset: 0;
  background-image: var(--banner-image);
  background-size: var(--banner-size, cover);
  background-position: var(--banner-position, center);
  background-repeat: no-repeat;
}
.intro-banner-scrim {
  position: absolute;
  inset: 0;
  /* 遮罩用「永远深色」的语义 token（--code-bg），不用会被暗色模式反转的灰度梯度。 */
  background: color-mix(in srgb, var(--code-bg) calc(var(--banner-overlay, 0) * 1%), transparent);
}
/* 左右分栏：文字在左（纸面上），图在右。 */
.intro-shell--banner-split .intro-banner {
  inset: 0 0 0 auto;
  width: 43%;
}
.intro-shell--banner-split .intro-copy {
  max-width: 55%;
}
/* 居中：图铺满整个介绍区，文字压在上面。 */
.intro-shell--banner-centered {
  justify-content: center;
  text-align: center;
}
.intro-shell--banner-centered .intro-copy {
  max-width: 720px;
}
.intro-shell--banner-centered .intro-description {
  margin-inline: auto;
}
/* 上下堆叠：图在上，文字在下。 */
.intro-shell--banner-stacked {
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-end;
}
.intro-shell--banner-stacked .intro-banner {
  inset: 0 0 auto 0;
  height: calc(var(--banner-height, 250px) * 0.58);
}
.intro-shell--banner-stacked .intro-copy {
  margin-top: calc(var(--banner-height, 250px) * 0.58);
  max-width: 100%;
}
/* 文字压在图上（居中排布）时才按明暗切换文字颜色。 */
.intro-shell--banner-centered[data-banner-tone='light'] .intro-copy,
.intro-shell--banner-centered[data-banner-tone='light'] .intro-description,
.intro-shell--banner-centered[data-banner-tone='light'] .intro-author {
  color: var(--text-inverse);
}
/* 横幅自带底图，不再叠那层纸面渐变。 */
.intro-shell--banner::after {
  content: none;
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
  /* 浮起档：纯色画布下别名回 --surface-primary（逐像素不变），
     壁纸画布下换成比纸面深一档的实底，卡片才从纸面上「浮」出来。 */
  background: var(--surface-raised);
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
  /* 纯色画布保持原样（虚线、无底色）以守住「不透明度 100 时逐像素不变」；
     壁纸模式下由 appearance.css 的控件档接管成实线 + 控件底色 —— 那条规则
     （`html[data-wallpaper] #selfblog .archive-callout`）优先级更高。 */
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
  /* 页面级纸面档：与 .page-frame 同一档，空态不会浮在壁纸上。 */
  background: var(--surface-paper);
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
  /* 横幅：窄屏下左右分栏退化成上下堆叠；min-height 仍按作者选的高度档位，
     上面那条 .intro-shell { min-height: 217px } 会盖掉横幅档位，这里补回来。 */
  .intro-shell--banner {
    min-height: var(--banner-height, 250px);
  }
  .intro-shell--banner-split .intro-banner {
    inset: 0 0 auto 0;
    width: 100%;
    height: calc(var(--banner-height, 250px) * 0.46);
  }
  .intro-shell--banner-split .intro-copy {
    max-width: 100%;
    margin-top: calc(var(--banner-height, 250px) * 0.46);
  }
  .intro-shell--banner-stacked .intro-banner {
    height: calc(var(--banner-height, 250px) * 0.46);
  }
  .intro-shell--banner-stacked .intro-copy {
    margin-top: calc(var(--banner-height, 250px) * 0.46);
  }
  .home-content {
    margin-top: 27px;
  }
  .section-heading h2 {
    font-size: var(--text-lg);
  }
}
</style>
