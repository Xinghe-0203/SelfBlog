<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { enhanceMarkdown } from '@/composables/useMarkdownEnhance'
const props = defineProps<{ source: string }>()
const root = ref<HTMLElement | null>(null)
let active = true
const html = computed(() =>
  DOMPurify.sanitize(marked.parse(props.source, { async: false }) as string, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['style', 'script', 'iframe', 'form', 'input', 'button'],
    FORBID_ATTR: ['style']
  })
)
watch(
  html,
  async () => {
    await nextTick()
    if (active && root.value) enhanceMarkdown(root.value)
  },
  { immediate: true }
)
onBeforeUnmount(() => {
  active = false
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
