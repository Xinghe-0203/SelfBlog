import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export interface SiteSettings {
  siteTitle: string
  siteDescription: string
  authorName: string
  authorBio: string
  authorAvatar: string
  adminPath: string
  commentsEnabled: string
  visitorTrackingEnabled: string
  musicEnabled: string
  musicUrl: string
  musicTitle: string
  themeSkin: string
  themePrimaryColor: string
  footerText: string
  /** 浏览器标签页图标，由后台上传后下发；为空时回落到 index.html 的静态 /favicon.svg。 */
  faviconUrl: string
  /** 顶栏与后台侧边栏的品牌图标；为空时回落内置 Icon name="logo"。 */
  siteLogoUrl: string
}

const defaults: SiteSettings = {
  siteTitle: '个人博客',
  siteDescription: '记录技术、生活与一路上的新发现。',
  authorName: '站长',
  authorBio: '欢迎来到我的个人博客。',
  authorAvatar: '',
  adminPath: 'admin',
  commentsEnabled: 'true',
  visitorTrackingEnabled: 'true',
  musicEnabled: 'false',
  musicUrl: '',
  musicTitle: '背景音乐',
  themeSkin: 'ocean',
  themePrimaryColor: '#165DFF',
  footerText: '用文字记录生活，用好奇探索世界。',
  faviconUrl: '',
  siteLogoUrl: ''
}

/** 运行时注入的 favicon 节点标记，用来和 index.html 里的静态兜底区分。 */
const FAVICON_DATA_ATTR = 'runtime'

/**
 * 由文件扩展名推导 favicon 的 MIME。
 * 浏览器只认 svg/ico/png 这几类；其余（webp 等）按 png 处理，
 * 猜错只是不显示，不会更糟。
 */
export function mimeForIcon(url: string): string {
  // 用 replace 剥掉 query/hash，而不是 split(...)[0]：tsconfig 开了
  // noUncheckedIndexedAccess，索引取值会退化成 string | undefined。
  const path = url.replace(/[?#].*$/, '').toLowerCase()
  if (path.endsWith('.svg')) return 'image/svg+xml'
  if (path.endsWith('.ico')) return 'image/x-icon'
  return 'image/png'
}

/**
 * 把 faviconUrl 同步到 <head>。
 *
 * 幂等：每次先清掉上一次运行时注入的节点，再按需注入。
 * url 为空时只清理、不注入 —— index.html 里的静态 /favicon.svg 从未被移除，
 * 因此"清空设置"会自然回落到默认图标。
 */
export function applyFavicon(url: string): void {
  document.head
    .querySelectorAll<HTMLLinkElement>(`link[rel][data-${FAVICON_DATA_ATTR}]`)
    .forEach((link) => link.remove())

  if (!url) return

  const link = document.createElement('link')
  link.rel = 'icon'
  link.type = mimeForIcon(url)
  link.href = url
  link.dataset[FAVICON_DATA_ATTR] = 'favicon'
  document.head.appendChild(link)
}

export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings>({ ...defaults })
  const adminPath = computed(() => settings.value.adminPath || 'admin')
  const adminRoot = computed(() => `/${adminPath.value}`)
  const isCommentsEnabled = computed(() => settings.value.commentsEnabled === 'true')
  const isMusicEnabled = computed(
    () => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl
  )

  function applySettings(input: Partial<SiteSettings> | Record<string, string>): void {
    settings.value = { ...settings.value, ...input }
    const appRoot = document.getElementById('selfblog')
    if (appRoot) appRoot.setAttribute('data-skin', settings.value.themeSkin || 'ocean')
    if (appRoot) {
      appRoot.style.setProperty('--accent', settings.value.themePrimaryColor || '#165DFF')
      appRoot.style.setProperty('--primary', settings.value.themePrimaryColor || '#165DFF')
    }
    document.title = settings.value.siteTitle || defaults.siteTitle
    applyFavicon(settings.value.faviconUrl || '')
  }

  async function loadPublicSettings(): Promise<void> {
    try {
      const response = await fetch('/api/site/public', { headers: { Accept: 'application/json' } })
      if (!response.ok) return
      const envelope = (await response.json()) as { data?: Partial<SiteSettings> }
      if (envelope.data) applySettings(envelope.data)
    } catch {
      // Keep the local defaults available when the API is temporarily offline.
    }
  }

  return {
    settings,
    adminPath,
    adminRoot,
    isCommentsEnabled,
    isMusicEnabled,
    applySettings,
    loadPublicSettings
  }
})
