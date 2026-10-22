import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import Albums from '../Albums.vue'
import { galleryApi } from '@/api/gallery'
import { llmApi } from '@/api/llm'

/**
 * 相册页的 AI 图片描述（E 块）：模型只生成候选，**不写库**。
 * 生成结果有两个显式出口：写照片图注（`album_photo.caption`）或带进相册简介。
 * 两者都必须由作者点击触发。
 */
vi.mock('@/api/gallery', () => ({
  galleryApi: {
    getAdminAlbums: vi.fn(),
    createAlbum: vi.fn(),
    updateAlbum: vi.fn(),
    deleteAlbum: vi.fn(),
    uploadPhotos: vi.fn(),
    deletePhoto: vi.fn(),
    updatePhotoCaption: vi.fn()
  }
}))
vi.mock('@/api/llm', () => ({ llmApi: { imageAlt: vi.fn() } }))
vi.mock('@/utils/toast', () => ({ toast: { success: vi.fn(), warning: vi.fn(), error: vi.fn() } }))
enableAutoUnmount(afterEach)

const response = <T>(data: T) => ({ code: 200, message: 'ok', data })

function albumWithPhotos() {
  return {
    id: 3,
    title: '海边',
    description: '',
    privateAlbum: false,
    locked: false,
    photoCount: 2,
    sortOrder: 0,
    createTime: '2026-10-22 10:00:00',
    photos: [
      { id: 7, imageUrl: '/uploads/albums/a.webp', caption: '', sortOrder: 0 },
      { id: 8, imageUrl: '/uploads/albums/b.webp', caption: '', sortOrder: 1 }
    ]
  }
}

async function setup(albums = [albumWithPhotos()]) {
  vi.mocked(galleryApi.getAdminAlbums).mockResolvedValue(response(albums))
  const wrapper = mount(Albums, {
    global: { stubs: { teleport: true, transition: true } }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(galleryApi.updateAlbum).mockResolvedValue(response(undefined))
  vi.mocked(galleryApi.deletePhoto).mockResolvedValue(response(undefined))
  vi.mocked(galleryApi.updatePhotoCaption).mockResolvedValue(response(undefined))
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('album AI image descriptions', () => {
  it('generates a caption for one photo and applies it only when the author confirms', async () => {
    vi.mocked(llmApi.imageAlt).mockResolvedValue(
      response({
        alt: '海边的礁石与落日',
        caption: '傍晚的海岸线',
        model: 'deepseek-flash',
        latencyMs: 900
      })
    )
    const wrapper = await setup()

    const altButtons = wrapper.findAll('[data-action="ai-photo-description"]')
    expect(altButtons).toHaveLength(2)
    await altButtons[0]?.trigger('click')
    await flushPromises()

    expect(llmApi.imageAlt).toHaveBeenCalledWith({ source: 'album', photoId: 7 })
    // 生成后仍是草稿：没有发任何写库请求，相册简介也没被改。
    expect(galleryApi.updateAlbum).not.toHaveBeenCalled()
    const draft = wrapper.get('[data-alt-photo="7"]')
    expect(draft.get('.alt-draft-text p').text()).toBe('傍晚的海岸线')
    expect(draft.get('.alt-draft-text small').text()).toContain('海边的礁石与落日')

    await draft.get('[data-action="apply-alt-caption"]').trigger('click')
    await flushPromises()

    // 应用只是把候选带进相册编辑表单，保存仍要作者点按钮。
    expect(wrapper.find('#album-form').exists()).toBe(true)
    expect((wrapper.get('.album-dialog textarea').element as HTMLTextAreaElement).value).toBe(
      '傍晚的海岸线'
    )
    expect(galleryApi.updateAlbum).not.toHaveBeenCalled()
    expect(wrapper.find('[data-alt-photo="7"]').exists()).toBe(false)
  })

  it('persists a generated caption as the photo caption through the new write endpoint', async () => {
    vi.mocked(llmApi.imageAlt).mockResolvedValue(
      response({
        alt: '海边的礁石与落日',
        caption: '傍晚的海岸线',
        model: 'deepseek-flash',
        latencyMs: 900
      })
    )
    const wrapper = await setup()

    await wrapper.findAll('[data-action="ai-photo-description"]')[0]?.trigger('click')
    await flushPromises()
    // 生成阶段绝不写库。
    expect(galleryApi.updatePhotoCaption).not.toHaveBeenCalled()

    const draft = wrapper.get('[data-alt-photo="7"]')
    await draft.get('[data-action="save-alt-caption"]').trigger('click')
    await flushPromises()

    expect(galleryApi.updatePhotoCaption).toHaveBeenCalledWith(3, 7, '傍晚的海岸线')
    // 保存成功后草稿消失，列表被重新拉取。
    expect(wrapper.find('[data-alt-photo="7"]').exists()).toBe(false)
    expect(galleryApi.getAdminAlbums).toHaveBeenCalledTimes(2)
  })

  it('falls back to the alt text when the model returns no caption, and reports save failures', async () => {
    vi.mocked(llmApi.imageAlt).mockResolvedValue(
      response({ alt: '窗台上的绿植', caption: '', model: 'deepseek-flash', latencyMs: 700 })
    )
    vi.mocked(galleryApi.updatePhotoCaption).mockRejectedValueOnce({
      userMessage: '照片不存在'
    })
    const wrapper = await setup()
    const { toast } = await import('@/utils/toast')

    await wrapper.findAll('[data-action="ai-photo-description"]')[1]?.trigger('click')
    await flushPromises()
    const draft = wrapper.get('[data-alt-photo="8"]')
    await draft.get('[data-action="save-alt-caption"]').trigger('click')
    await flushPromises()

    expect(galleryApi.updatePhotoCaption).toHaveBeenCalledWith(3, 8, '窗台上的绿植')
    expect(toast.error).toHaveBeenCalledWith('照片不存在')
    // 失败时草稿保留，作者可以重试或改走「应用到相册简介」。
    expect(wrapper.find('[data-alt-photo="8"]').exists()).toBe(true)
  })

  it('disables the album-level button without photos and surfaces backend failures', async () => {
    vi.mocked(llmApi.imageAlt).mockRejectedValue({
      response: {
        data: { message: '当前模型不支持图像输入，请改用 deepseek-flash 等支持视觉的模型' }
      }
    })
    const wrapper = await setup([
      { ...albumWithPhotos(), id: 4, title: '空相册', photoCount: 0, photos: [] }
    ])
    const { toast } = await import('@/utils/toast')

    const albumButton = wrapper.get('[data-action="ai-album-description"]')
    expect(albumButton.attributes('disabled')).toBeDefined()
    await albumButton.trigger('click')
    expect(llmApi.imageAlt).not.toHaveBeenCalled()

    // 有照片的相册：失败时把后端文案原样透出。
    const wrapperWithPhoto = await setup()
    await wrapperWithPhoto.findAll('[data-action="ai-photo-description"]')[0]?.trigger('click')
    await flushPromises()
    expect(toast.error).toHaveBeenCalledWith(
      '当前模型不支持图像输入，请改用 deepseek-flash 等支持视觉的模型'
    )
    expect(wrapperWithPhoto.find('.alt-draft').exists()).toBe(false)
  })
})
