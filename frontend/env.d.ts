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

/**
 * 构建期注入的应用版本号，唯一真源是 `frontend/package.json` 的 `version`
 * （见 `vite.config.ts` 的 `define`）。不要手写版本字面量：`main.ts` 里曾经
 * 硬编码 `'2.0.25'` 并落后了两个发布版本而无人察觉。
 */
declare const __APP_VERSION__: string
