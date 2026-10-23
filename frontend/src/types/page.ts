export type BuiltinPageId =
  | 'home'
  | 'about'
  | 'archives'
  | 'gallery'
  | 'search'
  | 'post'
  | 'tag'
  | 'notFound'
/** 页面横幅（首页头图 / 关于页作者卡）的背景图适配方式。 */
export type BannerFit = 'cover' | 'contain'
/** 背景图焦点位置；`cover` 下决定裁切重心。 */
export type BannerPosition = 'center' | 'top' | 'bottom'
/** 横幅高度档位（由样式表换算成 min-height）。 */
export type BannerHeight = 'compact' | 'standard' | 'tall'
/** 横幅上的文字明暗；`auto` 由遮罩强度推断。 */
export type BannerTone = 'auto' | 'light' | 'dark'
/** 横幅内容排布：左右分栏 / 居中 / 上下堆叠。 */
export type BannerLayout = 'split' | 'centered' | 'stacked'

/**
 * 页面级横幅配置：让作者在后台为「首页介绍区」和「关于页作者卡」
 * 单独上传背景图并调整尺寸、裁切与文字排布。
 *
 * 全部字段可选：缺省 = 沿用各页面原有的默认外观（升级零回归）。
 * 背景图地址只接受 http(s) 与站内 `/uploads/` 路径，服务端与前端各自校验一次。
 */
export interface PageBanner {
  image?: string | null
  fit?: BannerFit | null
  position?: BannerPosition | null
  height?: BannerHeight | null
  /** 背景图上的深色遮罩百分比 0–80，用来压住底图保证文字可读。 */
  overlay?: number | null
  tone?: BannerTone | null
  layout?: BannerLayout | null
}

export interface PageStyle {
  accent?: string | null
  background?: 'inherit' | 'plain' | 'soft' | 'paper' | null
  width?: 'inherit' | 'narrow' | 'standard' | 'wide' | null
  density?: 'inherit' | 'compact' | 'comfortable' | null
  banner?: PageBanner | null
}
export interface PageSeo {
  title?: string | null
  description?: string | null
  image?: string | null
  indexable?: boolean | null
}
export interface PageConfig {
  title?: string | null
  description?: string | null
  body?: string | null
  text?: Record<string, string>
  modules?: Record<string, boolean>
  style?: PageStyle | null
  seo?: PageSeo | null
}
export interface PageState {
  schemaVersion: number
  revision: number
  slug?: string | null
  everPublished: boolean
  updatedAt?: string | null
  config: PageConfig
}
export interface PublicPage {
  id: string
  slug?: string | null
  updatedAt?: string | null
  config: PageConfig
}
export interface PageEditorDocument {
  id: string
  builtin: boolean
  label: string
  draft: PageState
  published?: PageState | null
}
export interface NavigationItem {
  pageId: string
  label: string
}
export interface PublicNavigationItem extends NavigationItem {
  to: string
}
export interface NavigationState {
  schemaVersion: number
  revision: number
  items: NavigationItem[]
}
export interface PageManifest {
  pages: PublicPage[]
  navigation: PublicNavigationItem[]
}
