import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Settings from '../Settings.vue'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'
import { useSiteStore } from '@/stores/site'

const { getSettings, updateSettings, testConnection, listModels } = vi.hoisted(() => ({
  getSettings: vi.fn(),
  updateSettings: vi.fn(),
  testConnection: vi.fn(),
  listModels: vi.fn()
}))

vi.mock('@/api/site', () => ({
  siteApi: { getSettings, updateSettings, uploadSiteAsset: vi.fn() }
}))

vi.mock('@/api/seo', () => ({
  seoApi: { testConnection, listModels }
}))

const STATIC_FAVICON = '<link rel="icon" type="image/svg+xml" href="/favicon.svg">'

let pinia: ReturnType<typeof createPinia>

async function mountSettings() {
  const wrapper = mount(Settings, {
    global: {
      plugins: [pinia],
      // Settings 只有一条指向「SEO 管理」的 RouterLink，不需要真路由表；
      // 用保留 slot 的桩替代，避免未注册组件退化成 <routerlink> 拿不到 href。
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="to"><slot /></a>'
        }
      }
    }
  })
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

/** 与 requireField 相对：断言"某个资源字段当前没有渲染"时用它，找不到不会抛错。 */
function hasField(wrapper: Awaited<ReturnType<typeof mountSettings>>, kind: string) {
  return wrapper
    .findAllComponents(SiteAssetField)
    .some((candidate) => candidate.props('kind') === kind)
}

