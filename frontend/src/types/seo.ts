/**
 * SEO 自动管理相关类型
 *
 * 后端把「文章本身的字段」与「SEO 元数据」分开存：`blog_post` 只有标题/摘要，
 * per-post 的 SEO 字段存在 `post_seo` 表里。因此后台列表用专门的 Row VO，
 * 不携带正文（列表页只需要标题）。
 */

/** SEO 体检问题码，与后端 `SeoIssue` 枚举一一对应 */
export type SeoIssue =
  | 'missingDescription'
  | 'longTitle'
  | 'duplicateTitle'
  | 'missingCover'
  | 'noSeo'

/** SEO 元数据的来源：手写或模型生成 */
export type SeoSource = 'manual' | 'llm'

/** 后台 SEO 概览 */
export interface SeoOverview {
  /** 服务端派生：已存密钥且已配 endpoint，才算是「可以生成」 */
  llmConfigured: boolean
  llmEnabled: boolean
  autoOnPublish: boolean
  model: string
  totalPosts: number
  withSeo: number
  coveragePercent: number
  missingDescription: number
  longTitle: number
  duplicateTitle: number
  missingCover: number
}

/** 后台 SEO 列表行 */
export interface SeoPostRow {
  postId: number
  /** 文章自身标题（用于展示） */
  title: string
  status: number
  hasSeo: boolean
  seoTitle: string | null
  seoDescription: string | null
  seoKeywords: string | null
  ogImageUrl: string | null
  source: SeoSource | null
  generatedAt: string | null
  updateTime: string | null
  /** 体检出的问题码，无问题时为空数组 */
  issues: SeoIssue[]
}

/** 单篇 SEO 详情（列表行 + 编辑所需上下文） */
export interface SeoDetail extends SeoPostRow {
  /** 文章摘要，作为生成时的原始素材展示 */
  postSummary: string | null
  /** 当前前台实际会渲染的标题 */
  effectiveTitle: string
  /** 当前前台实际会渲染的描述 */
  effectiveDescription: string
}

/** 保存 SEO 元数据的请求体 */
export interface SeoUpdateRequest {
  seoTitle: string | null
  seoDescription: string | null
  seoKeywords: string | null
  ogImageUrl: string | null
}

/** 批量生成结果 */
export interface SeoBatchResult {
  requested: number
  succeeded: number
  failed: number
  skipped: number
  failures: Array<{ postId: number; message: string }>
}

/** 连通性测试结果 */
export interface SeoLlmTest {
  ok: boolean
  message: string
  latencyMs: number
  model: string
}

/** 列表筛选：issue 为空表示不过滤 */
export interface SeoPostListParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  issue?: SeoIssue | ''
}

/** 批量生成请求体 */
export interface SeoBatchRequest {
  postIds?: number[]
  limit?: number
  onlyMissing?: boolean
}
