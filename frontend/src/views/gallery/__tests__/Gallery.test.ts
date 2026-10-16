import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, enableAutoUnmount, flushPromises } from '@vue/test-utils'
import Gallery from '../Gallery.vue'
import { galleryApi, type PhotoAlbum } from '@/api/gallery'
import ImagePreview from '@/components/common/ImagePreview.vue'
import { createPinia } from 'pinia'

vi.mock('@/api/gallery', () => ({
  galleryApi: { getAlbums: vi.fn(), getAlbum: vi.fn(), unlock: vi.fn() }
}))
enableAutoUnmount(afterEach)
const album: PhotoAlbum = {
  id: 1,
  title: '一段路途',
  privateAlbum: false,
  locked: false,
  photoCount: 2,
  sortOrder: 0,
  createTime: '2026-10-16',
  photos: [
    { id: 1, imageUrl: '/uploads/one.jpg', caption: '第一张', sortOrder: 0 },
    { id: 2, imageUrl: '/uploads/two.jpg', caption: '第二张', sortOrder: 1 }
  ]
}
const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(galleryApi.getAlbums).mockResolvedValue(response([album]))
  vi.mocked(galleryApi.getAlbum).mockResolvedValue(response(album))
})
async function setup() {
  const wrapper = mount(Gallery, {
    attachTo: document.body,
    global: { plugins: [createPinia()], stubs: { teleport: true, transition: true } }
  })
  await flushPromises()
  return wrapper
}
describe('gallery overlay and access flow', () => {
  it('locks the page, closes on Escape and restores album-button focus', async () => {
    const wrapper = await setup()
    const button = wrapper.get<HTMLButtonElement>('.album-card')
    button.element.focus()
    await button.trigger('click')
    await flushPromises()
    expect(document.body.style.overflow).toBe('hidden')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(document.body.style.overflow).toBe('')
    expect(document.activeElement).toBe(button.element)
  })

  it('uses the shared lightbox and keeps the album open when only the photo closes', async () => {
    const wrapper = await setup()
    await wrapper.get('.album-card').trigger('click')
    await flushPromises()
    await wrapper.get('.photo-tile').trigger('click')
    expect(wrapper.getComponent(ImagePreview).props('show')).toBe(true)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(wrapper.find('.photo-grid').exists()).toBe(true)
    expect(wrapper.find('.preview-overlay').exists()).toBe(false)
    expect(document.body.style.overflow).toBe('hidden')
  })

  it('preserves password-to-token authorization without fetching locked photos early', async () => {
    vi.mocked(galleryApi.getAlbums).mockResolvedValue(
      response([{ ...album, privateAlbum: true, locked: true, photos: undefined }])
    )
    vi.mocked(galleryApi.unlock).mockResolvedValue(response({ accessToken: 'short-lived-token' }))
    const wrapper = await setup()
    await wrapper.get('.album-card').trigger('click')
    expect(galleryApi.getAlbum).not.toHaveBeenCalled()
    await wrapper.get('input[type="password"]').setValue('secret')
    await wrapper.get('.unlock-form').trigger('submit')
    await flushPromises()
    expect(galleryApi.unlock).toHaveBeenCalledWith(1, 'secret')
    expect(galleryApi.getAlbum).toHaveBeenCalledWith(1, 'short-lived-token')
    expect(wrapper.findAll('.photo-tile')).toHaveLength(2)
  })

  it('shows public album detail failures with an actionable retry', async () => {
    vi.mocked(galleryApi.getAlbum).mockRejectedValueOnce(new Error('offline'))
    const wrapper = await setup()
    await wrapper.get('.album-card').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('相册暂时无法加载')
    await wrapper.get('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.photo-tile')).toHaveLength(2)
  })

  it('does not reopen an album when its old detail response arrives after closing', async () => {
    let resolve!: (value: ReturnType<typeof response<PhotoAlbum>>) => void
    vi.mocked(galleryApi.getAlbum).mockReturnValueOnce(
      new Promise((yes) => {
        resolve = yes
      })
    )
    const wrapper = await setup()
    await wrapper.get('.album-card').trigger('click')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    resolve(response(album))
    await flushPromises()
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })
})
