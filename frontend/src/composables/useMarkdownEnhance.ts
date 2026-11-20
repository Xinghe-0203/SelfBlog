/**
 * Markdown 正文 DOM 后处理：代码块头部 / 复制按钮 / 长代码折叠 / 标题锚点 / 图片收集。
 *
 * 为什么是"渲染后处理"而不是 marked 自定义 renderer：
 * 复制与折叠按钮必须是真实 <button> 才能接收事件，而写进 v-html 的标记 Vue 不会编译
 * （只当字符串插入），DOMPurify 也会按安全策略剥掉交互属性。所以等正文落进真实 DOM 后
 * 再统一加工，组件只负责调用与保存状态。
 *
 * 幂等：对同一 root 重复调用不会重复包装，随内容刷新安全地重复执行。
 *
 * 语法高亮是纯客户端的后处理（渐进增强）：highlight.js 只认已知语言、不做自动检测，
 * 服务端阅读 HTML 保持转义原文不参与。github-dark 主题提供 token 配色，
 * 代码块表面（背景/文字/边框）始终走 --code-* 语义 token（见 typography.css 的覆写）。
 */
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github-dark.css'

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

/**
 * root → 增强产物持有的资源清理函数（目前只有复制按钮的回退定时器）。
 * 用 WeakMap 挂载：root 被回收时条目自动消失，不需要显式的全局登记表。
 */
const disposers = new WeakMap<HTMLElement, Array<() => void>>()

function registerDisposer(root: HTMLElement, dispose: () => void): void {
  const list = disposers.get(root)
  if (list) list.push(dispose)
  else disposers.set(root, [dispose])
}

/**
 * 释放 root 上增强产物持有的资源。
 *
 * 正文被 v-html 重新渲染或组件卸载时调用：旧的复制按钮节点已经脱离文档，
 * 它排队的回退定时器却还会在 1500ms 后写一次 textContent。幂等，可重复调用。
 * 高亮不在此列：它只改 code 自身的 innerHTML，旧 DOM 被替换时 hljs class 随之消失，
 * 不需要（也不应该）在这里手动"撤销"高亮。
 */
export function disposeMarkdownEnhance(root: HTMLElement): void {
  const list = disposers.get(root)
  if (!list) return
  list.forEach((dispose) => dispose())
  disposers.delete(root)
}

function enhanceCodeBlocks(root: HTMLElement, collapseHeight: number): void {
  root.querySelectorAll<HTMLElement>('pre > code').forEach((code) => {
    const pre = code.parentElement
    // 已包装过的跳过：重复调用不叠加 wrapper / 头部 / 按钮（也不会重复高亮）
    if (!pre || pre.closest('.code-block')) return

    // 先高亮再包装：折叠阈值读的是 pre.scrollHeight，高度测量必须发生在
    // 最终内容（含高亮 span）定型之后、is-collapsed 类加上之前
    applyHighlight(code)

    const wrapper = document.createElement('div')
    wrapper.className = 'code-block'
    pre.parentNode?.insertBefore(wrapper, pre)
    wrapper.appendChild(pre)

    const head = document.createElement('div')
    head.className = 'code-block-head'
    const lang = document.createElement('span')
    lang.className = 'code-lang'
    lang.textContent = detectLanguage(code)
    head.append(lang, createCopyButton(root, code))
    wrapper.insertBefore(head, pre)

    applyCollapse(wrapper, pre, collapseHeight)
  })
}

/** marked 把围栏语言写成 class="language-js"；提取围栏语言标识，没有语言时返回 undefined。 */
function extractLanguage(code: HTMLElement): string | undefined {
  return /language-([\w-]+)/.exec(code.className)?.[1]
}

/** 语言徽标文案：没有语言时给个人类可读的兜底。 */
function detectLanguage(code: HTMLElement): string {
  return extractLanguage(code) ?? '文本'
}

/**
 * 对声明了已知语言的围栏代码块做语法高亮。
 *
 * - 已知语言：hljs 输出（转义文本 + 无属性 class span）写回 innerHTML，并挂 .hljs
 *   供主题（github-dark）配色；getLanguage 对别名与大小写都能命中（js/JS → javascript）。
 * - 未知语言 / 纯文本：保持原样 —— 不做自动检测，正文场景误判率高（会把散文当代码）。
 *
 * 安全性：输入取自已经 DOMPurify 消毒的 DOM 文本；hljs.highlight 的输出只含转义后的
 * 文本与 class span，写回 innerHTML 不引入新的注入面。
 * 高亮只改 code 自己的 innerHTML，不登记任何 dispose 资源 —— 正文随 v-html 重渲染时
 * 旧 DOM 连同 hljs class 一起被丢弃，不存在残留；幂等守卫（.code-block）挡住重复高亮。
 */
function applyHighlight(code: HTMLElement): void {
  const lang = extractLanguage(code)
  if (!lang || !hljs.getLanguage(lang)) return
  try {
    code.innerHTML = hljs.highlight(code.textContent ?? '', {
      language: lang,
      ignoreIllegals: true
    }).value
    code.classList.add('hljs')
  } catch {
    // 单个坏代码块不能让整段正文增强失败：保持原文即可
  }
}

function createCopyButton(root: HTMLElement, code: HTMLElement): HTMLButtonElement {
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
  registerDisposer(root, () => {
    if (!revertTimer) return
    clearTimeout(revertTimer)
    revertTimer = null
  })

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
  // 正文标题已被 `demoteMarkdownHeadings` 整体降级（# → h2 …），所以这里从 h2 起查；
  // h1 只属于页面标题，不该出现正文锚点。
  root.querySelectorAll<HTMLElement>('h2, h3, h4, h5, h6').forEach((heading) => {
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
