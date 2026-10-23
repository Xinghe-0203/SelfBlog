import type { BannerLayout, BuiltinPageId } from '@/types/page'

export interface PageField {
  key: string
  label: string
  limit: number
  multiline?: boolean
}
/**
 * 页面级横幅能力。
 *
 * 只有声明了这一项的页面才允许在后台设置横幅背景（首页介绍区、关于页作者卡）；
 * `layouts` 是该页面允许的内容排布，编辑器据此渲染下拉选项。
 */
export interface PageBannerCapability {
  layouts: BannerLayout[]
}
export interface PageCapability {
  label: string
  fields: PageField[]
  modules: Array<{ key: string; label: string }>
  body?: boolean
  dynamic?: boolean
  banner?: PageBannerCapability
}
const title: PageField = { key: 'title', label: '页面标题', limit: 120 }
const description: PageField = {
  key: 'description',
  label: '页面介绍',
  limit: 1000,
  multiline: true
}
const modules = (values: Record<string, string>) =>
  Object.entries(values).map(([key, label]) => ({ key, label }))
/** 首页与关于页都支持横幅，且允许全部三种内容排布。 */
const bannerLayouts: BannerLayout[] = ['split', 'centered', 'stacked']
export const pageCatalog: Record<BuiltinPageId, PageCapability> = {
  home: {
    label: '首页',
    fields: [
      title,
      description,
      { key: 'eyebrow', label: '标题上方短标签', limit: 300 },
      { key: 'listTitle', label: '文章列表标题', limit: 300 }
    ],
    modules: modules({
      hero: '介绍区',
      cover: '介绍封面',
      author: '作者侧栏',
      tags: '标签区',
      archiveLink: '归档入口'
    }),
    banner: { layouts: bannerLayouts }
  },
  about: {
    label: '关于',
    fields: [title, description],
    body: true,
    modules: modules({ author: '作者资料', site: '网站说明', links: '继续阅读入口' }),
    banner: { layouts: bannerLayouts }
  },
  archives: {
    label: '归档',
    fields: [title, description],
    modules: modules({ category: '文章分类', count: '文章数量' })
  },
  gallery: {
    label: '相册',
    fields: [title, description],
    modules: modules({ description: '相册说明', count: '照片数量' })
  },
  search: {
    label: '搜索',
    fields: [
      title,
      description,
      { key: 'placeholder', label: '搜索输入提示', limit: 300 },
      { key: 'empty', label: '没有结果时的说明', limit: 300, multiline: true }
    ],
    modules: modules({ count: '结果数量' })
  },
  post: {
    label: '文章详情模板',
    fields: [description],
    dynamic: true,
    modules: modules({
      cover: '文章封面',
      meta: '时间与阅读信息',
      toc: '目录',
      adjacent: '上一篇与下一篇'
    })
  },
  tag: {
    label: '标签详情模板',
    fields: [description],
    dynamic: true,
    modules: modules({ count: '文章数量' })
  },
  notFound: {
    label: '404 页面',
    fields: [title, description],
    modules: modules({ homeLink: '返回首页入口', search: '搜索入口' })
  }
}
export const customPageCapability: PageCapability = {
  label: '内容页',
  fields: [title, description],
  body: true,
  modules: modules({ header: '页面标题区', updated: '更新时间', links: '继续阅读入口' })
}
export function pageCapability(id: string): PageCapability {
  return pageCatalog[id as BuiltinPageId] || customPageCapability
}
export function pageLabel(id: string): string {
  return pageCapability(id).label
}
