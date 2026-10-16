import { afterEach, describe, expect, it } from 'vitest'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PagePreview from '../PagePreview.vue'
enableAutoUnmount(afterEach)
describe('isolated responsive preview', () => {
  it('uses a real viewport for mobile media queries instead of just narrowing a desktop div', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push('/admin/pages/about')
    const wrapper = mount(PagePreview, {
      props: { id: 'about', config: { body: '未发布内容' } },
      global: { plugins: [createPinia(), router] }
    })
    await wrapper.get('select').setValue('mobile')
    expect(wrapper.get('iframe').attributes('width')).toBe('390')
    expect(wrapper.get('iframe').attributes('srcdoc')).not.toContain('未发布内容')
    expect(wrapper.get('iframe').attributes('src')).toBeUndefined()
  })
})
