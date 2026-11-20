<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import { resolveSidebarOrder, useSiteStore } from '@/stores/site'
import { formatDate, getSafeImageUrl } from '@/utils'
import ArticleList from '@/components/post/ArticleList.vue'
import CountUp from '@/components/common/CountUp.vue'
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
 * 首页侧栏三个模块的渲染顺序：由「外观深度自定义」的 homeSidebarOrder 决定，
 * 脏数据回落到现有模板顺序（author → tags → archive）。只调顺序，不增删模块，
 * 每个模块自身的显隐条件不变。
 */
const sidebarOrder = computed(() => resolveSidebarOrder(siteStore.settings.homeSidebarOrder))
/** 顺序字符串拆成模块 id：白名单保证恰好三段，`archive` 对应 showModule('archiveLink') 那一块。 */
const orderedWidgets = computed(() => sidebarOrder.value.split('-'))
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
/** 全站文章总数（列表接口返回的 total），用于首屏的「文章」统计位。 */
const postTotal = ref(0)
/** 最近一次更新的时间：取当前页第一篇文章的创建时间。 */
const latestDate = ref('')
const heroCover = ref('')
let heroChosen = false
const pageSize = 9
const beginPostsRequest = useLatestRequest().begin

/**
 * 列表排序档位：最新 / 最热 / 精选。
 *
 * 后端本来就支持这三种排序（PostQueryServiceImpl 的 sort 开关），
 * 这里把它暴露给读者：热门文章与高赞文章往往不是最新发布的那几篇，
 * 新读者最容易漏掉它们。
 */
const SORT_OPTIONS = [
  { value: 'latest', label: '最新' },
  { value: 'hot', label: '最热' },
  { value: 'essence', label: '精选' }
] as const
type SortOption = (typeof SORT_OPTIONS)[number]['value']

/** 只接受白名单里的取值；脏数据（手改的链接、旧缓存）回落到「最新」。 */
function normalizeSort(raw: unknown): SortOption {
  const value = String(raw ?? '')
  return (SORT_OPTIONS.some((option) => option.value === value) ? value : 'latest') as SortOption
}

const activeSort = ref<SortOption>(normalizeSort(route.query.sort))

/** 切换排序时回到第一页：第 3 页的「最热」没有意义。 */
function changeSort(sort: SortOption): void {
  if (sort === activeSort.value) return
  void router.push({
    query: { ...route.query, sort: sort === 'latest' ? undefined : sort, page: undefined }
  })
}

const sortGroup = ref<HTMLElement | null>(null)

/**
 * 分段控件的键盘行为（ARIA radio 的通行模式）。
 *
 * <p>只有**当前选中**那一枚在 Tab 序里（roving tabindex），其余 `tabindex=-1`：
 * 键盘用户 Tab 到这一组只需要停一次，再用方向键在组内切换 —— 否则三个按钮
 * 各占一个 Tab 位，读完一篇文章要多按两次 Tab。
 *
 * <p>方向键按「上一项 / 下一项」移动并立即选中（左/上 = 前一项，右/下 = 后一项，
 * 循环），与系统单选组一致；Home / End 跳到首尾。
 */
function onSortKeydown(event: KeyboardEvent): void {
  const keys = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End']
  if (!keys.includes(event.key)) return
  event.preventDefault()
  const total = SORT_OPTIONS.length
  const current = SORT_OPTIONS.findIndex((option) => option.value === activeSort.value)
  const from = current < 0 ? 0 : current
  let next = from
  if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') next = (from - 1 + total) % total
  else if (event.key === 'ArrowRight' || event.key === 'ArrowDown') next = (from + 1) % total
  else if (event.key === 'Home') next = 0
  else next = total - 1

  const option = SORT_OPTIONS[next]
  if (option) changeSort(option.value)
  // 选中项换了之后焦点要跟着走（否则下一次方向键仍从旧焦点算起）。
  const buttons = sortGroup.value?.querySelectorAll<HTMLElement>('button')
  buttons?.[next]?.focus()
}

