import api from './index'
import type { ApiResponse } from '@/types'
import type {
  HealthReportRequest,
  HealthReportVO,
  ImageAltRequest,
  ImageAltVO,
  ModerationCheckRequest,
  ModerationCheckVO,
  ModerationScanRequest,
  ModerationScanVO,
  ProofreadBatchRequest,
  ProofreadBatchVO,
  ProofreadVO,
  WritingRequest,
  WritingSummaryVO,
  WritingTagsVO,
  WritingTitlesVO
} from '@/types/llm'

/**
 * 这些请求**必须**禁用自动重试（`_noRetry`）。
 *
 * <p>axios 的超时错误码是 `ECONNABORTED`，而它在 `RETRYABLE_CODES` 里（对普通请求
 * 是对的：网络抖动值得重试）。但模型生成每次都会真实消耗额度，超时又恰恰是它最常见
 * 的失败形态 —— 自动重试会把一次超时放大成三次上游调用、三倍等待与三倍账单，
 * 管理员看到的是「转圈很久然后还是失败」。失败就让它失败，由人决定是否重来。</p>
 */
const NO_RETRY = { _noRetry: true } as const

/**
 * 模型生成类请求的超时。
 *
 * <p>上游模型的耗时不可预测（思考模式下更慢），后端自己还有 `llmTimeoutSeconds` 兜底，
 * 这里给足 130 秒，避免前端先于后端放弃而让管理员误以为请求失败。</p>
 */
const GENERATE_TIMEOUT = 130000

/**
 * 后台 AI 能力接口（B–F）。
 *
 * 全部走标准信封 `Result<T>`：调用方拿到的是 `{ code, message, data }`（拦截器已解包
 * AxiosResponse），因此这里返回 `Promise<ApiResponse<T>>`，调用点读 `response.data`。
 *
 * 生成端点**不写业务表**：摘要/标签/标题由前端回填文章表单后走既有保存接口，
 * caption 同理回填相册表单。
 */
export const llmApi = {
  // --- B 写作辅助 ---------------------------------------------------------

  /** 生成文章摘要（80–200 字纯文本），前端回填到摘要输入框 */
  generateSummary(data: WritingRequest): Promise<ApiResponse<WritingSummaryVO>> {
    return api.post('/admin/ai/writing/summary', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  /** 从既有标签池里推荐 3–6 个标签；池外值后端已丢弃 */
  generateTags(data: WritingRequest): Promise<ApiResponse<WritingTagsVO>> {
    return api.post('/admin/ai/writing/tags', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  /** 生成 3 个标题备选（每个 ≤60 字，互不相同） */
  generateTitles(data: WritingRequest): Promise<ApiResponse<WritingTitlesVO>> {
    return api.post('/admin/ai/writing/titles', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  // --- C 评论审核 ---------------------------------------------------------

  /** 单条评论审核；结论只返回给前端，不自动改评论状态 */
  moderationCheck(data: ModerationCheckRequest): Promise<ApiResponse<ModerationCheckVO>> {
    return api.post('/admin/ai/moderation/check', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  /** 批量扫描已公开的访客评论，只返回 flagged 项 */
  moderationScan(data: ModerationScanRequest): Promise<ApiResponse<ModerationScanVO>> {
    return api.post('/admin/ai/moderation/scan', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  // --- D 内容校对 ---------------------------------------------------------

  /** 单篇校对；正文超 6000 字会被后端截断并置 truncated */
  proofread(postId: number): Promise<ApiResponse<ProofreadVO>> {
    return api.post(`/admin/ai/proofread/${postId}`, null, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  /** 批量校对（串行，逐篇隔离失败） */
  proofreadBatch(data: ProofreadBatchRequest): Promise<ApiResponse<ProofreadBatchVO>> {
    return api.post('/admin/ai/proofread/batch', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  // --- E 图片描述 ---------------------------------------------------------

  /** 为封面或相册照片生成 alt/caption；后端不写库，前端回填后自行保存 */
  imageAlt(data: ImageAltRequest): Promise<ApiResponse<ImageAltVO>> {
    return api.post('/admin/ai/images/alt', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  },

  // --- F 站点内容体检 -----------------------------------------------------

  /** 生成站点内容体检报告（单次调用，不逐篇） */
  healthReport(data: HealthReportRequest): Promise<ApiResponse<HealthReportVO>> {
    return api.post('/admin/ai/health/report', data, {
      timeout: GENERATE_TIMEOUT,
      ...NO_RETRY
    })
  }
}
