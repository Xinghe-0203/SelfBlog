import { describe, it, expect } from 'vitest'
import { countWords, estimateReadMinutes } from '../reading'

/** 把词序列拼成一段文本，用于构造"刚好 N 个词"的样本。 */
function words(count: number, word = 'word'): string {
  return Array.from({ length: count }, () => word).join(' ')
}

describe('countWords', () => {
  it('counts every CJK character individually', () => {
    expect(countWords('你好世界')).toBe(4)
    // 中文标点与换行不是字
    expect(countWords('第一句。\n第二句！')).toBe(6)
  })

  it('counts western words, ignoring punctuation', () => {
    expect(countWords('Hello world')).toBe(2)
    expect(countWords('Hello, world!')).toBe(2)
  })

  it('keeps apostrophes and hyphens inside a word', () => {
    expect(countWords("it's well-known")).toBe(2)
  })

  it('adds CJK characters and western words together', () => {
    expect(countWords('Hello 你好 world 世界')).toBe(6)
  })

  it('ignores fenced code blocks (both ``` and ~~~)', () => {
    const markdown = [
      '正文',
      '',
      '```js',
      'const answer = 42',
      '```',
      '',
      '~~~',
      'tilde code',
      '~~~',
      '',
      '结尾'
    ].join('\n')
    expect(countWords(markdown)).toBe(4)
  })

  it('ignores inline code', () => {
    expect(countWords('使用 `npm install` 安装依赖')).toBe(6)
    expect(countWords('run ``pnpm dlx foo`` now')).toBe(2)
  })

  it('does not count images at all', () => {
    expect(countWords('![截图](/uploads/screenshot-2026.png)')).toBe(0)
    expect(countWords('看图 ![alt text](https://cdn.example.com/a.png) 结束')).toBe(4)
  })

  it('counts link text but never the URL', () => {
    expect(countWords('[阅读全文](https://example.com/blog/post-1)')).toBe(4)
    expect(countWords('[Read more](https://example.com/blog/post-1)')).toBe(2)
  })

  it('ignores markdown markers so headings and emphasis are not inflated', () => {
    expect(countWords('## 标题\n\n**加粗**文字')).toBe(6)
  })

  it('returns 0 for empty and whitespace-only input', () => {
    expect(countWords('')).toBe(0)
    expect(countWords('   \n\t  ')).toBe(0)
  })
})

describe('estimateReadMinutes', () => {
  it('returns at least one minute for empty input', () => {
    expect(estimateReadMinutes('')).toBe(1)
    expect(estimateReadMinutes('短句')).toBe(1)
  })

  it('rounds up to the next whole minute', () => {
    // 300 词 = 1 分钟
    expect(estimateReadMinutes(words(300))).toBe(1)
    // 301 词跨过一分钟边界 → 2
    expect(estimateReadMinutes(words(301))).toBe(2)
    expect(estimateReadMinutes(words(600))).toBe(2)
    expect(estimateReadMinutes(words(601))).toBe(3)
  })

  it('honours a custom words-per-minute rate', () => {
    expect(estimateReadMinutes(words(250), 100)).toBe(3)
  })
})
