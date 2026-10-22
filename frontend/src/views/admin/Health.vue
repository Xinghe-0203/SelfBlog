<template>
  <div class="health-page">
    <header class="page-heading">
      <div>
        <h1>内容体检</h1>
        <p>让模型通读最近发布的文章，给出站点内容质量评分与可执行的改进建议。</p>
      </div>
      <div class="heading-actions">
        <label class="limit-field"
          >抽样篇数
          <select v-model.number="limit" :disabled="running" aria-label="抽样篇数">
            <option :value="20">20 篇</option>
            <option :value="40">40 篇</option>
            <option :value="60">60 篇</option>
            <option :value="80">80 篇</option>
          </select>
        </label>
        <button
          class="primary-button"
          type="button"
          :disabled="running"
          data-action="run-health-report"
          @click="runReport"
        >
          <Icon name="sparkle" :size="16" />{{ running ? '体检中…' : '开始体检' }}
        </button>
      </div>
    </header>

    <p v-if="error" class="alert" role="alert">{{ error }}</p>
    <p v-if="notice" class="success" role="status">{{ notice }}</p>

    <div v-if="!report && !running && !error" class="empty" role="status">
      还没有体检报告。点击「开始体检」，模型会抽样最近的已发布文章给出评分与建议。
    </div>
    <div v-if="running && !report" class="empty" role="status">正在通读文章，请稍候…</div>

    <template v-if="report">
      <section class="score-card">
        <div class="score-number" :data-score-level="scoreLevel" :data-health-score="report.score">
          <strong>{{ report.score }}</strong>
          <small>内容质量分</small>
        </div>
        <div class="score-body">
          <p class="score-summary">{{ report.summary }}</p>
          <ul class="score-meta">
            <li>抽样 {{ report.sampledPosts }} 篇 / 共 {{ report.totalPosts }} 篇已发布</li>
            <li>生成于 {{ formatTime(report.generatedAt) }}</li>
            <li>{{ report.model }} · {{ report.latencyMs }}ms</li>
          </ul>
        </div>
      </section>

      <section class="findings">
        <header class="section-heading">
          <div>
            <h2>发现的问题</h2>
            <p>共 {{ report.findings.length }} 条，按严重程度从高到低排列。</p>
          </div>
        </header>
        <div class="finding-list">
          <article
            v-for="(finding, index) in report.findings"
            :key="`${finding.title}-${index}`"
            class="finding-card"
            :data-severity="finding.severity"
            :data-finding-index="index"
          >
            <div class="finding-head">
              <span class="severity-badge" :data-severity="finding.severity">
                {{ severityName(finding.severity) }}
              </span>
              <h3>{{ finding.title }}</h3>
              <span v-if="finding.count !== null" class="finding-count"
                >{{ finding.count }} 处</span
              >
            </div>
            <p class="finding-detail">{{ finding.detail }}</p>
            <p class="finding-action">建议：{{ finding.action }}</p>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { llmApi } from '@/api/llm'
import type { HealthReportVO, ProofreadSeverity } from '@/types/llm'
import { formatDate } from '@/utils'
import Icon from '@/components/common/Icon.vue'

/**
 * 内容体检（F 块）：只读。后端会抽样最近发布的文章并返回评分 + findings，
 * 前端不做任何写操作，也不自动修改文章。
 */
const SEVERITY_LABELS: Record<ProofreadSeverity, string> = {
  low: '轻微',
  medium: '中等',
  high: '重要'
}

const report = ref<HealthReportVO | null>(null)
const running = ref(false)
const error = ref('')
const notice = ref('')
const limit = ref(40)

const scoreLevel = computed(() => {
  const score = report.value?.score ?? 0
  if (score >= 80) return 'good'
  if (score >= 60) return 'fair'
  return 'poor'
})

function severityName(severity: ProofreadSeverity): string {
  return SEVERITY_LABELS[severity]
}

function formatTime(value: string): string {
  if (!value) return '未知时间'
  return formatDate(value.replace('T', ' '))
}

async function runReport() {
  running.value = true
  error.value = ''
  notice.value = ''
  try {
    const response = await llmApi.healthReport({ limit: limit.value })
    report.value = response.data || null
    notice.value = report.value ? '体检完成，报告已更新。' : ''
  } catch (err) {
    // 400（未配置模型 / 没有已发布文章）与 502（模型返回不合规）都带可读 message。
    const detail = err as Error & {
      userMessage?: string
      response?: { data?: { message?: string } }
    }
    error.value = detail.userMessage || detail.response?.data?.message || '体检失败，请稍后重试。'
  } finally {
    running.value = false
  }
}
</script>

