import { afterEach, describe, expect, it } from 'vitest'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ArticleList from '../ArticleList.vue'
import type { Post } from '@/types'

enableAutoUnmount(afterEach)
const post: Post = {
  id: 1,
  title: '一篇真实的笔记',
  content: '正文'.repeat(300),
  summary: '文章摘要',
  authorId: 1,
  authorName: '作者',
  viewCount: 1,
  likeCount: 0,
  commentCount: 0,
  collectCount: 0,
  shareCount: 0,
  status: 1,
  createdAt: '2026-10-16T10:00:00',
  tags: [{ id: 2, name: '技术' }]
}
async function setup(posts = [post]) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
  })
  await router.push('/')
  return mount(ArticleList, { props: { posts }, global: { plugins: [router] } })
}

describe('shared article rows', () => {
  it('keeps real title/tag links and content-based reading time', async () => {
    const wrapper = await setup()
    expect(wrapper.get('h3 a').attributes('href')).toBe('/post/1')
    expect(wrapper.get('h3').text()).toBe(post.title)
    expect(wrapper.get('a[href="/tag/2"]').text()).toContain('技术')
    expect(wrapper.text()).toContain('分钟阅读')
  })

  it('does not reserve a cover slot for a coverless article', async () => {
    const wrapper = await setup()
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('.row-cover').exists()).toBe(false)
  })

  it('removes failed thumbnails rather than showing a broken placeholder', async () => {
    const wrapper = await setup([{ ...post, coverImage: '/uploads/cover.jpg' }])
    await wrapper.get('img').trigger('error')
    expect(wrapper.find('.row-cover').exists()).toBe(false)
  })

  it('supports legacy coverUrl and rejects unsafe images without default artwork', async () => {
    const wrapper = await setup([{ ...post, coverUrl: '/uploads/old.jpg' }])
    expect(wrapper.get('img').attributes('src')).toBe('/uploads/old.jpg')
    await wrapper.setProps({ posts: [{ ...post, coverImage: 'javascript:alert(1)' }] })
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('preserves long titles and every tag instead of silently dropping links', async () => {
    const title = '这是一段不会被数据截短的长标题'.repeat(8)
    const tags = Array.from({ length: 6 }, (_, id) => ({ id, name: `标签${id}` }))
    const wrapper = await setup([{ ...post, title, tags }])
    expect(wrapper.get('h3').text()).toBe(title)
    expect(wrapper.findAll('.article-tags a')).toHaveLength(6)
  })
})
