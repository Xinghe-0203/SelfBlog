<template>
  <div class="seo-page">
    <header class="page-heading">
      <div>
        <h1>SEO 管理</h1>
        <p>为文章维护独立的标题、描述、关键词与分享图，并检查影响收录的常见问题。</p>
      </div>
      <div class="heading-actions">
        <button class="ghost-button" type="button" :disabled="loading" @click="reload">
          <Icon name="refresh" :size="16" />{{ loading ? '加载中…' : '刷新' }}
        </button>
        <button
          class="primary-button"
          type="button"
          :disabled="batchRunning || !llmConfigured"
          :title="llmConfigured ? '' : '请先在网站设置中配置模型接口'"
          @click="runBatch"
        >
          <Icon name="sparkle" :size="16" />{{ batchRunning ? '生成中…' : '批量生成' }}
        </button>
      </div>
    </header>

    <p v-if="error" class="alert" role="alert">{{ error }}</p>
    <p v-if="notice" class="success" role="status">{{ notice }}</p>

    <section v-if="overview && !overview.llmConfigured" class="notice" role="status">
      <Icon name="info" :size="18" />
      <p>
        尚未配置可用的模型接口，自动生成与连通性测试暂不可用。请到
        <RouterLink :to="`${siteStore.adminRoot}/settings`">网站设置</RouterLink>
        填写 API 地址与密钥。
      </p>
    </section>

    <div v-if="overview" class="metrics">
      <article>
        <small>SEO 覆盖率</small><strong>{{ overview.coveragePercent }}%</strong>
      </article>
      <article>
        <small>已生成</small>
        <strong
          >{{ overview.withSeo }}<em>/{{ overview.totalPosts }}</em></strong
        >
      </article>
      <article>
        <small>缺少描述</small><strong>{{ overview.missingDescription }}</strong>
      </article>
      <article>
        <small>标题过长</small><strong>{{ overview.longTitle }}</strong>
      </article>
      <article>
        <small>标题重复</small><strong>{{ overview.duplicateTitle }}</strong>
      </article>
      <article>
        <small>缺少封面</small><strong>{{ overview.missingCover }}</strong>
      </article>
    </div>

    <section v-if="batchResult" class="panel batch-panel">
      <div class="section-heading">
        <div>
          <h2>上次批量生成</h2>
          <p>
            请求 {{ batchResult.requested }} 篇 · 成功 {{ batchResult.succeeded }} · 失败
            {{ batchResult.failed }} · 跳过 {{ batchResult.skipped }}
          </p>
        </div>
        <button class="ghost-button" type="button" @click="batchResult = null">收起</button>
      </div>
      <ul v-if="batchResult.failures.length" class="failure-list">
        <li v-for="failure in batchResult.failures" :key="failure.postId">
          <code>#{{ failure.postId }}</code>
          <span>{{ failure.message }}</span>
        </li>
      </ul>
    </section>

    <section class="panel">
      <div class="section-heading">
        <div>
          <h2>文章 SEO</h2>
          <p>点「编辑」维护单篇元数据，点「生成」让模型基于标题与摘要起草。</p>
        </div>
      </div>

      <div class="filters">
        <label class="filter-field">
          <span>搜索标题</span>
          <input
            v-model.trim="keyword"
            type="search"
            maxlength="200"
            placeholder="输入文章标题关键词"
            data-filter="keyword"
            @keyup.enter="applyFilters"
          />
        </label>
        <label class="filter-field">
          <span>体检问题</span>
          <select v-model="issue" data-filter="issue" @change="applyFilters">
            <option value="">全部</option>
            <option v-for="option in issueOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <button class="ghost-button" type="button" @click="applyFilters">筛选</button>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>文章</th>
              <th>状态</th>
              <th>体检</th>
              <th>来源</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.postId">
              <td class="title-cell">
                <strong>{{ row.title }}</strong>
                <small v-if="row.seoTitle" class="seo-title">{{ row.seoTitle }}</small>
              </td>
              <td>
                <span class="status-badge" :data-status="row.status">{{
                  row.status === 1 ? '已发布' : '草稿'
                }}</span>
              </td>
              <td>
                <span v-if="!row.issues.length" class="clean-badge">无问题</span>
                <span
                  v-for="code in row.issues"
                  :key="code"
                  class="issue-chip"
                  :data-issue="code"
                  >{{ issueLabel(code) }}</span
                >
              </td>
              <td>
                <span v-if="row.source" class="source-badge" :data-source="row.source">
                  {{ row.source === 'llm' ? '模型生成' : '手动编辑' }}
                </span>
                <span v-else class="muted">未生成</span>
              </td>
              <td class="muted">
                {{ row.updateTime ? formatDate(row.updateTime, 'YYYY-MM-DD HH:mm') : '—' }}
              </td>
              <td class="row-actions">
                <button type="button" class="link-button" @click="openEditor(row.postId)">
                  编辑
                </button>
                <button
                  type="button"
                  class="link-button"
                  :disabled="generatingId === row.postId || !llmConfigured"
                  @click="generateOne(row.postId)"
                >
                  {{ generatingId === row.postId ? '生成中…' : '生成' }}
                </button>
                <button
                  v-if="row.hasSeo"
                  type="button"
                  class="link-button danger"
                  @click="removeOne(row.postId)"
                >
                  删除
                </button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="6" class="empty">没有匹配的文章</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">
          <Icon name="chevron-left" :size="15" />上一页
        </button>
        <span>第 {{ page }} 页 / 共 {{ pages }} 页</span>
        <button type="button" :disabled="page >= pages" @click="changePage(page + 1)">
          下一页<Icon name="chevron-right" :size="15" />
        </button>
      </div>
    </section>

    <Modal :show="editorOpen" title="编辑 SEO 元数据" width="720px" closable @close="closeEditor">
      <div v-if="detail" class="editor">
        <div class="editor-context">
          <p class="context-label">文章标题</p>
          <p class="context-value">{{ detail.title }}</p>
          <template v-if="detail.postSummary">
            <p class="context-label">文章摘要</p>
            <p class="context-value summary">{{ detail.postSummary }}</p>
          </template>
        </div>

        <label class="field">
          <span>SEO 标题</span>
          <input
            v-model.trim="form.seoTitle"
            type="text"
            maxlength="200"
            placeholder="留空则使用文章标题"
            data-editor="seoTitle"
          />
          <small>建议 50–60 个字符，前台最终显示为：{{ detail.effectiveTitle }}</small>
        </label>

        <label class="field">
          <span>SEO 描述</span>
          <textarea
            v-model.trim="form.seoDescription"
            rows="3"
            maxlength="400"
            placeholder="留空则由文章摘要自动截取"
            data-editor="seoDescription"
          ></textarea>
          <small>建议 120–160 个字符，前台最终显示为：{{ detail.effectiveDescription }}</small>
        </label>

        <label class="field">
          <span>关键词</span>
          <input
            v-model.trim="form.seoKeywords"
            type="text"
            maxlength="300"
            placeholder="用英文逗号分隔，例如：Spring Boot,Vue,博客"
            data-editor="seoKeywords"
          />
          <small>3–8 个词效果最好，会写入 meta keywords。</small>
        </label>

        <label class="field">
          <span>分享图地址</span>
          <input
            v-model.trim="form.ogImageUrl"
            type="text"
            maxlength="500"
            placeholder="留空则依次使用文章封面、站点 Logo"
            data-editor="ogImageUrl"
          />
          <small>需为 http(s) 链接或站内 / 开头的路径，用于 og:image 与 Twitter 卡片。</small>
        </label>

        <p v-if="editorError" class="alert" role="alert">{{ editorError }}</p>
      </div>

      <template #footer>
        <button
          v-if="detail?.hasSeo"
          class="ghost-button danger"
          type="button"
          :disabled="editorBusy"
          @click="removeOne(detail.postId)"
        >
          删除记录
        </button>
        <button
          class="ghost-button"
          type="button"
          :disabled="editorBusy || !llmConfigured"
          @click="generateInEditor"
        >
          <Icon name="sparkle" :size="16" />{{ editorBusy ? '处理中…' : '用模型生成' }}
        </button>
        <button class="primary-button" type="button" :disabled="editorBusy" @click="saveEditor">
          {{ editorBusy ? '保存中…' : '保存' }}
        </button>
      </template>
    </Modal>

    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { formatDate } from '@/utils'
