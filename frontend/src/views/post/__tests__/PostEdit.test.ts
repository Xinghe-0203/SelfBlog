import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import PostEdit from '../PostEdit.vue'
import { postApi } from '@/api/post'
import { tagApi } from '@/api/tag'
import { mediaApi } from '@/api/media'
import { seoApi } from '@/api/seo'
import { llmApi } from '@/api/llm'
import { toast } from '@/utils/toast'

/**
 * 编辑器冒烟测试只覆盖已有动作（草稿 / 发布 / 封面 / 标签），
 * 不发明新流程；Markdown 输入与富文本预览保持原有实现。
 */
vi.mock('@/api/post', () => ({
  postApi: {
    getPostById: vi.fn(),
    getDraft: vi.fn(),
    saveDraft: vi.fn(),
    createPost: vi.fn(),
    updatePost: vi.fn(),
    deleteDraft: vi.fn()
  }
}))
vi.mock('@/api/tag', () => ({ tagApi: { getTagList: vi.fn() } }))
vi.mock('@/api/media', () => ({ mediaApi: { uploadFile: vi.fn() } }))
vi.mock('@/api/seo', () => ({
  seoApi: { getDetail: vi.fn(), update: vi.fn(), generate: vi.fn() }
}))
vi.mock('@/api/llm', () => ({
  llmApi: { generateSummary: vi.fn(), generateTags: vi.fn(), generateTitles: vi.fn() }
}))
vi.mock('@/utils/toast', () => ({
  toast: { success: vi.fn(), warning: vi.fn(), error: vi.fn(), info: vi.fn() }
}))
enableAutoUnmount(afterEach)
const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
const nextFrame = () => new Promise((resolve) => setTimeout(resolve, 0))

/** 真实定时器里的 30s 自动保存与 3s 状态复位不参与断言，用假定时器只保留 rAF 行为。 */
beforeEach(() => {
  vi.resetAllMocks()
  vi.spyOn(window, 'confirm').mockReturnValue(false)
  vi.mocked(tagApi.getTagList).mockResolvedValue(response([]))
  vi.mocked(postApi.saveDraft).mockResolvedValue(
    response({ draftId: 7, id: 7, title: '', content: '', createdAt: '', updatedAt: '' })
  )
  vi.mocked(postApi.createPost).mockResolvedValue(
    response({
      id: 9,
      title: '要发布的文章',
      content: '正文内容',
      authorId: 1,
      authorName: '作者',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      collectCount: 0,
      shareCount: 0,
      status: 1,
      createdAt: '2026-10-16'
    })
  )
  vi.mocked(postApi.updatePost).mockResolvedValue(
    response({
      id: 9,
      title: '已发布文章',
      content: '正文',
      authorId: 1,
      authorName: '作者',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      collectCount: 0,
      shareCount: 0,
      status: 1,
      createdAt: '2026-10-16'
    })
  )
  vi.mocked(postApi.deleteDraft).mockResolvedValue(response(undefined))
  vi.mocked(postApi.getPostById).mockResolvedValue(
    response({
      id: 9,
      title: '已发布文章',
      content: '正文',
      category: 'tech',
      authorId: 1,
      authorName: '作者',
      viewCount: 1,
      likeCount: 0,
      commentCount: 0,
      collectCount: 0,
      shareCount: 0,
      status: 1,
      createdAt: '2026-10-16'
    })
  )
  vi.stubGlobal('requestAnimationFrame', (frame: FrameRequestCallback) => {
    frame(0)
    return 1
  })
  vi.stubGlobal('cancelAnimationFrame', vi.fn())
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  vi.useRealTimers()
})

async function setup(path = '/admin/write') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/moderation', component: { template: '<div />' } },
      { path: '/admin/write', component: PostEdit },
      { path: '/admin/edit/:id', component: PostEdit }
    ]
  })
  await router.push(path)
  const wrapper = mount(
    { template: '<RouterView />' },
    {
      attachTo: document.body,
      global: { plugins: [createPinia(), router], stubs: { teleport: true, transition: true } }
    }
  )
  await flushPromises()
  await nextFrame()
  return { wrapper, router }
}

