# 第六章 前端工程化

> 本章以 SelfBlog 项目（Vue 3.5 + Vite 6 + TypeScript 6.0 + ESLint + Prettier + Vitest）为实战案例，系统讲解现代前端工程化的完整链路。读者应具备基本的 HTML/CSS/JavaScript 知识和 Vue 3 基础。

---

## 6.1 Node.js 与 npm 基础

### 6.1.1 Node.js 版本管理

现代前端项目对 Node.js 版本有严格要求。SelfBlog 项目通过 `package.json` 的 `engines` 字段声明了版本约束：

```json
{
  "engines": {
    "node": "^22.22.2 || ^24.15.0 || >=26.0.0"
  }
}
```

这个约束的含义是：Node.js 版本必须满足 `22.22.2` 及以上（但小于 `23.0.0`）、或 `24.15.0` 及以上（但小于 `25.0.0`）、或 `26.0.0` 及以上。

**为什么需要版本管理？**

不同 Node.js 版本对 ES 模块、原生 API、构建工具链的支持不同。Vite 6 要求 Node.js 18+ 或 20+，而 TypeScript 6.0 的某些特性需要更新的运行时支持。版本管理确保团队所有成员和 CI 环境使用一致的运行时。

**nvm（Node Version Manager）实战：**

```bash
# 安装 nvm（Windows 使用 nvm-windows）
# 安装指定版本
nvm install 22.22.2
nvm install 24.15.0

# 切换版本
nvm use 22.22.2

# 查看已安装版本
nvm list

# 在项目根目录创建 .nvmrc 文件
echo "22.22.2" > .nvmrc
nvm use  # 自动读取 .nvmrc
```

**fnm（Fast Node Manager）替代方案：**

```bash
# 安装 fnm
winget install Schniz.fnm

# 使用 fnm
fnm install 22
fnm use 22
fnm default 22
```

### 6.1.2 包管理：npm 核心概念

npm（Node Package Manager）是 Node.js 的默认包管理器。理解其核心概念是前端工程化的基础。

**package.json 结构解析（以 SelfBlog 为例）：**

```json
{
  "name": "selfblog",
  "version": "2.0.26",
  "private": true,
  "type": "module",
  "scripts": { ... },
  "dependencies": {
    "axios": "^1.7.4",
    "dompurify": "^3.0.9",
    "marked": "^12.0.1",
    "pinia": "^2.1.7",
    "vue": "^3.5.44",
    "vue-router": "^4.3.0"
  },
  "devDependencies": {
    "@eslint/js": "^9.39.4",
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/vue": "^8.1.0",
    "@vitejs/plugin-vue": "^5.0.4",
    "@vue/test-utils": "^2.4.10",
    "@vue/tsconfig": "^0.9.1",
    "eslint": "^9.39.4",
    "eslint-config-prettier": "^10.1.8",
    "eslint-plugin-vue": "^10.9.1",
    "jsdom": "^29.1.1",
    "prettier": "^3.8.3",
    "typescript": "^6.0.3",
    "typescript-eslint": "^8.60.1",
    "vite": "^6.4.4",
    "vitest": "^4.1.8",
    "vue-tsc": "^3.3.3"
  }
}
```

**关键字段说明：**

| 字段 | 说明 |
|------|------|
| `name` | 包名，发布到 npm 时的标识 |
| `version` | 语义化版本号（SemVer） |
| `private` | `true` 表示私有包，不会被意外发布 |
| `type` | `"module"` 表示使用 ES 模块（import/export） |
| `scripts` | 可通过 `npm run <name>` 执行的命令 |
| `dependencies` | 生产环境依赖 |
| `devDependencies` | 开发环境依赖（构建、测试、检查工具） |
| `engines` | 运行时版本约束 |

**语义化版本（SemVer）规则：**

版本号格式为 `主版本.次版本.修订版本`（如 `2.0.26`）：

- **主版本（Major）**：不兼容的 API 变更
- **次版本（Minor）**：向下兼容的功能新增
- **修订版本（Patch）**：向下兼容的问题修复

**版本范围符号：**

| 符号 | 含义 | 示例 |
|------|------|------|
| `^` | 兼容主版本 | `^3.5.44` → `>=3.5.44 <4.0.0` |
| `~` | 兼容次版本 | `~3.5.44` → `>=3.5.44 <3.6.0` |
| `>=` | 大于等于 | `>=2.0.0` |
| `*` | 任意版本 | `*` → 最新版本 |
| 无符号 | 精确版本 | `3.5.44` → 仅 `3.5.44` |

**npm install vs npm ci：**

```bash
# npm install：安装依赖，可能更新 package-lock.json
npm install

# npm ci：严格按 package-lock.json 安装，不修改锁文件
# 速度更快，适合 CI 环境
npm ci
```

`npm ci` 的优势：
1. **确定性**：严格按锁文件安装，保证所有环境一致
2. **速度**：跳过依赖解析，直接安装
3. **安全**：先删除 `node_modules` 再安装，避免残留
4. **CI 友好**：如果锁文件与 `package.json` 不一致会报错

### 6.1.3 脚本运行与 npm scripts

npm scripts 是前端工程化的核心枢纽。SelfBlog 定义了完整的脚本链：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "build:vercel-proxy": "node scripts/build-vercel-proxy.mjs",
    "test:deployment": "node --test scripts/__tests__/*.test.mjs",
    "preview": "vite preview",
    "lint": "eslint src --fix",
    "lint:check": "eslint src",
    "format": "prettier --write \"src/**/*.{ts,tsx,vue,css,json}\"",
    "format:check": "prettier --check \"src/**/*.{ts,tsx,vue,css,json}\"",
    "type-check": "vue-tsc --noEmit",
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage",
    "test:ui": "vitest --ui"
  }
}
```

**脚本分类与用途：**

| 类别 | 脚本 | 用途 |
|------|------|------|
| 开发 | `dev` | 启动 Vite 开发服务器（端口 3000） |
| 构建 | `build` | 类型检查 + 生产构建 |
| 构建 | `build:vercel-proxy` | 生成 Vercel 代理配置 |
| 检查 | `lint:check` | ESLint 只读检查 |
| 检查 | `format:check` | Prettier 格式检查 |
| 检查 | `type-check` | TypeScript 类型检查 |
| 修复 | `lint` | ESLint 自动修复 |
| 修复 | `format` | Prettier 自动格式化 |
| 测试 | `test:run` | Vitest 单次运行 |
| 测试 | `test:coverage` | 带覆盖率报告 |
| 测试 | `test:deployment` | 部署契约测试 |

**脚本执行顺序（build 链）：**

```bash
npm run build
# 等价于：
# 1. vue-tsc --noEmit  （类型检查）
# 2. vite build         （生产构建）
```

`&&` 表示前一个命令成功（退出码 0）才执行下一个。如果类型检查失败，构建不会执行。

**环境变量注入：**

```bash
# 开发环境
VITE_API_TARGET=http://localhost:8825 npm run dev

# 或者使用 .env 文件
echo "VITE_API_TARGET=http://localhost:8825" > .env
npm run dev
```

### 6.1.4 包管理器对比：npm vs pnpm vs yarn

| 特性 | npm | pnpm | yarn |
|------|-----|------|------|
| 安装速度 | 中等 | 最快 | 快 |
| 磁盘占用 | 高 | 低（硬链接） | 中 |
| 幽灵依赖 | 有 | 无（严格隔离） | 有 |
| 锁文件 | package-lock.json | pnpm-lock.yaml | yarn.lock |
| Monorepo | 支持（workspaces） | 原生支持 | 支持（workspaces） |

SelfBlog 使用 npm，因为它是 Node.js 内置的，无需额外安装，且 `package-lock.json` 提供了足够的确定性。

### 6.1.5 依赖背后的真相：package-lock.json 与幽灵依赖

**package-lock.json 为什么必须提交进仓库**：`package.json` 里的 `^1.7.4` 表示「1.7.4 及以上、2.0 以下都可以」——每次安装可能拿到不同的小版本。锁文件把整棵依赖树**精确到版本号 + 下载地址 + 完整性哈希**固定下来。不提交锁文件的团队，会在某个周一发现「我这里好好的，你那里装出来就是坏的」。SelfBlog CI 里用 `npm ci`（而非 `npm install`）正是为了**严格按锁文件装，装不对就报错**：

| 命令 | 行为 | 适用 |
|------|------|------|
| `npm install` | 按 package.json 语义化范围解析，可能更新锁文件 | 本地开发加新依赖 |
| `npm ci` | 删掉 node_modules 按锁文件精确安装，不改锁文件 | CI、重置环境 |

**幽灵依赖（Phantom Dependencies）**：你只装了 `vite`，但代码里 `import { something } from 'esbuild'` 居然能跑——因为 npm 的扁平化 node_modules 把**依赖的依赖**也提升到了顶层。这些「你没声明却能 import」的包就是幽灵依赖：某天 vite 升级换了内部依赖，你的代码凭空编译失败。pnpm 用严格隔离（每个包只能看见自己声明的依赖）从结构上消灭它；npm 项目则靠纪律：**import 只允许 package.json 里声明过的包**（ESLint 的 `no-extraneous` 规则可以机器检查）。

### 6.1.6 Monorepo 认知：SelfBlog 为什么是「两个工程」而不是 monorepo

SelfBlog 的 `backend/` 与 `frontend/` 是两个完全独立的工程：后端 Maven 管依赖、前端 npm 管依赖，**没有根 package.json、没有 workspace 配置**。这是刻意的选择：

- **部署形态决定**：后端打 jar、前端产物内嵌 jar，两端生命周期不同步，强行 monorepo 只增加构建复杂度
- **技术栈无共享**：Java 与 TS 没有共享代码，monorepo 的核心收益（跨包原子提交、共享构建配置）在这里为零

什么时候值得上 monorepo（pnpm workspace / turborepo / nx）：多个前端包共享组件库、多个服务共享类型定义、需要原子化跨包变更时。**判断标准是「有没有真实共享」，不是「看起来高大上」。**

### 6.1.7 package.json 逐字段精读：以 SelfBlog 的声明为例

`package.json` 是前端工程化的「宪法」，值得逐字段读一遍。以下是 SelfBlog `frontend/package.json` 的完整骨架（版本号以仓库实际为准）：

```json
{
  "name": "selfblog",
  "version": "2.0.26",
  "private": true,
  "type": "module",
  "scripts": { "...": "见 6.1.3" },
  "dependencies": {
    "axios": "^1.7.4",
    "dompurify": "^3.0.9",
    "marked": "^12.0.1",
    "pinia": "^2.1.7",
    "vue": "^3.5.44",
    "vue-router": "^4.3.0"
  },
  "devDependencies": {
    "eslint": "^9.39.4",
    "prettier": "^3.8.3",
    "typescript": "^6.0.3",
    "vite": "^6.4.4",
    "vitest": "^4.1.8"
  }
}
```

逐字段说明：

| 字段 | 作用 | SelfBlog 的取值理由 |
|------|------|--------------------|
| `name` / `version` | 包标识 | 个人项目不发布 npm，仅作标识 |
| `private: true` | **禁止发布到 npm 仓库** | 应用类项目的标准防误操作 |
| `type: "module"` | 声明 ESM：`.js` 文件按 ES 模块解析 | Vite 生态默认 ESM；`scripts/*.mjs` 构建脚本因此可用顶层 `import` |
| `engines` | 声明 Node 版本要求 | 配合 `.npmrc` 的 `engine-strict=true`，版本不符直接报错（见 6.1.1） |

**dependencies 与 devDependencies 的分界线**：前者是「应用运行时需要的包」——axios（请求）、vue/pinia/vue-router（框架三件套）、marked + dompurify（Markdown 渲染，产物里真实包含，所以必须在 dependencies）；后者是「开发与构建时需要的包」——ESLint/Prettier/TS/Vite/Vitest 装与不装只影响开发体验和构建流程，不影响运行时行为。判别问题只有一句话：**构建产物里会不会出现它的代码？会 → dependencies，不会 → devDependencies。**

常见误区：把 Vite 放进 dependencies（多数部署平台无所谓，但语义错了）；或者以为 devDependencies 在生产不装——`npm install --production` 才会跳过它们，普通 `npm install` 全都装。**SelfBlog CI 前端 job 用 `npm ci` 装全部依赖**，因为构建与测试恰恰是 CI 的主业。

**semver（语义化版本）速读**：`主版本.次版本.修订号`——主版本变更允许破坏兼容，次版本向下兼容地加功能，修订号只修 bug。`package.json` 里的前缀决定自动升级的边界：

```
^1.7.4   允许 1.x.x 内升级（>=1.7.4 <2.0.0）——npm install 的默认值
~1.7.4   只允许修订号升级（>=1.7.4 <1.8.0）——更保守
1.7.4    锁死精确版本
* / latest  任何版本——危险，几乎不该出现在工程里
```

`^` 的哲学是「信任次版本兼容承诺」，把精确性交给锁文件（6.1.5）：package.json 表达**意图**，package-lock.json 表达**事实**。

**node_modules 与依赖树速写**：`npm install` 完成后，`node_modules/.bin/` 存放所有包的可执行垫片（`vite`、`vitest`、`eslint`……npm scripts 里的裸命令就是从这里解析的——这也是 6.2.x PATH 惨案的物理现场：垫片脚本最终调用 `node`，PATH 坏了它就找不到）。每装一个包，npm 同时安装它声明的全部依赖，递归成树；npm v3+ 用**扁平化**把公共依赖尽量提升到顶层以减少重复——代价就是幽灵依赖（6.1.5）。想知道「为什么会有这个包」时用 `npm ls <pkg>`：它会打印从直接依赖到该包的完整引用链，排查版本冲突（同一包多个版本并存）的第一工具。

```bash
npm ls marked          # marked 被谁依赖、装了几个版本
npm dedupe             # 尝试合并重复版本，缩小依赖树
npm outdated           # 列出「已装版本 / 想要版本 / 最新版本」三列，升级前先看
```

**npm scripts 的进阶细节**（补 6.1.3）：`pre`/`post` 前缀钩子会自动执行（`pretest` 先于 `test`）；`npm run` 会把 `node_modules/.bin` 临时加进 PATH，所以脚本里能直接写 `vite` 而不用全路径；`npm test`、`npm start`、`npm stop` 是内置别名可省略 `run`；脚本间传参用 `--` 分隔——`npm run test -- --coverage` 的 `--coverage` 会透传给 vitest。环境变量注入见 6.1.3 结尾（`NODE_ENV`、`npm_package_*` 系列由 npm 自动提供）。

---

## 6.2 Vite 构建工具

### 6.2.1 Vite 的核心理念

Vite（法语"快"的意思）由尤雨溪创建，是下一代前端构建工具。其核心理念是**利用浏览器原生 ES 模块**，在开发环境下无需打包，实现毫秒级冷启动。

**传统打包工具（Webpack）的问题：**

```
启动时间 = 项目大小 × 打包速度
项目越大，启动越慢
```

**Vite 的解决方案：**

```
开发环境：浏览器原生 ESM + 按需编译
生产环境：Rollup 打包（Tree-shaking + 代码分割）
```

### 6.2.2 vite.config.ts 完整解析

SelfBlog 的 `vite.config.ts` 是一个功能完整的配置示例：

```typescript
/// <reference types="vitest" />
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['src/test/setup.ts'],
      include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx}'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        include: ['src/**/*.{js,ts,vue}'],
        exclude: ['src/main.ts', 'src/**/*.test.ts', 'src/**/*.spec.ts']
      }
    },
    server: {
      port: 3000,
      fs: {
        allow: ['..']
      },
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8825',
          changeOrigin: true
        },
        '/uploads': {
          target: env.VITE_API_TARGET || 'http://localhost:8825',
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      minify: 'esbuild',
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['vue', 'vue-router', 'pinia'],
            markdown: ['marked', 'dompurify']
          }
        }
      }
    }
  }
})
```

**配置项详解：**

**1. `defineConfig` 函数形式**

```typescript
// 对象形式（静态配置）
export default defineConfig({ ... })

