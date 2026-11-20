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
  modifiedTime?: string
  author?: string
  tags?: string[]
  section?: string
  indexable?: boolean
}

/** 站点名，用于拼接 `<title>`；由调用方在首次使用时传入一次即可。 */
let siteTitle = ''

export function setSeoSiteTitle(value: string): void {
  siteTitle = value || ''
}

function upsertMeta(attr: 'name' | 'property', key: string, content: string): void {
  if (!content) {
    document.head.querySelectorAll(`meta[${attr}="${key}"]`).forEach((node) => node.remove())
    return
  }
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
  const origin = canonicalOrigin()
  return origin + (url.startsWith('/') ? url : '/' + url)
}

/**
 * 服务端渲染的阅读 HTML 会带上 selfblog-site-origin（配置的站点对外地址）。
 * 反向代理/预览域名与正式域名不同时，canonical 与分享图必须沿用它，
 * 否则挂载瞬间就会把服务端写好的地址改回请求主机。Vite 本地开发没有该标记时回落浏览器 origin。
 *
 * 导出给需要「站点对外绝对地址」的其它调用方（如文章页的版权声明 permalink）复用，
 * 避免各处各拼一份 origin。
 */
export function canonicalOrigin(): string {
  const marker = document.head
    .querySelector<HTMLMetaElement>('meta[name="selfblog-site-origin"]')
    ?.content.trim()
  if (marker && /^https?:\/\/[^/?#\s\\]+$/i.test(marker)) return marker.replace(/\/+$/, '')
  return window.location.origin
}

export function applySeoMeta(meta: SeoMeta): void {
  const fullTitle =
    siteTitle && meta.title !== siteTitle ? `${meta.title} - ${siteTitle}` : meta.title
  const description = meta.description || ''
  const image = absolute(meta.image || OG_IMAGE_FALLBACK)
  const url = absolute(meta.url || window.location.pathname + window.location.search)

  document.title = fullTitle
  upsertMeta('name', 'description', description)
  upsertMeta('name', 'robots', meta.indexable === false ? 'noindex,follow' : 'index,follow')
  upsertLink('canonical', url)

  upsertMeta('property', 'og:type', meta.type || 'website')
  upsertMeta('property', 'og:title', fullTitle)
  upsertMeta('property', 'og:description', description)
  upsertMeta('property', 'og:image', image)
  upsertMeta('property', 'og:url', url)
  upsertMeta('property', 'og:locale', 'zh_CN')
  // 图片的替代文字：社交平台用它决定卡片版式，缺省时会随机裁切。
  upsertMeta('property', 'og:image:alt', meta.title)
  if (siteTitle) upsertMeta('property', 'og:site_name', siteTitle)

  upsertMeta('name', 'twitter:card', 'summary_large_image')
  upsertMeta('name', 'twitter:title', fullTitle)
  upsertMeta('name', 'twitter:description', description)
  upsertMeta('name', 'twitter:image', image)
  upsertMeta('name', 'twitter:image:alt', meta.title)

  if (meta.type === 'article') {
    upsertMeta('property', 'article:published_time', meta.publishedTime || '')
    upsertMeta('property', 'article:modified_time', meta.modifiedTime || meta.publishedTime || '')
    upsertMeta('property', 'article:author', meta.author || '')
    upsertMeta('property', 'article:section', meta.section || '')
    // article:tag 可能有多条：先清掉旧的再写，避免上一篇的标签残留
    document.head.querySelectorAll('meta[property="article:tag"]').forEach((n) => n.remove())
    for (const tag of meta.tags || []) {
      if (!tag) continue
      const el = document.createElement('meta')
      el.setAttribute('property', 'article:tag')
      el.setAttribute('content', tag)
      document.head.appendChild(el)
    }
  } else {
    document.head.querySelectorAll('meta[property^="article:"]').forEach((node) => node.remove())
    clearArticleJsonLd()
  }
}

export function useSeoMeta() {
  return { applySeoMeta, setSeoSiteTitle }
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
  modifiedTime?: string
  author?: string
  tags?: string[]
  section?: string
}): void {
  let el = document.getElementById(JSONLD_ID) as HTMLScriptElement | null
  if (!el) {
    el = document.createElement('script')
    el.id = JSONLD_ID
    el.type = 'application/ld+json'
    document.head.appendChild(el)
  }
  const canonical = absolute(data.url || window.location.pathname)
  const payload: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: data.title,
    mainEntityOfPage: canonical,
    image: [absolute(data.image || OG_IMAGE_FALLBACK)]
  }
  if (data.description) payload.description = data.description
  if (data.publishedTime) payload.datePublished = data.publishedTime
  // 更新晚于发布时才值得单独声明（否则两个日期一样，阅读器会误以为「刚改过」）。
  if (data.modifiedTime && data.modifiedTime !== data.publishedTime) {
    payload.dateModified = data.modifiedTime
  }
  if (data.author) payload.author = { '@type': 'Person', name: data.author }
  if (siteTitle) payload.publisher = { '@type': 'Organization', name: siteTitle }
  if (data.tags?.length) payload.keywords = data.tags.join(', ')
  if (data.section) payload.articleSection = data.section
  if (document.documentElement.lang) payload.inLanguage = document.documentElement.lang

  /*
    面包屑必须由**客户端**也写一份：服务端阅读 HTML 里已经有 breadcrumb，
    但 Vue 挂载后会用本函数重建这枚 JSON-LD（同 id、整个节点替换），
    少写就等于把服务端刚写好的结构化数据降级掉了。形状与服务端
    ReadingHtmlRenderer.breadcrumbList 保持一致（首页 → 文章，URL 均为规范 origin 的绝对地址）。
  */
  if (siteTitle || canonical) {
    payload.breadcrumb = {
      '@type': 'BreadcrumbList',
      itemListElement: [
        {
          '@type': 'ListItem',
          position: 1,
          name: siteTitle || undefined,
          item: `${canonicalOrigin()}/`
        },
        { '@type': 'ListItem', position: 2, name: data.title, item: canonical }
      ]
    }
  }
  el.textContent = JSON.stringify(payload)
}

export function clearArticleJsonLd(): void {
  document.getElementById(JSONLD_ID)?.remove()
}
