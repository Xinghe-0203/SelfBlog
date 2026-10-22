<template>
  <div class="comments-page">
    <header class="page-heading">
      <div>
        <h1>评论管理</h1>
        <p>审核访客留言。评论展示开关位于网站设置。</p>
      </div>
      <div class="heading-actions">
        <button
          class="scan-button"
          type="button"
          :disabled="scanning"
          data-action="ai-scan"
          @click="runScan"
        >
          <Icon name="sparkle" :size="14" />{{ scanning ? '扫描中…' : '批量扫描' }}
        </button>
        <select v-model="statusFilter" aria-label="按状态筛选" @change="load">
          <option value="">全部状态</option>
          <option value="1">已展示</option>
          <option value="0">已隐藏</option>
          <option value="2">垃圾评论</option>
        </select>
      </div>
    </header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="scanNotice" class="scan-notice" role="status" data-scan-notice>{{ scanNotice }}</p>
    <section class="comment-list" aria-live="polite">
      <article v-for="comment in comments" :key="comment.id" class="comment-card">
        <div class="comment-top">
          <div>
            <strong>{{ comment.nickname }}</strong
            ><span
              >评论于
              <router-link :to="`/post/${comment.postId}`" target="_blank">{{
                comment.postTitle || `文章 #${comment.postId}`
              }}</router-link></span
            >
          </div>
          <time>{{ formatDate(comment.createTime) }}</time>
        </div>
        <p class="comment-content">{{ comment.content }}</p>
        <div v-if="verdictFor(comment.id)" class="verdict-row" data-verdict-row>
          <span class="verdict-badge" :data-verdict="verdictFor(comment.id)?.verdict">
            {{ verdictName(verdictFor(comment.id)?.verdict) }}
          </span>
          <span class="verdict-reason">{{ verdictFor(comment.id)?.reason }}</span>
          <span
            v-for="category in verdictFor(comment.id)?.categories || []"
            :key="category"
            class="verdict-category"
            >{{ categoryName(category) }}</span
          >
        </div>
        <div class="comment-footer">
          <span class="status" :class="`status-${comment.status}`">{{
            statusName(comment.status)
          }}</span>
          <div class="actions">
            <button
              type="button"
              :disabled="checkingId === comment.id"
              data-action="ai-check"
              @click="checkOne(comment)"
            >
              <Icon name="sparkle" :size="14" />{{
                checkingId === comment.id ? '审核中…' : 'AI 审核'
              }}</button
            ><button v-if="comment.status !== 1" type="button" @click="changeStatus(comment, 1)">
              展示</button
            ><button v-if="comment.status !== 0" type="button" @click="changeStatus(comment, 0)">
              隐藏</button
            ><button v-if="comment.status !== 2" type="button" @click="changeStatus(comment, 2)">
              标记垃圾</button
            ><button class="danger" type="button" @click="remove(comment)">
              <Icon name="trash" :size="14" />删除
            </button>
          </div>
        </div>
      </article>
      <p v-if="!loading && !comments.length" class="empty">没有找到评论。</p>
      <p v-if="loading" class="empty">正在加载评论…</p>
    </section>
    <div class="pagination">
      <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">
        <Icon name="chevron-left" :size="15" />上一页</button
      ><span>{{ page }} / {{ pages }}</span
      ><button type="button" :disabled="page >= pages" @click="changePage(page + 1)">
        下一页<Icon name="chevron-right" :size="15" />
      </button>
    </div>
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicInteractionApi, type GuestComment } from '@/api/public-interactions'
import { llmApi } from '@/api/llm'
import { formatDate } from '@/utils'
import { toast } from '@/utils/toast'
import { useConfirm } from '@/composables/useConfirm'
import Icon from '@/components/common/Icon.vue'
import type {
  ModerationCategory,
  ModerationCheckVO,
  ModerationScanVO,
  ModerationVerdict
} from '@/types/llm'

const comments = ref<GuestComment[]>([])
const loading = ref(true)
const error = ref('')
const statusFilter = ref('')
const page = ref(1)
const pages = ref(1)
const { confirm, ConfirmDialog } = useConfirm()

/**
 * AI 审核结果只存在本地（后端不落库，也**不会**自动改评论状态）：
 * 审核给出建议，改不改、改哪条仍由作者点既有按钮决定。
 */
const verdicts = ref<Record<number, ModerationCheckVO>>({})
const checkingId = ref<number | null>(null)
const scanning = ref(false)
const scanNotice = ref('')

const VERDICT_LABELS: Record<ModerationVerdict, string> = {
  pass: '建议通过',
  flag: '建议复核',
  block: '建议拦截'
}
const CATEGORY_LABELS: Record<ModerationCategory, string> = {
  ad: '广告',
  abuse: '辱骂',
  spam: '刷屏',
  offtopic: '跑题',
  meaningless: '无意义'
}

function verdictName(verdict: ModerationVerdict | undefined): string {
  return verdict ? VERDICT_LABELS[verdict] : ''
}

function categoryName(category: ModerationCategory): string {
  return CATEGORY_LABELS[category] || category
}

function verdictFor(commentId: number): ModerationCheckVO | undefined {
  return verdicts.value[commentId]
}

/** 单条审核：结果只挂在对应评论上，不写回 status。 */
async function checkOne(comment: GuestComment) {
  if (checkingId.value !== null) return
  checkingId.value = comment.id
  scanNotice.value = ''
  try {
    const response = await llmApi.moderationCheck({ commentId: comment.id, kind: 'guest' })
    const data = response.data
    if (data) verdicts.value = { ...verdicts.value, [comment.id]: data }
  } catch (err) {
    const detail = err as Error & {
      userMessage?: string
      response?: { data?: { message?: string } }
    }
    toast.error(detail.userMessage || detail.response?.data?.message || 'AI 审核失败，请稍后重试')
  } finally {
    checkingId.value = null
  }
}

