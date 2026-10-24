import { describe, expect, it } from 'vitest'
import {
  isInternalMarkdownHref,
  normalizeMarkdownHref,
  renderMarkdown,
  sanitizeMarkdownHtml
} from '../markdown'

describe('normalizeMarkdownHref', () => {
  it('补全缺协议的裸域名，修复"点链接跳到本站 404"', () => {
    expect(normalizeMarkdownHref('example.com')).toBe('https://example.com')
    expect(normalizeMarkdownHref('www.example.com')).toBe('https://www.example.com')
    expect(normalizeMarkdownHref('example.com/a/b?x=1')).toBe('https://example.com/a/b?x=1')
    expect(normalizeMarkdownHref('  example.com  ')).toBe('https://example.com')
    expect(normalizeMarkdownHref('sub.example.co.uk/path')).toBe('https://sub.example.co.uk/path')
  })

  it('保留已有协议、锚点、站内路径与协议相对地址', () => {
    expect(normalizeMarkdownHref('https://example.com/a')).toBe('https://example.com/a')
    expect(normalizeMarkdownHref('http://example.com')).toBe('http://example.com')
    expect(normalizeMarkdownHref('//cdn.example.com/a.js')).toBe('//cdn.example.com/a.js')
    expect(normalizeMarkdownHref('#section-1')).toBe('#section-1')
    expect(normalizeMarkdownHref('/post/2')).toBe('/post/2')
    expect(normalizeMarkdownHref('?page=2')).toBe('?page=2')
    expect(normalizeMarkdownHref('./docs/a.md')).toBe('./docs/a.md')
    expect(normalizeMarkdownHref('mailto:a@b.com')).toBe('mailto:a@b.com')
  })

  it('裸邮箱补 mailto，危险协议一律清空', () => {
    expect(normalizeMarkdownHref('a@b.com')).toBe('mailto:a@b.com')
    expect(normalizeMarkdownHref('javascript:alert(1)')).toBe('')
    expect(normalizeMarkdownHref('JAVASCRIPT:alert(1)')).toBe('')
    expect(normalizeMarkdownHref('data:text/html,<script>')).toBe('')
    expect(normalizeMarkdownHref('')).toBe('')
    expect(normalizeMarkdownHref(null)).toBe('')
  })

  it('不把相对文件名误判成域名', () => {
    expect(normalizeMarkdownHref('a.md')).toBe('a.md')
    expect(normalizeMarkdownHref('notes.txt')).toBe('notes.txt')
    expect(normalizeMarkdownHref('photo.png')).toBe('photo.png')
    expect(normalizeMarkdownHref('single')).toBe('single')
    expect(normalizeMarkdownHref('has space.com')).toBe('has space.com')
  })
})

describe('isInternalMarkdownHref', () => {
  it('识别站内地址', () => {
    expect(isInternalMarkdownHref('#top')).toBe(true)
    expect(isInternalMarkdownHref('?page=2')).toBe(true)
    expect(isInternalMarkdownHref('/post/1')).toBe(true)
    expect(isInternalMarkdownHref(`${location.origin}/post/1`)).toBe(true)
  })

  it('识别站外地址', () => {
    expect(isInternalMarkdownHref('https://example.com')).toBe(false)
    expect(isInternalMarkdownHref('//cdn.example.com/x')).toBe(false)
    expect(isInternalMarkdownHref('mailto:a@b.com')).toBe(false)
    expect(isInternalMarkdownHref('')).toBe(false)
  })
})

describe('sanitizeMarkdownHtml', () => {
  it('给站外链接补 target/rel，站内链接不新开标签', () => {
    const html = sanitizeMarkdownHtml(
      '<p><a href="example.com/a">外站</a><a href="/post/2">站内</a><a href="#top">锚点</a></p>'
    )
    const doc = new DOMParser().parseFromString(html, 'text/html')
    const anchors = Array.from(doc.querySelectorAll('a'))

    expect(anchors[0]?.getAttribute('href')).toBe('https://example.com/a')
    expect(anchors[0]?.getAttribute('target')).toBe('_blank')
    expect(anchors[0]?.getAttribute('rel')).toBe('noopener noreferrer')
    expect(anchors[0]?.getAttribute('referrerpolicy')).toBe('no-referrer')

    expect(anchors[1]?.getAttribute('href')).toBe('/post/2')
    expect(anchors[1]?.hasAttribute('target')).toBe(false)
    expect(anchors[2]?.getAttribute('href')).toBe('#top')
    expect(anchors[2]?.hasAttribute('target')).toBe(false)
  })

  it('危险协议被剥掉 href，不会渲染成可执行链接', () => {
    const html = sanitizeMarkdownHtml('<a href="javascript:alert(1)">x</a>')
    const doc = new DOMParser().parseFromString(html, 'text/html')
    const anchor = doc.querySelector('a')
    expect(anchor?.getAttribute('href') ?? '').not.toContain('javascript')
    expect(anchor?.hasAttribute('target')).toBe(false)
  })

  it('图片补 referrerpolicy 与懒加载，且不误伤 area/abbr', () => {
    const html = sanitizeMarkdownHtml(
      '<img src="/uploads/a.png" alt="a"><map name="m"><area href="/post/1" alt="x"></map><abbr title="t">ab</abbr>'
    )
    const doc = new DOMParser().parseFromString(html, 'text/html')
    expect(doc.querySelector('img')?.getAttribute('referrerpolicy')).toBe('no-referrer')
    expect(doc.querySelector('img')?.getAttribute('loading')).toBe('lazy')
    // 旧实现的正则 /<a\s/ 会给 area 也塞 target，这里必须没有
    expect(doc.querySelector('area')?.hasAttribute('target')).toBe(false)
    expect(doc.querySelector('abbr')?.getAttribute('title')).toBe('t')
  })

  it('清掉脚本、内联事件与样式', () => {
    const html = sanitizeMarkdownHtml(
      '<script>alert(1)</script><iframe src="x"></iframe><p style="color:red" onclick="alert(1)">hi</p>'
    )
    expect(html).not.toContain('<script')
    expect(html).not.toContain('<iframe')
    expect(html).not.toContain('onclick')
    expect(html).not.toContain('style=')
  })
})

describe('renderMarkdown', () => {
  it('空内容返回空串', () => {
    expect(renderMarkdown('')).toBe('')
    expect(renderMarkdown(null)).toBe('')
    expect(renderMarkdown(undefined)).toBe('')
  })

  it('markdown 链接语法里的裸域名会被补全为 https 绝对地址', () => {
    const html = renderMarkdown('见 [示例](example.com/page)。')
    expect(html).toContain('href="https://example.com/page"')
    expect(html).toContain('target="_blank"')
  })

  it('站内锚点与站内路径保持原样且不新开标签', () => {
    const html = renderMarkdown('[锚点](#a)\n\n[文章](/post/1)')
    expect(html).toContain('href="#a"')
    expect(html).toContain('href="/post/1"')
    expect(html).not.toContain('target="_blank"')
  })

  it('GFM 表格/删除线/任务列表仍然正常', () => {
    const html = renderMarkdown('| a | b |\n| --- | --- |\n| 1 | 2 |\n\n~~删除~~\n\n- [ ] 待办')
    expect(html).toContain('<table>')
    expect(html).toContain('<del>')
    expect(html).toContain('待办')
  })

  it('裸 URL 与 www 开头地址都能跳对', () => {
    expect(renderMarkdown('https://example.com/x')).toContain('href="https://example.com/x"')
    expect(renderMarkdown('www.example.com/x')).toContain('href="http://www.example.com/x"')
  })
})
