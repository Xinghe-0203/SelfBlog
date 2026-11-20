import { afterEach, describe, expect, it, vi } from 'vitest'
import { chunkForPath, prefetchPath, prefetchPublicRoutes } from '../prefetch'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('chunkForPath', () => {
  it('maps the public routes to their chunk', () => {
    expect(chunkForPath('/')).not.toBeNull()
    expect(chunkForPath('/archives')).not.toBeNull()
    expect(chunkForPath('/about')).not.toBeNull()
    expect(chunkForPath('/gallery')).not.toBeNull()
    expect(chunkForPath('/search')).not.toBeNull()
    expect(chunkForPath('/post/12')).not.toBeNull()
    expect(chunkForPath('/tag/3')).not.toBeNull()
  })

  it('ignores the query string and the hash', () => {
    expect(chunkForPath('/post/12?from=home')).not.toBeNull()
    expect(chunkForPath('/archives#2026')).not.toBeNull()
  })

  it('does not match the admin area or unknown paths', () => {
    // 后台 chunk 绝不能被提前下载：那是只有登录后才该拿到的代码。
    expect(chunkForPath('/admin/dashboard')).toBeNull()
    expect(chunkForPath('/admin/settings')).toBeNull()
    expect(chunkForPath('/definitely-not-a-page')).toBeNull()
  })
})

describe('prefetchPath', () => {
  it('never throws for an unknown path', () => {
    expect(() => prefetchPath('/admin/dashboard')).not.toThrow()
    expect(() => prefetchPath('/post/1')).not.toThrow()
  })
})

describe('prefetchPublicRoutes', () => {
  it('is safe to call outside a browser', () => {
    // 在没有 window 的环境（SSR / 纯 node 测试）里必须直接返回，而不是抛错。
    expect(() => prefetchPublicRoutes()).not.toThrow()
  })
})
