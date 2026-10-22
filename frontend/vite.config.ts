/// <reference types="vitest" />
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { readFileSync } from 'fs'
import { resolve } from 'path'

// 版本号的唯一真源是 package.json：手写字符串会像 main.ts 里那样落后两三个版本
// 而无人发现。这里在构建/测试期注入常量，运行时与打包产物始终一致。
const APP_VERSION: string = JSON.parse(
  readFileSync(resolve(__dirname, 'package.json'), 'utf-8')
).version

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    define: {
      __APP_VERSION__: JSON.stringify(APP_VERSION)
    },
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
        // 前端测试用 ?raw 读取仓库根共享的 GFM fixture（docs/fixtures/gfm-sample.md）
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
