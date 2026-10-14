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
  footerText: '用文字记录生活，用好奇探索世界。'
}

export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings>({ ...defaults })
  const adminPath = computed(() => settings.value.adminPath || 'admin')
  const adminRoot = computed(() => `/${adminPath.value}`)
  const isCommentsEnabled = computed(() => settings.value.commentsEnabled === 'true')
  const isMusicEnabled = computed(() => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl)

  function applySettings(input: Partial<SiteSettings> | Record<string, string>): void {
    settings.value = { ...settings.value, ...input }
    const appRoot = document.getElementById('selfblog')
    if (appRoot) appRoot.setAttribute('data-skin', settings.value.themeSkin || 'ocean')
    if (appRoot) {
      appRoot.style.setProperty('--accent', settings.value.themePrimaryColor || '#165DFF')
      appRoot.style.setProperty('--primary', settings.value.themePrimaryColor || '#165DFF')
    }
    document.title = settings.value.siteTitle || defaults.siteTitle
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

  return { settings, adminPath, adminRoot, isCommentsEnabled, isMusicEnabled, applySettings, loadPublicSettings }
})
