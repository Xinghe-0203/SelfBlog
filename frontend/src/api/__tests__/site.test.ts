import { describe, it, expect, beforeEach, vi } from 'vitest'
import { siteApi } from '../site'

// post 要被 mock 工厂引用，用 vi.hoisted 避免 TDZ（vi.mock 的工厂会提升到 import 之前）。
const { post } = vi.hoisted(() => ({ post: vi.fn() }))

vi.mock('../index', () => ({
  default: { post, get: vi.fn(), put: vi.fn(), delete: vi.fn() }
}))

describe('siteApi.uploadSiteAsset', () => {
  beforeEach(() => {
    post.mockReset()
  })

  it('posts file and kind to the admin asset endpoint', async () => {
    const envelope = {
      code: 200,
      message: 'OK',
      data: {
        url: '/uploads/site-assets/logo/a.png',
        kind: 'logo',
        size: 3,
        originalName: 'a.png'
      }
    }
    post.mockResolvedValue(envelope)

    const file = new File(['abc'], 'a.png', { type: 'image/png' })
    const onProgress = vi.fn()
    const result = await siteApi.uploadSiteAsset('logo', file, onProgress)

    // 拦截器返回的 envelope 原样透传
    expect(result).toBe(envelope)

    const call = post.mock.calls[0]
    expect(call?.[0]).toBe('/admin/site-assets')

    const body = call?.[1] as FormData
    expect(body).toBeInstanceOf(FormData)
    expect(body.get('kind')).toBe('logo')
    expect((body.get('file') as File).name).toBe('a.png')

    const config = call?.[2] as { onUploadProgress?: unknown; timeout?: number }
    expect(config.onUploadProgress).toBe(onProgress)
    expect(config.timeout).toBeGreaterThan(0)
  })
})
