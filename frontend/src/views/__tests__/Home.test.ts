import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import Home from '../Home.vue'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import {
  resolveSidebarOrder,
  SIDEBAR_ORDER_VALUES,
  useSiteStore,
  type SiteSettings
} from '@/stores/site'
import type { Post, Tag } from '@/types'

vi.mock('@/api/post', () => ({ postApi: { getPostList: vi.fn() } }))
vi.mock('@/api/tag', () => ({ tagApi: { getTagList: vi.fn() } }))
enableAutoUnmount(afterEach)
const post: Post = {
  id: 1,
  title: '测试文章',
  content: '正文',
  authorId: 1,
  authorName: '作者',
  viewCount: 1,
  likeCount: 0,
  commentCount: 0,
  collectCount: 0,
  shareCount: 0,
  status: 1,
  createdAt: '2026-10-16T10:00:00'
}
const page = (posts: Post[]) => ({
  code: 200,
  message: 'ok',
  data: { records: posts, total: 2, pages: 2, pageNum: 1, pageSize: 9 }
})
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(postApi.getPostList).mockResolvedValue(page([post]))
  vi.mocked(tagApi.getTagList).mockResolvedValue({ code: 200, message: 'ok', data: [] })
})
async function setup(path = '/', settings: Partial<SiteSettings> = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useSiteStore().applySettings({
    siteTitle: 'Brand',
    siteDescription: '我想认真记录的事情',
    authorName: '真实作者',
    authorBio: '来自站点设置的介绍',
    ...settings
  })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
  })
  await router.push(path)
  const wrapper = mount(Home, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('home spatial introduction and article list', () => {
  it('uses the configured introduction rather than a duplicate brand heading', async () => {
    const { wrapper } = await setup()
    expect(wrapper.get('h1').text()).toBe('我想认真记录的事情')
    expect(wrapper.text()).toContain('真实作者')
    expect(wrapper.text()).toContain('来自站点设置的介绍')
    expect(wrapper.find('.featured-cover').exists()).toBe(false)
    expect(wrapper.findAll('.article-row')).toHaveLength(1)
  })

  it('keeps the first successful batch cover stable across pagination', async () => {
    vi.mocked(postApi.getPostList)
      .mockResolvedValueOnce(page([post, { ...post, id: 2, coverImage: '/uploads/first.jpg' }]))
      .mockResolvedValueOnce(page([{ ...post, coverImage: '/uploads/later.jpg' }]))
    const { wrapper, router } = await setup()
    expect(wrapper.get('.intro-visual img').attributes('src')).toBe('/uploads/first.jpg')
    await router.push('/?page=2')
    await flushPromises()
    expect(wrapper.get('.intro-visual img').attributes('src')).toBe('/uploads/first.jpg')
    expect(postApi.getPostList).toHaveBeenLastCalledWith({
      pageNum: 2,
      pageSize: 9,
      sort: 'latest'
    })
  })

  it('uses a neutral hero after a failed cover without switching on later pages', async () => {
    vi.mocked(postApi.getPostList).mockResolvedValue(
      page([{ ...post, coverImage: '/uploads/broken.jpg' }])
    )
    const { wrapper, router } = await setup()
    // The cover panel paints its own grey backdrop, so a broken image must remove
    // the whole panel rather than leaving a 43%-wide empty rectangle behind.
    expect(wrapper.find('.intro-visual').exists()).toBe(true)
    await wrapper.get('.intro-visual img').trigger('error')
    await flushPromises()
    expect(wrapper.find('.intro-visual').exists()).toBe(false)
    expect(wrapper.find('.intro-visual img').exists()).toBe(false)
    await router.push('/?page=2')
    await flushPromises()
    expect(wrapper.find('.intro-visual').exists()).toBe(false)
  })

  it('allows retry after an initial failure and captures that successful cover', async () => {
    vi.mocked(postApi.getPostList)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(page([{ ...post, coverImage: '/uploads/retry.jpg' }]))
    const { wrapper } = await setup('/?page=2')
    await wrapper.get('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.get('.intro-visual img').attributes('src')).toBe('/uploads/retry.jpg')
  })
})

describe('resolveSidebarOrder', () => {
  it('passes the default order and every whitelisted permutation through unchanged', () => {
    // 默认值 = 现有模板顺序；六个合法排列逐一原样返回。
    expect(resolveSidebarOrder('author-tags-archive')).toBe('author-tags-archive')
    for (const order of SIDEBAR_ORDER_VALUES) {
      expect(resolveSidebarOrder(order)).toBe(order)
    }
  })

  it('falls back to the current template order on junk, empty or missing input', () => {
    expect(resolveSidebarOrder('author-tags')).toBe('author-tags-archive')
    expect(resolveSidebarOrder('author/tags/archive')).toBe('author-tags-archive')
    expect(resolveSidebarOrder('')).toBe('author-tags-archive')
    expect(resolveSidebarOrder(undefined)).toBe('author-tags-archive')
    // 与 pickEnum 同一条归一化规则：前后空白不影响判定
    expect(resolveSidebarOrder(' archive-tags-author ')).toBe('archive-tags-author')
  })
})

describe('home sidebar ordering', () => {
  const withTags: Tag[] = [
    { id: 3, name: '前端' },
    { id: 4, name: '随笔' }
  ]
  type HomeWrapper = Awaited<ReturnType<typeof setup>>['wrapper']

  /** 侧栏里渲染出来的模块，按 DOM 顺序给出 id（author / tags / archive）。 */
  function blockOrder(wrapper: HomeWrapper): string[] {
    return wrapper.findAll('.home-sidebar > *').map((node) => {
      if (node.classes().includes('author-card')) return 'author'
      if (node.classes().includes('archive-callout')) return 'archive'
      return 'tags'
    })
  }

  it('keeps the default template order (author, tags, archive)', async () => {
    vi.mocked(tagApi.getTagList).mockResolvedValue({ code: 200, message: 'ok', data: withTags })
    const { wrapper } = await setup()
    expect(blockOrder(wrapper)).toEqual(['author', 'tags', 'archive'])
  })

  it('reorders the same three blocks when a non-default order is configured', async () => {
    vi.mocked(tagApi.getTagList).mockResolvedValue({ code: 200, message: 'ok', data: withTags })
    const { wrapper } = await setup('/', { homeSidebarOrder: 'archive-tags-author' })
    expect(blockOrder(wrapper)).toEqual(['archive', 'tags', 'author'])
  })

  it('still honours each block visibility condition in any order', async () => {
    // 默认 mock 的标签列表为空：即使 tags 排在最前，空标签块也不渲染，
    // 其余两块按配置顺序出现（showModule 条件原样生效）。
    const { wrapper } = await setup('/', { homeSidebarOrder: 'tags-archive-author' })
    expect(blockOrder(wrapper)).toEqual(['archive', 'author'])
  })
})
