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
