import { describe, it, expect, beforeEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import SiteAssetField from '../SiteAssetField.vue'

const { uploadSiteAsset } = vi.hoisted(() => ({ uploadSiteAsset: vi.fn() }))

vi.mock('@/api/site', () => ({
  siteApi: { uploadSiteAsset }
}))

const UPLOAD_RESPONSE = {
  code: 200,
  message: 'OK',
  data: {
    url: '/uploads/site-assets/logo/mock.png',
    kind: 'logo',
    size: 12,
    originalName: 'logo.png'
  }
}

function mountField(props: Record<string, unknown> = {}) {
  return mount(SiteAssetField, {
    props: { modelValue: '', kind: 'logo', label: '站点 Logo', ...props }
  })
}

/** jsdom 里 file input 的 files 是只读的，直接改属性再派发 change。 */
async function selectFile(wrapper: ReturnType<typeof mountField>, file: File) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', {
    value: { 0: file, length: 1 },
    configurable: true
  })
  await input.trigger('change')
}

/** 让 mock 返回一个只有测试显式释放才 resolve 的 promise，用于测"上传在途"。 */
function makePendingUpload() {
  const control: { release?: () => void } = {}
  uploadSiteAsset.mockImplementation(
    () =>
      new Promise((resolve) => {
        control.release = () => resolve(UPLOAD_RESPONSE)
      })
  )
  return control
}

