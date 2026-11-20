import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import Search from '../Search.vue'
import { postApi } from '@/api/post'

vi.mock('@/api/post', () => ({
  postApi: { advancedSearch: vi.fn(), getSearchSuggestions: vi.fn() }
}))
enableAutoUnmount(afterEach)

const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
const SUGGESTIONS = ['Vue 3 组合式 API', 'Vue Router 导航']

async function setupSearch(initialQuery = '', attach = false) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/search', component: { template: '<div />' } },
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } }
    ]
  })
  await router.push(initialQuery ? `/search?q=${encodeURIComponent(initialQuery)}` : '/search')
  const wrapper = mount(Search, {
    // 焦点断言需要真实挂进 document：VTU 默认容器是游离的，jsdom 对游离节点
    // 调用 focus() 不会改变 document.activeElement。
    attachTo: attach ? document.body : undefined,
    global: { plugins: [createPinia(), router], stubs: { teleport: true, transition: true } }
  })
  await flushPromises()
  return { wrapper, router }
}

/** 只伪造 setTimeout/clearTimeout：debounce 可控，同时 flushPromises（setImmediate）保持真实可用。 */
function useDebounceFakeTimers() {
  vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(postApi.getSearchSuggestions).mockResolvedValue(response(SUGGESTIONS))
  vi.mocked(postApi.advancedSearch).mockResolvedValue(
    response({ records: [], total: 0, size: 12, current: 1, pages: 1 })
  )
})

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('search typeahead suggestions', () => {
  it('debounces rapid typing into exactly one suggest call after 300ms and renders the dropdown', async () => {
    useDebounceFakeTimers()
    const { wrapper } = await setupSearch()
    const input = wrapper.get('#article-search')
    await input.setValue('v')
    await input.setValue('vu')
    await input.setValue('vue')
    expect(postApi.getSearchSuggestions).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(299)
    expect(postApi.getSearchSuggestions).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(1)
    expect(postApi.getSearchSuggestions).toHaveBeenCalledTimes(1)
    expect(postApi.getSearchSuggestions).toHaveBeenCalledWith('vue')
    await flushPromises()
    await nextTick()

    const list = wrapper.get('#article-search-suggestions')
    expect(list.attributes('role')).toBe('listbox')
    const options = wrapper.findAll('[role="option"]')
    expect(options).toHaveLength(2)
    expect(options[0]?.text()).toBe('Vue 3 组合式 API')
    expect(options[0]?.attributes('aria-selected')).toBe('false')
    expect(input.attributes('role')).toBe('combobox')
    expect(input.attributes('aria-expanded')).toBe('true')
    expect(input.attributes('aria-controls')).toBe('article-search-suggestions')
  })

  it('fills and submits the highlighted suggestion on ArrowDown + Enter', async () => {
    useDebounceFakeTimers()
    const { wrapper, router } = await setupSearch()
    const input = wrapper.get('#article-search')
    await input.setValue('vue')
    await vi.advanceTimersByTimeAsync(300)
    await flushPromises()
    await nextTick()

    await input.trigger('keydown', { key: 'ArrowDown' })
    const options = wrapper.findAll('[role="option"]')
    expect(options[0]?.classes()).toContain('active')
    expect(options[0]?.attributes('aria-selected')).toBe('true')

    await input.trigger('keydown', { key: 'Enter' })
    expect((input.element as HTMLInputElement).value).toBe('Vue 3 组合式 API')
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(false)
    // 走既有提交流程：路由同步 q，搜索请求用联想词发出
    await flushPromises()
    await flushPromises()
    expect(router.currentRoute.value.query.q).toBe('Vue 3 组合式 API')
    expect(postApi.advancedSearch).toHaveBeenCalledWith({
      keyword: 'Vue 3 组合式 API',
      pageNum: 1,
      pageSize: 12
    })
    // 选中后不为同一个词再补发联想
    expect(postApi.getSearchSuggestions).toHaveBeenCalledTimes(1)
  })

  it('wraps the active index at both ends of the list', async () => {
    useDebounceFakeTimers()
    const { wrapper } = await setupSearch()
    const input = wrapper.get('#article-search')
    await input.setValue('vue')
    await vi.advanceTimersByTimeAsync(300)
    await flushPromises()
    await nextTick()

    const options = () => wrapper.findAll('[role="option"]')
    await input.trigger('keydown', { key: 'ArrowDown' })
    expect(options()[0]?.classes()).toContain('active')
    // 首项再往上 → 环形到末项
    await input.trigger('keydown', { key: 'ArrowUp' })
    expect(options()[1]?.classes()).toContain('active')
    expect(options()[0]?.classes()).not.toContain('active')
    // 末项再往下 → 回到首项
    await input.trigger('keydown', { key: 'ArrowDown' })
    expect(options()[0]?.classes()).toContain('active')
  })

  it('closes the dropdown when the keyword is cleared and skips the empty fetch', async () => {
    useDebounceFakeTimers()
    const { wrapper } = await setupSearch()
    const input = wrapper.get('#article-search')
    await input.setValue('vue')
    await vi.advanceTimersByTimeAsync(300)
    await flushPromises()
    await nextTick()
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(true)

    await input.setValue('   ')
    await nextTick()
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(false)
    await vi.advanceTimersByTimeAsync(300)
    expect(postApi.getSearchSuggestions).toHaveBeenCalledTimes(1)
  })

  it('closes the dropdown on Escape and on input blur', async () => {
    useDebounceFakeTimers()
    const { wrapper } = await setupSearch()
    const input = wrapper.get('#article-search')
    await input.setValue('vue')
    await vi.advanceTimersByTimeAsync(300)
    await flushPromises()
    await nextTick()
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(true)

    await input.trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(false)

    // 再输入一轮后失焦关闭
    await input.setValue('vu')
    await vi.advanceTimersByTimeAsync(300)
    await flushPromises()
    await nextTick()
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(true)
    await input.trigger('blur')
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(false)
  })

  it('renders no dropdown when suggestions come back empty', async () => {
    useDebounceFakeTimers()
    vi.mocked(postApi.getSearchSuggestions).mockResolvedValue(response([]))
    const { wrapper } = await setupSearch()
    const input = wrapper.get('#article-search')
    await input.setValue('zzz')
    await vi.advanceTimersByTimeAsync(300)
    await flushPromises()
    await nextTick()
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(false)
    expect(input.attributes('aria-expanded')).toBe('false')
  })

  it('hides the empty-state prompt while the dropdown is open and restores it after close', async () => {
    // 联想下拉是绝对定位，会压在空态的装饰放大镜上：下拉展开期间整块收起
    useDebounceFakeTimers()
    const { wrapper } = await setupSearch()
    const prompt = () => wrapper.find('.search-prompt')
    expect(prompt().exists()).toBe(true)
    const input = wrapper.get('#article-search')
    await input.setValue('vue')
    await vi.advanceTimersByTimeAsync(300)
    await flushPromises()
    await nextTick()
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(true)
    expect(prompt().exists()).toBe(false)

    await input.trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('#article-search-suggestions').exists()).toBe(false)
    expect(prompt().exists()).toBe(true)
  })

  it('keeps the plain submit flow when Enter is pressed without an active suggestion', async () => {
    const { wrapper } = await setupSearch()
    const input = wrapper.get('#article-search')
    await input.setValue('  vue  ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(postApi.advancedSearch).toHaveBeenCalledWith({
      keyword: 'vue',
      pageNum: 1,
      pageSize: 12
    })
    expect(postApi.getSearchSuggestions).not.toHaveBeenCalled()
  })
})

