<template>
  <div class="password-page">
    <div class="password-icon-wrapper">
      <div class="password-icon-bg">
        <svg
          width="32"
          height="32"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
        >
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
      </div>
    </div>
    <div class="password-card card">
      <div class="password-header">
        <h2>修改密码</h2>
        <router-link to="/" class="btn btn-ghost btn-sm">返回主页</router-link>
      </div>

      <form class="password-form" @submit.prevent="handleSubmit">
        <div class="form-group float-label-group">
          <input
            v-model="form.oldPassword"
            type="password"
            class="form-input"
            placeholder=" "
            required
          />
          <label class="float-label">当前密码</label>
        </div>

        <div class="form-group float-label-group">
          <input
            v-model="form.newPassword"
            type="password"
            class="form-input"
            :class="{ error: showPasswordError }"
            placeholder=" "
            required
            minlength="8"
            @input="updateStrength"
          />
          <label class="float-label">新密码</label>
          <div v-if="form.newPassword" class="strength-bar">
            <div
              class="strength-fill"
              :class="`strength-${strengthLevel}`"
              :style="{ width: strengthPercent + '%' }"
            ></div>
          </div>
          <span class="form-hint"
            >至少8位字符，强度：<span class="strength-text" :class="`strength-${strengthLevel}`">{{
              strengthText
            }}</span></span
          >
          <span v-if="showPasswordError" class="form-error">{{ passwordError }}</span>
        </div>

        <div class="form-group float-label-group">
          <input
            v-model="form.confirmPassword"
            type="password"
            class="form-input"
            :class="{ error: showConfirmError }"
            placeholder=" "
            required
            @input="validateConfirm"
          />
          <label class="float-label">确认新密码</label>
          <span v-if="showConfirmError" class="form-error">{{ confirmError }}</span>
        </div>

        <div class="form-actions">
          <router-link to="/" class="btn btn-ghost">取消</router-link>
          <button type="submit" class="btn btn-primary" :disabled="loading">
            <template v-if="loading">
              <span class="btn-spinner"></span>
              修改中...
            </template>
            <template v-else>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              确认修改
            </template>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { apiErrorMessage } from '../../utils/apiError'
import { checkPasswordStrength, type PasswordStrength } from '../../utils'

const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('PasswordChange')

const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
/** 只持档位：颜色/文案映射分别放在本组件的 CSS 与 computed 里，不落在 JS 常量色上。 */
const strengthResult = ref<PasswordStrength>({ level: 'empty' })
const showPasswordError = ref(false)
const passwordError = ref('')
const showConfirmError = ref(false)
const confirmError = ref('')

const strengthLevel = computed(() => strengthResult.value.level)
const strengthPercent = computed(() => {
  const map: Record<PasswordStrength['level'], number> = {
    empty: 0,
    weak: 33,
    medium: 66,
    strong: 100
  }
  return map[strengthResult.value.level] ?? 0
})
const strengthText = computed(() => {
  const map: Record<PasswordStrength['level'], string> = {
    empty: '无',
    weak: '弱',
    medium: '中',
    strong: '强'
  }
  return map[strengthResult.value.level]
})

function updateStrength() {
  strengthResult.value = checkPasswordStrength(form.newPassword)
  showPasswordError.value = false
}

function validateConfirm() {
  if (form.confirmPassword && form.newPassword !== form.confirmPassword) {
    showConfirmError.value = true
    confirmError.value = '两次输入的密码不一致'
  } else {
    showConfirmError.value = false
    confirmError.value = ''
  }
}

async function handleSubmit() {
  showPasswordError.value = false
  showConfirmError.value = false

  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    toast.warning('请填写完整信息')
    return
  }

  if (form.oldPassword === form.newPassword) {
    showPasswordError.value = true
    passwordError.value = '新密码不能与当前密码相同'
    toast.warning('新密码不能与当前密码相同')
    return
  }

  if (form.newPassword !== form.confirmPassword) {
    showConfirmError.value = true
    confirmError.value = '两次输入的密码不一致'
    toast.warning('两次输入的密码不一致')
    return
  }

  if (form.newPassword.length < 8) {
    showPasswordError.value = true
    passwordError.value = '密码长度至少8位'
    toast.warning('密码长度至少8位')
    return
  }

  if (strengthResult.value.level === 'weak') {
    showPasswordError.value = true
    passwordError.value = '密码强度不足'
    toast.warning('密码强度不足，请包含大小写字母、数字和特殊字符中的至少3种')
    return
  }

  loading.value = true
  try {
    const result = await userStore.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })

    if (result.success) {
      toast.success('密码修改成功，请重新登录')
      await userStore.logout()
      router.push('/login')
    } else {
      toast.error(result.message || '修改失败')
    }
  } catch (error: unknown) {
    logger.error('Failed to change password', {
      error: error instanceof Error ? error.message : String(error)
    })
    toast.error(apiErrorMessage(error, '修改密码失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-page {
  max-width: 560px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.password-icon-wrapper {
  display: flex;
  justify-content: center;
}

.password-icon-bg {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: var(--shadow-md), var(--shadow-glow-primary);
}

.password-card {
  padding: 32px;
  border-radius: var(--radius-xl);
  width: 100%;
}

.password-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.password-header h2 {
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--text-primary);
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.float-label-group {
  position: relative;
  margin-bottom: 20px;
}

.float-label-group .form-input {
  padding: 20px 14px 6px;
  border-radius: var(--radius);
}

.float-label {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: var(--text-sm);
  color: var(--text-muted);
  pointer-events: none;
  transition: all var(--transition);
  background: var(--surface);
  padding: 0 4px;
}

.float-label-group .form-input:focus ~ .float-label,
.float-label-group .form-input:not(:placeholder-shown) ~ .float-label {
  top: 6px;
  transform: none;
  font-size: var(--text-2xs);
  color: var(--primary);
}

.float-label-group .form-input.error ~ .float-label {
  color: var(--error);
}

.form-hint {
  display: block;
  font-size: var(--text-2xs);
  color: var(--text-muted);
  margin-top: 4px;
}

.strength-bar {
  height: 4px;
  background: var(--border);
  border-radius: 2px;
  margin-top: 6px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition:
    width 0.3s ease,
    background 0.3s ease;
}

/* 强度档位 → 语义 token：进度条是实心块用色板色，文字用墨色（浅深两主题都过 AA）。 */
.strength-fill.strength-empty {
  background: var(--border);
}
.strength-fill.strength-weak {
  background: var(--error);
}
.strength-fill.strength-medium {
  background: var(--warning);
}
.strength-fill.strength-strong {
  background: var(--success);
}

.strength-text {
  font-weight: 600;
}
.strength-text.strength-empty {
  color: var(--text-muted);
}
.strength-text.strength-weak {
  color: var(--error-ink);
}
.strength-text.strength-medium {
  color: var(--warning-ink);
}
.strength-text.strength-strong {
  color: var(--success-ink);
}

.form-error {
  display: block;
  font-size: var(--text-2xs);
  color: var(--error);
  margin-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--glass-border);
}

.btn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: var(--radius-full);
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .password-page {
    padding: 16px;
  }

  .password-card {
    padding: 20px;
  }
}
</style>
