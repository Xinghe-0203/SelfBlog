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
      data: { ...useSiteStore().settings, authorAvatar: '/uploads/site-assets/avatar/me.png' }
    })
    const wrapper = await mountSettings()

    const kinds = wrapper.findAllComponents(SiteAssetField).map((field) => field.props('kind'))
    expect(kinds).toEqual(expect.arrayContaining(['avatar', 'logo', 'favicon', 'music']))
    expect(requireField(wrapper, 'avatar').props('modelValue')).toBe(
      '/uploads/site-assets/avatar/me.png'
    )
    expect(wrapper.text()).toContain('站点 Logo')
    expect(wrapper.text()).toContain('网站图标')
  })

  it('applies an uploaded url optimistically, then persists it on save', async () => {
    const wrapper = await mountSettings()
    const favicon = requireField(wrapper, 'favicon')

    favicon.vm.$emit('update:modelValue', '/uploads/site-assets/favicon/new.ico')
    await nextTick()

    // 乐观更新：store 立刻换新，favicon link 也立刻注入
    const store = useSiteStore()
    expect(store.settings.faviconUrl).toBe('/uploads/site-assets/favicon/new.ico')
    const injected = document.head.querySelector<HTMLLinkElement>('link[data-runtime]')
    expect(injected?.getAttribute('href')).toBe('/uploads/site-assets/favicon/new.ico')

    updateSettings.mockResolvedValue({ code: 200, message: 'OK', data: { ...store.settings } })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(updateSettings).toHaveBeenCalledWith(
      expect.objectContaining({ faviconUrl: '/uploads/site-assets/favicon/new.ico' })
    )
  })
})
