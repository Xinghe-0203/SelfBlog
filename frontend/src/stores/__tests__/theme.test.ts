import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '../theme'

describe('Theme Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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
