import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  APPEARANCE_STORAGE_KEY,
  applyFavicon,
  buildBackgroundVars,
  effectiveSurfaceOpacities,
  isDarkCanvas,
  mimeForIcon,
  persistAppearance,
  useSiteStore
} from '../site'

const STATIC_FAVICON = '<link rel="icon" type="image/svg+xml" href="/favicon.svg">'

function injected(): HTMLLinkElement | null {
  return document.head.querySelector<HTMLLinkElement>('link[rel][data-runtime]')
}

describe('mimeForIcon', () => {
  it('maps svg', () => {
    expect(mimeForIcon('/uploads/site-assets/favicon/a.svg')).toBe('image/svg+xml')
  })

  it('maps ico', () => {
    expect(mimeForIcon('/uploads/site-assets/favicon/a.ico')).toBe('image/x-icon')
  })

  it('falls back to png for raster and unknown formats', () => {
    expect(mimeForIcon('/uploads/site-assets/favicon/a.png')).toBe('image/png')
    expect(mimeForIcon('/uploads/site-assets/favicon/a.webp')).toBe('image/png')
    expect(mimeForIcon('https://cdn.example.com/fav')).toBe('image/png')
  })

  it('ignores query strings when reading the extension', () => {
    expect(mimeForIcon('/uploads/a.svg?v=2')).toBe('image/svg+xml')
  })
})

describe('applyFavicon', () => {
  beforeEach(() => {
    document.head.innerHTML = STATIC_FAVICON
  })

  it('injects a link derived from the uploaded url', () => {
    applyFavicon('/uploads/site-assets/favicon/abc.svg')

    const link = injected()
    expect(link).not.toBeNull()
    expect(link?.getAttribute('href')).toBe('/uploads/site-assets/favicon/abc.svg')
    expect(link?.getAttribute('type')).toBe('image/svg+xml')
    expect(link?.getAttribute('rel')).toBe('icon')
  })

  it('replaces the previous injection instead of stacking links', () => {
    applyFavicon('/uploads/site-assets/favicon/first.png')
    applyFavicon('/uploads/site-assets/favicon/second.ico')

    expect(document.head.querySelectorAll('link[rel][data-runtime]')).toHaveLength(1)
    expect(injected()?.getAttribute('href')).toBe('/uploads/site-assets/favicon/second.ico')
  })

  it('keeps the static fallback untouched when the setting is cleared', () => {
    applyFavicon('/uploads/site-assets/favicon/first.png')
    applyFavicon('')

    expect(injected()).toBeNull()
    const links = document.head.querySelectorAll('link')
    expect(links).toHaveLength(1)
    expect(links[0]?.getAttribute('href')).toBe('/favicon.svg')
  })

  it('never removes the static fallback link', () => {
    applyFavicon('/uploads/site-assets/favicon/first.png')
    expect(document.head.querySelector('link[href="/favicon.svg"]')).not.toBeNull()
  })
})

