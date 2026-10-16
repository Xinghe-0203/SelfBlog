import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { applyFavicon, mimeForIcon, useSiteStore } from '../site'

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
