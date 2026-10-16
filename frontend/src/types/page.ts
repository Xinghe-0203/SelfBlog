export type BuiltinPageId =
  | 'home'
  | 'about'
  | 'archives'
  | 'gallery'
  | 'search'
  | 'post'
  | 'tag'
  | 'notFound'
export interface PageStyle {
  accent?: string | null
  background?: 'inherit' | 'plain' | 'soft' | 'paper' | null
  width?: 'inherit' | 'narrow' | 'standard' | 'wide' | null
  density?: 'inherit' | 'compact' | 'comfortable' | null
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