describe('site store brand assets', () => {
  beforeEach(() => {
    document.head.innerHTML = STATIC_FAVICON
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('defaults both brand assets to empty strings', () => {
    const store = useSiteStore()
    expect(store.settings.faviconUrl).toBe('')
    expect(store.settings.siteLogoUrl).toBe('')
  })

  it('applies uploaded brand assets and injects the favicon', () => {
    const store = useSiteStore()
    store.applySettings({
      siteLogoUrl: '/uploads/site-assets/logo/a.png',
      faviconUrl: '/uploads/site-assets/favicon/a.ico'
    })

    expect(store.settings.siteLogoUrl).toBe('/uploads/site-assets/logo/a.png')
    expect(injected()?.getAttribute('href')).toBe('/uploads/site-assets/favicon/a.ico')
  })

  it('falls back to the static favicon when the setting is empty', () => {
    const store = useSiteStore()
    store.applySettings({ faviconUrl: '' })
    expect(injected()).toBeNull()
  })

  it('derives soft accent colors from the configured primary', () => {
    const root = document.createElement('div')
    root.id = 'selfblog'
    document.body.appendChild(root)
    try {
      useSiteStore().applySettings({ themePrimaryColor: '#a34466' })
      expect(root.style.getPropertyValue('--site-primary')).toBe('#a34466')
      expect(root.style.getPropertyValue('--accent')).toContain('var(--site-primary)')
      expect(root.style.getPropertyValue('--accent-light')).toContain('color-mix')
    } finally {
      root.remove()
    }
  })
})

describe('isDarkCanvas', () => {
  afterEach(() => {
    document.documentElement.classList.remove('dark-mode')
  })

  it('is light by default for the default skin', () => {
    expect(isDarkCanvas('ocean')).toBe(false)
  })

  it('is dark whenever the dark-mode class is present', () => {
    document.documentElement.classList.add('dark-mode')
    expect(isDarkCanvas('ocean')).toBe(true)
  })

  it('treats the midnight skin as a dark canvas even without the class', () => {
    // midnight 把 --text-primary 换成浅色，画布本身已经深了；
    // 只看 .dark-mode 会漏判，浅色背景配浅色文字直接不可读。
    expect(isDarkCanvas('midnight')).toBe(true)
  })
})

describe('buildBackgroundVars', () => {
  const IMAGE = '/uploads/site-assets/background/hero.webp'
  const HEX_COLOR = /^#[0-9a-f]{3,8}$/i

  it('produces no visible background for an untouched site', () => {
    // 承重契约：作者没碰过的背景必须让 --bg-primary 继续说了算，
    // 否则换皮肤会被默认色覆盖，升级后观感也会变。
    const vars = buildBackgroundVars({})
    expect(vars['--ap-bg-image']).toBeUndefined()
    expect(vars['--ap-bg-color']).toBe('')
  })

  it('leaves the default solid color unset so skins keep winning', () => {
    const vars = buildBackgroundVars({ backgroundType: 'solid', backgroundColor: '#f5f6f8' })
    expect(vars['--ap-bg-color']).toBe('')
  })

  it('emits the author color when it differs from the default', () => {
    const vars = buildBackgroundVars({ backgroundType: 'solid', backgroundColor: '#123456' })
    expect(vars['--ap-bg-color']).toBe('#123456')
  })

  it('rejects a malformed color instead of splicing it into CSS', () => {
    const vars = buildBackgroundVars({ backgroundColor: 'red; } body { display:none' })
    expect(vars['--ap-bg-color']).toBe('')
  })

  it('builds a gradient from the two stops', () => {
    const vars = buildBackgroundVars({
      backgroundType: 'gradient',
      backgroundGradientFrom: '#101010',
      backgroundGradientTo: '#202020'
    })
    expect(vars['--ap-bg-image']).toBe('linear-gradient(180deg, #101010, #202020)')
  })

  it('lays a scrim over the image only when an overlay is configured', () => {
    const bare = buildBackgroundVars({ backgroundType: 'image', backgroundImageUrl: IMAGE })
    expect(bare['--ap-bg-image']).toBe(`url("${IMAGE}")`)
    expect(bare['--ap-bg-size']).toBe('cover')
    expect(bare['--ap-bg-attachment']).toBe('fixed')

    const scrimmed = buildBackgroundVars({
      backgroundType: 'image',
      backgroundImageUrl: IMAGE,
      backgroundOverlay: '30'
    })
    expect(scrimmed['--ap-bg-image']).toBe(
      `linear-gradient(rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.3)), url("${IMAGE}")`
    )
  })

  it('honours the scroll attachment choice', () => {
    const vars = buildBackgroundVars({
      backgroundType: 'image',
      backgroundImageUrl: IMAGE,
      backgroundFixed: 'false'
    })
    expect(vars['--ap-bg-attachment']).toBe('scroll')
  })

  it('drops image urls that could break out of url("…")', () => {
    const injected = buildBackgroundVars({
      backgroundType: 'image',
      backgroundImageUrl: 'https://evil.example/" ); background-image: url("x'
    })
    expect(injected['--ap-bg-image']).toBeUndefined()
  })

  it('rejects non-http, non-/uploads sources', () => {
    const vars = buildBackgroundVars({
      backgroundType: 'image',
      backgroundImageUrl: 'javascript:alert(1)'
    })
    expect(vars['--ap-bg-image']).toBeUndefined()
  })

  it('drops the author color and gradient on a dark canvas', () => {
    // 深色画布的文字是浅色，浅底浅字不可读；两者都是按浅色画布挑的，必须整体让位。
    const solid = buildBackgroundVars({ backgroundType: 'solid', backgroundColor: '#123456' }, true)
    expect(solid['--ap-bg-color']).toBe('')

    const gradient = buildBackgroundVars(
      {
        backgroundType: 'gradient',
        backgroundGradientFrom: '#101010',
        backgroundGradientTo: '#202020'
      },
      true
    )
    expect(gradient['--ap-bg-image']).toBeUndefined()
  })

  it('keeps the image on a dark canvas but forces a readable overlay', () => {
    // 背景图是作者明确的视觉选择，保留；可读性靠加厚遮罩保证。
    for (const overlay of [undefined, '0', '10']) {
      const vars = buildBackgroundVars(
        { backgroundType: 'image', backgroundImageUrl: IMAGE, backgroundOverlay: overlay },
        true
      )
      const image = vars['--ap-bg-image'] ?? ''
      expect(image).toContain(`url("${IMAGE}")`)
      const alpha = Number(image.match(/rgba\(0, 0, 0, ([\d.]+)\)/)?.[1])
      expect(alpha).toBeGreaterThanOrEqual(0.45)
      expect(alpha).toBeLessThanOrEqual(1)
    }
  })

  it('never lowers an overlay the author already set above the dark floor', () => {
    const vars = buildBackgroundVars(
      { backgroundType: 'image', backgroundImageUrl: IMAGE, backgroundOverlay: '80' },
      true
    )
    expect(vars['--ap-bg-image']).toContain('rgba(0, 0, 0, 0.8)')
  })

  it('keeps every emitted value CSS-safe', () => {
    const vars = buildBackgroundVars({
      backgroundType: 'gradient',
      backgroundColor: '#123456',
      backgroundGradientFrom: '#101010',
      backgroundGradientTo: '#202020'
    })
    for (const [key, value] of Object.entries(vars)) {
      if (key === '--ap-bg-color') expect(value).toMatch(HEX_COLOR)
      else expect(value).not.toContain(';')
    }
  })
})

describe('persistAppearance', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('stores only the whitelisted keys the prepaint script knows', () => {
    persistAppearance({
      '--ap-bg-image': 'url("/uploads/a.webp")',
      '--ap-bg-size': 'cover',
      '--ap-surface-opacity': '80%',
      '--not-whitelisted': 'nope'
    })

    const raw = localStorage.getItem(APPEARANCE_STORAGE_KEY)
    expect(raw).not.toBeNull()
    const payload = JSON.parse(raw as string)
    expect(payload).toEqual({
      '--ap-bg-image': 'url("/uploads/a.webp")',
      '--ap-bg-size': 'cover'
    })
  })

  it('clears the entry rather than storing an empty shell', () => {
    persistAppearance({ '--ap-bg-image': 'url("/uploads/a.webp")' })
    expect(localStorage.getItem(APPEARANCE_STORAGE_KEY)).not.toBeNull()

    persistAppearance({ '--ap-bg-color': '' })
    // 空串在预置脚本里等于「未设置」，留下空壳只会让存储和实际状态不一致。
    expect(localStorage.getItem(APPEARANCE_STORAGE_KEY)).toBeNull()
  })

  it('survives a storage that throws', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota exceeded')
    })
    // 预置只是消闪烁的优化，存储失败绝不能把渲染打断。
    expect(() => persistAppearance({ '--ap-bg-size': 'cover' })).not.toThrow()
    setItem.mockRestore()
  })
})

