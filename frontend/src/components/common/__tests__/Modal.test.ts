import { afterEach, describe, expect, it } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import { nextTick } from 'vue'
import Modal from '../Modal.vue'
import ImagePreview from '../ImagePreview.vue'

enableAutoUnmount(afterEach)

describe('shared modal integration', () => {
  it('keeps the underlying modal open and locked when a lightbox closes', async () => {
    const modal = mount(Modal, { props: { show: true, closable: true, title: 'Album' } })
    const preview = mount(ImagePreview, { props: { show: true, images: ['/a.jpg'] } })
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(preview.emitted('close')).toHaveLength(1)
    expect(modal.emitted('close')).toBeUndefined()
    preview.unmount()
    await nextTick()
    expect(document.body.style.overflow).toBe('hidden')
  })

  it('portals into the stable app host with an accessible name', () => {
    const host = document.createElement('div')
    host.id = 'overlay-root'
    document.body.append(host)
    const modal = mount(Modal, { props: { show: true, title: 'Site identity', closable: true } })
    try {
      expect(host.querySelector('[role="dialog"]')?.getAttribute('aria-label')).toBe(
        'Site identity'
      )
    } finally {
      modal.unmount()
      host.remove()
    }
  })
})
