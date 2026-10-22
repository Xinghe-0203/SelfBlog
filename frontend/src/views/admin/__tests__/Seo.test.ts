import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Seo from '../Seo.vue'
import type { SeoOverview, SeoPostRow } from '@/types/seo'

const { getOverview, getPosts, getDetail, update, generate, batchGenerate, remove, proofread } =
  vi.hoisted(() => ({
    getOverview: vi.fn(),
    getPosts: vi.fn(),
    getDetail: vi.fn(),
    update: vi.fn(),
    generate: vi.fn(),
    batchGenerate: vi.fn(),
    remove: vi.fn(),
    proofread: vi.fn()
  }))

vi.mock('@/api/seo', () => ({
  seoApi: { getOverview, getPosts, getDetail, update, generate, batchGenerate, remove }
}))

vi.mock('@/api/llm', () => ({ llmApi: { proofread } }))

vi.mock('vue-router', () => ({
  useRouter: () => ({ currentRoute: { value: { query: {} } } })
}))

enableAutoUnmount(afterEach)

const OVERVIEW: SeoOverview = {
  llmConfigured: true,
  llmEnabled: true,
  autoOnPublish: false,
  model: 'gpt-4o-mini',
  totalPosts: 12,
  withSeo: 3,
  coveragePercent: 25,
  missingDescription: 4,
  longTitle: 2,
  duplicateTitle: 1,
  missingCover: 5
}

const ROW: SeoPostRow = {
  postId: 7,
  title: 'Spring Boot 事务边界',
  status: 1,
  hasSeo: true,
  seoTitle: 'Spring Boot 事务边界与自调用陷阱',
  seoDescription: '描述',
  seoKeywords: 'Spring Boot,事务',
  ogImageUrl: null,
  source: 'llm',
  generatedAt: '2026-10-20T10:00:00',
  updateTime: '2026-10-20T10:00:00',
  issues: ['longTitle', 'missingCover']
}

function pageOf(records: SeoPostRow[]) {
  return { code: 200, message: 'OK', data: { records, total: records.length, pages: 1 } }
}