describe('effectiveSurfaceOpacities', () => {
  it('leaves the author value untouched when the canvas is a calm solid', () => {
    // 回归红线：没有壁纸时三档必须同值，且等于作者设置 —— 这就是改造前那档半透明。
    expect(effectiveSurfaceOpacities('60', false, false)).toEqual({
      paper: 60,
      raised: 60,
      control: 60
    })
    expect(effectiveSurfaceOpacities('100', false, false)).toEqual({
      paper: 100,
      raised: 100,
      control: 100
    })
    // 缺省 / 非法值走 clampInt 的兜底，仍然是完全实心。
    expect(effectiveSurfaceOpacities(undefined, false, false).paper).toBe(100)
    expect(effectiveSurfaceOpacities('abc', false, false).raised).toBe(100)
  })

  it('keeps the 60-100 storage contract instead of rewriting the author setting', () => {
    // 后端只允许 60–100；越界值在这里夹紧，但库里那条设置保持原样。
    expect(effectiveSurfaceOpacities('10', false, false).paper).toBe(60)
    expect(effectiveSurfaceOpacities('250', false, false).paper).toBe(100)
  })

  it('forces an opaque paper tier over a wallpaper so body text never sits on the photo', () => {
    // 弱化文字在纯白上也只有 4.35:1 ⇒ 任何半透明纸面都兜不住，纸面必须 100%。
    for (const dark of [false, true]) {
      expect(effectiveSurfaceOpacities('60', dark, true).paper).toBe(100)
    }
  })

  it('raises the translucent tiers to a per-canvas contrast floor', () => {
    // 浅色画布 88 / 深色画布 94：浅字压在半透明深色面上更容易掉到 2:1，所以深色要更厚。
    expect(effectiveSurfaceOpacities('60', false, true).raised).toBe(88)
    expect(effectiveSurfaceOpacities('60', true, true).raised).toBe(94)
  })

  it('never lowers an author setting that already sits above the floor', () => {
    expect(effectiveSurfaceOpacities('95', false, true).raised).toBe(95)
    expect(effectiveSurfaceOpacities('100', true, true).raised).toBe(100)
  })

  it('keeps three visibly distinct tiers over a wallpaper', () => {
    // I3：纸面 → 卡片 → 控件必须读得出来，不能三档同值（那就是改造前的单档半透明）。
    for (const dark of [false, true]) {
      const tiers = effectiveSurfaceOpacities('60', dark, true)
      expect(tiers.paper).toBeGreaterThan(tiers.raised)
      expect(tiers.raised).toBeGreaterThan(tiers.control)
      expect(tiers.control).toBeGreaterThanOrEqual(60)
    }
  })
})

