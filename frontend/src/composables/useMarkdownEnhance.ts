/**
 * Markdown 正文 DOM 后处理：代码块头部 / 复制按钮 / 长代码折叠 / 标题锚点 / 图片收集。
 *
 * 为什么是"渲染后处理"而不是 marked 自定义 renderer：
 * 复制与折叠按钮必须是真实 <button> 才能接收事件，而写进 v-html 的标记 Vue 不会编译
 * （只当字符串插入），DOMPurify 也会按安全策略剥掉交互属性。所以等正文落进真实 DOM 后
 * 再统一加工，组件只负责调用与保存状态。
 *
 * 幂等：对同一 root 重复调用不会重复包装，随内容刷新安全地重复执行。
 */

export interface MarkdownEnhanceOptions {
  /** 超过该高度（px）的代码块折叠起来，默认 320。 */
  collapseHeight?: number
}

const DEFAULT_COLLAPSE_HEIGHT = 320
const COPY_LABEL = '复制'
const COPIED_LABEL = '已复制'
const COPY_FAILED_LABEL = '复制失败'
const EXPAND_LABEL = '展开代码'
const COLLAPSE_LABEL = '收起代码'
/** 复制反馈时长：太短用户看不清，太长按钮上的文案会一直停在反馈态。 */
const COPY_FEEDBACK_MS = 1500

/**
 * 增强 root 内的 Markdown 正文。
 *
 * @returns 正文图片的 src 列表（按文档顺序去重），供灯箱从正确位置开始浏览。
 */
export function enhanceMarkdown(root: HTMLElement, options?: MarkdownEnhanceOptions): string[] {
  enhanceCodeBlocks(root, options?.collapseHeight ?? DEFAULT_COLLAPSE_HEIGHT)
  enhanceHeadingAnchors(root)
  return collectImageSources(root)
}

function enhanceCodeBlocks(root: HTMLElement, collapseHeight: number): void {
  root.querySelectorAll<HTMLElement>('pre > code').forEach((code) => {
    const pre = code.parentElement
    // 已包装过的跳过：重复调用不叠加 wrapper / 头部 / 按钮
    if (!pre || pre.closest('.code-block')) return

    const wrapper = document.createElement('div')
    wrapper.className = 'code-block'
    pre.parentNode?.insertBefore(wrapper, pre)
    wrapper.appendChild(pre)

    const head = document.createElement('div')
    head.className = 'code-block-head'
    const lang = document.createElement('span')
    lang.className = 'code-lang'
    lang.textContent = detectLanguage(code)
    head.append(lang, createCopyButton(code))
    wrapper.insertBefore(head, pre)

    applyCollapse(wrapper, pre, collapseHeight)
  })
}

/** marked 把围栏语言写成 class="language-js"；没有语言时给个人类可读的兜底。 */
function detectLanguage(code: HTMLElement): string {
  return /language-([\w-]+)/.exec(code.className)?.[1] ?? '文本'
}

function createCopyButton(code: HTMLElement): HTMLButtonElement {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'code-copy'
  button.textContent = COPY_LABEL

  // 回退定时器按按钮闭包持有：多个代码块互不干扰，重复点击也不会叠加多个定时器。
  let revertTimer: ReturnType<typeof setTimeout> | null = null
  const flash = (label: string): void => {
    button.textContent = label
    if (revertTimer) clearTimeout(revertTimer)
    revertTimer = setTimeout(() => {
      button.textContent = COPY_LABEL
      revertTimer = null
    }, COPY_FEEDBACK_MS)
  }

  button.addEventListener('click', () => {
    void (async () => {
      try {
        await navigator.clipboard.writeText(code.textContent ?? '')
        flash(COPIED_LABEL)
      } catch {
        // 非安全上下文（http 域名）或用户拒绝授权：明确失败而不是假装成功
        flash(COPY_FAILED_LABEL)
      }
    })()
  })

  return button
}

function applyCollapse(wrapper: HTMLElement, pre: HTMLElement, collapseHeight: number): void {
  // 必须在加 is-collapsed 之前读高度：类一加上 max-height 就会把 scrollHeight 截到阈值
  if (pre.scrollHeight <= collapseHeight) return

  wrapper.classList.add('is-collapsed')
  const expand = document.createElement('button')
  expand.type = 'button'
  expand.className = 'code-expand'
  expand.textContent = EXPAND_LABEL
  expand.addEventListener('click', () => {
    const collapsed = wrapper.classList.toggle('is-collapsed')
    expand.textContent = collapsed ? EXPAND_LABEL : COLLAPSE_LABEL
  })
  wrapper.appendChild(expand)
}

function enhanceHeadingAnchors(root: HTMLElement): void {
  root.querySelectorAll<HTMLElement>('h1, h2, h3').forEach((heading) => {
    const id = heading.id
    // 没有 id 的标题无法被锚定（setHeadingIds 会给正文标题补 id）
    if (!id || heading.querySelector('.heading-anchor')) return

    const anchor = document.createElement('a')
    anchor.className = 'heading-anchor'
    anchor.href = `#${id}`
    anchor.setAttribute('aria-label', '链接到本节')
    anchor.textContent = '#'
    heading.appendChild(anchor)
  })
}

/**
 * 收集正文图片（文档顺序去重）供灯箱使用。
 * 取 getAttribute('src') 而非 img.src：后者会被解析成带站点前缀的绝对 URL，
 * 与 PostDetail 里定位"点的是哪一张"所用的原始 src 不一致。
 */
function collectImageSources(root: HTMLElement): string[] {
  const sources: string[] = []
  root.querySelectorAll('img').forEach((img) => {
    const src = img.getAttribute('src')
    if (src && !sources.includes(src)) sources.push(src)
  })
  return sources
}
