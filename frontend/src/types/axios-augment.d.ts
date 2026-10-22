/**
 * axios 请求配置上的私有扩展字段。
 *
 * <p>这些字段由 `api/index.ts` 的响应拦截器读写，调用方也会显式传 `_noRetry`（见
 * `api/seo.ts`）。它们不属于 axios 公开 API，所以必须用 module augmentation 声明，
 * 否则 `api.post(url, data, { _noRetry: true })` 过不了 `vue-tsc`。</p>
 *
 * <p><b>必须是模块</b>（顶部有 import/export），否则这里会整体替换 axios 的类型声明
 * 而不是与之合并。`InternalAxiosRequestConfig` 继承 `AxiosRequestConfig`，因此声明在
 * 后者上同时覆盖两者。</p>
 */
import 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 401 刷新令牌流程的「已重放过一次」标记，防止无限重放 */
    _retry?: boolean
    /** 网络/超时重试的已尝试次数 */
    _networkRetryCount?: number
    /**
     * 显式禁止自动重试。用于「重试有副作用」的请求 —— 目前是 LLM 生成：
     * 每次调用都真实消耗模型额度，且超时本身就是它的常见失败形态
     * （`ECONNABORTED` 在 RETRYABLE_CODES 里），自动重试会把一次超时放大成
     * 三次上游调用、三倍等待与三倍账单。
     */
    _noRetry?: boolean
  }
}
