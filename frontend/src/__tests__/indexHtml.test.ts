import { afterEach, describe, expect, it, vi } from 'vitest'
import { STORAGE_KEY_PREFIX } from '@/constants'
import { APPEARANCE_STORAGE_KEY, PREPAINT_APPEARANCE_VARS } from '@/stores/site'
// 直接用 Vite 的 ?raw 读取真实 index.html，避免在 jsdom 环境下处理文件系统路径
import html from '../../index.html?raw'

/** 按出现顺序取出 index.html 里所有无属性内联脚本（带属性的模块脚本不匹配）。 */
function inlineScripts(): string[] {
  return [...html.matchAll(/<script>\s*([\s\S]*?)<\/script>/g)].map((match) => match[1] ?? '')
}

/** 取出 index.html 里唯一的无属性内联脚本（首屏主题引导）。 */
function bootScript(): string {
  const match = html.match(/<script>\s*([\s\S]*?)<\/script>/)
  if (!match?.[1]) throw new Error('index.html 缺少首屏主题引导脚本')
  return match[1]
}

/**
 * 取出首屏外观引导脚本（主题脚本之后的第二个内联脚本）。
 * 用下标而不是内容匹配：脚本必须排在主题脚本之后、样式表之前，
 * 顺序错了要直接报错，而不是靠筛选把自己筛出来。
 */
function appearanceScript(): string {
  const script = inlineScripts()[1]
  if (!script) throw new Error('index.html 缺少首屏外观引导脚本')
  return script
}

/** 与 runBootScript 相同的执行方式：写入 localStorage 后用 new Function 跑真实脚本。 */
function runAppearanceScript(savedAppearance: string | null): void {
  if (savedAppearance === null) localStorage.removeItem(`${STORAGE_KEY_PREFIX}site_appearance`)
  else localStorage.setItem(`${STORAGE_KEY_PREFIX}site_appearance`, savedAppearance)
  new Function(appearanceScript())()
}

/** 读取 <html> 上的自定义属性值（未写入时 jsdom 返回空字符串）。 */
function rootVar(name: string): string {
  return document.documentElement.style.getPropertyValue(name)
}

const APPEARANCE_KEYS = [
  '--ap-bg-color',
  '--ap-bg-image',
  '--ap-bg-attachment',
  '--ap-bg-size',
  '--ap-bg-repeat'
] as const

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

/**
 * 站点外观首屏不闪烁的契约：作者配置的页面背景图 / 背景色必须在首次绘制前
 * 就写到 <html> 上（#selfblog 那时还不存在，只有 documentElement 可写），
 * 而首屏拿不到 /api/site/public，所以只能读 localStorage 里 applyAppearance()
 * 写好的最终 CSS 值。脚本是零逻辑转发：只按白名单原样 setProperty。
 */
