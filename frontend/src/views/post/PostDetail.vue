<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { postApi } from '@/api/post'
import {
  publicInteractionApi,
  type AnonymousLike,
  type GuestComment
} from '@/api/public-interactions'
import { formatDate, getSafeImageUrl } from '@/utils'
import { estimateReadMinutes } from '@/utils/reading'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { usePageSeo } from '@/composables/usePageSeo'
import { enhanceMarkdown, disposeMarkdownEnhance } from '@/composables/useMarkdownEnhance'
import { toast } from '@/utils/toast'
import ImagePreview from '@/components/common/ImagePreview.vue'
import MobileTocSheet from '@/components/post/MobileTocSheet.vue'
import Icon from '@/components/common/Icon.vue'
import type { Post } from '@/types'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { STORAGE_KEY_PREFIX } from '@/constants'
const { config, text, module: showModule, isPreview, preview } = usePageConfig('post')

interface TocItem {
  id: string
  text: string
  level: number
}

const route = useRoute()
const userStore = useUserStore()
const siteStore = useSiteStore()
const post = ref<Post | null>(null)
usePageSeo('post', () => ({
  title: post.value?.title || '文章详情',
  description:
    post.value?.summary || post.value?.content.replace(/[#*`>_~-]/g, '').slice(0, 140) || '',
  image: post.value?.coverUrl || post.value?.coverImage,
  url: `/post/${post.value?.id || route.params.id}`,
  type: post.value ? 'article' : 'website',
  publishedTime: post.value?.createTime || post.value?.createdAt,
  author: post.value?.nickname || post.value?.authorName,
  tags: post.value?.tags?.map((tag) => tag.name),
  indexable: !!post.value
}))
/** 上一篇 / 下一篇，缺失的方向为 null */
const adjacent = ref<{
  prev: { id: number; title: string } | null
  next: { id: number; title: string } | null
}>({
  prev: null,
  next: null
})
const loading = ref(true)
const error = ref('')
const readingProgress = ref(0)
const activeHeading = ref('')
const toc = ref<TocItem[]>([])
const contentRef = ref<HTMLElement | null>(null)
const likeInfo = ref<AnonymousLike>({ liked: false, likeCount: 0 })
const comments = ref<GuestComment[]>([])
const commentsTotal = ref(0)
const commentPage = ref(1)
const commentsLoading = ref(false)
const commentsError = ref('')
const commentNickname = ref('访客')
const commentContent = ref('')
const submittingComment = ref(false)
const liking = ref(false)
/** 正文图片 URL（增强时收集，用于灯箱）与灯箱状态 */
const contentImages = ref<string[]>([])
const previewVisible = ref(false)
const previewIndex = ref(0)

/** 正文及其附属数据（相邻文章 / 点赞状态）共用一条令牌：切换文章后旧响应不得写回。 */
const beginPostRequest = useLatestRequest().begin
/** 留言单独一条令牌：翻页与切换文章互不干扰。 */
const beginCommentRequest = useLatestRequest().begin

const renderedContent = computed(() => {
  if (!post.value?.content) return ''
  const rawHtml = marked.parse(post.value.content) as string
  return DOMPurify.sanitize(rawHtml, {
    USE_PROFILES: { html: true },
    ADD_ATTR: ['target', 'rel', 'referrerpolicy'],
    FORBID_TAGS: ['style', 'script', 'iframe', 'form'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick']
  })
    .replace(/<a\s/g, '<a target="_blank" rel="noopener noreferrer" referrerpolicy="no-referrer" ')
    .replace(/<img\s/g, '<img referrerpolicy="no-referrer" loading="lazy" ')
})

/**
 * 预计阅读时长：按"中日韩字符按字 + 西文按词"统计正文后再除以每分钟词数。
 * 不用 content.length —— 那会把代码块、图片链接和中英混排一起算错。
 */
const estimatedReadMinutes = computed(() => estimateReadMinutes(post.value?.content || ''))

/** 最后更新提示的阈值：7 天内的小改动不值得在阅读入口提示。 */
const UPDATE_NOTICE_THRESHOLD_MS = 7 * 24 * 60 * 60 * 1000

/**
 * "最后更新于"的日期：仅当 updateTime 比 createTime 晚 7 天以上才展示；
 * 任一字段缺失或无法解析时不展示（没有可靠的比较基准）。
 */
const lastUpdated = computed(() => {
  const updated = post.value?.updateTime
  const created = post.value?.createTime || post.value?.createdAt
  if (!updated || !created) return ''
  const updatedAt = new Date(updated).getTime()
  const createdAt = new Date(created).getTime()
  if (Number.isNaN(updatedAt) || Number.isNaN(createdAt)) return ''
  if (updatedAt - createdAt <= UPDATE_NOTICE_THRESHOLD_MS) return ''
  return formatDate(updated, 'YYYY年MM月DD日')
})

const fetchPost = async () => {
  const isLatest = beginPostRequest()
  loading.value = true
  error.value = ''
  post.value = null
  try {
    const response = await postApi.getPostById(preview?.articleId?.value || String(route.params.id))
    if (!isLatest()) return
    if (!response.data) {
      error.value = '文章不存在或已被删除。'
      return
    }
    post.value = response.data
    comments.value = []
    commentsTotal.value = 0
    commentPage.value = 1
    commentsError.value = ''
    likeInfo.value = { liked: false, likeCount: response.data.likeCount || 0 }
    void loadLikeStatus(isLatest)
    void loadAdjacent(isLatest)
    if (siteStore.isCommentsEnabled) void loadComments()
  } catch (err: unknown) {
    if (!isLatest()) return
    const status = (err as { response?: { status?: number } }).response?.status
    if (status === 404) error.value = '文章不存在或已被删除。'
    else if (status === 403) error.value = '当前账号无权查看这篇文章。'
    else error.value = '文章暂时无法加载，请稍后重试。'
  } finally {
    if (isLatest()) loading.value = false
  }
}

/**
 * 相邻文章 / 点赞状态跟着正文走：共用 fetchPost 的令牌，切换文章后旧响应不得写回；
 * 取不到时静默处理，不影响正文展示。
 */
async function loadAdjacent(isLatest: () => boolean) {
  const id = post.value?.id
  if (!id) return
  try {
    const response = await postApi.getAdjacentPosts(id)
    if (!isLatest()) return
    adjacent.value = response.data || { prev: null, next: null }
  } catch {
    if (!isLatest()) return
    adjacent.value = { prev: null, next: null }
  }
}

async function loadLikeStatus(isLatest: () => boolean) {
  if (!post.value) return
  try {
    const response = await publicInteractionApi.getLikeStatus(Number(post.value.id))
    if (!isLatest()) return
    if (response.data) likeInfo.value = response.data
  } catch {
    // Keep the article's existing like total visible if anonymous state is unavailable.
  }
}

async function toggleAnonymousLike() {
  if (!post.value || liking.value || isPreview) return
  liking.value = true
  try {
    const response = await publicInteractionApi.toggleLike(Number(post.value.id))
    if (response.data) likeInfo.value = response.data
  } catch (err) {
    toast.error((err as Error & { userMessage?: string }).userMessage || '点赞失败，请稍后重试。')
  } finally {
    liking.value = false
  }
}

async function loadComments(append = false) {
  if (!post.value || !siteStore.isCommentsEnabled) return
  const requestPage = append ? commentPage.value + 1 : commentPage.value
  const isLatest = beginCommentRequest()
  commentsLoading.value = true
  commentsError.value = ''
  try {
    const response = await publicInteractionApi.getComments(Number(post.value.id), {
      pageNum: requestPage,
      pageSize: 10
    })
    if (!isLatest()) return
    const records = response.data?.records || []
    comments.value = append ? [...comments.value, ...records] : records
    commentsTotal.value = response.data?.total || 0
    // 只有新一页真的到手才推进页码；失败时停留原页，重试不会跳过这一页。
    commentPage.value = requestPage
  } catch {
    if (!isLatest()) return
    // 失败时保留已加载的留言：清空列表会把「加载更多失败」变成「留言全没了」。
    commentsError.value = '留言暂时无法加载，请稍后重试。'
  } finally {
    if (isLatest()) commentsLoading.value = false
  }
}

async function loadMoreComments() {
  if (commentsLoading.value) return
  await loadComments(true)
}

async function submitComment() {
  if (!post.value || submittingComment.value || isPreview) return
  const nickname = commentNickname.value.trim()
  const content = commentContent.value.trim()
  if (!nickname || !content) {
    toast.warning('请填写昵称和评论内容。')
    return
  }
  submittingComment.value = true
  try {
    await publicInteractionApi.createComment({ postId: Number(post.value.id), nickname, content })
    try {
      localStorage.setItem(STORAGE_KEY_PREFIX + 'comment-name', nickname)
    } catch {
      /* storage is optional */
    }
    commentContent.value = ''
    commentPage.value = 1
    await loadComments()
    toast.success('评论已发布，感谢分享。')
  } catch (err) {
    toast.error(
      (err as Error & { userMessage?: string }).userMessage || '评论提交失败，请稍后重试。'
    )
  } finally {
    submittingComment.value = false
  }
}

const extractToc = (markdown: string): TocItem[] => {
  const withoutCode = markdown.replace(/```[\s\S]*?```/g, '')
  return [...withoutCode.matchAll(/^(#{1,3})\s+(.+?)\s*#*\s*$/gm)].map((match, index) => ({
    id: `section-${index}`,
    text: (match[2] || '').replace(/[`*_~]/g, '').trim(),
    level: (match[1] || '').length
  }))
}

const setHeadingIds = async () => {
  await nextTick()
  const headings = contentRef.value?.querySelectorAll('h1, h2, h3') || []
  headings.forEach((heading, index) => {
    heading.id = `section-${index}`
  })
}

/**
 * 标题高亮：用 IntersectionObserver 取代"每次 scroll 遍历全部标题"。
 * 观察区是固定导航栏下沿到视口下半部（上边留 110px，下边砍掉 60%）；
 * 落在区内、boundingClientRect.top 最小的标题就是"正在读"的那一条。
 */
let headingObserver: IntersectionObserver | null = null

function observeHeadings(): void {
  headingObserver?.disconnect()
  // 不支持 IntersectionObserver 的环境（如 jsdom）由 scroll 扫描兜底，见 updateReadingProgress
  if (typeof IntersectionObserver === 'undefined' || !contentRef.value) return
  const headings = contentRef.value.querySelectorAll('h1, h2, h3')
  if (!headings.length) return
  headingObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries.filter((entry) => entry.isIntersecting)
      // 没有标题进入观察区时保持上一个高亮：滚过最后一条标题后不该突然清空目录状态
      if (!visible.length) return
      const topmost = visible.reduce((top, entry) =>
        entry.boundingClientRect.top < top.boundingClientRect.top ? entry : top
      )
      if (topmost.target.id) activeHeading.value = topmost.target.id
    },
    { rootMargin: '-110px 0px -60% 0px' }
  )
  headings.forEach((heading) => headingObserver?.observe(heading))
}

watch(
  () => post.value?.content,
  async (content) => {
    toc.value = content ? extractToc(content) : []
    if (!content) return
    await setHeadingIds()
    // 标题 id 必须先落地：锚点 href 指向 #section-N，灯箱索引也依赖增强后的结构。
    // 增强加工的是真实 DOM，所以放在 v-html 渲染完成（nextTick）之后。
    if (contentRef.value) {
      // 旧正文的复制按钮随 v-html 一起被丢弃，先释放它排队的回退定时器
      disposeMarkdownEnhance(contentRef.value)
      contentImages.value = enhanceMarkdown(contentRef.value)
    }
    observeHeadings()
  }
)

const scrollToHeading = (item: TocItem) => {
  const heading = contentRef.value?.querySelector(`#${item.id}`)
  if (!heading) return
  heading.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    block: 'start'
  })
  activeHeading.value = item.id
}

/**
 * v-html 里的图片没有事件绑定，只能在正文容器上做事件委托：
 * 用原始 src（与 enhanceMarkdown 收集时一致）定位点击的是第几张，灯箱从它开始浏览。
 */
function onContentClick(event: MouseEvent): void {
  const target = event.target
  if (!(target instanceof HTMLImageElement)) return
  const src = target.getAttribute('src')
  if (!src) return
  const index = contentImages.value.indexOf(src)
  if (index < 0) return
  previewIndex.value = index
  previewVisible.value = true
}

const updateReadingProgress = () => {
  const documentHeight = document.documentElement.scrollHeight - window.innerHeight
  readingProgress.value =
    documentHeight > 0 ? Math.min(100, Math.round((window.scrollY / documentHeight) * 100)) : 0

  // 标题高亮已交给 IntersectionObserver；只有它不可用时才回退到整表扫描，
  // 避免长文里每次 scroll 都遍历全部标题。
  if (typeof IntersectionObserver !== 'undefined') return
  const headings = contentRef.value?.querySelectorAll('h1, h2, h3') || []
  let activeId = ''
  headings.forEach((heading) => {
    if (heading.getBoundingClientRect().top <= 120) activeId = heading.id
  })
  activeHeading.value = activeId
}

let progressFrame = 0
function scheduleReadingProgress(): void {
  if (progressFrame) return
  progressFrame = requestAnimationFrame(() => {
    progressFrame = 0
    updateReadingProgress()
  })
}

watch(() => route.params.id, fetchPost)

onMounted(() => {
  try {
    // 旧版本用的是无前缀的裸键：一次性回退读取，避免老访客丢掉已保存的昵称。
    commentNickname.value =
      localStorage.getItem(STORAGE_KEY_PREFIX + 'comment-name') ||
      localStorage.getItem('selfblog-comment-name') ||
      '访客'
  } catch {
    /* storage is optional */
  }
  fetchPost()
  window.addEventListener('scroll', scheduleReadingProgress, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', scheduleReadingProgress)
  if (progressFrame) cancelAnimationFrame(progressFrame)
  headingObserver?.disconnect()
  if (contentRef.value) disposeMarkdownEnhance(contentRef.value)
})
</script>

<template>
  <div
    v-if="!isPreview"
    class="reading-progress"
    :style="{ width: `${readingProgress}%` }"
    aria-hidden="true"
  ></div>

  <PageFrame :config="config" class="reader-page">
    <div class="reader-topline">
      <router-link to="/" class="back-link"
        ><Icon name="chevron-left" :size="16" />所有文章</router-link
      >
      <router-link
        v-if="post && userStore.isAdmin"
        :to="`${siteStore.adminRoot}/edit/${post.id}`"
        class="edit-link"
        >编辑文章<Icon name="external" :size="14"
      /></router-link>
    </div>

    <div v-if="loading" class="state-panel" role="status">正在打开文章…</div>
    <div v-else-if="error" class="state-panel state-panel--error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="fetchPost">重新加载</button>
      <router-link to="/">返回首页</router-link>
    </div>

    <article
      v-else-if="post"
      class="reader-layout"
      :class="{ 'reader-layout--no-toc': !toc.length || !showModule('toc') }"
    >
      <div class="article-column">
        <header class="article-header">
          <div v-if="post.tags?.length" class="article-tags">
            <router-link v-for="tag in post.tags" :key="tag.id" :to="`/tag/${tag.id}`"
              >#{{ tag.name }}</router-link
            >
          </div>
          <p v-if="post.category" class="article-category">{{ post.category }}</p>
          <h1>{{ post.title }}</h1>
          <p v-if="text('description', '')" class="article-summary">
            {{ text('description', '') }}
          </p>
          <p v-if="post.summary" class="article-summary">{{ post.summary }}</p>
          <div v-if="showModule('meta')" class="article-meta">
            <time :datetime="post.createTime || post.createdAt">
              {{ formatDate(post.createTime || post.createdAt, 'YYYY年MM月DD日') }}
            </time>
            <span class="meta-dot" aria-hidden="true"></span>
            <span>约 {{ estimatedReadMinutes }} 分钟阅读</span>
            <span class="meta-dot" aria-hidden="true"></span>
            <span>{{ post.viewCount || 0 }} 次阅读</span>
            <template v-if="lastUpdated">
              <span class="meta-dot" aria-hidden="true"></span>
              <span>最后更新于 {{ lastUpdated }}</span>
            </template>
          </div>
        </header>

        <img
          v-if="showModule('cover') && (post.coverImage || post.coverUrl)"
          class="article-cover"
          :src="getSafeImageUrl(post.coverImage || post.coverUrl)"
          alt=""
        />

        <div
          ref="contentRef"
          class="markdown-body"
          v-html="renderedContent"
          @click="onContentClick"
        ></div>

        <nav
          v-if="showModule('adjacent') && (adjacent.prev || adjacent.next)"
          class="post-adjacent"
          aria-label="相邻文章"
        >
          <router-link v-if="adjacent.prev" class="adjacent-link" :to="`/post/${adjacent.prev.id}`">
            <span class="adjacent-label"><Icon name="chevron-left" :size="14" />上一篇</span>
            <span class="adjacent-title">{{ adjacent.prev.title }}</span>
          </router-link>
          <router-link
            v-if="adjacent.next"
            class="adjacent-link adjacent-link--next"
            :to="`/post/${adjacent.next.id}`"
          >
            <span class="adjacent-label">下一篇<Icon name="chevron-right" :size="14" /></span>
            <span class="adjacent-title">{{ adjacent.next.title }}</span>
          </router-link>
        </nav>

        <section class="reader-interactions" :aria-label="`《${post.title}》的互动`">
          <button
            class="like-action"
            :class="{ active: likeInfo.liked }"
            type="button"
            :disabled="liking"
            @click="toggleAnonymousLike"
          >
            <span aria-hidden="true">{{ likeInfo.liked ? '♥' : '♡' }}</span>
            {{ likeInfo.liked ? '已点赞' : '匿名点赞' }} <strong>{{ likeInfo.likeCount }}</strong>
          </button>
          <span class="interaction-note">无需登录，点赞状态保存在本设备</span>
        </section>

        <section v-if="siteStore.isCommentsEnabled" class="comments-section">
          <header class="comments-heading">
            <h2>
              读者留言 <span>{{ commentsTotal }}</span>
            </h2>
          </header>
          <form class="comment-form" @submit.prevent="submitComment">
            <label
              ><span>昵称</span
              ><input v-model="commentNickname" maxlength="80" placeholder="访客" required
            /></label>
            <label
              ><span>留言内容</span
              ><textarea
                v-model="commentContent"
                maxlength="2000"
                rows="4"
                placeholder="写下你的想法…"
                required
              ></textarea>
            </label>
            <div class="comment-form-footer">
              <small>友善交流，请勿填写敏感个人信息。</small
              ><button type="submit" :disabled="submittingComment">
                {{ submittingComment ? '正在发布…' : '发布留言' }}
              </button>
            </div>
          </form>
          <div v-if="comments.length" class="comment-list">
            <article v-for="comment in comments" :key="comment.id" class="reader-comment">
              <div class="comment-avatar" aria-hidden="true">
                {{ comment.nickname.slice(0, 1) }}
              </div>
              <div>
                <header>
                  <strong>{{ comment.nickname }}</strong
                  ><time>{{ formatDate(comment.createTime, 'YYYY-MM-DD HH:mm') }}</time>
                </header>
                <p>{{ comment.content }}</p>
              </div>
            </article>
          </div>
          <p v-if="commentsError" class="comments-error" role="alert">{{ commentsError }}</p>
          <p v-else-if="!comments.length" class="no-comments">
            还没有留言，欢迎成为第一个留言的人。
          </p>
          <button
            v-if="comments.length < commentsTotal"
            class="load-more-comments"
            type="button"
            :disabled="commentsLoading"
            @click="loadMoreComments"
          >
            {{ commentsLoading ? '正在加载…' : '加载更多留言' }}
          </button>
        </section>

        <footer class="article-end">
          <span class="end-rule"></span>
          <p>感谢阅读</p>
          <router-link to="/archives"
            >继续浏览文章归档<Icon name="chevron-right" :size="14"
          /></router-link>
        </footer>
      </div>

      <aside v-if="showModule('toc') && toc.length" class="article-aside">
        <div class="toc-card">
          <h2>文章目录</h2>
          <nav aria-label="文章目录">
            <button
              v-for="item in toc"
              :key="item.id"
              type="button"
              :class="[`level-${item.level}`, { active: activeHeading === item.id }]"
              @click="scrollToHeading(item)"
            >
              {{ item.text }}
            </button>
          </nav>
        </div>
      </aside>
    </article>

    <MobileTocSheet
      v-if="showModule('toc') && toc.length && !isPreview"
      :items="toc"
      :active-id="activeHeading"
      @select="scrollToHeading"
    />

    <ImagePreview
      v-if="previewVisible"
      :images="contentImages"
      :initial-index="previewIndex"
      :show="true"
      @close="previewVisible = false"
    />
  </PageFrame>
</template>

<style scoped>
.reading-progress {
  position: fixed;
  z-index: var(--z-sticky);
  top: 0;
  left: 0;
  height: 2px;
  background: var(--primary);
  transition: width 90ms linear;
}
.reader-page {
  max-width: var(--page-width, 1120px);
  margin: 0 auto;
  padding: var(--page-padding-block, 24px) 24px 48px;
  color: var(--text-primary);
}
.reader-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.back-link,
.edit-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  color: var(--text-muted);
  font-size: var(--text-xs);
  text-decoration: none;
}
.back-link:hover,
.edit-link:hover {
  color: var(--primary);
}
.edit-link {
  color: var(--primary);
}
.reader-layout {
  display: grid;
  grid-template-columns: minmax(0, 760px) 200px;
  justify-content: center;
  gap: var(--page-section-gap, 32px);
  align-items: start;
}
.reader-layout--no-toc {
  grid-template-columns: minmax(0, 760px);
}
.article-column {
  min-width: 0;
}
.article-cover {
  display: block;
  width: 100%;
  max-height: 410px;
  margin-block: 24px 0;
  border-radius: var(--radius-lg);
  object-fit: cover;
}
.article-header {
  padding: 4px 0 25px;
  border-bottom: 1px solid var(--border-solid);
}
.reader-interactions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 25px 0 30px;
  padding: 16px 0;
  border-top: 1px solid var(--border-solid);
  border-bottom: 1px solid var(--border-solid);
}
.like-action {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 7px;
  min-height: 44px;
  padding: 9px 13px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-secondary);
  font: inherit;
  font-size: var(--text-sm);
  cursor: pointer;
}
.like-action span {
  color: var(--like-accent);
  font-size: var(--text-lg);
}
.like-action.active {
  border-color: var(--like-accent-border);
  background: var(--like-accent-soft);
  color: var(--like-accent-strong);
}
.like-action:disabled {
  opacity: 0.6;
  cursor: wait;
}
.like-action strong {
  font-size: var(--text-xs);
}
.interaction-note {
  color: var(--text-muted);
  font-size: var(--text-xs);
}

/* 上一篇 / 下一篇：博客阅读器的标准出口，避免读完只能在浏览器里按后退 */
.post-adjacent {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin: 30px 0 8px;
}
.adjacent-link {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 14px 16px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  background: var(--surface-primary);
  text-decoration: none;
  transition:
    border-color var(--duration-fast) var(--ease-default),
    transform var(--duration-fast) var(--ease-default);
}
.adjacent-link:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}
.adjacent-link--next {
  text-align: right;
}
.adjacent-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.adjacent-title {
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
@media (max-width: 640px) {
  .adjacent-link--next {
    text-align: left;
  }
}
.comments-section {
  margin: 46px 0 30px;
}
.comments-heading {
  margin-bottom: 18px;
}
.comments-heading h2 {
  margin: 0;
  font: 700 1.5rem var(--font-display);
}
.comments-heading h2 span {
  margin-left: 5px;
  color: var(--text-muted);
  font: 500 0.85rem var(--font-sans);
}
.comment-form {
  display: grid;
  gap: 12px;
  padding: 17px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  background: var(--surface-primary);
}
.comment-form label {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.comment-form input,
.comment-form textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-sm);
  background: var(--bg-primary);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
}
.comment-form textarea {
  resize: vertical;
}
.comment-form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.comment-form-footer small {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.comment-form-footer button {
  padding: 9px 13px;
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: white;
  font-size: var(--text-xs);
  font-weight: 700;
  cursor: pointer;
}
.comment-form-footer button:disabled {
  opacity: 0.55;
}
.comment-list {
  display: grid;
  gap: 16px;
  margin-top: 22px;
}
.reader-comment {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 12px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--border-solid);
}
.comment-avatar {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 700;
}
.reader-comment header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.reader-comment header strong {
  font-size: var(--text-sm);
}
.reader-comment time {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.reader-comment p {
  margin: 7px 0 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.no-comments {
  padding: 23px 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
  text-align: center;
}
.comments-error {
  margin: 0;
  padding: 12px 0;
  color: var(--danger);
  font-size: var(--text-sm);
  text-align: center;
}
.load-more-comments {
  display: block;
  margin: 16px auto;
  padding: 9px 13px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-sm);
  background: var(--surface-primary);
  color: var(--primary);
  font-size: var(--text-xs);
  cursor: pointer;
}
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 17px;
}
.article-tags a {
  color: var(--primary);
  font-size: var(--text-xs);
  text-decoration: none;
}
.article-tags a:hover {
  text-decoration: underline;
}
.article-category {
  margin: 0 0 9px;
  color: var(--primary);
  font-size: var(--text-2xs);
  font-weight: 700;
}
.article-header h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(28px, 3.2vw, 38px);
  line-height: 1.4;
  letter-spacing: -0.045em;
  overflow-wrap: anywhere;
}
.article-summary {
  margin: 17px 0 0;
  color: var(--text-secondary);
  font-size: var(--text-base);
  line-height: 1.8;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.meta-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--text-muted);
  opacity: 0.55;
}
.markdown-body {
  padding-top: 28px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: var(--text-lg);
  line-height: 1.9;
  overflow-wrap: anywhere;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 2em 0 0.7em;
  font-family: var(--font-display);
  line-height: 1.35;
  scroll-margin-top: 0;
}
.markdown-body :deep(h1) {
  font-size: var(--text-3xl);
}
.markdown-body :deep(h2) {
  padding-bottom: 0.45em;
  border-bottom: 1px solid var(--border-solid);
  font-size: var(--text-2xl);
}
.markdown-body :deep(h3) {
  font-size: var(--text-xl);
}
.markdown-body :deep(p) {
  margin: 0 0 1.35em;
}
.markdown-body :deep(a) {
  color: var(--primary);
  text-decoration-thickness: 1px;
  text-underline-offset: 3px;
}
.markdown-body :deep(blockquote) {
  margin: 1.7em 0;
  padding: 12px 18px;
  border-left: 3px solid var(--primary);
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.markdown-body :deep(pre) {
  overflow-x: auto;
  margin: 1.5em 0;
  padding: 17px;
  border-radius: var(--radius-sm);
  /* 用 --code-bg 而非 --gray-900：深色模式下灰度会反转，
     --gray-900 会变成 #EBEBEB，让代码块成为全页最亮的元素。 */
  background: var(--code-bg);
  color: var(--code-text);
  border: 1px solid var(--code-border);
  font-size: var(--text-sm);
  line-height: 1.65;
}
.markdown-body :deep(pre code) {
  color: inherit;
}
/* 代码块增强：头部 / 语言 / 复制 / 折叠由 useMarkdownEnhance 在真实 DOM 上生成，
   v-html 内容不带 scoped 属性，所以这里全部走 :deep()。 */
.markdown-body :deep(.code-block) {
  position: relative;
  margin: 1.5em 0;
  border: 1px solid var(--code-border);
  border-radius: var(--radius);
  background: var(--code-bg);
  overflow: hidden;
}
.markdown-body :deep(.code-block pre) {
  margin: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}
.markdown-body :deep(.code-block-head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 12px;
  border-bottom: 1px solid var(--code-border);
  color: var(--code-text);
}
.markdown-body :deep(.code-lang) {
  color: var(--code-text);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  opacity: 0.6;
}
.markdown-body :deep(.code-copy) {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 3px 12px;
  border: 1px solid var(--code-border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--code-text);
  font: inherit;
  font-size: var(--text-xs);
  opacity: 0.75;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.markdown-body :deep(.code-copy:hover) {
  border-color: var(--primary);
  background: var(--primary);
  color: var(--text-inverse);
  opacity: 1;
}
.markdown-body :deep(.code-expand) {
  display: block;
  width: 100%;
  padding: 8px;
  border: 0;
  border-top: 1px solid var(--code-border);
  background: transparent;
  color: var(--code-text);
  font: inherit;
  font-size: var(--text-xs);
  opacity: 0.7;
  cursor: pointer;
}
.markdown-body :deep(.code-expand:hover) {
  color: var(--text-inverse);
  opacity: 1;
}
/* 折叠阈值 320px 必须与 useMarkdownEnhance 的默认 collapseHeight 保持一致 */
.markdown-body :deep(.code-block.is-collapsed pre) {
  max-height: 320px;
  overflow-x: auto;
  overflow-y: hidden;
  /* 底部淡出用 mask 而不是伪元素：伪元素会跟着代码内容一起滚动 */
  -webkit-mask-image: linear-gradient(to bottom, #000 calc(100% - 64px), transparent);
  mask-image: linear-gradient(to bottom, #000 calc(100% - 64px), transparent);
}

/* 标题锚点：默认隐形，悬停标题时出现，避免正文里排满 # 干扰阅读 */
.markdown-body :deep(.heading-anchor) {
  margin-left: 8px;
  color: var(--text-muted);
  font-family: var(--font-sans);
  font-size: 0.7em;
  text-decoration: none;
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-default);
}
.markdown-body :deep(h1:hover .heading-anchor),
.markdown-body :deep(h2:hover .heading-anchor),
.markdown-body :deep(h3:hover .heading-anchor),
.markdown-body :deep(.heading-anchor:focus-visible) {
  opacity: 1;
}
/* 触屏没有悬停，锚点保持半透明可见 */
@media (hover: none) {
  .markdown-body :deep(.heading-anchor) {
    opacity: 0.45;
  }
}
.markdown-body :deep(:not(pre) > code) {
  padding: 0.12em 0.35em;
  border-radius: var(--radius-xs);
  background: var(--bg-secondary);
  color: var(--primary);
  font-size: 0.88em;
}
.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-sm);
  /* 正文图片可点开灯箱（onContentClick 事件委托） */
  cursor: zoom-in;
}
.markdown-body :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 8px 11px;
  border: 1px solid var(--border-solid);
  text-align: left;
}
.markdown-body :deep(li) {
  margin: 0.35em 0;
}
.article-end {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 46px 0 22px;
  text-align: center;
}
.end-rule {
  width: 46px;
  height: 2px;
  background: var(--primary);
  opacity: 0.55;
}
.article-end p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: var(--text-lg);
}
.article-end a {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  color: var(--primary);
  font-size: var(--text-xs);
  text-decoration: none;
}
.article-end a:hover {
  text-decoration: underline;
}
.article-aside {
  position: sticky;
  top: var(--anchor-offset);
}
.toc-card {
  padding: 17px 0;
  border-top: 1px solid var(--border-solid);
  border-bottom: 1px solid var(--border-solid);
}
.toc-card h2 {
  margin: 0 0 11px;
  font-family: var(--font-display);
  font-size: var(--text-base);
}
.toc-card nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-height: 60vh;
  overflow: auto;
}
.toc-card button {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 5px 0;
  border: 0;
  background: none;
  color: var(--text-muted);
  cursor: pointer;
  font: inherit;
  font-size: var(--text-xs);
  line-height: 1.5;
  text-align: left;
}
.toc-card button:hover,
.toc-card button.active {
  color: var(--primary);
}
.toc-card button.level-2 {
  padding-left: 10px;
}
.toc-card button.level-3 {
  padding-left: 20px;
}
.state-panel {
  max-width: 620px;
  margin: 65px auto;
  padding: 36px 22px;
  border: 1px dashed var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-secondary);
  text-align: center;
}
.state-panel p {
  margin: 0 0 12px;
}
.state-panel button,
.state-panel a {
  display: inline-block;
  margin: 0 5px;
  padding: 8px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font: inherit;
  font-size: var(--text-xs);
  text-decoration: none;
}
.state-panel button:hover,
.state-panel a:hover {
  border-color: var(--primary);
  color: var(--primary);
}
@media (max-width: 1020px) {
  .reader-layout {
    grid-template-columns: minmax(0, 760px);
  }
  /* 窄屏改用 MobileTocSheet（左下浮动胶囊 + 底部抽屉），侧边目录整块隐藏 */
  .article-aside {
    display: none;
  }
}
@media (max-width: 600px) {
  .reader-page {
    padding: 8px 18px 36px;
  }
  .reader-layout {
    gap: 25px;
  }
  .article-cover {
    max-height: 240px;
    margin-bottom: 0;
  }
  .article-header h1 {
    font-size: var(--text-3xl);
  }
  .markdown-body {
    font-size: var(--text-base);
  }
  .markdown-body :deep(h1) {
    font-size: var(--text-2xl);
  }
  .markdown-body :deep(h2) {
    font-size: var(--text-xl);
  }
  .reader-interactions {
    flex-wrap: wrap;
  }
  .comment-form-footer {
    flex-wrap: wrap;
  }
  .comment-form-footer button {
    min-height: 44px;
  }
  .reader-comment header {
    flex-wrap: wrap;
  }
}
</style>
