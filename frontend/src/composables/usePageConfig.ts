import { computed, inject, type CSSProperties, type InjectionKey, type Ref } from 'vue'
import { usePagesStore } from '@/stores/pages'
import type {
  BannerFit,
  BannerHeight,
  BannerLayout,
  BannerPosition,
  BannerTone,
  PageBanner,
  PageConfig
} from '@/types/page'

export interface PagePreviewContext {
  id: string
  config: Ref<PageConfig>
  articleId?: Ref<string>
  tagId?: Ref<string>
}
export const pagePreviewKey: InjectionKey<PagePreviewContext> = Symbol('page-preview')

export function resolvePageText(config: PageConfig, key: string, fallback: string): string {
  const value =
    key === 'title' || key === 'description' || key === 'body' ? config[key] : config.text?.[key]
  return value ?? fallback
}
export function pageModule(config: PageConfig, key: string, fallback = true): boolean {
  return config.modules?.[key] ?? fallback
}
export function pageStyleVars(config: PageConfig): CSSProperties {
  const style: CSSProperties = {}
  const accent = config.style?.accent
  if (accent && /^#[a-f\d]{6}$/i.test(accent)) {
    style['--accent'] = accent
    style['--primary'] = 'var(--accent)'
    style['--accent-dark'] = `color-mix(in srgb, ${accent} 82%, var(--accent-shade, black))`
    style['--accent-light'] = `color-mix(in srgb, ${accent} 12%, transparent)`
  }
  return style
}
/**
 * 横幅高度档位 → 首页介绍区 min-height（px）。
 *
 * 档位值写死在这里而不是散落到各个视图：后台编辑器、首页与关于页必须对同一档位
 * 有同一个理解，否则作者在后台看到的高度和前台渲染出来的不一样。
 */
export const BANNER_HEIGHTS: Record<BannerHeight, number> = {
  compact: 190,
  standard: 250,
  tall: 330
}
/** 横幅高度档位 → 关于页作者卡 min-height（px）。 */
export const BANNER_CARD_HEIGHTS: Record<BannerHeight, number> = {
  compact: 160,
  standard: 210,
  tall: 280
}

/**
 * 横幅背景图的地址白名单。
 *
 * 与 `stores/site.ts` 的 `safeBackgroundUrl` 同级：只接受 http(s):// 或 /uploads/ 开头，
 * 且不含空白、引号、圆括号与反斜杠 —— 该值会被拼进 `url("…")`，任何能闭合引号或
 * 括号的字符都可能撑开一条 CSS 注入路径。前端不能假设数据一定来自本服务端的校验。
 */
export function safeBannerUrl(raw: string | null | undefined): string {
  const value = String(raw ?? '').trim()
  if (!value) return ''
  if (!/^https?:\/\//i.test(value) && !value.startsWith('/uploads/')) return ''
  return /[\s"'()\\]/.test(value) ? '' : value
}

/** 归一化后的横幅：枚举值全部落到合法区间，图片地址已通过白名单。 */
export interface PageBannerView {
  image: string
  fit: BannerFit
  position: BannerPosition
  height: BannerHeight
  overlay: number
  tone: BannerTone
  layout: BannerLayout
  /** 文字是否用浅色：tone=light，或 auto 且遮罩已经压得够深（≥40）。 */
  light: boolean
}

/**
 * 把作者填的 banner 配置收敛成一个可直接渲染的对象。
 *
 * 背景图为空或地址非法时返回 `null` —— 视图据此保持原有外观（逐像素不变），
 * 绝不把来路不明的地址拼进 CSS。
 */
export function normalizePageBanner(banner?: PageBanner | null): PageBannerView | null {
  const image = safeBannerUrl(banner?.image)
  if (!image) return null
  const rawOverlay = Number(banner?.overlay ?? 0)
  const overlay = Number.isFinite(rawOverlay)
    ? Math.min(80, Math.max(0, Math.round(rawOverlay)))
    : 0
  const tone: BannerTone =
    banner?.tone === 'light' || banner?.tone === 'dark' ? banner.tone : 'auto'
  return {
    image,
    fit: banner?.fit === 'contain' ? 'contain' : 'cover',
    position:
      banner?.position === 'top' ? 'top' : banner?.position === 'bottom' ? 'bottom' : 'center',
    height:
      banner?.height === 'compact' ? 'compact' : banner?.height === 'tall' ? 'tall' : 'standard',
    overlay,
    tone,
    layout:
      banner?.layout === 'centered'
        ? 'centered'
        : banner?.layout === 'stacked'
          ? 'stacked'
          : 'split',
    light: tone === 'light' || (tone === 'auto' && overlay >= 40)
  }
}

/**
 * 横幅的 CSS 变量。
 *
 * 变量名固定为 `--banner-image/--banner-size/--banner-position/--banner-overlay/--banner-height`，
 * 首页与关于页共用同一套变量名，样式表各写各的版式。
 */
export function pageBannerVars(
  banner: PageBannerView | null | undefined,
  heights: Record<BannerHeight, number> = BANNER_HEIGHTS
): CSSProperties {
  if (!banner) return {}
  return {
    '--banner-image': `url("${banner.image}")`,
    '--banner-size': banner.fit,
    '--banner-position': banner.position,
    '--banner-overlay': String(banner.overlay),
    '--banner-height': `${heights[banner.height]}px`
  }
}

export function usePageConfig(
  id: string,
  options: { heights?: Record<BannerHeight, number> } = {}
) {
  const pages = usePagesStore()
  const preview = inject(pagePreviewKey, undefined)
  const isPreview = !!preview
  const config = computed<PageConfig>(() =>
    preview?.id === id ? preview.config.value : pages.pages[id]?.config || {}
  )
  /** 归一化后的横幅；没有横幅（或地址非法）时为 null。 */
  const banner = computed(() => normalizePageBanner(config.value.style?.banner))
  /** 横幅的 CSS 变量；没有横幅时是空对象（不产生 style 属性）。 */
  const bannerVars = computed(() => pageBannerVars(banner.value, options.heights))
  return {
    config,
    isPreview,
    preview,
    banner,
    bannerVars,
    text: (key: string, fallback: string) => resolvePageText(config.value, key, fallback),
    module: (key: string, fallback = true) => pageModule(config.value, key, fallback)
  }
}