/** 批量扫描：只返回被判 flag/block 的评论，逐条挂到列表上展示。 */
async function runScan() {
  if (scanning.value) return
  scanning.value = true
  scanNotice.value = ''
  try {
    const response = await llmApi.moderationScan({ limit: 20 })
    const data: ModerationScanVO | undefined = response.data
    if (!data) return
    const next = { ...verdicts.value }
    for (const item of data.items) {
      next[item.commentId] = {
        verdict: item.verdict,
        reason: item.reason,
        categories: item.categories,
        model: '',
        latencyMs: 0
      }
    }
    verdicts.value = next
    scanNotice.value = `已扫描 ${data.scanned} 条评论，其中 ${data.flagged} 条需要人工确认。`
  } catch (err) {
    const detail = err as Error & {
      userMessage?: string
      response?: { data?: { message?: string } }
    }
    scanNotice.value =
      detail.userMessage || detail.response?.data?.message || '批量扫描失败，请稍后重试'
  } finally {
    scanning.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await publicInteractionApi.getAdminComments({
      pageNum: page.value,
      pageSize: 20,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
    })
    comments.value = result.data?.records || []
    pages.value = Math.max(1, result.data?.pages || 1)
  } catch {
    error.value = '评论列表暂时无法加载。'
  } finally {
    loading.value = false
  }
}

function statusName(status: number) {
  return status === 1 ? '展示中' : status === 2 ? '垃圾评论' : '已隐藏'
}
function changePage(next: number) {
  if (next < 1 || next > pages.value) return
  page.value = next
  void load()
}
async function changeStatus(comment: GuestComment, status: number) {
  try {
    await publicInteractionApi.updateCommentStatus(comment.id, status)
    comment.status = status
    toast.success('评论状态已更新')
  } catch {
    toast.error('更新失败，请重试')
  }
}
async function remove(comment: GuestComment) {
  if (!(await confirm(`确定删除来自「${comment.nickname}」的这条评论吗？`, '删除评论'))) return
  try {
    await publicInteractionApi.deleteComment(comment.id)
    comments.value = comments.value.filter((item) => item.id !== comment.id)
    // 审核结果跟着评论走，评论删掉后不再留着悬空徽章。
    if (verdicts.value[comment.id]) {
      const remaining = { ...verdicts.value }
      delete remaining[comment.id]
      verdicts.value = remaining
    }
    toast.success('评论已删除')
    // 本地过滤不会更新服务端页数：删空当前页时退回上一页并重新拉取。
    if (comments.value.length === 0 && page.value > 1) {
      page.value -= 1
    }
    await load()
  } catch {
    toast.error('删除失败，请重试')
  }
}

onMounted(() => void load())
</script>

<style scoped>
.comments-page {
  display: grid;
  gap: 16px;
  color: var(--text-primary);
}
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}
.heading-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}
.scan-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  padding-inline: 13px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.scan-button:hover:not(:disabled) {
  color: var(--accent);
}
.scan-button:disabled {
  opacity: 0.55;
}
.scan-notice {
  margin: 0;
  padding: 11px 13px;
  border-radius: var(--radius);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.page-heading h1 {
  margin: 0 0 8px;
  font-size: var(--text-2xl);
}
.page-heading p:not(.eyebrow) {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
select {
  min-height: 44px;
  padding-inline: 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: var(--text-sm);
}
.comment-list {
  display: grid;
  gap: 12px;
}
.comment-card {
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}
.comment-top,
.comment-footer,
.comment-top > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.comment-top > div {
  justify-content: flex-start;
  flex-wrap: wrap;
}
.comment-top span,
.comment-top time {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.comment-top a {
  color: var(--accent);
}
.comment-content {
  margin: 15px 0;
  font-size: var(--text-sm);
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.comment-footer {
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
.status {
  padding: 3px 8px;
  border-radius: var(--radius-xs);
  font-size: var(--text-2xs);
}
.status-1 {
  background: var(--success-light);
  color: var(--success);
}
.status-0 {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.status-2 {
  background: var(--error-light);
  color: var(--error);
}
/* AI 审核建议：只是提示层，不替代上面的评论状态徽章 */
.verdict-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
}
.verdict-badge {
  padding: 3px 8px;
  border-radius: var(--radius-xs);
  font-size: var(--text-2xs);
  font-weight: 600;
}
.verdict-badge[data-verdict='pass'] {
  background: var(--success-light);
  color: var(--success);
}
.verdict-badge[data-verdict='flag'] {
  background: var(--warning-light);
  color: var(--warning);
}
.verdict-badge[data-verdict='block'] {
  background: var(--error-light);
  color: var(--error);
}
.verdict-reason {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.verdict-category {
  padding: 2px 7px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: end;
  gap: 4px;
}
.actions button,
.pagination button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding-inline: 10px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.actions button:hover {
  background: var(--bg-secondary);
  color: var(--accent);
}
.actions .danger:hover {
  color: var(--error);
}
.empty,
.error {
  padding: 30px;
  color: var(--text-muted);
  text-align: center;
  font-size: var(--text-sm);
}
.error {
  color: var(--error);
}
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.pagination button {
  min-height: 44px;
}
.pagination button:disabled {
  opacity: 0.45;
}
@media (max-width: 580px) {
  .page-heading {
    align-items: start;
    flex-direction: column;
  }
  .heading-actions {
    flex-wrap: wrap;
    width: 100%;
  }
  .comment-top,
  .comment-footer {
    align-items: start;
    flex-direction: column;
  }
  .actions {
    justify-content: start;
  }
}
</style>
