import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const PREFERENCES_KEY = 'selfblog-music-preferences'
type MusicConfiguration = { enabled: boolean; url: string; title: string }

export const useMusicStore = defineStore('music', () => {
  const source = ref('')
  const title = ref('背景音乐')
  const playing = ref(false)
  const loading = ref(false)
  const error = ref('')
  const currentTime = ref(0)
  const duration = ref(0)
  const volume = ref(.65)
  const loop = ref(true)
  const volumeSupported = ref(true)
  const available = computed(() => !!source.value)
  let audio: HTMLAudioElement | null = null
  let cleanup: (() => void) | null = null
  let suspended = false
  let requested = false
  let revision = 0
  let pending: Promise<void> | null = null

  try {
    const saved: unknown = JSON.parse(localStorage.getItem(PREFERENCES_KEY) || 'null')
    if (saved && typeof saved === 'object') {
      const preferences = saved as Record<string, unknown>
      if (typeof preferences.volume === 'number' && Number.isFinite(preferences.volume)) {
        volume.value = Math.max(0, Math.min(1, preferences.volume))
      }
      if (typeof preferences.loop === 'boolean') loop.value = preferences.loop
    }
  } catch { /* Preferences must never prevent reading or playback. */ }

  function persist(): void {
    try { localStorage.setItem(PREFERENCES_KEY, JSON.stringify({ volume: volume.value, loop: loop.value })) }
    catch { /* Private browsing and blocked storage are supported. */ }
  }

  function pause(): void {
    revision++
    requested = false
    playing.value = false
    loading.value = false
    audio?.pause()
  }

  function configure(configuration: MusicConfiguration): void {
    const url = configuration.url.trim()
    const next = configuration.enabled && /^(https?:\/\/|\/uploads\/)/i.test(url) ? url : ''
    title.value = configuration.title.trim() || '背景音乐'
    if (next === source.value) return
    attach(null)
    source.value = next
    currentTime.value = 0
    duration.value = 0
    error.value = ''
  }

  function applyVolume(element: HTMLAudioElement): void {
    const target = volume.value
    try {
      element.volume = target
      volumeSupported.value = Math.abs(element.volume - target) < .01
    } catch { volumeSupported.value = false }
  }

  function attach(element: HTMLAudioElement | null): void {
    if (audio === element) return
    pause()
    cleanup?.()
    cleanup = null
    pending = null
    audio = element
    if (!element) return
    element.preload = 'none'
    element.loop = loop.value
    applyVolume(element)
    const active = () => audio === element && available.value && !suspended
    const listeners: Array<[string, EventListener]> = []
    const listen = (name: string, handler: () => void) => {
      const guarded = () => { if (audio === element) handler() }
      element.addEventListener(name, guarded)
      listeners.push([name, guarded])
    }
    listen('playing', () => {
      if (!active() || !requested) { element.pause(); return }
      playing.value = true
      loading.value = false
      error.value = ''
    })
    listen('pause', () => { playing.value = false; loading.value = false })
    listen('ended', () => { requested = false; playing.value = false; loading.value = false })
    listen('waiting', () => { if (requested) loading.value = true })
    const metadata = () => {
      duration.value = Number.isFinite(element.duration) && element.duration > 0 ? element.duration : 0
    }
    listen('loadedmetadata', metadata)
    listen('durationchange', metadata)
    listen('timeupdate', () => {
      currentTime.value = Number.isFinite(element.currentTime) ? Math.max(0, element.currentTime) : 0
    })
    listen('error', () => {
      pause()
      error.value = '音乐暂时无法播放，请检查音频地址后重试。'
    })
    cleanup = () => listeners.forEach(([name, handler]) => element.removeEventListener(name, handler))
  }

  function startPlay(): Promise<void> {
    const element = audio
    if (!element || !available.value || suspended) return Promise.resolve()
    const attempt = ++revision
    requested = true
    loading.value = true
    const retry = !!error.value
    error.value = ''
    const operation = (async () => {
      try {
        if (retry) element.load()
        await element.play()
        // A stale continuation can stop only its captured element, never a new source.
        if (attempt !== revision || audio !== element || !requested || suspended || !available.value) {
          element.pause()
        }
      } catch (reason) {
        if (attempt !== revision || audio !== element) return
        requested = false
        playing.value = false
        // DOMException can come from another browser realm and fail instanceof Error.
        const name = reason && typeof reason === 'object' && 'name' in reason ? reason.name : ''
        error.value = name === 'NotAllowedError'
          ? '浏览器限制了播放，请点击播放按钮重试。'
          : '音乐暂时无法播放，请检查音频地址后重试。'
      } finally {
        if (attempt === revision && audio === element) loading.value = false
      }
    })()
    pending = operation
    void operation.finally(() => { if (pending === operation) pending = null })
    return operation
  }

  async function play(): Promise<void> {
    if (!available.value || suspended || !audio) return
    if (pending) {
      if (requested) return pending
      // Serialize a fresh play after pause with the old request. Safari also needs
      // the original media.play() call to remain in the initial user gesture.
      const element = audio
      const attempt = ++revision
      requested = true
      loading.value = true
      await pending
      if (attempt !== revision || audio !== element || suspended || !requested) return
    }
    return startPlay()
  }

  function setSuspended(value: boolean): void {
    if (suspended === value) return
    suspended = value
    if (value) pause()
  }

  function seek(value: number): void {
    if (!audio || !Number.isFinite(value) || duration.value <= 0) return
    try {
      audio.currentTime = Math.max(0, Math.min(duration.value, value))
      currentTime.value = audio.currentTime
    } catch { /* A media source may become unseekable while buffering. */ }
  }

  function setVolume(value: number): void {
    if (!Number.isFinite(value)) return
    volume.value = Math.max(0, Math.min(1, value))
    if (audio) applyVolume(audio)
    persist()
  }

  function setLoop(value: boolean): void {
    loop.value = value
    if (audio) audio.loop = value
    persist()
  }

  return { source, title, available, playing, loading, error, currentTime, duration,
    volume, loop, volumeSupported, configure, attach, play, pause, setSuspended, seek, setVolume, setLoop }
})
