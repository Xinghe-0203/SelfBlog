import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, enableAutoUnmount, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import PostDetail from '../PostDetail.vue'
import { postApi } from '@/api/post'
import { publicInteractionApi } from '@/api/public-interactions'
import type { Post } from '@/types'

vi.mock('@/api/post', () => ({ postApi: { getPostById: vi.fn(), getAdjacentPosts: vi.fn() } }))
vi.mock('@/api/public-interactions', () => ({ publicInteractionApi: { getLikeStatus: vi.fn(), toggleLike: vi.fn(), getComments: vi.fn(), createComment: vi.fn() } }))
vi.mock('@/utils/toast', () => ({ toast: { success: vi.fn(), warning: vi.fn(), error: vi.fn() } }))
enableAutoUnmount(afterEach)
let frames: FrameRequestCallback[] = []
const post: Post = {
  id: 1, title: '阅读测试', content: '## 第一节\n\n正文\n\n```ts\nconst a = 1\n```\n\n<script>alert(1)</script>',
  authorId: 1, authorName: '作者', viewCount: 1, likeCount: 0, commentCount: 0, collectCount: 0,
  shareCount: 0, status: 1, createdAt: '2026-10-16', coverImage: '/uploads/cover.jpg'
}
const response = <T,>(data: T) => ({ code: 200, message: 'ok', data })
beforeEach(() => {
  vi.resetAllMocks()
  frames = []
  vi.stubGlobal('requestAnimationFrame', (frame: FrameRequestCallback) => { frames.push(frame); return frames.length })
  vi.stubGlobal('cancelAnimationFrame', vi.fn())
  Object.defineProperty(window, 'scrollY', { configurable: true, value: 0, writable: true })
  Object.defineProperty(window, 'innerHeight', { configurable: true, value: 1000 })
  Object.defineProperty(document.documentElement, 'scrollHeight', { configurable: true, value: 3000 })
  vi.mocked(postApi.getPostById).mockResolvedValue(response(post))
  vi.mocked(postApi.getAdjacentPosts).mockResolvedValue(response({ prev: null, next: null }))
  vi.mocked(publicInteractionApi.getLikeStatus).mockResolvedValue(response({ liked: false, likeCount: 0 }))
  vi.mocked(publicInteractionApi.getComments).mockResolvedValue(response({ records: [], total: 0, pages: 1, size: 10, current: 1 }))
})
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals() })
async function setup() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }, { path: '/post/:id', component: { template: '<div />' } }] })
  await router.push('/post/1')
  const wrapper = mount(PostDetail, { global: { plugins: [createPinia(), router], stubs: { teleport: true, transition: true } } })
  await flushPromises()
  return wrapper
}
describe('reader layout and preserved interactions', () => {
  it('introduces the title and metadata before the optional cover', async () => {
    const wrapper = await setup()
    expect(wrapper.get('.article-header').element.compareDocumentPosition(wrapper.get('.article-cover').element) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })
  it('preserves sanitization, code tools and same-page heading anchors', async () => {
    const wrapper = await setup()
    expect(wrapper.find('.markdown-body script').exists()).toBe(false)
    expect(wrapper.get('.code-copy').text()).toBe('复制')
    expect(wrapper.get('.heading-anchor').attributes('href')).toBe('#section-0')
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
    Object.defineProperty(wrapper.get('#section-0').element, 'scrollIntoView', { configurable: true, value: scroll })
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList)
    await wrapper.get('.toc-card button').trigger('click')
    expect(scroll).toHaveBeenCalledWith({ behavior: 'auto', block: 'start' })
  })
  it('keeps anonymous likes and trimmed guest comments available', async () => {
    vi.mocked(publicInteractionApi.toggleLike).mockResolvedValue(response({ liked: true, likeCount: 1 }))
    vi.mocked(publicInteractionApi.createComment).mockResolvedValue(response(1))
    const wrapper = await setup()
    await wrapper.get('.like-action').trigger('click')
    await flushPromises()
    expect(wrapper.get('.like-action').text()).toContain('已点赞')
    await wrapper.get('.comment-form input').setValue('  读者  ')
    await wrapper.get('.comment-form textarea').setValue('  一段留言  ')
    await wrapper.get('.comment-form').trigger('submit')
    await flushPromises()
    expect(publicInteractionApi.createComment).toHaveBeenCalledWith({ postId: 1, nickname: '读者', content: '一段留言' })
  })
})
