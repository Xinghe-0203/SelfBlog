<script setup lang="ts">
import { createApp, getCurrentInstance, h, onBeforeUnmount, onMounted, ref, type App } from 'vue'
import { getActivePinia } from 'pinia'
import { useRouter } from 'vue-router'
import PagePreviewContent from './PagePreviewContent.vue'
import type { PageConfig } from '@/types/page'
const props = defineProps<{
  id: string
  config: PageConfig
  slug?: string | null
  updatedAt?: string | null
}>()
const router = useRouter()
const pinia = getActivePinia()
const parentContext = getCurrentInstance()?.appContext
const device = ref('desktop')
const frame = ref<HTMLIFrameElement | null>(null)
const theme = ref({ class: '', style: '', skin: 'ocean' })
let app: App | undefined
let observer: MutationObserver | undefined
const shell =
  '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>只读页面预览</title></head><body><div id="page-preview-app"></div></body></html>'

function syncDocument() {
  const target = frame.value?.contentDocument
  if (!target) return
  target.head.querySelectorAll('[data-preview-style]').forEach((node) => node.remove())
  document.head.querySelectorAll('style,link[rel="stylesheet"]').forEach((node) => {
    const copy = node.cloneNode(true) as HTMLElement
    copy.setAttribute('data-preview-style', '')
    target.head.appendChild(copy)
  })
  target.documentElement.className = document.documentElement.className
  const root = document.getElementById('selfblog')
  theme.value = {
    class: root?.className || '',
    style: root?.getAttribute('style') || '',
    skin: root?.dataset.skin || 'ocean'
  }
}
function mountFrame() {
  const container = frame.value?.contentDocument?.getElementById('page-preview-app')
  if (!container) return
  app?.unmount()
  syncDocument()
  // srcdoc contains no author content or scripts. Vue mounts the same safe components
  // into a real viewport, without a public draft URL or changing frame-security headers.
  app = createApp({
    render: () =>
      h(
        'div',
        {
          id: 'selfblog',
          class: theme.value.class,
          style: theme.value.style,
          'data-skin': theme.value.skin
        },
        [
          h('div', { id: 'overlay-root' }),
          h(PagePreviewContent, {
            id: props.id,
            config: props.config,
            slug: props.slug,
            updatedAt: props.updatedAt
          })
        ]
      )
  })
  if (pinia) app.use(pinia)
  app.use(router)
  if (parentContext?.config.errorHandler)
    app.config.errorHandler = parentContext.config.errorHandler
  app.mount(container)
}
onMounted(() => {
  observer = new MutationObserver(syncDocument)
  observer.observe(document.head, { childList: true, subtree: true, characterData: true })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
  const root = document.getElementById('selfblog')
  if (root)
    observer.observe(root, { attributes: true, attributeFilter: ['class', 'style', 'data-skin'] })
})
onBeforeUnmount(() => {
  observer?.disconnect()
  app?.unmount()
})
</script>
<template>
  <section class="page-preview" aria-label="页面预览">
    <header class="preview-toolbar">
      <label
        >预览宽度<select v-model="device">
          <option value="desktop">桌面</option>
          <option value="mobile">手机</option>
        </select></label
      ><small id="page-preview-caption"
        >只读预览复用真实页面；草稿不公开，也不会改变网站设置。</small
      >
    </header>
    <div class="preview-scroll" tabindex="0" aria-label="预览窗口，可横向滚动">
      <iframe
        ref="frame"
        :width="device === 'mobile' ? 390 : 1100"
        height="700"
        :srcdoc="shell"
        title="当前草稿的只读预览"
        aria-describedby="page-preview-caption"
        tabindex="-1"
        @load="mountFrame"
      />
    </div>
  </section>
</template>
<style scoped>
.page-preview {
  min-width: 0;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: var(--surface-primary);
}
.preview-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px 24px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
}
.preview-toolbar label {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}
.preview-toolbar select {
  min-height: 44px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  padding-inline: 12px;
  background: var(--surface-primary);
  color: var(--text-primary);
}
.preview-toolbar small {
  font-size: 12px;
  color: var(--text-muted);
}
.preview-scroll {
  max-width: 100%;
  overflow: auto;
  overscroll-behavior: contain;
  background: var(--bg-primary);
}
.preview-scroll iframe {
  display: block;
  max-width: none;
  margin-inline: auto;
  border: 0;
}
</style>
