<template>
  <div class="auth-page">
    <div class="auth-card" :class="{ 'card-enter': mounted }">
      <!-- Window chrome: title bar -->
      <div class="chrome-bar" aria-hidden="true">
        <span class="chrome-dot chrome-dot--red"></span>
        <span class="chrome-dot chrome-dot--yellow"></span>
        <span class="chrome-dot chrome-dot--green"></span>
      </div>

      <!-- Brand -->
      <div class="auth-brand">
        <div class="brand-icon">
          <svg width="32" height="32" viewBox="0 0 56 56" fill="none">
            <rect width="56" height="56" rx="16" fill="url(#logo-grad-r)" />
            <path d="M16 20h24M16 28h18M16 36h12" stroke="white" stroke-width="2.5" stroke-linecap="round" />
            <circle cx="40" cy="36" r="4" fill="white" opacity="0.9" />
            <defs>
              <linearGradient id="logo-grad-r" x1="0" y1="0" x2="56" y2="56">
                <stop stop-color="#0D9488" />
                <stop offset="1" stop-color="#14B8A6" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">SelfBlog</span>
          <span class="brand-tagline">SelfBlog</span>
        </div>
      </div>

      <!-- Header -->
      <div class="auth-header">
        <h1>创建账号</h1>
        <p>注册成为SelfBlog的一员</p>
      </div>

      <!-- Form -->
      <form class="auth-form" @submit.prevent="handleRegister" novalidate>
        <!-- Username -->
        <div class="form-field">
          <label class="form-label" for="reg-username">用户名</label>
          <div class="input-wrap">
            <input
              id="reg-username"
              v-model="form.username"
              type="text"
              class="form-input"
              placeholder="6位以上小写英文字母、数字"
              autocomplete="username"
              required
              pattern="[a-z0-9]{6,}"
              title="用户名需为6位以上的小写英文字母和数字"
            />
          </div>
        </div>

        <!-- Email -->
        <div class="form-field">
          <label class="form-label" for="reg-email">邮箱</label>
          <div class="input-wrap">
            <input
              id="reg-email"
              v-model="form.email"
              type="email"
              class="form-input"
              placeholder="请输入邮箱"
              autocomplete="email"
              required
            />
          </div>
        </div>

        <!-- Password -->
        <div class="form-field">
          <label class="form-label" for="reg-password">密码</label>
          <div class="input-wrap">
            <input
              id="reg-password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input form-input--password"
              placeholder="请输入密码（至少8位）"
              autocomplete="new-password"
              required
              minlength="8"
              @input="updateStrength"
            />
            <button
              type="button"
              class="input-toggle"
              @click="showPassword = !showPassword"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              tabindex="-1"
            >
              <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
          <!-- Strength meter -->
          <transition name="meter-slide">
            <div v-if="form.password" class="strength-meter">
              <div class="strength-track">
                <div class="strength-fill" :style="{ width: strengthPercent + '%', background: strengthColor }"></div>
              </div>
              <span class="strength-label" :style="{ color: strengthColor }">{{ strengthText }}</span>
            </div>
          </transition>
        </div>

        <!-- Confirm password -->
        <div class="form-field">
          <label class="form-label" for="reg-confirm">确认密码</label>
          <div class="input-wrap">
            <input
              id="reg-confirm"
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="form-input form-input--password"
              placeholder="请再次输入密码"
              autocomplete="new-password"
              required
            />
            <button
              type="button"
              class="input-toggle"
              @click="showConfirmPassword = !showConfirmPassword"
              :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
              tabindex="-1"
            >
              <svg v-if="!showConfirmPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
          <!-- Error -->
          <transition name="error-slide">
            <span v-if="form.confirmPassword && form.confirmPassword !== form.password" class="field-error">
              两次输入的密码不一致
            </span>
          </transition>
        </div>

        <!-- Verification code -->
        <div class="form-field">
          <label class="form-label" for="reg-code">邮箱验证码</label>
          <div class="code-group">
            <div class="input-wrap">
              <input
                id="reg-code"
                v-model="form.code"
                type="text"
                class="form-input"
                placeholder="请输入验证码"
                required
                maxlength="6"
                autocomplete="one-time-code"
              />
            </div>
            <button
              type="button"
              class="btn-code"
              @click="sendCode"
              :disabled="sendingCode || codeSent"
            >
              <transition name="fade-swap" mode="out-in">
                <span v-if="sendingCode" key="sending" class="code-spinner"></span>
                <span v-else key="label">{{ codeSent ? `${countdown}s` : '获取验证码' }}</span>
              </transition>
            </button>
          </div>
        </div>

        <!-- Nickname (optional) -->
        <div class="form-field">
          <label class="form-label" for="reg-nickname">
            昵称 <span class="label-optional">（选填）</span>
          </label>
          <div class="input-wrap">
            <input
              id="reg-nickname"
              v-model="form.nickname"
              type="text"
              class="form-input"
              placeholder="给自己取个昵称吧"
              autocomplete="nickname"
            />
          </div>
        </div>

        <!-- Agreement -->
        <div class="agreement-row">
          <label class="check-label">
            <input type="checkbox" v-model="form.agreed" class="check-input" required />
            <span class="check-box">
              <svg width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="2 6 5 9 10 3" />
              </svg>
            </span>
            <span class="check-text">
              我已阅读并同意
              <a href="/agreement" target="_blank" class="link-primary">用户协议</a>
              和
              <a href="/privacy" target="_blank" class="link-primary">隐私政策</a>
            </span>
          </label>
        </div>

        <!-- Submit -->
        <button type="submit" class="btn-primary" :disabled="loading || !form.agreed">
          <transition name="fade-swap" mode="out-in">
            <span v-if="loading" key="spinner" class="btn-loading">
              <span class="spinner"></span>
              <span>注册中...</span>
            </span>
            <span v-else key="text">创建账号</span>
          </transition>
        </button>
      </form>

      <!-- Divider -->
      <div class="auth-divider">
        <span>或</span>
      </div>

      <!-- Switch -->
      <div class="auth-footer">
        <p>已有账号？<router-link to="/login" class="link-primary">立即登录</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { userApi } from '../../api/user'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { checkPasswordStrength } from '../../utils'

