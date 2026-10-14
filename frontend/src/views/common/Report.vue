<template>
  <div class="report-page">
    <div class="report-container">
      <div class="report-card glass">
        <div class="card-header">
          <div class="header-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
          </div>
          <h2 class="card-title">举报内容</h2>
          <p class="card-subtitle">您的反馈将帮助我们维护良好的社区环境</p>
        </div>

        <form class="report-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label">举报原因</label>
            <div class="reason-grid">
              <label
                v-for="reason in reasons"
                :key="reason.value"
                class="reason-option"
                :class="{ selected: form.reason === reason.value }"
              >
                <input type="radio" v-model="form.reason" :value="reason.value" class="reason-radio" />
                <span class="reason-icon">{{ reason.icon }}</span>
                <span class="reason-text">{{ reason.label }}</span>
              </label>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">详细说明 <span class="optional">（可选）</span></label>
            <textarea
              v-model="form.description"
              class="form-input"
              placeholder="请详细描述举报原因，以便我们更好地处理..."
              rows="4"
              maxlength="500"
            ></textarea>
            <span class="char-count">{{ form.description.length }}/500</span>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="goBack">取消</button>
            <button type="submit" class="btn btn-primary" :class="{ loading: loading }" :disabled="loading || !form.reason">
              <svg v-if="!loading" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
              {{ loading ? '提交中...' : '提交举报' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { reportApi } from '../../api/report'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'

const route = useRoute()
const router = useRouter()
const logger = useLogger('Report')

const loading = ref(false)
const form = reactive({
  reason: '',
  description: ''
})

const reasons = [
  { value: 'spam', label: '垃圾广告', icon: '📢' },
  { value: 'harassment', label: '骚扰或霸凌', icon: '😠' },
  { value: 'hate', label: '仇恨言论', icon: '💢' },
  { value: 'violence', label: '暴力内容', icon: '⚠️' },
  { value: 'adult', label: '成人内容', icon: '🔞' },
  { value: 'copyright', label: '版权侵犯', icon: '©️' },
  { value: 'other', label: '其他原因', icon: '❓' }
]

const handleSubmit = async () => {
  if (!form.reason) return

  const typeStr = String(route.params.type || '')
  const validTypes = ['post', 'comment', 'user']
  if (!validTypes.includes(typeStr)) {
    toast.error('无效的举报类型')
    return
  }
  const targetId = parseInt(String(route.params.id || ''))
  if (!targetId || targetId <= 0) {
    toast.error('无效的举报目标')
    return
  }

  loading.value = true
  try {
    await reportApi.createReport({
      targetType: typeStr as any,
      targetId,
      reason: form.reason,
      description: form.description
    })

    toast.success('举报已提交，感谢您的反馈')
    goBack()
  } catch (err: any) {
    logger.error('Failed to submit report', { error: err.message })
    toast.error('提交失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.report-page {
  max-width: 600px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

.report-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  box-shadow: var(--glass-shadow-wet);
  position: relative;
}

.report-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  pointer-events: none;
  z-index: 1;
}

.card-header {
  text-align: center;
  padding: var(--spacing-xl) var(--spacing-xl) var(--spacing-lg);
  border-bottom: 1px solid var(--glass-border);
}

.header-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--spacing-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--error-light);
  border-radius: var(--radius-md);
  color: var(--error);
}

.card-title {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0 0 var(--spacing-xs);
}

.card-subtitle {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin: 0;
}

.report-form {
  padding: var(--spacing-lg) var(--spacing-xl) var(--spacing-xl);
}

.form-group {
  margin-bottom: var(--spacing-lg);
}

.form-label {
  display: block;
  margin-bottom: var(--spacing-sm);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.optional {
  font-weight: 400;
  color: var(--text-muted);
}

.reason-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-sm);
}

.reason-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all var(--transition);
}

.reason-option:hover {
  border-color: var(--primary);
  background: var(--primary-light);
}

.reason-option.selected {
  border-color: var(--primary);
  background: var(--primary-light);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.reason-radio {
  display: none;
}

.reason-icon {
  font-size: 1.125rem;
  flex-shrink: 0;
}

.reason-text {
  font-size: 0.8125rem;
  color: var(--text-primary);
  font-weight: 500;
}

textarea.form-input {
  min-height: 100px;
  resize: vertical;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--glass-border);
}

@media (max-width: 480px) {
  .report-page { padding: var(--spacing-md); }
  .card-header { padding: var(--spacing-lg); }
  .report-form { padding: var(--spacing-md); }
  .reason-grid { grid-template-columns: 1fr; }
}
</style>