async function fetchPosts(): Promise<void> {
  // 快速翻页会并发发请求：先发后到的旧响应不许覆盖新一页。
  const isLatest = beginPostsRequest()
  loading.value = true
  error.value = ''
  try {
    const response = await postApi.getPostList({
      pageNum: currentPage.value,
      pageSize,
      sort: activeSort.value
    })
    if (!isLatest()) return
    const data = response.data || {}
    posts.value = data.records || []
    totalPages.value = Math.max(1, data.pages || 1)
    postTotal.value = data.total ?? posts.value.length
    // 「最近更新」只跟着最新一页走：翻到第 3 页时它显示的仍然是最新的一篇，
    // 而不是第三页的第一篇 —— 后者会让人误以为站点停更了。
    latestDate.value = posts.value[0]?.createTime || posts.value[0]?.createdAt || ''
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
// 排序是 URL 的一部分（?sort=hot 可以直接分享），因此它变化时也要重新取数。
watch(
  () => route.query.sort,
  (value) => {
    const next = normalizeSort(value)
    if (next === activeSort.value) return
    activeSort.value = next
    currentPage.value = 1
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
      :class="[bannerClass, { 'intro-shell--cover': !banner && !!heroCover }]"
      :style="bannerVars"
      :data-banner-tone="bannerTone"
    >
      <div v-if="banner" class="intro-banner" aria-hidden="true">
        <div class="intro-banner-media"></div>
        <div class="intro-banner-scrim"></div>
      </div>
      <div v-if="!banner" class="intro-decor ambient-drift" aria-hidden="true"></div>
      <div
        v-if="showModule('cover') && heroCover && !banner"
        class="intro-visual"
        aria-hidden="true"
      >
        <img :src="heroCover" alt="" @error="heroCover = ''" />
      </div>
      <div class="intro-copy">
        <p class="intro-author">
          <span class="intro-dot" aria-hidden="true"></span
          >{{ text('eyebrow', siteStore.settings.authorName) }}
        </p>
        <h1>{{ text('title', siteStore.settings.siteDescription) }}</h1>
        <p v-if="text('description', siteStore.settings.authorBio)" class="intro-description">
          {{ text('description', siteStore.settings.authorBio) }}
        </p>
        <!-- 首屏数据条：文章数 / 标签数 / 最近更新。全部来自已加载的列表与标签接口，
             不额外发请求；取不到时该位不渲染（不留空位）。 -->
        <dl class="intro-stats">
          <div v-if="postTotal" class="intro-stat">
            <dt>文章</dt>
            <dd><CountUp :value="postTotal" /></dd>
          </div>
          <div v-if="tags.length" class="intro-stat">
            <dt>标签</dt>
            <dd><CountUp :value="tags.length" /></dd>
          </div>
          <div v-if="latestDate" class="intro-stat">
            <dt>最近更新</dt>
            <dd>{{ formatDate(latestDate, 'YYYY年MM月DD日') }}</dd>
          </div>
        </dl>
      </div>
    </section>
    <h1 v-else class="sr-only">{{ text('title', siteStore.settings.siteTitle) }}</h1>
    <div class="home-content" :class="{ 'home-content--full': !hasSidebar }">
      <section class="article-column" aria-labelledby="latest-heading">
        <div class="section-heading">
          <h2 id="latest-heading">
            <span class="section-rule" aria-hidden="true"></span>{{ text('listTitle', '最近写作') }}
          </h2>
          <div class="section-tools">
            <!-- 排序切换：分段控件，三档互斥。用 radiogroup 语义而不是三个按钮，
                 读屏会念出「已选中」，而不是让读者自己判断当前是哪一档。 -->
            <div
              ref="sortGroup"
              class="sort-switch"
              role="radiogroup"
              aria-label="文章排序方式"
              @keydown="onSortKeydown"
            >
              <button
                v-for="(option, index) in SORT_OPTIONS"
                :key="option.value"
                type="button"
                role="radio"
                :aria-checked="activeSort === option.value"
                :class="{ active: activeSort === option.value }"
                :tabindex="activeSort === option.value ? 0 : -1"
                :data-sort-index="index"
                @click="changeSort(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <span v-if="posts.length" class="result-count">第 {{ currentPage }} 页</span>
          </div>
        </div>
        <div v-if="loading" class="state-panel" role="status">正在加载文章…</div>
        <div v-else-if="error" class="state-panel state-panel--error" role="alert">
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
        <!-- 默认顺序（author-tags-archive）渲染出的 DOM 与固定模板逐字一致：template v-for 不产生额外节点，
             每个模块自身的 v-if 条件原样保留，只是套上「当前顺序位是否轮到自己」的判断。 -->
        <template v-for="widget in orderedWidgets" :key="widget">
          <section
            v-if="widget === 'author' && showModule('author')"
            class="sidebar-block author-card"
          >
            <div class="author-heading">
              <img
                v-if="siteStore.settings.authorAvatar"
                :src="getSafeImageUrl(siteStore.settings.authorAvatar)"
                alt=""
              /><span v-else class="author-initial" aria-hidden="true">{{
                siteStore.settings.authorName.slice(0, 1)
              }}</span>
              <div class="author-meta">
                <h2>{{ siteStore.settings.authorName }}</h2>
                <p class="author-role">本站唯一作者</p>
              </div>
            </div>
            <p>{{ siteStore.settings.authorBio }}</p>
            <router-link to="/about" class="text-link"
              >关于作者<Icon name="chevron-right" :size="14"
            /></router-link>
          </section>
          <section
            v-else-if="widget === 'tags' && showModule('tags') && tags.length"
            class="sidebar-block"
          >
            <h2>文章标签</h2>
            <div class="sidebar-tags">
              <router-link v-for="tag in tags" :key="tag.id" :to="`/tag/${tag.id}`">{{
                tag.name
              }}</router-link>
            </div>
            <router-link to="/tags" class="text-link sidebar-more"
              >查看全部标签<Icon name="chevron-right" :size="14"
            /></router-link>
          </section>
          <router-link
            v-else-if="widget === 'archive' && showModule('archiveLink')"
            to="/archives"
            class="archive-callout"
            ><Icon name="archive" :size="19" /><span>按时间浏览文章</span
            ><Icon name="chevron-right" :size="15"
          /></router-link>
        </template>
      </aside>
    </div>
  </PageFrame>
</template>

<style scoped>
.blog-home {
  max-width: var(--page-width, 1130px);
  margin-inline: auto;
  padding: var(--page-padding-block, 24px) 30px 56px;
}

/* ---------------------------------------------------------------------------
   首屏介绍区
   ---------------------------------------------------------------------------
   构图：左侧文案（作者 / 大标题 / 简介 / 数据条），右侧封面图；
   没有封面时右侧留白，由 .intro-decor 的两团柔光补住画面重心。
   纸面 = --surface-paper（纯色画布下即 --surface-primary），边界用一条发丝线。 */
.intro-shell {
  position: relative;
  display: flex;
  align-items: center;
  overflow: hidden;
  min-height: 240px;
  padding: 36px 40px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-2xl);
  background: var(--surface-secondary);
  box-shadow: var(--shadow-xs);
}
.intro-copy {
  position: relative;
  z-index: 2;
  max-width: 680px;
  /* 首屏入场：文案先到，说明与数据条随后，像被「写上去」一样。 */
  animation: enter-up 460ms var(--ease-out) both;
}
.intro-author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  font-weight: 500;
  letter-spacing: 0.02em;
  animation: enter-fade 340ms var(--ease-out) both;
  animation-delay: 40ms;
}
/* 强调色小圆点：给首屏一个视觉起点，也提示这里可以点（作者名是纯文本，不是链接）。 */
.intro-dot {
  position: relative;
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-soft);
}
/* 呼吸圈：点一下再看，能注意到它在动。 */
.intro-dot::after {
  position: absolute;
  content: '';
  inset: 0;
  border-radius: inherit;
  background: var(--accent);
  opacity: 0;
  animation: pulse-ring 3.2s var(--ease-out) infinite;
}
.intro-copy h1 {
  max-width: 620px;
  font-size: clamp(30px, 3.6vw, 44px);
  font-weight: 700;
  line-height: 1.28;
  letter-spacing: -0.03em;
  overflow-wrap: anywhere;
  animation: enter-up 460ms var(--ease-out) both;
  animation-delay: 60ms;
}
.intro-description {
  margin-top: 16px;
  max-width: 52ch;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.85;
  animation: enter-up 460ms var(--ease-out) both;
  animation-delay: 110ms;
}
/* 数据条：文章数 / 标签数 / 最近更新。用「·」分隔而不是竖线：
   换行时竖线会在行首留下一道孤零零的竖线，点号则天然属于后一项。 */
.intro-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 14px;
  margin-top: 22px;
  animation: enter-fade 420ms var(--ease-out) both;
  animation-delay: 160ms;
}
.intro-stat {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
  font-size: var(--text-xs);
}
.intro-stat dt {
  color: var(--text-muted);
}
.intro-stat dd {
  color: var(--text-primary);
  font-weight: 600;
}
/* 项目之间的分隔不用伪元素圆点：窄屏换行时它会孤零零地落在行首。
   靠 gap + 「标签弱化、数值加粗」的对比已经足够区分。 */