const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('Register')

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

const loading = ref(false)
const sendingCode = ref(false)
const codeSent = ref(false)
const countdown = ref(60)
let countdownTimer: ReturnType<typeof setInterval> | undefined = undefined
const strengthResult = ref({ level: 'weak', text: '弱', color: '#DC2626' })
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const mounted = ref(false)

const strengthPercent = computed(() => {
  const map: Record<string, number> = { weak: 33, medium: 66, strong: 100 }
  return map[strengthResult.value.level] || 0
})
const strengthColor = computed(() => strengthResult.value.color)
const strengthText = computed(() => strengthResult.value.text)

function updateStrength() {
  strengthResult.value = checkPasswordStrength(form.password)
}

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  code: '',
  agreed: false
})

onMounted(() => {
  requestAnimationFrame(() => {
    mounted.value = true
  })
})

const sendCode = async () => {
  const emailTrimmed = form.email.trim()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailTrimmed)) {
    toast.error('请输入有效的邮箱地址')
    return
  }

  sendingCode.value = true
  try {
    const usernameTrimmed = form.username.trim()
    await userApi.sendRegisterCode(emailTrimmed, usernameTrimmed)
    codeSent.value = true
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        codeSent.value = false
      }
    }, 1000)
    toast.success('验证码已发送')
  } catch (error: any) {
    logger.error('Failed to send code', { error: error.message })
    toast.error(error.response?.data?.message || '发送验证码失败')
  } finally {
    sendingCode.value = false
  }
}

