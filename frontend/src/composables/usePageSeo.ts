import { inject, watchEffect } from 'vue'
import { routeLocationKey } from 'vue-router'
import { useSiteStore } from '@/stores/site'
import { usePageConfig } from './usePageConfig'
import { applySeoMeta, applyArticleJsonLd, setSeoSiteTitle, type SeoMeta } from './useSeoMeta'
import type { PageConfig } from '@/types/page'

export function pageSeo(id: string, config: PageConfig, defaults: SeoMeta): SeoMeta {
  const seo = config.seo
  const dynamic = id === 'post' || id === 'tag'
  return {
    ...defaults,
    title: seo?.title ? (dynamic ? `${defaults.title} - ${seo.title}` : seo.title) : defaults.title,
    description: seo?.description || defaults.description,
    image: seo?.image || defaults.image,
    indexable:
      ['search', 'notFound', 'admin', 'preview'].includes(id) || defaults.indexable === false
        ? false
        : (seo?.indexable ?? defaults.indexable)
  }
}
export function usePageSeo(
  id: string,
  defaults: () => SeoMeta,
  configSource?: () => PageConfig
): void {
  const site = useSiteStore()
  const { config, isPreview } = usePageConfig(id)
  const route = inject(routeLocationKey, undefined)
  if (isPreview) return
  watchEffect(() => {
    if (route?.meta.adminArea) return
    const meta = pageSeo(id, configSource ? configSource() : config.value, defaults())
    setSeoSiteTitle(site.settings.siteTitle)
    applySeoMeta(meta)
    if (meta.type === 'article') applyArticleJsonLd(meta)
  })
}
