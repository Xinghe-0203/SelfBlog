import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PagesManagement from '../PagesManagement.vue'
import NavigationEditor from '../NavigationEditor.vue'
import { pageApi } from '@/api/pages'

vi.mock('@/api/pages', () => ({
  pageApi: {
    list: vi.fn(),
    create: vi.fn(),
    getNavigation: vi.fn(),
    saveNavigation: vi.fn(),
    getManifest: vi.fn()
  }
}))
enableAutoUnmount(afterEach)
afterEach(() => vi.restoreAllMocks())
const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(pageApi.list).mockResolvedValue(response([]))
  vi.mocked(pageApi.getNavigation).mockResolvedValue(
    response({
      schemaVersion: 1,
      revision: 2,
      items: [
        { pageId: 'home', label: '首页' },
        { pageId: 'about', label: '关于' }
      ]
    })
  )
})
async function setup(component = PagesManagement) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin/pages', component },
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } }
    ]
  })
  await router.push('/admin/pages')
  const wrapper = mount(
    { template: '<RouterView />' },
    {
      global: {
        plugins: [createPinia(), router],
        stubs: { NavigationEditor: component === PagesManagement }
      }
    }
  )
  await flushPromises()
  return { wrapper, router }
}
describe('page management and navigation', () => {
  it('does not redirect back to a created draft after the author has left the page', async () => {
    let finish!: (value: Awaited<ReturnType<typeof pageApi.create>>) => void
    vi.mocked(pageApi.create).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    const { wrapper, router } = await setup()
    await wrapper.get('#new-page-title').setValue('项目')
    await wrapper.get('#new-page-slug').setValue('projects')
    await wrapper.get('.create-page-form').trigger('submit')
    wrapper.unmount()
    await router.push('/')
    finish(
      response({
        id: 'custom-1',
        label: '内容页',
        builtin: false,
        draft: {
          schemaVersion: 1,
          revision: 1,
          slug: 'projects',
          everPublished: false,
          config: { title: '项目' }
        }
      })
    )
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
  })
  it('protects unsaved navigation labels when reloading after a conflict', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    vi.mocked(pageApi.saveNavigation).mockRejectedValue({ userMessage: '导航已被更新' })
    const { wrapper } = await setup(NavigationEditor)
    await wrapper.get('input[aria-label="导航名称：首页"]').setValue('我的主页')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    await wrapper.get('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(window.confirm).toHaveBeenCalled()
    expect(wrapper.get<HTMLInputElement>('input').element.value).toBe('我的主页')
    expect(pageApi.getNavigation).toHaveBeenCalledTimes(1)
    vi.restoreAllMocks()
  })
  it('creates a private content draft before navigating to its editor', async () => {
    vi.mocked(pageApi.create).mockResolvedValue(
      response({
        id: 'custom-1',
        label: '内容页',
        builtin: false,
        draft: {
          schemaVersion: 1,
          revision: 1,
          slug: 'projects',
          everPublished: false,
          config: { title: '项目' }
        }
      })
    )
    const { wrapper, router } = await setup()
    await wrapper.get('#new-page-title').setValue('项目')
    await wrapper.get('#new-page-slug').setValue('projects')
    await wrapper.get('.create-page-form').trigger('submit')
    await flushPromises()
    expect(pageApi.create).toHaveBeenCalledWith('projects', '项目')
    expect(router.currentRoute.value.path).toBe('/admin/pages/custom-1')
  })
  it('shows a create conflict and retains the author-entered slug', async () => {
    vi.mocked(pageApi.create).mockRejectedValue({ userMessage: '地址已被占用' })
    const { wrapper } = await setup()
    await wrapper.get('#new-page-title').setValue('项目')
    await wrapper.get('#new-page-slug').setValue('projects')
    await wrapper.get('.create-page-form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('地址已被占用')
    expect(wrapper.get<HTMLInputElement>('#new-page-slug').element.value).toBe('projects')
  })
  it('edits navigation labels and sends its own revision without exposing page drafts', async () => {
    vi.mocked(pageApi.saveNavigation).mockResolvedValue(
      response({
        schemaVersion: 1,
        revision: 3,
        items: [
          { pageId: 'home', label: '主页' },
          { pageId: 'about', label: '关于' }
        ]
      })
    )
    vi.mocked(pageApi.getManifest).mockResolvedValue(response({ pages: [], navigation: [] }))
    const { wrapper } = await setup(NavigationEditor)
    await wrapper.get('input[aria-label="导航名称：首页"]').setValue('主页')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(pageApi.saveNavigation).toHaveBeenCalledWith(2, [
      { pageId: 'home', label: '主页' },
      { pageId: 'about', label: '关于' }
    ])
  })
})
