import { ref } from 'vue'
import { defineStore } from 'pinia'
import { pageApi } from '@/api/pages'
import type { PublicPage, PublicNavigationItem } from '@/types/page'

/** Bodies and summaries may arrive in either order; compare the actual publication, not request order. */
export function publicationKey(page: PublicPage): string {
  if (page.updatedAt) return page.updatedAt
  const normalize = (value: unknown): unknown => {
    if (Array.isArray(value)) return value.map(normalize)
    if (value && typeof value === 'object')
      return Object.fromEntries(
        Object.entries(value)
          .filter(([key, child]) => key !== 'body' && child != null)
          .sort(([left], [right]) => left.localeCompare(right))
          .map(([key, child]) => [key, normalize(child)])
      )
    return value
  }
  return JSON.stringify(normalize(page.config))
}

export const defaultNavigation: PublicNavigationItem[] = [
  { pageId: 'home', label: '首页', to: '/' },
  { pageId: 'archives', label: '归档', to: '/archives' },
  { pageId: 'gallery', label: '相册', to: '/gallery' },
  { pageId: 'about', label: '关于', to: '/about' }
]
export const usePagesStore = defineStore('pages', () => {
  const pages = ref<Record<string, PublicPage>>({})
  const navigation = ref<PublicNavigationItem[]>([...defaultNavigation])
  let manifestRequest = 0
  let generation = 0
  const pageRequests = new Map<string, number>()

  async function loadManifest(): Promise<void> {
    const request = ++manifestRequest
    try {
      const response = await pageApi.getManifest()
      if (request !== manifestRequest) return
      generation++
      pages.value = Object.fromEntries(
        response.data.pages.map((page) => {
          const previous = pages.value[page.id]
          const keepBody =
            previous &&
            previous.config.body != null &&
            publicationKey(previous) === publicationKey(page)
          return [
            page.id,
            keepBody ? { ...page, config: { ...page.config, body: previous.config.body } } : page
          ]
        })
      )
      navigation.value = response.data.navigation
    } catch {
      /* Optional configuration: the existing site remains usable on failure. */
    }
  }
  async function loadPage(id: string): Promise<PublicPage> {
    const request = (pageRequests.get(id) || 0) + 1
    pageRequests.set(id, request)
    const started = generation
    const response = await pageApi.getPage(id)
    if (pageRequests.get(id) === request) {
      const current = pages.value[id]
      if (generation === started) pages.value[id] = response.data
      else if (current && publicationKey(current) === publicationKey(response.data)) {
        pages.value[id] = {
          ...current,
          config: { ...current.config, body: response.data.config.body }
        }
      }
    }
    return response.data
  }
  async function loadSlug(slug: string): Promise<PublicPage> {
    return (await pageApi.getBySlug(slug)).data
  }
  return { pages, navigation, loadManifest, loadPage, loadSlug }
})
