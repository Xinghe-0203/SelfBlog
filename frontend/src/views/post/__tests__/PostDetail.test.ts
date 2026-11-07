import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, enableAutoUnmount, mount } from '@vue/test-utils'
import { nextTick, ref } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import PostDetail from '../PostDetail.vue'
import { postApi } from '@/api/post'
import { publicInteractionApi } from '@/api/public-interactions'
import { pagePreviewKey } from '@/composables/usePageConfig'
import { useSiteStore, type SiteSettings } from '@/stores/site'
import type { Post, RelatedPostItem } from '@/types'

vi.mock('@/api/post', () => ({
  postApi: { getPostById: vi.fn(), getAdjacentPosts: vi.fn(), getRelated: vi.fn() }
}))
vi.mock('@/api/public-interactions', () => ({
  publicInteractionApi: {
    getLikeStatus: vi.fn(),
    toggleLike: vi.fn(),
    getComments: vi.fn(),
    createComment: vi.fn()
  }
}))
vi.mock('@/utils/toast', () => ({ toast: { success: vi.fn(), warning: vi.fn(), error: vi.fn() } }))
enableAutoUnmount(afterEach)
let frames: FrameRequestCallback[] = []
const post: Post = {
  id: 1,
  title: '阅读测试',
  content: '## 第一节\n\n正文\n\n```ts\nconst a = 1\n```\n\n<script>alert(1)</script>',
  authorId: 1,
  authorName: '作者',
  viewCount: 1,
  likeCount: 0,
  commentCount: 0,
  collectCount: 0,
  shareCount: 0,
  status: 1,
  createdAt: '2026-10-16',
  coverImage: '/uploads/cover.jpg'
}
const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
beforeEach(() => {
  vi.resetAllMocks()
  frames = []
  vi.stubGlobal('requestAnimationFrame', (frame: FrameRequestCallback) => {
    frames.push(frame)
    return frames.length
  })
  vi.stubGlobal('cancelAnimationFrame', vi.fn())
  Object.defineProperty(window, 'scrollY', { configurable: true, value: 0, writable: true })
  Object.defineProperty(window, 'innerHeight', { configurable: true, value: 1000 })
  Object.defineProperty(document.documentElement, 'scrollHeight', {
    configurable: true,
    value: 3000
  })
  vi.mocked(postApi.getPostById).mockResolvedValue(response(post))
  vi.mocked(postApi.getAdjacentPosts).mockResolvedValue(response({ prev: null, next: null }))
  vi.mocked(postApi.getRelated).mockResolvedValue(response([]))
  vi.mocked(publicInteractionApi.getLikeStatus).mockResolvedValue(
    response({ liked: false, likeCount: 0 })
  )
  vi.mocked(publicInteractionApi.getComments).mockResolvedValue(
    response({ records: [], total: 0, pages: 1, size: 10, current: 1 })
  )
})
afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})
async function setupWithContent(
  content: string,
  settings?: Partial<SiteSettings>,
  postOverrides?: Partial<Post>
) {
  vi.mocked(postApi.getPostById).mockResolvedValue(response({ ...post, ...postOverrides, content }))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
      { path: '/post/:id', component: { template: '<div />' } }
    ]
  })
  await router.push('/post/1')
  const pinia = createPinia()
  if (settings) {
    setActivePinia(pinia)
    useSiteStore().applySettings(settings)
  }
  const wrapper = mount(PostDetail, {
    global: { plugins: [pinia, router], stubs: { teleport: true, transition: true } }
  })
  await flushPromises()
  return { wrapper, router }
}
async function setup() {
  return (await setupWithContent(post.content)).wrapper
}
describe('reader layout and preserved interactions', () => {
  it('introduces the title and metadata before the optional cover', async () => {
    const wrapper = await setup()
    expect(
      wrapper
        .get('.article-header')
        .element.compareDocumentPosition(wrapper.get('.article-cover').element) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })
  it('preserves sanitization, code tools and same-page heading anchors', async () => {
    const wrapper = await setup()
    expect(wrapper.find('.markdown-body script').exists()).toBe(false)
    expect(wrapper.get('.code-copy').text()).toBe('复制')
    expect(wrapper.get('.heading-anchor').attributes('href')).toBe('#section-0')
  })
  it('hides the reading progress bar when the setting is off', async () => {
    const { wrapper } = await setupWithContent(post.content, { readingProgress: 'off' })
    expect(wrapper.find('.reading-progress').exists()).toBe(false)
  })
  it('keeps the toc aside, mobile sheet and fab on the default style', async () => {
    const wrapper = await setup()
    expect(wrapper.find('.article-aside').exists()).toBe(true)
    expect(wrapper.find('.toc-fab').exists()).toBe(true)
    expect(wrapper.find('.reader-layout').classes()).not.toContain('reader-layout--no-toc')
  })
  it('hides the toc aside, mobile sheet and fab and collapses the grid when tocStyle is hidden', async () => {
    const { wrapper } = await setupWithContent(post.content, { tocStyle: 'hidden' })
    expect(wrapper.find('.article-aside').exists()).toBe(false)
    // MobileTocSheet（含左下角的 .toc-fab 浮动胶囊）整块不渲染
    expect(wrapper.find('.toc-sheet').exists()).toBe(false)
    expect(wrapper.find('.toc-fab').exists()).toBe(false)
    // 侧栏没了，阅读网格要塌成单列，不留 200px 空槽
    expect(wrapper.get('.reader-layout').classes()).toContain('reader-layout--no-toc')
  })
  it('coalesces progress updates into one frame and cancels work on unmount', async () => {
    const wrapper = await setup()
    window.scrollY = 500
    window.dispatchEvent(new Event('scroll'))
    window.dispatchEvent(new Event('scroll'))
    expect(wrapper.get('.reading-progress').attributes('style')).toContain('0%')
    expect(frames).toHaveLength(1)
    frames.shift()?.(0)
    await nextTick()
    expect(wrapper.get('.reading-progress').attributes('style')).toContain('25%')
    window.dispatchEvent(new Event('scroll'))
    wrapper.unmount()
    expect(cancelAnimationFrame).toHaveBeenCalled()
  })
  it('respects reduced motion for directory navigation', async () => {
    const wrapper = await setup()
    const scroll = vi.fn()
    Object.defineProperty(wrapper.get('#section-0').element, 'scrollIntoView', {
      configurable: true,
      value: scroll
    })
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList)
    await wrapper.get('.toc-card button').trigger('click')
    expect(scroll).toHaveBeenCalledWith({ behavior: 'auto', block: 'start' })
  })
  it('keeps anonymous likes and trimmed guest comments available', async () => {
    vi.mocked(publicInteractionApi.toggleLike).mockResolvedValue(
      response({ liked: true, likeCount: 1 })
    )
    vi.mocked(publicInteractionApi.createComment).mockResolvedValue(response(1))
    const wrapper = await setup()
    await wrapper.get('.like-action').trigger('click')
    await flushPromises()
    expect(wrapper.get('.like-action').text()).toContain('已点赞')
    await wrapper.get('.comment-form input').setValue('  读者  ')
    await wrapper.get('.comment-form textarea').setValue('  一段留言  ')
    await wrapper.get('.comment-form').trigger('submit')
    await flushPromises()
    expect(publicInteractionApi.createComment).toHaveBeenCalledWith({
      postId: 1,
      nickname: '读者',
      content: '一段留言'
    })
  })

  it('keeps loaded comments and the page cursor when loading more fails', async () => {
    const first = {
      id: 1,
      postId: 1,
      nickname: '甲',
      content: '第一条',
      status: 1,
      createTime: '2026-10-16T10:00:00'
    }
    vi.mocked(publicInteractionApi.getComments)
      .mockResolvedValueOnce(
        response({ records: [first], total: 3, pages: 1, size: 10, current: 1 })
      )
      .mockRejectedValueOnce(new Error('network down'))

    const wrapper = await setup()
    expect(wrapper.findAll('.reader-comment')).toHaveLength(1)

    await wrapper.get('.load-more-comments').trigger('click')
    await flushPromises()

    // 失败只提示错误：已加载的留言不能被清空。
    expect(wrapper.findAll('.reader-comment')).toHaveLength(1)
    expect(wrapper.get('.reader-comment').text()).toContain('第一条')
    expect(wrapper.get('.comments-error').text()).toContain('留言暂时无法加载')

    // 页码没有推进：重试仍然请求第 2 页，不会跳过失败的这一页。
    await wrapper.get('.load-more-comments').trigger('click')
    await flushPromises()
    expect(publicInteractionApi.getComments).toHaveBeenLastCalledWith(1, {
      pageNum: 2,
      pageSize: 10
    })
  })

  it('turns a scheme-less markdown link into an external absolute URL', async () => {
    const { wrapper } = await setupWithContent('[外站](example.com/path)')
    const anchor = wrapper.get('.markdown-body a')

    // 修复前 href="example.com/path" 会被浏览器当成站内相对路径 → 跳到本站 404。
    expect(anchor.attributes('href')).toBe('https://example.com/path')
    expect(anchor.attributes('target')).toBe('_blank')
    expect(anchor.attributes('rel')).toBe('noopener noreferrer')
  })

  it('routes in-site links through the router instead of reloading the page', async () => {
    const { wrapper, router } = await setupWithContent('[站内文章](/post/2)')
    const anchor = wrapper.get('.markdown-body a')

    // 站内链接不加 target，交给 onContentClick 走 SPA 跳转。
    expect(anchor.attributes('target')).toBeUndefined()
    await anchor.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/post/2')
  })

  it('scrolls to same-page anchors without leaving the article', async () => {
    const { wrapper, router } = await setupWithContent('## 第二节\n\n[回到本节](#section-0)')
    const anchor = wrapper.get('.markdown-body a[href="#section-0"]')
    const scrollIntoView = vi.fn()
    const section = wrapper.get('#section-0').element
    section.scrollIntoView = scrollIntoView

    await anchor.trigger('click')

    expect(scrollIntoView).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/post/1')
  })
})