// 函数形式（动态配置，可读取 mode 和 env）
export default defineConfig(({ mode }) => {
  return { ... }
})
```

函数形式的优势是可以根据 `mode`（development/production/test）和环境变量动态调整配置。

**2. `loadEnv` 加载环境变量**

```typescript
const env = loadEnv(mode, process.cwd(), '')
// 第三个参数 '' 表示加载所有环境变量（包括非 VITE_ 前缀的）
// 默认只加载 VITE_ 前缀的变量
```

**3. 插件系统**

```typescript
import vue from '@vitejs/plugin-vue'

plugins: [vue()]
```

`@vitejs/plugin-vue` 是 Vue 3 的官方 Vite 插件，负责：
- 编译 `.vue` 单文件组件（SFC）
- 处理 `<template>`、`<script>`、`<style>` 块
- 支持 HMR（热模块替换）

**4. 路径别名**

```typescript
resolve: {
  alias: {
    '@': resolve(__dirname, 'src')
  }
}
```

使用 `@` 代替 `src` 目录，避免相对路径过深：

```typescript
// 不使用别名
import { api } from '../../../api/index'

// 使用别名
import { api } from '@/api/index'
```

**5. 开发服务器配置**

```typescript
server: {
  port: 3000,
  fs: {
    allow: ['..']  // 允许访问父目录（用于读取仓库根共享的 fixture）
  },
  proxy: {
    '/api': {
      target: env.VITE_API_TARGET || 'http://localhost:8825',
      changeOrigin: true
    }
  }
}
```

**代理配置详解：**

| 选项 | 说明 |
|------|------|
| `target` | 代理目标地址 |
| `changeOrigin` | 修改请求头中的 `Host` 为目标地址 |
| `rewrite` | 重写路径（如 `rewrite: (path) => path.replace(/^\/api/, '')`） |
| `secure` | 是否验证 SSL 证书 |

**为什么需要代理？**

浏览器同源策略（CORS）阻止跨域请求。开发时前端运行在 `localhost:3000`，后端在 `localhost:8825`，通过 Vite 代理可以绕过 CORS 限制。

**6. 生产构建配置**

```typescript
build: {
  outDir: 'dist',           // 输出目录
  assetsDir: 'assets',      // 静态资源子目录
  sourcemap: false,         // 不生成 sourcemap（生产环境）
  minify: 'esbuild',        // 使用 esbuild 压缩（比 terser 快）
  rollupOptions: {
    output: {
      manualChunks: {
        vendor: ['vue', 'vue-router', 'pinia'],
        markdown: ['marked', 'dompurify']
      }
    }
  }
}
```

**代码分割（Code Splitting）：**

`manualChunks` 将第三方库拆分为独立 chunk：

```
dist/
├── assets/
│   ├── index-[hash].js      # 业务代码
│   ├── vendor-[hash].js     # Vue 生态（vue, vue-router, pinia）
│   └── markdown-[hash].js   # Markdown 渲染（marked, dompurify）
```

**为什么手动分割？**

1. **缓存优化**：第三方库变化频率低，浏览器可长期缓存
2. **并行加载**：多个 chunk 可并行下载
3. **首屏优化**：只加载必要的 chunk

### 6.2.3 Vite 插件生态

**常用插件：**

```typescript
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [
    vue(),
    // 其他插件
  ]
})
```

| 插件 | 用途 |
|------|------|
| `@vitejs/plugin-vue` | Vue 3 SFC 支持 |
| `@vitejs/plugin-react` | React JSX 支持 |
| `vite-plugin-eslint` | 开发时 ESLint 检查 |
| `vite-plugin-pwa` | PWA 支持 |
| `vite-plugin-compression` | 构建时 gzip/brotli 压缩 |
| `vite-plugin-html` | HTML 模板处理 |

### 6.2.4 环境变量与模式

Vite 支持多环境配置：

```
.env                # 所有环境
.env.local          # 所有环境（不提交 git）
.env.development    # 开发环境
.env.production     # 生产环境
.env.test           # 测试环境
```

**SelfBlog 的环境变量：**

```env
# .env
VITE_APP_TITLE=个人博客
VITE_API_TARGET=http://localhost:8825
VITE_API_BASE_URL=/api
VITE_APP_VERSION=2.0.25
```

**在代码中使用：**

```typescript
// 通过 import.meta.env 访问
const apiTarget = import.meta.env.VITE_API_TARGET
const isDev = import.meta.env.DEV      // 是否开发模式
const isProd = import.meta.env.PROD    // 是否生产模式
const mode = import.meta.env.MODE     // 当前模式
```

**类型声明（env.d.ts）：**

```typescript
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TARGET?: string
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

### 6.2.5 开发服务器与 HMR

**启动开发服务器：**

```bash
npm run dev
# 或
npx vite
```

**HMR（热模块替换）工作原理：**

```
1. 修改文件
2. Vite 检测文件变化
3. 通过 WebSocket 通知浏览器
4. 浏览器请求更新的模块
5. 只替换变化的模块，不刷新页面
```

**Vue SFC 的 HMR：**

```vue
<template>
  <div>{{ message }}</div>
</template>

<script setup lang="ts">
const message = 'Hello'
</script>

<style>
div { color: red; }
</style>
```

修改 `<template>` 时，Vite 只更新模板部分，保留组件状态。修改 `<style>` 时，只替换 CSS，不重新渲染组件。

### 6.2.x 本机环境的真实陷阱：PATH 引号惨案（来自 SelfBlog 开发机实录）

教程里的命令在你机器上「跑不起来」时，先怀疑环境本身。这是一个真实的、极其隐蔽的案例：

**症状**：`npm run dev`、`npm run build`、`npm run test:run` 全部失败，报错 `'"node"' 不是内部或外部命令`。但 `npm ci`、`npm install` 正常；`where node` 也能找到 node。

**根因**：系统 PATH 里有一条 `C:\Program Files\Java\jdk-25\bin"`——末尾多了个**不配对的引号**。`cmd.exe` 解析 PATH 时把它当成一段未闭合引号的开头，其后所有条目全部失效。`node` 本身靠 App Paths 侥幸能找到，但 `node_modules/.bin/*.cmd` 垫片脚本结尾调用的是裸 `node`，于是全军覆没。bash 不受影响（解析规则不同）。

**临时解法**（SelfBlog 仓库采用）：绕开 npm 脚本层，直接调用 POSIX 垫片：

```bash
cd frontend
./node_modules/.bin/vite              # = npm run dev
./node_modules/.bin/vue-tsc --noEmit  # = npm run type-check
./node_modules/.bin/eslint src        # = npm run lint:check
./node_modules/.bin/vitest run        # = npm run test:run
./node_modules/.bin/vite build        # 生产构建
```

**根治解法**：删掉 PATH 里那条多余的引号。**教训**：错误信息指向「node 不存在」时，别急着重装 Node——先用 `echo %PATH%`（cmd）逐条目视检查引号、分号是否配对完整。环境变量损坏的症状往往离根因很远。

### 6.2.y Vite 为什么快：esbuild 预构建与原生 ESM 深入

6.2.1 给了结论，这里补「为什么」。两个关键词撑起 Vite 的开发态速度：

**① esbuild 预构建（依赖预处理）**。首次启动（或升级依赖后）时，Vite 扫描入口的 `import`，把 `node_modules` 里的第三方包（vue、axios、marked…）用 **esbuild**（Go 语言编写，比 JS 系打包器快一个数量级）打包成单个 ESM 文件，缓存到 `node_modules/.vite`。它同时解决三个问题：

- **格式问题**：不少包发布的是 CommonJS 格式，浏览器原生 ESM 不认识——预构建把 CJS 转成 ESM；
- **请求数问题**：一个包内部可能拆成几百个小模块文件（如 lodash-es），逐个请求会把开发服务器打成瀑布——预构建「合多为一」；
- **速度问题**：依赖基本不变，缓存复用后二次启动几乎零成本；依赖变了 Vite 会自动重新预构建，排障时也可 `npx vite --force` 强制重跑。

**② 原生 ESM 开发服务器（源码按需编译）**。你写的 `src/` 模块不打包：每个模块就是一个 URL，浏览器请求 `/src/main.ts` 时 Vite 才按需把它编译成 JS 返回——**请求谁编译谁，没被请求的模块根本不参与**。因此启动时间与项目总大小无关，只与当前页面用到的模块数有关；HMR 快也是同一原理：改一个文件只需失效并重编译这一个模块，不像 Webpack 要重算整片受影响的依赖图。

一句话分工：**预构建管「第三方依赖」（启动时一次、缓存复用），按需编译管「你的源码」（请求时逐个）**。代价是开发态的请求瀑布——一个模块 import 十个依赖就是十连请求，HTTP/1.1 六连接限制下更明显；好在开发服务器走 localhost、HTTP/1.1 keep-alive 复用连接，实际可接受。

**生产构建为什么换 Rollup**：开发态要「快与随缘」，生产态要「最优产物」——Tree-shaking 摇掉未引用代码、按路由/手动策略做代码分割、成熟的 chunk 命名与资源内联。Vite 已把压缩交给 esbuild（`build.minify: 'esbuild'` 默认值，见 6.2.2），但整体打包仍是 Rollup。想验证 esbuild 的存在感：`npm run build` 输出里的转换速度、以及 6.8.6 产物分析中 chunk 体积——用 esbuild 压缩比 terser 快数倍且体积接近。

**环境变量与模式的工程细节**（补 6.2.4 的实战面）：

- `VITE_` 前缀是**安全边界**：只有带前缀的变量会注入 `import.meta.env` 暴露给客户端代码——任何变量一旦加 `VITE_` 就要当成「会被打包进产物、可被任何访客看到」的公开信息，密钥绝对不能放（放后端 `.env`）。
- `.env.local` / `.env.*.local` 默认被 gitignore——放个人本机覆盖值（如指向不同的 `VITE_API_TARGET`），团队共享的默认值放 `.env`。
- 优先级从高到低：`process` 环境变量 > `.env.[mode].local` > `.env.[mode]` > `.env.local` > `.env`——CI 里注入的环境变量会覆盖文件值，这正是 GitHub Actions 能「改环境不改文件」的原因。
- SelfBlog 的 `vite.config.ts` 用 `loadEnv(mode, process.cwd(), '')` 第三个参数空串加载**全部**变量（含不带 `VITE_` 前缀的），因为配置文件本身是 Node 进程、不进浏览器，读取 `VITE_API_TARGET` 决定代理目标不需要它对客户端可见（见 6.2.2 配置解析）。

### 6.2.z 构建产物分析与优化清单

```bash
cd frontend
./node_modules/.bin/vite build          # 先看默认产物
npx vite preview                        # 本地起服务预览 dist，验证的不是源码而是产物
```

`vite build` 输出的 chunk 清单就是最轻量的分析工具：`dist/assets/index-xxxx.js`（入口+路由）、`vendor-xxxx.js`（manualChunks 切出的框架包）、`markdown-xxxx.js`（marked+dompurify）……每个 chunk 后面跟着 gzip 后体积。要更细的可视化可加 `rollup-plugin-visualizer` 生成 treemap（SelfBlog 未装——按需原则，chunk 清单已够定位异常）。

SelfBlog 的切包策略（`manualChunks`）与理由：

```typescript
manualChunks: {
  vendor: ['vue', 'vue-router', 'pinia'],   // 框架三件套：几乎不变，强缓存收益最大化
  markdown: ['marked', 'dompurify']         // 渲染库：只有文章详情页真正用到，
}                                            // 单独成 chunk 让首页访客不必下载
```

优化清单（按收益排序）：

