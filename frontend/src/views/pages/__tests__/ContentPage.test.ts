import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ContentPage from '../ContentPage.vue'
import { pageApi } from '@/api/pages'

vi.mock('@/api/pages', () => ({ pageApi: { getBySlug: vi.fn() } }))
enableAutoUnmount(afterEach)
beforeEach(() => vi.resetAllMocks())
const response = (title: string) => ({
  code: 200,
  message: 'ok',
  data: { id: 'custom-1', slug: title, config: { title, body: '## 页面正文' } }
})
async function setup() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pages/:slug', component: ContentPage },
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } }
    ]
  })
  await router.push('/pages/projects')
  const wrapper = mount(
    { template: '<RouterView />' },
    { global: { plugins: [createPinia(), router] } }
  )
  await flushPromises()
  return { wrapper, router }
}
describe('published content page', () => {
  it('renders published content with page styles and safe Markdown', async () => {
    vi.mocked(pageApi.getBySlug).mockResolvedValue(response('我的项目'))
    const { wrapper } = await setup()
    expect(wrapper.get('h1').text()).toBe('我的项目')
    expect(wrapper.get('.page-markdown').text()).toContain('页面正文')
  })
  it('shows unpublished/missing pages as not found, not as a successful empty page', async () => {
    vi.mocked(pageApi.getBySlug).mockRejectedValue({ response: { status: 404 } })
    const { wrapper } = await setup()
    expect(wrapper.text()).toContain('404')
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe(
      'noindex,follow'
    )
  })
  it('keeps transient API failures retryable instead of pretending they are 404', async () => {
    vi.mocked(pageApi.getBySlug)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(response('已恢复'))
    const { wrapper } = await setup()
    expect(wrapper.get('[role="alert"]').text()).toContain('重试')
    await wrapper.get('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.get('h1').text()).toBe('已恢复')
  })
  it('ignores an older slug response after navigation to another content page', async () => {
    let finish!: (value: ReturnType<typeof response>) => void
    vi.mocked(pageApi.getBySlug)
      .mockReturnValueOnce(
        new Promise((resolve) => {
          finish = resolve
        })
      )
      .mockResolvedValueOnce(response('第二个页面'))
    const { wrapper, router } = await setup()
    await router.push('/pages/second')
    await flushPromises()
    finish(response('过期页面'))
    await flushPromises()
    expect(wrapper.get('h1').text()).toBe('第二个页面')
  })
})