describe('Settings brand asset fields', () => {
  beforeEach(() => {
    document.head.innerHTML = STATIC_FAVICON
    pinia = createPinia()
    setActivePinia(pinia)
    getSettings.mockReset()
    updateSettings.mockReset()
    listModels.mockReset()
    // 默认：探测成功但上游只给内置候选；个别用例会覆盖成 reject 验证静默回落。
    listModels.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: {
        models: [
          { id: 'deepseek-flash', name: 'DeepSeek-V4.1-Flash', supportsImage: true },
          { id: 'deepseek-v4-pro', name: 'DeepSeek-V4-Pro', supportsImage: false }
        ],
        source: 'fallback'
      }
    })
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
    // 四个分组 tab（v2.0.30 起把 7 段长表单拆成 4 个页签）
    const tabs = wrapper.findAll('[data-tab]').map((tab) => tab.attributes('data-tab'))
    expect(tabs).toEqual(['profile', 'appearance', 'interaction', 'system'])
    const activeTab = wrapper
      .findAll('[role="tab"]')
      .find((tab) => tab.attributes('aria-selected') === 'true')
    expect(activeTab?.attributes('data-tab')).toBe('profile')

    // 面板按分组归位，标题顺序与作者阅读顺序一致
    const groupTitles = wrapper.findAll('.panel-heading h2').map((heading) => heading.text())
    expect(groupTitles).toEqual([
      '站点资料',
      '成品主题',
      '视觉主题',
      '玻璃感',
      '页面背景',
      '外观深度自定义',
      '互动开关',
      '背景音乐',
      '留言审核',
      '后台入口',
      '访客统计',
      '模型接口'
    ])
    expect(wrapper.findAll('form')).toHaveLength(1)
  })

  it('switches the visible panel group without leaving the page', async () => {
    const wrapper = await mountSettings()
    const panelFor = (tab: string) => wrapper.get(`[data-tab-panel="${tab}"]`)
    // VTU 默认挂载在脱离 document 的容器里，jsdom 对游离节点的 getComputedStyle 拿不到
    // 内联 display，isVisible() 会恒为 true；这里直接读 v-show 写下的内联样式。
    const hidden = (tab: string) =>
      (panelFor(tab).attributes('style') ?? '').includes('display: none')

    expect(hidden('profile')).toBe(false)
    expect(hidden('appearance')).toBe(true)

    await wrapper.get('[data-tab="appearance"]').trigger('click')
    expect(hidden('profile')).toBe(true)
    expect(hidden('appearance')).toBe(false)
    expect(wrapper.get('[data-tab="appearance"]').attributes('aria-selected')).toBe('true')
    // 换分组不丢表单内容，也不触发保存
    expect(updateSettings).not.toHaveBeenCalled()
  })

  it('keeps the comment moderation toggle in 内容与互动 and ships it with the save payload', async () => {
    const wrapper = await mountSettings()
    expect(wrapper.find('[data-setting="llmCommentModeration"]').exists()).toBe(true)
    const toggle = wrapper.get('[data-setting="llmCommentModeration"]')

    await toggle.setValue(true)
    updateSettings.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ...useSiteStore().settings }
    })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(updateSettings.mock.calls[0]?.[0]).toMatchObject({ llmCommentModeration: 'true' })
  })

  it('points the model endpoint at the SEO page instead of duplicating its form', async () => {
    const wrapper = await mountSettings()
    // 模型连接表单已整体迁到「SEO 管理」页顶部，这里只留一条入口说明
    for (const name of ['llmBaseUrl', 'llmApiKey', 'llmModel', 'llmEnabled']) {
      expect(wrapper.find(`[data-setting="${name}"]`).exists()).toBe(false)
    }
    const entry = wrapper.get('[data-panel="llm-entry"]')
    expect(entry.text()).toContain('模型接口')
    expect(entry.get('a').attributes('href')).toContain('/seo')
  })

  it('saves the site domain, documents the env fallback and surfaces backend validation errors', async () => {
    const wrapper = await mountSettings()
    const label = wrapper
      .findAll('label')
      .find((candidate) => candidate.text().includes('网站域名'))
    if (!label) throw new Error('missing 网站域名 field')
    expect(label.text()).toContain('.env')
    expect(label.text()).toContain('sitemap')

    await label.get('input').setValue('https://blog.example.com')
    updateSettings.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ...useSiteStore().settings, siteBaseUrl: 'https://blog.example.com' }
    })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(updateSettings.mock.calls[0]?.[0]).toMatchObject({
      siteBaseUrl: 'https://blog.example.com'
    })

    updateSettings.mockRejectedValue({
      userMessage: '网站域名需为 http(s) 站点地址（如 https://blog.example.com）'
    })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('网站域名需为')
  })

  describe('page background panel', () => {
    /** 9 个新字段的完整取值，用来验证「加载 → 渲染 → 提交」整条链路。 */
    const BACKGROUND = {
      backgroundType: 'image',
      backgroundColor: '#123456',
      backgroundGradientFrom: '#101010',
      backgroundGradientTo: '#202020',
      backgroundImageUrl: '/uploads/site-assets/background/hero.webp',
      backgroundOverlay: '30',
      backgroundFixed: 'false',
      surfaceOpacity: '80',
      surfaceBlur: '12'
    } as const

    function control(wrapper: Awaited<ReturnType<typeof mountSettings>>, name: string) {
      const found = wrapper.find(`[data-setting="${name}"]`)
      if (!found.exists()) throw new Error(`missing control: ${name}`)
      return found
    }

    /** 断言"某个控件当前不存在"时必须用这个：control() 找不到会直接抛错。 */
    function hasControl(wrapper: Awaited<ReturnType<typeof mountSettings>>, name: string) {
      return wrapper.find(`[data-setting="${name}"]`).exists()
    }

    function preview(wrapper: Awaited<ReturnType<typeof mountSettings>>) {
      return wrapper.get('[data-preview="page-background"]')
    }

    it('appends the panel after 视觉主题 and lists every control across the three types', async () => {
      const wrapper = await mountSettings()
      // 后面的「外观深度自定义」也会追加面板，因此按标题定位而不是取最后一个。
      const panel = wrapper
        .findAll('section.settings-panel')
        .find((candidate) => candidate.find('.panel-heading h2').text() === '页面背景')
      if (!panel) throw new Error('missing 页面背景 panel')
      expect(panel.find('.panel-desc').text()).toContain('卡片的不透明度与模糊')

      // 类型选择；卡片表面控制已独立到「玻璃感」面板，但仍在同一张表单里
      expect(control(wrapper, 'backgroundType').exists()).toBe(true)
      expect(control(wrapper, 'surfaceOpacity').attributes('min')).toBe('60')
      // 玻璃感开启时不透明度上限是 92%：100% 等于不透明，交给总开关表达
      expect(control(wrapper, 'surfaceOpacity').attributes('max')).toBe('92')
      expect(control(wrapper, 'surfaceBlur').attributes('min')).toBe('0')
      expect(control(wrapper, 'surfaceBlur').attributes('max')).toBe('40')

      await control(wrapper, 'backgroundType').setValue('solid')
      expect(control(wrapper, 'backgroundColor').exists()).toBe(true)
      expect(hasControl(wrapper, 'backgroundGradientFrom')).toBe(false)

      await control(wrapper, 'backgroundType').setValue('gradient')
      expect(control(wrapper, 'backgroundGradientFrom').exists()).toBe(true)
      expect(control(wrapper, 'backgroundGradientTo').exists()).toBe(true)

      await control(wrapper, 'backgroundType').setValue('image')
      const image = requireField(wrapper, 'background')
      expect(image.props('kind')).toBe('background')
      expect(control(wrapper, 'backgroundOverlay').attributes('max')).toBe('90')
      expect(control(wrapper, 'backgroundFixed').exists()).toBe(true)
    })

    it('swaps the conditional controls with the selected background type', async () => {
      const wrapper = await mountSettings()

      await control(wrapper, 'backgroundType').setValue('solid')
      expect(hasControl(wrapper, 'backgroundColor')).toBe(true)
      expect(hasControl(wrapper, 'backgroundGradientFrom')).toBe(false)
      expect(hasControl(wrapper, 'backgroundOverlay')).toBe(false)
      expect(hasField(wrapper, 'background')).toBe(false)

      await control(wrapper, 'backgroundType').setValue('gradient')
      expect(hasControl(wrapper, 'backgroundColor')).toBe(false)
      expect(hasControl(wrapper, 'backgroundGradientFrom')).toBe(true)
      expect(hasControl(wrapper, 'backgroundGradientTo')).toBe(true)
      expect(hasField(wrapper, 'background')).toBe(false)

      await control(wrapper, 'backgroundType').setValue('image')
      expect(hasControl(wrapper, 'backgroundColor')).toBe(false)
      expect(hasControl(wrapper, 'backgroundGradientFrom')).toBe(false)
      expect(hasField(wrapper, 'background')).toBe(true)
      expect(hasControl(wrapper, 'backgroundFixed')).toBe(true)
      // 卡片表面控制不随背景类型消失，作者在任一模式下都能调
      expect(hasControl(wrapper, 'surfaceOpacity')).toBe(true)
      expect(hasControl(wrapper, 'surfaceBlur')).toBe(true)
    })

    it('submits all nine background keys in the save payload', async () => {
      getSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings, ...BACKGROUND }
      })
      const wrapper = await mountSettings()

      updateSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings, ...BACKGROUND }
      })
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
      for (const [key, value] of Object.entries(BACKGROUND)) {
        expect(payload[key]).toBe(value)
      }
    })

    it('updates each range readout as the slider moves', async () => {
      const wrapper = await mountSettings()

      await control(wrapper, 'backgroundType').setValue('image')
      await control(wrapper, 'backgroundOverlay').setValue('45')
      // 读数要读自己那一行：页面里还有玻璃感的两条滑杆，不能抓第一个 .range-row
      expect(control(wrapper, 'backgroundOverlay').element.parentElement?.textContent).toContain(
        '45%'
      )

      await control(wrapper, 'surfaceOpacity').setValue('72')
      expect(control(wrapper, 'surfaceOpacity').element.parentElement?.textContent).toContain('72%')

      await control(wrapper, 'surfaceBlur').setValue('18')
      expect(control(wrapper, 'surfaceBlur').element.parentElement?.textContent).toContain('18px')

      updateSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings }
      })
      await wrapper.find('form').trigger('submit')
      await flushPromises()
      expect(updateSettings.mock.calls[0]?.[0]).toMatchObject({
        backgroundOverlay: '45',
        surfaceOpacity: '72',
        surfaceBlur: '18'
      })
    })

    it('mirrors the chosen background and card translucency in the preview only', async () => {
      const wrapper = await mountSettings()

      await control(wrapper, 'backgroundType').setValue('solid')
      await control(wrapper, 'backgroundColor').setValue('#abcdef')
      expect(preview(wrapper).attributes('data-bg-type')).toBe('solid')
      expect(preview(wrapper).attributes('style')).toContain('rgb(171, 205, 239)')

      await control(wrapper, 'backgroundType').setValue('gradient')
      expect(preview(wrapper).attributes('style')).toContain('linear-gradient')

      await control(wrapper, 'backgroundType').setValue('image')
      // 显式给一张背景图，不依赖 store 默认值；再调遮罩看预览是否跟着叠色
      requireField(wrapper, 'background').vm.$emit(
        'update:modelValue',
        '/uploads/site-assets/background/hero.webp'
      )
      await nextTick()
      await control(wrapper, 'backgroundOverlay').setValue('50')
      const imageStyle = preview(wrapper).attributes('style') ?? ''
      expect(imageStyle).toContain('rgba(0, 0, 0, 0.5)')
      expect(imageStyle).toContain('background-size: cover')

      const card = wrapper.get('.bg-preview-card').attributes('style') ?? ''
      await control(wrapper, 'surfaceBlur').setValue('20')
      expect(wrapper.get('.bg-preview-card').attributes('style')).not.toBe(card)
      expect(wrapper.get('.bg-preview-card').attributes('style')).toContain('blur(20px)')

      // 预览是纯局部状态：不得改全局 DOM，也不得写 CSS 自定义属性
      const root = document.getElementById('selfblog')
      expect(root?.getAttribute('style') ?? '').not.toContain('--bg')
      expect(wrapper.find('.bg-preview').attributes('style')).not.toContain('--')
    })
  })

  describe('appearance panel', () => {
    /** 11 个外观键，顺序与面板里的渲染顺序一致。 */
    const APPEARANCE_KEYS = [
      'fontFamily',
      'baseFontSize',
      'bodyLineHeight',
      'readerWidth',
      'paragraphSpacing',
      'radiusScale',
      'shadowDepth',
      'layoutDensity',
      'homeHeroStyle',
      'postCoverStyle',
      'motionIntensity'
    ] as const

    /** 与 stores/site.ts 的 defaults、后端 defaults() 三处对齐的重置目标值。 */
    const APPEARANCE_DEFAULTS: Record<(typeof APPEARANCE_KEYS)[number], string> = {
      fontFamily: 'system',
      baseFontSize: '16',
      bodyLineHeight: '1.75',
      readerWidth: '760',
      paragraphSpacing: '1',
      radiusScale: 'default',
      shadowDepth: 'default',
      layoutDensity: 'default',
      homeHeroStyle: 'default',
      postCoverStyle: 'default',
      motionIntensity: 'default'
    }

    function control(wrapper: Awaited<ReturnType<typeof mountSettings>>, name: string) {
      const found = wrapper.find(`[data-setting="${name}"]`)
      if (!found.exists()) throw new Error(`missing control: ${name}`)
      return found
    }

    function appearancePanel(wrapper: Awaited<ReturnType<typeof mountSettings>>) {
      const panel = wrapper
        .findAll('section.settings-panel')
        .find((candidate) => candidate.find('.panel-heading h2').text() === '外观深度自定义')
      if (!panel) throw new Error('missing 外观深度自定义 panel')
      return panel
    }

    function value(wrapper: Awaited<ReturnType<typeof mountSettings>>, name: string): string {
      return (control(wrapper, name).element as HTMLInputElement | HTMLSelectElement).value
    }

    it('renders all eleven appearance controls with server-side bounds', async () => {
      const wrapper = await mountSettings()
      const panel = appearancePanel(wrapper)
      expect(panel.find('.panel-heading p').text()).toContain('只影响前台内容区')

      for (const key of APPEARANCE_KEYS) {
        expect(control(wrapper, key).exists()).toBe(true)
      }
      // 数值控件的区间与步长必须与后端 validateValue 的校验范围一致
      expect(control(wrapper, 'baseFontSize').attributes('min')).toBe('14')
      expect(control(wrapper, 'baseFontSize').attributes('max')).toBe('19')
      expect(control(wrapper, 'baseFontSize').attributes('step')).toBe('1')
      expect(control(wrapper, 'bodyLineHeight').attributes('min')).toBe('1.5')
      expect(control(wrapper, 'bodyLineHeight').attributes('max')).toBe('2.2')
      expect(control(wrapper, 'bodyLineHeight').attributes('step')).toBe('0.05')
      expect(control(wrapper, 'readerWidth').attributes('min')).toBe('640')
      expect(control(wrapper, 'readerWidth').attributes('max')).toBe('960')
      expect(control(wrapper, 'readerWidth').attributes('step')).toBe('10')
      expect(control(wrapper, 'paragraphSpacing').attributes('min')).toBe('0')
      expect(control(wrapper, 'paragraphSpacing').attributes('max')).toBe('2')
      expect(control(wrapper, 'paragraphSpacing').attributes('step')).toBe('0.1')

      // 7 个 select 的 option value 必须就是后端枚举的字面值
      const enumOptions: Record<string, string[]> = {
        fontFamily: ['system', 'sans', 'serif', 'mono'],
        radiusScale: ['none', 'sharp', 'default', 'soft', 'round'],
        shadowDepth: ['flat', 'default', 'deep'],
        layoutDensity: ['compact', 'default', 'comfortable'],
        homeHeroStyle: ['default', 'compact', 'minimal', 'off'],
        postCoverStyle: ['default', 'wide', 'hidden'],
        motionIntensity: ['off', 'subtle', 'default', 'lively']
      }
      for (const [key, values] of Object.entries(enumOptions)) {
        expect(
          control(wrapper, key)
            .findAll('option')
            .map((option) => option.attributes('value'))
        ).toEqual(values)
      }
    })

    it('shows the loaded appearance values and their readouts', async () => {
      getSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: {
          ...useSiteStore().settings,
          fontFamily: 'serif',
          baseFontSize: '18',
          bodyLineHeight: '2',
          readerWidth: '820',
          paragraphSpacing: '0.5',
          radiusScale: 'soft',
          shadowDepth: 'deep',
          layoutDensity: 'compact',
          homeHeroStyle: 'minimal',
          postCoverStyle: 'wide',
          motionIntensity: 'subtle'
        }
      })
      const wrapper = await mountSettings()

      expect(value(wrapper, 'fontFamily')).toBe('serif')
      expect(value(wrapper, 'baseFontSize')).toBe('18')
      expect(value(wrapper, 'motionIntensity')).toBe('subtle')
      // 滑杆读数：px / em 单位只出现在读数上，不写回表单
      expect(control(wrapper, 'baseFontSize').element.parentElement?.textContent).toContain('18 px')
      expect(control(wrapper, 'readerWidth').element.parentElement?.textContent).toContain('820 px')
      expect(control(wrapper, 'paragraphSpacing').element.parentElement?.textContent).toContain(
        '0.5 em'
      )
      expect(control(wrapper, 'bodyLineHeight').element.parentElement?.textContent).toContain('2')
    })

    it('submits the edited font family and base font size in the save payload', async () => {
      const wrapper = await mountSettings()

      await control(wrapper, 'fontFamily').setValue('serif')
      await control(wrapper, 'baseFontSize').setValue('18')

      updateSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings, fontFamily: 'serif', baseFontSize: '18' }
      })
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
      expect(payload.fontFamily).toBe('serif')
      expect(payload.baseFontSize).toBe('18')
      // 字符串契约：滑杆写回的仍是字符串，不是 number
      expect(typeof payload.baseFontSize).toBe('string')
    })

    it('resets the eleven keys locally and never saves on its own', async () => {
      getSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: {
          ...useSiteStore().settings,
          fontFamily: 'mono',
          baseFontSize: '19',
          bodyLineHeight: '2.2',
          readerWidth: '960',
          paragraphSpacing: '2',
          radiusScale: 'round',
          shadowDepth: 'flat',
          layoutDensity: 'comfortable',
          homeHeroStyle: 'off',
          postCoverStyle: 'hidden',
          motionIntensity: 'lively'
        }
      })
      const wrapper = await mountSettings()
      const store = useSiteStore()
      expect(value(wrapper, 'fontFamily')).toBe('mono')

      // 重置前先给全局 store 快照：重置只能改本地表单，不得写 store。
      const storeBefore = { ...store.settings }

      await wrapper.get('[data-action="reset-appearance"]').trigger('click')
      await flushPromises()

      // 只改本地表单：既不自动保存，也不改全局 store
      expect(updateSettings).not.toHaveBeenCalled()
      expect({ ...store.settings }).toEqual(storeBefore)
      for (const key of APPEARANCE_KEYS) {
        expect(value(wrapper, key)).toBe(APPEARANCE_DEFAULTS[key])
      }

      // 重置后的值确实会随作者主动保存一起提交
      updateSettings.mockResolvedValue({ code: 200, message: 'OK', data: { ...store.settings } })
      await wrapper.find('form').trigger('submit')
      await flushPromises()
      const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
      for (const key of APPEARANCE_KEYS) {
        expect(payload[key]).toBe(APPEARANCE_DEFAULTS[key])
      }
    })
  })
})
