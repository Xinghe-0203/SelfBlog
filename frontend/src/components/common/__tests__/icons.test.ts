import { describe, it, expect } from 'vitest'
import { icons, iconNames, type IconName } from '../icons'

/** 图标片段允许出现的元素：只用描边绘制，避免混入 <style>/<script>。 */
const ALLOWED_TAGS = ['path', 'circle', 'rect', 'line', 'polyline', 'polygon', 'ellipse']

const tagPattern = /<\/?([a-zA-Z][a-zA-Z0-9-]*)/g

function tagsIn(fragment: string): string[] {
  return [...fragment.matchAll(tagPattern)].map((m) => (m[1] ?? '').toLowerCase())
}

describe('icon registry', () => {
  it('exposes a non-empty list of names matching the registry keys', () => {
    expect(iconNames.length).toBeGreaterThanOrEqual(21)
    expect([...iconNames].sort()).toEqual(Object.keys(icons).sort())
  })

  it('includes every icon the UI references', () => {
    const required: IconName[] = [
      'logo',
      'search',
      'sun',
      'moon',
      'menu',
      'close',
      'home',
      'archive',
      'image',
      'info',
      'settings',
      'gauge',
      'file-text',
      'tag',
      'users',
      'message',
      'music',
      'play',
      'pause',
      'arrow-up',
      'external'
    ]
    for (const name of required) {
      expect(iconNames).toContain(name)
    }
  })

  it.each(Object.keys(icons))('%s contains at least one drawing element', (name) => {
    const fragment = icons[name as IconName]
    expect(fragment.trim().length).toBeGreaterThan(0)
    expect(tagsIn(fragment).length).toBeGreaterThan(0)
  })

  it.each(Object.keys(icons))('%s only uses stroke-safe elements', (name) => {
    for (const tag of tagsIn(icons[name as IconName])) {
      expect(ALLOWED_TAGS).toContain(tag)
    }
  })

  it.each(Object.keys(icons))('%s has no inline handlers or style blocks', (name) => {
    const fragment = icons[name as IconName]
    expect(fragment).not.toMatch(/<script|<style|on[a-z]+\s*=/i)
  })
})
