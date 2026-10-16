import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import Navbar from '../Navbar.vue'
import Icon from '@/components/common/Icon.vue'
import { useSiteStore } from '@/stores/site'
import { useMusicStore } from '@/stores/music'

/**
 * 用真实 memory router，而不是 `stubs: { RouterLink: true }`。
 * stub 会把 router-link 渲染成 `<router-link-stub>` 标签、并且默认吞掉插槽，
 * 于是 active class 只能靠"属性透传"断言 —— 测不到 vue-router 真正算出来的结果。
 * 装真 router 之后：链接是真的 <a>，active class 真的由当前路由决定。
 */
const Blank = { template: '<div />' }

const routes = ['/', '/archives', '/gallery', '/about', '/search'].map((path) => ({
  path,
  component: Blank
}))

/**
 * 这里**不要** mock useScrollState。vi.mock 的工厂会被提升到 import 之前执行，
 * 在工厂里引用测试模块顶层用 ref() 建的变量会直接撞 TDZ。
 * 改用真实 composable + 真实 scroll 事件，顺带把"composable 的输出确实接到了
 * class 上"这层集成也一起测了。
 */
let frameQueue: FrameRequestCallback[] = []

function flushFrame(): void {
  const pending = frameQueue
  frameQueue = []
  for (const cb of pending) cb(0)
}

/** 写 scrollY → 派发 scroll → 跑完一帧 → 等 Vue 刷新。 */
async function scrollTo(y: number): Promise<void> {
  Object.defineProperty(window, 'scrollY', { writable: true, configurable: true, value: y })
  window.dispatchEvent(new Event('scroll'))
  flushFrame()
  await nextTick()
}

/** 组件和测试必须共用同一个 pinia 实例，否则改 store 组件看不到。 */
let pinia: ReturnType<typeof createPinia>
let router: ReturnType<typeof createRouter>

function mountNavbar(realTransition = false) {
  return mount(Navbar, {
    attachTo: document.body,
    global: { plugins: [pinia, router], stubs: { transition: !realTransition } }
  })
}

/** window 上的 scroll 监听是全局的，不卸载会在用例之间叠加。 */
enableAutoUnmount(afterEach)

