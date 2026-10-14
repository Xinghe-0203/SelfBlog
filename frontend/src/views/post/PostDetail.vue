<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { postApi } from '@/api/post'
import { publicInteractionApi, type AnonymousLike, type GuestComment } from '@/api/public-interactions'
import { formatDate, getSafeImageUrl } from '@/utils'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { toast } from '@/utils/toast'
import type { Post } from '@/types'

interface TocItem {
  id: string
  text: string
  level: number
}

const route = useRoute()
const userStore = useUserStore()
const siteStore = useSiteStore()
const post = ref<Post | null>(null)
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
const commentNickname = ref('访客')
const commentContent = ref('')
const submittingComment = ref(false)
const liking = ref(false)

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

const estimatedReadMinutes = computed(() => {
  const count = post.value?.content?.length || 0
  return Math.max(1, Math.ceil(count / 400))
})

const fetchPost = async () => {
  loading.value = true
  error.value = ''
  post.value = null
  try {
    const response = await postApi.getPostById(String(route.params.id))
    if (!response.data) {
      error.value = '文章不存在或已被删除。'
      return
    }
    post.value = response.data
    comments.value = []
    commentsTotal.value = 0
    commentPage.value = 1
    likeInfo.value = { liked: false, likeCount: response.data.likeCount || 0 }
    void loadLikeStatus()
    if (siteStore.isCommentsEnabled) void loadComments()
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } }).response?.status
    if (status === 404) error.value = '文章不存在或已被删除。'
    else if (status === 403) error.value = '当前账号无权查看这篇文章。'
    else error.value = '文章暂时无法加载，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function loadLikeStatus() {
  if (!post.value) return
  try {
    const response = await publicInteractionApi.getLikeStatus(Number(post.value.id))
    if (response.data) likeInfo.value = response.data
  } catch {
    // Keep the article's existing like total visible if anonymous state is unavailable.
  }
}

async function toggleAnonymousLike() {
  if (!post.value || liking.value) return
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
  try {
    const response = await publicInteractionApi.getComments(Number(post.value.id), { pageNum: commentPage.value, pageSize: 10 })
    const records = response.data?.records || []
    comments.value = append ? [...comments.value, ...records] : records
    commentsTotal.value = response.data?.total || 0
  } catch {
    comments.value = []
  }
}

async function loadMoreComments() {
  commentPage.value += 1
  await loadComments(true)
}

