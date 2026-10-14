/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}

interface ImportMetaEnv {
  /** API 基础路径 */
  readonly VITE_API_BASE_URL: string
  /** API 目标地址（开发环境） */
  readonly VITE_API_TARGET?: string
  /** 应用标题 */
  readonly VITE_APP_TITLE: string
  /** 应用版本 */
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