1. **查异常 chunk**：某个 chunk 突然变大，八成是哪个组件整包引入了全家桶库——`import debounce from 'lodash/debounce'` 改子路径引入，或换原生 `Intl`/`crypto` API；
2. **动态 import 路由组件**：Vue Router 的 `() => import('...')` 让每个页面各自成 chunk，首屏只载入口——SelfBlog 路由已如此；
3. **`sourcemap: false`**（生产默认）：sourcemap 方便排错但体积翻倍，公开站点关掉，需要时本地用 `--sourcemap` 临时构建；
4. **别手动优化压缩**：`minify: 'esbuild'` 默认已是速度与体积的甜点，Gzip/Brotli 交给反代（Vercel 自动、Nginx 见 6.8.9）。

**原则收束**：产物优化先「看」（chunk 清单/可视化）再「动」，每动一步用 6.8.10 的 Lighthouse 复测——没有测量就没有优化。

---

## 6.3 ESLint 代码检查

### 6.3.1 ESLint 概述

ESLint 是 JavaScript/TypeScript 的静态代码分析工具，用于发现代码中的潜在问题并强制执行编码规范。

**ESLint 能做什么：**

- 发现未使用的变量
- 检测未定义的变量
- 强制一致的代码风格
- 发现潜在 bug（如 `==` vs `===`）
- 强制执行最佳实践

### 6.3.2 Flat Config（ESLint 9+）

ESLint 9 引入了 Flat Config（`eslint.config.js`），替代了旧的 `.eslintrc` 格式。SelfBlog 使用 Flat Config：

```javascript
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

export default tseslint.config(
  // Global ignores
  {
    ignores: ['dist/**', 'node_modules/**', '*.d.ts']
  },

  // Browser globals
  {
    languageOptions: {
      globals: {
        ...globals.browser
      }
    }
  },

  // Base JS rules
  js.configs.recommended,

  // TypeScript rules
  ...tseslint.configs.recommended,

  // Vue rules
  ...pluginVue.configs['flat/essential'],

  // Prettier (disables conflicting rules)
  prettier,

  // Custom rules
  {
    files: ['**/*.{ts,tsx,vue}'],
    rules: {
      // TypeScript
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],

      // Vue
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'warn',

      // General
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'warn'
    }
  },

  // Vue file specific parser
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  }
)
```

**配置结构解析：**

**1. 全局忽略（ignores）**

```javascript
{
  ignores: ['dist/**', 'node_modules/**', '*.d.ts']
}
```

这些路径下的文件不会被 ESLint 检查。

**2. 全局变量（globals）**

```javascript
{
  languageOptions: {
    globals: {
      ...globals.browser  // window, document, navigator 等
    }
  }
}
```

声明浏览器全局变量，避免 `no-undef` 规则误报。

**3. 推荐规则集**

```javascript
js.configs.recommended,           // JavaScript 推荐规则
...tseslint.configs.recommended,  // TypeScript 推荐规则
...pluginVue.configs['flat/essential'],  // Vue 推荐规则
```

**4. Prettier 集成**

```javascript
prettier,  // eslint-config-prettier
```

`eslint-config-prettier` 会关闭所有与 Prettier 格式化冲突的 ESLint 规则。这是 ESLint 与 Prettier 协作的关键。

**5. 自定义规则**

```javascript
{
  files: ['**/*.{ts,tsx,vue}'],
  rules: {
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    '@typescript-eslint/no-explicit-any': 'warn',
    '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'warn',
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'no-debugger': 'warn'
  }
}
```

**规则严重级别：**

| 级别 | 含义 | 退出码 |
|------|------|--------|
| `'off'` / `0` | 关闭规则 | - |
| `'warn'` / `1` | 警告（不中断构建） | 0 |
| `'error'` / `2` | 错误（中断构建） | 1 |

**常用规则说明：**

| 规则 | 说明 |
|------|------|
| `@typescript-eslint/no-unused-vars` | 未使用的变量 |
| `@typescript-eslint/no-explicit-any` | 禁止使用 `any` 类型 |
| `@typescript-eslint/consistent-type-imports` | 强制使用 `import type` |
| `vue/multi-word-component-names` | 组件名必须多单词 |
| `vue/no-v-html` | 警告使用 `v-html`（XSS 风险） |
| `no-console` | 限制 `console` 使用 |
| `no-debugger` | 禁止 `debugger` 语句 |

**6. Vue 文件解析器**

```javascript
{
  files: ['**/*.vue'],
  languageOptions: {
    parserOptions: {
      parser: tseslint.parser  // 使用 TypeScript 解析器解析 <script> 块
    }
  }
}
```

Vue SFC 的 `<script>` 块可能包含 TypeScript，需要指定 TypeScript 解析器。

### 6.3.3 自动修复

ESLint 支持自动修复（`--fix`）：

```bash
# 自动修复
npm run lint
# 等价于：eslint src --fix

# 只检查不修复
npm run lint:check
# 等价于：eslint src
```

**可自动修复的规则：**

- 代码风格（缩进、引号、分号）
- 未使用的导入
- 简单的语法问题

**不可自动修复的规则：**

- 逻辑错误
- 类型错误
- 复杂的代码问题

### 6.3.4 与 Prettier 集成

**为什么需要同时使用 ESLint 和 Prettier？**

- **ESLint**：代码质量检查（发现 bug、强制最佳实践）
- **Prettier**：代码格式化（统一的代码风格）

**集成方式：**

```javascript
// eslint.config.js
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  // ... 其他配置
  prettier  // 必须放在最后，关闭冲突规则
)
```

**eslint-config-prettier 的作用：**

它会关闭所有与 Prettier 格式化冲突的 ESLint 规则，例如：
- `indent`（缩进）
- `quotes`（引号）
- `semi`（分号）
- `max-len`（行宽）

这样 ESLint 专注于代码质量，Prettier 专注于代码格式，互不干扰。

### 6.3.5 编辑器集成

**VS Code 设置（.vscode/settings.json）：**

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  },
  "eslint.validate": [
    "javascript",
    "typescript",
    "vue"
  ]
}
```

**保存时自动修复：**

1. 保存文件
2. Prettier 格式化代码
3. ESLint 自动修复可修复的问题

---

## 6.4 Prettier 代码格式化

### 6.4.1 Prettier 概述

Prettier 是一个"有主见"的代码格式化工具，支持 JavaScript、TypeScript、CSS、HTML、JSON、Markdown 等多种语言。

**Prettier 的设计哲学：**

- **一致性**：所有代码看起来像同一个人写的
- **无配置**：提供合理的默认值，减少团队争论
- **不可协商**：格式化规则不可自定义（少数选项除外）

### 6.4.2 .prettierrc 配置解析

SelfBlog 的 Prettier 配置：

```json
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "none",
  "printWidth": 100,
  "bracketSpacing": true,
  "arrowParens": "always",
  "endOfLine": "auto",
  "vueIndentScriptAndStyle": false
}
```

**配置项详解：**

| 选项 | 值 | 说明 |
|------|-----|------|
| `semi` | `false` | 不在语句末尾添加分号 |
| `singleQuote` | `true` | 使用单引号（而非双引号） |
| `tabWidth` | `2` | 缩进宽度为 2 个空格 |
| `trailingComma` | `none` | 不在数组/对象末尾添加逗号 |
| `printWidth` | `100` | 每行最大字符数 |
| `bracketSpacing` | `true` | 对象字面量括号内添加空格 |
| `arrowParens` | `always` | 箭头函数参数始终加括号 |
| `endOfLine` | `auto` | 行尾符自动检测（兼容 Windows/Linux） |
| `vueIndentScriptAndStyle` | `false` | 不缩进 Vue SFC 的 `<script>` 和 `<style>` |

**格式化效果对比：**

```javascript
// 格式化前
const obj={name:'test',age:18}
function greet(name){return `Hello, ${name}`}

// 格式化后
const obj = { name: 'test', age: 18 }
function greet(name) {
  return `Hello, ${name}`
}
```

### 6.4.3 命令行使用

```bash
# 格式化所有文件
npm run format
# 等价于：prettier --write "src/**/*.{ts,tsx,vue,css,json}"

# 检查格式（不修改文件）
npm run format:check
# 等价于：prettier --check "src/**/*.{ts,tsx,vue,css,json}"

# 格式化单个文件
npx prettier --write src/main.ts

# 检查单个文件
npx prettier --check src/main.ts
```

### 6.4.4 编辑器集成

**VS Code 插件：**

1. 安装 `Prettier - Code formatter` 插件
2. 设置为默认格式化工具

```json
{
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.formatOnSave": true
}
```

**保存时自动格式化：**

```json
{
  "editor.formatOnSave": true
}
```

### 6.4.5 .prettierignore

```gitignore
# 构建输出
dist
node_modules

# 锁文件
package-lock.json
pnpm-lock.yaml
yarn.lock

# 文档
*.md

# 第三方代码
vendor
```

### 6.4.6 Prettier 与 ESLint 的协作

**分工明确：**

| 工具 | 职责 |
|------|------|
| Prettier | 代码格式（缩进、引号、分号、行宽） |
| ESLint | 代码质量（未使用变量、潜在 bug、最佳实践） |

**协作流程：**

```
1. 保存文件
2. Prettier 格式化代码风格
3. ESLint 检查代码质量
4. ESLint 自动修复可修复的问题
```

**为什么需要 eslint-config-prettier？**

ESLint 的一些规则与 Prettier 冲突（如缩进、引号），`eslint-config-prettier` 会关闭这些冲突规则，让两个工具和平共处。

---

## 6.5 TypeScript 集成

### 6.5.1 TypeScript 概述

TypeScript 是 JavaScript 的超集，添加了静态类型系统。它能在编译时发现类型错误，提高代码质量和开发效率。

**TypeScript 的优势：**

- **类型安全**：编译时发现类型错误
- **智能提示**：IDE 提供准确的自动补全
- **重构支持**：类型系统使重构更安全
- **文档作用**：类型即文档

### 6.5.2 tsconfig.json 完整解析

SelfBlog 的 TypeScript 配置：

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "ignoreDeprecations": "6.0",

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",

    /* Linting */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,

    /* Path aliases */
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },

    /* Vue */
    "types": ["vite/client"]
  },
  "include": [
    "src/**/*.ts",
    "src/**/*.tsx",
    "src/**/*.vue",
    "src/**/*.d.ts",
    "env.d.ts"
  ],
  "exclude": ["node_modules", "dist"]
}
```

**配置项详解：**

**1. 目标与模块**

| 选项 | 值 | 说明 |
|------|-----|------|
| `target` | `ES2020` | 编译目标为 ES2020 |
| `module` | `ESNext` | 使用 ES 模块 |
| `moduleResolution` | `bundler` | 打包工具模式（Vite/Webpack） |
| `lib` | `["ES2020", "DOM", "DOM.Iterable"]` | 包含的库类型 |

**2. Bundler 模式**

```json
{
  "moduleResolution": "bundler",
  "allowImportingTsExtensions": true,
  "isolatedModules": true,
  "moduleDetection": "force",
  "noEmit": true
}
```

这些选项是为打包工具（Vite）设计的：
- `moduleResolution: "bundler"`：支持打包工具的模块解析
- `allowImportingTsExtensions`：允许导入 `.ts` 扩展名
- `isolatedModules`：确保每个文件可独立编译
- `noEmit`：不生成输出文件（由 Vite 负责打包）

**3. 严格模式**

```json
{
  "strict": true,
  "noUnusedLocals": true,
  "noUnusedParameters": true,
  "noFallthroughCasesInSwitch": true,
  "noUncheckedIndexedAccess": true
}
```

`strict: true` 启用所有严格检查选项：

| 选项 | 说明 |
|------|------|
| `strict` | 启用所有严格检查 |
| `noUnusedLocals` | 报告未使用的局部变量 |
| `noUnusedParameters` | 报告未使用的函数参数 |
| `noFallthroughCasesInSwitch` | 报告 switch 语句的 fall-through |
| `noUncheckedIndexedAccess` | 索引访问返回 `T \| undefined` |

**`noUncheckedIndexedAccess` 详解：**

```typescript
const arr = [1, 2, 3]
const first = arr[0]  // 类型为 number | undefined

// 错误：可能为 undefined
console.log(first.toFixed(2))

// 正确：使用可选链
console.log(first?.toFixed(2))

// 正确：使用非空断言（确定不为空时）
console.log(first!.toFixed(2))
```

**4. 路径别名**

```json
{
  "baseUrl": ".",
  "paths": {
    "@/*": ["src/*"]
  }
}
```

与 Vite 的 `resolve.alias` 对应：

```typescript
// vite.config.ts
resolve: {
  alias: {
    '@': resolve(__dirname, 'src')
  }
}
```

**5. Vue 类型支持**

```json
{
  "types": ["vite/client"]
}
```

`vite/client` 提供了 Vite 特有的类型定义（如 `import.meta.env`）。

### 6.5.3 vue-tsc 类型检查

`vue-tsc` 是 Vue 官方的 TypeScript 类型检查工具，它在 `tsc` 的基础上增加了对 `.vue` 文件的支持。

**类型检查命令：**

```bash
npm run type-check
# 等价于：vue-tsc --noEmit
```

**`--noEmit` 的作用：**

只进行类型检查，不生成输出文件。因为 Vite 负责实际的打包工作，TypeScript 只需要检查类型。

**类型检查流程：**

```
1. vue-tsc 读取 tsconfig.json
2. 解析所有 .ts/.tsx/.vue 文件
3. 检查类型错误
4. 报告错误（不生成文件）
```

### 6.5.4 TypeScript 与 Vue 3

**`<script setup lang="ts">` 的优势：**

```vue
<template>
  <div>{{ message }}</div>
</template>

<script setup lang="ts">
// 类型推断
const message = 'Hello'  // 类型为 string

// 类型注解
const count: number = 0

// 接口定义
interface User {
  name: string
  age: number
}

const user: User = {
  name: 'Alice',
  age: 25
}

// 泛型
function identity<T>(arg: T): T {
  return arg
}

const result = identity<string>('Hello')
</script>
```

**Props 类型定义：**

