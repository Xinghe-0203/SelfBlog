import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  APPEARANCE_STORAGE_KEY,
  applyFavicon,
  buildBackgroundVars,
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