describe('Navbar', () => {
  beforeEach(async () => {
    frameQueue = []
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      frameQueue.push(cb)
      return frameQueue.length
    })
    Object.defineProperty(window, 'scrollY', { writable: true, configurable: true, value: 0 })
    pinia = createPinia()
    setActivePinia(pinia)
    router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/')
    await router.isReady()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('falls back to the built-in logo icon when no logo is uploaded', () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('img.brand-mark--image').exists()).toBe(false)
    expect(wrapper.find('.brand-mark .icon').exists()).toBe(true)
    expect(wrapper.findComponent(Icon).props('name')).toBe('sparkle')
  })

  it('renders the uploaded logo instead of the built-in icon', async () => {
    useSiteStore().applySettings({ siteLogoUrl: '/uploads/site-assets/logo/a.png' })
    const wrapper = mountNavbar()
    await nextTick()

    const img = wrapper.find('img.brand-mark--image')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/uploads/site-assets/logo/a.png')
  })

  it('renders four real navigation links', () => {
    expect(mountNavbar().findAll('.nav-links > a')).toHaveLength(4)
  })

  // 首页必须用 exact-active-class："/" 是所有路径的前缀，用 active-class 会永远高亮。
  it('marks only 首页 as active on the home route', () => {
    const active = mountNavbar().findAll('.nav-links a.active')
    expect(active).toHaveLength(1)
    expect(active[0]?.text()).toBe('首页')
  })

  it('moves the pill to the link matching the current route', async () => {
    await router.push('/archives')
    await nextTick()

    const active = mountNavbar().findAll('.nav-links a.active')
    expect(active).toHaveLength(1)
    expect(active[0]?.text()).toBe('归档')
  })

  it('applies the active class to the mobile menu links too', async () => {
    await router.push('/gallery')
    await nextTick()

    const wrapper = mountNavbar()
    // 移动端面板平时是 v-if 关着的，先展开。
    await wrapper.find('.menu-toggle').trigger('click')

    const active = wrapper.findAll('.mobile-panel a.active')
    expect(active).toHaveLength(1)
    expect(active[0]?.text()).toBe('相册')
  })

  it('toggles the scrolled state past the threshold', async () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-scrolled')

    await scrollTo(400)
    expect(wrapper.find('.site-nav-wrap').classes()).toContain('is-scrolled')

    await scrollTo(0)
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-scrolled')
  })

  it('keeps navigation visible in both scroll directions', async () => {
    const wrapper = mountNavbar()

    await scrollTo(400)
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-hidden')

    await scrollTo(200)
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-hidden')
  })

  it('never hides the nav before the page is scrolled', async () => {
    const wrapper = mountNavbar()
    // 未过阈值：scrolled 为 false，方向不该把它带进隐藏态。
    await scrollTo(8)
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-hidden')
  })

  it('uses the built-in icon set instead of text glyphs', () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('.nav-search .icon').exists()).toBe(true)
    expect(wrapper.find('.theme-button .icon').exists()).toBe(true)
    expect(wrapper.find('.menu-toggle .icon').exists()).toBe(true)
    expect(wrapper.find('.theme-button').text()).toBe('')
  })

  it('closes the menu on a programmatic route change', async () => {
    const wrapper = mountNavbar()
    await wrapper.get('.menu-toggle').trigger('click')
    await router.push('/archives')
    await nextTick()
    expect(wrapper.get('.menu-toggle').attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('.mobile-panel').exists()).toBe(false)
  })

  it('submits a trimmed query but does not navigate for whitespace', async () => {
    const wrapper = mountNavbar()
    await wrapper.get('.nav-search input').setValue('   ')
    await wrapper.get('.nav-search').trigger('submit')
    expect(router.currentRoute.value.path).toBe('/')
    await wrapper.get('.nav-search input').setValue('  Vue  ')
    await wrapper.get('.nav-search').trigger('submit')
    await router.isReady()
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(router.currentRoute.value.fullPath).toBe('/search?q=Vue')
  })

  it('keeps search, music and menu panels mutually exclusive', async () => {
    useMusicStore().configure({ enabled: true, url: '/uploads/song.mp3', title: 'Song' })
    const wrapper = mountNavbar()
    await wrapper.get('.menu-toggle').trigger('click')
    await wrapper.get('.music-trigger').trigger('click')
    expect(wrapper.find('.mobile-panel').exists()).toBe(false)
    expect(wrapper.find('.music-panel').exists()).toBe(true)
    await wrapper.get('.search-toggle').trigger('click')
    expect(wrapper.find('.music-panel').exists()).toBe(false)
    expect(wrapper.find('.search-panel').exists()).toBe(true)
  })

  it('focuses the arriving search input even while a music panel is leaving', async () => {
    useMusicStore().configure({ enabled: true, url: '/uploads/song.mp3', title: 'Song' })
    const wrapper = mountNavbar(true)
    await wrapper.get('.music-trigger').trigger('click')
    await wrapper.get('.search-toggle').trigger('click')
    expect(document.activeElement).toBe(wrapper.get('.search-panel input').element)
  })

  it('closes on Escape and restores the opening button focus', async () => {
    const wrapper = mountNavbar()
    const trigger = wrapper.get<HTMLButtonElement>('.menu-toggle')
    trigger.element.focus()
    await trigger.trigger('click')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await nextTick()
    expect(wrapper.find('.mobile-panel').exists()).toBe(false)
    expect(document.activeElement).toBe(trigger.element)
  })

  it('closes on outside pointer input or a modal opening', async () => {
    const wrapper = mountNavbar()
    await wrapper.get('.menu-toggle').trigger('click')
    document.body.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await nextTick()
    expect(wrapper.find('.mobile-panel').exists()).toBe(false)
    await wrapper.get('.menu-toggle').trigger('click')
    document.dispatchEvent(new Event('selfblog:dialog-open'))
    await nextTick()
    expect(wrapper.find('.mobile-panel').exists()).toBe(false)
  })
})
