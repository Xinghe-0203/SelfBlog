import { afterEach, describe, expect, it, vi } from 'vitest'
import { STORAGE_KEY_PREFIX } from '@/constants'
// 直接用 Vite 的 ?raw 读取真实 index.html，避免在 jsdom 环境下处理文件系统路径
import html from '../../index.html?raw'

/** 取出 index.html 里唯一的无属性内联脚本（首屏主题引导）。 */
function bootScript(): string {
  const match = html.match(/<script>\s*([\s\S]*?)<\/script>/)
  if (!match?.[1]) throw new Error('index.html 缺少首屏主题引导脚本')
  return match[1]
}

function runBootScript(savedTheme: string | null, prefersDark: boolean): void {
  if (savedTheme === null) localStorage.removeItem(`${STORAGE_KEY_PREFIX}theme`)
  else localStorage.setItem(`${STORAGE_KEY_PREFIX}theme`, savedTheme)
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: prefersDark && query.includes('prefers-color-scheme: dark'),
    media: query,
    onchange: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    addListener: () => {},
    removeListener: () => {},
    dispatchEvent: () => false
  }))
  new Function(bootScript())()
}

/**
 * 深色模式首屏不闪烁的契约：引导脚本必须在 Vue 挂载前就把 dark-mode 加到 <html>，
 * 并且与 stores/theme.ts 的存储键和取值语义完全一致（默认 light）。
 */
describe('index.html 首屏主题引导脚本', () => {
  afterEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark-mode')
    vi.unstubAllGlobals()
  })

  it('使用与 stores/theme.ts 相同的存储键', () => {
    expect(bootScript()).toContain(`${STORAGE_KEY_PREFIX}theme`)
  })

  it('dark 设置在首次绘制前加上 dark-mode', () => {
    runBootScript('dark', false)
    expect(document.documentElement.classList.contains('dark-mode')).toBe(true)
  })

  it('light 与未设置时保持浅色（与 theme.ts 默认值一致）', () => {
    runBootScript('light', true)
    expect(document.documentElement.classList.contains('dark-mode')).toBe(false)
    runBootScript(null, true)
    expect(document.documentElement.classList.contains('dark-mode')).toBe(false)
  })

  it('system 跟随系统偏好', () => {
    runBootScript('system', true)
    expect(document.documentElement.classList.contains('dark-mode')).toBe(true)
    document.documentElement.classList.remove('dark-mode')
    runBootScript('system', false)
    expect(document.documentElement.classList.contains('dark-mode')).toBe(false)
  })

  it('localStorage 抛错时静默保持浅色，不阻塞渲染', () => {
    const getItem = Storage.prototype.getItem
    Storage.prototype.getItem = () => {
      throw new Error('storage blocked')
    }
    try {
      expect(() => new Function(bootScript())()).not.toThrow()
      expect(document.documentElement.classList.contains('dark-mode')).toBe(false)
    } finally {
      Storage.prototype.getItem = getItem
    }
  })
})