async function submitComment() {
  if (!post.value || submittingComment.value) return
  const nickname = commentNickname.value.trim()
  const content = commentContent.value.trim()
  if (!nickname || !content) {
    toast.warning('请填写昵称和评论内容。')
    return
  }
  submittingComment.value = true
  try {
    await publicInteractionApi.createComment({ postId: Number(post.value.id), nickname, content })
    try { localStorage.setItem('selfblog-comment-name', nickname) } catch { /* storage is optional */ }
    commentContent.value = ''
    commentPage.value = 1
    await loadComments()
    toast.success('评论已发布，感谢分享。')
  } catch (err) {
    toast.error((err as Error & { userMessage?: string }).userMessage || '评论提交失败，请稍后重试。')
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

watch(
  () => post.value?.content,
  async (content) => {
    toc.value = content ? extractToc(content) : []
    if (content) await setHeadingIds()
  }
)

const scrollToHeading = (item: TocItem) => {
  const heading = contentRef.value?.querySelector(`#${item.id}`)
  if (!heading) return
  heading.scrollIntoView({ behavior: 'smooth', block: 'start' })
  activeHeading.value = item.id
}

const updateReadingProgress = () => {
  const documentHeight = document.documentElement.scrollHeight - window.innerHeight
  readingProgress.value =
    documentHeight > 0 ? Math.min(100, Math.round((window.scrollY / documentHeight) * 100)) : 0

  const headings = contentRef.value?.querySelectorAll('h1, h2, h3') || []
  let activeId = ''
  headings.forEach((heading) => {
    if (heading.getBoundingClientRect().top <= 120) activeId = heading.id
  })
  activeHeading.value = activeId
}

watch(() => route.params.id, fetchPost)

onMounted(() => {
  try { commentNickname.value = localStorage.getItem('selfblog-comment-name') || '访客' } catch { /* storage is optional */ }
  fetchPost()
  window.addEventListener('scroll', updateReadingProgress, { passive: true })
})

onUnmounted(() => window.removeEventListener('scroll', updateReadingProgress))
</script>

<template>
  <div class="reading-progress" :style="{ width: `${readingProgress}%` }" aria-hidden="true"></div>

  <div class="reader-page">
    <div class="reader-topline">
      <router-link to="/" class="back-link">← 所有文章</router-link>
      <router-link v-if="post && userStore.isAdmin" :to="`${siteStore.adminRoot}/edit/${post.id}`" class="edit-link"
        >编辑文章 ↗</router-link
      >
    </div>

    <div v-if="loading" class="state-panel" role="status">正在打开文章…</div>
    <div v-else-if="error" class="state-panel state-panel--error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="fetchPost">重新加载</button>
      <router-link to="/">返回首页</router-link>
    </div>

    <article v-else-if="post" class="reader-layout">
      <div class="article-column">
        <img
          v-if="post.coverImage"
          class="article-cover"
          :src="getSafeImageUrl(post.coverImage)"
          :alt="post.title"
        />

        <header class="article-header">
          <div v-if="post.tags?.length" class="article-tags">
            <router-link v-for="tag in post.tags" :key="tag.id" :to="`/tag/${tag.id}`"
              >#{{ tag.name }}</router-link
            >
          </div>
          <p class="eyebrow">{{ post.category || '文章' }}</p>
          <h1>{{ post.title }}</h1>
          <p v-if="post.summary" class="article-summary">{{ post.summary }}</p>
          <div class="article-meta">
            <time :datetime="post.createTime || post.createdAt">
              {{ formatDate(post.createTime || post.createdAt, 'YYYY年MM月DD日') }}
            </time>
            <span class="meta-dot" aria-hidden="true"></span>
            <span>约 {{ estimatedReadMinutes }} 分钟阅读</span>
            <span class="meta-dot" aria-hidden="true"></span>
            <span>{{ post.viewCount || 0 }} 次阅读</span>
          </div>
        </header>

        <div ref="contentRef" class="markdown-body" v-html="renderedContent"></div>

        <section class="reader-interactions" :aria-label="`《${post.title}》的互动`">
          <button class="like-action" :class="{ active: likeInfo.liked }" type="button" :disabled="liking" @click="toggleAnonymousLike">
            <span aria-hidden="true">{{ likeInfo.liked ? '♥' : '♡' }}</span>
            {{ likeInfo.liked ? '已点赞' : '匿名点赞' }} <strong>{{ likeInfo.likeCount }}</strong>
          </button>
          <span class="interaction-note">无需登录，点赞状态保存在本设备</span>
        </section>

        <section v-if="siteStore.isCommentsEnabled" class="comments-section">
          <header class="comments-heading"><div><p class="eyebrow">GUESTBOOK</p><h2>读者留言 <span>{{ commentsTotal }}</span></h2></div></header>
          <form class="comment-form" @submit.prevent="submitComment">
            <label><span>昵称</span><input v-model="commentNickname" maxlength="80" placeholder="访客" required /></label>
            <label><span>留言内容</span><textarea v-model="commentContent" maxlength="2000" rows="4" placeholder="写下你的想法…" required></textarea></label>
            <div class="comment-form-footer"><small>友善交流，请勿填写敏感个人信息。</small><button type="submit" :disabled="submittingComment">{{ submittingComment ? '正在发布…' : '发布留言' }}</button></div>
          </form>
          <div v-if="comments.length" class="comment-list">
            <article v-for="comment in comments" :key="comment.id" class="reader-comment">
              <div class="comment-avatar" aria-hidden="true">{{ comment.nickname.slice(0, 1) }}</div>
              <div><header><strong>{{ comment.nickname }}</strong><time>{{ formatDate(comment.createTime, 'YYYY-MM-DD HH:mm') }}</time></header><p>{{ comment.content }}</p></div>
            </article>
          </div>
          <p v-else class="no-comments">还没有留言，欢迎成为第一个留言的人。</p>
          <button v-if="comments.length < commentsTotal" class="load-more-comments" type="button" @click="loadMoreComments">加载更多留言</button>
        </section>

        <footer class="article-end">
          <span class="end-rule"></span>
          <p>感谢阅读</p>
          <router-link to="/archives">继续浏览文章归档 →</router-link>
        </footer>
      </div>

      <aside v-if="toc.length" class="article-aside">
        <div class="toc-card">
          <p class="eyebrow">CONTENTS</p>
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
  </div>
</template>

<style scoped>
.reading-progress {
  position: fixed;
  z-index: 1200;
  top: 0;
  left: 0;
  height: 2px;
  background: var(--primary);
  transition: width 90ms linear;
}
.reader-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 32px 24px 20px;
  color: var(--text-primary);
}
.reader-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.back-link,
.edit-link {
  color: var(--text-muted);
  font-size: 0.8rem;
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
  grid-template-columns: minmax(0, 760px) 220px;
  justify-content: center;
  gap: 56px;
  align-items: start;
}
.article-column {
  min-width: 0;
}
.article-cover {
  display: block;
  width: 100%;
  max-height: 410px;
  margin-bottom: 34px;
  border-radius: 13px;
  object-fit: cover;
}
.article-header {
  padding: 4px 0 25px;
  border-bottom: 1px solid var(--border-solid);
}
.reader-interactions { display: flex; align-items: center; gap: 14px; margin: 25px 0 30px; padding: 16px 0; border-top: 1px solid var(--border-solid); border-bottom: 1px solid var(--border-solid); }
.like-action { display: inline-flex; align-items: center; gap: 7px; padding: 9px 13px; border: 1px solid var(--border-solid); border-radius: 999px; background: var(--surface-primary); color: var(--text-secondary); font: inherit; font-size: .82rem; cursor: pointer; }
.like-action span { color: #dc4762; font-size: 1.1rem; }.like-action.active { border-color: #f0bdc9; background: #fff0f3; color: #c82f50; }.like-action:disabled { opacity: .6; cursor: wait; }.like-action strong { font-size: .76rem; }
.interaction-note { color: var(--text-muted); font-size: .72rem; }
.comments-section { margin: 46px 0 30px; }
.comments-heading { margin-bottom: 18px; }.comments-heading .eyebrow { margin: 0 0 6px; color: var(--primary); font-size: .67rem; font-weight: 700; letter-spacing: .15em; }.comments-heading h2 { margin: 0; font: 700 1.5rem var(--font-display); }.comments-heading h2 span { margin-left: 5px; color: var(--text-muted); font: 500 .85rem var(--font-sans); }
.comment-form { display: grid; gap: 12px; padding: 17px; border: 1px solid var(--border-solid); border-radius: 12px; background: var(--surface-primary); }.comment-form label { display: grid; gap: 6px; color: var(--text-secondary); font-size: .75rem; }.comment-form input, .comment-form textarea { width: 100%; padding: 10px 12px; border: 1px solid var(--border-solid); border-radius: 8px; background: var(--bg-primary); color: var(--text-primary); font: inherit; font-size: .85rem; }.comment-form textarea { resize: vertical; }.comment-form-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.comment-form-footer small { color: var(--text-muted); font-size: .7rem; }.comment-form-footer button { padding: 9px 13px; border: 0; border-radius: 8px; background: var(--primary); color: white; font-size: .76rem; font-weight: 700; cursor: pointer; }.comment-form-footer button:disabled { opacity: .55; }
.comment-list { display: grid; gap: 16px; margin-top: 22px; }.reader-comment { display: grid; grid-template-columns: 36px minmax(0, 1fr); gap: 12px; padding-bottom: 15px; border-bottom: 1px solid var(--border-solid); }.comment-avatar { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 50%; background: var(--primary-light); color: var(--primary); font-weight: 700; }.reader-comment header { display: flex; align-items: center; gap: 10px; }.reader-comment header strong { font-size: .82rem; }.reader-comment time { color: var(--text-muted); font-size: .67rem; }.reader-comment p { margin: 7px 0 0; color: var(--text-secondary); font-size: .85rem; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }.no-comments { padding: 23px 0; color: var(--text-muted); font-size: .82rem; text-align: center; }
.load-more-comments { display: block; margin: 16px auto; padding: 9px 13px; border: 1px solid var(--border-solid); border-radius: 8px; background: var(--surface-primary); color: var(--primary); font-size: .78rem; cursor: pointer; }
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 17px;
}
.article-tags a {
  color: var(--primary);
  font-size: 0.75rem;
  text-decoration: none;
}
.article-tags a:hover {
  text-decoration: underline;
}
.eyebrow {
  margin: 0 0 9px;
  color: var(--primary);
  font-size: 0.69rem;
  font-weight: 700;
  letter-spacing: 0.14em;
}
.article-header h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(2rem, 5vw, 3.25rem);
  line-height: 1.15;
  letter-spacing: -0.045em;
  overflow-wrap: anywhere;
}
.article-summary {
  margin: 17px 0 0;
  color: var(--text-secondary);
  font-size: 1.03rem;
  line-height: 1.8;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  color: var(--text-muted);
  font-size: 0.76rem;
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
  font-size: 1.075rem;
  line-height: 1.9;
  overflow-wrap: anywhere;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 2em 0 0.7em;
  font-family: var(--font-display);
  line-height: 1.35;
  scroll-margin-top: 105px;
}
.markdown-body :deep(h1) {
  font-size: 1.85rem;
}
.markdown-body :deep(h2) {
  padding-bottom: 0.45em;
  border-bottom: 1px solid var(--border-solid);
  font-size: 1.55rem;
}
.markdown-body :deep(h3) {
  font-size: 1.25rem;
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
  border-radius: 9px;
  background: var(--gray-900);
  color: var(--gray-100);
  font-size: 0.82rem;
  line-height: 1.65;
}
.markdown-body :deep(pre code) {
  color: inherit;
}
.markdown-body :deep(:not(pre) > code) {
  padding: 0.12em 0.35em;
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--primary);
  font-size: 0.88em;
}
.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 9px;
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
  font-size: 1.1rem;
}
.article-end a {
  color: var(--primary);
  font-size: 0.8rem;
  text-decoration: none;
}
.article-end a:hover {
  text-decoration: underline;
}
.article-aside {
  position: sticky;
  top: 105px;
}
.toc-card {
  padding: 17px 0;
  border-top: 1px solid var(--border-solid);
  border-bottom: 1px solid var(--border-solid);
}
.toc-card .eyebrow {
  margin-bottom: 6px;
}
.toc-card h2 {
  margin: 0 0 11px;
  font-family: var(--font-display);
  font-size: 1rem;
}
.toc-card nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-height: 60vh;
  overflow: auto;
}
.toc-card button {
  padding: 5px 0;
  border: 0;
  background: none;
  color: var(--text-muted);
  cursor: pointer;
  font: inherit;
  font-size: 0.75rem;
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
  border-radius: 12px;
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
  border-radius: 7px;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font: inherit;
  font-size: 0.8rem;
  text-decoration: none;
}
.state-panel button:hover,
.state-panel a:hover {
  border-color: var(--primary);
  color: var(--primary);
}
@media (max-width: 860px) {
  .reader-layout {
    grid-template-columns: minmax(0, 760px);
  }
  .article-aside {
    position: static;
    order: -1;
  }
  .toc-card nav {
    max-height: 180px;
  }
}
@media (max-width: 600px) {
  .reader-page {
    padding: 26px 18px 14px;
  }
  .reader-layout {
    gap: 25px;
  }
  .article-cover {
    max-height: 240px;
    margin-bottom: 24px;
  }
  .article-header h1 {
    font-size: 2rem;
  }
  .markdown-body {
    font-size: 1rem;
  }
  .markdown-body :deep(h1) {
    font-size: 1.55rem;
  }
  .markdown-body :deep(h2) {
    font-size: 1.35rem;
  }
}
</style>