```vue
<script setup lang="ts">
// 方式一：运行时声明
const props = defineProps<{
  title: string
  count?: number
}>()

// 方式二：类型声明（推荐）
interface Props {
  title: string
  count?: number
}
const props = defineProps<Props>()
</script>
```

**Emits 类型定义：**

```vue
<script setup lang="ts">
const emit = defineEmits<{
  (e: 'change', value: string): void
  (e: 'update', id: number): void
}>()

// 使用
emit('change', 'new value')
</script>
```

**Ref 类型：**

```typescript
import { ref } from 'vue'

// 自动推断类型
const count = ref(0)  // Ref<number>

// 显式类型声明
const user = ref<User | null>(null)

// 访问值
count.value++
console.log(user.value?.name)
```

### 6.5.5 类型声明文件（.d.ts）

**env.d.ts：**

```typescript
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TARGET?: string
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

**`declare module '*.vue'` 的作用：**

让 TypeScript 理解 `.vue` 文件的导入。Vue SFC 不是标准的 TypeScript 文件，需要声明模块类型。

**`ImportMetaEnv` 的作用：**

为 `import.meta.env` 提供类型提示，避免访问不存在的环境变量时报错。

### 6.5.6 现代 JavaScript 语法补强：ES6+ 特性清单

SelfBlog 代码里铺满了 ES2015 以来的语法。它们常被「当作空气」，但每个都有明确语义，值得集中过一遍——尤其 `?.` 与 `??` 这对组合，是 `noUncheckedIndexedAccess` 严格模式下的生存技能（见 6.5.2）。

**解构赋值（Destructuring）**——从对象/数组中按模式取值：

```typescript
// 对象解构：props 声明与 store 使用处随处可见
const { records, total } = res.data                  // 分页信封拆包
const { name, age = 18 } = user                      // 带默认值
let { name: userName } = user                        // 重命名

// 数组解构
const [first, second] = list                          // 注意：noUncheckedIndexedAccess 下 first/second 类型含 undefined
const [head, ...rest] = list                          // rest 收集剩余项

// 函数参数解构（Vue 组合式 API 的标准形态）
function usePagination({ page = 1, size = 10 }: Options = {}) { /* ... */ }
```

**展开运算符（Spread / Rest）**——`...` 既是展开也是收集：

```typescript
// 展开：创建新对象而非修改旧对象（Pinia/响应式系统友好）
const next = { ...state, page: 2 }                    // 浅拷贝 + 覆盖字段
const merged = [...oldList, newItem]                  // 追加而不 push

// 注意浅拷贝边界：嵌套对象仍共享引用
const copy = { ...user }                              // copy.address === user.address
// Rest 参数：收集函数剩余实参
function logAll(level: string, ...args: unknown[]) { /* ... */ }
```

**可选链 `?.` 与空值合并 `??`**——SecurityUtils 风格的「可能为空」防御（前端对应物）：

```typescript
const city = user?.address?.city          // 任一环节 null/undefined 就返回 undefined，不抛 TypeError
const port = config.port ?? 3000          // 仅在 null/undefined 时取默认值
const first = arr.at(0)?.name ?? '未知'   // 典型组合：索引访问 + 可选链 + 兜底

// 关键区别：?? 与 || 不同——0、''、false 是合法值不该被吞
const volume = 0
volume || 100   // 100（错误！0 被当成假值）
volume ?? 100   // 0（正确）
```

这解释了 6.5.2 的规则：`noUncheckedIndexedAccess` 让 `arr[0].foo` 编译报错（返回类型是 `T | undefined`），惯用修法正是 `arr[0]?.foo ?? fallback`。SelfBlog 真实案例：`import.meta.env.VITE_API_TARGET ?? 'http://localhost:8825'`——用 `??` 而不是 `||`，虽然此处恰好是字符串，但语义更准确：只有「确实没配置」才回落默认值。

**Promise 与 async/await 深入**——axios 拦截器（6.8.2 提到的刷新排队）就是 Promise 状态机的教科书案例：

```typescript
// async 函数总是返回 Promise；await 只在 async 函数（或顶层）可用
async function load(): Promise<Post> {
  const res = await api.get<Post>(`/post/${id}`)   // res 就是 { code, message, data } 信封
  if (res.code !== 200) throw new Error(res.message)
  return res.data
}

// 串行 vs 并行：无依赖的请求应并行
const [post, comments] = await Promise.all([loadPost(), loadComments()])

// Promise.all vs allSettled：一个失败全炸 vs 全部出结果（成败并存）
const results = await Promise.allSettled([loadA(), loadB()])
const ok = results.filter(r => r.status === 'fulfilled')

// 错误处理：try/catch 捕获 await 的拒绝
try { await load() } catch (e) { /* 拦截器里 handleAuthError 已处理 401 */ }
```

三个高频误区：①循环里 `await`（串行慢）应改 `Promise.all`；②`forEach` 不认 `await`——回调里的 Promise 无人等待，要用 `for...of`；③async 函数抛出的错误是 Promise 拒绝，忘了 `await` 它就静默丢失。

**模块系统：ESM vs CommonJS**——SelfBlog 前端全 ESM（`"type": "module"`），但理解对照表才能看懂构建脚本与老文档：

```javascript
// ESM（浏览器原生 + Vite/Node 现代标准）：静态、可 Tree-shaking
import { marked } from 'marked'
export function render(src: string) { /* ... */ }
export default markdownIt

// CommonJS（Node 传统格式，构建脚本 .mjs 之外的老 .js 可能遇到）
const path = require('path')
module.exports = { buildVercelProxy }
```

关键差异：ESM 的 `import` 是**静态声明**（编译期可知依赖图，Rollup 才能摇树），CJS 的 `require` 是**运行时调用**（可以写在 if 里，代价是无法静态分析）。`import()` 动态导入是 ESM 的例外——返回 Promise，正是路由懒加载的底层机制。Node 里 `.mjs` 后缀或 `type: "module"` 走 ESM，`.cjs` 强制 CJS；Vite 项目的 `scripts/*.mjs` 用 `.mjs` 后缀就是为了在 `"type"` 缺省时也明确 ESM 身份。

### 6.5.7 CSS3 与 HTML5 基础补强：语义化、布局与设计 Token

工程化教程不该假装样式不存在——SelfBlog 的设计系统恰是 CSS 现代特性的集中示范。先补 HTML 侧：

**HTML5 语义化标签**——用「这是什么」而非「长什么样」命名结构，搜索引擎与读屏软件都靠它：

```html
<body>
  <header>   <!-- 站点头：logo、导航 -->
    <nav aria-label="主导航">…</nav>
  </header>
  <main>     <!-- 每页唯一；<h1> 也应唯一 -->
    <article>  <!-- 独立可分发的内容单元：一篇文章、一条动态 -->
      <h1>文章标题</h1>
      <section>  <!-- 主题分组，通常配标题 -->
        <h2>小节</h2>
      </section>
    </article>
    <aside>    <!-- 与主内容弱相关：侧栏、推荐阅读 -->
  </main>
  <footer>   <!-- 站点脚：备案、版权 -->
</body>
```

三个原则：①**布局容器优先语义标签**，`<div>` 只留给纯粹的样式包装；②`<article>` 内可以再有 `<section>`，判断标准是「脱离页面还有没有意义」——博客文章是，装饰性卡片不是；③无障碍属性（`aria-label` 等）修饰交互控件，`<button>` 别写成 `<div @click>`——后者键盘无法聚焦，读屏不识别。Vue SFC 里写法相同，模板就是 HTML。

**Flexbox 与 Grid 布局详解**——现代 CSS 布局的两根支柱，分工是：**Flex 一维流（一行或一列），Grid 二维面（行×列同时定）**：

```css
/* Flexbox：导航栏、工具条、卡片内的水平排布 */
.site-header {
  display: flex;
  justify-content: space-between; /* 主轴：两端对齐 */
  align-items: center;            /* 交叉轴：垂直居中 */
  gap: 16px;                      /* 子项间距，取代 margin hack */
}
.card-list {
  display: flex;
  flex-wrap: wrap;                /* 换行 */
  gap: 16px;
}
.card-list > * { flex: 1 1 240px; }  /* 伸展/收缩/基准：每张卡至少 240px，可伸缩 */

/* Grid：页面骨架、相册网格、后台表单 */
.page-layout {
  display: grid;
  grid-template-columns: 240px 1fr;   /* 固定侧栏 + 自适应主区 */
  grid-template-rows: auto 1fr auto;  /* 头/身/脚 */
  min-height: 100vh;
}
.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); /* 响应式：不需要媒体查询 */
  gap: 12px;
}
```

选择经验：**先问「是一行还是一张表」**。一行内的对齐/分布用 Flex；需要「列宽互相约束」（如 `1fr` 与固定列共舞）或单元跨越行列时用 Grid。两者可以嵌套：Grid 定页面骨架，每个格子里再用 Flex 排内容——SelfBlog 的 `layout.css` 正是这种组合。此外 Flex 的 `flex: 1` 三值缩写（grow/shrink/basis）与 Grid 的 `minmax()` 是响应式布局的两把钥匙，`auto-fill + minmax` 更是「无媒体查询的响应式网格」标准答案。

**CSS 变量与主题切换**——这是 SelfBlog 设计系统的核心机制，也是工程化「 token 思维」的最佳教材。先看架构：`src/styles/main.css` 只是 `@import` 入口，真正分三层：

```css
/* variables.css —— 基础 token 层：:root 与 #selfblog 上定义全量变量 */
:root, #selfblog {
  --bg-primary: #f5f6f8;        /* 画布背景 */
  --surface-primary: #ffffff;   /* 卡片表面 */
  --text-primary: #262a31;      /* 正文 */
  --accent: #165dff;            /* 品牌蓝 */
  --radius-lg: 12px;            /* 圆角 token */
  --code-bg: #1d2129;           /* 代码块（永远深色） */
  --contrast-bg: #111111;       /* 高对比表面（主按钮） */
}

/* dark-mode.css —— 暗色覆盖层：同名变量重新赋值，选择器特异性略高 */
.dark-mode, #selfblog.dark-mode {
  --bg-primary: #171a20;
  --surface-primary: #20242c;
  --text-primary: #edf0f5;
  --gray-900: #ebebeb;          /* ← 注意！灰度梯度整体反转 */
}

/* skins.css —— 皮肤层：#selfblog[data-skin=…] 属性选择器换肤 */
#selfblog[data-skin='forest']:not(.dark-mode)  { --accent: #2d7c54; /* … */ }
#selfblog[data-skin='sunset']:not(.dark-mode)  { --accent: #c45b3e; /* … */ }
#selfblog[data-skin='midnight']                { --bg-primary: #101823; /* … */ }
```

切换的实现因此轻得不可思议——**只改一个类或一个属性，全部颜色联动**（见 6.8.12 的重绘/重排分析）：

```typescript
// stores/theme.ts（要点摘录）
document.documentElement.classList.add('dark-mode')        // 暗色 = 加根类
localStorage.setItem(STORAGE_KEY_PREFIX + 'theme', theme)  // 偏好持久化
window.matchMedia('(prefers-color-scheme: dark)').matches   // 跟随系统
// skins：site.ts 把后台选的皮肤写到 #selfblog 根元素的 data-skin 属性
```

**陷阱清单：`--gray-*` 不是「深色表面」**。SelfBlog 的暗色模式把灰度梯度**整体反转**：`--gray-900` 在浅色模式是 `#111111`（最深），在暗色模式变成 `#EBEBEB`（最浅）——因为它的语义是「梯度上最深的一档」，暗色模式里「最深的一档」应该是浅灰白。如果你写 `background: var(--gray-900)` 想要「深色按钮/深色代码块」，暗色模式下就会得到刺眼的亮块。正确做法是用**语义稳定**的 token：

| 需求 | 正确 token | 说明 |
|------|-----------|------|
| 永远深色的代码块 | `--code-bg` / `--code-text` / `--code-border` | 浅色 `#1d2129`、暗色 `#161616`，**两种模式都深** |
| 高对比实心按钮 | `--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg` | 浅色下深底白字，暗色下反转成浅底黑字——**语义是「最强对比」而非「最深颜色」** |
| 圆角 | `--radius-*`（`--radius-xs` 4px 到 `--radius-full` 9999px） | 圆角一律用 token，别写裸 `8px`——设计系统升级时一键全局调整 |

区分两类 token 的口诀：**梯度 token（`--gray-*`）描述「在同一色阶里的位置」，语义 token（`--code-*`/`--contrast-*`）描述「这个 UI 元素的角色」**。写组件样式时永远按角色取色，按位置取色迟早踩反转陷阱。

**CSS 变量的进阶技巧**（SelfBlog 真实用法）：`--accent-light: color-mix(in srgb, var(--accent) 10%, var(--surface-primary))`——用 `color-mix()` 从主色**派生**浅色变体，换肤时只改 `--accent` 一个值，hover 底色、边框淡色全部自动跟随；暗色模式的 `--accent-dark` 混入 `white` 而非 `black`（深底上提亮更自然）。这就是 token 体系的美感：**改一处，动全局**。

---

## 6.6 单元测试：Vitest 框架

### 6.6.1 Vitest 概述

Vitest 是 Vite 官方的测试框架，与 Vite 共享配置，提供极速的测试体验。

**Vitest 的优势：**

- **速度快**：利用 Vite 的模块系统，无需打包
- **兼容性好**：兼容 Jest API，迁移成本低
- **TypeScript 支持**：原生支持 TypeScript
- **HMR**：测试代码修改后自动重新运行

### 6.6.2 Vitest 配置

SelfBlog 在 `vite.config.ts` 中配置 Vitest：

```typescript
/// <reference types="vitest" />
import { defineConfig } from 'vite'

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{js,ts,vue}'],
      exclude: ['src/main.ts', 'src/**/*.test.ts', 'src/**/*.spec.ts']
    }
  }
})
```

**配置项详解：**

