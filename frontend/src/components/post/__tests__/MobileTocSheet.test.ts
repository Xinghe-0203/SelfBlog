import { afterEach, describe, it, expect } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import { nextTick } from 'vue'
import MobileTocSheet from '../MobileTocSheet.vue'

enableAutoUnmount(afterEach)

const items = [
  { id: 'section-0', text: '第一节', level: 1 },
  { id: 'section-1', text: '小节', level: 2 },
  { id: 'section-2', text: '更深的小节', level: 3 }
]

function mountSheet(activeId = '') {
  return mount(MobileTocSheet, {
    props: { items, activeId },
    global: { stubs: { teleport: true, transition: true } }
  })
}

describe('MobileTocSheet', () => {
  it('moves focus inside a sheet opened from its initially closed trigger', async () => {
    const wrapper = mount(MobileTocSheet, {
      props: { items },
      attachTo: document.body,
      global: { stubs: { teleport: true } }
    })
    await wrapper.get('.toc-fab').trigger('click')
    await nextTick()
    expect(document.activeElement).toBe(wrapper.get('.toc-close').element)
  })
  it('locks scrolling and closes with Escape', async () => {
    const wrapper = mountSheet()
    await wrapper.get('.toc-fab').trigger('click')
    expect(document.body.style.overflow).toBe('hidden')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await nextTick()
    expect(wrapper.find('.toc-panel').exists()).toBe(false)
    expect(document.body.style.overflow).toBe('')
  })

  it('closes and releases scrolling when resized to a desktop directory', async () => {
    const wrapper = mountSheet()
    await wrapper.get('.toc-fab').trigger('click')
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1200 })
    window.dispatchEvent(new Event('resize'))
    await nextTick()
    expect(wrapper.find('.toc-panel').exists()).toBe(false)
    expect(document.body.style.overflow).toBe('')
  })
  it('renders one button per item with level classes and the active class', async () => {
    const wrapper = mountSheet('section-1')
    await wrapper.find('.toc-fab').trigger('click')

    const buttons = wrapper.findAll('.toc-nav button')
    expect(buttons).toHaveLength(3)
    expect(buttons[0]?.classes()).toContain('level-1')
    expect(buttons[1]?.classes()).toContain('level-2')
    expect(buttons[2]?.classes()).toContain('level-3')
    expect(buttons[1]?.classes()).toContain('active')
    expect(buttons[0]?.classes()).not.toContain('active')
    expect(buttons[2]?.text()).toBe('更深的小节')
  })

  it('emits select with the item and closes the sheet', async () => {
    const wrapper = mountSheet()
    await wrapper.find('.toc-fab').trigger('click')
    await wrapper.findAll('.toc-nav button')[2]?.trigger('click')

    expect(wrapper.emitted('select')?.[0]).toEqual([items[2]])
    expect(wrapper.find('.toc-panel').exists()).toBe(false)
  })

  it('opens the drawer from the floating capsule button', async () => {
    const wrapper = mountSheet()
    expect(wrapper.find('.toc-panel').exists()).toBe(false)
    expect(wrapper.find('.toc-backdrop').exists()).toBe(false)

    const fab = wrapper.find('.toc-fab')
    expect(fab.attributes('aria-label')).toBe('打开文章目录')
    expect(fab.text()).toContain('目录')
    expect(fab.find('.icon').exists()).toBe(true)

    await fab.trigger('click')

    const panel = wrapper.find('.toc-panel')
    expect(panel.exists()).toBe(true)
    expect(panel.attributes('role')).toBe('dialog')
    expect(panel.attributes('aria-modal')).toBe('true')
    expect(panel.attributes('aria-label')).toBe('文章目录')
    expect(panel.text()).toContain('文章目录')
  })

  it('closes the drawer from the backdrop', async () => {
    const wrapper = mountSheet()
    await wrapper.find('.toc-fab').trigger('click')
    expect(wrapper.find('.toc-backdrop').exists()).toBe(true)

    await wrapper.find('.toc-backdrop').trigger('click')
    expect(wrapper.find('.toc-panel').exists()).toBe(false)
  })

  it('closes the drawer from the close button', async () => {
    const wrapper = mountSheet()
    await wrapper.find('.toc-fab').trigger('click')

    const close = wrapper.find('.toc-close')
    expect(close.find('.icon').exists()).toBe(true)
    await close.trigger('click')
    expect(wrapper.find('.toc-panel').exists()).toBe(false)
  })

  // 宽屏隐藏（@media min-width: 861px）由 CSS 决定，jsdom 不跑媒体查询，只能目视验证。
})