describe('wallpaper flag', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    delete document.documentElement.dataset.wallpaper
    document.documentElement.removeAttribute('style')
  })

  afterEach(() => {
    delete document.documentElement.dataset.wallpaper
    document.documentElement.removeAttribute('style')
    document.documentElement.classList.remove('dark-mode')
    localStorage.clear()
  })

  it('marks the root element when the canvas is an image or author gradient', () => {
    // data-wallpaper 是整套纸面分层的开关，写在 <html> 上 —— 与 index.html 首屏
    // 预置脚本同一个元素、同一个属性，否则两边会各写各的、CSS 选择器只认一边。
    const store = useSiteStore()
    store.applySettings({
      backgroundType: 'image',
      backgroundImageUrl: '/uploads/site-assets/background/hero.webp'
    })
    expect(document.documentElement.dataset.wallpaper).toBe('')

    store.applySettings({ backgroundType: 'gradient' })
    expect(document.documentElement.dataset.wallpaper).toBe('')
  })

  it('clears the flag when the canvas is a solid color', () => {
    const store = useSiteStore()
    store.applySettings({
      backgroundType: 'image',
      backgroundImageUrl: '/uploads/site-assets/background/hero.webp'
    })
    store.applySettings({ backgroundType: 'solid' })
    expect(document.documentElement.dataset.wallpaper).toBeUndefined()
  })

  it('ignores an unsafe background url instead of flagging the canvas', () => {
    const store = useSiteStore()
    store.applySettings({
      backgroundType: 'image',
      backgroundImageUrl: 'javascript:alert(1)'
    })
    expect(document.documentElement.dataset.wallpaper).toBeUndefined()
  })

  it('writes the flag even before #selfblog exists, so prepaint and runtime agree', () => {
    // 挂载前 loadPublicSettings 就要落地这个属性；applyAppearance 在 #selfblog 早退
    // 之前必须先写它，否则首屏到挂载之间会有一段「纸面分层不生效」的窗口。
    expect(document.getElementById('selfblog')).toBeNull()
    useSiteStore().applySettings({
      backgroundType: 'image',
      backgroundImageUrl: '/uploads/site-assets/background/hero.webp'
    })
    expect(document.documentElement.dataset.wallpaper).toBe('')
  })
})