| 选项 | 说明 |
|------|------|
| `globals` | 全局注入 `describe`、`it`、`expect` 等 API |
| `environment` | 测试环境（`jsdom` 模拟浏览器 DOM） |
| `setupFiles` | 测试前执行的文件 |
| `include` | 测试文件匹配模式 |
| `coverage` | 覆盖率配置 |

### 6.6.3 测试环境：jsdom

**jsdom 是什么？**

jsdom 是一个纯 JavaScript 实现的 DOM 环境，可以在 Node.js 中模拟浏览器 DOM API。

**为什么需要 jsdom？**

Vue 组件需要 DOM 环境才能渲染和测试。在 Node.js 中没有 `document`、`window` 等浏览器 API，jsdom 提供了这些 API 的模拟实现。

**jsdom 的局限性：**

- 不支持布局（`getBoundingClientRect` 返回 0）
- 不支持 `window.matchMedia`（需要手动 mock）
- 不支持 `IntersectionObserver`（需要手动 mock）

### 6.6.4 测试 setup 文件

SelfBlog 的 `src/test/setup.ts`：

```typescript
import '@testing-library/jest-dom'
import { config } from '@vue/test-utils'
import { vi } from 'vitest'

// 全局测试配置
config.global.stubs = {}
config.global.mocks = {
  $route: { path: '/', params: {}, query: {} },
  $router: { push: vi.fn(), replace: vi.fn() }
}

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
})
```

**setup 文件的作用：**

1. **导入 jest-dom 匹配器**：提供 `toBeInTheDocument()`、`toHaveTextContent()` 等匹配器
2. **配置全局 mocks**：为所有测试提供 `$route` 和 `$router` 的 mock
3. **Mock 浏览器 API**：提供 `window.matchMedia` 的 mock 实现

#### jsdom 陷阱清单（SelfBlog 实测踩过的每一个）

jsdom 不是浏览器。以下差异每一个都让 SelfBlog 的测试「第一次跑就挂」，全部有成熟解法：

**陷阱一：setup 只桩了 matchMedia 和 $router/$route，没有自动卸载组件。**

凡挂载时会注册全局监听器（window scroll / resize / matchMedia change）的组件，测试之间会互相污染——上一个测试注册的监听器还活着，下一个测试里触发 scroll 事件，幽灵监听器就执行了。解法是**每个测试文件显式开启自动卸载**：

```typescript
import { enableAutoUnmount, mount } from '@vue/test-utils'
import { afterEach } from 'vitest'

enableAutoUnmount(afterEach)   // 每个 afterEach 自动销毁所有已挂载组件
```

**陷阱二：逐帧行为（requestAnimationFrame）不能桩成同步调用。**

BackToTop 这类「滚动渐显」组件常用 rAF 节流。若把 rAF mock 成 `(cb) => cb()`（同步执行），依赖「下一帧才更新」的逻辑全部错乱。正确姿势是**排队 + 手动 flush**：

```typescript
const rafQueue: FrameRequestCallback[] = []
vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
  rafQueue.push(cb)
  return rafQueue.length
})
// 需要推进一帧时：
rafQueue.forEach((cb) => cb(performance.now()))
rafQueue.length = 0
```

**陷阱三：改 scrollY 不能直接赋值。**

`window.scrollY = 500` 在 jsdom 里要么无效要么报错（只读 getter）。必须用 `Object.defineProperty` 覆盖，再手动派发事件：

```typescript
Object.defineProperty(window, 'scrollY', { value: 800, writable: true, configurable: true })
window.dispatchEvent(new Event('scroll'))
await wrapper.vm.$nextTick()   // Vue 的 DOM 更新是异步的，必须 nextTick
```

**陷阱四：不要 stub RouterLink。**

`stubs: { RouterLink: true }` 图省事，但 stub 会丢掉 RouterLink 的 slot 内容，也表达不了 active 态（`router-link-active` class）。SelfBlog 的约定：**路由测试一律用真实 router**：

```typescript
import { createRouter, createMemoryHistory } from 'vue-router'

const router = createRouter({
  history: createMemoryHistory(),   // 内存路由：不碰地址栏，测试专用
  routes,
})
const wrapper = mount(Component, { global: { plugins: [router] } })
```

**陷阱五：Pinia 测试要手动隔离。**

每个测试用 `setActivePinia(createPinia())` 建全新实例，否则 store 状态在测试间泄漏（上个测试改的 token 这个测试还能看到）：

```typescript
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach } from 'vitest'

beforeEach(() => {
  setActivePinia(createPinia())
})
```

### 6.6.5 组件测试基础

**安装依赖：**

```bash
npm install -D vitest @vue/test-utils @testing-library/vue jsdom
```

**基本测试示例：**

```typescript
// src/components/Button.test.ts
import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import Button from './Button.vue'

describe('Button', () => {
  it('renders correctly', () => {
    const wrapper = mount(Button, {
      props: {
        label: 'Click me'
      }
    })

    expect(wrapper.text()).toContain('Click me')
  })

  it('emits click event', async () => {
    const wrapper = mount(Button, {
      props: {
        label: 'Click me'
      }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
```

**@vue/test-utils 核心 API：**

| API | 说明 |
|-----|------|
| `mount` | 完整挂载组件（包括子组件） |
| `shallowMount` | 浅挂载（子组件被 stub） |
| `wrapper.text()` | 获取组件文本内容 |
| `wrapper.html()` | 获取组件 HTML |
| `wrapper.find(selector)` | 查找元素 |
| `wrapper.findAll(selector)` | 查找所有匹配元素 |
| `wrapper.props()` | 获取 props |
| `wrapper.emitted()` | 获取触发的事件 |
| `wrapper.trigger(event)` | 触发事件 |
| `wrapper.setValue(value)` | 设置表单值 |

### 6.6.6 Mock 技术

**Mock 函数：**

```typescript
import { vi } from 'vitest'

// 创建 mock 函数
const mockFn = vi.fn()

// 设置返回值
mockFn.mockReturnValue('mocked value')

// 设置实现
mockFn.mockImplementation((arg: string) => arg.toUpperCase())

// 断言调用
expect(mockFn).toHaveBeenCalled()
expect(mockFn).toHaveBeenCalledWith('arg')
expect(mockFn).toHaveBeenCalledTimes(1)
```

**Mock 模块：**

```typescript
import { vi } from 'vitest'

// Mock 整个模块
vi.mock('@/api/index', () => ({
  api: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: vi.fn().mockResolvedValue({ data: {} })
  }
}))

// 在测试中使用
import { api } from '@/api/index'

it('should call api', async () => {
  await api.get('/posts')
  expect(api.get).toHaveBeenCalledWith('/posts')
})
```

**Mock 路由：**

```typescript
import { mount } from '@vue/test-utils'
import { vi } from 'vitest'

const wrapper = mount(Component, {
  global: {
    mocks: {
      $route: { path: '/posts', params: { id: '1' }, query: {} },
      $router: { push: vi.fn(), replace: vi.fn() }
    }
  }
})
```

### 6.6.7 覆盖率报告

**生成覆盖率报告：**

```bash
npm run test:coverage
# 等价于：vitest run --coverage
```

**覆盖率配置：**

```typescript
coverage: {
  provider: 'v8',           // 使用 V8 引擎的覆盖率
  reporter: ['text', 'json', 'html'],  // 报告格式
  include: ['src/**/*.{js,ts,vue}'],    // 包含的文件
  exclude: ['src/main.ts', 'src/**/*.test.ts', 'src/**/*.spec.ts']  // 排除的文件
}
```

**覆盖率指标：**

| 指标 | 说明 |
|------|------|
| Statements | 语句覆盖率 |
| Branches | 分支覆盖率 |
| Functions | 函数覆盖率 |
| Lines | 行覆盖率 |

**查看报告：**

```bash
# 文本报告（终端输出）
npm run test:coverage

# HTML 报告（浏览器打开）
open coverage/index.html
```

### 6.6.8 测试最佳实践

**测试命名：**

```typescript
// 推荐：描述行为
it('should display error message when API fails', () => { })

// 不推荐：描述实现
it('test api error', () => { })
```

**AAA 模式（Arrange-Act-Assert）：**

```typescript
it('should add item to list', () => {
  // Arrange（准备）
  const wrapper = mount(Component)
  const input = wrapper.find('input')

  // Act（执行）
  await input.setValue('new item')
  await wrapper.find('button').trigger('click')

  // Assert（断言）
  expect(wrapper.text()).toContain('new item')
})
```

**避免测试实现细节：**

```typescript
// 不推荐：测试内部状态
expect(wrapper.vm.internalState).toBe('value')

// 推荐：测试用户可见的行为
expect(wrapper.text()).toContain('expected text')
```

---

## 6.7 部署与 CI/CD

### 6.7.1 GitHub Actions 基础

GitHub Actions 是 GitHub 提供的 CI/CD 服务，通过 YAML 文件定义工作流。

**工作流结构：**

```yaml
name: CI                    # 工作流名称
on:                        # 触发条件
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:       # 手动触发

jobs:                      # 任务
  job1:
    runs-on: ubuntu-latest   # 运行环境
    steps:                 # 步骤
      - uses: actions/checkout@v4
      - run: echo "Hello"
```

### 6.7.2 SelfBlog CI 工作流解析

SelfBlog 的 `.github/workflows/ci.yml` 包含四个任务：

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  backend:
    name: Backend tests (H2)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Run backend tests
        working-directory: backend
        run: ./mvnw -B -ntp test -Dskip.npm=true -Dskip.installnodenpm=true

  frontend:
    name: Frontend checks
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '24.15.0'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint:check
      - run: npm run format:check
      - run: npm run type-check
      - run: npm run test:run
      - run: npm run test:deployment
      - run: npm run build

  mysql:
    name: Backend tests (MySQL 8)
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: campus_blog
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h 127.0.0.1 -u root --password=root"
          --health-interval=10s
          --health-timeout=5s
          --health-retries:12
    env:
      DB_TYPE: mysql
      DB_HOST: 127.0.0.1
      DB_PORT: '3306'
      DB_NAME: campus_blog
      DB_USERNAME: root
      DB_PASSWORD: root
      SPRING_SQL_INIT_MODE: never
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Initialize MySQL with the fresh-install script
        run: mysql --protocol=tcp -h 127.0.0.1 -P 3306 -u root -proot < backend/schema-fresh-install.sql
      - name: Run backend tests against MySQL
        working-directory: backend
        run: ./mvnw -B -ntp test -Dskip.npm=true -Dskip.installnodenpm=true

  docker:
    name: Docker image smoke test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - name: Build image (repository-root context, like Compose uses)
        uses: docker/build-push-action@v6
        with:
          context: .
          file: backend/Dockerfile
          load: true
          push: false
          tags: selfblog:ci
          cache-from: type=gha
          cache-to: type=gha,mode=max
      - name: Start container and wait for the real app
        run: |
          docker run -d --name selfblog-ci -p 8825:8825 \
            -e DB_TYPE=sqlite \
            -e JWT_SECRET=ci-only-jwt-secret-0123456789abcdefghijklmn \
            -e JWT_EXPIRATION=86400000 \
            -e JWT_REFRESH_EXPIRATION=604800000 \
            -e ADMIN_PASSWORD=ci-only-admin-password \
            selfblog:ci
          ready=0
          for attempt in $(seq 1 60); do
            if curl -fsS http://127.0.0.1:8825/actuator/health > /dev/null; then
              echo "healthy after ${attempt} attempt(s)"
              ready=1
              break
            fi
            if [ "$(docker inspect -f '{{.State.Running}}' selfblog-ci 2>/dev/null)" != "true" ]; then
              echo "container exited early"
              docker logs selfblog-ci
              exit 1
            fi
            sleep 5
          done
          if [ "$ready" != "1" ]; then
            echo "container never became healthy"
            docker logs selfblog-ci
            exit 1
          fi
          curl -fsS http://127.0.0.1:8825/actuator/health
          curl -fsS http://127.0.0.1:8825/ | grep -q 'seo-reading'
      - name: Container logs on failure
        if: failure()
        run: docker logs selfblog-ci || true
      - name: Cleanup
        if: always()
        run: docker rm -f selfblog-ci || true
```

**工作流详解：**

**1. 触发条件**

```yaml
on:
  push:
    branches: [main]      # 推送到 main 分支
  pull_request:           # 拉取请求
  workflow_dispatch:      # 手动触发
```

**2. 并发控制**

```yaml
concurrency:
  group: ci-${{ github.workspace }}-${{ github.ref }}
  cancel-in-progress: true
```

同一分支的多个工作流运行时，取消之前的运行，只保留最新的。

**3. 前端任务**

```yaml
frontend:
  name: Frontend checks
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: frontend
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: '24.15.0'
        cache: npm
        cache-dependency-path: frontend/package-lock.json
    - run: npm ci
    - run: npm run lint:check
    - run: npm run format:check
    - run: npm run type-check
    - run: npm run test:run
    - run: npm run test:deployment
    - run: npm run build
```

**前端 CI 流程：**

1. 检出代码
2. 设置 Node.js 24.15.0（带缓存）
3. 安装依赖（`npm ci`）
4. ESLint 检查
5. Prettier 检查
6. TypeScript 类型检查
7. 单元测试
8. 部署契约测试
9. 生产构建

**4. MySQL 服务**

```yaml
services:
  mysql:
    image: mysql:8.0
    env:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: campus_blog
    ports:
      - 3306:3306
    options: >-
      --health-cmd="mysqladmin ping -h 127.0.0.1 -u root --password=root"
      --health-interval=10s
      --health-timeout=5s
      --health-retries:12
```

GitHub Actions 的 `services` 允许在任务中启动辅助容器（如数据库）。

**5. Docker 任务**

```yaml
docker:
  name: Docker image smoke test
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: docker/setup-buildx-action@v3
    - name: Build image
      uses: docker/build-push-action@v6
      with:
        context: .
        file: backend/Dockerfile
        load: true
        push: false
        tags: selfblog:ci
