import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import PagePreviewContent from '../PagePreviewContent.vue'
import { postApi } from '@/api/post'
vi.mock('@/api/post', () => ({ postApi: { getPostList: vi.fn() } }))
enableAutoUnmount(afterEach)
beforeEach(() => vi.resetAllMocks())
describe('preview recovery', () => {
  it('keeps the failed-data retry outside the inert read-only page content', async () => {
    vi.mocked(postApi.getPostList).mockRejectedValue(new Error('offline'))
    const wrapper = mount(PagePreviewContent, {
      props: { id: 'post', config: {} },
      global: { plugins: [createPinia()] }
    })
    await flushPromises()
    const retry = wrapper.get('button')
    expect(retry.element.closest('[inert]')).toBeNull()
    await retry.trigger('click')
    await flushPromises()
    expect(postApi.getPostList).toHaveBeenCalledTimes(2)
  })
})
