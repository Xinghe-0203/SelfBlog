<template>
  <div class="auth-page">
    <div class="auth-bg-orb orb-1"></div>
    <div class="auth-bg-orb orb-2"></div>
    <div class="auth-bg-orb orb-3"></div>
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <div class="auth-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
          </div>
          <h1>找回密码</h1>
          <p>通过邮箱重置密码</p>
        </div>
        
        <form class="auth-form" @submit.prevent="handleSubmit">
          <template v-if="step === 1">
            <div class="step-indicator">
              <div class="step active">1</div>
              <div class="step-line"></div>
              <div class="step">2</div>
              <div class="step-line"></div>
              <div class="step">3</div>
            </div>
            
            <div class="form-group">
              <label class="form-label">注册邮箱</label>
              <input 
                v-model="form.email" 
                type="email" 
                class="form-input"
                placeholder="请输入注册时使用的邮箱"
                required
              />
            </div>
            
            <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
              <span v-if="loading" class="loading-spinner"></span>
              <span v-else>发送验证码</span>
            </button>
          </template>
          
          <template v-if="step === 2">
            <div class="step-indicator">
              <div class="step completed">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </div>
              <div class="step-line active"></div>
              <div class="step active">2</div>
              <div class="step-line"></div>
              <div class="step">3</div>
            </div>
            
            <div class="form-group">
              <label class="form-label">验证码</label>
              <div class="code-input-group">
                <input 
                  v-model="form.code" 
                  type="text" 
                  class="form-input"
                  placeholder="请输入邮箱收到的验证码"
                  required
                />
                <button 
                  type="button" 
                  class="btn btn-secondary btn-code"
                  @click="resendCode"
                  :disabled="sendingCode || codeSent"
                >
                  <span v-if="sendingCode" class="btn-spinner"></span>
                  <span v-else>{{ codeSent ? `${countdown}s` : '重新发送' }}</span>
                </button>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">新密码</label>
              <input 
                v-model="form.newPassword" 
                type="password" 
                class="form-input"
                placeholder="请输入新密码（至少8位）"
                required
                minlength="8"
                @input="updateStrength"
              />
              <div v-if="form.newPassword" class="strength-meter">
                <div class="strength-bar">
                  <div class="strength-fill" :style="{ width: strengthPercent + '%', background: strengthColor }"></div>
                </div>
                <span class="strength-text">强度：<span :style="{ color: strengthColor, fontWeight: 600 }">{{ strengthText }}</span></span>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">确认新密码</label>
              <input 
                v-model="form.confirmPassword" 
                type="password" 
                class="form-input"
                :class="{ 'input-error': form.confirmPassword && form.confirmPassword !== form.newPassword }"
                placeholder="请再次输入新密码"
                required
              />
              <span v-if="form.confirmPassword && form.confirmPassword !== form.newPassword" class="error-hint">
                两次输入的密码不一致
              </span>
            </div>
            
            <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
              <span v-if="loading" class="loading-spinner"></span>
              <span v-else>重置密码</span>
            </button>
          </template>
          
          <template v-if="step === 3">
            <div class="step-indicator">
              <div class="step completed">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </div>
              <div class="step-line active"></div>
              <div class="step completed">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </div>
              <div class="step-line active"></div>
              <div class="step completed">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </div>
            </div>
            
            <div class="success-message">
              <div class="success-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
              <h3>密码重置成功</h3>
              <p>请使用新密码登录</p>
            </div>
            
            <router-link to="/login" class="btn btn-primary btn-block">
              立即登录
            </router-link>
          </template>
        </form>
        
        <div class="auth-footer">
          <p>想起密码了？<router-link to="/login">返回登录</router-link></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { userApi } from '../../api/user'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { checkPasswordStrength } from '../../utils'

const logger = useLogger('PasswordReset')

const step = ref(1)
const loading = ref(false)
const sendingCode = ref(false)
const codeSent = ref(false)
const countdown = ref(60)
let countdownTimer: ReturnType<typeof setInterval> | undefined = undefined
const strengthResult = ref({ level: 'weak', text: '弱', color: '#F44336' })

