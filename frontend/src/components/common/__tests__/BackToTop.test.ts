import { afterEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import BackToTop from '../BackToTop.vue'

enableAutoUnmount(afterEach)
afterEach(() => vi.restoreAllMocks())
describe('back to top motion', () => {
  it('does not request smooth scrolling when reduced motion is enabled', async () => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList)
    const scroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    const wrapper = mount(BackToTop)
    await wrapper.get('button').trigger('click')
    expect(scroll).toHaveBeenCalledWith({ top: 0, behavior: 'auto' })
  })
})
