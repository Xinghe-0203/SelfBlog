import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, enableAutoUnmount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import { useSiteStore } from '@/stores/site'
import Search from '../search/Search.vue'
import TagDetail from '../tag/TagDetail.vue'
import Archives from '../Archives.vue'
import About from '../About.vue'
import NotFound from '../common/NotFound.vue'
import type { Component } from 'vue'
import type { Post, PostArchiveItem } from '@/types'

vi.mock('@/api/post', () => ({
  postApi: { getPostList: vi.fn(), advancedSearch: vi.fn(), getArchives: vi.fn() }
}))
vi.mock('@/api/tag', () => ({ tagApi: { getTagById: vi.fn() } }))
enableAutoUnmount(afterEach)
const post: Post = {
  id: 1,
  title: '一篇笔记',
  content: '正文',
  viewCount: 1,
  likeCount: 0,
  commentCount: 0,
  collectCount: 0,
  shareCount: 0,
  status: 1,
  createdAt: '2026-10-16T10:00:00',
  tags: Array.from({ length: 6 }, (_, id) => ({ id, name: `标签${id}` }))
}
const archiveItem = (overrides: Partial<PostArchiveItem> = {}): PostArchiveItem => ({
  id: 1,
  title: '一篇笔记',
  category: '',
  createTime: '2026-10-16T10:00:00',
  updateTime: '2026-10-16T10:00:00',
  viewCount: 1,
  ...overrides
})
const page = (records: Post[], pages = 1) => ({
  code: 200,
  message: 'ok',
  data: { records, pages, total: records.length, size: 12, current: 1 }
})
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(postApi.advancedSearch).mockResolvedValue(page([post]))
  vi.mocked(postApi.getPostList).mockResolvedValue(page([post]))
  vi.mocked(postApi.getArchives).mockResolvedValue({
    code: 200,
    message: 'ok',
    data: [archiveItem()]
  })
  vi.mocked(tagApi.getTagById).mockResolvedValue({
    code: 200,
    message: 'ok',
    data: { id: 1, name: '设计' }
  })
})
async function setup(component: Component, path: string) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useSiteStore().applySettings({
    authorName: '真实作者',
    authorBio: '真实介绍',
    authorAvatar: '/uploads/avatar.png'
  })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/tag/:id', component: { template: '<div />' } },
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } }
    ]
  })
  await router.push(path)
  const wrapper = mount(component, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}
describe('public reading pages', () => {
  it('searches a trimmed query and uses the shared article rows', async () => {
    const { wrapper, router } = await setup(Search, '/search')
    await wrapper.get('input[type="search"]').setValue('  Vue  ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query.q).toBe('Vue')
    expect(wrapper.find('.article-list').exists()).toBe(true)
    expect(wrapper.get('a[href="/post/1"]').text()).toBe('一篇笔记')
  })
  it('marks an empty tag page as noindex while keeping it readable', async () => {
    // 空标签页可访问（作者可能刚建好标签就分享链接），但不该被搜索引擎收录。
    vi.mocked(postApi.advancedSearch).mockResolvedValue(page([], 1))
    await setup(TagDetail, '/tag/1')
    await flushPromises()
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe(
      'noindex,follow'
    )
  })

  it('retains every tag link in shared tag results and appends older articles', async () => {
    vi.mocked(postApi.advancedSearch)
      .mockResolvedValueOnce(page([post], 2))
      .mockResolvedValueOnce(page([{ ...post, id: 2, title: '旧文章' }], 2))
    const { wrapper } = await setup(TagDetail, '/tag/1')
    expect(wrapper.findAll('.article-tags a')).toHaveLength(6)
    await wrapper.get('.load-more button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.article-row')).toHaveLength(2)
  })
  it('renders every archive month from the single compact endpoint', async () => {
    vi.mocked(postApi.getArchives).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: [
        archiveItem(),
        archiveItem({
          id: 2,
          title: '旧月份',
          category: '手记',
          createTime: '2025-03-01T09:00:00',
          updateTime: '2025-03-01T09:00:00'
        })
      ]
    })
    const { wrapper } = await setup(Archives, '/archives')
    // 后端按创建时间倒序返回，前端保持原序分组：最新月份的分组在前。
    const [firstGroup, secondGroup] = wrapper.findAll('.archive-group')
    expect(firstGroup?.find('a[href="/post/1"]').exists()).toBe(true)
    expect(secondGroup?.find('a[href="/post/2"]').exists()).toBe(true)
    expect(wrapper.findAll('.archive-group')).toHaveLength(2)
    expect(postApi.getArchives).toHaveBeenCalledTimes(1)
    expect(postApi.getPostList).not.toHaveBeenCalled()
  })
  it('makes the about page author-first with configured avatar and biography', async () => {
    const { wrapper } = await setup(About, '/about')
    expect(wrapper.get('h1').text()).toBe('真实作者')
    expect(wrapper.get('img').attributes('src')).toBe('/uploads/avatar.png')
    expect(wrapper.text()).toContain('真实介绍')
    expect(wrapper.text()).not.toContain('如何浏览')
  })
  it('offers a named search form on 404 and trims the submitted query', async () => {
    const { wrapper, router } = await setup(NotFound, '/missing')
    await wrapper.get('input[type="search"]').setValue('  笔记  ')
    await wrapper.get('form[role="search"]').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query.q).toBe('笔记')
  })
  it('drops a slow response that lands after a newer search has already rendered', async () => {
    let resolveOld: ((value: ReturnType<typeof page>) => void) | undefined
    const old = new Promise<ReturnType<typeof page>>((resolve) => {
      resolveOld = resolve
    })
    vi.mocked(postApi.advancedSearch)
      .mockReturnValueOnce(old)
      .mockResolvedValueOnce(page([{ ...post, id: 2, title: '新结果' }]))

    const { wrapper, router } = await setup(Search, '/search')
    await router.push('/search?q=old')
    await flushPromises()
    await router.push('/search?q=new')
    await flushPromises()
    expect(wrapper.get('a[href="/post/2"]').text()).toBe('新结果')

    // 先发的旧请求最后才返回：它不许覆盖已经渲染的新结果。
    resolveOld?.(page([{ ...post, id: 3, title: '旧结果' }]))
    await flushPromises()
    expect(wrapper.find('a[href="/post/3"]').exists()).toBe(false)
    expect(wrapper.get('a[href="/post/2"]').text()).toBe('新结果')
  })
})
