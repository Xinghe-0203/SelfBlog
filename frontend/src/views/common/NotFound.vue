<template>
  <div class="not-found-page">
    <div class="not-found-container">
      <div class="not-found-content glass">
        <div class="error-visual">
          <div class="floating-shapes">
            <div class="shape shape-1"></div>
            <div class="shape shape-2"></div>
            <div class="shape shape-3"></div>
          </div>
          <h1 class="error-code">
            <span class="digit digit-1">4</span>
            <span class="digit digit-0">0</span>
            <span class="digit digit-2">4</span>
          </h1>
        </div>
        <h2 class="error-title">页面不存在</h2>
        <p class="error-text">抱歉，您访问的页面不存在或已被移除</p>
        <div class="search-suggestion">
          <div class="search-box">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索你想找的内容..."
              @keyup.enter="doSearch"
            />
            <button class="search-btn" @click="doSearch">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            </button>
          </div>
        </div>
        <div class="error-actions">
          <router-link to="/" class="btn btn-primary btn-lg">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            返回首页
          </router-link>
          <button class="btn btn-secondary btn-lg" @click="goBack">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
            返回上一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchQuery = ref('')

const goBack = () => {
  router.go(-1)
}

const doSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
  }
}
</script>

<style scoped>
.not-found-page {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
}

.not-found-container {
  width: 100%;
  max-width: 640px;
}

.not-found-content {
  text-align: center;
  padding: var(--spacing-3xl) var(--spacing-xl);
  border-radius: var(--radius-2xl);
  position: relative;
  overflow: hidden;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border-wet);
  box-shadow: var(--glass-shadow-wet);
}

.not-found-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.25), transparent);
  pointer-events: none;
  z-index: 1;
}

.error-visual {
  position: relative;
  margin-bottom: var(--spacing-xl);
}

.floating-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: var(--radius-full);
  opacity: 0.15;
}

.shape-1 {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  top: -20px;
  left: 10%;
  animation: float1 6s ease-in-out infinite;
}

.shape-2 {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  top: 10px;
  right: 15%;
  animation: float2 5s ease-in-out infinite;
}

.shape-3 {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--success), #059669);
  bottom: 0;
  left: 25%;
  animation: float3 7s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(15px, -20px) rotate(180deg); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-10px, 15px) rotate(-180deg); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -10px) scale(1.2); }
}

.error-code {
  font-size: 7rem;
  font-weight: 900;
  line-height: 1;
  margin: 0;
  display: flex;
  justify-content: center;
  gap: 4px;
}

.digit {
  display: inline-block;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: pulse 3s ease-in-out infinite;
}

.digit-1 { animation-delay: 0s; }
.digit-0 { animation-delay: 0.5s; }
.digit-2 { animation-delay: 1s; }

@keyframes pulse {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.error-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: var(--spacing-md) 0 var(--spacing-xs);
}

.error-text {
  font-size: 0.9375rem;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-lg);
}

.search-suggestion {
  margin-bottom: var(--spacing-xl);
}

.search-box {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  max-width: 360px;
  margin: 0 auto;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  padding: 4px;
  box-shadow: var(--glass-shadow);
  transition: all var(--transition);
}

.search-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light), var(--glass-shadow);
}

.search-box input {
  flex: 1;
  padding: 10px 12px;
  border: none;
  background: transparent;
  font-size: 0.875rem;
  color: var(--text-primary);
  outline: none;
}

.search-box input::placeholder {
  color: var(--text-muted);
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  border: none;
  border-radius: var(--radius-sm);
  color: white;
  cursor: pointer;
  transition: all var(--transition);
}

.search-btn:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-glow-primary);
}

.error-actions {
  display: flex;
  gap: var(--spacing-md);
  justify-content: center;
  flex-wrap: wrap;
}

@media (max-width: 600px) {
  .not-found-page { padding: var(--spacing-md); }
  .not-found-content { padding: var(--spacing-xl) var(--spacing-md); }
  .error-code { font-size: 5rem; }
  .error-title { font-size: 1.25rem; }
  .error-actions { flex-direction: column; }
  .error-actions .btn { width: 100%; }
  .search-box { max-width: 100%; }
}
</style>