import { seoApi } from '@/api/seo'
import { useConfirm } from '@/composables/useConfirm'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { useSiteStore } from '@/stores/site'
import type {
  SeoBatchResult,
  SeoDetail,
  SeoIssue,
  SeoOverview,
  SeoPostRow,
  SeoUpdateRequest
} from '@/types/seo'
import Icon from '@/components/common/Icon.vue'
import Modal from '@/components/common/Modal.vue'

const ISSUE_LABELS: Record<SeoIssue, string> = {
  missingDescription: '缺少描述',
  longTitle: '标题过长',
  duplicateTitle: '标题重复',
  missingCover: '缺少封面',
  noSeo: '未生成'
}

const issueOptions: Array<{ value: SeoIssue; label: string }> = (
  Object.keys(ISSUE_LABELS) as SeoIssue[]
).map((value) => ({ value, label: ISSUE_LABELS[value] }))

const siteStore = useSiteStore()
const router = useRouter()
const { confirm, ConfirmDialog } = useConfirm()
const beginListRequest = useLatestRequest().begin

const overview = ref<SeoOverview | null>(null)
const rows = ref<SeoPostRow[]>([])
const page = ref(1)
const pages = ref(1)
const pageSize = 10
const keyword = ref('')
const issue = ref<SeoIssue | ''>('')
/** 已生效的筛选值；输入框里的值只有点「筛选」或回车才生效，避免每敲一个字发一次请求。 */
const appliedKeyword = ref('')
const appliedIssue = ref<SeoIssue | ''>('')

