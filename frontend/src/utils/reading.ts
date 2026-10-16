/**
 * 正文文本统计 —— 预计阅读时长。
 *
 * 从 Markdown 源码估算字数：先剔除非正文内容（代码块、行内代码、图片），
 * 再按"中日韩字符按字 + 西文按词"分别计数后相加。
 * 直接拿 content.length 当字数会把 Markdown 语法和中英混排都算错：
 * 一段代码块或一个中文标题就能让估算翻倍（中文字符串长度与信息量都不是 1:1 的）。
 */

/** 中日韩字符：基本区 + 扩展 A + 兼容汉字区 */
const CJK_PATTERN = /[\u4e00-\u9fff\u3400-\u4dbf\uf900-\ufaff]/g

/** 西文单词：允许词内出现 ' 与 -（如 it's、well-known） */
const WESTERN_WORD_PATTERN = /[A-Za-z0-9]+(?:['-][A-Za-z0-9]+)*/g

/**
 * 去掉围栏代码块（``` 与 ~~~ 两种围栏）。
 *
 * 逐行扫描而不是一条正则：围栏需要配对（``` 只能被 ``` 关闭），
 * 且未闭合的代码块要一直吃到文末 —— 单条正则很难同时表达这两点。
 */
function stripFencedCodeBlocks(markdown: string): string {
  const kept: string[] = []
  let openFence: string | null = null

  for (const line of markdown.split('\n')) {
    const fence = /^[ \t]*(`{3,}|~{3,})/.exec(line)?.[1]
    if (openFence) {
      // 代码块内：只有同种字符（``` 或 ~~~）的围栏行才能关闭它，围栏行本身不保留
      if (fence && fence[0] === openFence[0]) openFence = null
      continue
    }
    if (fence) {
      openFence = fence
      continue
    }
    kept.push(line)
  }

  return kept.join('\n')
}

/** 剔除代码、图片，并去掉 Markdown 标记，只留下会被人读到的文字。 */
function stripMarkdownNoise(markdown: string): string {
  let text = stripFencedCodeBlocks(markdown)

  // 行内代码不是散文，`` 形式的双反引号一并处理
  text = text.replace(/`{1,3}[^`\n]*`{1,3}/g, ' ')

  // 图片整条剔除（alt 只是无障碍标签，不是正文）。
  // 必须先于链接规则：否则 ![alt](url) 会被链接规则匹配成 "!alt"，alt 被错误计入。
  text = text.replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')

  // 链接保留可读文字，丢掉 URL —— URL 里的字母数字不是正文内容
  text = text.replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')

  return (
    text
      // 标题、引用、强调等标记符号
      .replace(/[#>*_~`]/g, ' ')
      // 连字符只在词外（列表符号 / 分隔线）时剔除：词内的连字符是英文单词的一部分，
      // 一起替换成空格会把 "well-known" 拆成两个词。
      .replace(/(^|[^A-Za-z0-9])-+($|[^A-Za-z0-9])/g, '$1 $2')
  )
}

/**
 * 统计 Markdown 正文的字数：中日韩字符按字算 + 西文单词按词算。
 */
export function countWords(markdown: string): number {
  if (!markdown) return 0
  const text = stripMarkdownNoise(markdown)
  const cjk = text.match(CJK_PATTERN)?.length ?? 0
  const western = text.match(WESTERN_WORD_PATTERN)?.length ?? 0
  return cjk + western
}

/**
 * 预计阅读时长（分钟）。空内容也返回 1，避免展示"约 0 分钟阅读"。
 */
export function estimateReadMinutes(markdown: string, wordsPerMinute = 300): number {
  return Math.max(1, Math.ceil(countWords(markdown) / wordsPerMinute))
}
