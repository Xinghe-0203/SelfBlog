import { beforeEach, describe, expect, it } from 'vitest'
import { applySeoMeta, applyArticleJsonLd, setSeoSiteTitle } from '../useSeoMeta'
import { pageSeo } from '../usePageSeo'

beforeEach(() => {
  document.head
    .querySelectorAll(
      'meta[property^="article:"], meta[name="description"], script[type="application/ld+json"]'
    )
    .forEach((node) => node.remove())
  setSeoSiteTitle('博客')
})
describe('page metadata lifecycle', () => {
  it('retains dynamic identity and never indexes missing or system pages', () => {
    expect(
      pageSeo(
        'post',
        { seo: { title: '写作', indexable: true } },
        { title: '真实文章', indexable: true }
      ).title
    ).toBe('真实文章 - 写作')
    expect(
      pageSeo('post', { seo: { indexable: true } }, { title: '不存在', indexable: false }).indexable
    ).toBe(false)
    expect(pageSeo('search', { seo: { indexable: true } }, { title: '搜索' }).indexable).toBe(false)
    expect(pageSeo('admin', {}, { title: '后台' }).indexable).toBe(false)
  })
  it('clears article tags and structured data when a website page replaces an article', () => {
    applySeoMeta({
      title: '文章',
      description: '文章介绍',
      type: 'article',
      author: '作者',
      tags: ['技术']
    })
    applyArticleJsonLd({ title: '文章' })
    applySeoMeta({ title: '关于', description: '', type: 'website', indexable: true })
    expect(document.head.querySelector('meta[property^="article:"]')).toBeNull()
    expect(document.getElementById('selfblog-article-jsonld')).toBeNull()
    expect(document.head.querySelector('meta[name="description"]')).toBeNull()
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe(
      'index,follow'
    )
  })
  it('respects explicit noindex and drops old optional author/date fields', () => {
    applySeoMeta({
      title: '旧文章',
      type: 'article',
      author: '旧作者',
      publishedTime: '2026-01-01'
    })
    applySeoMeta({ title: '新文章', type: 'article', indexable: false })
    expect(document.head.querySelector('meta[property="article:author"]')).toBeNull()
    expect(document.head.querySelector('meta[property="article:published_time"]')).toBeNull()
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe(
      'noindex,follow'
    )
  })
})
