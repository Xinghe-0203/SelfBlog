import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { pageApi } from '@/api/pages'
import { usePagesStore } from '../pages'

vi.mock('@/api/pages', () => ({
  pageApi: { getManifest: vi.fn(), getPage: vi.fn(), getBySlug: vi.fn() }
}))
beforeEach(() => {
  vi.resetAllMocks()
  setActivePinia(createPinia())
})
const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
describe('published page store', () => {
  it('keeps a loaded body when the slower manifest describes the same publication', async () => {
    vi.mocked(pageApi.getPage).mockResolvedValue(
      response({
        id: 'about',
        updatedAt: '2026-10-16T12:00:00Z',
        config: { title: '关于', body: '不能被摘要丢弃的正文' }
      })
    )
    vi.mocked(pageApi.getManifest).mockResolvedValue(
      response({
        pages: [{ id: 'about', updatedAt: '2026-10-16T12:00:00Z', config: { title: '关于' } }],
        navigation: []
      })
    )
    const store = usePagesStore()
    await store.loadPage('about')
    await store.loadManifest()
    expect(store.pages.about?.config.body).toBe('不能被摘要丢弃的正文')
  })
  it('accepts a later body response when the manifest still points at that publication', async () => {
    let finish!: (value: Awaited<ReturnType<typeof pageApi.getPage>>) => void
    vi.mocked(pageApi.getPage).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    vi.mocked(pageApi.getManifest).mockResolvedValue(
      response({
        pages: [{ id: 'about', updatedAt: '2026-10-16T12:00:00Z', config: { title: '关于' } }],
        navigation: []
      })
    )
    const store = usePagesStore()
    const pending = store.loadPage('about')
    await store.loadManifest()
    finish(
      response({
        id: 'about',
        updatedAt: '2026-10-16T12:00:00Z',
        config: { title: '关于', body: '晚到的同版本正文' }
      })
    )
    await pending
    expect(store.pages.about?.config.body).toBe('晚到的同版本正文')
  })
  it('retains the original four navigation items if the optional API is unavailable', async () => {
    vi.mocked(pageApi.getManifest).mockRejectedValue(new Error('offline'))
    const store = usePagesStore()
    await store.loadManifest()
    expect(store.navigation.map((link) => link.to)).toEqual([
      '/',
      '/archives',
      '/gallery',
      '/about'
    ])
    expect(store.pages.about).toBeUndefined()
  })
  it('uses published manifest data and never stores author drafts in browser storage', async () => {
    vi.mocked(pageApi.getManifest).mockResolvedValue(
      response({
        pages: [{ id: 'about', config: { title: '关于我' } }],
        navigation: [{ pageId: 'home', label: '主页', to: '/' }]
      })
    )
    const store = usePagesStore()
    await store.loadManifest()
    expect(store.pages.about?.config.title).toBe('关于我')
    expect(store.navigation[0]?.label).toBe('主页')
    expect(localStorage.getItem('selfblog-pages')).toBeNull()
  })
  it('does not let a stale page read replace a newer manifest', async () => {
    let finish!: (value: Awaited<ReturnType<typeof pageApi.getPage>>) => void
    vi.mocked(pageApi.getPage).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    const store = usePagesStore()
    const pending = store.loadPage('about')
    vi.mocked(pageApi.getManifest).mockResolvedValue(
      response({ pages: [{ id: 'about', config: { title: '新版' } }], navigation: [] })
    )
    await store.loadManifest()
    finish(response({ id: 'about', config: { title: '旧版', body: '过时正文' } }))
    await pending
    expect(store.pages.about?.config.title).toBe('新版')
  })
})
