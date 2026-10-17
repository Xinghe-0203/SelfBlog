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
import type { Post } from '@/types'

vi.mock('@/api/post', () => ({ postApi: { getPostList: vi.fn(), advancedSearch: vi.fn() } }))
vi.mock('@/api/tag', () => ({ tagApi: { getTagById: vi.fn() } }))
enableAutoUnmount(afterEach)
const post: Post = {
  id: 1,
  title: '一篇笔记',
  content: '正文',
  authorId: 1,
  authorName: '作者',
  viewCount: 1,
  likeCount: 0,
  commentCount: 0,
  collectCount: 0,
  shareCount: 0,
  status: 1,
  createdAt: '2026-10-16T10:00:00',
  tags: Array.from({ length: 6 }, (_, id) => ({ id, name: `标签${id}` }))
}
const page = (records: Post[], pages = 1) => ({
  code: 200,
  message: 'ok',
  data: { records, pages, total: records.length, pageNum: 1, pageSize: 12 }
})
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(postApi.advancedSearch).mockResolvedValue(page([post]))
  vi.mocked(postApi.getPostList).mockResolvedValue(page([post]))
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
  it('loads every archive page, including older months', async () => {
    vi.mocked(postApi.getPostList)
      .mockResolvedValueOnce(page([post], 2))
      .mockResolvedValueOnce(
        page([{ ...post, id: 2, title: '旧月份', createdAt: '2025-03-01' }], 2)
      )
    const { wrapper } = await setup(Archives, '/archives')
    expect(wrapper.findAll('.archive-group')).toHaveLength(2)
    expect(wrapper.find('a[href="/post/2"]').exists()).toBe(true)
    expect(postApi.getPostList).toHaveBeenLastCalledWith({
      pageNum: 2,
      pageSize: 100,
      sort: 'latest'
    })
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
})