```

**Docker 构建流程：**

1. 检出代码
2. 设置 Buildx
3. 构建镜像（不推送）
4. 启动容器
5. 等待健康检查
6. 验证服务端渲染 HTML

### 6.7.3 Docker 部署

**Dockerfile 示例：**

```dockerfile
# 多阶段构建
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**docker-compose.yml 示例：**

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: backend/Dockerfile
    ports:
      - "8825:8825"
    environment:
      - DB_TYPE=sqlite
      - JWT_SECRET=${JWT_SECRET}
      - ADMIN_PASSWORD=${ADMIN_PASSWORD}
    volumes:
      - ./data:/app/data
      - ./uploads:/app/uploads
```

### 6.7.4 Vercel 代理部署

SelfBlog 的 Vercel 配置是一个**代理生成器**，而不是静态前端部署：

```json
{
  "framework": null,
  "buildCommand": "node scripts/build-vercel-proxy.mjs",
  "installCommand": "node -e \"console.log('Proxy deployment needs only Node built-ins')\""
}
```

**为什么使用代理模式？**

SelfBlog 的生产部署是后端单 JAR 内嵌前端，Vercel 只作为反向代理，不在平台侧再构建一份前端。

**构建脚本（build-vercel-proxy.mjs）：**

```javascript
import { fileURLToPath } from 'node:url'
import { writeProxyOutput } from './vercel-proxy.mjs'

