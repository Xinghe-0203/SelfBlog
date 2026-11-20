import { afterEach, describe, expect, it, vi } from 'vitest'
import { cachedGet, clearApiCache, invalidateApiCache } from '../apiCache'

afterEach(() => {
  clearApiCache()
  vi.restoreAllMocks()
})

describe('cachedGet', () => {
  it('serves the second read from memory without calling the fetcher again', async () => {
    const fetcher = vi.fn().mockResolvedValue({ value: 1 })

    const first = await cachedGet('post:list|1', 1000, fetcher)
    const second = await cachedGet('post:list|1', 1000, fetcher)

    expect(first).toEqual({ value: 1 })
    expect(second).toEqual({ value: 1 })
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('refetches once the entry expired', async () => {
    vi.useFakeTimers()
    const fetcher = vi.fn().mockResolvedValue('x')

    await cachedGet('post:archives', 1000, fetcher)
    vi.advanceTimersByTime(1001)
    await cachedGet('post:archives', 1000, fetcher)

    expect(fetcher).toHaveBeenCalledTimes(2)
    vi.useRealTimers()
  })

  it('dedupes concurrent reads of the same key into one request', async () => {
    let resolve: (value: string) => void = () => {}
    const fetcher = vi.fn().mockImplementation(
      () =>
        new Promise<string>((r) => {
          resolve = r
        })
    )

    const a = cachedGet('tag:list', 1000, fetcher)
    const b = cachedGet('tag:list', 1000, fetcher)
    resolve('done')

    expect(await a).toBe('done')
    expect(await b).toBe('done')
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('does not cache a failed request', async () => {
    const fetcher = vi
      .fn()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce('recovered')

    await expect(cachedGet('post:list|err', 1000, fetcher)).rejects.toThrow('offline')
    await expect(cachedGet('post:list|err', 1000, fetcher)).resolves.toBe('recovered')
    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('invalidates entries by key prefix', async () => {
    const fetcher = vi.fn().mockResolvedValue('v')

    await cachedGet('post:list|1', 1000, fetcher)
    await cachedGet('post:archives', 1000, fetcher)
    invalidateApiCache('post:')

    await cachedGet('post:list|1', 1000, fetcher)
    await cachedGet('post:archives', 1000, fetcher)
    expect(fetcher).toHaveBeenCalledTimes(4)
  })
})
