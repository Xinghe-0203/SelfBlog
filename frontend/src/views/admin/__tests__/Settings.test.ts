import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Settings from '../Settings.vue'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'
import { useSiteStore } from '@/stores/site'

const { getSettings, updateSettings } = vi.hoisted(() => ({
  getSettings: vi.fn(),
  updateSettings: vi.fn()
}))

vi.mock('@/api/site', () => ({
  siteApi: { getSettings, updateSettings, uploadSiteAsset: vi.fn() }
}))

const STATIC_FAVICON = '<link rel="icon" type="image/svg+xml" href="/favicon.svg">'

let pinia: ReturnType<typeof createPinia>

async function mountSettings() {
  const wrapper = mount(Settings, { global: { plugins: [pinia] } })
  await flushPromises()
  return wrapper
}

function requireField(wrapper: Awaited<ReturnType<typeof mountSettings>>, kind: string) {
  const field = wrapper
    .findAllComponents(SiteAssetField)
    .find((candidate) => candidate.props('kind') === kind)
  if (!field) throw new Error(`missing SiteAssetField: ${kind}`)
  return field
}

describe('Settings brand asset fields', () => {
  beforeEach(() => {
    document.head.innerHTML = STATIC_FAVICON
    pinia = createPinia()
    setActivePinia(pinia)
    getSettings.mockReset()
    updateSettings.mockReset()
    getSettings.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ...useSiteStore().settings }
    })
  })

  it('binds all four asset fields to the loaded settings', async () => {
    getSettings.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: {
        ...useSiteStore().settings,
        authorAvatar: '/uploads/site-assets/avatar/me.png',
        siteLogoUrl: '/uploads/site-assets/logo/me.svg',
        faviconUrl: '/uploads/site-assets/favicon/me.ico',
        musicUrl: '/uploads/site-assets/music/me.mp3'
      }
    })
    const wrapper = await mountSettings()

    const loaded = {
      avatar: '/uploads/site-assets/avatar/me.png',
      logo: '/uploads/site-assets/logo/me.svg',
      favicon: '/uploads/site-assets/favicon/me.ico',
      music: '/uploads/site-assets/music/me.mp3'
    } as const
    for (const [kind, url] of Object.entries(loaded)) {
      expect(requireField(wrapper, kind).props('modelValue')).toBe(url)
    }
    expect(wrapper.text()).toContain('站点 Logo')
    expect(wrapper.text()).toContain('网站图标')
  })

  it('routes every uploaded url to its own store key and to the save payload', async () => {
    const wrapper = await mountSettings()
    const store = useSiteStore()
    const uploads = [
      { kind: 'avatar', key: 'authorAvatar', url: '/uploads/site-assets/avatar/sample.png' },
      { kind: 'logo', key: 'siteLogoUrl', url: '/uploads/site-assets/logo/sample.svg' },
      { kind: 'favicon', key: 'faviconUrl', url: '/uploads/site-assets/favicon/sample.ico' },
      { kind: 'music', key: 'musicUrl', url: '/uploads/site-assets/music/sample.mp3' }
    ] as const

    for (const upload of uploads) {
      requireField(wrapper, upload.kind).vm.$emit('uploaded', upload.url)
      await nextTick()
      expect(store.settings[upload.key]).toBe(upload.url)
      if (upload.kind === 'favicon') {
        const injected = document.head.querySelector<HTMLLinkElement>('link[data-runtime]')
        expect(injected?.getAttribute('href')).toBe(upload.url)
      }
    }

    updateSettings.mockResolvedValue({ code: 200, message: 'OK', data: { ...store.settings } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
    for (const upload of uploads) {
      expect(payload[upload.key]).toBe(upload.url)
    }
  })

  it('keeps manual url edits out of the global store until save', async () => {
    const wrapper = await mountSettings()
    const store = useSiteStore()
    const manual = [
      { kind: 'avatar', key: 'authorAvatar', url: '/uploads/site-assets/avatar/manual.png' },
      { kind: 'logo', key: 'siteLogoUrl', url: '/uploads/site-assets/logo/manual.svg' },
      { kind: 'favicon', key: 'faviconUrl', url: '/uploads/site-assets/favicon/manual.ico' },
      { kind: 'music', key: 'musicUrl', url: '/uploads/site-assets/music/manual.mp3' }
    ] as const

    for (const item of manual) {
      requireField(wrapper, item.kind).vm.$emit('update:modelValue', item.url)
      await nextTick()
      expect(store.settings[item.key]).toBe('')
    }
    expect(document.head.querySelector('link[data-runtime]')).toBeNull()

    updateSettings.mockResolvedValue({ code: 200, message: 'OK', data: { ...store.settings } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
    for (const item of manual) {
      expect(payload[item.key]).toBe(item.url)
    }
  })

  it('groups real setting fields by meaning and keeps a single save path', async () => {
    const wrapper = await mountSettings()
    const groupTitles = wrapper.findAll('.panel-heading h2').map((heading) => heading.text())
    expect(groupTitles).toEqual(['站点资料', '后台与访客', '评论与音乐', '视觉主题'])
    expect(wrapper.findAll('form')).toHaveLength(1)
  })
})
