/**
 * 文章 / 页面 Markdown 的统一渲染链路。
 *
 * 之前三个渲染点（PostDetail、PostEdit 预览、MarkdownContent）各自调 marked + DOMPurify，
 * 并用正则 `<a\s` 给所有链接补 target。那套写法有两个问题：
 *
 * 1. marked 只在 GFM autolink（裸 `https://…` 或裸 `www.…`）时补协议；
 *    markdown 链接语法 `[文字](example.com)` 的 href 会原样保留成相对地址，
 *    浏览器把它解析成 `https://本站/example.com`，SPA 兜底路由命中 NotFound ——
 *    也就是用户看到的「点文章里的 URL 跳到本网站的 404」。
 * 2. 正则 `/<a\s/` 会命中 `<area `、`<abbr ` 等标签开头，且无差别给站内锚点加
 *    `target="_blank"`，点个 `#section` 就弹新标签。
 *
 * 这里改成「DOMPurify 先净化 → DOMParser 遍历真实 DOM 逐个修 href → 按站内/站外
 * 决定外链属性」，三处共用同一份实现，行为不会再漂移。
 */
import DOMPurify from 'dompurify'
import { marked } from 'marked'

/** 危险协议直接判空，避免渲染出可执行链接（DOMPurify 通常已拦，这里做二次兜底）。 */
const DANGEROUS_SCHEME = /^(?:javascript|vbscript|data|file):/i

/** 明确安全的协议，原样保留。注意只认白名单：`example.com:8080` 不能被当成 scheme。 */
const SAFE_SCHEME = /^(?:https?|mailto|tel|ftp|sms|irc|news|nntp|feed|magnet):/i

/** 常见文件名后缀。带这些后缀的「域名形态」多半是相对文件引用，不该补 https://。 */
const FILE_EXTENSIONS = new Set([
  'md',
  'markdown',
  'png',
  'jpg',
  'jpeg',
  'gif',
  'webp',
  'svg',
  'avif',
  'bmp',
  'ico',
  'pdf',
  'txt',
  'json',
  'js',
  'mjs',
  'ts',
  'tsx',
  'jsx',
  'vue',
  'css',
  'scss',
  'html',
  'htm',
  'xml',
  'yml',
  'yaml',
  'csv',
  'zip',
  'tar',
  'gz',
  'mp4',
  'mov',
  'avi',
  'mp3',
  'wav',
  'm4a',
  'ogg',
  'exe',
  'apk'
])

const EMAIL = /^[^\s@/?#]+@[^\s@/?#]+\.[a-zA-Z]{2,24}$/

/** 判断 `example.com`、`www.example.com/a/b` 这类「缺协议的裸域名」。 */
function looksLikeBareHost(value: string): boolean {
  if (!value || /\s/.test(value)) return false
  const authority = value.split(/[/?#]/, 1)[0] ?? ''
  if (!authority || authority.includes('@')) return false
  const labels = authority.split('.')
  if (labels.length < 2) return false
  const first = labels[0] ?? ''
  const last = labels[labels.length - 1] ?? ''
  // 单字母首段多半是文件名（a.md）而不是域名。
  if (first.length < 2) return false
  if (!/^[a-zA-Z]{2,24}$/.test(last)) return false
  if (FILE_EXTENSIONS.has(last.toLowerCase())) return false
  if (labels.some((label) => !/^[a-zA-Z0-9-]+$/.test(label))) return false
  return true
}

/**
 * 把 markdown 链接的 href 规范成浏览器能正确解析的地址。
 *
 * - `example.com` / `www.example.com/a` → 补 `https://`
 * - `a@b.com` → 补 `mailto:`
 * - `#anchor`、`/post/2`、`?page=2`、`//cdn.example.com`、`https://…`、`mailto:…` 原样保留
 * - `javascript:` 等危险协议 → 返回空串（调用方应移除 href）
 */
export function normalizeMarkdownHref(raw: string | null | undefined): string {
  const value = (raw ?? '').trim()
  if (!value) return ''
  if (DANGEROUS_SCHEME.test(value)) return ''
  if (SAFE_SCHEME.test(value)) return value
  if (value.startsWith('//')) return value
  if (value.startsWith('#') || value.startsWith('/') || value.startsWith('?')) return value
  // 以 . 开头是相对文件引用（./docs/a.md），补协议只会更错。
  if (value.startsWith('.')) return value
  if (EMAIL.test(value)) return `mailto:${value}`
  if (looksLikeBareHost(value)) return `https://${value}`
  return value
}

/** 站内链接：`#`、`?`、`/`（但不是 `//`）、或与当前 origin 相同的绝对地址。 */
export function isInternalMarkdownHref(href: string | null | undefined): boolean {
  const value = (href ?? '').trim()
  if (!value) return false
  if (value.startsWith('#')) return true
  if (value.startsWith('?')) return true
  if (value.startsWith('//')) return false
  if (value.startsWith('/')) return true
  if (!/^https?:/i.test(value)) return false
  try {
    const parsed = new URL(value)
    const origin = typeof location === 'undefined' ? '' : location.origin
    return !!origin && parsed.origin === origin
  } catch {
    return false
  }
}

const SANITIZE_OPTIONS = {
  USE_PROFILES: { html: true },
  ADD_ATTR: ['target', 'rel', 'referrerpolicy', 'loading'],
  FORBID_TAGS: ['style', 'script', 'iframe', 'form', 'input', 'button', 'textarea', 'select'],
  FORBID_ATTR: ['style', 'onerror', 'onload', 'onclick', 'onmouseover']
}

/**
 * 净化 marked 输出的 HTML，并逐个修正链接与图片属性。
 *
 * 顺序很关键：先 DOMPurify（此时 `javascript:` 之类已经被清掉），
 * 再用 DOMParser 拿真实 DOM 修改，最后序列化回去 —— 全程不碰正则匹配标签。
 */
export function sanitizeMarkdownHtml(rawHtml: string): string {
  const clean = DOMPurify.sanitize(rawHtml, SANITIZE_OPTIONS)
  if (!clean) return ''
  const doc = new DOMParser().parseFromString(clean, 'text/html')

  doc.querySelectorAll('a[href]').forEach((anchor) => {
    const original = anchor.getAttribute('href') ?? ''
    const href = normalizeMarkdownHref(original)
    if (!href) {
      anchor.removeAttribute('href')
      anchor.removeAttribute('target')
      anchor.removeAttribute('rel')
      return
    }
    if (href !== original) anchor.setAttribute('href', href)
    anchor.setAttribute('referrerpolicy', 'no-referrer')
    if (isInternalMarkdownHref(href)) {
      // 站内链接交给浏览器/路由自己处理，不要新开标签。
      anchor.removeAttribute('target')
      anchor.removeAttribute('rel')
    } else {
      anchor.setAttribute('target', '_blank')
      anchor.setAttribute('rel', 'noopener noreferrer')
    }
  })

  doc.querySelectorAll('img').forEach((image) => {
    image.setAttribute('referrerpolicy', 'no-referrer')
    if (!image.hasAttribute('loading')) image.setAttribute('loading', 'lazy')
  })

  return doc.body.innerHTML
}

/** marked 解析 + 上面的净化链路，三处渲染点统一走这一个入口。 */
export function renderMarkdown(source: string | null | undefined): string {
  if (!source) return ''
  const parsed = marked.parse(source, { async: false }) as string
  return sanitizeMarkdownHtml(parsed)
}
