import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import Comments from '../Comments.vue'
import { publicInteractionApi } from '@/api/public-interactions'
import { llmApi } from '@/api/llm'

/**
 * 评论审核页：AI 只给建议，**不改评论状态**。
 * 审核结果挂在本条评论上展示徽章，改状态仍走既有的展示/隐藏/标记垃圾按钮。
 */
vi.mock('@/api/public-interactions', () => ({
  publicInteractionApi: {
    getAdminComments: vi.fn(),
    updateCommentStatus: vi.fn(),
    deleteComment: vi.fn()
  }
}))
vi.mock('@/api/llm', () => ({
  llmApi: { moderationCheck: vi.fn(), moderationScan: vi.fn() }
}))
vi.mock('@/utils/toast', () => ({ toast: { success: vi.fn(), warning: vi.fn(), error: vi.fn() } }))
enableAutoUnmount(afterEach)

const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
const comment = (id: number, content: string, status = 1) => ({
  id,
  postId: 9,
  postTitle: '测试文章',
  nickname: `访客${id}`,
  content,
  status,
  createTime: '2026-10-22 10:00:00'
})

async function setup() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div />' } }]
  })
  await router.push('/')
  const wrapper = mount(Comments, {
    global: {
      plugins: [createPinia(), router],
      stubs: { teleport: true, transition: true, RouterLink: { template: '<a><slot /></a>' } }
    }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(publicInteractionApi.getAdminComments).mockResolvedValue(
    response({
      records: [comment(5, '加微信 xxx 领取资料'), comment(6, '写得很好，学习了')],
      total: 2,
      size: 20,
      current: 1,
      pages: 1
    })
  )
  vi.mocked(publicInteractionApi.updateCommentStatus).mockResolvedValue(response(undefined))
  vi.mocked(publicInteractionApi.deleteComment).mockResolvedValue(response(undefined))
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('comment AI moderation', () => {
  it('renders a verdict badge with reason and categories for a checked comment', async () => {
    vi.mocked(llmApi.moderationCheck).mockResolvedValue(
      response({
        verdict: 'block',
        reason: '疑似导流广告',
        categories: ['ad'],
        model: 'deepseek-flash',
        latencyMs: 640
      })
    )
    const wrapper = await setup()
    expect(wrapper.findAll('.comment-card')).toHaveLength(2)
    expect(wrapper.find('[data-verdict-row]').exists()).toBe(false)

    await wrapper.get('[data-action="ai-check"]').trigger('click')
    await flushPromises()

    // 只审核了第一条，所以只有一个徽章。
    expect(llmApi.moderationCheck).toHaveBeenCalledWith({ commentId: 5, kind: 'guest' })
    expect(wrapper.findAll('[data-verdict-row]')).toHaveLength(1)
    expect(wrapper.get('.verdict-badge').attributes('data-verdict')).toBe('block')
    expect(wrapper.get('.verdict-badge').text()).toBe('建议拦截')
    expect(wrapper.get('.verdict-reason').text()).toBe('疑似导流广告')
    expect(wrapper.get('.verdict-category').text()).toBe('广告')
    // 审核不改状态：既有的「展示中」徽章仍在，且没有发状态更新请求。
    expect(wrapper.get('.status').text()).toBe('展示中')
    expect(publicInteractionApi.updateCommentStatus).not.toHaveBeenCalled()
  })

  it('marks only flagged items from a batch scan and reports the counts', async () => {
    vi.mocked(llmApi.moderationScan).mockResolvedValue(
      response({
        scanned: 20,
        flagged: 1,
        items: [
          {
            commentId: 6,
            kind: 'guest',
            nickname: '访客6',
            excerpt: '写得很好',
            verdict: 'flag',
            reason: '疑似刷屏',
            categories: ['spam']
          }
        ]
      })
    )
    const wrapper = await setup()

    await wrapper.get('[data-action="ai-scan"]').trigger('click')
    await flushPromises()

    expect(llmApi.moderationScan).toHaveBeenCalledWith({ limit: 20 })
    expect(wrapper.get('[data-scan-notice]').text()).toBe(
      '已扫描 20 条评论，其中 1 条需要人工确认。'
    )
    expect(wrapper.findAll('[data-verdict-row]')).toHaveLength(1)
    // 徽章挂在被标记的第 2 条评论上（items 只含 flagged）。
    expect(wrapper.findAll('.comment-card')[1]?.find('.verdict-badge').text()).toBe('建议复核')
  })

  it('shows the backend message when a single moderation check fails', async () => {
    vi.mocked(llmApi.moderationCheck).mockRejectedValue({
      response: { data: { message: '尚未配置可用的模型接口，请先在设置中填写' } }
    })
    const { toast } = await import('@/utils/toast')
    const wrapper = await setup()

    await wrapper.get('[data-action="ai-check"]').trigger('click')
    await flushPromises()

    expect(toast.error).toHaveBeenCalledWith('尚未配置可用的模型接口，请先在设置中填写')
    expect(wrapper.find('[data-verdict-row]').exists()).toBe(false)
  })
})