describe('search input focus', () => {
  it('focuses the search input after mount', async () => {
    const { wrapper } = await setupSearch('', true)
    // 全局 Ctrl/Cmd+K 或 `/` 直达搜索页后，光标应该已经在输入框里
    expect(document.activeElement).toBe(wrapper.get('#article-search').element)
    wrapper.unmount()
  })
})

describe('page query sync with the route (and the navbar search)', () => {
  it('initializes the page input from route.query.q at mount', async () => {
    const { wrapper } = await setupSearch('hello')
    expect((wrapper.get('#article-search').element as HTMLInputElement).value).toBe('hello')
    expect(postApi.advancedSearch).toHaveBeenCalledWith({
      keyword: 'hello',
      pageNum: 1,
      pageSize: 12
    })
  })

  it('keeps the sync one-way: local typing never writes back to the route', async () => {
    const { wrapper, router } = await setupSearch('hello')
    const input = wrapper.get('#article-search')
    await input.setValue('hello2')
    await flushPromises()
    expect(router.currentRoute.value.query.q).toBe('hello')
    expect(router.currentRoute.value.fullPath).not.toContain('hello2')
  })

  it('follows a route change made outside the page (navbar submit) without a loop', async () => {
    const { wrapper, router } = await setupSearch('hello')
    await router.push({ path: '/search', query: { q: 'navbar word' } })
    await flushPromises()
    expect((wrapper.get('#article-search').element as HTMLInputElement).value).toBe('navbar word')
    expect(postApi.advancedSearch).toHaveBeenLastCalledWith({
      keyword: 'navbar word',
      pageNum: 1,
      pageSize: 12
    })
  })
})
