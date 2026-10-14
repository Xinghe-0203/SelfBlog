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
      savedTheme = (localStorage.getItem(STORAGE_KEY_PREFIX + 'theme') as ThemeMode) || 'light'
    } catch {
      // storage unavailable
    }
    theme.value = savedTheme

    // 应用主题
    applyTheme()

    // 清理旧监听器
    destroyTheme()

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
    if (mediaQuery && mediaHandler) {
      mediaQuery.removeEventListener('change', mediaHandler)
      mediaQuery = null
      mediaHandler = null
    }
  }

  // 应用主题
  function applyTheme(): void {
    const effectiveTheme = currentTheme.value
    isDarkMode.value = effectiveTheme === 'dark'

    if (isDarkMode.value) {
      document.documentElement.classList.add('dark-mode')
    } else {
      document.documentElement.classList.remove('dark-mode')
    }
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