const loading = ref(false)
const error = ref('')
const notice = ref('')
const batchRunning = ref(false)
const batchResult = ref<SeoBatchResult | null>(null)
const generatingId = ref<number | null>(null)

const editorOpen = ref(false)
const editorBusy = ref(false)
const editorError = ref('')
const detail = ref<SeoDetail | null>(null)
const form = reactive<SeoUpdateRequest>({
  seoTitle: null,
  seoDescription: null,
  seoKeywords: null,
  ogImageUrl: null
})

const llmConfigured = computed(() => overview.value?.llmConfigured === true)

function issueLabel(code: SeoIssue): string {
  return ISSUE_LABELS[code] || code
}

function userMessage(err: unknown, fallback: string): string {
  return (err as Error & { userMessage?: string }).userMessage || fallback
}

async function loadOverview() {
  try {
    const response = await seoApi.getOverview()
    overview.value = response.data || null
  } catch (err) {
    error.value = userMessage(err, 'SEO 概览暂时无法加载。')
  }
}

async function loadPosts() {
  const isLatest = beginListRequest()
  loading.value = true
  error.value = ''
  try {
    const response = await seoApi.getPosts({
      pageNum: page.value,
      pageSize,
      keyword: appliedKeyword.value,
      issue: appliedIssue.value
    })
    if (!isLatest()) return
    rows.value = response.data?.records || []
    pages.value = Math.max(1, response.data?.pages || 1)
  } catch (err) {
    if (!isLatest()) return
    error.value = userMessage(err, '文章列表暂时无法加载。')
  } finally {
    if (isLatest()) loading.value = false
  }
}

/** 概览与列表一起刷新：生成/删除都会同时影响两处数字。 */
async function reload() {
  await Promise.all([loadOverview(), loadPosts()])
}

function applyFilters() {
  appliedKeyword.value = keyword.value
  appliedIssue.value = issue.value
  page.value = 1
  void loadPosts()
}

function changePage(next: number) {
  if (next < 1 || next > pages.value) return
  page.value = next
  void loadPosts()
}

async function generateOne(postId: number) {
  generatingId.value = postId
  error.value = ''
  notice.value = ''
  try {
    await seoApi.generate(postId)
    notice.value = '已生成并保存该篇的 SEO 元数据。'
    await reload()
  } catch (err) {
    error.value = userMessage(err, '生成失败，请稍后重试。')
  } finally {
    generatingId.value = null
  }
}

