<script setup lang="ts">
import type { PublicPage } from '@/types/page'
import { resolvePageText, pageModule } from '@/composables/usePageConfig'
import { formatDate } from '@/utils'
import PageFrame from './PageFrame.vue'
import MarkdownContent from './MarkdownContent.vue'
defineProps<{ page: PublicPage }>()
</script>
<template>
  <PageFrame :config="page.config" class="content-page">
    <header v-if="pageModule(page.config, 'header')" class="content-heading">
      <h1>{{ resolvePageText(page.config, 'title', '内容页') }}</h1>
      <p v-if="page.config.description">{{ page.config.description }}</p>
    </header>
    <h1 v-else class="sr-only">{{ page.config.title }}</h1>
    <p v-if="pageModule(page.config, 'updated') && page.updatedAt" class="content-updated">
      更新于 {{ formatDate(page.updatedAt, 'YYYY年MM月DD日') }}
    </p>
    <MarkdownContent v-if="page.config.body" :source="page.config.body" />
    <nav v-if="pageModule(page.config, 'links')" class="content-links" aria-label="继续阅读">
      <router-link to="/">返回首页</router-link><router-link to="/archives">文章归档</router-link>
    </nav>
  </PageFrame>
</template>
<style scoped>
.content-page {
  max-width: var(--page-width, 920px);
  margin-inline: auto;
  padding: var(--page-padding-block, 24px) 24px 48px;
}
.content-heading {
  margin-bottom: var(--page-section-gap, 28px);
}
.content-heading h1 {
  font-size: var(--text-3xl);
  overflow-wrap: anywhere;
}
.content-heading p {
  margin-top: 16px;
  white-space: pre-line;
  line-height: 1.9;
}
.content-updated {
  margin-bottom: 24px;
  font-size: var(--text-xs);
  color: var(--text-muted);
}
.content-links {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: var(--page-section-gap, 32px);
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}
.content-links a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  color: var(--accent);
  font-size: var(--text-sm);
}
@media (max-width: 600px) {
  .content-page {
    padding: 8px 18px 36px;
  }
  .content-heading h1 {
    font-size: var(--text-2xl);
  }
}
</style>
