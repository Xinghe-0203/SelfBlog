/**
 * LLM 能力扩展相关类型（A–F）
 *
 * 后端契约见 `.tmp-llm/CONTRACT.md`：全部端点前缀 `/api/admin/ai/**`，返回标准信封
 * `Result<T>`，因此这里只描述 `data` 的形状。所有生成端点**不写业务表**，前端拿结果
 * 去调既有保存接口（摘要/标签/标题 → 文章保存，caption → 相册保存）。
 *
 * 时间戳字段是 `LocalDateTime.toString()` 的字符串（如 `2026-10-22T13:30:00`），不是毫秒数。
 */

// ---------------------------------------------------------------------------
// A — 模型列表
// ---------------------------------------------------------------------------

/** 服务商：只有 `deepseek` 才会发送 thinking / reasoning_effort 字段 */
export type LlmProvider = 'deepseek' | 'custom'

/** 思考模式开关 */
export type LlmThinking = 'disabled' | 'enabled'

/** 思考强度（仅 `llmThinking=enabled` 时发送给上游） */
export type LlmReasoningEffort = 'low' | 'high' | 'max'

/** 上游 `/models` 返回的单个模型；`supportsImage` 缺失时视为 false */
export interface LlmModel {
  id: string
  name: string
  supportsImage: boolean
}

/**
 * 模型候选列表。后端**恒 200**：上游不可用时回落内置候选并把 `source` 置为 `fallback`，
 * 因此前端不需要为「探测失败」单独做错误分支。
 */
export interface LlmModelList {
  models: LlmModel[]
  source: 'upstream' | 'fallback'
}

// ---------------------------------------------------------------------------
// B — AI 写作辅助
// ---------------------------------------------------------------------------

/** 写作辅助请求体：三个端点形状一致，只带文章 id */
export interface WritingRequest {
  postId: number
}

/** 摘要生成结果：80–200 字中文纯文本，无 markdown */
export interface WritingSummaryVO {
  summary: string
  model: string
  latencyMs: number
}

/**
 * 推荐标签结果。后端**只会**返回已有标签池里的名字（池外值被丢弃），
 * 因此前端可以放心地把它们当作既有标签名做匹配。
 */
export interface WritingTagsVO {
  tags: string[]
  model: string
  latencyMs: number
}

/** 标题备选：恰好 3 个，每个 ≤60 字，互不相同 */
export interface WritingTitlesVO {
  titles: string[]
  model: string
  latencyMs: number
}

// ---------------------------------------------------------------------------
// C — 评论审核
// ---------------------------------------------------------------------------

/** 评论类型：访客留言（`guest_comment`）或历史站点评论（`blog_comment`） */
export type ModerationKind = 'guest' | 'site'

/** 审核结论：通过 / 可疑（标记）/ 拦截 */
export type ModerationVerdict = 'pass' | 'flag' | 'block'

/** 命中的问题类别 */
export type ModerationCategory = 'ad' | 'abuse' | 'spam' | 'offtopic' | 'meaningless'

/** 单条评论审核请求；`kind` 缺省按 `guest` 处理 */
export interface ModerationCheckRequest {
  commentId: number
  kind?: ModerationKind
}

/** 单条评论审核结果 */
export interface ModerationCheckVO {
  verdict: ModerationVerdict
  reason: string
  /** 可空数组；verdict=pass 时通常为空 */
  categories: ModerationCategory[]
  model: string
  latencyMs: number
}

/** 批量扫描请求；`limit` 默认 20，上限 50 */
export interface ModerationScanRequest {
  limit?: number
}

/** 批量扫描命中的单条（只含 verdict != pass 的评论） */
export interface ModerationScanItem {
  commentId: number
  kind: ModerationKind
  nickname: string
  excerpt: string
  verdict: ModerationVerdict
  reason: string
  categories: ModerationCategory[]
}

/** 批量扫描结果：`flagged` = `items.length`，只扫已公开（status=1）的访客评论 */
export interface ModerationScanVO {
  scanned: number
  flagged: number
  items: ModerationScanItem[]
}

// ---------------------------------------------------------------------------
// D — 内容校对
// ---------------------------------------------------------------------------

/** 问题类型：错别字 / 语法 / 措辞 / 事实 */
export type ProofreadIssueType = 'typo' | 'grammar' | 'wording' | 'fact'

/** 严重程度 */
export type ProofreadSeverity = 'low' | 'medium' | 'high'

/** 单条校对问题；`excerpt` ≤120 字、`suggestion` ≤200 字 */
export interface ProofreadIssue {
  type: ProofreadIssueType
  excerpt: string
  suggestion: string
  severity: ProofreadSeverity
}

/** 单篇校对结果；正文超 6000 字时截断并置 `truncated=true`，最多 20 条 */
export interface ProofreadVO {
  postId: number
  issues: ProofreadIssue[]
  truncated: boolean
  model: string
  latencyMs: number
}

/** 批量校对请求；`postIds` 可选（缺省按后端默认取样），`limit` 默认 5 上限 10 */
export interface ProofreadBatchRequest {
  postIds?: number[]
  limit?: number
}

/** 批量校对结果：串行执行，逐篇隔离失败 */
export interface ProofreadBatchVO {
  requested: number
  succeeded: number
  failed: number
  results: ProofreadVO[]
  failures: Array<{ postId: number; reason: string }>
}

// ---------------------------------------------------------------------------
// E — 图片描述
// ---------------------------------------------------------------------------

/** 图片来源：文章封面（`blog_post.cover_url`）或相册照片（`album_photo.file_path`） */
export type ImageAltSource = 'cover' | 'album'

/** 图片描述请求：封面用 `postId`，相册用 `photoId` */
export type ImageAltRequest =
  | { source: 'cover'; postId: number }
  | { source: 'album'; photoId: number }

/** 图片描述结果；`alt` ≤120 字、`caption` ≤80 字，后端不写库 */
export interface ImageAltVO {
  alt: string
  caption: string
  model: string
  latencyMs: number
}

// ---------------------------------------------------------------------------
// F — 站点内容体检
// ---------------------------------------------------------------------------

/** 体检请求；`limit` = 送进模型的文章条数，默认 40 上限 80 */
export interface HealthReportRequest {
  limit?: number
}

/** 单条体检发现；`count` 为 null 表示非计数类问题 */
export interface HealthFinding {
  title: string
  detail: string
  severity: ProofreadSeverity
  /** 建议的处置动作（给人看的一句话） */
  action: string
  count: number | null
}

/** 站点体检报告；`score` 0–100，`summary` 100–300 字，`findings` 3–8 条 */
export interface HealthReportVO {
  generatedAt: string
  score: number
  summary: string
  findings: HealthFinding[]
  sampledPosts: number
  totalPosts: number
  model: string
  latencyMs: number
}
