import '@testing-library/jest-dom'
import { config } from '@vue/test-utils'
import { vi } from 'vitest'
import { vScrollReveal } from '@/composables/useScrollReveal'

// 全局测试配置
config.global.stubs = {}
// 注册滚动入场指令：否则模板里的 v-scroll-reveal 解析不到，
// 控制台会刷一串 “Failed to resolve directive” 警告。
config.global.directives = {
  'scroll-reveal': vScrollReveal,
  scrollReveal: vScrollReveal
}
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
