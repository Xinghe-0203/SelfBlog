import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter } from 'axios'
import api from '../index'

/**
 * 请求在进入 dispatch 之前就被拦下时，axios 抛出的错误上没有 config。
 * 这里把 adapter 换成直接抛错的替身，复现「error.config === undefined」这条路径。
 */
const originalAdapter = api.defaults.adapter

afterEach(() => {
  api.defaults.adapter = originalAdapter
  vi.useRealTimers()
})

/** 造一个带 config 的超时错误：拦截器只认 `error.config` 上挂的重试计数。 */
function timeoutAdapter(counter: { calls: number }): AxiosAdapter {
  return (async (config) => {
    counter.calls += 1
    throw Object.assign(new Error('timeout of 130000ms exceeded'), {
      code: 'ECONNABORTED',
      config
    })
  }) as AxiosAdapter
}

/** 拦截器会往错误上挂 `userMessage`；axios 自己挂 `code`。 */
type RejectedRequest = Error & { userMessage?: string; code?: string }

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
    expect((error as RejectedRequest).userMessage).toBe('Network Error')
  })

  it('retries a plain timeout error by default (ECONNABORTED is in RETRYABLE_CODES)', async () => {
    vi.useFakeTimers()
    const counter = { calls: 0 }
    api.defaults.adapter = timeoutAdapter(counter)

    const pending = api.get('/post/list').catch((reason: unknown) => reason)
    // 重试等待是 RETRY_DELAY * attempt（1000ms、2000ms），必须显式推进。
    await vi.advanceTimersByTimeAsync(1000)
    await vi.advanceTimersByTimeAsync(2000)
    const error = (await pending) as RejectedRequest

    // 首次 + MAX_RETRIES(2) 次重试。
    expect(counter.calls).toBe(3)
    expect(error.code).toBe('ECONNABORTED')
    expect(error.userMessage).toBeTruthy()
  })

  it('does not retry when the caller opts out with _noRetry', async () => {
    vi.useFakeTimers()
    const counter = { calls: 0 }
    api.defaults.adapter = timeoutAdapter(counter)

    // LLM 生成用这条路径：超时重试会三倍消耗模型额度，必须一次失败即止。
    const pending = api
      .post('/admin/seo/posts/1/generate', null, { timeout: 130000, _noRetry: true })
      .catch((reason: unknown) => reason)
    await vi.advanceTimersByTimeAsync(5000)
    const error = (await pending) as RejectedRequest

    expect(counter.calls).toBe(1)
    expect(error.code).toBe('ECONNABORTED')
    // 仍然给出面向用户的中文文案，而不是把 axios 原文抛给页面。
    expect(error.userMessage).toBeTruthy()
  })

  it('does not tear down the session when an anonymous request is rejected with 401', async () => {
    // 访客访问公开接口时后端返回 401（例如某个尚未开放的公开路由），
    // 不能因此清掉会话并把他弹到后台登录页 —— 读者只是想看文章。
    api.defaults.adapter = (async (config) => {
      throw Object.assign(new Error('Request failed with status code 401'), {
        response: { status: 401, data: { message: '认证失败，请先登录' } },
        config
      })
    }) as AxiosAdapter

    const error = (await api
      .get('/post/14/related')
      .catch((reason: unknown) => reason)) as RejectedRequest

    expect(error.userMessage).toBe('认证失败，请先登录')
    // 没有触发 handleAuthError：本地存储里的登录痕迹仍然在原处。
    expect(window.location.pathname).not.toContain('/admin')
  })
})