describe('related reading section', () => {
  const related: RelatedPostItem[] = [
    {
      id: 2,
      title: '相关文章一',
      summary: '第一篇的摘要',
      coverUrl: '',
      createTime: '2026-10-01T10:00:00'
    },
    {
      id: 3,
      title: '相关文章二',
      summary: '',
      coverUrl: '',
      createTime: '2026-09-20T10:00:00'
    }
  ]

  it('renders rows with internal links after the markdown body when getRelated returns posts', async () => {
    vi.mocked(postApi.getRelated).mockResolvedValue(response(related))
    vi.mocked(postApi.getAdjacentPosts).mockResolvedValue(
      response({ prev: { id: 8, title: '上一篇' }, next: { id: 9, title: '下一篇' } })
    )
    const wrapper = await setup()

    const rows = wrapper.findAll('.related-item')
    expect(rows).toHaveLength(2)
    expect(rows[0]?.text()).toContain('相关文章一')
    expect(rows[0]?.text()).toContain('2026年10月01日')
    expect(rows[0]?.get('a').attributes('href')).toBe('/post/2')
    expect(rows[1]?.text()).toContain('相关文章二')
    expect(rows[1]?.text()).toContain('2026年09月20日')
    // 正文在前，相关阅读随后、相邻文章导航之前
    expect(
      wrapper
        .get('.markdown-body')
        .element.compareDocumentPosition(wrapper.get('.related-posts').element) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
    expect(
      wrapper
        .get('.related-posts')
        .element.compareDocumentPosition(wrapper.get('.post-adjacent').element) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })

  it('stays absent when the related list is empty or the request fails', async () => {
    vi.mocked(postApi.getRelated).mockResolvedValue(response([]))
    const wrapper = await setup()
    expect(wrapper.find('.related-posts').exists()).toBe(false)

    vi.mocked(postApi.getRelated).mockRejectedValue(new Error('offline'))
    const failed = (await setupWithContent('另一篇文章')).wrapper
    expect(failed.find('.related-posts').exists()).toBe(false)
  })

  it('is not fetched in preview mode', async () => {
    vi.mocked(postApi.getRelated).mockResolvedValue(response(related))
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
        { path: '/post/:id', component: { template: '<div />' } }
      ]
    })
    await router.push('/post/1')
    const wrapper = mount(PostDetail, {
      global: {
        plugins: [createPinia(), router],
        provide: {
          [pagePreviewKey as symbol]: { id: 'post', config: ref({}), articleId: ref('1') }
        },
        stubs: { teleport: true, transition: true }
      }
    })
    await flushPromises()

    expect(postApi.getRelated).not.toHaveBeenCalled()
    expect(wrapper.find('.related-posts').exists()).toBe(false)
  })
})

