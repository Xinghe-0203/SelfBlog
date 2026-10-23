import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PageEditor from '../PageEditor.vue'
import { pageApi } from '@/api/pages'
import type { PageConfig, PageEditorDocument } from '@/types/page'

vi.mock('@/api/pages', () => ({
  pageApi: {
    getEditor: vi.fn(),
    saveDraft: vi.fn(),
    publish: vi.fn(),
    unpublish: vi.fn(),
    delete: vi.fn(),
    getManifest: vi.fn()
  }
}))
enableAutoUnmount(afterEach)
const response = <T>(data: T) => ({ code: 200, message: 'ok', data })
function document(config: PageConfig = {}, revision = 0, id = 'about'): PageEditorDocument {
  const draft = { schemaVersion: 1, revision, config, everPublished: true }
  return { id, builtin: true, label: '关于', draft, published: { ...draft } }
}
beforeEach(() => {
  vi.resetAllMocks()
  vi.spyOn(window, 'confirm').mockReturnValue(false)
  vi.mocked(pageApi.getEditor).mockResolvedValue(response(document()))
  vi.mocked(pageApi.saveDraft).mockImplementation(async (_id, revision, config) =>
    response(document(config, revision + 1))
  )
  vi.mocked(pageApi.publish).mockResolvedValue(response(document({}, 2)))
  vi.mocked(pageApi.getManifest).mockResolvedValue(response({ pages: [], navigation: [] }))
})
afterEach(() => vi.restoreAllMocks())
async function setup(id = 'about') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin/pages/:id', component: PageEditor },
      { path: '/admin/pages', component: { template: '<div>列表</div>' } },
      { path: '/', component: { template: '<div />' } }
    ]
  })
  await router.push(`/admin/pages/${id}`)
  const wrapper = mount(
    { template: '<RouterView />' },
    {
      attachTo: globalThis.document.body,
      global: {
        plugins: [createPinia(), router],
        stubs: {
          PagePreview: { template: '<div class="preview-stub" />' },
          teleport: true,
          transition: true
        }
      }
    }
  )
  await flushPromises()
  return { wrapper, router }
}
async function editDescription(
  wrapper: Awaited<ReturnType<typeof setup>>['wrapper'],
  value: string
) {
  await wrapper.get('[data-override="description"]').setValue(true)
  await wrapper.get('#page-description').setValue(value)
}
describe('page draft editor', () => {
  it('does not display the previous page as editable if the next page fails to load', async () => {
    const { wrapper, router } = await setup()
    vi.mocked(pageApi.getEditor).mockRejectedValue({ userMessage: '新页面无法加载' })
    await router.push('/admin/pages/home')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('新页面无法加载')
    expect(wrapper.find('[data-action="save"]').exists()).toBe(false)
  })
  it('requires confirmation before deleting a custom page and does not redirect on cancellation', async () => {
    const custom = { ...document({ title: '内容页' }, 3), id: 'custom-one', builtin: false }
    vi.mocked(pageApi.getEditor).mockResolvedValue(response(custom))
    vi.mocked(pageApi.delete).mockResolvedValue(response(undefined))
    const { wrapper, router } = await setup()
    await wrapper.get('[data-action="delete"]').trigger('click')
    await wrapper.get('.modal-footer .btn-secondary').trigger('click')
    await flushPromises()
    expect(pageApi.delete).not.toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/admin/pages/about')
    await wrapper.get('[data-action="delete"]').trigger('click')
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    expect(pageApi.delete).toHaveBeenCalledWith('custom-one', 3)
    expect(router.currentRoute.value.path).toBe('/admin/pages')
  })
  it('does not leave another page editor stuck busy after accepting navigation during a save', async () => {
    let finish!: (value: Awaited<ReturnType<typeof pageApi.saveDraft>>) => void
    vi.mocked(pageApi.saveDraft).mockReturnValueOnce(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    const { wrapper, router } = await setup()
    await editDescription(wrapper, '旧页面保存中')
    await wrapper.get('[data-action="save"]').trigger('click')
    vi.mocked(window.confirm).mockReturnValue(true)
    vi.mocked(pageApi.getEditor).mockResolvedValue(
      response({ ...document(), id: 'home', label: '首页' })
    )
    await router.push('/admin/pages/home')
    await flushPromises()
    await editDescription(wrapper, '新页面编辑')
    expect(wrapper.get<HTMLButtonElement>('[data-action="save"]').element.disabled).toBe(false)
    finish(response(document({ description: '旧页面保存中' }, 1)))
    await flushPromises()
    expect(wrapper.get<HTMLTextAreaElement>('#page-description').element.value).toBe('新页面编辑')
  })
  it('saves a draft without automatically publishing or changing public settings', async () => {
    const { wrapper, router } = await setup()
    await editDescription(wrapper, '独立介绍')
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    expect(pageApi.saveDraft).toHaveBeenCalledWith(
      'about',
      0,
      expect.objectContaining({ description: '独立介绍' }),
      null
    )
    expect(pageApi.publish).not.toHaveBeenCalled()
    await router.push('/')
    expect(window.confirm).not.toHaveBeenCalled()
  })
  it('retains failed-save edits and guards real route departure', async () => {
    vi.mocked(pageApi.saveDraft).mockRejectedValue({ userMessage: '页面已在其他窗口更新' })
    const { wrapper, router } = await setup()
    await editDescription(wrapper, '未保存内容')
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('其他窗口更新')
    await router.push('/')
    expect(window.confirm).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/admin/pages/about')
    expect(wrapper.get<HTMLTextAreaElement>('#page-description').element.value).toBe('未保存内容')
  })
  it('does not mark edits made during a save as saved', async () => {
    let finish!: (value: Awaited<ReturnType<typeof pageApi.saveDraft>>) => void
    vi.mocked(pageApi.saveDraft).mockReturnValueOnce(
      new Promise((resolve) => {
        finish = resolve
      })
    )
    const { wrapper, router } = await setup()
    await editDescription(wrapper, '提交的内容')
    await wrapper.get('[data-action="save"]').trigger('click')
    await wrapper.get('#page-description').setValue('请求期间的新内容')
    finish(response(document({ description: '提交的内容' }, 1)))
    await flushPromises()
    await router.push('/')
    expect(window.confirm).toHaveBeenCalled()
    expect(wrapper.get<HTMLTextAreaElement>('#page-description').element.value).toBe(
      '请求期间的新内容'
    )
  })
  it('publishes only after saving and sends the current revision', async () => {
    const { wrapper } = await setup()
    await editDescription(wrapper, '准备发布')
    expect(wrapper.get<HTMLButtonElement>('[data-action="publish"]').element.disabled).toBe(true)
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-action="publish"]').trigger('click')
    await flushPromises()
    expect(pageApi.publish).toHaveBeenCalledWith('about', 1)
    expect(pageApi.getManifest).toHaveBeenCalled()
  })
  it('restores builtin defaults in the local draft only, not the published page', async () => {
    vi.mocked(pageApi.getEditor).mockResolvedValue(
      response(document({ title: '个性化标题', description: '介绍' }, 2))
    )
    const { wrapper } = await setup()
    await wrapper.get('[data-action="reset"]').trigger('click')
    expect(pageApi.saveDraft).not.toHaveBeenCalled()
    expect(pageApi.publish).not.toHaveBeenCalled()
    expect(wrapper.get<HTMLInputElement>('[data-override="title"]').element.checked).toBe(false)
    expect(wrapper.find('[data-action="delete"]').exists()).toBe(false)
  })
  it('offers the banner controls on pages that support a banner background', async () => {
    const { wrapper } = await setup()
    expect(wrapper.find('[data-banner-fields]').exists()).toBe(true)
    expect(wrapper.find('[data-banner-field="layout"]').exists()).toBe(true)
    expect(wrapper.find('[data-banner-field="overlay"]').exists()).toBe(true)
  })
  it('hides the banner controls on pages without a banner background', async () => {
    vi.mocked(pageApi.getEditor).mockResolvedValue(response(document({}, 0, 'archives')))
    const { wrapper } = await setup('archives')
    expect(wrapper.find('[data-banner-fields]').exists()).toBe(false)
    expect(wrapper.find('.asset-input').exists()).toBe(false)
  })
  it('writes the banner as an object instead of a string style value', async () => {
    const { wrapper } = await setup()
    await wrapper.get('.asset-input').setValue('/uploads/site-assets/hero.png')
    await wrapper.get('[data-banner-field="fit"]').setValue('contain')
    await wrapper.get('[data-banner-field="position"]').setValue('top')
    await wrapper.get('[data-banner-field="height"]').setValue('tall')
    await wrapper.get('[data-banner-field="tone"]').setValue('light')
    await wrapper.get('[data-banner-field="layout"]').setValue('centered')
    await wrapper.get('[data-banner-field="overlay"]').setValue('45')
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    expect(pageApi.saveDraft).toHaveBeenCalledWith(
      'about',
      0,
      expect.objectContaining({
        style: expect.objectContaining({
          banner: {
            image: '/uploads/site-assets/hero.png',
            fit: 'contain',
            position: 'top',
            height: 'tall',
            tone: 'light',
            layout: 'centered',
            overlay: 45
          }
        })
      }),
      null
    )
  })
  it('rejects an unsafe background address and blocks the draft save', async () => {
    const { wrapper } = await setup()
    await wrapper.get('.asset-input').setValue('/uploads/site-assets/hero.png')
    await wrapper.get('.asset-input').setValue('javascript:alert(1)')
    expect(wrapper.get('[data-banner-error]').text()).toContain('背景图地址不合法')
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    expect(pageApi.saveDraft).not.toHaveBeenCalled()
    // 改成合法地址后再保存：非法地址从未写进草稿。
    await wrapper.get('.asset-input').setValue('/uploads/site-assets/hero.png')
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    expect(pageApi.saveDraft).toHaveBeenCalledWith(
      'about',
      0,
      expect.objectContaining({
        style: expect.objectContaining({
          banner: expect.objectContaining({ image: '/uploads/site-assets/hero.png' })
        })
      }),
      null
    )
  })
  it('clears the banner together with the other defaults', async () => {
    vi.mocked(pageApi.getEditor).mockResolvedValue(
      response(document({ style: { banner: { image: '/uploads/hero.png' } } }, 2))
    )
    const { wrapper } = await setup()
    expect(wrapper.get<HTMLInputElement>('.asset-input').element.value).toBe('/uploads/hero.png')
    await wrapper.get('[data-action="reset"]').trigger('click')
    await flushPromises()
    expect(wrapper.get<HTMLInputElement>('.asset-input').element.value).toBe('')
    await wrapper.get('[data-action="save"]').trigger('click')
    await flushPromises()
    const payload = vi.mocked(pageApi.saveDraft).mock.calls[0]?.[2] as PageConfig
    expect(payload.style?.banner).toBeUndefined()
  })
})
