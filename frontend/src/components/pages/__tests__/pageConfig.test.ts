import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { resolvePageText, pageModule, pageStyleVars } from '@/composables/usePageConfig'
import MarkdownContent from '../MarkdownContent.vue'
import PageFrame from '../PageFrame.vue'

describe('public page configuration', () => {
  beforeEach(() => {
    document.documentElement.style.removeProperty('--accent')
  })
  it('distinguishes inherited text from an intentionally empty introduction', () => {
    expect(resolvePageText({}, 'description', '默认介绍')).toBe('默认介绍')
    expect(resolvePageText({ description: '' }, 'description', '默认介绍')).toBe('')
    expect(resolvePageText({ text: { listTitle: '新的文章' } }, 'listTitle', '最近写作')).toBe(
      '新的文章'
    )
  })
  it('keeps existing modules by default and honors explicit false', () => {
    expect(pageModule({}, 'cover')).toBe(true)
    expect(pageModule({ modules: { cover: false } }, 'cover')).toBe(false)
    expect(pageModule({}, 'updated', false)).toBe(false)
  })
  it('scopes style variables and does not introduce default zoom or enlargement', () => {
    const wrapper = mount(PageFrame, {
      props: { config: { style: { accent: '#345678', width: 'narrow' } } },
      slots: { default: '<p>内容</p>' }
    })
    expect(wrapper.attributes('style')).toContain('--accent: #345678')
    expect(document.documentElement.style.getPropertyValue('--accent')).toBe('')
    expect(pageStyleVars({})).toEqual({})
    expect(wrapper.attributes('style')).not.toMatch(/zoom|scale\(/)
    wrapper.unmount()
  })
  it('renders Markdown but strips executable elements, event handlers and javascript URLs', () => {
    const wrapper = mount(MarkdownContent, {
      props: {
        source:
          '## 正文\n\n<script>alert(1)</script>\n\n[危险](javascript:alert(1))\n\n<img src="/uploads/a.jpg" onerror="alert(1)"><iframe src="https://evil.example"></iframe><style>*{display:none}</style>'
      }
    })
    expect(wrapper.find('h2').text()).toBe('正文')
    expect(wrapper.find('script,iframe,style,[onerror],a[href^="javascript:"]').exists()).toBe(
      false
    )
    expect(wrapper.find('img').exists()).toBe(true)
    wrapper.unmount()
  })
})