const handleRegister = async () => {
  const usernameTrimmed = form.username.trim()
  const emailTrimmed = form.email.trim()
  const passwordTrimmed = form.password.trim()
  const codeTrimmed = form.code.trim()

  if (!usernameTrimmed || !emailTrimmed || !passwordTrimmed) {
    toast.error('请填写所有必填项')
    return
  }

  if (!/^[a-z0-9]{6,20}$/.test(usernameTrimmed)) {
    toast.error('用户名需为6-20位小写英文字母和数字')
    return
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailTrimmed)) {
    toast.error('请输入有效的邮箱地址')
    return
  }

  if (passwordTrimmed.length < 8) {
    toast.error('密码长度至少8位')
    return
  }

  if (passwordTrimmed !== form.confirmPassword) {
    toast.error('两次输入的密码不一致')
    return
  }

  if (!codeTrimmed) {
    toast.error('请输入邮箱验证码')
    return
  }

  if (!form.agreed) {
    toast.error('请阅读并同意用户协议和隐私政策')
    return
  }

  loading.value = true

  try {
    const response = await userApi.verifyRegisterCode({
      username: usernameTrimmed,
      email: emailTrimmed,
      password: passwordTrimmed,
      nickname: form.nickname?.trim() || usernameTrimmed,
      code: codeTrimmed
    })

    if (response.code === 200) {
      logger.info('Registration successful')
      const res = await userStore.login({
        username: usernameTrimmed,
        password: passwordTrimmed
      })
      if (res.success) {
        toast.success('注册成功，欢迎加入！')
        router.push('/')
      } else {
        toast.success('注册成功，请登录')
        router.push('/login')
      }
    } else {
      logger.warn('Registration failed', { message: response.message })
      toast.error(response.message || '注册失败')
    }
  } catch (error: any) {
    logger.error('Registration error', { error: error.message })
    toast.error(error.response?.data?.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== Page ===== */
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
  background: var(--background-gradient);
  position: relative;
}

.auth-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 0, 0, 0.015) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.015) 1px, transparent 1px);
  background-size: 48px 48px;
  pointer-events: none;
  z-index: 0;
}

/* ===== Card ===== */
.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  background: var(--surface-solid);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-xl);
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  padding: 0 var(--spacing-xl) var(--spacing-xl);
  opacity: 0;
  transform: translateY(16px);
  transition:
    opacity 0.5s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.auth-card.card-enter {
  opacity: 1;
  transform: translateY(0);
}

/* ===== Chrome Bar ===== */
.chrome-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: var(--spacing-md) 0;
  margin-bottom: var(--spacing-xs);
}

.chrome-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.chrome-dot--red    { background: #FF5F57; }
.chrome-dot--yellow { background: #FEBC2E; }
.chrome-dot--green  { background: #28C840; }

/* ===== Brand ===== */
.auth-brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xl);
}

.brand-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-icon svg {
  width: 32px;
  height: 32px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.brand-name {
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  letter-spacing: var(--tracking-tight);
  line-height: 1.2;
}

.brand-tagline {
  font-size: 0.6875rem;
  color: var(--text-muted);
  line-height: 1.2;
}

/* ===== Header ===== */
.auth-header {
  margin-bottom: var(--spacing-xl);
}

.auth-header h1 {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: var(--leading-tight);
  margin-bottom: var(--spacing-1);
}

.auth-header p {
  font-size: var(--text-sm);
  color: var(--text-muted);
  margin: 0;
}

/* ===== Form ===== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* ===== Form Field ===== */
.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1_5);
}

.form-label {
  font-size: 0.6875rem;
  font-weight: var(--font-semibold);
  color: var(--text-secondary);
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.label-optional {
  font-weight: var(--font-normal);
  color: var(--text-muted);
  text-transform: none;
  letter-spacing: 0;
}

.input-wrap {
  position: relative;
}

.form-input {
  width: 100%;
  height: 44px;
  padding: 0 var(--spacing-md);
  font-size: var(--text-base);
  font-family: var(--font-sans);
  background: var(--gray-50);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-primary);
  outline: none;
  transition:
    border-color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default);
  box-sizing: border-box;
}

.form-input--password {
  padding-right: 44px;
}

.form-input::placeholder {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.form-input:hover {
  border-color: var(--gray-300);
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
  background: var(--surface-solid);
}

/* Password toggle */
.input-toggle {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition:
    color var(--duration-fast) var(--ease-default),
    background var(--duration-fast) var(--ease-default);
}

.input-toggle:hover {
  color: var(--text-secondary);
  background: var(--gray-100);
}

/* ===== Strength Meter ===== */
.strength-meter {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-1);
}

.strength-track {
  flex: 1;
  height: 3px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1), background 0.35s ease;
}

.strength-label {
  font-size: 0.625rem;
  font-weight: var(--font-medium);
  white-space: nowrap;
  letter-spacing: 0.02em;
}

/* ===== Field Error ===== */
.field-error {
  display: block;
  font-size: 0.6875rem;
  color: var(--error);
  margin-top: var(--spacing-1);
  font-weight: var(--font-medium);
}

/* ===== Code Group ===== */
.code-group {
  display: flex;
  gap: var(--spacing-sm);
}

.code-group .input-wrap {
  flex: 1;
  min-width: 0;
}

