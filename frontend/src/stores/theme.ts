/**
 * 主题状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { STORAGE_KEY_PREFIX } from '@/constants'

type ThemeMode = 'light' | 'dark' | 'system'

export const useThemeStore = defineStore('theme', () => {
  // 状态
  const isDarkMode = ref(false)
  const theme = ref<ThemeMode>('light')
  let mediaQuery: MediaQueryList | null = null
  let mediaHandler: (() => void) | null = null
  let themeFrame = 0
  let themeFrameInner = 0

  // 计算属性
  const currentTheme = computed<'light' | 'dark'>(() => {
    if (theme.value === 'system') {
      return (mediaQuery || window.matchMedia('(prefers-color-scheme: dark)')).matches
        ? 'dark'
        : 'light'
    }
    return theme.value
  })

  // 初始化主题
  function initTheme(): void {
    // 从本地存储读取主题设置
    let savedTheme: ThemeMode = 'light'
    try {
      const stored = localStorage.getItem(STORAGE_KEY_PREFIX + 'theme')
      // 不能直接 as ThemeMode：历史/脏值（如 'dark-mode'）会让 UI 与存储值互相矛盾。
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        savedTheme = stored
      }
    } catch {
      // storage unavailable
    }
    theme.value = savedTheme

    // 清理旧监听器
    destroyTheme()

    // 应用主题
    applyTheme()

    // 监听系统主题变化
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaHandler = () => {
      if (theme.value === 'system') {
        applyTheme()
      }
    }
    mediaQuery.addEventListener('change', mediaHandler)
  }

  // 清理监听器
  function destroyTheme(): void {
    if (themeFrame) cancelAnimationFrame(themeFrame)
    if (themeFrameInner) cancelAnimationFrame(themeFrameInner)
    themeFrame = 0
    themeFrameInner = 0
    document.documentElement.classList.remove('theme-changing')
    if (mediaQuery && mediaHandler) {
      mediaQuery.removeEventListener('change', mediaHandler)
      mediaQuery = null
      mediaHandler = null
    }
  }

  // 应用主题
  function applyTheme(): void {
    if (themeFrame) cancelAnimationFrame(themeFrame)
    if (themeFrameInner) cancelAnimationFrame(themeFrameInner)
    document.documentElement.classList.add('theme-changing')
    const effectiveTheme = currentTheme.value
    isDarkMode.value = effectiveTheme === 'dark'

    if (isDarkMode.value) {
      document.documentElement.classList.add('dark-mode')
    } else {
      document.documentElement.classList.remove('dark-mode')
    }
    // Let Vue's app-root class and the document palette settle in the same paint.
    // 两个帧 id 必须分开记录：外层回调曾把内层 id 写进 themeFrame，
    // 期间到达的 cancelAnimationFrame 会取消一个已经执行过的帧，.theme-changing 就永远摘不掉。
    themeFrame = requestAnimationFrame(() => {
      themeFrame = 0
      themeFrameInner = requestAnimationFrame(() => {
        themeFrameInner = 0
        document.documentElement.classList.remove('theme-changing')
      })
    })
  }

  // 切换主题
  function toggleTheme(): void {
    if (theme.value === 'light') {
      theme.value = 'dark'
    } else if (theme.value === 'dark') {
      theme.value = 'system'
    } else {
      theme.value = 'light'
    }

    try {
      localStorage.setItem(STORAGE_KEY_PREFIX + 'theme', theme.value)
    } catch {
      // storage unavailable
    }
    applyTheme()
  }

  // 设置主题
  function setTheme(newTheme: ThemeMode): void {
    theme.value = newTheme
    try {
      localStorage.setItem(STORAGE_KEY_PREFIX + 'theme', theme.value)
    } catch {
      // storage unavailable
    }
    applyTheme()
  }

  return {
    isDarkMode,
    theme,
    currentTheme,
    initTheme,
    destroyTheme,
    toggleTheme,
    setTheme
  }
})
