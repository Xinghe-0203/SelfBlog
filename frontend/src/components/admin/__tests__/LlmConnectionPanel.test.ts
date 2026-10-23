import { describe, it, expect, beforeEach, vi } from 'vitest'
import { reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import LlmConnectionPanel, { type LlmForm } from '../LlmConnectionPanel.vue'
import { useSiteStore } from '@/stores/site'

/**
 * 模型连接面板从「网站设置」搬到「SEO 管理」页顶部后，这组用例跟着一起搬过来。
 *
 * 组件与宿主页共用同一份设置对象（`form` prop），所以这里直接传一个 reactive
 * 表单进去，断言面板自己的保存动作只提交 11 个 llm* 键。
 */
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

const MASK = '••••••••'

function llmForm(overrides: Partial<LlmForm> = {}): LlmForm {
  return reactive<LlmForm>({
    llmEnabled: 'false',
    llmProvider: 'deepseek',
    llmBaseUrl: '',
    llmApiKey: '',
    llmModel: '',
    llmTimeoutSeconds: '30',
    llmMaxTokens: '0',
    llmThinking: 'disabled',
    llmReasoningEffort: 'low',
    llmAutoOnPublish: 'false',
    llmCommentModeration: 'false',
    ...overrides
  })
}

async function mountPanel(overrides: Partial<LlmForm> = {}) {
  const form = llmForm(overrides)
  const wrapper = mount(LlmConnectionPanel, {
    props: { form },
    global: { plugins: [createPinia()] }
  })
  await flushPromises()
  return { wrapper, form }
}

function control(wrapper: Awaited<ReturnType<typeof mountPanel>>['wrapper'], name: string) {
  const found = wrapper.find(`[data-setting="${name}"]`)
  if (!found.exists()) throw new Error(`missing control: ${name}`)
  return found
}

describe('LlmConnectionPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getSettings.mockReset()
    updateSettings.mockReset()
    testConnection.mockReset()
    listModels.mockReset()
    listModels.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: {
        models: [
          { id: 'deepseek-flash', name: 'DeepSeek Flash', supportsImage: false },
          { id: 'deepseek-v4-pro', name: 'DeepSeek Pro', supportsImage: true }
        ],
        source: 'upstream'
      }
    })
    updateSettings.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ...useSiteStore().settings, llmApiKey: MASK }
    })
  })

  it('renders every llm setting and derives the key badge from the mask sentinel', async () => {
    const { wrapper } = await mountPanel({
      llmEnabled: 'true',
      llmBaseUrl: 'https://api.example.com/v1',
      llmApiKey: MASK,
      llmModel: 'gpt-4o-mini',
      llmTimeoutSeconds: '45',
      llmAutoOnPublish: 'true'
    })

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
    const { wrapper } = await mountPanel()
    expect((control(wrapper, 'llmApiKey').element as HTMLInputElement).value).toBe('')
    expect(wrapper.get('[data-llm-key]').attributes('data-llm-key')).toBe('empty')
    expect(wrapper.get('[data-llm-key]').text()).toBe('未配置')
  })

  it('round-trips the mask unchanged so saving never wipes a stored key', async () => {
    const { wrapper } = await mountPanel({
      llmApiKey: MASK,
      llmBaseUrl: 'https://api.example.com'
    })

    await wrapper.get('[data-action="save-llm"]').trigger('click')
    await flushPromises()

    // 关键不变量：不动密钥输入框时，提交的必须仍是哨兵串本身。
    // 若前端把它换成空串或占位符，后端会理解为"清除密钥"。
    const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
    expect(payload.llmApiKey).toBe(MASK)
    expect(payload.llmBaseUrl).toBe('https://api.example.com')
    expect(wrapper.find('[data-llm-save="saved"]').exists()).toBe(true)
  })

  it('only submits the eleven llm keys so the host page settings stay untouched', async () => {
    const { wrapper } = await mountPanel({ llmMaxTokens: '4096', llmThinking: 'enabled' })

    await wrapper.get('[data-action="save-llm"]').trigger('click')
    await flushPromises()

    const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
    expect(Object.keys(payload).sort()).toEqual(
      [
        'llmApiKey',
        'llmAutoOnPublish',
        'llmBaseUrl',
        'llmCommentModeration',
        'llmEnabled',
        'llmMaxTokens',
        'llmModel',
        'llmProvider',
        'llmReasoningEffort',
        'llmThinking',
        'llmTimeoutSeconds'
      ].sort()
    )
    expect(String(payload.llmMaxTokens)).toBe('4096')
  })

  it('tests the saved configuration and reports latency on success', async () => {
    testConnection.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ok: true, message: '连接成功', latencyMs: 412, model: 'gpt-4o-mini' }
    })
    const { wrapper } = await mountPanel()

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
    const { wrapper } = await mountPanel()

    await wrapper.get('[data-action="test-llm"]').trigger('click')
    await flushPromises()

    const result = wrapper.get('.llm-test')
    expect(result.attributes('data-ok')).toBe('false')
    expect(result.text()).toContain('请求超时')
  })

  it('fills the DeepSeek defaults without touching the stored key', async () => {
    const { wrapper, form } = await mountPanel({ llmApiKey: MASK })

    await wrapper.get('[data-action="llm-preset"]').trigger('click')
    await flushPromises()

    expect((control(wrapper, 'llmBaseUrl').element as HTMLInputElement).value).toBe(
      'https://api.deepseek.com'
    )
    expect((control(wrapper, 'llmModel').element as HTMLInputElement).value).toBe('deepseek-flash')
    expect((control(wrapper, 'llmProvider').element as HTMLSelectElement).value).toBe('deepseek')

    await wrapper.get('[data-action="save-llm"]').trigger('click')
    await flushPromises()
    const payload = updateSettings.mock.calls[0]?.[0] as Record<string, string>
    expect(payload.llmBaseUrl).toBe('https://api.deepseek.com')
    expect(payload.llmModel).toBe('deepseek-flash')
    // 预设绝不能顺手改写密钥：哨兵串必须原样往返
    expect(payload.llmApiKey).toBe(MASK)
    expect(form.llmApiKey).toBe(MASK)
  })

  it('keeps the reasoning effort select disabled until thinking is enabled', async () => {
    const { wrapper } = await mountPanel()
    const effort = () => control(wrapper, 'llmReasoningEffort')

    expect(effort().attributes('disabled')).toBeDefined()

    await control(wrapper, 'llmThinking').setValue(true)
    expect(effort().attributes('disabled')).toBeUndefined()

    // 自定义服务商不发思考字段，下拉重新禁用
    await control(wrapper, 'llmProvider').setValue('custom')
    expect(effort().attributes('disabled')).toBeDefined()
  })

  it('exposes the server-side bounds of the numeric provider keys', async () => {
    const { wrapper } = await mountPanel()

    expect(control(wrapper, 'llmMaxTokens').attributes('min')).toBe('0')
    expect(control(wrapper, 'llmMaxTokens').attributes('max')).toBe('131072')
    expect(control(wrapper, 'llmTimeoutSeconds').attributes('min')).toBe('5')
    expect(control(wrapper, 'llmTimeoutSeconds').attributes('max')).toBe('120')
  })

  it('renders the model candidates from the probe and falls back to a manual input', async () => {
    listModels.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: {
        models: [
          { id: 'deepseek-flash', name: 'DeepSeek Flash', supportsImage: false },
          { id: 'deepseek-v4-pro', name: 'DeepSeek Pro', supportsImage: true }
        ],
        source: 'fallback'
      }
    })
    const { wrapper } = await mountPanel()

    expect(listModels).toHaveBeenCalledTimes(1)
    const options = control(wrapper, 'llmModelSelect').findAll('option')
    expect(options.map((option) => option.attributes('value'))).toEqual([
      'deepseek-flash',
      'deepseek-v4-pro'
    ])
    // source=upstream 才是"上游实时列表"，回落时要如实标注
    expect(wrapper.text()).toContain('内置候选')
    // 手填 input 始终存在，候选只是便利
    expect(control(wrapper, 'llmModel').exists()).toBe(true)
  })

  it('silently drops the candidate select when the probe request fails', async () => {
    listModels.mockRejectedValue(new Error('network down'))
    const { wrapper } = await mountPanel()

    expect(wrapper.find('[data-setting="llmModelSelect"]').exists()).toBe(false)
    expect(control(wrapper, 'llmModel').exists()).toBe(true)
    // 探测失败不该把面板标成错误
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })
})
