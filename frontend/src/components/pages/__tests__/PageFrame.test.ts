import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PageFrame from '../PageFrame.vue'
import type { PageConfig } from '@/types/page'

/**
 * I1：任何页面都不许把正文直接画在壁纸照片上。
 *
 * 缺陷的根因就落在这一条属性上：改造前 PageFrame 直接绑定
 * `:data-page-background="config.style?.background"`，页面配置没有
 * `style.background` 时属性**整个缺失**，而 `.page-frame` 没有任何兜底背景规则，
 * 于是文章正文、目录、首页列表全都透到壁纸照片上（用户截图三）。
 *
 * 现在 PageFrame 必须永远解析出一个具体档位（缺省 'auto'），
 * 'inherit' 是唯一「我知道自己在装饰、不需要纸面」的显式退出开关。
 */
describe('PageFrame surface resolution', () => {
  const mountFrame = (config: PageConfig) =>
    mount(PageFrame, { props: { config }, slots: { default: '<p>正文</p>' } })

  it('always resolves data-page-background, defaulting to auto', () => {
    const wrapper = mountFrame({})
    expect(wrapper.attributes('data-page-background')).toBe('auto')
    wrapper.unmount()
  })

  it('defaults to auto when the config has a style block without a background', () => {
    const wrapper = mountFrame({ style: { width: 'narrow' } })
    expect(wrapper.attributes('data-page-background')).toBe('auto')
    wrapper.unmount()
  })

  it('defaults to auto when style.background is explicitly null', () => {
    const wrapper = mountFrame({ style: { background: null } })
    expect(wrapper.attributes('data-page-background')).toBe('auto')
    wrapper.unmount()
  })

  it('preserves every explicitly configured background, including the inherit opt-out', () => {
    for (const background of ['inherit', 'plain', 'soft', 'paper'] as const) {
      const wrapper = mountFrame({ style: { background } })
      expect(wrapper.attributes('data-page-background')).toBe(background)
      wrapper.unmount()
    }
  })

  it('keeps the class and slot contract intact for views that pass their own class', () => {
    const wrapper = mount(PageFrame, {
      props: { config: {} },
      attrs: { class: 'reader-page' },
      slots: { default: '<p>正文</p>' }
    })
    expect(wrapper.classes()).toContain('page-frame')
    expect(wrapper.classes()).toContain('reader-page')
    expect(wrapper.text()).toContain('正文')
    wrapper.unmount()
  })
})