async function setup() {
  const wrapper = mount(Seo, {
    global: {
      plugins: [createPinia()],
      stubs: {
        teleport: true,
        transition: true,
        RouterLink: { template: '<a><slot /></a>' }
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('SEO admin page', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getOverview.mockReset()
    getPosts.mockReset()
    getDetail.mockReset()
    update.mockReset()
    generate.mockReset()
    batchGenerate.mockReset()
    remove.mockReset()
    proofread.mockReset()
    getOverview.mockResolvedValue({ code: 200, message: 'OK', data: OVERVIEW })
    getPosts.mockResolvedValue(pageOf([ROW]))
  })

  it('renders the coverage metrics and every issue chip from the backend codes', async () => {
    const wrapper = await setup()

    expect(wrapper.text()).toContain('25%')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('4')
    const chips = wrapper.findAll('.issue-chip').map((chip) => chip.text())
    expect(chips).toEqual(['标题过长', '缺少封面'])
    expect(wrapper.text()).toContain('模型生成')
  })

  it('blocks generation and explains why when the model endpoint is not configured', async () => {
    getOverview.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { ...OVERVIEW, llmConfigured: false }
    })
    const wrapper = await setup()

    expect(wrapper.find('.notice').exists()).toBe(true)
    expect(wrapper.get('.notice').text()).toContain('尚未配置可用的模型接口')
    const buttons = wrapper.findAll('button')
    const batch = buttons.find((button) => button.text().includes('批量生成'))
    const generateButton = buttons.find((button) => button.text().includes('生成'))
    expect(batch?.attributes('disabled')).toBeDefined()
    expect(generateButton?.attributes('disabled')).toBeDefined()
  })

  it('keeps only the newest list response when the filters are applied twice', async () => {
    // 用显式 deferred 控制两次请求的完成顺序，避免依赖 microtask 调度。
    const pending: Array<(value: unknown) => void> = []
    getPosts.mockImplementation(
      () => new Promise((resolve) => pending.push(resolve as (value: unknown) => void))
    )

    const wrapper = await setup()
    pending[0]?.(pageOf([ROW]))
    await flushPromises()

    const keywordInput = wrapper.get('[data-filter="keyword"]')
    await keywordInput.setValue('Spring')
    await keywordInput.trigger('keyup.enter')
    await flushPromises()
    await keywordInput.trigger('keyup.enter')
    await flushPromises()

    const fast = pageOf([{ ...ROW, postId: 2, title: '新结果' }])
    const slow = pageOf([{ ...ROW, postId: 1, title: '旧结果' }])
    // 后发的请求先回：表格必须写新结果。
    pending[2]?.(fast)
    await flushPromises()
    expect(wrapper.text()).toContain('新结果')
    // 先发的请求后回：晚到的旧响应必须被丢弃。
    pending[1]?.(slow)
    await flushPromises()
    expect(wrapper.text()).toContain('新结果')
    expect(wrapper.text()).not.toContain('旧结果')
  })

  it('loads a post detail into the editor and saves blank fields as null', async () => {
    getDetail.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: {
        ...ROW,
        postSummary: '摘要正文',
        effectiveTitle: 'Spring Boot 事务边界',
        effectiveDescription: '描述'
      }
    })
    update.mockResolvedValue({ code: 200, message: 'OK', data: ROW })
    const wrapper = await setup()

    const editButton = wrapper.findAll('.row-actions .link-button')[0]
    await editButton?.trigger('click')
    await flushPromises()

    const titleInput = wrapper.get('[data-editor="seoTitle"]')
    expect((titleInput.element as HTMLInputElement).value).toBe('Spring Boot 事务边界与自调用陷阱')

    await titleInput.setValue('   ')
    const footerButtons = wrapper.findAll('.modal-footer button')
    await footerButtons[footerButtons.length - 1]?.trigger('click')
    await flushPromises()

    expect(update).toHaveBeenCalledWith(7, {
      seoTitle: null,
      seoDescription: '描述',
      seoKeywords: 'Spring Boot,事务',
      ogImageUrl: null
    })
  })

  it('shows the server message when a single generation fails', async () => {
    generate.mockRejectedValue({ userMessage: '上游模型返回 401' })
    const wrapper = await setup()

    const generateButton = wrapper
      .findAll('.row-actions .link-button')
      .find((button) => button.text() === '生成')
    await generateButton?.trigger('click')
    await flushPromises()

    expect(wrapper.get('.alert').text()).toBe('上游模型返回 401')
  })

  /**
   * AI 校对是**只读**功能：弹窗展示问题清单，绝不自动改正文、也不写库。
   * 这里验证 issues 的渲染（类型/严重度/摘录/建议）与截断提示。
   */
  it('renders proofread issues with type and severity without touching the article', async () => {
    proofread.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: {
        postId: 7,
        issues: [
          {
            type: 'typo',
            excerpt: '事务边界与自调用陷井',
            suggestion: '改为「陷阱」',
            severity: 'high'
          },
          {
            type: 'wording',
            excerpt: '这个写法非常非常地不好',
            suggestion: '精简为「这种写法不好」',
            severity: 'low'
          }
        ],
        truncated: true,
        model: 'deepseek-flash',
        latencyMs: 2100
      }
    })
    const wrapper = await setup()

    await wrapper.get('[data-action="ai-proofread"]').trigger('click')
    await flushPromises()

    // 工具栏入口默认校对列表第一篇。
    expect(proofread).toHaveBeenCalledWith(7)
    const items = wrapper.findAll('.issue-item')
    expect(items).toHaveLength(2)
    expect(items[0]?.attributes('data-severity')).toBe('high')
    expect(items[0]?.get('.issue-type').text()).toBe('错别字')
    expect(items[0]?.get('.issue-severity').text()).toBe('重要')
    expect(items[0]?.get('.issue-excerpt').text()).toBe('事务边界与自调用陷井')
    expect(items[0]?.get('.issue-suggestion').text()).toBe('建议：改为「陷阱」')
    expect(items[1]?.get('.issue-severity').text()).toBe('轻微')
    expect(wrapper.get('[data-truncated]').text()).toContain('6000')

    // 只读：不触发任何 SEO 写入，详情编辑面板也不会被打开。
    expect(update).not.toHaveBeenCalled()
    expect(generate).not.toHaveBeenCalled()
    expect(wrapper.find('[data-editor="seoTitle"]').exists()).toBe(false)
  })

  it('shows the empty and failure states of the proofread dialog', async () => {
    proofread.mockResolvedValue({
      code: 200,
      message: 'OK',
      data: { postId: 7, issues: [], truncated: false, model: 'm', latencyMs: 10 }
    })
    const wrapper = await setup()
    await wrapper.get('[data-action="ai-proofread"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-proofread-empty]').text()).toBe('没有发现问题。')
    expect(wrapper.find('[data-truncated]').exists()).toBe(false)

    proofread.mockRejectedValue({ userMessage: '模型返回的内容不符合要求，请重试或手动填写' })
    await wrapper.get('.modal-footer .primary-button').trigger('click')
    await flushPromises()
    expect(wrapper.get('.modal-body .alert, .proofread .alert').text()).toBe(
      '模型返回的内容不符合要求，请重试或手动填写'
    )
  })
})