const strengthPercent = computed(() => {
  const map: Record<string, number> = { weak: 33, medium: 66, strong: 100 }
  return map[strengthResult.value.level] || 0
})
const strengthColor = computed(() => strengthResult.value.color)
const strengthText = computed(() => strengthResult.value.text)

function updateStrength() {
  strengthResult.value = checkPasswordStrength(form.newPassword)
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const resendCode = async () => {
  const emailTrimmed = form.email.trim()
  if (!emailTrimmed) {
    toast.error('邮箱为空')
    return
  }
  if (countdownTimer) clearInterval(countdownTimer)
  sendingCode.value = true
  try {
    await userApi.sendCode(emailTrimmed)
    codeSent.value = true
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        codeSent.value = false
      }
    }, 1000)
    toast.success('验证码已重新发送')
  } catch (error) {
    toast.error('发送验证码失败')
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async () => {
  if (step.value === 1) {
    const emailTrimmed = form.email.trim()
    if (!emailTrimmed) {
      toast.error('请输入邮箱')
      return
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailTrimmed)) {
      toast.error('请输入有效的邮箱地址')
      return
    }

    loading.value = true
    try {
      await userApi.sendCode(emailTrimmed)
      logger.info('Verification code sent', { email: emailTrimmed })
      toast.success('验证码已发送至邮箱')
      codeSent.value = true
      countdown.value = 60
      countdownTimer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(countdownTimer)
          codeSent.value = false
        }
      }, 1000)
      step.value = 2
    } catch (error: any) {
      logger.error('Failed to send code', { error: error.message })
      toast.error(error.response?.data?.message || '发送验证码失败')
    } finally {
      loading.value = false
    }
  } else if (step.value === 2) {
    const codeTrimmed = form.code.trim()
    const newPasswordTrimmed = form.newPassword.trim()
    const confirmPasswordTrimmed = form.confirmPassword.trim()

    if (!codeTrimmed) {
      toast.error('请输入验证码')
      return
    }

    if (!newPasswordTrimmed) {
      toast.error('请输入新密码')
      return
    }

    if (newPasswordTrimmed.length < 8) {
      toast.error('密码长度至少8位')
      return
    }

    if (newPasswordTrimmed !== confirmPasswordTrimmed) {
      toast.error('两次输入的密码不一致')
      return
    }

    loading.value = true
    try {
      await userApi.resetPassword({
        email: form.email.trim(),
        code: codeTrimmed,
        newPassword: newPasswordTrimmed
      })
      logger.info('Password reset successful')
      toast.success('密码重置成功')
      step.value = 3
    } catch (error: any) {
      logger.error('Failed to reset password', { error: error.message })
      toast.error(error.response?.data?.message || '重置密码失败')
    } finally {
      loading.value = false
    }
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
  background: var(--background-gradient);
  position: relative;
  overflow: hidden;
}

.auth-bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: orb-float 20s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, var(--info), var(--primary-start));
  top: -10%;
  left: 20%;
  animation-delay: 0s;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, var(--primary-end), var(--accent));
  bottom: -5%;
  right: -5%;
  animation-delay: -7s;
}

.orb-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, var(--success), var(--info));
  top: 60%;
  left: -10%;
  animation-delay: -14s;
}

@keyframes orb-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.1); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(20px, 10px) scale(1.05); }
}

.auth-container {
  width: 100%;
  max-width: 500px;
  position: relative;
  z-index: 1;
}

.auth-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow-wet);
  padding: var(--spacing-2xl);
  transition: all var(--transition-slow);
  position: relative;
  overflow: hidden;
}

.auth-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
}

.auth-card:hover {
  box-shadow: var(--shadow-lg), var(--glass-shadow-wet);
}

.auth-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.auth-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto var(--spacing-md);
  background: linear-gradient(135deg, var(--info), var(--primary-start));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.4);
}

.auth-header h1 {
  font-size: 1.875rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
  letter-spacing: -0.02em;
}

.auth-header p {
  font-size: 0.9375rem;
  color: var(--text-secondary);
}

.auth-form {
  margin-bottom: var(--spacing-lg);
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-xl);
  gap: 0;
}