describe('index.html 首屏外观引导脚本', () => {
  afterEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('style')
    document.documentElement.removeAttribute('data-wallpaper')
    vi.unstubAllGlobals()
  })

  it('使用与 STORAGE_KEY_PREFIX 一致的存储键', () => {
    expect(appearanceScript()).toContain(`${STORAGE_KEY_PREFIX}site_appearance`)
  })

  it('存储键与白名单和 stores/site.ts 的导出常量逐字一致', () => {
    // 生产端（buildBackgroundVars / persistAppearance）与首屏转发端必须共用同一份白名单，
    // 否则任一侧改名都会静默失效：这里直接对常量断言，不复制字面量。
    expect(APPEARANCE_STORAGE_KEY).toBe(`${STORAGE_KEY_PREFIX}site_appearance`)
    expect([...PREPAINT_APPEARANCE_VARS]).toEqual([...APPEARANCE_KEYS])
    expect(appearanceScript()).toContain(APPEARANCE_STORAGE_KEY)
    for (const key of PREPAINT_APPEARANCE_VARS) expect(appearanceScript()).toContain(key)
  })

  it('排在主题引导脚本之后、样式表之前', () => {
    const scripts = inlineScripts()
    expect(scripts).toHaveLength(2)
    expect(html.indexOf(appearanceScript())).toBeGreaterThan(html.indexOf(bootScript()))
    expect(html.indexOf(appearanceScript())).toBeLessThan(html.indexOf('</head>'))
  })

  it('只放行 5 个白名单属性，不额外声明其它属性名', () => {
    for (const key of APPEARANCE_KEYS) expect(appearanceScript()).toContain(key)
  })

  it('把缓存里的白名单属性写到 documentElement', () => {
    runAppearanceScript(
      JSON.stringify({
        '--ap-bg-color': '#1f2933',
        '--ap-bg-image': 'url("/uploads/cover.webp")',
        '--ap-bg-attachment': 'fixed',
        '--ap-bg-size': 'cover',
        '--ap-bg-repeat': 'no-repeat'
      })
    )
    expect(rootVar('--ap-bg-color')).toBe('#1f2933')
    expect(rootVar('--ap-bg-image')).toBe('url("/uploads/cover.webp")')
    expect(rootVar('--ap-bg-attachment')).toBe('fixed')
    expect(rootVar('--ap-bg-size')).toBe('cover')
    expect(rootVar('--ap-bg-repeat')).toBe('no-repeat')
  })

  it('多层组合值原样转发，脚本不参与拼接', () => {
    const composed =
      'linear-gradient(rgba(0,0,0,0.35), rgba(0,0,0,0.35)), url("/uploads/cover.webp")'
    runAppearanceScript(JSON.stringify({ '--ap-bg-image': composed }))
    expect(rootVar('--ap-bg-image')).toBe(composed)
  })

  /**
   * 壁纸标记必须在首次绘制前就落到 <html> 上。
   *
   * `.page-frame` 的纸面是否不透明取决于「这一屏有没有作者配置的装饰画布」，而这个判据
   * 在运行时由 applyAppearance() 写。若首屏不等它，读者会先看到一帧透明纸面 —— 正文
   * 直接压在壁纸上的错位观感 —— 再跳变到正确外观。因此标记必须与 --ap-bg-image
   * 同一次落地。
   *
   * 判据刻意与 stores/site.ts 的 buildBackgroundVars 保持同一个：「--ap-bg-image 非空」。
   * 照片与作者自定义渐变都走这个键（见 buildBackgroundVars 的 gradient 分支），
   * 两者都属于「画布不再是皮肤默认色」，正文都需要纸面兜底，所以不按 url() 再细分。
   */
  it('有背景图时首屏就打上 data-wallpaper 标记', () => {
    runAppearanceScript(JSON.stringify({ '--ap-bg-image': 'url("/uploads/cover.webp")' }))
    expect(document.documentElement.hasAttribute('data-wallpaper')).toBe(true)
  })

  it('只有纯色背景时不打标记（画布仍是皮肤色，无需纸面兜底）', () => {
    runAppearanceScript(JSON.stringify({ '--ap-bg-color': '#1f2933' }))
    expect(document.documentElement.hasAttribute('data-wallpaper')).toBe(false)
  })

  it('缓存为空串时清掉可能残留的标记', () => {
    document.documentElement.setAttribute('data-wallpaper', '')
    runAppearanceScript(JSON.stringify({ '--ap-bg-image': '' }))
    expect(document.documentElement.hasAttribute('data-wallpaper')).toBe(false)
  })

  it('白名单之外的键一律忽略（安全断言）', () => {
    runAppearanceScript(
      JSON.stringify({
        '--ap-bg-color': '#1f2933',
        '--ap-evil': 'red',
        '--bg-primary': 'red',
        '--accent': 'red',
        color: 'red'
      })
    )
    expect(rootVar('--ap-bg-color')).toBe('#1f2933')
    expect(rootVar('--ap-evil')).toBe('')
    expect(rootVar('--bg-primary')).toBe('')
    expect(rootVar('--accent')).toBe('')
    const style = document.documentElement.getAttribute('style') ?? ''
    expect(style).not.toContain('--ap-evil')
    expect(style).not.toContain('--bg-primary')
    expect(style).not.toContain('--accent')
    expect(style).not.toContain('color: red')
    expect(document.documentElement.style.getPropertyValue('color')).toBe('')
  })

  it('畸形 JSON / 非对象 / 非字符串值都不抛错也不写入', () => {
    for (const raw of ['{', 'not json', 'null', '42', '"string"', 'true', '[]']) {
      expect(() => runAppearanceScript(raw)).not.toThrow()
      for (const key of APPEARANCE_KEYS) expect(rootVar(key)).toBe('')
      expect(document.documentElement.getAttribute('style')).toBeNull()
    }
    expect(() =>
      runAppearanceScript(
        JSON.stringify({
          '--ap-bg-color': 123,
          '--ap-bg-image': null,
          '--ap-bg-size': ['cover'],
          '--ap-bg-repeat': { value: 'no-repeat' },
          '--ap-bg-attachment': false
        })
      )
    ).not.toThrow()
    for (const key of APPEARANCE_KEYS) expect(rootVar(key)).toBe('')
    expect(document.documentElement.getAttribute('style')).toBeNull()
  })

  it('空字符串表示“未设置”，不写入属性（留给 CSS 回退链）', () => {
    runAppearanceScript(
      JSON.stringify({
        '--ap-bg-color': '',
        '--ap-bg-image': '',
        '--ap-bg-attachment': '',
        '--ap-bg-size': '',
        '--ap-bg-repeat': ''
      })
    )
    for (const key of APPEARANCE_KEYS) expect(rootVar(key)).toBe('')
    expect(document.documentElement.getAttribute('style')).toBeNull()
  })

  it('没有缓存值时什么都不做', () => {
    runAppearanceScript(null)
    for (const key of APPEARANCE_KEYS) expect(rootVar(key)).toBe('')
    expect(document.documentElement.getAttribute('style')).toBeNull()
  })

  it('localStorage 抛错时静默忽略，不阻塞渲染', () => {
    const getItem = Storage.prototype.getItem
    Storage.prototype.getItem = () => {
      throw new Error('storage blocked')
    }
    try {
      expect(() => new Function(appearanceScript())()).not.toThrow()
      expect(document.documentElement.getAttribute('style')).toBeNull()
    } finally {
      Storage.prototype.getItem = getItem
    }
  })
})