describe('summary callout', () => {
  const summaryOverrides = { summary: '这是一段面向访客的文章摘要。' }

  it('renders nothing by default even when the post has a summary', async () => {
    const { wrapper } = await setupWithContent(post.content, undefined, summaryOverrides)
    expect(wrapper.find('.summary-callout').exists()).toBe(false)
  })

  it('stays hidden when the summary is blank even with the setting on', async () => {
    const { wrapper } = await setupWithContent(
      post.content,
      { summaryCallout: 'on' },
      { summary: '   ' }
    )
    expect(wrapper.find('.summary-callout').exists()).toBe(false)
  })

  it('shows the summary as plain text between the cover area and the body when enabled', async () => {
    const { wrapper } = await setupWithContent(
      post.content,
      { summaryCallout: 'on' },
      summaryOverrides
    )
    const callout = wrapper.get('.summary-callout')
    // 纯文本插值，不是 v-html：节点内部不允许出现标签
    expect(callout.get('.summary-callout-text').text()).toBe(summaryOverrides.summary)
    expect(callout.get('.summary-callout-text').element.innerHTML).not.toMatch(/<\w+/)
    expect(callout.get('.summary-callout-label').text()).toBe('摘要')
    // 封面 → 摘要卡 → 正文 的顺序
    expect(
      wrapper.get('.article-cover').element.compareDocumentPosition(callout.element) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
    expect(
      callout.element.compareDocumentPosition(wrapper.get('.markdown-body').element) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })

  it('is not rendered in preview mode even with the setting on', async () => {
    vi.mocked(postApi.getPostById).mockResolvedValue(response({ ...post, ...summaryOverrides }))
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
        { path: '/post/:id', component: { template: '<div />' } }
      ]
    })
    await router.push('/post/1')
    const pinia = createPinia()
    setActivePinia(pinia)
    useSiteStore().applySettings({ summaryCallout: 'on' })
    const wrapper = mount(PostDetail, {
      global: {
        plugins: [pinia, router],
        provide: {
          [pagePreviewKey as symbol]: { id: 'post', config: ref({}), articleId: ref('1') }
        },
        stubs: { teleport: true, transition: true }
      }
    })
    await flushPromises()

    expect(wrapper.find('.summary-callout').exists()).toBe(false)
  })
})

