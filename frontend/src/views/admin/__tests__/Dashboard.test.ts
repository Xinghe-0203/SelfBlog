import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import Dashboard from '../Dashboard.vue'
import { siteApi } from '@/api/site'

/**
 * 概览页新增了「内容体检」入口（F 块）。这里只锁住入口指向，
 * 不重复测访客统计本身。
 */
vi.mock('@/api/site', () => ({ siteApi: { getVisitorSummary: vi.fn() } }))
enableAutoUnmount(afterEach)

async function setup() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div />' } }]
  })
  await router.push('/')
  const wrapper = mount(Dashboard, {
    global: {
      plugins: [createPinia(), router],
      stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } }
    }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(siteApi.getVisitorSummary).mockResolvedValue({
    code: 200,
    message: 'ok',
    data: {
      totalVisitors: 10,
      todayVisitors: 2,
      totalPageViews: 30,
      todayPageViews: 5,
      articleViews: 12,
      daily: [],
      topPages: []
    }
  })
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('dashboard workspace entries', () => {
  it('links to the content health page next to the other author workspaces', async () => {
    const wrapper = await setup()
    const cards = wrapper.findAll('.workspace-card')
    const labels = cards.map((card) => card.get('h2').text())
    expect(labels).toContain('内容体检')
    expect(labels).toContain('SEO 管理')

    const health = cards.find((card) => card.get('h2').text() === '内容体检')
    expect(health?.attributes('href')).toBe('/admin/health')
    expect(health?.get('.card-link').text()).toContain('开始体检')
  })
})
