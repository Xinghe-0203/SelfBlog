<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '@/components/common/Icon.vue'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { usePageSeo } from '@/composables/usePageSeo'
const { config, text, module: showModule } = usePageConfig('notFound')
usePageSeo('notFound', () => ({
  title: text('title', '页面不存在'),
  description: text('description', ''),
  indexable: false
}))
const router = useRouter()
const searchQuery = ref('')
function doSearch(): void {
  const q = searchQuery.value.trim()
  if (q) void router.push({ path: '/search', query: { q } })
}
</script>

<template>
  <PageFrame :config="config" class="not-found-page"
    ><section class="not-found-content">
      <p class="error-code">404</p>
      <h1>{{ text('title', '页面不存在') }}</h1>
      <p class="error-text">
        {{ text('description', '这个链接可能已失效，也可以搜索文章或返回首页。') }}
      </p>
      <form
        v-if="showModule('search')"
        class="search-box"
        role="search"
        aria-label="查找文章"
        @submit.prevent="doSearch"
      >
        <input
          v-model="searchQuery"
          type="search"
          aria-label="搜索文章"
          placeholder="输入关键词"
        /><button type="submit" class="btn btn-primary" aria-label="搜索">
          <Icon name="search" :size="18" />
        </button>
      </form>
      <div class="error-actions">
        <router-link v-if="showModule('homeLink')" to="/" class="btn btn-secondary"
          ><Icon name="home" :size="17" />返回首页</router-link
        ><button type="button" class="btn btn-ghost" @click="router.go(-1)">返回上一页</button>
      </div>
    </section></PageFrame
  >
</template>

<style scoped>
.not-found-page {
  max-width: var(--page-width, 680px);
  margin-inline: auto;
  padding: var(--page-padding-block, 48px) 24px;
}
.not-found-content {
  padding: 38px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.error-code {
  margin-bottom: 14px;
  font-size: var(--text-sm);
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
h1 {
  font-size: var(--text-3xl);
}
.error-text {
  margin-block: 16px 24px;
  font-size: var(--text-sm);
}
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
}
.search-box:focus-within {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.search-box input {
  flex: 1;
  min-width: 0;
  padding-inline: 10px;
  border: 0;
  outline: 0;
  color: var(--text-primary);
  background: transparent;
  font-size: var(--text-base);
}
.error-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}
@media (max-width: 600px) {
  .not-found-page {
    padding: 8px 18px 36px;
  }
  .not-found-content {
    padding: 24px;
  }
  h1 {
    font-size: var(--text-2xl);
  }
}
</style>