describe('article meta word count', () => {
  it('shows the estimated word count next to the reading time', async () => {
    // 4 个中文字 + 1 个英文词 = 5
    const { wrapper } = await setupWithContent('你好世界 hello')
    expect(wrapper.get('.article-meta').text()).toContain('约 5 字')
  })

  it('omits the word count when the body has no countable words', async () => {
    const { wrapper } = await setupWithContent('')
    expect(wrapper.get('.article-meta').text()).not.toContain('字')
  })
})

describe('post license footer', () => {
  const LICENSE = 'CC BY-NC-SA 4.0'

  afterEach(() => {
    // 单条用例注入的 selfblog-site-origin 标记不能泄漏进其它用例的 origin 解析
    document.querySelector('meta[name="selfblog-site-origin"]')?.remove()
  })

  it('renders nothing by default so the DOM stays as before the feature', async () => {
    const wrapper = await setup()
    expect(wrapper.find('.post-license').exists()).toBe(false)
  })

  it('stays hidden when the setting is blank whitespace', async () => {
    const { wrapper } = await setupWithContent(post.content, { postLicense: '   ' })
    expect(wrapper.find('.post-license').exists()).toBe(false)
  })

  it('renders the license text and a self permalink between the body and related reading', async () => {
    vi.mocked(postApi.getRelated).mockResolvedValue(
      response([{ id: 2, title: '相关一', summary: '', coverUrl: '', createTime: '2026-10-01' }])
    )
    const { wrapper } = await setupWithContent(post.content, { postLicense: LICENSE })
    const license = wrapper.get('.post-license')
    // 纯文本插值，不是 v-html
    expect(license.get('.post-license-text').text()).toBe(LICENSE)
    const link = license.get('a')
    expect(link.text()).toBe(`${window.location.origin}/post/1`)
    expect(link.attributes('href')).toBe(`${window.location.origin}/post/1`)
    expect(link.attributes('referrerpolicy')).toBe('no-referrer')
    // 正文之后、相关阅读之前
    expect(
      wrapper.get('.markdown-body').element.compareDocumentPosition(license.element) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
    expect(
      license.element.compareDocumentPosition(wrapper.get('.related-posts').element) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })

  it('prefers the selfblog-site-origin marker for the permalink like SEO canonicals do', async () => {
    const meta = document.createElement('meta')
    meta.setAttribute('name', 'selfblog-site-origin')
    meta.setAttribute('content', 'https://blog.example.com')
    document.head.appendChild(meta)
    const { wrapper } = await setupWithContent(post.content, { postLicense: LICENSE })
    expect(wrapper.get('.post-license a').attributes('href')).toBe(
      'https://blog.example.com/post/1'
    )
  })
})
