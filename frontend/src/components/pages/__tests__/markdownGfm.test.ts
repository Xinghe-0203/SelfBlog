import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownContent from '../MarkdownContent.vue'
// 与后端 SafeMarkdownTest 共用同一篇 fixture（仓库根 docs/fixtures/gfm-sample.md）
import fixture from '../../../../../docs/fixtures/gfm-sample.md?raw'

/**
 * 表格/删除线/任务列表在浏览器与服务端渲染里必须呈现同样的语义；
 * 复选框两侧都被安全策略移除，只保留条目文字。
 */
describe('MarkdownContent GFM 渲染一致性', () => {
  it('渲染表格、删除线与任务列表，并移除复选框', () => {
    const wrapper = mount(MarkdownContent, { props: { source: fixture } })
    expect(wrapper.findAll('table')).toHaveLength(1)
    expect(wrapper.findAll('thead th')).toHaveLength(2)
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.find('del').text()).toBe('删除线')
    expect(wrapper.findAll('li')).toHaveLength(2)
    expect(wrapper.find('li').text()).toBe('已完成的任务')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.find('blockquote p').text()).toBe('引用块也必须保留。')
    wrapper.unmount()
  })
})