async function runBatch() {
  batchRunning.value = true
  error.value = ''
  notice.value = ''
  try {
    const response = await seoApi.batchGenerate({ limit: 10, onlyMissing: true })
    batchResult.value = response.data || null
    if (batchResult.value) {
      notice.value = `批量生成完成：成功 ${batchResult.value.succeeded} 篇，失败 ${batchResult.value.failed} 篇。`
    }
    await reload()
  } catch (err) {
    error.value = userMessage(err, '批量生成失败，请稍后重试。')
  } finally {
    batchRunning.value = false
  }
}

async function removeOne(postId: number) {
  const ok = await confirm('删除后该篇将回落到「由标题与摘要派生」的默认元数据。', '删除 SEO 记录')
  if (!ok) return
  try {
    await seoApi.remove(postId)
    notice.value = '已删除该篇的 SEO 记录。'
    if (detail.value?.postId === postId) closeEditor()
    await reload()
  } catch (err) {
    error.value = userMessage(err, '删除失败，请稍后重试。')
  }
}

async function openEditor(postId: number) {
  editorOpen.value = true
  editorBusy.value = true
  editorError.value = ''
  detail.value = null
  try {
    const response = await seoApi.getDetail(postId)
    detail.value = response.data || null
    form.seoTitle = detail.value?.seoTitle ?? null
    form.seoDescription = detail.value?.seoDescription ?? null
    form.seoKeywords = detail.value?.seoKeywords ?? null
    form.ogImageUrl = detail.value?.ogImageUrl ?? null
  } catch (err) {
    editorError.value = userMessage(err, 'SEO 详情暂时无法加载。')
  } finally {
    editorBusy.value = false
  }
}

function closeEditor() {
  editorOpen.value = false
  detail.value = null
  editorError.value = ''
}

async function generateInEditor() {
  if (!detail.value) return
  editorBusy.value = true
  editorError.value = ''
  try {
    const response = await seoApi.generate(detail.value.postId)
    applyDetail(response.data)
    notice.value = '已用模型生成该篇的 SEO 元数据。'
  } catch (err) {
    editorError.value = userMessage(err, '生成失败，请稍后重试。')
  } finally {
    editorBusy.value = false
  }
}

async function saveEditor() {
  if (!detail.value) return
  editorBusy.value = true
  editorError.value = ''
  try {
    const response = await seoApi.update(detail.value.postId, {
      seoTitle: form.seoTitle || null,
      seoDescription: form.seoDescription || null,
      seoKeywords: form.seoKeywords || null,
      ogImageUrl: form.ogImageUrl || null
    })
    applyDetail(response.data)
    notice.value = 'SEO 元数据已保存。'
    closeEditor()
    await reload()
  } catch (err) {
    editorError.value = userMessage(err, '保存失败，请检查填写内容后重试。')
  } finally {
    editorBusy.value = false
  }
}

function applyDetail(next: SeoDetail | undefined | null) {
  if (!next) return
  detail.value = next
  form.seoTitle = next.seoTitle ?? null
  form.seoDescription = next.seoDescription ?? null
  form.seoKeywords = next.seoKeywords ?? null
  form.ogImageUrl = next.ogImageUrl ?? null
}

onMounted(() => {
  // 从文章管理跳过来时可能带了 postId，直接打开编辑面板省一次点击。
  const queryPostId = Number(router.currentRoute.value.query.postId)
  void reload().then(() => {
    if (Number.isFinite(queryPostId) && queryPostId > 0) void openEditor(queryPostId)
  })
})
</script>

