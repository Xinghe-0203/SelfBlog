<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { enhanceMarkdown, disposeMarkdownEnhance } from '@/composables/useMarkdownEnhance'
import { renderMarkdown } from '@/utils/markdown'
const props = defineProps<{ source: string }>()
const root = ref<HTMLElement | null>(null)
let active = true
// 统一走 utils/markdown：协议补全 + 站内/站外链接属性，与文章详情、编辑器预览保持一致
const html = computed(() => renderMarkdown(props.source))
watch(
  html,
  async () => {
    await nextTick()
    if (!active || !root.value) return
    // 旧正文的复制按钮即将随 v-html 一起被丢弃，先释放它排队的回退定时器
    disposeMarkdownEnhance(root.value)
    enhanceMarkdown(root.value)
  },
  { immediate: true }
)
onBeforeUnmount(() => {
  active = false
  if (root.value) disposeMarkdownEnhance(root.value)
})
</script>
<template><div ref="root" class="markdown-body page-markdown" v-html="html" /></template>
<style scoped>
.page-markdown {
  overflow-wrap: anywhere;
  font-size: var(--text-base);
  line-height: 1.9;
}
.page-markdown :deep(img) {
  max-width: 100%;
  height: auto;
}
.page-markdown :deep(pre) {
  overflow-x: auto;
}
.page-markdown :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
}
</style>