try {
  await writeProxyOutput(process.env, fileURLToPath(new URL('../.vercel/output/', import.meta.url)))
  console.log('Vercel proxy routing generated; HTML and assets remain owned by the source JAR.')
} catch (error) {
  console.error(error instanceof Error ? error.message : 'Vercel proxy configuration failed')
  process.exitCode = 1
}
```

**环境变量：**

| 变量 | 说明 |
|------|------|
| `SELFBLOG_BACKEND_ORIGIN` | 源站 HTTPS origin |
| `SITE_BASE_URL` | 正式访客 origin |

**部署步骤：**

1. Root Directory 设为 `frontend`
2. Framework Preset 选 **Other**
3. 配置环境变量
4. Build Command 使用 `node scripts/build-vercel-proxy.mjs`

**Build Output v3 速读**：`build-vercel-proxy.mjs` 生成的 `.vercel/output/` 是 Vercel 的开放部署格式——`config.json` 声明路由与变量，`static/` 放静态文件。对纯代理场景，关键只有一条：**把所有路径（`/api/*`、`/uploads/*`、页面路由）全部 rewrite 转发到 `SELFBLOG_BACKEND_ORIGIN` 这一个 origin**——产物校验脚本会拒绝空 origin、非法 URL 与自引用 origin（指向 Vercel 自己会造成无限回环），这正是 `npm run test:deployment` 契约测试存在的意义：部署配置也是代码，也要有测试。`framework: null`（Preset 选 Other）的含义是**关掉 Vercel 对前端的自动构建猜测**——不识别框架、不跑 `npm run build`、不上传静态前端，一切由我们的生成器说了算。

**Nginx 配置示例（自托管静态 + 反代的对照）**——如果不走 Vercel，同样的「静态托管 + API 反代」拓扑用 Nginx 实现：

```nginx
server {
    listen 80;
    server_name blog.example.com;

    # SPA 静态资源：带哈希的产物强缓存一年（对应 6.8.12 缓存策略）
    location /assets/ {
        root /var/www/selfblog/dist;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 入口 HTML：协商缓存，绝不强缓存
    location = /index.html {
        root /var/www/selfblog/dist;
        add_header Cache-Control "no-cache";
    }

    # SPA 路由回退：找不到的路径回 index.html，交给 Vue Router
    location / {
        root /var/www/selfblog/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 与上传文件反代到后端（同源拓扑，免 CORS）
    location /api/     { proxy_pass http://127.0.0.1:8825; }
    location /uploads/ { proxy_pass http://127.0.0.1:8825; }

    # Gzip：文本类资源压缩后再传
    gzip on;
    gzip_types text/css application/javascript application/json image/svg+xml;
}
```

三个配置要点：`try_files $uri $uri/ /index.html` 是 SPA 部署的签名行（没有它，刷新 `/post/42` 直接 404）；`immutable` 告诉浏览器「这一年里文件名带哈希、内容永不变」，配合协商缓存的 HTML 才是完整闭环；`/uploads/` 也要反代——用户上传的图片存在后端卷里，不在前端静态目录。

### 6.7.5 部署契约测试

SelfBlog 使用 `node:test` 进行部署契约测试：

```bash
npm run test:deployment
# 等价于：node --test scripts/__tests__/*.test.mjs
```

**测试内容：**

- Vercel 代理生成器输出验证
- Docker/Compose 源契约验证
- 环境变量验证

### 6.7.6 前端工程的「五道闸门」：为什么全都要绿

SelfBlog 前端定义了五条必须全绿的闸门，这是现代前端工程的标准化质量体系，值得逐个理解「它在防什么」：

| 闸门 | 命令 | 防住的事故 |
|------|------|-----------|
| Lint | `npm run lint:check` | 未使用变量、错误 hooks 依赖数组、危险 API（防「代码能跑但埋雷」） |
| Format | `npm run format:check` | 格式不一致（防 diff 噪音淹没真实改动、防代码审查浪费时间在空格上） |
| Type-check | `npm run type-check` | 类型错误（防 `undefined` 上取属性这种运行时崩溃） |
| Unit tests | `npm run test:run` | 逻辑回归（防「改 A 坏 B」） |
| Build | `npm run build` | vue-tsc + vite 全量编译（防「本地 dev 正常、构建才炸」的增量编译漏洞） |

**为什么 lint 和 type-check 不能互相替代**：ESLint 管不了类型（`str.foo` 这种拼写错误它不认识），vue-tsc 管不了风格与逻辑陷阱（条件分支漏 return 它不报）。**两者正交，缺一不可。**

**本地与 CI 的关系**：CI 只是把本地闸门原样跑一遍。养成 `git push` 前本地先全绿的肌肉记忆，比「推上去看 CI 脸色」高效得多——CI 排队一轮常常 5 分钟起步，本地跑全套不到 1 分钟。

**format:check 的一个真实演化**：SelfBlog 的 `.prettierrc` 曾因 `endOfLine` 默认值跨平台漂移（Windows CRLF vs Linux LF）导致 CI 偶发红。2026-10-17 改为 `endOfLine: "auto"` 并一次性重写历史漂移后，该闸门才真正稳定。**教训：格式规则必须跨平台确定性，否则闸门会变成随机红绿灯。**

---

## 6.8 项目实战：SelfBlog 工程化全景

### 6.8.1 项目架构总览

```
SelfBlog/
├── backend/                 # Spring Boot 3.3 + Java 21
│   ├── src/main/java/       # 后端源码
│   ├── src/main/resources/  # 配置文件
│   ├── src/test/            # 测试代码
│   ├── Dockerfile           # Docker 镜像
│   └── pom.xml              # Maven 配置
├── frontend/                # Vue 3.5 + Vite 6
│   ├── src/                 # 前端源码
│   │   ├── api/             # API 接口
│   │   ├── components/      # 组件
│   │   ├── composables/     # 组合式函数
│   │   ├── router/          # 路由
│   │   ├── stores/          # Pinia 状态
│   │   ├── styles/          # 样式
│   │   ├── types/           # 类型定义
│   │   ├── utils/           # 工具函数
│   │   └── views/           # 页面组件
│   ├── scripts/             # 构建脚本
│   ├── public/              # 静态资源
│   ├── vite.config.ts       # Vite 配置
│   ├── tsconfig.json        # TypeScript 配置
│   ├── eslint.config.js     # ESLint 配置
│   ├── .prettierrc          # Prettier 配置
│   ├── vercel.json          # Vercel 配置
│   └── package.json         # 依赖配置
├── .github/workflows/       # GitHub Actions
│   └── ci.yml               # CI 工作流
└── docs/                    # 文档
```

### 6.8.2 开发工作流

**本地开发流程：**

```bash
# 1. 启动后端（端口 8825）
cd backend
./mvnw spring-boot:run

# 2. 启动前端（端口 3000）
cd frontend
npm run dev

# 3. 访问 http://localhost:3000
```

**代码检查流程：**

```bash
# 1. 类型检查
npm run type-check

# 2. ESLint 检查
npm run lint:check

# 3. Prettier 检查
npm run format:check

# 4. 自动修复
npm run lint
npm run format

# 5. 运行测试
npm run test:run

# 6. 生产构建
npm run build
```

### 6.8.3 质量闸门

SelfBlog 定义了严格的质量闸门：

| 闸门 | 命令 | 说明 |
|------|------|------|
| 类型检查 | `npm run type-check` | TypeScript 类型必须正确 |
| 代码检查 | `npm run lint:check` | ESLint 规则必须通过 |
| 格式检查 | `npm run format:check` | Prettier 格式必须一致 |
| 单元测试 | `npm run test:run` | 所有测试必须通过 |
| 部署测试 | `npm run test:deployment` | 部署契约必须满足 |
| 生产构建 | `npm run build` | 构建必须成功 |

**CI 执行顺序：**

```
npm ci
  ↓
npm run lint:check
  ↓
npm run format:check
  ↓
npm run type-check
  ↓
npm run test:run
  ↓
npm run test:deployment
  ↓
npm run build
```

### 6.8.4 依赖管理策略

**生产依赖（dependencies）：**

```json
{
  "dependencies": {
    "axios": "^1.7.4",
    "dompurify": "^3.0.9",
    "marked": "^12.0.1",
    "pinia": "^2.1.7",
    "vue": "^3.5.44",
    "vue-router": "^4.3.0"
  }
}
```

**开发依赖（devDependencies）：**

```json
{
  "devDependencies": {
    "@eslint/js": "^9.39.4",
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/vue": "^8.1.0",
    "@vitejs/plugin-vue": "^5.0.4",
    "@vue/test-utils": "^2.4.10",
    "@vue/tsconfig": "^0.9.1",
    "eslint": "^9.39.4",
    "eslint-config-prettier": "^10.1.8",
    "eslint-plugin-vue": "^10.9.1",
    "jsdom": "^29.1.1",
    "prettier": "^3.8.3",
    "typescript": "^6.0.3",
    "typescript-eslint": "^8.60.1",
    "vite": "^6.4.4",
    "vitest": "^4.1.8",
    "vue-tsc": "^3.3.3"
  }
}
```

**依赖分类原则：**

| 类别 | 说明 | 示例 |
|------|------|------|
| 生产依赖 | 运行时需要的库 | vue, vue-router, pinia, axios |
| 开发依赖 | 开发/构建/测试工具 | vite, eslint, prettier, vitest |

### 6.8.5 环境配置

**开发环境（.env）：**

```env
VITE_APP_TITLE=个人博客
VITE_API_TARGET=http://localhost:8825
VITE_API_BASE_URL=/api
VITE_APP_VERSION=2.0.25
```

**生产环境：**

生产环境的前端构建后内嵌到后端 JAR 中，不需要单独的环境变量配置。

### 6.8.6 构建产物分析

**构建输出结构：**

```
dist/
├── index.html              # 入口 HTML
├── assets/
│   ├── index-[hash].js     # 业务代码
│   ├── vendor-[hash].js    # Vue 生态
│   ├── markdown-[hash].js  # Markdown 渲染
│   └── index-[hash].css    # 样式
└── favicon.ico             # 站点图标
```

**构建优化：**

1. **代码分割**：第三方库独立 chunk
2. **Tree-shaking**：移除未使用的代码
3. **压缩**：esbuild 压缩 JS/CSS
4. **哈希**：文件名包含内容哈希，支持长期缓存

### 6.8.7 错误处理与监控

**前端错误处理：**

```typescript
// src/api/index.ts
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 响应拦截器
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 统一错误处理
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，跳转登录
          break
        case 403:
          // 禁止访问
          break
        case 404:
          // 资源不存在
          break
        case 500:
          // 服务器错误
          break
      }
    }
    return Promise.reject(error)
  }
)
```

### 6.8.8 性能优化入门：代码分割与请求缓存

> 本节是通用入门；SelfBlog 的完整性能清单（构建产物/运行时/网络/测量四层）见 6.8.10。

**代码分割：**

```typescript
// 路由懒加载
const routes = [
  {
    path: '/',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/about',
    component: () => import('@/views/About.vue')
  }
]
```

**图片优化：**

```vue
<template>
  <!-- 使用 WebP 格式 -->
  <img src="@/assets/image.webp" alt="description" loading="lazy">
</template>
```

**缓存策略：**

```typescript
// 使用浏览器缓存
const cache = new Map()

async function fetchData(key: string) {
  if (cache.has(key)) {
    return cache.get(key)
  }
  const data = await api.get(key)
  cache.set(key, data)
  return data
}
```

### 6.8.9 部署形态：Vercel 代理生成器与 Docker（SelfBlog 实战）

SelfBlog 的部署有一个非常容易误解的设计：**`frontend/vercel.json` 不是静态站点配置，而是一个反向代理生成器**。

```json
{
  "framework": null,
  "buildCommand": "node scripts/build-vercel-proxy.mjs",
  "routes": "（由脚本生成 Build Output v3 格式的转发规则）"
}
```

- `framework: null`：明确告诉 Vercel「别猜，我不需要你注入默认构建」
- `scripts/build-vercel-proxy.mjs`：构建期生成 Build Output v3 的 forwarding 规则，把 `/api/*` 与 `/uploads/*` 转发到唯一后端 origin；**无效或自引用的 origin 直接让部署失败**（fail fast 优于上线后 404）
- 需要两个环境变量：`SELFBLOG_BACKEND_ORIGIN`（后端地址）与 `SITE_BASE_URL`（站点规范域名）

为什么这么做？前后端分离部署时，前端域名与后端 API 域名不同源会有 CORS 问题。统一反代理后浏览器看到的一切请求都同源——**用部署拓扑消灭 CORS，而不是在代码里到处配 CORS 头**。这套适配器有专门的契约测试：`npm run test:deployment`（27 项断言，同时覆盖 Vercel 代理生成器与 Docker/Compose 契约）。

**Docker 部署则相反**：单容器包含前后端——`backend/Dockerfile` 以仓库根为构建上下文，把已构建的 `selfblog.jar`（内嵌前端产物）与数据卷、上传卷、相册卷一起编排，`docker compose up -d --build` 一条命令起全栈，健康检查靠 `/actuator/health`。

> 诚实声明（SelfBlog 文档的惯例）：真实 Vercel 部署与搜索引擎收录从未真机验证过，`vercel.json` 的正确性由契约测试背书；Docker 镜像在无 Docker 守护进程的本机也未验证过。**验证记录与代码同等重要——「我没测过」必须写进文档**，这是工程诚信的一部分。

### 6.8.10 前端性能优化：SelfBlog 的务实清单

性能优化最忌「上来就背清单」。正确的姿势：**先测量，再优化，优化完再测量**。SelfBlog 的做法展示了单人博客场景下「够用且有效」的取舍。

**① 构建产物层面（一次性投入，长期收益）**

```
构建策略                SelfBlog 的做法                     效果
路由懒加载              每个页面 () => import('...')        首屏只加载访问到的 chunk
Tree-shaking            全 ESM 依赖（Vite 生产走 Rollup）    未引用代码不进产物
依赖瘦身                审查 package.json，能原生就原生      少一个依赖少一份体积
生产构建压缩            Vite 默认 esbuild 压缩              开箱即得
```

验证手段：`npm run build` 后看产物清单里每个 chunk 的体积，突然变大的 chunk 要问为什么。**警惕**：某个组件 import 了一个全家桶库（如只用了 `lodash` 一个函数却整包引入），改 `import debounce from 'lodash/debounce'` 就能瘦身。

**② 运行时层面（响应性纪律）**

- **computed 缓存派生值**：模板里写 `{{ filteredList.length }}` 每次 render 都重算；提到 `computed` 里只依赖变化时重算
- **v-for 必带稳定 key**：用业务 id 而非数组下标——下标 key 在列表头部插入时会让 Vue 复错节点
- **大列表虚拟化**：博客文章归档页几百条尚可直渲染；上千条上虚拟列表（SelfBlog 未用，规模不需要——诚实评估，不为炫技上方案）
- **事件防抖**：搜索框输入触发请求用 `debounce(300)`；SelfBlog 的滚动监听用 `passive: true` 的原生监听而不是 Vue 响应式
- **组件级缓存**：路由出口 `<KeepAlive>` 包住归档页这类「进出频繁、重渲染昂贵」的页面

**③ 网络层面（利用后端已有的设计）**

- **HTTP 缓存**：静态资源 Vite 产物带 hash 文件名 → `Cache-Control: max-age=31536000, immutable`；`index.html` 不缓存 → 版本更新立即生效
- **接口聚合**：页面需要的多个数据尽量一个接口返回（后端 `Result` 信封天然支持多数据块），减少请求往返
- **缓存兜底**：`stores/site.ts` 的默认值设计让「后端挂了页面照样渲染」——这同时是可用性与「感知性能」的双保险

**④ 测量工具（没有测量就没有优化）**

```bash
npm run build          # 看 chunk 体积分布
# Chrome DevTools:
#   Lighthouse 面板    → 性能评分 + LCP/FCP/CLS 三大指标
#   Performance 面板   → 录制交互，看长任务（>50ms 的红色块）
#   Network 面板       → 按体积排序，找异常大的资源
```

**单人博客的边界**：不追求 100 分 Lighthouse，追求「文章秒开、交互不卡」。SelfBlog 把主要精力花在 SEO 可读 HTML（对爬虫与低网速用户的意义远大于锦上添花的动画）——**性能优化的第一原则：优化对用户可感知的部分**。

### 6.8.11 前端安全实战：XSS、令牌存储与游客身份

安全不是后端一个人的事。前端是所有用户输入的**第一现场**，也是令牌的**保管员**。

**① XSS：前端防线的三层**

XSS（跨站脚本）的本质：攻击者的字符串被浏览器当成了代码。三层防线各司其职：

```typescript
// 第一层：框架默认转义——Vue 模板插值自动 HTML 转义
// {{ user.nickname }} 即使内容是 <script>... 也只是文本，不会执行
// 危险点只有一个：v-html——它绕过转义，把字符串当 HTML 插入
<div v-html="renderedMarkdown"></div>   // 只允许「可信来源+已消毒」的内容走这里
```

```typescript
// 第二层：入库前消毒是后端的事（SelfBlog 的 htmlSanitizer.sanitizePlainText），
// 但前端也要防御「来自后端的数据」被二次污染——比如评论内容里带 <img onerror>
// 规则：凡是要 v-html 的内容，必须经过 DOMPurify 这类库清洗
import DOMPurify from 'dompurify'
const safe = DOMPurify.sanitize(rawHtml)
```

```typescript
// 第三层：CSP（内容安全策略）——即使前两层失守，浏览器拒绝执行未授权来源的脚本
// 由后端响应头下发：Content-Security-Policy: script-src 'self'
```

SelfBlog 的分工：**后端消毒入库（GuestCommentServiceImpl 先 sanitize 再校验）→ 前端默认插值渲染 → SEO 可读 HTML 由 SafeMarkdown 白名单渲染**。三层各挡一次。

**② 令牌存储：localStorage vs cookie 的诚实权衡**

| 方案 | XSS 可读 | CSRF 风险 | SelfBlog 的选择 |
|------|----------|-----------|-----------------|
| localStorage | ✗ 可被读走 | 无（不自动携带） | ✅ 采用 + 双重防御 |
| HttpOnly cookie | ✓ 读不到 | 有（自动携带，需 CSRF 防御） | 后端备有 XSRF-TOKEN 机制 |

localStorage 怕 XSS，cookie 怕 CSRF——没有完美方案，只有**纵深防御**：SelfBlog 用 localStorage + Axios 自动附加 `X-XSRF-TOKEN` 头（cookie 里的 XSRF token 由后端校验），同时用严格的 XSS 三层防线压低「token 被读走」的概率。**安全是概率工程，不是非黑即白。**

**②的展开：CSRF「双提交」（Double Submit）到底防住了什么**。先看攻击面：CSRF 之所以存在，是因为**浏览器在任何跨站请求里都会自动携带目标域的 cookie**——恶意网站放一个 `<form action="https://blog.com/api/admin/post/delete" method="POST">` 再用脚本提交，受害者的浏览器就带着合法 cookie 发出了删除请求，网站无法区分「这是恶意站点代发的」。防御思路因此分两派：让请求「自带不可伪造的凭证」。

**双提交模式**正是后者的经典实现——同一个随机 token 存两份：一份在 **cookie**（`XSRF-TOKEN`），一份由前端 JS 读出后放进**请求头**（`X-XSRF-TOKEN`，Axios 对 cookie 名 `XSRF-TOKEN` 自动这么干）。后端只收「头与 cookie 值相等」的请求。它为什么有效：**跨站攻击者能强迫浏览器带上 cookie（这是浏览器行为，拦不住），却读不到 cookie**（同源策略禁止跨域 JS 读取他域文档/存储），自然也放不进自定义头——两个值对不上，请求被拒。

```text
正常用户：cookie XSRF-TOKEN=abc  +  头 X-XSRF-TOKEN=abc（JS 从 cookie 读出再放头里）→ 匹配 ✅
攻击者：  cookie XSRF-TOKEN=abc  +  无头（读不到 cookie，放不了头）        → 401/403 ❌
```

SameSite cookie 属性（`SameSite=Lax/Strict`）是补充防线：让浏览器干脆不给跨站请求带 cookie。二者叠加后 CSRF 在现代浏览器里已很难得手。

**CSP 概念展开**：`Content-Security-Policy` 是服务器下发给浏览器的「白名单清单」，核心指令如 `default-src 'self'; script-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'`——分别是脚本/图片/样式允许的来源。它对 XSS 的意义是**兜底**：即使前两层失守、恶意 `<script>` 真被插进页面，浏览器发现来源不在白名单也会拒绝执行。Vue SPA 的常见痛点是 `'unsafe-inline'`（Vue 的运行时注入与内联样式需要它，削弱了 CSP 强度），更严的做法是 nonce/hash 方案。SelfBlog 的 CSP 由后端响应头下发，前端只需理解：**CSP 报错（Console 里的 `Refused to …`）不是 bug，是防线在工作**。

**③ token 的生命周期管理（stores/user.ts 的三个细节）**

- **存取经过编码**：token 经 `utils/crypto` 编解码后进 localStorage——不是加密，是防「一眼明文」的最低限
- **本地校验优先**：`validateToken` 只解 JWT 的 exp 判断过期，不联网——避免每次路由跳转都打后端
- **失效即清理**：拦截器 `handleAuthError()` 清 Pinia + localStorage 并跳 `adminRoot`，绝不让半死状态残留

**④ 游客身份：X-Visitor-Id 的设计**

匿名点赞/评论需要「不登录也能识别同一个人」。SelfBlog 的方案：

```typescript
// utils/visitor.ts 的思想：
function getOrCreateVisitorId(): string {
  let id = localStorage.getItem('visitor_id')
  if (!id) {
    id = crypto.randomUUID()          // 浏览器原生 UUID，无需后端参与
    localStorage.setItem('visitor_id', id)
  }
  return id
}
// Axios 请求拦截器给公开写接口统一附加 X-Visitor-Id 头
```

配套的**后端信任边界**（决定这套方案是否安全的关键）：visitorId 只作为「关联键」——后端校验格式白名单（`[A-Za-z0-9_-]{8,80}`）、只存加盐哈希（`VisitorHashUtils.hash(visitorId, secret)`）、权限永远与服务端校验挂钩。**客户端生成的任何身份标识都不可信，可信的只有它经过服务端约束后的形态。**

**⑤ 前端安全自查清单**

- [ ] 有没有 v-html？它的内容来源可信且消毒过吗？
- [ ] 敏感数据有没有出现在 URL / 日志 / 错误上报里？
- [ ] 登出时清干净了吗（Pinia + localStorage + 内存变量三处）？
- [ ] 路由守卫的鉴权判断只是 UX——**每个接口后端都重新校验了吗**（前端鉴权永远可绕过，它只负责体验）？
- [ ] 依赖有没有已知漏洞（`npm audit` 进 CI 是加分项）？

---

### 6.8.12 浏览器与 HTTP：前端工程的底层契约

工程化工具解决了「代码怎么组织」，但代码最终跑在浏览器里、数据走 HTTP——理解这两层底层契约，很多「玄学问题」会变成「常识问题」。

**一次页面加载的完整旅程**（SelfBlog 首页为例）：DNS 解析 → TCP/TLS 握手 → 请求 `index.html`（后端 SpaController/SEO 路由返回）→ 解析 HTML 构建 DOM → 遇到 `<script type="module">` 加载 JS（Vite 开发态按需、生产态打包）→ JS 执行到 `mount('#app')` → Vue 渲染 → Axios 发 `/api/*` 拿数据 → 二次渲染。**首屏时间的大头通常在 JS 体积与接口串联**，这解释了 6.8.10 为什么把「构建产物」列为优化第一层。

**HTTP 缓存：构建工具哈希文件名的底层依据**。浏览器缓存分两层：

- **强缓存**：响应头 `Cache-Control: max-age=31536000` 有效期内**不发请求**直接用本地副本。Vite 生产构建给每个产物文件名加内容哈希（`index-C8fH2x.js`），内容一变文件名就变——所以可以放心让 JS/CSS **强缓存一年**。
- **协商缓存**：强缓存过期后带 `ETag`/`If-None-Match` 问服务器「变了没」，没变返回 304 省流量，变了返回新内容。`index.html` 必须走协商缓存（它引用着会变的哈希文件名），否则用户拿旧 HTML 引用已删除的旧 JS 就白屏。

一句话记住 SelfBlog 式约定：**带哈希的文件强缓存，入口 HTML 协商缓存**。这也是为什么不能用「改了代码用户要清缓存」来搪塞线上问题——缓存策略写错了，才是要用户清缓存的原因。

**同源策略与 CORS：为什么开发要用代理**。浏览器规定：协议、域名、端口任一不同即**跨域**，跨域响应默认被浏览器拦下（请求其实发出去了，是响应被扣）。`http://localhost:3000`（Vite）调 `http://localhost:8825`（Spring Boot）就是跨域。解法两条：后端加 CORS 响应头，或**让请求看起来同源**——SelfBlog 两条都用了：开发态 Vite 代理把 `/api` 转发到 8825（浏览器只看到同源的 3000），生产态 `vercel.json` 反向代理同理。**用部署拓扑消灭 CORS**（6.8.9 节）不是取巧，是生产环境更安全的正统做法： cookie/token 不暴露给第三方域，也省掉一堆 `Access-Control-Allow-*` 配置。

**连接限制：HTTP/1.1 的六连接现实**。同一域名下 HTTP/1.1 最多 6 个并发 TCP 连接，第 7 个请求排队。这解释了三件事：① 6.8.10 强调雪碧图/合并请求的历史由来；② ch03 §3.16 讲 SSE 时「EventSource 占一个常驻连接，开多了挤占并发额度」的原因；③ HTTP/2 多路复用（单连接并行多请求）为什么是质变——SelfBlog 部署在反代之后，若反代与浏览器协商出 HTTP/2，前端无需改一行代码就吃到红利。

**浏览器渲染管线与 CSS token 的关系**：浏览器把 CSS 解析成 CSSOM、HTML 解析成 DOM，二者合成渲染树 → 布局（回流）→ 绘制（重绘）。**改 `width`/`top` 触发回流（贵），改颜色/透明度只触发重绘（便宜），改 `transform`/`opacity` 走合成层（最便宜）**。SelfBlog 的皮肤系统（`skins.css` 的 `#selfblog[data-skin=…]`）与暗色模式（`.dark-mode` 根类 + CSS 变量覆盖）之所以高效，正因为它们只切换**变量与类名**——浏览器只需对受影响元素重算样式，不触碰布局。6.5.7 提到的「不要拿 `--gray-*` 当深色表面」的暗色反转问题，本质就是变量在 `dark-mode.css` 里被整体重定义——理解了变量的级联机制，设计 token 才不会用错。

**存储五兄弟速查**（前端持久化的全部选项）：

| 机制 | 容量 | 生命周期 | 是否随请求自动发送 | SelfBlog 用途 |
|------|------|---------|------------------|--------------|
| localStorage | ~5MB | 永久（手动删） | 否 | token（经 `utils/crypto` 编解码）、`edu_theme` |
| sessionStorage | ~5MB | 标签页关闭 | 否 | 一次性向导状态 |
| cookie | ~4KB | 可设过期 | **每次同源请求自动带上** | 服务端会话（SelfBlog 未用） |
| IndexedDB | 大 | 永久 | 否 | 离线应用、音乐缓存（未用，够用原则） |
| 内存（Pinia） | - | 刷新即失 | 否 | 运行时状态真身，localStorage 只是「恢复现场」的备份 |

token 放 localStorage 还是 cookie 的权衡在 6.8.11 已展开：XSS 可读 localStorage vs CSRF 打 cookie，SelfBlog 用「localStorage + Bearer 头」并把防线压在输出消毒与 CSP 上。理解这张表，就能判断任何「存哪儿」的问题。

**DevTools 调试技巧与 Lighthouse 评估**——6.8.10 已给出面板速查（Performance/Network/Lighthouse），这里补三个高频排查手法：

- **Network 面板抓接口问题**：勾选 `Disable cache`（DevTools 打开期间禁用缓存）排除缓存干扰；右键列勾选 `Initiator` 看请求由哪行代码发出（定位重复请求）；status 列分颜色——灰色 304 是协商缓存生效（正常），红色 4xx/5xx 才是真错误；Slow/瓶颈排序定位慢接口后，到 Timing 标签区分「等待服务器（TTFB）」还是「下载大响应」。
- **Elements 面板调试 Vue**：Vue DevTools 浏览器插件可看组件树与 Pinia 状态；没装插件时，Elements 里选中 DOM 后控制台 `$0` 就是该元素，配合 `__VUE_DEVTOOLS_GLOBAL_HOOK__` 或直接在代码里 `console.log` store 响应式对象（Pinia store 打印出来是可展开的 Proxy）。
- **Lighthouse 读数姿势**：性能分不是唯一指标——展开指标看 **LCP**（最大内容绘制，SelfBlog 首屏通常是标题区，超过 2.5s 才算差）、**CLS**（累计布局偏移，图片/骨架屏导致的跳动，SelfBlog 用 skeleton.css 预占位控制）、**TBT**（长任务阻塞）。注意：本地跑分总是偏高，Vercel 生产环境 + 无缓存设备才是真实分数；调试 Motivation 里「Opportunities/Diagnostics」每一项都给了具体建议与预计节省。

## 6.9 版本控制与协作：Git 工程化

前六章的所有代码变化，最终都要落成一次次提交。Git 命令在附录 B.3 已列速查表，这里补「**协作层面的工程化**」——分支模型、提交信息规范与仓库卫生。

**分支模型（单人项目也有用）**。SelfBlog 是单人作者项目，最贴合的是 **GitHub Flow**：`main` 永远可部署，任何改动开短命分支 → 提 PR → CI 绿 → 合并删除。比 Git Flow（develop/release/hotfix 多分支并行）轻得多，适合独立开发者与小团队。纪律只有两条：①分支寿命越短越好（几天内合并，避免长期分叉的合并地狱）；②**CI 必须全绿才允许合并**——6.7.6 的五道闸门正是为此存在。

```bash
git switch -c feat/comment-emoji    # 创建并切换（新语法，替代 checkout -b）
# …开发、commit…
git push -u origin feat/comment-emoji
# GitHub 上开 PR，等 ci.yml 四个 job 全绿，merge，然后：
git switch main && git pull && git branch -d feat/comment-emoji
```

**提交信息规范：Conventional Commits**。格式 `<type>(<scope>): <subject>`，type 常用 `feat`（新功能）/ `fix`（修复）/ `docs`（文档）/ `style`（格式）/ `refactor`（重构）/ `test` / `chore`（构建与杂务）。好处不只是好看：CHANGELOG 自动化、语义化版本升降级可推断（`feat` → 次版本 +1，带 `!` 或 `BREAKING CHANGE` 脚注 → 主版本 +1）、`git log --oneline` 一眼读出演进史。

```bash
git commit -m "feat(post): 文章详情支持代码块高亮主题切换"
git commit -m "fix(auth): 刷新令牌并发时重复请求问题（isRefreshing 排队）"
git commit -m "docs!: 调整目录结构为两工程制"   # ! 表示 breaking change
```

**.gitignore 与仓库卫生**。原则：**生成物不入库**——`node_modules/`、`dist/`、构建缓存、`.env`（密钥！`.env.example` 才入库）、编辑器目录。SelfBlog 特别注意两处：`backend/target/`（Maven 产物）与 `frontend/dist/`；而 `backend/campus_blog.db`（SQLite 数据文件）按需决定——开发库想共享就提交，生产库绝不入库。`.gitignore` 只对未跟踪文件生效，已被跟踪的文件要 `git rm --cached <file>` 移出索引。

**.editorconfig**。跨编辑器统一缩进/换行/编码的 humble 配置，放在仓库根，VS Code / JetBrains / Vim 都认：

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 2

[*.md]
trim_trailing_whitespace = false   # Markdown 行尾双空格是换行语法
```

它与 Prettier 分工：EditorConfig 管**所有文件**的基础属性（缩进、换行符、编码），Prettier 管**代码文件**的风格细节——两层配合后，`endOfLine: "auto"` 在 Windows 上才真正稳定（文件层面 LF 已被统一，Prettier 只需按现存风格跟随）。

**单人项目的提交纪律**：没有队友不等于没有规范——三个月后的你就是队友。每次提交聚焦一件事（一个 `feat` 或一个 `fix`），提交前跑一遍五道闸门（6.7.6），绝不提交 `console.log` 调试残留与注释掉的死代码（要删就真删，Git 历史会替你记住）。回滚靠 `git revert`（生成反向提交，历史完整可追溯）而不是 `reset --force` 推公共分支——单人项目一旦开了 Actions，`main` 就是公共分支。

**六个 6.9.x 小节**最后来一次总收束——

### 6.9.1 工程化清单

**项目初始化：**

- [ ] 选择合适的 Node.js 版本
- [ ] 初始化 `package.json`
- [ ] 配置 `.gitignore`
- [ ] 配置 `.editorconfig`

**开发环境：**

- [ ] 配置 Vite
- [ ] 配置 TypeScript
- [ ] 配置 ESLint
- [ ] 配置 Prettier
- [ ] 配置路径别名

**代码质量：**

- [ ] 启用 TypeScript 严格模式
- [ ] 配置 ESLint 规则
- [ ] 配置 Prettier 规则
- [ ] 配置编辑器自动修复

**测试：**

- [ ] 配置 Vitest
- [ ] 编写单元测试
- [ ] 配置覆盖率报告
- [ ] 设置测试阈值

**CI/CD：**

- [ ] 配置 GitHub Actions
- [ ] 设置质量闸门
- [ ] 配置自动部署
- [ ] 配置监控告警

### 6.9.2 常见问题与解决方案

**问题 1：Node.js 版本不兼容**

```bash
# 错误
Error: The engine "node" is incompatible with this module.

# 解决方案
nvm install 22.22.2
nvm use 22.22.2
```

**问题 2：依赖安装失败**

```bash
# 清除缓存
npm cache clean --force

# 删除 node_modules 和锁文件
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

**问题 3：ESLint 与 Prettier 冲突**

```javascript
// eslint.config.js
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  // ... 其他配置
  prettier  // 必须放在最后
)
```

**问题 4：TypeScript 类型错误**

```typescript
// 错误：Object is possibly 'undefined'
const value = arr[0].name

// 解决方案 1：可选链
const value = arr[0]?.name

// 解决方案 2：非空断言
const value = arr[0]!.name

// 解决方案 3：类型守卫
if (arr[0]) {
  const value = arr[0].name
}
```

### 6.9.3 推荐工具链

| 类别 | 工具 | 版本 |
|------|------|------|
| 运行时 | Node.js | 22.22.2+ |
| 包管理 | npm | 10+ |
| 构建工具 | Vite | 6+ |
| 类型系统 | TypeScript | 6+ |
| 代码检查 | ESLint | 9+ |
| 代码格式化 | Prettier | 3+ |
| 测试框架 | Vitest | 4+ |
| 组件测试 | @vue/test-utils | 2+ |
| DOM 模拟 | jsdom | 29+ |

### 6.9.4 进一步学习

**官方文档：**

- [Vite 官方文档](https://vitejs.dev/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)
- [ESLint 官方文档](https://eslint.org/)
- [Prettier 官方文档](https://prettier.io/)
- [Vitest 官方文档](https://vitest.dev/)
- [GitHub Actions 官方文档](https://docs.github.com/en/actions)

**社区资源：**

- [Vue 3 官方文档](https://vuejs.org/)
- [Pinia 官方文档](https://pinia.vuejs.org/)
- [Vue Router 官方文档](https://router.vuejs.org/)

---

## 附录 A：配置文件模板

### A.1 package.json

```json
{
  "name": "my-project",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview",
    "lint": "eslint src --fix",
    "lint:check": "eslint src",
    "format": "prettier --write \"src/**/*.{ts,tsx,vue,css,json}\"",
    "format:check": "prettier --check \"src/**/*.{ts,tsx,vue,css,json}\"",
    "type-check": "vue-tsc --noEmit",
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage"
  },
  "dependencies": {
    "vue": "^3.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "typescript": "^6.0.0",
    "vite": "^6.0.0",
    "vue-tsc": "^3.0.0",
    "eslint": "^9.0.0",
    "prettier": "^3.0.0",
    "vitest": "^4.0.0"
  }
}
```

### A.2 vite.config.ts

```typescript
/// <reference types="vitest" />
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts']
  },
  server: {
    port: 3000
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    minify: 'esbuild'
  }
})
```

### A.3 tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },
    "types": ["vite/client"]
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "src/**/*.d.ts"],
  "exclude": ["node_modules", "dist"]
}
```

