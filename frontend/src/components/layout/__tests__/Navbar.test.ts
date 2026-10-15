import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

import Navbar from '../Navbar.vue'
import { useSiteStore } from '@/stores/site'

/**
 * 这里**不要** mock `@/composables/useScrollState`。
 * vi.mock 的工厂会被提升到 import 之前执行，在工厂里引用测试模块顶层用 ref()
 * 建的变量会直接撞 TDZ 报 "Cannot access 'scrolled' before initialization"。
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

function mountNavbar() {
  return mount(Navbar, {
    global: {
      // RouterLink 用 stub 顶替，但 stub 默认不渲染默认插槽内容，
      // 品牌 logo/站点标题都在插槽里，必须打开才能断言。
      renderStubDefaultSlot: true,
      stubs: { RouterLink: true },
      plugins: [pinia]
    }
  })
}

/** window 上的 scroll 监听是全局的，不卸载会在用例之间叠加。 */
enableAutoUnmount(afterEach)

describe('Navbar', () => {
  beforeEach(() => {
    frameQueue = []
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      frameQueue.push(cb)
      return frameQueue.length
    })
    Object.defineProperty(window, 'scrollY', { writable: true, configurable: true, value: 0 })
    pinia = createPinia()
    setActivePinia(pinia)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('falls back to the built-in logo icon when no logo is uploaded', () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('img.brand-mark--image').exists()).toBe(false)
    expect(wrapper.find('.brand-mark .icon').exists()).toBe(true)
  })

  it('renders the uploaded logo instead of the built-in icon', async () => {
    const store = useSiteStore()
    store.applySettings({ siteLogoUrl: '/uploads/site-assets/logo/a.png' })
    const wrapper = mountNavbar()
    await nextTick()

    const img = wrapper.find('img.brand-mark--image')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/uploads/site-assets/logo/a.png')
  })

  // 首页用 exact-active-class："/" 是所有路径的前缀，用 active-class 会永远高亮。
  it('wires the active class onto the nav links', () => {
    const links = mountNavbar().findAll('.nav-links > *')
    expect(links).toHaveLength(4)
    expect(links[0]?.attributes('exact-active-class')).toBe('active')
    for (const link of links.slice(1)) {
      expect(link.attributes('active-class')).toBe('active')
    }
  })

  it('toggles the scrolled state past the threshold', async () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-scrolled')

    await scrollTo(400)
    expect(wrapper.find('.site-nav-wrap').classes()).toContain('is-scrolled')

    await scrollTo(0)
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-scrolled')
  })

  // 这里只断言状态 class；实际隐藏效果由 CSS 的 @media (max-width: 760px) 决定，
  // jsdom 不跑媒体查询，所以视觉效果只能目视验证。
  it('marks the nav as hidden while scrolling down, and reveals it on the way up', async () => {
    const wrapper = mountNavbar()

    await scrollTo(400)
    expect(wrapper.find('.site-nav-wrap').classes()).toContain('is-hidden')

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
})
