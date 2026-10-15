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
    expect(wrapper.emitted('update:modelValue')).toEqual([
      ['/uploads/site-assets/logo/mock.png']
    ])
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
})
