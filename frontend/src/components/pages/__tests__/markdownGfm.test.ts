import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownContent from '../MarkdownContent.vue'
import { renderMarkdown } from '@/utils/markdown'
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

  /**
   * HTML 实体与后端一致：正文里的 `&nbsp;` 渲染成不换行空格（不是字面的「&nbsp;」），
   * `&amp;` 渲染成 `&`，`&lt;` 渲染成字面的 `<`。
   *
   * 后端修掉的是「CommonMark 把 `&` 转义成 `&amp;`」导致的**二次转义**：
   * 服务端 HTML 里出现字面的「&nbsp;」，而浏览器里是空格 —— 同一篇文章两边长得不一样。
   * 这里钉住浏览器侧的行为，两侧必须一致。
   */
  it('renders HTML entities as characters instead of literal entity text', () => {
    const html = renderMarkdown('A&nbsp;B\n\nA&amp;B\n\nA&lt;B')
    expect(html).toContain('A&nbsp;B')
    expect(html).not.toContain('&amp;nbsp;')
    expect(html).toContain('A&amp;B')
    expect(html).not.toContain('&amp;amp;')
    expect(html).toContain('A&lt;B')
    expect(html).not.toContain('&amp;lt;')
  })

  /** 行内代码里的实体显示为字面量（代码段就该显示源文），与后端一致。 */
  it('keeps entities literal inside code spans', () => {
    const html = renderMarkdown('行内 `&nbsp;` 代码')
    expect(html).toContain('<code>&amp;nbsp;</code>')
  })

  /**
   * 正文标题整体降一级：每个页面只有一个 h1（页面标题），正文从 h2 起。
   * 与后端 SafeMarkdown 同一策略，两侧大纲一致。
   */
  it('demotes markdown headings so the page keeps a single h1', () => {
    const html = renderMarkdown('# 一级\n\n## 二级\n\n### 三级\n\n#### 四级')
    expect(html).not.toContain('<h1')
    expect(html).toContain('<h2>一级</h2>')
    expect(html).toContain('<h3>二级</h3>')
    expect(html).toContain('<h4>三级</h4>')
    expect(html).toContain('<h5>四级</h5>')
  })

  it('never lets a demoted heading collide with the page title', () => {
    const wrapper = mount(MarkdownContent, { props: { source: '# 正文一级标题' } })
    expect(wrapper.findAll('h1')).toHaveLength(0)
    expect(wrapper.findAll('h2')).toHaveLength(1)
    wrapper.unmount()
  })
})