.step {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 600;
  background: var(--glass-bg);
  border: 2px solid var(--glass-border);
  color: var(--text-muted);
  transition: all var(--transition);
}

.step.active {
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  border-color: var(--primary);
  color: white;
  box-shadow: 0 4px 12px var(--primary-glow);
}

.step.completed {
  background: linear-gradient(135deg, var(--success), #059669);
  border-color: var(--success);
  color: white;
  box-shadow: 0 4px 12px var(--success-glow);
}

.step-line {
  width: 60px;
  height: 3px;
  background: var(--glass-border);
  border-radius: var(--radius-full);
  margin: 0 8px;
  transition: all var(--transition);
}

.step-line.active {
  background: linear-gradient(90deg, var(--success), var(--primary-start));
}

.form-group {
  margin-bottom: var(--spacing-md);
}

.form-label {
  display: block;
  margin-bottom: var(--spacing-xs);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 0.9375rem;
  font-family: inherit;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  color: var(--text-primary);
  transition: all var(--transition);
  box-shadow: var(--glass-shadow);
}

.form-input:hover:not(:disabled):not(:focus) {
  border-color: var(--text-muted);
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light), var(--glass-shadow);
  background: var(--glass-hover);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.form-input.input-error {
  border-color: var(--error);
}

.form-input.input-error:focus {
  box-shadow: 0 0 0 3px var(--error-light), var(--glass-shadow);
}

.strength-meter {
  margin-top: var(--spacing-sm);
}

.strength-bar {
  height: 6px;
  background: var(--border);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: 4px;
}

.strength-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1), background 0.4s ease;
}

.strength-text {
  display: block;
  font-size: 0.75rem;
  color: var(--text-muted);
}

.error-hint {
  display: block;
  font-size: 0.75rem;
  color: var(--error);
  margin-top: var(--spacing-xs);
}

.code-input-group {
  display: flex;
  gap: var(--spacing-sm);
}

.code-input-group .form-input {
  flex: 1;
}

.btn-code {
  white-space: nowrap;
  flex-shrink: 0;
  padding: 0.75rem 1rem;
  font-size: 0.8125rem;
  min-width: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--primary);
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.btn-block {
  width: 100%;
  padding: 0.875rem;
  font-size: 1rem;
  font-weight: 600;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--info), var(--primary-start));
  color: var(--text-inverse);
  border: none;
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

.btn-block:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md), 0 0 20px rgba(59, 130, 246, 0.4);
}

.btn-block:active:not(:disabled) {
  transform: translateY(0);
}

.btn-block:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.success-message {
  text-align: center;
  padding: var(--spacing-xl) 0;
}

.success-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--spacing-md);
  background: linear-gradient(135deg, var(--success), #059669);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px var(--success-glow);
  animation: success-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes success-pop {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.success-message h3 {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-sm);
}

.success-message p {
  font-size: 0.9375rem;
  color: var(--text-secondary);
}

.auth-footer {
  text-align: center;
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--glass-border);
}

.auth-footer p {
  font-size: 0.9375rem;
  color: var(--text-secondary);
}

.auth-footer a {
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  transition: all var(--transition-fast);
}

.auth-footer a:hover {
  color: var(--primary-hover);
  text-decoration: underline;
}

@media (max-width: 640px) {
  .auth-page {
    padding: var(--spacing-md);
  }
  
  .auth-card {
    padding: var(--spacing-xl);
    border-radius: var(--radius-lg);
  }
  
  .auth-header h1 {
    font-size: 1.5rem;
  }
  
  .auth-icon {
    width: 60px;
    height: 60px;
  }
  
  .auth-icon svg {
    width: 32px;
    height: 32px;
  }
  
  .step-line {
    width: 40px;
  }
  
  .code-input-group {
    flex-direction: column;
  }
  
  .btn-code {
    width: 100%;
  }
  
  .orb-1, .orb-2, .orb-3 {
    filter: blur(60px);
    opacity: 0.3;
  }
  
  .orb-1 { width: 250px; height: 250px; }
  .orb-2 { width: 200px; height: 200px; }
  .orb-3 { width: 180px; height: 180px; }
}
</style>