<style scoped>
.seo-page {
  display: grid;
  gap: 18px;
  color: var(--text-primary);
}
.page-heading,
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.page-heading h1 {
  margin: 0 0 8px;
  font-size: var(--text-2xl);
}
.page-heading p:not(.eyebrow),
.section-heading p {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.heading-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}
.ghost-button,
.primary-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  padding-inline: 13px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  font-size: var(--text-sm);
  cursor: pointer;
}
.ghost-button {
  background: var(--surface-primary);
  color: var(--text-primary);
}
.ghost-button:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.primary-button {
  border-color: transparent;
  background: var(--contrast-bg);
  color: var(--contrast-fg);
}
.primary-button:hover:not(:disabled) {
  background: var(--contrast-bg-hover);
}
.ghost-button:disabled,
.primary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.ghost-button.danger {
  color: var(--error);
}
.ghost-button.danger:hover:not(:disabled) {
  border-color: var(--error);
}
.notice,
.alert,
.success {
  margin: 0;
  padding: 12px 13px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
  line-height: 1.6;
}
.notice {
  display: flex;
  align-items: start;
  gap: 9px;
  border: 1px solid var(--border-light);
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.notice p {
  margin: 0;
}
.notice a {
  color: var(--accent);
}
.alert {
  background: var(--error-light);
  color: var(--error);
}
.success {
  background: var(--success-light);
  color: var(--success);
}
.metrics {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}
.metrics article,
.panel {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}
.metrics article {
  display: grid;
  gap: 8px;
  padding: 16px;
}
.metrics small {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.metrics strong {
  font-size: var(--text-2xl);
  font-variant-numeric: tabular-nums;
}
.metrics em {
  color: var(--text-muted);
  font-size: var(--text-sm);
  font-style: normal;
}
.panel {
  min-width: 0;
  padding: 20px;
}
.section-heading {
  margin-bottom: 16px;
}
.section-heading h2 {
  margin: 0 0 5px;
  font-size: var(--text-base);
}
.batch-panel .failure-list {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.batch-panel .failure-list li {
  display: flex;
  gap: 10px;
  color: var(--error);
  font-size: var(--text-sm);
}
.batch-panel .failure-list code {
  flex: 0 0 auto;
  color: var(--text-muted);
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 12px;
  margin-bottom: 16px;
}
.filter-field {
  display: grid;
  flex: 1 1 200px;
  gap: 6px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.filter-field input,
.filter-field select {
  width: 100%;
  min-height: 40px;
  padding: 8px 11px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--text-sm);
}
th,
td {
  padding: 11px 12px;
  border-bottom: 1px solid var(--border-ultra-light);
  text-align: left;
  vertical-align: top;
}
th {
  color: var(--text-muted);
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
}
tbody tr:last-child td {
  border-bottom: 0;
}
.title-cell {
  min-width: 220px;
}
.title-cell strong {
  display: block;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.seo-title {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.status-badge,
.source-badge,
.clean-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: var(--text-2xs);
  font-weight: 600;
  white-space: nowrap;
}
.status-badge[data-status='1'] {
  background: var(--success-light);
  color: var(--success);
}
.status-badge[data-status='0'] {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}
.source-badge[data-source='llm'] {
  background: var(--accent-light);
  color: var(--accent);
}
.source-badge[data-source='manual'] {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.clean-badge {
  background: var(--success-light);
  color: var(--success);
}
.issue-chip {
  display: inline-block;
  margin: 0 4px 4px 0;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--warning-light);
  color: var(--warning);
  font-size: var(--text-2xs);
  font-weight: 600;
  white-space: nowrap;
}
.issue-chip[data-issue='noSeo'] {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}
.muted {
  color: var(--text-muted);
}
.row-actions {
  white-space: nowrap;
}
.link-button {
  padding: 4px 6px;
  border: 0;
  background: none;
  color: var(--accent);
  font: inherit;
  font-size: var(--text-sm);
  cursor: pointer;
}
.link-button:hover:not(:disabled) {
  text-decoration: underline;
}
.link-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.link-button.danger {
  color: var(--error);
}
.empty {
  padding: 24px 12px;
  color: var(--text-muted);
  text-align: center;
}
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 16px;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.pagination button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 40px;
  padding-inline: 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: var(--text-sm);
  cursor: pointer;
}
.pagination button:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.editor {
  display: grid;
  gap: 16px;
}
.editor-context {
  padding: 12px 13px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-secondary);
}
.context-label {
  margin: 0 0 4px;
  color: var(--text-muted);
  font-size: var(--text-2xs);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
.context-value {
  margin: 0 0 10px;
  font-size: var(--text-sm);
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.context-value.summary {
  margin-bottom: 0;
  color: var(--text-secondary);
}
.field {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.field input,
.field textarea {
  width: 100%;
  padding: 9px 11px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
  resize: vertical;
}
.field small {
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
  overflow-wrap: anywhere;
}
@media (max-width: 1100px) {
  .metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 760px) {
  .page-heading {
    flex-direction: column;
    align-items: stretch;
  }
  .heading-actions {
    justify-content: flex-end;
  }
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .filters {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
