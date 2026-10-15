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
            <rect width="56" height="56" rx="16" fill="url(#logo-grad)" />
            <path
              d="M16 20h24M16 28h18M16 36h12"
              stroke="white"
              stroke-width="2.5"
              stroke-linecap="round"
            />
            <circle cx="40" cy="36" r="4" fill="white" opacity="0.9" />
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="56" y2="56">
                <!-- 跟随主题主色，避免与导航栏蓝色 Logo 不一致（原先硬编码为青色 #0D9488） -->
                <stop stop-color="var(--accent)" />
                <stop offset="1" stop-color="var(--accent-dark)" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">{{ siteStore.settings.siteTitle }}</span>
          <span class="brand-tagline">作者管理入口</span>
        </div>
      </div>

      <!-- Header -->
      <div class="auth-header">
        <h1>欢迎回来</h1>
        <p>仅作者管理员可以登录后台</p>
      </div>

      <!-- Form -->
      <form class="auth-form" @submit.prevent="handleLogin" novalidate>
        <div class="form-field">
          <label class="form-label" for="login-account">用户名 / 邮箱</label>
          <div class="input-wrap">
            <input
              id="login-account"
              v-model="form.account"
              type="text"
              class="form-input"
              placeholder="请输入用户名或邮箱"
              autocomplete="username"
              required
            />
          </div>
        </div>

        <div class="form-field">
          <label class="form-label" for="login-password">密码</label>
          <div class="input-wrap">
            <input
              id="login-password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input form-input--password"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
              minlength="8"
            />
            <button
              type="button"
              class="input-toggle"
              @click="showPassword = !showPassword"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              tabindex="-1"
            >
              <svg
                v-if="!showPassword"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg
                v-else
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
                />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
        </div>

        <div class="form-row">
          <label class="check-label">
            <input type="checkbox" v-model="form.remember" class="check-input" />
            <span class="check-box">
              <svg
                width="10"
                height="10"
                viewBox="0 0 12 12"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="2 6 5 9 10 3" />
              </svg>
            </span>
            <span class="check-text">记住我</span>
          </label>
          <span class="link-subtle">请联系站点所有者重置登录凭据</span>
        </div>

        <button type="submit" class="btn-primary" :disabled="loading">
          <transition name="fade-swap" mode="out-in">
            <span v-if="loading" key="spinner" class="btn-loading">
              <span class="spinner"></span>
              <span>登录中...</span>
            </span>
            <span v-else key="text">登录</span>
          </transition>
        </button>
      </form>

      <div class="auth-footer">
        <p>访客无需登录即可阅读和搜索文章。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useSiteStore } from '../../stores/site'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import type { LoginRequest } from '../../types/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const siteStore = useSiteStore()
const logger = useLogger('Login')

const loading = ref(false)
const showPassword = ref(false)
const mounted = ref(false)

const form = reactive({
  account: '',
  password: '',
  remember: false
})

onMounted(() => {
  requestAnimationFrame(() => {
    mounted.value = true
  })
})

const isEmail = (str: string) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(str)
}

const handleLogin = async () => {
  if (!form.account || !form.password) {
    toast.error('请输入用户名/邮箱和密码')
    return
  }

  const accountTrimmed = form.account.trim()
  const accountIsEmail = isEmail(accountTrimmed)

  if (!accountIsEmail && (accountTrimmed.length < 3 || accountTrimmed.length > 20)) {
    toast.error('用户名长度应为3-20个字符')
    return
  }

  if (form.password.length < 8) {
    toast.error('密码长度至少8位')
    return
  }

  loading.value = true

  try {
    const loginData: LoginRequest = {
      username: '',
      password: form.password,
      remember: form.remember
    }

    if (accountIsEmail) {
      loginData.email = accountTrimmed
    } else {
      loginData.username = accountTrimmed
    }

    const result = await userStore.login(loginData)

    if (result.success) {
      logger.info('Login successful')
      const redirect = (route.query.redirect as string) || `${siteStore.adminRoot}/dashboard`
      const safeRedirect = redirect.startsWith(siteStore.adminRoot) && !redirect.startsWith('//')
        ? redirect
        : `${siteStore.adminRoot}/dashboard`
      router.push(safeRedirect)
    } else {
      logger.warn('Login failed', { message: result.message })
      toast.error(result.message || '登录失败')
    }
  } catch (error: any) {
    logger.error('Login error', { error: error.message })
    toast.error('登录失败，请稍后重试')
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

/* Subtle grid pattern for depth */
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

/* ===== Card — Faux-OS Window ===== */
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

/* ===== Window Chrome Bar ===== */
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

.chrome-dot--red {
  background: #ff5f57;
}
.chrome-dot--yellow {
  background: #febc2e;
}
.chrome-dot--green {
  background: #28c840;
}

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

/* ===== Row: checkbox + link ===== */
.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.check-label {
  display: flex;
  align-items: center;
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
}

.link-subtle {
  font-size: var(--text-sm);
  color: var(--text-muted);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-default);
}

.link-subtle:hover {
  color: var(--primary);
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
  background: var(--contrast-bg);
  color: var(--contrast-fg);
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  transition:
    background var(--duration-fast) var(--ease-default),
    transform var(--duration-instant) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default);
}

.btn-primary:hover:not(:disabled) {
  background: var(--contrast-bg-hover);
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
  to {
    transform: rotate(360deg);
  }
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

/* ===== Footer Switch ===== */
.auth-footer {
  text-align: center;
}

.auth-footer p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
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

/* ===== Transition ===== */
.fade-swap-enter-active,
.fade-swap-leave-active {
  transition:
    opacity 0.12s ease,
    transform 0.12s ease;
}

.fade-swap-enter-from {
  opacity: 0;
  transform: scale(0.92);
}

.fade-swap-leave-to {
  opacity: 0;
  transform: scale(0.92);
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
    display: flex;
    flex-direction: column;
    justify-content: center;
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

  .fade-swap-enter-active,
  .fade-swap-leave-active {
    transition: none;
  }

  .spinner {
    animation: none;
    border-color: rgba(255, 255, 255, 0.5);
  }
}
</style>
