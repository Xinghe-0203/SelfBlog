import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '../theme'

describe('Theme Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })
  afterEach(() => {
    useThemeStore().destroyTheme()
    document.documentElement.classList.remove('dark-mode', 'theme-changing')
    vi.unstubAllGlobals()
  })

  it('changes foreground and background atomically, then restores ordinary motion', () => {
    const frames: FrameRequestCallback[] = []
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
      frames.push(callback)
      return frames.length
    })
    const store = useThemeStore()
    store.setTheme('dark')
    expect(document.documentElement.classList.contains('theme-changing')).toBe(true)
    expect(document.documentElement.classList.contains('dark-mode')).toBe(true)
    frames.shift()?.(0)
    frames.shift()?.(0)
    expect(document.documentElement.classList.contains('theme-changing')).toBe(false)
  })

  it('should initialize with default theme', () => {
    const store = useThemeStore()
    expect(store.theme).toBeDefined()
    expect(['light', 'dark', 'system']).toContain(store.theme)
  })

  it('should toggle theme', () => {
    const store = useThemeStore()
    store.initTheme()
    const initial = store.theme
    store.toggleTheme()
    expect(store.theme).not.toBe(initial)
  })
})
