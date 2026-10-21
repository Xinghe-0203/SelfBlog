import { afterEach, describe, expect, it } from 'vitest'
import type { AxiosAdapter } from 'axios'
import api from '../index'

/**
 * 请求在进入 dispatch 之前就被拦下时，axios 抛出的错误上没有 config。
 * 这里把 adapter 换成直接抛错的替身，复现「error.config === undefined」这条路径。
 */
const originalAdapter = api.defaults.adapter

afterEach(() => {
  api.defaults.adapter = originalAdapter
})

describe('api response interceptor', () => {
  it('rejects with the original error when the failure carries no request config', async () => {
    const failure = Object.assign(new Error('Network Error'), { code: 'ERR_NETWORK' })
    api.defaults.adapter = (async () => {
      throw failure
    }) as AxiosAdapter

    const error = await api.get('/post/list').catch((reason: unknown) => reason)

    // 原样抛出：既不能抛 TypeError（读 undefined 的 _networkRetryCount），
    // 也不能丢掉错误本身。
    expect(error).toBe(failure)
    expect((error as Error & { userMessage?: string }).userMessage).toBe('Network Error')
  })
})
