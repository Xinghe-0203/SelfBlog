/**
 * 运行时维护页面级 SEO / 社交分享 meta。
 *
 * 单页应用所有路由共用同一份 `index.html`，搜索引擎与社交平台的抓取器
 * 读到的是那份静态标签。所以每进入一篇文章，都要把 title / description /
 * og:image 换成文章自己的，否则分享出去永远是站点首页的标题。
 */

const OG_IMAGE_FALLBACK = '/default-cover.jpg'

export interface SeoMeta {
  title: string
  description?: string
  image?: string
  url?: string
  type?: 'website' | 'article'
  publishedTime?: string
  author?: string
  tags?: string[]
}

/** 站点名，用于拼接 `<title>`；由调用方在首次使用时传入一次即可。 */
let siteTitle = ''

export function setSeoSiteTitle(value: string): void {
  siteTitle = value || ''
}

function upsertMeta(attr: 'name' | 'property', key: string, content: string): void {
  if (!content) return
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${key}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function upsertLink(rel: string, href: string): void {
  if (!href) return
  let el = document.head.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`)
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', rel)
    document.head.appendChild(el)
  }
  el.setAttribute('href', href)
}

/** 相对路径补成绝对地址——社交平台不会去猜图片的相对路径。 */
function absolute(url: string): string {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return window.location.origin + (url.startsWith('/') ? url : '/' + url)
}

export function applySeoMeta(meta: SeoMeta): void {
  const fullTitle =
    siteTitle && meta.title !== siteTitle ? `${meta.title} - ${siteTitle}` : meta.title
  const description = meta.description || ''
  const image = absolute(meta.image || OG_IMAGE_FALLBACK)
  const url = absolute(meta.url || window.location.pathname + window.location.search)

  document.title = fullTitle
  upsertMeta('name', 'description', description)
  upsertLink('canonical', url)

  upsertMeta('property', 'og:type', meta.type || 'website')
  upsertMeta('property', 'og:title', fullTitle)
  upsertMeta('property', 'og:description', description)
  upsertMeta('property', 'og:image', image)
  upsertMeta('property', 'og:url', url)
  if (siteTitle) upsertMeta('property', 'og:site_name', siteTitle)

  upsertMeta('name', 'twitter:card', 'summary_large_image')
  upsertMeta('name', 'twitter:title', fullTitle)
  upsertMeta('name', 'twitter:description', description)
  upsertMeta('name', 'twitter:image', image)

  if (meta.type === 'article') {
    upsertMeta('property', 'article:published_time', meta.publishedTime || '')
    upsertMeta('property', 'article:author', meta.author || '')
    // article:tag 可能有多条：先清掉旧的再写，避免上一篇的标签残留
    document.head.querySelectorAll('meta[property="article:tag"]').forEach((n) => n.remove())
    for (const tag of meta.tags || []) {
      if (!tag) continue
      const el = document.createElement('meta')
      el.setAttribute('property', 'article:tag')
      el.setAttribute('content', tag)
      document.head.appendChild(el)
    }
  }
}

/** 离开文章页时把文章级标签降级回站点级，避免残留到其他页面。 */
export function resetSeoMeta(siteDescription = ''): void {
  document.head.querySelectorAll('meta[property^="article:"]').forEach((n) => n.remove())
  applySeoMeta({
    title: siteTitle || document.title,
    description: siteDescription,
    type: 'website'
  })
}

export function useSeoMeta() {
  return { applySeoMeta, resetSeoMeta, setSeoSiteTitle }
}

const JSONLD_ID = 'selfblog-article-jsonld'

/**
 * 注入 BlogPosting 结构化数据（JSON-LD）。
 * 搜索引擎据此有机会在结果里显示作者、发布时间、封面等富摘要。
 * 用 textContent 写入而非 innerHTML——文章标题里出现 `</script>` 也不会破坏页面。
 */
export function applyArticleJsonLd(data: {
  title: string
  description?: string
  image?: string
  url?: string
  publishedTime?: string
  author?: string
}): void {
  let el = document.getElementById(JSONLD_ID) as HTMLScriptElement | null
  if (!el) {
    el = document.createElement('script')
    el.id = JSONLD_ID
    el.type = 'application/ld+json'
    document.head.appendChild(el)
  }
  const payload: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: data.title,
    mainEntityOfPage: absolute(data.url || window.location.pathname),
    image: [absolute(data.image || OG_IMAGE_FALLBACK)]
  }
  if (data.description) payload.description = data.description
  if (data.publishedTime) payload.datePublished = data.publishedTime
  if (data.author) payload.author = { '@type': 'Person', name: data.author }
  if (siteTitle) payload.publisher = { '@type': 'Organization', name: siteTitle }
  el.textContent = JSON.stringify(payload)
}

export function clearArticleJsonLd(): void {
  document.getElementById(JSONLD_ID)?.remove()
}
