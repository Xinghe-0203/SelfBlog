import { describe, expect, it } from 'vitest'
import { shouldMoveFocusToMain } from '../guards'
import type { RouteLocationNormalized } from 'vue-router'

/** 造一个够用的路由位置快照（只用到 matched / hash / path 三个字段）。 */
function location(
  path: string,
  options: { matched?: number; hash?: string } = {}
): RouteLocationNormalized {
  return {
    path,
    hash: options.hash ?? '',
    matched: Array.from({ length: options.matched ?? 1 }, () => ({}))
  } as unknown as RouteLocationNormalized
}

describe('shouldMoveFocusToMain', () => {
  it('moves focus on a real page change', () => {
    expect(shouldMoveFocusToMain(location('/post/14'), location('/'))).toBe(true)
    expect(shouldMoveFocusToMain(location('/tags'), location('/archives'))).toBe(true)
  })

  it('leaves focus alone on the very first navigation', () => {
    // 首次进入时 from 还没匹配到任何路由：焦点必须从文档开头开始。
    expect(shouldMoveFocusToMain(location('/'), location('/', { matched: 0 }))).toBe(false)
  })

  it('leaves focus alone when the navigation only changes the query', () => {
    // 翻页 / 切排序：页面主体没换，焦点应留在刚点的控件上。
    expect(shouldMoveFocusToMain(location('/'), location('/'))).toBe(false)
  })

  it('leaves focus alone for hash navigation', () => {
    // 带锚点的导航由浏览器滚动接管，抢焦点会打断它。
    expect(shouldMoveFocusToMain(location('/post/1', { hash: '#section-2' }), location('/'))).toBe(
      false
    )
  })
})