### A.4 eslint.config.js

```javascript
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

export default tseslint.config(
  {
    ignores: ['dist/**', 'node_modules/**', '*.d.ts']
  },
  {
    languageOptions: {
      globals: {
        ...globals.browser
      }
    }
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  prettier,
  {
    files: ['**/*.{ts,tsx,vue}'],
    rules: {
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'warn'
    }
  }
)
```

### A.5 .prettierrc

```json
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "none",
  "printWidth": 100,
  "bracketSpacing": true,
  "arrowParens": "always",
  "endOfLine": "auto"
}
```

### A.6 .github/workflows/ci.yml

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

jobs:
  frontend:
    name: Frontend checks
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint:check
      - run: npm run format:check
      - run: npm run type-check
      - run: npm run test:run
      - run: npm run build
```

---

## 附录 B：常用命令速查

### B.1 npm 命令

```bash
# 安装依赖
npm install                    # 安装所有依赖
npm install <package>          # 安装生产依赖
npm install -D <package>       # 安装开发依赖
npm ci                         # 严格按锁文件安装

# 卸载依赖
npm uninstall <package>

# 更新依赖
npm update                     # 更新所有依赖
npm update <package>           # 更新指定依赖

# 查看依赖
npm list                       # 查看已安装依赖
npm list --depth=0             # 查看顶层依赖
npm outdated                   # 查看过期依赖

# 运行脚本
npm run <script>               # 运行脚本
npm run <script> -- <args>     # 运行脚本并传参

# 其他
npm init                       # 初始化 package.json
npm init -y                    # 快速初始化
```

### B.2 Vite 命令

```bash
# 开发
npm run dev                    # 启动开发服务器
npx vite                       # 启动开发服务器
npx vite --port 3001           # 指定端口

# 构建
npm run build                  # 生产构建
npx vite build                 # 生产构建
npx vite build --mode test     # 指定模式构建

# 预览
npm run preview                # 预览生产构建
npx vite preview               # 预览生产构建
```

### B.3 Git 命令

```bash
# 初始化
git init                       # 初始化仓库
git clone <url>                # 克隆仓库

# 基本操作
git add <file>                 # 暂存文件
git commit -m "message"        # 提交
git push                       # 推送
git pull                       # 拉取

# 分支操作
git branch                     # 查看分支
git checkout <branch>          # 切换分支
git checkout -b <branch>       # 创建并切换分支
git merge <branch>             # 合并分支

# 查看状态
git status                     # 查看状态
git log                        # 查看提交历史
git diff                       # 查看差异
```

---

## 附录 C：术语表

| 术语 | 说明 |
|------|------|
| CI/CD | 持续集成/持续部署 |
| ESM | ES 模块（ECMAScript Modules） |
| HMR | 热模块替换（Hot Module Replacement） |
| SFC | 单文件组件（Single File Component） |
| SemVer | 语义化版本（Semantic Versioning） |
| Tree-shaking | 移除未使用的代码 |
| V8 | Google 的 JavaScript 引擎 |
| jsdom | JavaScript 实现的 DOM |
| mock | 模拟对象/函数 |
| stub | 桩对象/函数 |
| fixture | 测试固件 |

---

**本章完**

> 下一章将深入讲解 SelfBlog 后端工程化实践，包括 Spring Boot 配置、数据库设计、API 设计、测试策略等内容。
