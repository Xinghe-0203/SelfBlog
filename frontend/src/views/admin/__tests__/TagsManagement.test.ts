import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import TagsManagement from '../TagsManagement.vue'
import { adminApi } from '@/api/admin'
import { toast } from '@/utils/toast'

vi.mock('@/api/admin', () => ({
  adminApi: { getTagList: vi.fn(), createTag: vi.fn(), updateTag: vi.fn() }
}))
vi.mock('@/utils/toast', () => ({ toast: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }))
enableAutoUnmount(afterEach)
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(adminApi.getTagList).mockResolvedValue({
    code: 200,
    message: 'ok',
    data: { records: [{ id: 1, name: '技术' }], pages: 1, total: 1, pageNum: 1, pageSize: 20 }
  })
})
async function setup() {
  const wrapper = mount(TagsManagement, { global: { stubs: { teleport: true, transition: true } } })
  await flushPromises()
  return wrapper
}
describe('tag form contract', () => {
  it('limits both create and edit inputs to the backend 20-character rule', async () => {
    const wrapper = await setup()
    expect(wrapper.get('input[aria-label="标签名称"]').attributes('maxlength')).toBe('20')
    await wrapper.get('.actions button').trigger('click')
    expect(wrapper.get('#tag-edit-name').attributes('maxlength')).toBe('20')
  })
  it('rejects programmatic overlong names before creating a tag', async () => {
    const wrapper = await setup()
    await wrapper.get('input[aria-label="标签名称"]').setValue('x'.repeat(21))
    await wrapper.get('.create-form').trigger('submit')
    expect(adminApi.createTag).not.toHaveBeenCalled()
    expect(toast.warning).toHaveBeenCalledWith('标签名称不能超过20个字符')
  })
  it('shows the server reason when saving fails without closing the edit dialog', async () => {
    vi.mocked(adminApi.updateTag).mockRejectedValue({ userMessage: '标签名称已存在' })
    const wrapper = await setup()
    await wrapper.get('.actions button').trigger('click')
    await wrapper.get('#tag-edit-name').setValue('新名称')
    await wrapper.get('.modal-footer .btn-primary').trigger('click')
    await flushPromises()
    expect(wrapper.find('#tag-edit-name').exists()).toBe(true)
    expect(toast.error).toHaveBeenCalledWith('标签名称已存在')
  })
})
