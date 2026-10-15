import { describe, it, expect, beforeEach } from 'vitest'
import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import AdminLayout from '../AdminLayout.vue'
import { useSiteStore } from '@/stores/site'

const Blank = { template: '<div />' }

const routes = [
  '/',
  '/admin/dashboard',
  '/admin/posts',
  '/admin/tags',
  '/admin/visitors',
  '/admin/comments',
  '/admin/albums',
  '/admin/settings'
].map((path) => ({ path, component: Blank }))

/** 组件不注册全局监听，router/pinia 每个用例重建即可，无需 enableAutoUnmount。 */
let pinia: ReturnType<typeof createPinia>
let router: ReturnType<typeof createRouter>

function mountLayout() {
  return mount(AdminLayout, { global: { plugins: [pinia, router] } })
}

describe('AdminLayout', () => {
  beforeEach(async () => {
    pinia = createPinia()
    setActivePinia(pinia)
    router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/admin/dashboard')
    await router.isReady()
  })

  it('falls back to the built-in logo icon when no logo is uploaded', () => {
    const wrapper = mountLayout()
    expect(wrapper.find('.brand-mark .icon').exists()).toBe(true)
    expect(wrapper.find('img.brand-mark--image').exists()).toBe(false)
  })

  it('renders the uploaded logo instead of the built-in icon', async () => {
    useSiteStore().applySettings({ siteLogoUrl: '/uploads/site-assets/logo/a.svg' })
    const wrapper = mountLayout()
    await nextTick()

    const img = wrapper.find('img.brand-mark--image')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/uploads/site-assets/logo/a.svg')
  })

  it('replaces the text glyphs with the built-in icon set', () => {
    const wrapper = mountLayout()

    expect(wrapper.findAll('.sidebar-nav .nav-icon .icon')).toHaveLength(7)
    expect(wrapper.findAll('.nav-icon').every((node) => node.text() === '')).toBe(true)
    expect(wrapper.find('.menu-button .icon').exists()).toBe(true)
    expect(wrapper.find('.close-menu .icon').exists()).toBe(true)
    expect(wrapper.find('.sidebar-footer .icon').exists()).toBe(true)
  })

  it('marks the current admin route as active', async () => {
    await router.push('/admin/posts')
    const wrapper = mountLayout()
    await nextTick()

    const active = wrapper.findAll('.sidebar-nav a.active')
    expect(active).toHaveLength(1)
    expect(active[0]?.text()).toContain('文章管理')
  })
})
