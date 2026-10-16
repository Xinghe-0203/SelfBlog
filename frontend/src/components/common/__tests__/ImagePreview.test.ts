import { describe, it, expect, afterEach } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import ImagePreview from '../ImagePreview.vue'

// 组件在 show 为 true 时会往 document 挂 keydown，卸载才移除；
// 不自动卸载会在用例之间残留监听。
enableAutoUnmount(afterEach)

describe('ImagePreview keyboard handling', () => {
  it('preserves optional photo captions and accessible image descriptions', () => {
    const wrapper = mount(ImagePreview, { props: { images: ['/a.png'], captions: ['沿途的光影'], show: true }, global: { stubs: { teleport: true } } })
    expect(wrapper.get('img').attributes('alt')).toBe('沿途的光影')
    expect(wrapper.get('.preview-caption').text()).toBe('沿途的光影')
  })
  it('does not lock the page for an empty image set', () => {
    mount(ImagePreview, { props: { images: [], show: true } })
    expect(document.body.style.overflow).toBe('')
  })

  it('resets the selected photo when reopened with a new initial index', async () => {
    const wrapper = mount(ImagePreview, { props: { images: ['/a.png', '/b.png'], show: false }, global: { stubs: { teleport: true, transition: true } } })
    await wrapper.setProps({ show: true, initialIndex: 1 })
    expect(wrapper.get('img').attributes('src')).toBe('/b.png')
  })
  it('closes on Escape when it is opened at mount time', () => {
    const wrapper = mount(ImagePreview, { props: { images: ['/a.png'], show: true } })

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('locks and restores the body scroll around its lifetime', () => {
    const wrapper = mount(ImagePreview, { props: { images: ['/a.png'], show: true } })
    expect(document.body.style.overflow).toBe('hidden')

    wrapper.unmount()
    expect(document.body.style.overflow).toBe('')
  })
})