describe('appearance customizations', () => {
  let root: HTMLDivElement

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    document.documentElement.removeAttribute('style')
    delete document.documentElement.dataset.wallpaper
    root = document.createElement('div')
    root.id = 'selfblog'
    document.body.appendChild(root)
  })

  afterEach(() => {
    root.remove()
    document.documentElement.removeAttribute('style')
    delete document.documentElement.dataset.wallpaper
    localStorage.clear()
  })

  it('leaves every hook unset on a default site', () => {
    // 承重：默认档必须是「一条覆盖规则都不命中」。数值变量若无条件写死，
    // .page-markdown 原本的 line-height:1.9 会被 1.75 静默盖掉。
    useSiteStore().applySettings({})
    expect(root.dataset.apFont).toBeUndefined()
    expect(root.dataset.apRadius).toBeUndefined()
    expect(root.dataset.apShadow).toBeUndefined()
    expect(root.dataset.apDensity).toBeUndefined()
    expect(root.dataset.apHero).toBeUndefined()
    expect(root.dataset.apPostCover).toBeUndefined()
    expect(root.dataset.apMotion).toBeUndefined()
    expect(root.style.getPropertyValue('--ap-base-font-size')).toBe('')
    expect(root.style.getPropertyValue('--ap-body-line-height')).toBe('')
    expect(root.style.getPropertyValue('--ap-reader-width')).toBe('')
    expect(root.style.getPropertyValue('--ap-paragraph-spacing')).toBe('')
  })

  it('maps enum settings onto data attributes and numeric ones onto variables', () => {
    useSiteStore().applySettings({
      fontFamily: 'serif',
      radiusScale: 'soft',
      shadowDepth: 'deep',
      layoutDensity: 'compact',
      homeHeroStyle: 'minimal',
      postCoverStyle: 'wide',
      motionIntensity: 'subtle',
      baseFontSize: '18',
      bodyLineHeight: '1.9',
      readerWidth: '820',
      paragraphSpacing: '0.5'
    })
    expect(root.dataset.apFont).toBe('serif')
    expect(root.dataset.apRadius).toBe('soft')
    expect(root.dataset.apShadow).toBe('deep')
    expect(root.dataset.apDensity).toBe('compact')
    expect(root.dataset.apHero).toBe('minimal')
    expect(root.dataset.apPostCover).toBe('wide')
    expect(root.dataset.apMotion).toBe('subtle')
    expect(root.style.getPropertyValue('--ap-base-font-size')).toBe('18px')
    expect(root.style.getPropertyValue('--ap-body-line-height')).toBe('1.9')
    expect(root.style.getPropertyValue('--ap-reader-width')).toBe('820px')
    expect(root.style.getPropertyValue('--ap-paragraph-spacing')).toBe('0.5em')
  })

  it('clamps out-of-range numbers and drops unknown enum values', () => {
    useSiteStore().applySettings({
      baseFontSize: '99',
      bodyLineHeight: '0.2',
      readerWidth: '10',
      paragraphSpacing: '8',
      fontFamily: 'comic-sans',
      motionIntensity: ''
    })
    expect(root.style.getPropertyValue('--ap-base-font-size')).toBe('19px')
    expect(root.style.getPropertyValue('--ap-body-line-height')).toBe('1.5')
    expect(root.style.getPropertyValue('--ap-reader-width')).toBe('640px')
    expect(root.style.getPropertyValue('--ap-paragraph-spacing')).toBe('2em')
    expect(root.dataset.apFont).toBeUndefined()
    expect(root.dataset.apMotion).toBeUndefined()
  })

  it('removes a hook again when the author returns to the default', () => {
    const store = useSiteStore()
    store.applySettings({ fontFamily: 'mono', readerWidth: '900' })
    expect(root.dataset.apFont).toBe('mono')
    expect(root.style.getPropertyValue('--ap-reader-width')).toBe('900px')

    store.applySettings({ fontFamily: 'system', readerWidth: '760' })
    expect(root.dataset.apFont).toBeUndefined()
    expect(root.style.getPropertyValue('--ap-reader-width')).toBe('')
  })
})