/* 封面图占据右半幅；左侧用纸面渐变把文字从图上「推」出来。 */
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
  pointer-events: none;
  background: linear-gradient(
    90deg,
    var(--surface-secondary) 34%,
    color-mix(in srgb, var(--surface-secondary) 93%, transparent) 56%,
    color-mix(in srgb, var(--surface-secondary) 12%, transparent)
  );
}
/* 有封面时才需要那层渐变遮罩；没有封面时遮罩会把装饰柔光一起压掉。 */
.intro-shell:not(.intro-shell--cover)::after {
  content: none;
}
/* 装饰柔光：两团低饱和的强调色光晕，给纯色纸面一点纵深。
   只在「无横幅且无封面」时出现，位在文案之下。 */
.intro-decor {
  position: absolute;
  z-index: 0;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(46% 78% at 88% 8%, var(--accent-soft), transparent 72%),
    radial-gradient(
      38% 64% at 74% 96%,
      color-mix(in srgb, var(--accent) 6%, transparent),
      transparent 70%
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
.intro-shell--banner-centered .intro-stats {
  justify-content: center;
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

/* ---------------------------------------------------------------------------
   文章流 + 侧栏
   --------------------------------------------------------------------------- */
.home-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 248px;
  gap: var(--page-section-gap, 32px);
  margin-top: var(--page-section-gap, 36px);
}
.home-content--full {
  grid-template-columns: minmax(0, 1fr);
}
.article-column {
  min-width: 0;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  animation: enter-fade 420ms var(--ease-out) both;
  animation-delay: 80ms;
}
.section-tools {
  display: flex;
  align-items: center;
  gap: 14px;
}
/* 排序分段控件：一枚胶囊底座 + 三个等宽选项。当前档是白底浮起的那一枚。 */
.sort-switch {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  background: var(--surface-tertiary);
}
.sort-switch button {
  min-height: 30px;
  padding-inline: 13px;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: var(--text-xs);
  font-weight: 500;
  transition:
    background var(--transition-fast),
    color var(--transition-fast),
    box-shadow var(--transition-fast);
}
.sort-switch button:hover {
  color: var(--text-primary);
}
.sort-switch button.active {
  background: var(--surface-primary);
  color: var(--accent);
  font-weight: 600;
  box-shadow: var(--shadow-xs);
}
.section-heading h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--text-xl);
  letter-spacing: -0.02em;
}
/* 标题前的强调色短横：给分区一个稳定的视觉锚点。 */
.section-rule {
  width: 18px;
  height: 3px;
  border-radius: var(--radius-full);
  background: var(--accent);
}
.result-count {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.home-sidebar {
  position: sticky;
  top: calc(var(--navbar-height) + 28px);
  align-self: start;
  /* 侧栏从右侧滑入：与主栏的入场错开 120ms，两边不会同时「啪」地出现。 */
  animation: enter-right 480ms var(--ease-out) both;
  animation-delay: 100ms;
}
.sidebar-block {
  padding: 22px;
  margin-bottom: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  /* 浮起档：纯色画布下别名回 --surface-primary（逐像素不变），
     壁纸画布下换成比纸面深一档的实底，卡片才从纸面上「浮」出来。 */
  background: var(--surface-raised);
  box-shadow: var(--shadow-xs);
}
.sidebar-block h2 {
  font-size: var(--text-sm);
  letter-spacing: 0.01em;
  overflow-wrap: anywhere;
}
.author-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}
.author-heading img,
.author-initial {
  display: grid;
  flex: 0 0 42px;
  place-items: center;
  width: 42px;
  height: 42px;
  object-fit: cover;
  border-radius: var(--radius-full);
  background: var(--accent-light);
  color: var(--accent);
  font-size: var(--text-lg);
  font-weight: 600;
}
.author-meta {
  min-width: 0;
}
.author-role {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.author-card p {
  margin-block: 15px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.9;
  overflow-wrap: anywhere;
}
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  font-size: var(--text-xs);
  font-weight: 500;
}
/* 「查看全部标签」：紧跟在标签芯片下方，与上一行留一点呼吸。 */
.sidebar-more {
  margin-top: 12px;
}
.sidebar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 14px;
}
.sidebar-tags a {
  padding: 4px 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  background: var(--surface-tertiary);
  color: var(--text-secondary);
  font-size: var(--text-2xs);
  transition:
    color var(--transition-fast),
    border-color var(--transition-fast),
    background var(--transition-fast);
}
.sidebar-tags a:hover {
  background: var(--accent-light);
  border-color: transparent;
  color: var(--accent);
}
.archive-callout {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
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
  padding: 34px 26px;
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
  margin-top: 24px;
  font-size: var(--text-xs);
  color: var(--text-muted);
}
.home-pagination button {
  display: flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  padding-inline: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  background: var(--surface-primary);
  color: var(--text-primary);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast);
}
.home-pagination button:hover:not(:disabled) {
  background: var(--accent-light);
  border-color: transparent;
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
    padding: 8px 18px 40px;
  }
  .intro-shell {
    min-height: 224px;
    padding: 26px 24px;
    border-radius: var(--radius-xl);
  }
  .intro-copy h1 {
    font-size: var(--text-3xl);
    max-width: 95%;
  }
  .intro-description {
    max-width: 100%;
  }
  /* 窄屏下首屏改为「文字在上、封面在下」的堆叠：侧置封面会占掉 69% 宽度，
     文案只能压到图上，可读性与层次都变差。堆叠后文字有整幅纸面可站。
     只作用于没有横幅的默认首屏 —— 横幅有自己的版式（居中/分栏/堆叠）。 */
  .intro-shell:not(.intro-shell--banner) {
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    gap: 18px;
  }
  .intro-shell:not(.intro-shell--banner) .intro-copy {
    order: 1;
  }
  .intro-shell:not(.intro-shell--banner) .intro-visual {
    position: relative;
    inset: auto;
    order: 2;
    width: 100%;
    height: 168px;
    overflow: hidden;
    border-radius: var(--radius-lg);
    opacity: 1;
  }
  /* 堆叠之后不再需要「把文字从图上推开」的横向遮罩。 */
  .intro-shell:not(.intro-shell--banner)::after {
    content: none;
  }
  html[data-wallpaper] #selfblog .intro-shell:not(.intro-shell--banner)::after {
    content: none;
  }
  /* 横幅：窄屏下左右分栏退化成上下堆叠；min-height 仍按作者选的高度档位，
     上面那条 .intro-shell { min-height: 224px } 会盖掉横幅档位，这里补回来。 */
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
    margin-top: 30px;
  }
  .section-heading h2 {
    font-size: var(--text-lg);
  }
}
</style>
