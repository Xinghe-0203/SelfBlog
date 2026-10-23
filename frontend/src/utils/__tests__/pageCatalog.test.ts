import { describe, expect, it } from 'vitest'
import { customPageCapability, pageCapability, pageCatalog } from '@/utils/pageCatalog'

describe('页面能力位里的横幅能力', () => {
  it('首页与关于页声明横幅能力，并允许三种内容排布', () => {
    expect(pageCapability('home').banner?.layouts).toEqual(['split', 'centered', 'stacked'])
    expect(pageCapability('about').banner?.layouts).toEqual(['split', 'centered', 'stacked'])
  })

  it('其它内置页面没有横幅能力位', () => {
    for (const id of ['archives', 'gallery', 'search', 'post', 'tag', 'notFound'] as const) {
      expect(pageCatalog[id].banner).toBeUndefined()
    }
  })

  it('内容页与未知页面同样没有横幅能力位', () => {
    expect(customPageCapability.banner).toBeUndefined()
    expect(pageCapability('some-content-page').banner).toBeUndefined()
  })
})