async function fillPost(
  wrapper: Awaited<ReturnType<typeof setup>>['wrapper'],
  title = '新的记录',
  content = '正文内容'
) {
  await wrapper.get('.title-input').setValue(title)
  await wrapper.get('.content-input').setValue(content)
  await wrapper.get('select.form-input').setValue('tech')
}

describe('author editor smoke coverage', () => {
  it('restores a draft article identity instead of publishing a duplicate after reopening', async () => {
    vi.mocked(postApi.getDraft).mockResolvedValue(
      response({
        id: 7,
        postId: 9,
        title: '已发布后的继续修改',
        content: '新正文',
        category: 'tech',
        createdAt: '',
        updatedAt: ''
      })
    )
    const { wrapper } = await setup('/admin/write?draft=7')
    await wrapper.get('.header-actions .btn-primary').trigger('click')
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    expect(postApi.createPost).not.toHaveBeenCalled()
    expect(postApi.updatePost).toHaveBeenCalledWith(
      9,
      expect.objectContaining({ title: '已发布后的继续修改' })
    )
  })

  it('keeps a free-typed tag name in the publish payload and limits input to the backend rule', async () => {
    vi.mocked(tagApi.getTagList).mockResolvedValue(response([{ id: 5, name: '已有标签' }]))
    const { wrapper } = await setup()
    const tagInput = wrapper.get('.tag-input')
    expect(tagInput.attributes('maxlength')).toBe('20')

    await tagInput.setValue('已有标签')
    await tagInput.trigger('keyup.enter')
    await tagInput.setValue('全新标签')
    await tagInput.trigger('keyup.enter')

    await fillPost(wrapper)
    await wrapper.get('.header-actions .btn-primary').trigger('click')
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()

    const payload = vi.mocked(postApi.createPost).mock.calls[0]?.[0] as {
      tagIds?: number[]
      tagNames?: string[]
    }
    expect(payload.tagIds).toEqual([5])
    expect(payload.tagNames).toEqual(['已有标签', '全新标签'])
  })

  it('keeps the publish opener enabled while confirmation captures and returns its focus', async () => {
    const { wrapper } = await setup()
    await fillPost(wrapper)
    const publish = wrapper.get<HTMLButtonElement>('.header-actions .btn-primary')
    publish.element.focus()
    await publish.trigger('click')
    expect(publish.element.disabled).toBe(false)
    await wrapper.get('.modal-footer .btn-secondary').trigger('click')
    await flushPromises()
    expect(document.activeElement).toBe(publish.element)
    expect(postApi.createPost).not.toHaveBeenCalled()
  })
  it('does not auto-save while publication is pending', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
    let finish!: (value: Awaited<ReturnType<typeof postApi.createPost>>) => void
    vi.mocked(postApi.createPost).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    const { wrapper } = await setup()
    await fillPost(wrapper)
    await wrapper.get('.header-actions .btn-primary').trigger('click')
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    await vi.advanceTimersByTimeAsync(30000)
    expect(postApi.saveDraft).not.toHaveBeenCalled()
    finish(response({ id: 9 } as Awaited<ReturnType<typeof postApi.createPost>>['data']))
    await flushPromises()
  })

  it('preserves edits during publication and updates the created article on the next publish', async () => {
    let finish!: (value: Awaited<ReturnType<typeof postApi.createPost>>) => void
    vi.mocked(postApi.createPost).mockReturnValueOnce(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    const { wrapper, router } = await setup()
    await fillPost(wrapper)
    await wrapper.get('.header-actions .btn-primary').trigger('click')
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    await wrapper.get('.content-input').setValue('发布期间的新改动')
    finish(response({ id: 9 } as Awaited<ReturnType<typeof postApi.createPost>>['data']))
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/admin/write')
    expect(window.confirm).not.toHaveBeenCalled()
    await wrapper.get('.header-actions .btn-primary').trigger('click')
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    expect(postApi.createPost).toHaveBeenCalledTimes(1)
    expect(postApi.updatePost).toHaveBeenCalledWith(
      9,
      expect.objectContaining({ content: '发布期间的新改动' })
    )
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('does not warn when leaving after an unchanged successful manual save', async () => {
    const { wrapper, router } = await setup()
    await fillPost(wrapper)
    await wrapper.get('.header-actions .btn-ghost').trigger('click')
    await flushPromises()
    await router.push('/')
    expect(window.confirm).not.toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('keeps edits made during a pending save unsaved', async () => {
    let finish!: (value: Awaited<ReturnType<typeof postApi.saveDraft>>) => void
    vi.mocked(postApi.saveDraft).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    const { wrapper, router } = await setup()
    await fillPost(wrapper)
    await wrapper.get('.header-actions .btn-ghost').trigger('click')
    await wrapper.get('.content-input').setValue('保存期间的新改动')
    finish(response({ id: 7, title: '', content: '', createdAt: '', updatedAt: '' }))
    await flushPromises()
    await router.push('/')
    expect(window.confirm).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value.path).toBe('/admin/write')
  })

  it('protects summary-only and tag-only edits when leaving', async () => {
    const { wrapper, router } = await setup()
    await wrapper.get('.summary-input').setValue('只修改摘要')
    await router.push('/')
    expect(window.confirm).toHaveBeenCalledTimes(1)
    await wrapper.get('.summary-input').setValue('')
    await wrapper.get('.tag-input').setValue('一个标签')
    await wrapper.get('.tag-input').trigger('keyup', { key: 'Enter' })
    await router.push('/')
    expect(window.confirm).toHaveBeenCalledTimes(2)
  })

  it('keeps failed saves protected instead of marking content clean', async () => {
    vi.mocked(postApi.saveDraft).mockRejectedValue(new Error('offline'))
    const { wrapper, router } = await setup()
    await fillPost(wrapper)
    await wrapper.get('.header-actions .btn-ghost').trigger('click')
    await flushPromises()
    await router.push('/')
    expect(window.confirm).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value.path).toBe('/admin/write')
  })

  it('keeps the markdown editor, preview toggle and metadata geometry', async () => {
    const { wrapper } = await setup()
    expect(wrapper.find('.content-input').exists()).toBe(true)
    expect(wrapper.find('.toolbar').exists()).toBe(true)
    await wrapper.get('.header-actions button.btn-secondary').trigger('click')
    expect(wrapper.find('.content-input').exists()).toBe(false)
    expect(wrapper.find('.editor-preview').exists()).toBe(true)
    await wrapper.get('.header-actions button.btn-secondary').trigger('click')
    expect(wrapper.find('.content-input').exists()).toBe(true)
    expect(wrapper.text()).toContain('字数')
    expect(wrapper.text()).toContain('预计阅读')
  })

  it('links the selection with a protocol placeholder instead of a bare url', async () => {
    const { wrapper } = await setup()
    const textarea = wrapper.get('.content-input').element as HTMLTextAreaElement
    await wrapper.get('.content-input').setValue('看看这个')
    textarea.setSelectionRange(0, 4)

    await wrapper.get('.toolbar button[title="链接"]').trigger('click')

    // 修复前插入的是 [看看这个](url)：不改就会变成站内相对路径，点开跳到本站 404。
    expect((wrapper.get('.content-input').element as HTMLTextAreaElement).value).toBe(
      '[看看这个](https://)'
    )
    expect(textarea.selectionStart).toBe(7)
    expect(textarea.selectionEnd).toBe(15)
  })

  it('blocks in-site link navigation inside the preview but leaves external links alone', async () => {
    const { wrapper, router } = await setup()
    await wrapper.get('.content-input').setValue('[站内文章](/post/2)\n\n[外站](example.com/path)')
    await wrapper.get('.header-actions button.btn-secondary').trigger('click')
    await flushPromises()

    const [internal, external] = wrapper.findAll('.editor-preview .markdown-body a')
    // 预览里点站内链接会离开编辑页、丢掉未保存改动，所以只提示不跳转。
    await internal!.trigger('click')
    expect(vi.mocked(toast.info)).toHaveBeenCalledWith(
      '预览中不跳转站内链接，发布后点击即可正常打开'
    )
    expect(router.currentRoute.value.path).toBe('/admin/write')

    // 外链由渲染链路加上 target="_blank"，不受预览拦截影响。
    expect(external!.attributes('href')).toBe('https://example.com/path')
    expect(external!.attributes('target')).toBe('_blank')
  })

  it('saves a draft with the real payload and reports the saved state', async () => {
    const { wrapper } = await setup()
    await fillPost(wrapper)
    await wrapper
      .findAll('.header-actions button')
      .find((button) => button.text().includes('保存草稿'))
      ?.trigger('click')
    await flushPromises()
    const payload = vi.mocked(postApi.saveDraft).mock.calls[0]?.[0] as unknown as Record<
      string,
      unknown
    >
    expect(payload.title).toBe('新的记录')
    expect(payload.content).toBe('正文内容')
    expect(payload).not.toHaveProperty('postId')
    expect(wrapper.text()).toContain('已保存')
  })

  it('publishes after confirmation and removes the draft trail', async () => {
    const { wrapper, router } = await setup()
    await fillPost(wrapper, '要发布的文章')
    await wrapper
      .findAll('.header-actions button')
      .find((button) => button.text().includes('发布'))
      ?.trigger('click')
    await flushPromises()
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    expect(postApi.createPost).toHaveBeenCalledTimes(1)
    const payload = vi.mocked(postApi.createPost).mock.calls[0]?.[0] as unknown as Record<
      string,
      unknown
    >
    expect(payload.title).toBe('要发布的文章')
    // 发布即上线：正文与分类随请求提交，不额外引入待审核状态字段。
    expect(payload.content).toBe('正文内容')
    expect(payload).not.toHaveProperty('status')
    await router.isReady()
    await nextFrame()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('updates an existing post instead of creating a duplicate', async () => {
    const { wrapper } = await setup('/admin/edit/9')
    await flushPromises()
    await nextFrame()
    expect(wrapper.get<HTMLInputElement>('.title-input').element.value).toBe('已发布文章')
    await fillPost(wrapper, '已发布文章', '更新后的正文')
    await wrapper
      .findAll('.header-actions button')
      .find((button) => button.text().includes('发布'))
      ?.trigger('click')
    await flushPromises()
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    expect(postApi.updatePost).toHaveBeenCalledWith(
      '9',
      expect.objectContaining({ title: '已发布文章' })
    )
    expect(postApi.createPost).not.toHaveBeenCalled()
  })

  it('warns instead of publishing when the category is missing', async () => {
    const wrapper = (await setup()).wrapper
    await wrapper.get('.title-input').setValue('没有分类')
    await wrapper.get('.content-input').setValue('正文')
    await wrapper
      .findAll('.header-actions button')
      .find((button) => button.text().includes('发布'))
      ?.trigger('click')
    await flushPromises()
    expect(postApi.createPost).not.toHaveBeenCalled()
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })

  it('keeps the cover upload flow on the existing media api', async () => {
    vi.mocked(mediaApi.uploadFile).mockResolvedValue(
      response({ fileUrl: '/uploads/cover.jpg' } as never)
    )
    const { wrapper } = await setup()
    const inputs = wrapper.findAll<HTMLInputElement>('input[type="file"]')
    const coverInput = inputs[inputs.length - 1]
    if (!coverInput) throw new Error('missing cover input')
    const file = new File([new Uint8Array(64)], 'cover.jpg', { type: 'image/jpeg' })
    Object.defineProperty(coverInput.element, 'files', { value: [file] })
    await coverInput.trigger('change')
    await flushPromises()
    expect(mediaApi.uploadFile).toHaveBeenCalledWith(file, 'cover')
    expect(wrapper.find('.cover-preview img').attributes('src')).toBe('/uploads/cover.jpg')
  })

  it('only offers the SEO panel for a saved post and loads its stored metadata', async () => {
    vi.mocked(seoApi.getDetail).mockResolvedValue(
      response({
        postId: 9,
        title: '已发布文章',
        status: 1,
        hasSeo: true,
        seoTitle: '存过的搜索标题',
        seoDescription: '存过的描述',
        seoKeywords: 'a,b',
        ogImageUrl: null,
        source: 'manual',
        generatedAt: null,
        updateTime: null,
        issues: [],
        postSummary: null,
        effectiveTitle: '存过的搜索标题',
        effectiveDescription: '存过的描述'
      })
    )

    const draft = (await setup()).wrapper
    expect(draft.find('[data-action="seo-save"]').exists()).toBe(false)

    const { wrapper } = await setup('/admin/edit/9')
    await flushPromises()
    expect(seoApi.getDetail).toHaveBeenCalledWith(9)
    expect(wrapper.get<HTMLInputElement>('#seo-title').element.value).toBe('存过的搜索标题')
  })

  it('treats a missing SEO record as empty instead of an error, and saves blanks as null', async () => {
    vi.mocked(seoApi.getDetail).mockRejectedValue({ response: { status: 404 } })
    vi.mocked(seoApi.update).mockResolvedValue(response(null as unknown as never))
    const { wrapper } = await setup('/admin/edit/9')
    await flushPromises()

    expect(wrapper.find('.seo-status').exists()).toBe(false)
    await wrapper.get('#seo-title').setValue('  新标题 ')
    await wrapper.get('#seo-description').setValue('  ')
    await wrapper.get('[data-action="seo-save"]').trigger('click')
    await flushPromises()

    expect(seoApi.update).toHaveBeenCalledWith(9, {
      seoTitle: '新标题',
      seoDescription: null,
      seoKeywords: null,
      ogImageUrl: null
    })
    expect(wrapper.get('.seo-status').text()).toBe('SEO 元数据已保存')
  })

  /**
   * 60/160 是搜索结果的**建议**长度，不是存储上限（服务端 200/400）。
   *
   * 若把 maxlength 直接设成建议值，作者粘贴一段更长的描述会被浏览器静默截断 ——
   * 那正是「保存成功但内容被悄悄改掉」这类最难排查的缺陷。所以输入框按服务端上限
   * 放行，超长只在计数上给出提示。
   */
  it('allows over-length SEO text up to the server cap and only warns on the counter', async () => {
    vi.mocked(seoApi.getDetail).mockRejectedValue({ response: { status: 404 } })
    vi.mocked(seoApi.update).mockResolvedValue(response(null as unknown as never))
    const { wrapper } = await setup('/admin/edit/9')
    await flushPromises()

    const title = wrapper.get<HTMLInputElement>('#seo-title')
    const description = wrapper.get<HTMLTextAreaElement>('#seo-description')
    expect(title.attributes('maxlength')).toBe('200')
    expect(description.attributes('maxlength')).toBe('400')

    /** 摘要框也有一个 .char-hint，按所属 form-group 取，避免依赖全页序号。 */
    const hintFor = (id: string) =>
      wrapper.get(`#${id}`).element.closest('.form-group')?.querySelector('.char-hint') ?? null

    expect(hintFor('seo-title')?.getAttribute('data-over')).toBe('false')
    expect(hintFor('seo-title')?.textContent?.trim()).toBe('0/60')

    await title.setValue('长'.repeat(72))
    await description.setValue('长'.repeat(170))
    await flushPromises()

    expect(hintFor('seo-title')?.textContent?.trim()).toBe('72/60')
    expect(hintFor('seo-title')?.getAttribute('data-over')).toBe('true')
    expect(hintFor('seo-description')?.textContent?.trim()).toBe('170/160')
    expect(hintFor('seo-description')?.getAttribute('data-over')).toBe('true')

    // 超长只是提示，不拦保存：提交的仍是完整原文。
    await wrapper.get('[data-action="seo-save"]').trigger('click')
    await flushPromises()
    expect(seoApi.update).toHaveBeenCalledWith(9, {
      seoTitle: '长'.repeat(72),
      seoDescription: '长'.repeat(170),
      seoKeywords: null,
      ogImageUrl: null
    })
  })

  /**
   * AI 写作卡片只产出候选，**绝不自动改文章**：
   * 摘要要点「采用」、标题要点某一条、标签要勾选后点「应用」，
   * 而且三者都不落库（仍需作者自己保存）。这里逐个验证回填路径。
   */
  it('offers AI writing candidates only for a saved post and applies them on explicit choice', async () => {
    vi.mocked(seoApi.getDetail).mockRejectedValue({ response: { status: 404 } })
    vi.mocked(tagApi.getTagList).mockResolvedValue(
      response([
        { id: 5, name: 'Spring Boot' },
        { id: 6, name: '事务' }
      ])
    )
    vi.mocked(llmApi.generateSummary).mockResolvedValue(
      response({ summary: '模型生成的摘要正文', model: 'deepseek-flash', latencyMs: 900 })
    )
    vi.mocked(llmApi.generateTags).mockResolvedValue(
      response({
        tags: ['Spring Boot', '事务', '池外标签'],
        model: 'deepseek-flash',
        latencyMs: 800
      })
    )
    vi.mocked(llmApi.generateTitles).mockResolvedValue(
      response({
        titles: ['候选标题一', '候选标题二', '候选标题三'],
        model: 'deepseek-flash',
        latencyMs: 700
      })
    )

    // 新建草稿还没有 postId，卡片不出现：后端要求文章必须存在。
    const draft = (await setup()).wrapper
    expect(draft.find('[data-action="ai-summary"]').exists()).toBe(false)

    const { wrapper } = await setup('/admin/edit/9')
    await flushPromises()

    await wrapper.get('[data-action="ai-summary"]').trigger('click')
    await flushPromises()
    expect(llmApi.generateSummary).toHaveBeenCalledWith({ postId: 9 })
    // 生成完只是候选，摘要框还没被动过。
    expect(wrapper.get<HTMLTextAreaElement>('.summary-input').element.value).toBe('')
    expect(wrapper.get('.ai-candidate-text').text()).toBe('模型生成的摘要正文')

    await wrapper.get('[data-action="ai-summary-apply"]').trigger('click')
    await flushPromises()
    expect(wrapper.get<HTMLTextAreaElement>('.summary-input').element.value).toBe(
      '模型生成的摘要正文'
    )

    await wrapper.get('[data-action="ai-tags"]').trigger('click')
    await flushPromises()
    const chips = wrapper.findAll('.ai-tag-chip')
    expect(chips.map((chip) => chip.text())).toEqual(['Spring Boot', '事务', '池外标签'])
    expect(chips.every((chip) => chip.attributes('data-selected') === 'false')).toBe(true)

    // 只勾选前两个（都来自标签池），池外值即使后端漏放也不会被默认选中。
    await chips[0]?.trigger('click')
    await chips[1]?.trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-action="ai-tags-apply"]').text()).toContain('2')
    await wrapper.get('[data-action="ai-tags-apply"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.tag-badge').map((badge) => badge.text().trim())).toEqual([
      'Spring Boot',
      '事务'
    ])

    await wrapper.get('[data-action="ai-titles"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.ai-title-option').map((option) => option.text())).toEqual([
      '候选标题一',
      '候选标题二',
      '候选标题三'
    ])

    await wrapper.get('[data-action="ai-title-apply-1"]').trigger('click')
    await flushPromises()
    expect(wrapper.get<HTMLInputElement>('.title-input').element.value).toBe('候选标题二')
    // 点击后候选清空，避免重复点选造成来回覆盖。
    expect(wrapper.find('.ai-titles').exists()).toBe(false)
  })

  it('surfaces the backend message when an AI writing call fails', async () => {
    vi.mocked(seoApi.getDetail).mockRejectedValue({ response: { status: 404 } })
    vi.mocked(llmApi.generateSummary).mockRejectedValue({
      response: { data: { message: 'AI 辅助功能已关闭，请先在设置中启用' } }
    })
    const { wrapper } = await setup('/admin/edit/9')
    await flushPromises()

    await wrapper.get('[data-action="ai-summary"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('.ai-writing .seo-status').text()).toBe(
      'AI 辅助功能已关闭，请先在设置中启用'
    )
    expect(wrapper.find('.ai-candidate').exists()).toBe(false)
  })
})
