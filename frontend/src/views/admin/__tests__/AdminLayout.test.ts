import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import AdminLayout from '../AdminLayout.vue'
import { useSiteStore } from '@/stores/site'
import { useThemeStore } from '@/stores/theme'

enableAutoUnmount(afterEach)

const Blank = { template: '<div />' }

const routes = [
  '/',
  '/admin/dashboard',
  '/admin/posts',
  '/admin/tags',
  '/admin/visitors',
  '/admin/comments',
  '/admin/albums',
  '/admin/pages',
  '/admin/seo',
  '/admin/health',
  '/admin/settings'
].map((path) => ({ path, component: Blank }))

/** 抽屉会注册全局 Escape 监听，测试后自动卸载。 */
let pinia: ReturnType<typeof createPinia>
let router: ReturnType<typeof createRouter>

function mountLayout() {
  return mount(AdminLayout, { attachTo: document.body, global: { plugins: [pinia, router] } })
}

describe('AdminLayout', () => {
  beforeEach(async () => {
    vi.stubGlobal('innerWidth', 390)
    pinia = createPinia()
    setActivePinia(pinia)
    router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/admin/dashboard')
    await router.isReady()
  })

  afterEach(() => vi.unstubAllGlobals())

  it('keeps a closed mobile sidebar out of keyboard navigation and names the controls target', async () => {
    const wrapper = mountLayout()
    expect(wrapper.get('.sidebar').attributes('inert')).toBeDefined()
    expect(wrapper.get('.sidebar').attributes('id')).toBe(
      wrapper.get('.menu-button').attributes('aria-controls')
    )
    await wrapper.get('.menu-button').trigger('click')
    expect(wrapper.get('.sidebar').attributes('inert')).toBeUndefined()
    expect(wrapper.get('.sidebar').attributes('aria-modal')).toBe('true')
  })

  it('releases modal focus and scroll ownership when resizing to desktop', async () => {
    const wrapper = mountLayout()
    await wrapper.get('.menu-button').trigger('click')
    expect(document.body.style.overflow).toBe('hidden')
    vi.stubGlobal('innerWidth', 1440)
    window.dispatchEvent(new Event('resize'))
    await nextTick()
    expect(wrapper.get('.menu-button').attributes('aria-expanded')).toBe('false')
    expect(document.body.style.overflow).toBe('')
    expect(wrapper.get('.sidebar').attributes('inert')).toBeUndefined()
    expect(wrapper.get('.sidebar').attributes('aria-modal')).toBeUndefined()
    const logout = wrapper.get<HTMLButtonElement>('.logout-button').element
    logout.focus()
    expect(document.activeElement).toBe(logout)
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

    // 侧栏直接渲染内置图标，不再包一层 .nav-icon 文本容器。
    // 数量必须与 navLinks 对齐，否则新增后台页时会静默漏图标。
    expect(wrapper.findAll('.sidebar-nav .icon')).toHaveLength(
      wrapper.findAll('.sidebar-nav a').length
    )
    expect(wrapper.findAll('.sidebar-nav a').every((node) => node.text().length > 0)).toBe(true)
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

  it('closes the mobile drawer on a programmatic route change', async () => {
    const wrapper = mountLayout()
    await wrapper.get('.menu-button').trigger('click')
    expect(wrapper.get('.sidebar').classes()).toContain('sidebar-open')

    await router.push('/admin/posts')
    await nextTick()
    expect(wrapper.get('.sidebar').classes()).not.toContain('sidebar-open')
  })

  it('closes the drawer with Escape and returns focus to the menu button', async () => {
    const wrapper = mountLayout()
    const trigger = wrapper.get<HTMLButtonElement>('.menu-button')
    trigger.element.focus()
    await trigger.trigger('click')
    await nextTick()

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await nextTick()
    expect(wrapper.get('.sidebar').classes()).not.toContain('sidebar-open')
    expect(document.activeElement).toBe(trigger.element)
    expect(document.body.style.overflow).toBe('')
  })

  it('exposes drawer state, a theme toggle and only real author destinations', async () => {
    const wrapper = mountLayout()
    const trigger = wrapper.get('.menu-button')
    expect(trigger.attributes('aria-expanded')).toBe('false')
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')

    const hrefs = wrapper.findAll('.sidebar-nav a').map((link) => link.attributes('href'))
    expect(hrefs).toEqual([
      '/admin/dashboard',
      '/admin/posts',
      '/admin/tags',
      '/admin/visitors',
      '/admin/comments',
      '/admin/albums',
      '/admin/pages',
      '/admin/seo',
      '/admin/health',
      '/admin/settings'
    ])
    const theme = useThemeStore()
    expect(theme.isDarkMode).toBe(false)
    await wrapper.get('.admin-theme-toggle').trigger('click')
    expect(theme.isDarkMode).toBe(true)
  })
})
