import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMusicStore } from '../music'

function media() {
  const audio = document.createElement('audio')
  Object.defineProperties(audio, {
    play: { value: vi.fn().mockResolvedValue(undefined), configurable: true },
    pause: { value: vi.fn(), configurable: true },
    load: { value: vi.fn(), configurable: true },
    duration: { value: 180, configurable: true },
  })
  return audio
}

function deferred() {
  let resolve!: () => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<void>((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

describe('music transport', () => {
  beforeEach(() => { localStorage.clear(); setActivePinia(createPinia()) })
  afterEach(() => vi.restoreAllMocks())

  function ready() {
    const store = useMusicStore()
    store.configure({ enabled: true, url: '/uploads/song.mp3', title: 'My song' })
    const audio = media()
    store.attach(audio)
    return { store, audio }
  }

  it('does not autoplay when settings and an audio host become available', () => {
    const { store, audio } = ready()
    expect(store.available).toBe(true)
    expect(store.title).toBe('My song')
    expect(audio.play).not.toHaveBeenCalled()
    expect(store.playing).toBe(false)
    expect(audio.preload).toBe('none')
  })

  it('reflects media events rather than optimistic clicks', async () => {
    const { store, audio } = ready()
    await store.play()
    expect(store.playing).toBe(false)
    audio.dispatchEvent(new Event('playing'))
    expect(store.playing).toBe(true)
    expect(store.loading).toBe(false)
    audio.dispatchEvent(new Event('pause'))
    expect(store.playing).toBe(false)
  })

  it('uses finite metadata and clamps seeks to the track', () => {
    const { store, audio } = ready()
    store.seek(80)
    expect(audio.currentTime).toBe(0)
    audio.dispatchEvent(new Event('loadedmetadata'))
    expect(store.duration).toBe(180)
    store.seek(400)
    expect(audio.currentTime).toBe(180)
    store.seek(-5)
    expect(audio.currentTime).toBe(0)
    store.seek(Number.NaN)
    expect(audio.currentTime).toBe(0)
    Object.defineProperty(audio, 'duration', { value: Infinity })
    audio.dispatchEvent(new Event('durationchange'))
    expect(store.duration).toBe(0)
  })

  it('updates time from media and resets playback when a track ends', () => {
    const { store, audio } = ready()
    audio.currentTime = 27
    audio.dispatchEvent(new Event('timeupdate'))
    expect(store.currentTime).toBe(27)
    audio.dispatchEvent(new Event('playing'))
    audio.dispatchEvent(new Event('ended'))
    expect(store.playing).toBe(false)
  })

  it('handles browser rejection without claiming playback started', async () => {
    const { store, audio } = ready()
    vi.mocked(audio.play).mockRejectedValueOnce(new DOMException('Not allowed', 'NotAllowedError'))
    await store.play()
    expect(store.playing).toBe(false)
    expect(store.loading).toBe(false)
    expect(store.error).toContain('点击')
  })

  it('reports media errors inline and can retry explicitly', async () => {
    const { store, audio } = ready()
    audio.dispatchEvent(new Event('error'))
    expect(store.error).toContain('播放')
    await store.play()
    expect(audio.load).toHaveBeenCalled()
    expect(audio.play).toHaveBeenCalledTimes(1)
  })

  it('pauses and resets when disabled without resuming on re-enable', () => {
    const { store, audio } = ready()
    audio.dispatchEvent(new Event('playing'))
    store.configure({ enabled: false, url: '/uploads/song.mp3', title: 'My song' })
    expect(audio.pause).toHaveBeenCalled()
    expect(store.available).toBe(false)
    expect(store.playing).toBe(false)
    expect(store.currentTime).toBe(0)
    store.configure({ enabled: true, url: '/uploads/song.mp3', title: 'My song' })
    expect(audio.play).not.toHaveBeenCalled()
  })

  it('rejects unsafe sources even when settings are malformed', () => {
    const store = useMusicStore()
    store.configure({ enabled: true, url: 'javascript:alert(1)', title: 'Invalid' })
    expect(store.available).toBe(false)
  })

  it('ignores old-source events and late play resolution after replacement', async () => {
    const { store, audio: old } = ready()
    const pending = deferred()
    vi.mocked(old.play).mockReturnValueOnce(pending.promise)
    const attempt = store.play()
    store.configure({ enabled: true, url: '/uploads/new.mp3', title: 'New song' })
    const current = media()
    store.attach(current)
    await store.play()
    current.dispatchEvent(new Event('playing'))
    old.dispatchEvent(new Event('error'))
    pending.resolve()
    await attempt
    expect(store.title).toBe('New song')
    expect(store.playing).toBe(true)
    expect(store.error).toBe('')
    expect(current.pause).not.toHaveBeenCalled()
    expect(old.pause).toHaveBeenCalled()
  })

  it('cancels a pending play when paused and never starts twice concurrently', async () => {
    const { store, audio } = ready()
    const pending = deferred()
    vi.mocked(audio.play).mockReturnValueOnce(pending.promise)
    const attempt = store.play()
    void store.play()
    expect(audio.play).toHaveBeenCalledTimes(1)
    store.pause()
    pending.resolve()
    await attempt
    expect(store.playing).toBe(false)
    expect(store.loading).toBe(false)
    expect(audio.pause).toHaveBeenCalled()
  })

  it('serializes play-pause-play so stale work cannot pause a fresh playback', async () => {
    const { store, audio } = ready()
    const pending = deferred()
    vi.mocked(audio.play).mockReturnValueOnce(pending.promise)
    const first = store.play()
    store.pause()
    const second = store.play()
    pending.resolve()
    await Promise.all([first, second])
    expect(audio.play).toHaveBeenCalledTimes(2)
    audio.dispatchEvent(new Event('playing'))
    expect(store.playing).toBe(true)
  })

  it('exposes queued replay as cancelable loading and respects a second cancel', async () => {
    const { store, audio } = ready()
    const pending = deferred()
    vi.mocked(audio.play).mockReturnValueOnce(pending.promise)
    const first = store.play()
    store.pause()
    const queued = store.play()
    expect(store.loading).toBe(true)
    store.pause()
    pending.resolve()
    await Promise.all([first, queued])
    expect(audio.play).toHaveBeenCalledTimes(1)
    expect(store.playing).toBe(false)
  })

  it('pauses in admin areas and does not automatically resume on return', async () => {
    const { store, audio } = ready()
    audio.dispatchEvent(new Event('playing'))
    store.setSuspended(true)
    await store.play()
    expect(audio.play).not.toHaveBeenCalled()
    expect(store.playing).toBe(false)
    store.setSuspended(false)
    expect(audio.play).not.toHaveBeenCalled()
  })

  it('cleans up listeners on detach', () => {
    const { store, audio } = ready()
    store.attach(null)
    audio.dispatchEvent(new Event('playing'))
    expect(store.playing).toBe(false)
  })

  it('restores only preferences, never playback intent', () => {
    const { store, audio } = ready()
    store.setVolume(.3)
    store.setLoop(false)
    expect(audio.volume).toBe(.3)
    expect(audio.loop).toBe(false)
    setActivePinia(createPinia())
    const fresh = useMusicStore()
    expect(fresh.volume).toBe(.3)
    expect(fresh.loop).toBe(false)
    expect(fresh.playing).toBe(false)
  })

  it('tolerates malformed and unavailable storage', () => {
    localStorage.setItem('selfblog-music-preferences', 'broken-json')
    expect(useMusicStore().volume).toBe(.65)
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Blocked') })
    expect(() => useMusicStore().setVolume(.2)).not.toThrow()
  })

  it('does not show a fake working volume control on unsupported platforms', () => {
    const audio = media()
    Object.defineProperty(audio, 'volume', { get: () => 1, set: () => {}, configurable: true })
    const store = useMusicStore()
    store.configure({ enabled: true, url: '/uploads/song.mp3', title: 'Song' })
    store.attach(audio)
    expect(store.volumeSupported).toBe(false)
  })
})
