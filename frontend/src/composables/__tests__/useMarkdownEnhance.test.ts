import { describe, it, expect, afterEach, vi } from 'vitest'
import { enhanceMarkdown, disposeMarkdownEnhance } from '../useMarkdownEnhance'

/** 挂到文档里再增强 —— 复制/折叠都要求节点是真实可交互的 DOM。 */
function fixture(html: string): HTMLElement {
  const root = document.createElement('div')
  root.className = 'markdown-body'
  root.innerHTML = html
  document.body.appendChild(root)
  return root
}

function query<T extends Element>(parent: ParentNode, selector: string): T {
  const found = parent.querySelector<T>(selector)
  if (!found) throw new Error(`缺少匹配 ${selector} 的节点`)
  return found
}

/** clipboard 在 jsdom 里不存在，需要显式造一个。 */
function stubClipboard(writeText: (text: string) => Promise<void>): void {
  Object.defineProperty(navigator, 'clipboard', {
    value: { writeText },
    configurable: true
  })
}

/**
 * 等待若干轮原生微任务。剪贴板 mock 用已决议的 Promise，
 * click 处理函数里的 async 续体排在原生微任务队列里 —— 与假定时器互不干扰。
 */
async function flushMicrotasks(times = 3): Promise<void> {
  for (let i = 0; i < times; i++) await Promise.resolve()
}

afterEach(() => {
  vi.useRealTimers()
  document.body.innerHTML = ''
})

describe('enhanceMarkdown — 代码块', () => {
  it('wraps a code block with a header carrying the language and a copy button', () => {
    const root = fixture('<pre><code class="language-js">const a = 1</code></pre>')

    enhanceMarkdown(root)

    const block = query<HTMLElement>(root, '.code-block')
    // 结构：div.code-block > (div.code-block-head + pre > code)
    expect(block.parentElement).toBe(root)
    expect(block.querySelector('pre > code')?.textContent).toBe('const a = 1')
    expect(query(root, '.code-block-head .code-lang').textContent).toBe('js')
    const copy = query<HTMLButtonElement>(root, '.code-block-head .code-copy')
    expect(copy.tagName).toBe('BUTTON')
    expect(copy.getAttribute('type')).toBe('button')
    expect(copy.textContent).toBe('复制')
  })

  it('falls back to 文本 when the code element has no language class', () => {
    const root = fixture('<pre><code>plain</code></pre>')
    enhanceMarkdown(root)
    expect(query(root, '.code-lang').textContent).toBe('文本')
  })

  it('copies the code text, flashes 已复制 and reverts after 1500ms', async () => {
    const writeText = vi.fn<(text: string) => Promise<void>>().mockResolvedValue(undefined)
    stubClipboard(writeText)
    const root = fixture('<pre><code class="language-ts">let x = 1</code></pre>')
    enhanceMarkdown(root)
    const copy = query<HTMLButtonElement>(root, '.code-copy')

    // 假定时器必须在点击前安装：回退用的 setTimeout 是在点击处理函数里创建的
    vi.useFakeTimers()
    try {
      copy.click()
      await flushMicrotasks()

      expect(writeText).toHaveBeenCalledWith('let x = 1')
      expect(copy.textContent).toBe('已复制')

      vi.advanceTimersByTime(1500)
      expect(copy.textContent).toBe('复制')
    } finally {
      vi.useRealTimers()
    }
  })

  it('flashes 复制失败 when the clipboard rejects', async () => {
    stubClipboard(vi.fn<(text: string) => Promise<void>>().mockRejectedValue(new Error('denied')))
    const root = fixture('<pre><code>oops</code></pre>')
    enhanceMarkdown(root)
    const copy = query<HTMLButtonElement>(root, '.code-copy')

    vi.useFakeTimers()
    try {
      copy.click()
      await flushMicrotasks()
      expect(copy.textContent).toBe('复制失败')

      vi.advanceTimersByTime(1500)
      expect(copy.textContent).toBe('复制')
    } finally {
      vi.useRealTimers()
    }
  })
})

describe('enhanceMarkdown — 长代码折叠', () => {
  it('collapses an over-height code block and toggles it from the expand button', () => {
    const root = fixture('<pre><code>long</code></pre>')
    // jsdom 没有布局引擎，scrollHeight 恒为 0；在增强前造出超出阈值的高度
    Object.defineProperty(query(root, 'pre'), 'scrollHeight', { value: 500, configurable: true })

    enhanceMarkdown(root)

    const block = query<HTMLElement>(root, '.code-block')
    expect(block.classList.contains('is-collapsed')).toBe(true)
    const expand = query<HTMLButtonElement>(root, '.code-expand')
    expect(expand.textContent).toBe('展开代码')

    expand.click()
    expect(block.classList.contains('is-collapsed')).toBe(false)
    expect(expand.textContent).toBe('收起代码')

    expand.click()
    expect(block.classList.contains('is-collapsed')).toBe(true)
    expect(expand.textContent).toBe('展开代码')
  })

  it('leaves a short code block expanded and adds no expand button', () => {
    const root = fixture('<pre><code>short</code></pre>')
    Object.defineProperty(query(root, 'pre'), 'scrollHeight', { value: 120, configurable: true })

    enhanceMarkdown(root)

    expect(root.querySelector('.code-expand')).toBeNull()
    expect(query(root, '.code-block').classList.contains('is-collapsed')).toBe(false)
  })

  it('honours a custom collapseHeight option', () => {
    const root = fixture('<pre><code>x</code></pre>')
    Object.defineProperty(query(root, 'pre'), 'scrollHeight', { value: 200, configurable: true })

    enhanceMarkdown(root, { collapseHeight: 100 })

    expect(query(root, '.code-block').classList.contains('is-collapsed')).toBe(true)
    expect(query(root, '.code-expand').textContent).toBe('展开代码')
  })
})

