import { computed, inject, type CSSProperties, type InjectionKey, type Ref } from 'vue'
import { usePagesStore } from '@/stores/pages'
import type { PageConfig } from '@/types/page'

export interface PagePreviewContext {
  id: string
  config: Ref<PageConfig>
  articleId?: Ref<string>
  tagId?: Ref<string>
}
export const pagePreviewKey: InjectionKey<PagePreviewContext> = Symbol('page-preview')

export function resolvePageText(config: PageConfig, key: string, fallback: string): string {
  const value =
    key === 'title' || key === 'description' || key === 'body' ? config[key] : config.text?.[key]
  return value ?? fallback
}
export function pageModule(config: PageConfig, key: string, fallback = true): boolean {
  return config.modules?.[key] ?? fallback
}
export function pageStyleVars(config: PageConfig): CSSProperties {
  const style: CSSProperties = {}
  const accent = config.style?.accent
  if (accent && /^#[a-f\d]{6}$/i.test(accent)) {
    style['--accent'] = accent
    style['--primary'] = 'var(--accent)'
    style['--accent-dark'] = `color-mix(in srgb, ${accent} 82%, var(--accent-shade, black))`
    style['--accent-light'] = `color-mix(in srgb, ${accent} 12%, transparent)`
  }
  return style
}
export function usePageConfig(id: string) {
  const pages = usePagesStore()
  const preview = inject(pagePreviewKey, undefined)
  const isPreview = !!preview
  const config = computed<PageConfig>(() =>
    preview?.id === id ? preview.config.value : pages.pages[id]?.config || {}
  )
  return {
    config,
    isPreview,
    preview,
    text: (key: string, fallback: string) => resolvePageText(config.value, key, fallback),
    module: (key: string, fallback = true) => pageModule(config.value, key, fallback)
  }
}