.btn-code {
  flex-shrink: 0;
  padding: 0 var(--spacing-md);
  min-width: 110px;
  height: 44px;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  font-family: var(--font-sans);
  background: var(--gray-50);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  color: var(--primary);
  cursor: pointer;
  transition:
    background var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default);
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-code:hover:not(:disabled) {
  background: var(--surface-solid);
  border-color: var(--primary);
}

.btn-code:active:not(:disabled) {
  transform: scale(0.97);
}

.btn-code:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.code-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--primary-light);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.65s linear infinite;
}

/* ===== Agreement ===== */
.agreement-row {
  margin-top: calc(-1 * var(--spacing-1));
}

.check-label {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  cursor: pointer;
  user-select: none;
}

.check-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.check-box {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-xs);
  background: var(--surface-solid);
  color: transparent;
  margin-top: 2px;
  transition:
    background var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default);
}

.check-input:checked + .check-box {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.check-input:focus-visible + .check-box {
  box-shadow: 0 0 0 3px var(--primary-light);
}

.check-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.5;
}

.link-primary {
  color: var(--primary);
  text-decoration: none;
  font-weight: var(--font-semibold);
  transition: color var(--duration-fast) var(--ease-default);
}

.link-primary:hover {
  color: var(--primary-hover);
}

/* ===== Primary Button ===== */
.btn-primary {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: 0 var(--spacing-xl);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  font-family: var(--font-sans);
  background: var(--gray-900);
  color: var(--text-inverse);
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  transition:
    background var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default);
}

.btn-primary:hover:not(:disabled) {
  background: var(--gray-800);
}

.btn-primary:active:not(:disabled) {
  transform: scale(0.98);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-loading {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.65s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== Divider ===== */
.auth-divider {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin: var(--spacing-xl) 0;
}

.auth-divider::before,
.auth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-solid);
}

.auth-divider span {
  font-size: 0.625rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: var(--font-medium);
}

/* ===== Footer ===== */
.auth-footer {
  text-align: center;
}

.auth-footer p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
}

/* ===== Transitions ===== */
.fade-swap-enter-active,
.fade-swap-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.fade-swap-enter-from {
  opacity: 0;
  transform: scale(0.92);
}

.fade-swap-leave-to {
  opacity: 0;
  transform: scale(0.92);
}

.meter-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.meter-slide-leave-active {
  transition: all 0.2s ease;
}

.meter-slide-enter-from {
  opacity: 0;
  transform: translateY(-4px);
  max-height: 0;
}

.meter-slide-leave-to {
  opacity: 0;
  max-height: 0;
}

.error-slide-enter-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.error-slide-leave-active {
  transition: all 0.15s ease;
}

.error-slide-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

.error-slide-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}

/* ===== Mobile ===== */
@media (max-width: 640px) {
  .auth-page {
    padding: 0;
    align-items: stretch;
  }

  .auth-card {
    max-width: none;
    border-radius: 0;
    border: none;
    box-shadow: none;
    min-height: 100vh;
    min-height: 100dvh;
    padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-2xl);
  }

  .chrome-bar {
    justify-content: center;
    padding-top: var(--spacing-xl);
    margin-bottom: var(--spacing-md);
  }

  .auth-brand {
    justify-content: center;
    margin-bottom: var(--spacing-lg);
  }

  .auth-header {
    text-align: center;
    margin-bottom: var(--spacing-lg);
  }

  .form-input {
    height: 48px;
    font-size: 16px; /* Prevent iOS zoom */
  }

  .btn-primary {
    height: 48px;
    font-size: 1rem;
  }

  .input-toggle {
    width: 44px;
    height: 44px;
    min-width: 44px;
  }

  .btn-code {
    height: 48px;
    width: 100%;
    min-width: 0;
  }

  .code-group {
    flex-direction: column;
  }
}

/* ===== Accessibility ===== */
@media (prefers-reduced-motion: reduce) {
  .auth-card {
    opacity: 1;
    transform: none;
    transition: none;
  }

  .btn-primary:active:not(:disabled) {
    transform: none;
  }

  .btn-code:active:not(:disabled) {
    transform: none;
  }

  .fade-swap-enter-active,
  .fade-swap-leave-active,
  .meter-slide-enter-active,
  .meter-slide-leave-active,
  .error-slide-enter-active,
  .error-slide-leave-active {
    transition: none;
  }

  .spinner,
  .code-spinner {
    animation: none;
  }
}
</style>
