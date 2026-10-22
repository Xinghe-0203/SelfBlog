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
    const groupTitles = wrapper.findAll('.panel-heading h2').map((heading) => heading.text())
    expect(groupTitles).toEqual([
      '站点资料',
      '后台与访客',
      '评论与音乐',
      'AI 辅助与 SEO',
      '视觉主题',
      '页面背景'
    ])
    expect(wrapper.findAll('form')).toHaveLength(1)
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
      // tsconfig 的 lib 是 ES2020，没有 Array.prototype.at；用长度取最后一节。
      const panels = wrapper.findAll('section.settings-panel')
      const panel = panels[panels.length - 1]
      expect(panel?.find('.panel-heading h2').text()).toBe('页面背景')
      expect(panel?.find('.panel-desc').text()).toContain('卡片的不透明度与模糊')

      // 类型选择 + 两个卡片表面控制（渐变/背景图分支之外始终可见）
      expect(control(wrapper, 'backgroundType').exists()).toBe(true)
      expect(control(wrapper, 'surfaceOpacity').attributes('min')).toBe('60')
      expect(control(wrapper, 'surfaceOpacity').attributes('max')).toBe('100')
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
      expect(wrapper.get('.range-row code').text()).toBe('45%')

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

  describe('AI / LLM panel', () => {
    const MASK = '••••••••'

    function control(wrapper: Awaited<ReturnType<typeof mountSettings>>, name: string) {
      const found = wrapper.find(`[data-setting="${name}"]`)
      if (!found.exists()) throw new Error(`missing control: ${name}`)
      return found
    }

    it('renders every llm setting and derives the key badge from the mask sentinel', async () => {
      getSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: {
          ...useSiteStore().settings,
          llmEnabled: 'true',
          llmBaseUrl: 'https://api.example.com/v1',
          llmApiKey: MASK,
          llmModel: 'gpt-4o-mini',
          llmTimeoutSeconds: '45',
          llmAutoOnPublish: 'true'
        }
      })
      const wrapper = await mountSettings()

      for (const name of [
        'llmEnabled',
        'llmBaseUrl',
        'llmApiKey',
        'llmModel',
        'llmTimeoutSeconds',
        'llmAutoOnPublish'
      ]) {
        expect(control(wrapper, name).exists()).toBe(true)
      }
      expect((control(wrapper, 'llmBaseUrl').element as HTMLInputElement).value).toBe(
        'https://api.example.com/v1'
      )
      // v-model 写的是 DOM property，不是 attribute，必须读 element.value。
      expect((control(wrapper, 'llmApiKey').element as HTMLInputElement).value).toBe(MASK)
      // 已配置 = 服务端回的就是哨兵串；此时徽标必须是"已配置"，且不能把掩码当真实密钥展示
      const badge = wrapper.get('[data-llm-key]')
      expect(badge.attributes('data-llm-key')).toBe('configured')
      expect(badge.text()).toBe('已配置')
    })

    it('shows an unconfigured key as empty rather than the mask', async () => {
      const wrapper = await mountSettings()
      expect((control(wrapper, 'llmApiKey').element as HTMLInputElement).value).toBe('')
      expect(wrapper.get('[data-llm-key]').attributes('data-llm-key')).toBe('empty')
      expect(wrapper.get('[data-llm-key]').text()).toBe('未配置')
    })

    it('round-trips the mask unchanged so saving never wipes a stored key', async () => {
      getSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings, llmApiKey: MASK, llmBaseUrl: 'https://api.example.com' }
      })
      const wrapper = await mountSettings()
      updateSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings, llmApiKey: MASK }
      })

      await wrapper.find('form').trigger('submit')
      await flushPromises()

      // 关键不变量：不动密钥输入框时，提交的必须仍是哨兵串本身。
      // 若前端把它换成空串或占位符，后端会理解为"清除密钥"。
      const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
      expect(payload.llmApiKey).toBe(MASK)
      expect(payload.llmBaseUrl).toBe('https://api.example.com')
    })

    it('tests the saved configuration and reports latency on success', async () => {
      testConnection.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ok: true, message: '连接成功', latencyMs: 412, model: 'gpt-4o-mini' }
      })
      const wrapper = await mountSettings()

      await wrapper.get('[data-action="test-llm"]').trigger('click')
      await flushPromises()

      expect(testConnection).toHaveBeenCalledTimes(1)
      const result = wrapper.get('.llm-test')
      expect(result.attributes('data-ok')).toBe('true')
      expect(result.text()).toContain('连接成功')
      expect(result.text()).toContain('412ms')
      expect(result.text()).toContain('gpt-4o-mini')
    })

    it('surfaces a failed connection attempt without throwing', async () => {
      testConnection.mockRejectedValue({ userMessage: '请求超时' })
      const wrapper = await mountSettings()

      await wrapper.get('[data-action="test-llm"]').trigger('click')
      await flushPromises()

      const result = wrapper.get('.llm-test')
      expect(result.attributes('data-ok')).toBe('false')
      expect(result.text()).toContain('请求超时')
    })

    it('fills the DeepSeek defaults without touching the stored key', async () => {
      getSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings, llmApiKey: MASK }
      })
      const wrapper = await mountSettings()

      await wrapper.get('[data-action="llm-preset"]').trigger('click')
      await flushPromises()

      expect((control(wrapper, 'llmBaseUrl').element as HTMLInputElement).value).toBe(
        'https://api.deepseek.com'
      )
      expect((control(wrapper, 'llmModel').element as HTMLInputElement).value).toBe(
        'deepseek-flash'
      )
      expect((control(wrapper, 'llmProvider').element as HTMLSelectElement).value).toBe('deepseek')

      updateSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings, llmApiKey: MASK }
      })
      await wrapper.find('form').trigger('submit')
      await flushPromises()
      const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
      expect(payload.llmBaseUrl).toBe('https://api.deepseek.com')
      expect(payload.llmModel).toBe('deepseek-flash')
      // 预设绝不能顺手改写密钥：哨兵串必须原样往返
      expect(payload.llmApiKey).toBe(MASK)
    })

    it('keeps the reasoning effort select disabled until thinking is enabled', async () => {
      const wrapper = await mountSettings()
      const effort = () => control(wrapper, 'llmReasoningEffort')

      expect(effort().attributes('disabled')).toBeDefined()

      await control(wrapper, 'llmThinking').setValue(true)
      expect(effort().attributes('disabled')).toBeUndefined()

      // 自定义服务商不发思考字段，下拉重新禁用
      await control(wrapper, 'llmProvider').setValue('custom')
      expect(effort().attributes('disabled')).toBeDefined()
    })

    it('submits the five new provider keys with their server-side bounds', async () => {
      const wrapper = await mountSettings()

      expect(control(wrapper, 'llmMaxTokens').attributes('min')).toBe('0')
      expect(control(wrapper, 'llmMaxTokens').attributes('max')).toBe('131072')
      expect(control(wrapper, 'llmCommentModeration').exists()).toBe(true)

      await control(wrapper, 'llmMaxTokens').setValue('4096')
      await control(wrapper, 'llmCommentModeration').setValue(true)
      await control(wrapper, 'llmThinking').setValue(true)
      await control(wrapper, 'llmReasoningEffort').setValue('high')

      updateSettings.mockResolvedValue({
        code: 200,
        message: 'OK',
        data: { ...useSiteStore().settings }
      })
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(updateSettings.mock.calls[0]?.[0]).toMatchObject({
        llmCommentModeration: 'true',
        llmThinking: 'enabled',
        llmReasoningEffort: 'high'
      })
      // number 输入框的 v-model 会把值转成数字（与既有 llmTimeoutSeconds 一致），
      // 这里只断言它确实带上了作者填的数值。
      const payload = updateSettings.mock.calls[0]?.[0] as Record<string, unknown>
      expect(String(payload.llmMaxTokens)).toBe('4096')
    })

    it('renders the model candidates from the probe and falls back to a manual input', async () => {
      const wrapper = await mountSettings()

      expect(listModels).toHaveBeenCalledTimes(1)
      const options = control(wrapper, 'llmModelSelect').findAll('option')
      expect(options.map((option) => option.attributes('value'))).toEqual([
        'deepseek-flash',
        'deepseek-v4-pro'
      ])
      expect(wrapper.text()).toContain('内置候选')
      // 手填 input 始终存在，候选只是便利
      expect(control(wrapper, 'llmModel').exists()).toBe(true)
    })

    it('silently drops the candidate select when the probe request fails', async () => {
      listModels.mockRejectedValue(new Error('network down'))
      const wrapper = await mountSettings()

      expect(wrapper.find('[data-setting="llmModelSelect"]').exists()).toBe(false)
      expect(control(wrapper, 'llmModel').exists()).toBe(true)
      // 探测失败不该把设置页标成错误
      expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    })
  })
})
