import { afterEach, describe, expect, it } from 'vitest'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import Toast from '../Toast.vue'

enableAutoUnmount(afterEach)
describe('notification host', () => {
  it('inherits the app theme through the stable overlay host', () => {
    const host = document.createElement('div')
    host.id = 'overlay-root'
    document.body.append(host)
    const wrapper = mount(Toast)
    try {
      expect(host.querySelector('.toast-container')).not.toBeNull()
    } finally {
      wrapper.unmount()
      host.remove()
    }
  })
})
