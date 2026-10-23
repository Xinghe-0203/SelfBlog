import { describe, expect, it } from 'vitest'
import {
  BANNER_CARD_HEIGHTS,
  BANNER_HEIGHTS,
  normalizePageBanner,
  pageBannerVars,
  safeBannerUrl
} from '@/composables/usePageConfig'
import type { BannerFit, BannerLayout } from '@/types/page'

describe('横幅背景的地址校验与归一化', () => {
  it('只接受 http(s):// 或 /uploads/ 开头的地址', () => {
    expect(safeBannerUrl('https://cdn.example.com/hero.png')).toBe(
      'https://cdn.example.com/hero.png'
    )
    expect(safeBannerUrl('http://cdn.example.com/hero.png')).toBe('http://cdn.example.com/hero.png')
    expect(safeBannerUrl('/uploads/site-assets/hero.png')).toBe('/uploads/site-assets/hero.png')
    expect(safeBannerUrl('  https://cdn.example.com/hero.png  ')).toBe(
      'https://cdn.example.com/hero.png'
    )
  })

  it('拒绝协议相对地址、非上传目录与可能撑开 CSS 的字符', () => {
    expect(safeBannerUrl('//evil.example.com/a.png')).toBe('')
    expect(safeBannerUrl('/static/a.png')).toBe('')
    expect(safeBannerUrl('javascript:alert(1)')).toBe('')
    expect(safeBannerUrl('https://cdn.example.com/a".png')).toBe('')
    expect(safeBannerUrl('https://cdn.example.com/a b.png')).toBe('')
    expect(safeBannerUrl('https://cdn.example.com/a(1).png')).toBe('')
    expect(safeBannerUrl('https://cdn.example.com/a\\b.png')).toBe('')
  })

  it('空值与缺失图片不产生横幅', () => {
    expect(safeBannerUrl(null)).toBe('')
    expect(safeBannerUrl(undefined)).toBe('')
    expect(normalizePageBanner(null)).toBeNull()
    expect(normalizePageBanner({})).toBeNull()
    expect(normalizePageBanner({ image: 'javascript:alert(1)' })).toBeNull()
  })

  it('补齐缺省枚举，并把遮罩限制在 0-80', () => {
    const banner = normalizePageBanner({ image: '/uploads/a.png' })
    expect(banner).toMatchObject({
      fit: 'cover',
      position: 'center',
      height: 'standard',
      overlay: 0,
      tone: 'auto',
      layout: 'split',
      light: false
    })
    expect(normalizePageBanner({ image: '/uploads/a.png', overlay: 120 })?.overlay).toBe(80)
    expect(normalizePageBanner({ image: '/uploads/a.png', overlay: -5 })?.overlay).toBe(0)
    // 契约外的枚举值（旧数据 / 手改草稿）要回落默认，不能透传到 CSS
    const stale = {
      image: '/uploads/a.png',
      fit: 'stretch' as unknown as BannerFit,
      layout: 'weird' as unknown as BannerLayout
    }
    expect(normalizePageBanner(stale)?.fit).toBe('cover')
    expect(normalizePageBanner(stale)?.layout).toBe('split')
  })

  it('auto 模式按遮罩深度推断文字明暗', () => {
    expect(normalizePageBanner({ image: '/uploads/a.png', overlay: 40 })?.light).toBe(true)
    expect(normalizePageBanner({ image: '/uploads/a.png', overlay: 39 })?.light).toBe(false)
    expect(normalizePageBanner({ image: '/uploads/a.png', tone: 'light' })?.light).toBe(true)
    expect(normalizePageBanner({ image: '/uploads/a.png', tone: 'dark', overlay: 60 })?.light).toBe(
      false
    )
  })

  it('拼装首页与关于页共用的 CSS 变量', () => {
    expect(pageBannerVars(null)).toEqual({})
    const vars = pageBannerVars(
      normalizePageBanner({
        image: 'https://cdn.example.com/a.png',
        fit: 'contain',
        position: 'top',
        height: 'tall',
        overlay: 30
      })
    ) as Record<string, string>
    expect(vars['--banner-image']).toBe('url("https://cdn.example.com/a.png")')
    expect(vars['--banner-size']).toBe('contain')
    expect(vars['--banner-position']).toBe('top')
    expect(vars['--banner-overlay']).toBe('30')
    expect(vars['--banner-height']).toBe('330px')
  })

  it('首页与关于页的高度档位各自独立', () => {
    expect(BANNER_HEIGHTS).toEqual({ compact: 190, standard: 250, tall: 330 })
    expect(BANNER_CARD_HEIGHTS).toEqual({ compact: 160, standard: 210, tall: 280 })
    const cardVars = pageBannerVars(
      normalizePageBanner({ image: '/uploads/a.png', height: 'compact' }),
      BANNER_CARD_HEIGHTS
    ) as Record<string, string>
    expect(cardVars['--banner-height']).toBe('160px')
  })
})