<style scoped>
.health-page {
  display: grid;
  gap: 18px;
  color: var(--text-primary);
}
.page-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}
.page-heading h1 {
  margin: 0 0 8px;
  font-size: var(--text-2xl);
}
.page-heading p {
  margin: 0;
  max-width: 62ch;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.heading-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: end;
  gap: 10px;
}
.limit-field {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.limit-field select {
  min-height: 44px;
  padding-inline: 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: var(--text-sm);
}
.primary-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 44px;
  padding-inline: 14px;
  border: 1px solid transparent;
  border-radius: var(--radius);
  background: var(--contrast-bg);
  color: var(--contrast-fg);
  font-size: var(--text-sm);
  cursor: pointer;
}
.primary-button:hover:not(:disabled) {
  background: var(--contrast-bg-hover);
}
.primary-button:disabled {
  cursor: progress;
  opacity: 0.65;
}
.alert,
.success {
  margin: 0;
  padding: 12px 13px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
}
.alert {
  border: 1px solid var(--error);
  background: var(--error-light);
  color: var(--error);
}
.success {
  border: 1px solid var(--success);
  background: var(--success-light);
  color: var(--success);
}
.empty {
  padding: 34px;
  border: 1px dashed var(--border-light);
  border-radius: var(--radius-lg);
  color: var(--text-muted);
  text-align: center;
  font-size: var(--text-sm);
}
.score-card {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
  padding: 22px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.score-number {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  align-content: center;
  gap: 2px;
  min-width: 132px;
  min-height: 132px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--bg-secondary);
}
.score-number strong {
  font-size: var(--text-5xl);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.score-number small {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.score-number[data-score-level='good'] strong {
  color: var(--success);
}
.score-number[data-score-level='fair'] strong {
  color: var(--warning);
}
.score-number[data-score-level='poor'] strong {
  color: var(--error);
}
.score-body {
  display: grid;
  flex: 1 1 260px;
  align-content: start;
  gap: 10px;
}
.score-summary {
  margin: 0;
  font-size: var(--text-sm);
  line-height: 1.75;
}
.score-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin: 0;
  padding: 0;
  color: var(--text-muted);
  font-size: var(--text-2xs);
  list-style: none;
}
.findings {
  display: grid;
  gap: 12px;
}
.section-heading h2 {
  margin: 0 0 4px;
  font-size: var(--text-base);
}
.section-heading p {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.finding-list {
  display: grid;
  gap: 10px;
}
.finding-card {
  padding: 15px 16px;
  border: 1px solid var(--border-light);
  border-left: 3px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}
.finding-card[data-severity='low'] {
  border-left-color: var(--text-muted);
}
.finding-card[data-severity='medium'] {
  border-left-color: var(--warning);
}
.finding-card[data-severity='high'] {
  border-left-color: var(--error);
}
.finding-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 7px;
}
.finding-head h3 {
  flex: 1 1 auto;
  margin: 0;
  font-size: var(--text-sm);
}
.severity-badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: var(--text-2xs);
  font-weight: 600;
}
.severity-badge[data-severity='low'] {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.severity-badge[data-severity='medium'] {
  background: var(--warning-light);
  color: var(--warning);
}
.severity-badge[data-severity='high'] {
  background: var(--error-light);
  color: var(--error);
}
.finding-count {
  color: var(--text-muted);
  font-size: var(--text-2xs);
  font-variant-numeric: tabular-nums;
}
.finding-detail {
  margin: 0 0 6px;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.7;
}
.finding-action {
  margin: 0;
  color: var(--accent);
  font-size: var(--text-xs);
  line-height: 1.7;
}
@media (max-width: 760px) {
  .page-heading {
    align-items: start;
    flex-direction: column;
  }
  .heading-actions {
    flex-wrap: wrap;
    width: 100%;
  }
  .score-card {
    gap: 16px;
  }
  .score-number {
    min-width: 108px;
    min-height: 108px;
  }
  .score-number strong {
    font-size: var(--text-4xl);
  }
}
</style>
