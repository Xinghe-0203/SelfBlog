import { afterEach, describe, it, expect } from 'vitest'
import { shouldOpenSearch } from '../searchShortcut'

/** 构造一个挂在 document 上的目标元素，模拟真实 keydown 的 event.target。 */
function targetOf(tag: string, contenteditable = false): EventTarget {
  const el = document.createElement(tag)
  if (contenteditable) el.setAttribute('contenteditable', 'true')
  document.body.appendChild(el)
  return el
}

describe('shouldOpenSearch', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('matches Ctrl+K and Cmd+K in both casing', () => {
    expect(shouldOpenSearch(null, 'k', { ctrlKey: true })).toBe(true)
    expect(shouldOpenSearch(null, 'k', { metaKey: true })).toBe(true)
    // 大写锁定时 e.key 是 'K'，同样要命中
    expect(shouldOpenSearch(null, 'K', { ctrlKey: true })).toBe(true)
    expect(shouldOpenSearch(null, 'K', { metaKey: true })).toBe(true)
  })

  it('matches a bare slash but not with Ctrl/Meta held', () => {
    expect(shouldOpenSearch(null, '/', {})).toBe(true)
    expect(shouldOpenSearch(null, '/', { ctrlKey: true })).toBe(false)
    expect(shouldOpenSearch(null, '/', { metaKey: true })).toBe(false)
  })

  it('ignores other keys without modifiers', () => {
    expect(shouldOpenSearch(null, 'k', {})).toBe(false)
    expect(shouldOpenSearch(null, '?', {})).toBe(false)
    expect(shouldOpenSearch(null, 'j', { ctrlKey: true })).toBe(false)
  })

  it('ignores held-down repeats', () => {
    expect(shouldOpenSearch(null, 'k', { ctrlKey: true, repeat: true })).toBe(false)
    expect(shouldOpenSearch(null, '/', { repeat: true })).toBe(false)
  })

  it('never fires while typing in inputs, textareas, selects or contenteditable', () => {
    for (const tag of ['input', 'textarea', 'select']) {
      expect(shouldOpenSearch(targetOf(tag), 'k', { ctrlKey: true })).toBe(false)
      expect(shouldOpenSearch(targetOf(tag), '/', {})).toBe(false)
    }
    const editable = targetOf('div', true)
    expect(shouldOpenSearch(editable, 'k', { ctrlKey: true })).toBe(false)
    expect(shouldOpenSearch(editable, '/', {})).toBe(false)
  })

  it('treats non-element targets (window, document) as editable-free', () => {
    expect(shouldOpenSearch(document, '/', {})).toBe(true)
    expect(shouldOpenSearch(null, '/', {})).toBe(true)
  })
})
