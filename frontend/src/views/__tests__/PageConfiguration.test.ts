import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ref, type Component } from 'vue'
import { pagePreviewKey } from '@/composables/usePageConfig'
import Home from '../Home.vue'
import About from '../About.vue'
import Archives from '../Archives.vue'
import Gallery from '../gallery/Gallery.vue'
import Search from '../search/Search.vue'
import TagDetail from '../tag/TagDetail.vue'
import PostDetail from '../post/PostDetail.vue'
import NotFound from '../common/NotFound.vue'
import { usePagesStore } from '@/stores/pages'
import { pageApi } from '@/api/pages'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import { galleryApi } from '@/api/gallery'
import { publicInteractionApi } from '@/api/public-interactions'
import type { PageConfig } from '@/types/page'
import type { Post } from '@/types'

vi.mock('@/api/pages', () => ({ pageApi: { getPage: vi.fn() } }))
vi.mock('@/api/post', () => ({
  postApi: {
    getPostList: vi.fn(),
    advancedSearch: vi.fn(),
    getPostById: vi.fn(),
    getAdjacentPosts: vi.fn()
  }
}))
vi.mock('@/api/tag', () => ({ tagApi: { getTagList: vi.fn(), getTagById: vi.fn() } }))
vi.mock('@/api/gallery', () => ({ galleryApi: { getAlbums: vi.fn() } }))
vi.mock('@/api/public-interactions', () => ({
  publicInteractionApi: { getLikeStatus: vi.fn(), getComments: vi.fn() }
}))
enableAutoUnmount(afterEach)
const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
const post: Post = {
  id: 1,
  title: '真实文章标题',
  content: '## 正文\n\n内容',
  category: 'tech',
  authorId: 1,
  authorName: '作者',
  viewCount: 1,
  likeCount: 0,
  commentCount: 0,
  collectCount: 0,
  shareCount: 0,
  status: 1,
  createdAt: '2026-10-16',
  coverImage: '/uploads/a.jpg'
}
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(postApi.getPostList).mockResolvedValue(
    response({ records: [post], total: 1, pages: 1, pageNum: 1, pageSize: 10 })
  )
  vi.mocked(postApi.advancedSearch).mockResolvedValue(
    response({ records: [post], total: 1, pages: 1, pageNum: 1, pageSize: 10 })
  )
  vi.mocked(postApi.getPostById).mockResolvedValue(response(post))
  vi.mocked(postApi.getAdjacentPosts).mockResolvedValue(
    response({ prev: { id: 2, title: '上一篇' }, next: null })
  )
  vi.mocked(tagApi.getTagList).mockResolvedValue(response([{ id: 1, name: '技术' }]))
  vi.mocked(tagApi.getTagById).mockResolvedValue(response({ id: 1, name: '真实标签' }))
  vi.mocked(galleryApi.getAlbums).mockResolvedValue(
    response([
      {
        id: 1,
        title: '旅行',
        privateAlbum: true,
        locked: true,
        sortOrder: 0,
        createTime: '2026-10-16',
        description: '相册介绍',
        photoCount: 3,
        photos: []
      }
    ])
  )
  vi.mocked(publicInteractionApi.getLikeStatus).mockResolvedValue(
    response({ liked: false, likeCount: 0 })
  )
  vi.mocked(publicInteractionApi.getComments).mockResolvedValue(
    response({ records: [], total: 0, size: 10, current: 1, pages: 1 })
  )
})
async function setup(
  component: Component,
  id: string,
  config: PageConfig,
  path = '/',
  preview = false
) {
  const pinia = createPinia()
  setActivePinia(pinia)
  usePagesStore().pages[id] = { id, config }
  vi.mocked(pageApi.getPage).mockResolvedValue(response({ id, config }))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
  })
  await router.push(path)
  const wrapper = mount(component, {
    global: {
      plugins: [pinia, router],
      provide: preview
        ? { [pagePreviewKey as symbol]: { id, config: ref(config), articleId: ref('1') } }
        : {},
      stubs: { teleport: true, transition: true }
    }
  })
  await flushPromises()
  return wrapper
}
describe('published configuration reaches every public template', () => {
  it('reloads About body when a newer manifest publication arrives after the first read', async () => {
    const wrapper = await setup(About, 'about', { title: '旧关于', body: '旧正文' })
    vi.mocked(pageApi.getPage).mockResolvedValue(
      response({
        id: 'about',
        updatedAt: '2026-10-16T13:00:00Z',
        config: { title: '新关于', body: '新的完整正文' }
      })
    )
    usePagesStore().pages.about = {
      id: 'about',
      updatedAt: '2026-10-16T13:00:00Z',
      config: { title: '新关于' }
    }
    await flushPromises()
    expect(wrapper.get('.page-markdown').text()).toContain('新的完整正文')
  })
  it('does not change document metadata while previewing an article template', async () => {
    document.title = '页面编辑器'
    document.getElementById('selfblog-article-jsonld')?.remove()
    await setup(PostDetail, 'post', {}, '/admin/pages/post', true)
    expect(document.title).toBe('页面编辑器')
    expect(document.getElementById('selfblog-article-jsonld')).toBeNull()
  })
  it('supports About Markdown without exposing the global author/site sections', async () => {
    const wrapper = await setup(About, 'about', {
      title: '我的故事',
      description: '',
      body: '## 独立正文\n\n正文可以编辑',
      modules: { author: false, site: false, links: false }
    })
    expect(wrapper.get('h1').text()).toBe('我的故事')
    expect(wrapper.get('.page-markdown').text()).toContain('独立正文')
    expect(wrapper.find('.author-profile, .about-site, nav').exists()).toBe(false)
  })
  it('allows the Home hero and sidebar modules to be hidden without removing articles', async () => {
    const wrapper = await setup(Home, 'home', {
      text: { listTitle: '文章目录' },
      modules: { hero: false, author: false, tags: false, archiveLink: false }
    })
    expect(wrapper.get('#latest-heading').text()).toBe('文章目录')
    expect(wrapper.find('.intro-shell, .home-sidebar').exists()).toBe(false)
    expect(wrapper.text()).toContain('真实文章标题')
  })
  it('applies Archives copy and metadata switches', async () => {
    const wrapper = await setup(Archives, 'archives', {
      title: '写作时间线',
      description: '',
      modules: { category: false, count: false }
    })
    expect(wrapper.get('h1').text()).toBe('写作时间线')
    expect(wrapper.find('.category, .archive-group h2 span').exists()).toBe(false)
  })
  it('changes Gallery copy without bypassing password locks', async () => {
    const wrapper = await setup(Gallery, 'gallery', {
      title: '光影',
      modules: { description: false, count: false }
    })
    expect(wrapper.get('h1').text()).toBe('光影')
    expect(wrapper.find('.album-description, .album-meta small').exists()).toBe(false)
    expect(wrapper.get('.lock-badge').text()).toContain('密码相册')
  })
  it('customizes Search copy but preserves its functional form and results', async () => {
    const wrapper = await setup(
      Search,
      'search',
      { title: '查找文章', text: { placeholder: '键入主题' }, modules: { count: false } },
      '/search?q=技术'
    )
    expect(wrapper.get('h1').text()).toBe('查找文章')
    expect(wrapper.get('#article-search').attributes('placeholder')).toBe('键入主题')
    expect(wrapper.find('.results-heading span').exists()).toBe(false)
    expect(wrapper.text()).toContain('真实文章标题')
  })
  it('retains real tag identity while changing its description and count visibility', async () => {
    const wrapper = await setup(
      TagDetail,
      'tag',
      { description: '标签补充说明', modules: { count: false } },
      '/tag/1'
    )
    expect(wrapper.get('h1').text()).toContain('真实标签')
    expect(wrapper.text()).toContain('标签补充说明')
    expect(wrapper.text()).not.toContain('1 篇相关文章')
  })
  it('respects Post template modules without replacing the actual title/body', async () => {
    const wrapper = await setup(
      PostDetail,
      'post',
      {
        description: '阅读提示',
        modules: { cover: false, meta: false, toc: false, adjacent: false }
      },
      '/post/1'
    )
    expect(wrapper.get('h1').text()).toBe('真实文章标题')
    expect(wrapper.text()).toContain('阅读提示')
    expect(
      wrapper
        .find('.article-cover, .article-meta, .article-aside, .post-adjacent, .toc-trigger')
        .exists()
    ).toBe(false)
    expect(wrapper.get('.markdown-body').text()).toContain('内容')
  })
  it('customizes NotFound without changing its not-found identity', async () => {
    const wrapper = await setup(
      NotFound,
      'notFound',
      {
        title: '走错路了',
        description: '从首页重新出发',
        modules: { search: false, homeLink: false }
      },
      '/missing'
    )
    expect(wrapper.get('h1').text()).toBe('走错路了')
    expect(wrapper.text()).toContain('404')
    expect(wrapper.find('.search-box, a[href="/"]').exists()).toBe(false)
  })
})
