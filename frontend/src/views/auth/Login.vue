<template>
  <div class="auth-page">
    <div class="auth-card">
      <header class="auth-brand">
        <span class="brand-mark" aria-hidden="true">
          <img v-if="siteStore.settings.siteLogoUrl" class="brand-mark--image" :src="siteStore.settings.siteLogoUrl" alt="" />
          <Icon v-else name="logo" :size="20" />
        </span>
        <span class="brand-text">
          <span class="brand-name">{{ siteStore.settings.siteTitle }}</span>
          <span class="brand-tagline">作者管理入口</span>
        </span>
      </header>

      <div class="auth-header">
        <h1>登录后台</h1>
        <p>仅作者管理员可以登录；访客无需登录即可阅读文章。</p>
      </div>

      <form class="auth-form" novalidate @submit.prevent="handleLogin">
        <div class="form-field">
          <label class="form-label" for="login-account">用户名 / 邮箱</label>
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
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              :aria-pressed="showPassword"
              @click="showPassword = !showPassword"
            >
              <Icon :name="showPassword ? 'eye-off' : 'eye'" :size="17" />
            </button>
          </div>
        </div>

        <div class="form-row">
          <label class="check-label">
            <input v-model="form.remember" type="checkbox" class="check-input" />
            <span class="check-box" aria-hidden="true"><Icon name="check" :size="12" /></span>
            <span class="check-text">记住我</span>
          </label>
          <span class="link-subtle">忘记密码请联系站点作者</span>
        </div>

        <button type="submit" class="btn-primary" :disabled="loading" :aria-busy="loading">
          <span v-if="loading" class="btn-loading"><span class="spinner" aria-hidden="true"></span>登录中…</span>
          <span v-else>登录</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useSiteStore } from '../../stores/site'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import Icon from '@/components/common/Icon.vue'
import type { LoginRequest } from '../../types/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const siteStore = useSiteStore()
const logger = useLogger('Login')

const loading = ref(false)
const showPassword = ref(false)

const form = reactive({
  account: '',
  password: '',
  remember: false
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
  } catch (error) {
    logger.error('Login error', { error: (error as Error).message })
    toast.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100dvh;
  padding: 24px;
  background: var(--bg-primary);
}

.auth-card {
  width: 100%;
  max-width: 380px;
  padding: 28px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-2xl);
  background: var(--surface-primary);
  box-shadow: var(--shadow-sm);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.brand-mark {
  display: grid;
  flex: 0 0 34px;
  place-items: center;
  width: 34px;
  height: 34px;
  overflow: hidden;
  border-radius: var(--radius);
  background: var(--accent-light);
  color: var(--accent);
}

.brand-mark--image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: transparent;
}

.brand-text {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.brand-name {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-tagline {
  color: var(--text-muted);
  font-size: 11px;
}

.auth-header {
  margin-block: 24px 22px;
}

.auth-header h1 {
  margin-bottom: 8px;
  font-size: 22px;
}

.auth-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.auth-form {
  display: grid;
  gap: 18px;
}

.form-field {
  display: grid;
  gap: 7px;
}

.form-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.input-wrap {
  position: relative;
}

.form-input {
  width: 100%;
  height: 44px;
  padding-inline: 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: 16px;
}

.form-input--password {
  padding-right: 48px;
}

.form-input::placeholder {
  color: var(--text-muted);
  font-size: 14px;
}

.form-input:hover {
  border-color: var(--text-muted);
}

.form-input:focus-visible {
  border-color: var(--accent);
  outline: 2px solid var(--accent-light);
  outline-offset: 1px;
}

.input-toggle {
  position: absolute;
  right: 2px;
  top: 50%;
  display: grid;
  place-items: center;
  width: 42px;
  height: 40px;
  transform: translateY(-50%);
  border-radius: var(--radius);
  color: var(--text-muted);
}

.input-toggle:hover {
  color: var(--text-primary);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.check-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.check-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.check-box {
  display: grid;
  place-items: center;
  width: 16px;
  height: 16px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-xs);
  background: var(--surface-primary);
  color: transparent;
}

.check-input:checked + .check-box {
  border-color: var(--accent);
  background: var(--accent);
  color: var(--text-inverse);
}

.check-input:focus-visible + .check-box {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.check-text {
  color: var(--text-secondary);
  font-size: 13px;
}

.link-subtle {
  color: var(--text-muted);
  font-size: 12px;
}

.btn-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 46px;
  border-radius: var(--radius);
  background: var(--contrast-bg);
  color: var(--contrast-fg);
  font-size: 15px;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  background: var(--contrast-bg-hover);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-loading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 15px;
  height: 15px;
  border: 2px solid color-mix(in srgb, currentColor 30%, transparent);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.65s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 480px) {
  .auth-page {
    padding: 16px;
  }

  .auth-card {
    padding: 22px;
  }
}
</style>
