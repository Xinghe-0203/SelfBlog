import api from './index'
import type { ApiResponse } from '@/types'
import type {
  PageManifest,
  PublicPage,
  PageEditorDocument,
  PageConfig,
  NavigationState,
  NavigationItem
} from '@/types/page'

export const pageApi = {
  getManifest(): Promise<ApiResponse<PageManifest>> {
    return api.get('/site/pages')
  },
  getPage(id: string): Promise<ApiResponse<PublicPage>> {
    return api.get(`/site/pages/${encodeURIComponent(id)}`)
  },
  getBySlug(slug: string): Promise<ApiResponse<PublicPage>> {
    return api.get(`/site/pages/by-slug/${encodeURIComponent(slug)}`)
  },
  list(): Promise<ApiResponse<PageEditorDocument[]>> {
    return api.get('/admin/pages')
  },
  getEditor(id: string): Promise<ApiResponse<PageEditorDocument>> {
    return api.get(`/admin/pages/${encodeURIComponent(id)}`)
  },
  create(slug: string, title: string): Promise<ApiResponse<PageEditorDocument>> {
    return api.post('/admin/pages', { slug, title })
  },
  saveDraft(
    id: string,
    revision: number,
    config: PageConfig,
    slug?: string | null
  ): Promise<ApiResponse<PageEditorDocument>> {
    return api.put(`/admin/pages/${encodeURIComponent(id)}/draft`, { revision, config, slug })
  },
  publish(id: string, revision: number): Promise<ApiResponse<PageEditorDocument>> {
    return api.put(`/admin/pages/${encodeURIComponent(id)}/publication`, { revision })
  },
  unpublish(id: string, revision: number): Promise<ApiResponse<PageEditorDocument>> {
    return api.delete(`/admin/pages/${encodeURIComponent(id)}/publication`, {
      params: { revision }
    })
  },
  delete(id: string, revision: number): Promise<ApiResponse<void>> {
    return api.delete(`/admin/pages/${encodeURIComponent(id)}`, { params: { revision } })
  },
  getNavigation(): Promise<ApiResponse<NavigationState>> {
    return api.get('/admin/page-navigation')
  },
  saveNavigation(revision: number, items: NavigationItem[]): Promise<ApiResponse<NavigationState>> {
    return api.put('/admin/page-navigation', { revision, items })
  }
}
