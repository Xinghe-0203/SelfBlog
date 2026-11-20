import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, enableAutoUnmount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import TagsIndex from '../TagsIndex.vue'
import { tagApi } from '@/api/tag'

vi.mock('@/api/tag', () => ({ tagApi: { getTagsWithCounts: vi.fn() } }))
enableAutoUnmount(afterEach)

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(tagApi.getTagsWithCounts).mockResolvedValue({
    code: 200,
    message: 'ok',
    data: [
      { id: 1, name: '工程实践', postCount: 5 },
      { id: 2, name: '随笔', postCount: 1 }
    ]
  })
})

async function setup() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
  })
  await router.push('/')
  const wrapper = mount(TagsIndex, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return wrapper
}

describe('公开标签索引页', () => {
  it('按文章数倒序展示标签并链接到对应标签页', async () => {
    const wrapper = await setup()
    const names = wrapper.findAll('.tag-name').map((node) => node.text())
    expect(names).toEqual(['工程实践', '随笔'])
    expect(wrapper.find('a[href="/tag/1"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/tag/2"]').exists()).toBe(true)
  })

  it('展示每个标签下的已发布文章数', async () => {
    const wrapper = await setup()
    const counts = wrapper.findAll('.tag-count').map((node) => node.text())
    expect(counts).toEqual(['5', '1'])
  })

  it('接口失败时给出可重试的提示而不是空白页', async () => {
    vi.mocked(tagApi.getTagsWithCounts).mockRejectedValue(new Error('offline'))
    const wrapper = await setup()
    expect(wrapper.get('[role="alert"]').text()).toContain('标签暂时无法加载')
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('隐藏 0 篇已发布文章的标签，避免把读者送到空白页', async () => {
    vi.mocked(tagApi.getTagsWithCounts).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: [
        { id: 1, name: '工程实践', postCount: 3 },
        { id: 2, name: '空标签', postCount: 0 }
      ]
    })
    const wrapper = await setup()
    expect(wrapper.findAll('.tag-name').map((node) => node.text())).toEqual(['工程实践'])
    expect(wrapper.find('a[href="/tag/2"]').exists()).toBe(false)
  })

  it('没有标签时说明原因并给回首页的入口', async () => {
    vi.mocked(tagApi.getTagsWithCounts).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: []
    })
    const wrapper = await setup()
    expect(wrapper.text()).toContain('还没有任何标签')
    expect(wrapper.find('a[href="/"]').exists()).toBe(true)
  })
})
