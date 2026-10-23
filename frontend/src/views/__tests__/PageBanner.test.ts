import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { Component } from 'vue'
import Home from '../Home.vue'
import About from '../About.vue'
import { usePagesStore } from '@/stores/pages'
import { useSiteStore } from '@/stores/site'
import { pageApi } from '@/api/pages'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import type { PageConfig } from '@/types/page'
import type { Post } from '@/types'

vi.mock('@/api/pages', () => ({ pageApi: { getPage: vi.fn() } }))
vi.mock('@/api/post', () => ({ postApi: { getPostList: vi.fn() } }))
vi.mock('@/api/tag', () => ({ tagApi: { getTagList: vi.fn() } }))
enableAutoUnmount(afterEach)

const response = <T>(data: T) => ({ code: 200, message: 'ok', data })

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
  createdAt: '2026-10-16T10:00:00',
  coverImage: '/uploads/first.jpg'
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(postApi.getPostList).mockResolvedValue(
    response({ records: [post], total: 1, pages: 1, pageNum: 1, pageSize: 9 })
  )
  vi.mocked(tagApi.getTagList).mockResolvedValue(response([]))
  vi.mocked(pageApi.getPage).mockResolvedValue(response({ id: 'about', config: {} }))
})

async function setup(component: Component, id: string, config: PageConfig) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useSiteStore().applySettings({
    siteTitle: 'Brand',
    siteDescription: '我想认真记录的事情',
    authorName: '真实作者',
    authorBio: '来自站点设置的介绍'
  })
  usePagesStore().pages[id] = { id, config }
  vi.mocked(pageApi.getPage).mockResolvedValue(response({ id, config }))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
  })
  await router.push('/')
  const wrapper = mount(component, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper }
}

describe('首页与关于页的横幅背景', () => {
  it('没有横幅时首页介绍区保持原样：仍然是第一篇文章封面', async () => {
    const { wrapper } = await setup(Home, 'home', {})
    const shell = wrapper.get('.intro-shell')
    expect(shell.classes()).not.toContain('intro-shell--banner')
    expect(wrapper.find('.intro-banner').exists()).toBe(false)
    expect(wrapper.get('.intro-visual img').attributes('src')).toBe('/uploads/first.jpg')
    expect(shell.attributes('style') ?? '').not.toContain('--banner-image')
    expect(shell.attributes('data-banner-tone')).toBeUndefined()
  })

  it('左右排布：文字在左、背景图在右，封面不再单独出现', async () => {
    const { wrapper } = await setup(Home, 'home', {
      style: { banner: { image: '/uploads/hero.jpg', layout: 'split', height: 'tall' } }
    })
    const shell = wrapper.get('.intro-shell')
    expect(shell.classes()).toContain('intro-shell--banner')
    expect(shell.classes()).toContain('intro-shell--banner-split')
    expect(wrapper.find('.intro-banner').exists()).toBe(true)
    expect(wrapper.find('.intro-visual').exists()).toBe(false)
    expect(shell.attributes('style')).toContain('--banner-image: url("/uploads/hero.jpg")')
    expect(shell.attributes('style')).toContain('--banner-height: 330px')
  })

  it('居中排布：整块铺图，文字压在上面；遮罩够深时文字转浅色', async () => {
    const { wrapper } = await setup(Home, 'home', {
      style: {
        banner: { image: 'https://cdn.example.com/hero.jpg', layout: 'centered', overlay: 50 }
      }
    })
    const shell = wrapper.get('.intro-shell')
    expect(shell.classes()).toContain('intro-shell--banner-centered')
    expect(shell.attributes('data-banner-tone')).toBe('light')
    expect(shell.attributes('style')).toContain('--banner-overlay: 50')
  })

  it('上下排布：图在上、文字在下', async () => {
    const { wrapper } = await setup(Home, 'home', {
      style: { banner: { image: '/uploads/hero.jpg', layout: 'stacked' } }
    })
    const shell = wrapper.get('.intro-shell')
    expect(shell.classes()).toContain('intro-shell--banner-stacked')
    expect(shell.attributes('data-banner-tone')).toBe('dark')
  })

  it('背景图地址非法时按没有横幅处理，绝不把地址拼进样式', async () => {
    const { wrapper } = await setup(Home, 'home', {
      style: { banner: { image: 'javascript:alert(1)' } }
    })
    expect(wrapper.find('.intro-banner').exists()).toBe(false)
    expect(wrapper.get('.intro-visual img').attributes('src')).toBe('/uploads/first.jpg')
    expect(wrapper.get('.intro-shell').attributes('style') ?? '').not.toContain('--banner-image')
  })

  it('关于页没有横幅时作者卡保持原样', async () => {
    const { wrapper } = await setup(About, 'about', {})
    const card = wrapper.get('.author-profile')
    expect(card.classes()).not.toContain('author-profile--banner')
    expect(wrapper.find('.profile-banner').exists()).toBe(false)
    expect(card.attributes('style') ?? '').not.toContain('--banner-image')
  })

  it('关于页左右排布：头像与文字在左、背景图在右', async () => {
    const { wrapper } = await setup(About, 'about', {
      style: { banner: { image: '/uploads/about.jpg', layout: 'split', height: 'compact' } }
    })
    const card = wrapper.get('.author-profile')
    expect(card.classes()).toContain('author-profile--banner')
    expect(card.classes()).toContain('author-profile--banner-split')
    expect(wrapper.find('.profile-banner').exists()).toBe(true)
    expect(card.attributes('style')).toContain('--banner-height: 160px')
  })

  it('关于页居中排布：整卡铺图，文字压在上面', async () => {
    const { wrapper } = await setup(About, 'about', {
      style: { banner: { image: '/uploads/about.jpg', layout: 'centered', tone: 'light' } }
    })
    const card = wrapper.get('.author-profile')
    expect(card.classes()).toContain('author-profile--banner-centered')
    expect(card.attributes('data-banner-tone')).toBe('light')
  })
})