describe('enhanceMarkdown — 标题锚点', () => {
  it('appends an anchor link to headings that have an id', () => {
    const root = fixture(
      '<h1>没有 id 的标题</h1><h2 id="section-0">第一节</h2><h3 id="section-1">小节</h3>'
    )

    enhanceMarkdown(root)

    expect(root.querySelectorAll('.heading-anchor')).toHaveLength(2)
    const anchor = query<HTMLAnchorElement>(root, 'h2 .heading-anchor')
    expect(anchor.getAttribute('href')).toBe('#section-0')
    expect(anchor.getAttribute('aria-label')).toBe('链接到本节')
    expect(anchor.textContent).toBe('#')
    expect(root.querySelector('h1 .heading-anchor')).toBeNull()
  })
})

describe('enhanceMarkdown — 图片收集', () => {
  it('returns unique non-empty image sources in document order', () => {
    const root = fixture('<img src="/a.png"><img src="/b.png"><img src="/a.png"><img src=""><img>')
    expect(enhanceMarkdown(root)).toEqual(['/a.png', '/b.png'])
  })

  it('returns an empty array when there is nothing to collect', () => {
    expect(enhanceMarkdown(fixture('<p>正文</p>'))).toEqual([])
  })
})

describe('enhanceMarkdown — 幂等性', () => {
  it('adds no duplicate wrappers, buttons or anchors when run twice on the same root', () => {
    const root = fixture(
      '<h2 id="section-0">标题</h2><pre><code class="language-js">x</code></pre>'
    )
    Object.defineProperty(query(root, 'pre'), 'scrollHeight', { value: 500, configurable: true })

    enhanceMarkdown(root)
    enhanceMarkdown(root)

    expect(root.querySelectorAll('.code-block')).toHaveLength(1)
    expect(root.querySelectorAll('.code-block-head')).toHaveLength(1)
    expect(root.querySelectorAll('.code-copy')).toHaveLength(1)
    expect(root.querySelectorAll('.code-expand')).toHaveLength(1)
    expect(root.querySelectorAll('.heading-anchor')).toHaveLength(1)
    expect(root.querySelectorAll('pre')).toHaveLength(1)
  })
})

describe('enhanceMarkdown — 资源清理', () => {
  it('cancels the pending copy-feedback timer when the root is disposed', async () => {
    const writeText = vi.fn<(text: string) => Promise<void>>().mockResolvedValue(undefined)
    stubClipboard(writeText)
    const root = fixture('<pre><code class="language-ts">let x = 1</code></pre>')
    enhanceMarkdown(root)
    const copy = query<HTMLButtonElement>(root, '.code-copy')

    vi.useFakeTimers()
    try {
      copy.click()
      await flushMicrotasks()
      expect(copy.textContent).toBe('已复制')

      // 正文被重新渲染：旧按钮的节点即将被丢弃，回退定时器必须先释放
      disposeMarkdownEnhance(root)
      vi.advanceTimersByTime(1500)
      expect(copy.textContent).toBe('已复制')
    } finally {
      vi.useRealTimers()
    }
  })

  it('is a no-op for a root that was never enhanced or already disposed', () => {
    const root = fixture('<p>正文</p>')
    expect(() => disposeMarkdownEnhance(root)).not.toThrow()
    enhanceMarkdown(root)
    disposeMarkdownEnhance(root)
    expect(() => disposeMarkdownEnhance(root)).not.toThrow()
  })

  it('does not cancel timers owned by another root', async () => {
    const writeText = vi.fn<(text: string) => Promise<void>>().mockResolvedValue(undefined)
    stubClipboard(writeText)
    const first = fixture('<pre><code class="language-ts">let a = 1</code></pre>')
    const second = fixture('<pre><code class="language-ts">let b = 2</code></pre>')
    enhanceMarkdown(first)
    enhanceMarkdown(second)
    const copyFirst = query<HTMLButtonElement>(first, '.code-copy')
    const copySecond = query<HTMLButtonElement>(second, '.code-copy')

    vi.useFakeTimers()
    try {
      copyFirst.click()
      copySecond.click()
      await flushMicrotasks()
      disposeMarkdownEnhance(first)
      vi.advanceTimersByTime(1500)
      expect(copyFirst.textContent).toBe('已复制')
      expect(copySecond.textContent).toBe('复制')
    } finally {
      vi.useRealTimers()
    }
  })
})
