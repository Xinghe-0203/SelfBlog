import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import Health from '../Health.vue'
import { llmApi } from '@/api/llm'

/**
 * 内容体检页（F 块）：只读。断言评分、摘要、findings 的严重度徽章与建议，
 * 以及未配置模型 / 没有已发布文章时后端 message 的透出。
 */
vi.mock('@/api/llm', () => ({ llmApi: { healthReport: vi.fn() } }))
enableAutoUnmount(afterEach)

const response = <T>(data: T) => ({ code: 200, message: 'ok', data })

function mountHealth() {
  return mount(Health, { global: { stubs: { teleport: true, transition: true } } })
}

beforeEach(() => {
  vi.resetAllMocks()
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('content health report', () => {
  it('renders the score, summary and every finding with its severity and action', async () => {
    vi.mocked(llmApi.healthReport).mockResolvedValue(
      response({
        generatedAt: '2026-10-22T13:30:00',
        score: 72,
        summary: '文章结构整体清晰，但代码示例偏少，部分标题缺少关键词，建议补充小节导语。',
        findings: [
          {
            title: '多数文章缺少小标题',
            detail: '抽样的 40 篇里有 26 篇只有一级标题，长文可读性下降。',
            severity: 'high',
            action: '为超过 1500 字的文章补充二级标题。',
            count: 26
          },
          {
            title: '代码块缺少语言标注',
            detail: '部分代码块没有标注语言，阅读器无法高亮。',
            severity: 'medium',
            action: '在代码块开头上补上语言名。',
            count: 12
          },
          {
            title: '摘要长度略短',
            detail: '部分摘要不足 40 字，列表页预览信息量不足。',
            severity: 'low',
            action: '把摘要补到 80-120 字。',
            count: null
          }
        ],
        sampledPosts: 40,
        totalPosts: 15,
        model: 'deepseek-flash',
        latencyMs: 8400
      })
    )
    const wrapper = mountHealth()

    expect(wrapper.get('[data-action="run-health-report"]').text()).toContain('开始体检')
    await wrapper.get('[data-action="run-health-report"]').trigger('click')
    await flushPromises()

    expect(llmApi.healthReport).toHaveBeenCalledWith({ limit: 40 })
    expect(wrapper.get('[data-health-score]').text()).toContain('72')
    expect(wrapper.get('[data-health-score]').attributes('data-score-level')).toBe('fair')
    expect(wrapper.get('.score-summary').text()).toContain('文章结构整体清晰')
    expect(wrapper.get('.score-meta').text()).toContain('抽样 40 篇 / 共 15 篇已发布')
    expect(wrapper.get('.score-meta').text()).toContain('deepseek-flash · 8400ms')

    const findings = wrapper.findAll('[data-finding-index]')
    expect(findings).toHaveLength(3)
    expect(findings[0]?.attributes('data-severity')).toBe('high')
    expect(findings[0]?.get('.severity-badge').text()).toBe('重要')
    expect(findings[0]?.get('h3').text()).toBe('多数文章缺少小标题')
    expect(findings[0]?.get('.finding-count').text()).toBe('26 处')
    expect(findings[0]?.get('.finding-action').text()).toBe(
      '建议：为超过 1500 字的文章补充二级标题。'
    )
    expect(findings[1]?.get('.severity-badge').text()).toBe('中等')
    expect(findings[2]?.get('.severity-badge').text()).toBe('轻微')
    // count 为 null 时不渲染计数徽章。
    expect(findings[2]?.find('.finding-count').exists()).toBe(false)
    expect(wrapper.get('.success').text()).toBe('体检完成，报告已更新。')
  })

  it('sends the chosen sample size and surfaces the backend message on failure', async () => {
    vi.mocked(llmApi.healthReport).mockRejectedValue({
      response: { data: { message: '还没有已发布的文章' } }
    })
    const wrapper = mountHealth()

    expect(wrapper.get('.empty').text()).toContain('还没有体检报告')
    await wrapper.get('.limit-field select').setValue('20')
    await wrapper.get('[data-action="run-health-report"]').trigger('click')
    await flushPromises()

    expect(llmApi.healthReport).toHaveBeenCalledWith({ limit: 20 })
    expect(wrapper.get('.alert').text()).toBe('还没有已发布的文章')
    // 失败不留半截报告。
    expect(wrapper.find('[data-health-score]').exists()).toBe(false)
  })
})