describe('SiteAssetField', () => {
  beforeEach(() => {
    uploadSiteAsset.mockReset()
  })

  it('maps accept to the kind whitelist', () => {
    expect(mountField({ kind: 'favicon' }).find('input[type="file"]').attributes('accept')).toBe(
      '.png,.ico,.svg'
    )
    expect(mountField({ kind: 'music' }).find('input[type="file"]').attributes('accept')).toBe(
      '.mp3,.ogg,.wav,.m4a,.aac'
    )
  })

  it('rejects extensions outside the whitelist without a request', async () => {
    const wrapper = mountField()
    await selectFile(wrapper, new File(['x'], 'logo.gif', { type: 'image/gif' }))

    expect(wrapper.find('.asset-error').text()).toContain('仅支持')
    expect(uploadSiteAsset).not.toHaveBeenCalled()
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('rejects files over the image cap without a request', async () => {
    const wrapper = mountField()
    const oversized = new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'big.png')
    await selectFile(wrapper, oversized)

    expect(wrapper.find('.asset-error').text()).toContain('2MB')
    expect(uploadSiteAsset).not.toHaveBeenCalled()
  })

  it('uploads a valid file and emits the returned url', async () => {
    uploadSiteAsset.mockResolvedValue(UPLOAD_RESPONSE)
    const wrapper = mountField()
    const file = new File(['png-bytes'], 'logo.png', { type: 'image/png' })

    await selectFile(wrapper, file)
    await flushPromises()

    expect(uploadSiteAsset).toHaveBeenCalledTimes(1)
    const call = uploadSiteAsset.mock.calls[0]
    expect(call?.[0]).toBe('logo')
    expect(call?.[1]).toBe(file)
    expect(typeof call?.[2]).toBe('function')
    expect(wrapper.emitted('update:modelValue')).toEqual([['/uploads/site-assets/logo/mock.png']])
    expect(wrapper.emitted('uploaded')).toEqual([['/uploads/site-assets/logo/mock.png']])
  })

  it('shows the API message inline when the upload fails', async () => {
    uploadSiteAsset.mockRejectedValue(
      Object.assign(new Error('bad'), { userMessage: '文件内容与扩展名不匹配，请上传真实文件' })
    )
    const wrapper = mountField()

    await selectFile(wrapper, new File(['x'], 'logo.png', { type: 'image/png' }))
    await flushPromises()

    expect(wrapper.find('.asset-error').text()).toContain('文件内容与扩展名不匹配')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('emits manual url edits', async () => {
    const wrapper = mountField({ modelValue: '/uploads/site-assets/logo/old.png' })
    await wrapper.find('.asset-input').setValue('https://cdn.example.com/new.png')

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted?.[emitted.length - 1]).toEqual(['https://cdn.example.com/new.png'])
  })

  it('renders a square preview for the current image value', () => {
    expect(mountField().find('.asset-thumb-empty').text()).toBe('未设置')

    const wrapper = mountField({ modelValue: '/uploads/site-assets/logo/current.png' })
    expect(wrapper.find('.asset-thumb-image').attributes('src')).toBe(
      '/uploads/site-assets/logo/current.png'
    )
  })

  it('renders the music filename and duration once metadata is read', async () => {
    const wrapper = mountField({
      kind: 'music',
      modelValue: '/uploads/site-assets/music/song.mp3'
    })
    const audio = wrapper.find('.asset-audio-player')
    expect(audio.attributes('src')).toBe('/uploads/site-assets/music/song.mp3')

    Object.defineProperty(audio.element, 'duration', { value: 204, configurable: true })
    await audio.trigger('loadedmetadata')

    const meta = wrapper.find('.asset-audio-meta').text()
    expect(meta).toContain('song.mp3')
    expect(meta).toContain('3:24')
  })

  it('ignores a second selection while an upload is in flight', async () => {
    const control = makePendingUpload()
    const wrapper = mountField()
    const file = new File(['png'], 'logo.png', { type: 'image/png' })

    await selectFile(wrapper, file)
    await selectFile(wrapper, file)

    expect(uploadSiteAsset).toHaveBeenCalledTimes(1)
    control.release?.()
    await flushPromises()
    expect(wrapper.emitted('update:modelValue')).toHaveLength(1)
  })

  it('locks the url input while an upload is in flight', async () => {
    const control = makePendingUpload()
    const wrapper = mountField()

    await selectFile(wrapper, new File(['png'], 'logo.png'))
    expect(wrapper.find('.asset-input').attributes('disabled')).toBeDefined()

    control.release?.()
    await flushPromises()
    expect(wrapper.find('.asset-input').attributes('disabled')).toBeUndefined()
  })

  it('rejects music over the 20MB cap without a request', async () => {
    const wrapper = mountField({ kind: 'music' })
    const oversized = new File([new Uint8Array(20 * 1024 * 1024 + 1)], 'big.mp3')
    await selectFile(wrapper, oversized)

    expect(wrapper.find('.asset-error').text()).toContain('20MB')
    expect(uploadSiteAsset).not.toHaveBeenCalled()
  })

  it('accepts uppercase extensions', async () => {
    uploadSiteAsset.mockResolvedValue(UPLOAD_RESPONSE)
    const wrapper = mountField()

    await selectFile(wrapper, new File(['x'], 'LOGO.PNG'))
    await flushPromises()

    expect(uploadSiteAsset).toHaveBeenCalledTimes(1)
  })

  it('shows a generic error when the response has no url', async () => {
    uploadSiteAsset.mockResolvedValue({ code: 200, message: 'OK', data: {} })
    const wrapper = mountField()

    await selectFile(wrapper, new File(['x'], 'logo.png'))
    await flushPromises()

    expect(wrapper.find('.asset-error').text()).toContain('上传失败')
    expect(wrapper.emitted('uploaded')).toBeUndefined()
  })

  it('shows the uploaded original name in the music preview', async () => {
    uploadSiteAsset.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: {
        url: '/uploads/site-assets/music/new.mp3',
        kind: 'music',
        size: 5,
        originalName: '我的歌.mp3'
      }
    })
    const wrapper = mountField({ kind: 'music' })

    await selectFile(wrapper, new File(['id3'], 'song.mp3'))
    await flushPromises()
    await wrapper.setProps({ modelValue: '/uploads/site-assets/music/new.mp3' })

    expect(wrapper.find('.asset-audio-meta').text()).toContain('我的歌.mp3')
  })

  it('clears stale messages and duration when the url is edited by hand', async () => {
    const wrapper = mountField({
      kind: 'music',
      modelValue: '/uploads/site-assets/music/song.mp3'
    })
    const audio = wrapper.find('.asset-audio-player')
    Object.defineProperty(audio.element, 'duration', { value: 204, configurable: true })
    await audio.trigger('loadedmetadata')
    expect(wrapper.find('.asset-audio-meta').text()).toContain('3:24')

    await selectFile(wrapper, new File(['x'], 'bad.gif'))
    expect(wrapper.find('.asset-error').exists()).toBe(true)

    await wrapper.find('.asset-input').setValue('https://cdn.example.com/other.mp3')
    expect(wrapper.find('.asset-error').exists()).toBe(false)
    expect(wrapper.find('.asset-audio-meta').text()).not.toContain('3:24')
  })
})
