# 设计系统底座 + 公开页导航 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把圆角设计 token、内置 SVG 图标系统、滚动状态 composable 和站点品牌资产（logo/favicon）落到公开页导航栏上，产出大 R 角 + 胶囊选中态 + DeepSeek 式滚动收缩的顶栏。

**Architecture:** 先做共享底座（token / `Icon.vue` / `useScrollState` / `site.ts` 品牌键），再做唯一消费者 `Navbar.vue`。底座全部是可独立单测的纯逻辑或薄渲染层，Navbar 只负责把状态映射成 CSS class，不直接操作样式。

**Tech Stack:** Vue 3.5 `<script setup>` + TypeScript 6 + Pinia 2 + Vitest 4 + @vue/test-utils 2。不新增任何依赖。

## Global Constraints

- **不新增任何运行时依赖**：`frontend/package.json` 的 `dependencies` 保持 7 项不变。
- **布局高度守恒**：`.app-main { padding-top: 80px }`（`src/App.vue:111`）**不得修改**。导航栏滚动收缩只改视觉（高度、模糊、底色、阴影），不得改变它占据的布局高度，否则 `src/views/post/PostDetail.vue:540` 的 `scroll-margin-top:105px` 与 `:641` 的 `.article-side{top:105px}` 锚点全部失效。
- **胶囊选中态统一用 `var(--radius-full)`**，不新增 `--radius-pill` / `--radius-nav`。
- **只替换 `border-radius: <裸值>`**，不动 `50%`（圆形头像）与 `999px`（已是胶囊语义）。
- **命令一律在 `frontend/` 下执行**。
- 每个任务结束必须 `npm run lint:check && npm run type-check` 通过再提交。
- 提交信息用英文 conventional commit 前缀。

---

### Task 1: 圆角 scale 上移一档

纯 CSS 数值调整，无逻辑可测。验证靠格式化检查 + 目视，不用假测试。

**Files:**
- Modify: `frontend/src/styles/variables.css:277-288`

**Interfaces:**
- Consumes: 无
- Produces: 全站可用的 `--radius-xs/sm/md/lg/xl/2xl/3xl`（数值上调一档）；`--radius-full` 保持 `9999px` 不变

- [ ] **Step 1: 替换圆角 scale**

把 `variables.css` 中这一整块：

```css
  /* ---------------------------------------------------------------
     Border Radius — Restrained
     --------------------------------------------------------------- */
  --radius-none: 0;
  --radius-xs:   4px;
  --radius-sm:   4px;
  --radius:      8px;
  --radius-md:   8px;
  --radius-lg:   12px;
  --radius-xl:   16px;
  --radius-2xl:  20px;
  --radius-3xl:  24px;
  --radius-full: 9999px;
```

替换为：

```css
  /* ---------------------------------------------------------------
     Border Radius — Generous
     ---------------------------------------------------------------
     整体上移一档，让卡片/输入框/按钮呈现明显的大 R 角。
     --radius-full 保持 9999px：胶囊选中态和圆形头像共用它，
     同一个值同时覆盖"小方块变圆"和"胶囊"，不必再开新 token。 */
  --radius-none: 0;
  --radius-xs:   6px;
  --radius-sm:   8px;
  --radius:      12px;
  --radius-md:   12px;
  --radius-lg:   16px;
  --radius-xl:   20px;
  --radius-2xl:  24px;
  --radius-3xl:  32px;
  --radius-full: 9999px;
```

- [ ] **Step 2: 验证格式化与 lint**

Run: `npm run format:check && npm run lint:check`
Expected: 两条都通过（无输出 / 无 error）

- [ ] **Step 3: 目视确认没有灾难性回归**

Run: `npm run dev`
Expected: 打开 `http://localhost:3000`，卡片、按钮、输入框圆角明显变大；没有元素被撑破或裁切。深色模式（右上角切换）同样正常。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/variables.css
git commit -m "style: lift the border-radius scale one step for larger corner radii"
```

---

### Task 2: 内置 SVG 图标系统

**Files:**
- Create: `frontend/src/components/common/icons.ts`
- Create: `frontend/src/components/common/Icon.vue`
- Test: `frontend/src/components/common/__tests__/icons.test.ts`

**Interfaces:**
- Consumes: 无
- Produces:
  - `icons.ts`：`export type IconName = keyof typeof icons`，`export const icons: Record<IconName, string>`，`export const iconNames: IconName[]`
  - `Icon.vue`：props `{ name: IconName; size?: number }`（`size` 默认 `20`），渲染 `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">`

- [ ] **Step 1: 写图标注册表的失败测试**

创建 `frontend/src/components/common/__tests__/icons.test.ts`：

```ts
import { describe, it, expect } from 'vitest'
import { icons, iconNames, type IconName } from '../icons'

/** 图标片段允许出现的元素：只用描边绘制，避免混入 <style>/<script>。 */
const ALLOWED_TAGS = [
  'path',
  'circle',
  'rect',
  'line',
  'polyline',
  'polygon',
  'ellipse'
]

const tagPattern = /<\/?([a-zA-Z][a-zA-Z0-9-]*)/g

function tagsIn(fragment: string): string[] {
  return [...fragment.matchAll(tagPattern)].map((m) => m[1].toLowerCase())
}

describe('icon registry', () => {
  it('exposes a non-empty list of names matching the registry keys', () => {
    expect(iconNames.length).toBeGreaterThanOrEqual(21)
    expect([...iconNames].sort()).toEqual(Object.keys(icons).sort())
  })

  it('includes every icon the UI references', () => {
    const required: IconName[] = [
      'logo', 'search', 'sun', 'moon', 'menu', 'close', 'home', 'archive',
      'image', 'info', 'settings', 'gauge', 'file-text', 'tag', 'users',
      'message', 'music', 'play', 'pause', 'arrow-up', 'external'
    ]
    for (const name of required) {
      expect(iconNames).toContain(name)
    }
  })

  it.each(Object.keys(icons))('%s contains at least one drawing element', (name) => {
    const fragment = icons[name as IconName]
    expect(fragment.trim().length).toBeGreaterThan(0)
    expect(tagsIn(fragment).length).toBeGreaterThan(0)
  })

  it.each(Object.keys(icons))('%s only uses stroke-safe elements', (name) => {
    for (const tag of tagsIn(icons[name as IconName])) {
      expect(ALLOWED_TAGS).toContain(tag)
    }
  })

  it.each(Object.keys(icons))('%s has no inline handlers or style blocks', (name) => {
    const fragment = icons[name as IconName]
    expect(fragment).not.toMatch(/<script|<style|on[a-z]+\s*=/i)
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:run -- src/components/common/__tests__/icons.test.ts`
Expected: FAIL，报错 `Failed to resolve import "../icons"`

- [ ] **Step 3: 实现图标注册表**

创建 `frontend/src/components/common/icons.ts`：

```ts
/**
 * 内置 UI 图标集。
 *
 * 全部为 24×24 描边图形，靠 `currentColor` 取色，因此深浅两套主题
 * 自动适配，不需要维护两份资源。站点品牌图标（logo/favicon）不走这里，
 * 由后台上传后通过设置下发。
 */
export const icons = {
  logo: '<path d="M12 2.5 20.5 7.4v9.2L12 21.5 3.5 16.6V7.4z"/><path d="M9 15.5 12 8.5l3 7"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m16.5 16.5 4 4"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/>',
  moon: '<path d="M20.5 15.5A8.5 8.5 0 0 1 8.5 3.5 8.5 8.5 0 1 0 20.5 15.5Z"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  home: '<path d="M4 10.5 12 4l8 6.5V19a1 1 0 0 1-1 1h-4v-5H9v5H5a1 1 0 0 1-1-1z"/>',
  archive: '<path d="M4 5h16v3H4zM5.5 8v11h13V8M10 12h4"/>',
  image: '<rect x="3.5" y="4.5" width="17" height="15" rx="2.5"/><circle cx="8.5" cy="9.5" r="1.6"/><path d="M5 17.5 10 13l3.5 3.5L16 14l3.5 3.5"/>',
  info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5M12 7.6h.01"/>',
  settings: '<path d="M5 7h14M5 12h14M5 17h14"/><circle cx="9" cy="7" r="2"/><circle cx="15" cy="12" r="2"/><circle cx="8" cy="17" r="2"/>',
  gauge: '<path d="M4 17a8 8 0 1 1 16 0"/><path d="m12 17 4-5"/><circle cx="12" cy="17" r="1.4"/>',
  'file-text': '<path d="M6 3.5h7l5 5V20.5H6z"/><path d="M13 3.5v5h5M9 13h6M9 16.5h4"/>',
  tag: '<path d="M12.6 3.5H20.5v7.9L11 21 3 13z"/><circle cx="16.8" cy="7.2" r="1.4"/>',
  users: '<circle cx="9.5" cy="8.5" r="3.2"/><path d="M3.5 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5"/><path d="M16 5.6a3.2 3.2 0 0 1 0 5.9M17.5 14.9c2.1.6 3.5 2.4 3.5 4.6"/>',
  message: '<path d="M20.5 12.5c0 3.6-3.8 6.5-8.5 6.5-1 0-2-.1-2.9-.4L4 20.5l1.3-3.4A6.9 6.9 0 0 1 3.5 12.5C3.5 8.9 7.3 6 12 6s8.5 2.9 8.5 6.5z"/>',
  music: '<path d="M9 18V6.5l10-2v11"/><circle cx="6.5" cy="18" r="2.5"/><circle cx="16.5" cy="15.5" r="2.5"/>',
  play: '<path d="M8 5.5 18.5 12 8 18.5z"/>',
  pause: '<path d="M9 5.5v13M15 5.5v13"/>',
  'arrow-up': '<path d="M12 19V5M6 11l6-6 6 6"/>',
  external: '<path d="M14 4.5h5.5V10M19 5l-8 8"/><path d="M18 13.5V19a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5.5"/>'
} as const

export type IconName = keyof typeof icons

export const iconNames = Object.keys(icons) as IconName[]
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run -- src/components/common/__tests__/icons.test.ts`
Expected: PASS，21 个图标 × 4 组断言全绿

- [ ] **Step 5: 实现 Icon 组件**

创建 `frontend/src/components/common/Icon.vue`：

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { icons, type IconName } from './icons'

const props = withDefaults(defineProps<{ name: IconName; size?: number }>(), {
  size: 20
})

/**
 * 片段来自本仓库的静态注册表，不掺入任何用户输入，因此 v-html 是安全的。
 * 不用外部图标库是为了保持零新增依赖，且描边图形能直接吃 currentColor。
 */
const fragment = computed(() => icons[props.name] ?? icons.logo)
</script>

<template>
  <svg
    class="icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
    v-html="fragment"
  />
</template>

<style scoped>
.icon {
  display: block;
  flex: 0 0 auto;
}
</style>
```

- [ ] **Step 6: 验证类型与 lint**

Run: `npm run type-check && npm run lint:check`
Expected: 两条都通过

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/common/icons.ts frontend/src/components/common/Icon.vue frontend/src/components/common/__tests__/icons.test.ts
git commit -m "feat: add built-in SVG icon registry and Icon component"
```

---

### Task 3: 滚动状态 composable

**Files:**
- Create: `frontend/src/composables/useScrollState.ts`
- Test: `frontend/src/composables/__tests__/useScrollState.test.ts`

**Interfaces:**
- Consumes: 无
- Produces: `useScrollState(threshold?: number)` → `{ scrolled: Ref<boolean>; direction: Ref<'up' | 'down'> }`。必须在组件 `setup()` 内调用（用了 `onMounted`/`onUnmounted`）。`threshold` 默认 `16`。

- [ ] **Step 1: 写失败测试**

创建 `frontend/src/composables/__tests__/useScrollState.test.ts`：

```ts
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { useScrollState } from '../useScrollState'

/** 用文本渲染状态，避免断言 wrapper.vm 上的动态属性带来的类型体操。 */
const Harness = defineComponent({
  setup() {
    const { scrolled, direction } = useScrollState(16)
    return { scrolled, direction }
  },
  template: '<div data-test="state">{{ scrolled }}|{{ direction }}</div>'
})

function mountHarness() {
  return mount(Harness)
}

/** jsdom 的 scrollY 默认不可写，setScrollY 之后同步派发一次 scroll。 */
function setScrollY(y: number) {
  Object.defineProperty(window, 'scrollY', {
    writable: true,
    configurable: true,
    value: y
  })
  window.dispatchEvent(new Event('scroll'))
}

describe('useScrollState', () => {
  beforeEach(() => {
    // 让 rAF 同步执行，测试无需等帧。
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      cb(0)
      return 0
    })
    Object.defineProperty(window, 'scrollY', {
      writable: true,
      configurable: true,
      value: 0
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('starts unscrolled at the top of the page', () => {
    expect(mountHarness().get('[data-test="state"]').text()).toBe('false|up')
  })

  it('stays unscrolled while below the threshold', async () => {
    const wrapper = mountHarness()
    setScrollY(16)
    await nextTick()
    expect(wrapper.get('[data-test="state"]').text()).toBe('false|up')
  })

  it('flags scrolled once past the threshold', async () => {
    const wrapper = mountHarness()
    setScrollY(17)
    await nextTick()
    expect(wrapper.get('[data-test="state"]').text()).toBe('true|down')
  })

  it('reports upward movement after scrolling down', async () => {
    const wrapper = mountHarness()
    setScrollY(400)
    await nextTick()
    setScrollY(200)
    await nextTick()
    expect(wrapper.get('[data-test="state"]').text()).toBe('true|up')
  })

  it('drops back to unscrolled when returning to the top', async () => {
    const wrapper = mountHarness()
    setScrollY(400)
    await nextTick()
    setScrollY(0)
    await nextTick()
    expect(wrapper.get('[data-test="state"]').text()).toBe('false|up')
  })

  it('coalesces bursts of scroll events into a single frame', async () => {
    const raf = vi.fn((cb: FrameRequestCallback) => {
      cb(0)
      return 0
    })
    vi.stubGlobal('requestAnimationFrame', raf)
    const wrapper = mountHarness()
    raf.mockClear()

    for (let i = 0; i < 20; i++) window.dispatchEvent(new Event('scroll'))

    await nextTick()
    expect(raf).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-test="state"]').text()).toBe('false|up')
  })

  it('stops listening after unmount', async () => {
    const wrapper = mountHarness()
    wrapper.unmount()
    setScrollY(500)
    await nextTick()
    expect(wrapper.get('[data-test="state"]').text()).toBe('false|up')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:run -- src/composables/__tests__/useScrollState.test.ts`
Expected: FAIL，报错 `Failed to resolve import "../useScrollState"`

- [ ] **Step 3: 实现 composable**

创建 `frontend/src/composables/useScrollState.ts`：

```ts
import { onMounted, onUnmounted, ref, type Ref } from 'vue'

export type ScrollDirection = 'up' | 'down'

export interface ScrollState {
  /** 页面是否已滚过 `threshold` 像素。 */
  scrolled: Ref<boolean>
  /** 最近一次位移方向，静止时保持上一次的方向。 */
  direction: Ref<ScrollDirection>
}

/**
 * 跟踪页面滚动状态，供顶栏做收缩/隐藏。
 *
 * scroll 事件在每帧最多处理一次：滚动是高频事件，直接读 scrollY 会让
 * 长页面掉帧，所以用 rAF 合并同一帧内的多次触发。
 *
 * 必须在组件 setup() 内调用。
 */
export function useScrollState(threshold = 16): ScrollState {
  const scrolled = ref(false)
  const direction = ref<ScrollDirection>('up')
  let lastY = 0
  let ticking = false

  const update = (): void => {
    ticking = false
    const y = window.scrollY
    scrolled.value = y > threshold
    // 只在真正发生位移时改方向，避免同帧重复触发把方向刷回 up。
    if (y > lastY) direction.value = 'down'
    else if (y < lastY) direction.value = 'up'
    lastY = y
  }

  const onScroll = (): void => {
    if (ticking) return
    ticking = true
    requestAnimationFrame(update)
  }

  onMounted(() => {
    lastY = window.scrollY
    update()
    window.addEventListener('scroll', onScroll, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll)
  })

  return { scrolled, direction }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run -- src/composables/__tests__/useScrollState.test.ts`
Expected: PASS，7 个用例全绿

- [ ] **Step 5: Commit**

```bash
git add frontend/src/composables/useScrollState.ts frontend/src/composables/__tests__/useScrollState.test.ts
git commit -m "feat: add rAF-throttled useScrollState composable"
```

---

### Task 4: 站点品牌资产设置键 + favicon 运行时注入

**Files:**
- Modify: `frontend/src/stores/site.ts`（全文重写，文件仅 68 行）
- Test: `frontend/src/stores/__tests__/site.test.ts`

**Interfaces:**
- Consumes: 无
- Produces:
  - `SiteSettings` 增补两个必填字段 `faviconUrl: string`、`siteLogoUrl: string`
  - `export function mimeForIcon(url: string): string` —— `.svg`→`image/svg+xml`，`.ico`→`image/x-icon`，其余 `image/png`
  - `export function applyFavicon(url: string): void` —— 幂等：先清掉所有 `link[rel][data-runtime]`，再按 `url` 决定是否注入。`url` 为空时只做清理，保留 `index.html:12` 的静态兜底。

- [ ] **Step 1: 写失败测试**

创建 `frontend/src/stores/__tests__/site.test.ts`：

```ts
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { applyFavicon, mimeForIcon, useSiteStore } from '../site'

const STATIC_FAVICON = '<link rel="icon" type="image/svg+xml" href="/favicon.svg">'

function injected(): HTMLLinkElement | null {
  return document.head.querySelector<HTMLLinkElement>('link[rel][data-runtime]')
}

describe('mimeForIcon', () => {
  it('maps svg', () => {
    expect(mimeForIcon('/uploads/site-assets/favicon/a.svg')).toBe('image/svg+xml')
  })

  it('maps ico', () => {
    expect(mimeForIcon('/uploads/site-assets/favicon/a.ico')).toBe('image/x-icon')
  })

  it('falls back to png for raster and unknown formats', () => {
    expect(mimeForIcon('/uploads/site-assets/favicon/a.png')).toBe('image/png')
    expect(mimeForIcon('/uploads/site-assets/favicon/a.webp')).toBe('image/png')
    expect(mimeForIcon('https://cdn.example.com/fav')).toBe('image/png')
  })

  it('ignores query strings when reading the extension', () => {
    expect(mimeForIcon('/uploads/a.svg?v=2')).toBe('image/svg+xml')
  })
})

describe('applyFavicon', () => {
  beforeEach(() => {
    document.head.innerHTML = STATIC_FAVICON
  })

  it('injects a link derived from the uploaded url', () => {
    applyFavicon('/uploads/site-assets/favicon/abc.svg')

    const link = injected()
    expect(link).not.toBeNull()
    expect(link?.getAttribute('href')).toBe('/uploads/site-assets/favicon/abc.svg')
    expect(link?.getAttribute('type')).toBe('image/svg+xml')
    expect(link?.getAttribute('rel')).toBe('icon')
  })

  it('replaces the previous injection instead of stacking links', () => {
    applyFavicon('/uploads/site-assets/favicon/first.png')
    applyFavicon('/uploads/site-assets/favicon/second.ico')

    expect(document.head.querySelectorAll('link[rel][data-runtime]')).toHaveLength(1)
    expect(injected()?.getAttribute('href')).toBe('/uploads/site-assets/favicon/second.ico')
  })

  it('keeps the static fallback untouched when the setting is cleared', () => {
    applyFavicon('/uploads/site-assets/favicon/first.png')
    applyFavicon('')

    expect(injected()).toBeNull()
    const links = document.head.querySelectorAll('link')
    expect(links).toHaveLength(1)
    expect(links[0].getAttribute('href')).toBe('/favicon.svg')
  })

  it('never removes the static fallback link', () => {
    applyFavicon('/uploads/site-assets/favicon/first.png')
    expect(document.head.querySelector('link[href="/favicon.svg"]')).not.toBeNull()
  })
})

describe('site store brand assets', () => {
  beforeEach(() => {
    document.head.innerHTML = STATIC_FAVICON
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('defaults both brand assets to empty strings', () => {
    const store = useSiteStore()
    expect(store.settings.faviconUrl).toBe('')
    expect(store.settings.siteLogoUrl).toBe('')
  })

  it('applies uploaded brand assets and injects the favicon', () => {
    const store = useSiteStore()
    store.applySettings({
      siteLogoUrl: '/uploads/site-assets/logo/a.png',
      faviconUrl: '/uploads/site-assets/favicon/a.ico'
    })

    expect(store.settings.siteLogoUrl).toBe('/uploads/site-assets/logo/a.png')
    expect(injected()?.getAttribute('href')).toBe('/uploads/site-assets/favicon/a.ico')
  })

  it('falls back to the static favicon when the setting is empty', () => {
    const store = useSiteStore()
    store.applySettings({ faviconUrl: '' })
    expect(injected()).toBeNull()
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:run -- src/stores/__tests__/site.test.ts`
Expected: FAIL，报错 `mimeForIcon is not a function` 或 `faviconUrl` 为 `undefined`

- [ ] **Step 3: 重写 site store**

用以下内容整体替换 `frontend/src/stores/site.ts`：

```ts
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export interface SiteSettings {
  siteTitle: string
  siteDescription: string
  authorName: string
  authorBio: string
  authorAvatar: string
  adminPath: string
  commentsEnabled: string
  visitorTrackingEnabled: string
  musicEnabled: string
  musicUrl: string
  musicTitle: string
  themeSkin: string
  themePrimaryColor: string
  footerText: string
  /** 浏览器标签页图标，由后台上传后下发；为空时回落到 index.html 的静态 /favicon.svg。 */
  faviconUrl: string
  /** 顶栏与后台侧边栏的品牌图标；为空时回落内置 Icon name="logo"。 */
  siteLogoUrl: string
}

const defaults: SiteSettings = {
  siteTitle: '个人博客',
  siteDescription: '记录技术、生活与一路上的新发现。',
  authorName: '站长',
  authorBio: '欢迎来到我的个人博客。',
  authorAvatar: '',
  adminPath: 'admin',
  commentsEnabled: 'true',
  visitorTrackingEnabled: 'true',
  musicEnabled: 'false',
  musicUrl: '',
  musicTitle: '背景音乐',
  themeSkin: 'ocean',
  themePrimaryColor: '#165DFF',
  footerText: '用文字记录生活，用好奇探索世界。',
  faviconUrl: '',
  siteLogoUrl: ''
}

/** 运行时注入的 favicon 节点标记，用来和 index.html 里的静态兜底区分。 */
const FAVICON_DATA_ATTR = 'runtime'

/**
 * 由文件扩展名推导 favicon 的 MIME。
 * 浏览器只认 svg/ico/png 这几类；其余（webp 等）按 png 处理，
 * 猜错只是不显示，不会更糟。
 */
export function mimeForIcon(url: string): string {
  const path = url.split('?')[0].split('#')[0].toLowerCase()
  if (path.endsWith('.svg')) return 'image/svg+xml'
  if (path.endsWith('.ico')) return 'image/x-icon'
  return 'image/png'
}

/**
 * 把 faviconUrl 同步到 <head>。
 *
 * 幂等：每次先清掉上一次运行时注入的节点，再按需注入。
 * url 为空时只清理、不注入 —— index.html 里的静态 /favicon.svg 从未被移除，
 * 因此"清空设置"会自然回落到默认图标。
 */
export function applyFavicon(url: string): void {
  document.head
    .querySelectorAll<HTMLLinkElement>(`link[rel][data-${FAVICON_DATA_ATTR}]`)
    .forEach((link) => link.remove())

  if (!url) return

  const link = document.createElement('link')
  link.rel = 'icon'
  link.type = mimeForIcon(url)
  link.href = url
  link.dataset[FAVICON_DATA_ATTR] = 'favicon'
  document.head.appendChild(link)
}

export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings>({ ...defaults })
  const adminPath = computed(() => settings.value.adminPath || 'admin')
  const adminRoot = computed(() => `/${adminPath.value}`)
  const isCommentsEnabled = computed(() => settings.value.commentsEnabled === 'true')
  const isMusicEnabled = computed(() => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl)

  function applySettings(input: Partial<SiteSettings> | Record<string, string>): void {
    settings.value = { ...settings.value, ...input }
    const appRoot = document.getElementById('selfblog')
    if (appRoot) appRoot.setAttribute('data-skin', settings.value.themeSkin || 'ocean')
    if (appRoot) {
      appRoot.style.setProperty('--accent', settings.value.themePrimaryColor || '#165DFF')
      appRoot.style.setProperty('--primary', settings.value.themePrimaryColor || '#165DFF')
    }
    document.title = settings.value.siteTitle || defaults.siteTitle
    applyFavicon(settings.value.faviconUrl || '')
  }

  async function loadPublicSettings(): Promise<void> {
    try {
      const response = await fetch('/api/site/public', { headers: { Accept: 'application/json' } })
      if (!response.ok) return
      const envelope = (await response.json()) as { data?: Partial<SiteSettings> }
      if (envelope.data) applySettings(envelope.data)
    } catch {
      // Keep the local defaults available when the API is temporarily offline.
    }
  }

  return { settings, adminPath, adminRoot, isCommentsEnabled, isMusicEnabled, applySettings, loadPublicSettings }
})
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run -- src/stores/__tests__/site.test.ts`
Expected: PASS，11 个用例全绿

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/site.ts frontend/src/stores/__tests__/site.test.ts
git commit -m "feat: add faviconUrl/siteLogoUrl settings keys with runtime favicon injection"
```

---

### Task 5: 导航栏滚动三态 + 图标 + 胶囊选中 + 品牌 logo

**Files:**
- Modify: `frontend/src/components/layout/Navbar.vue`（全文重写，当前 380 行）
- Test: `frontend/src/components/layout/__tests__/Navbar.test.ts`

**Interfaces:**
- Consumes: `useScrollState`（Task 3）、`Icon.vue`（Task 2）、`siteStore.settings.siteLogoUrl`（Task 4）
- Produces: 根元素 `.site-nav-wrap` 上的 `is-scrolled` / `is-hidden` class；`.brand-mark--image` 用于上传的 logo

- [ ] **Step 1: 写失败测试**

创建 `frontend/src/components/layout/__tests__/Navbar.test.ts`：

```ts
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

const scrolled = ref(false)
const direction = ref<'up' | 'down'>('up')

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('@/composables/useScrollState', () => ({
  useScrollState: () => ({ scrolled, direction })
}))

import Navbar from '../Navbar.vue'
import { useSiteStore } from '@/stores/site'

/** 组件和测试必须共用同一个 pinia 实例，否则改 store 组件看不到。 */
let pinia: ReturnType<typeof createPinia>

function mountNavbar() {
  return mount(Navbar, {
    global: {
      stubs: { RouterLink: true },
      plugins: [pinia]
    }
  })
}

describe('Navbar', () => {
  beforeEach(() => {
    scrolled.value = false
    direction.value = 'up'
    pinia = createPinia()
    setActivePinia(pinia)
  })

  it('falls back to the built-in logo icon when no logo is uploaded', () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('img.brand-mark--image').exists()).toBe(false)
    expect(wrapper.find('.brand-mark .icon').exists()).toBe(true)
  })

  it('renders the uploaded logo instead of the built-in icon', async () => {
    const store = useSiteStore()
    store.applySettings({ siteLogoUrl: '/uploads/site-assets/logo/a.png' })
    const wrapper = mountNavbar()
    await nextTick()

    const img = wrapper.find('img.brand-mark--image')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/uploads/site-assets/logo/a.png')
  })

  // 首页用 exact-active-class："/" 是所有路径的前缀，用 active-class 会永远高亮。
  it('wires the active class onto the nav links', () => {
    const links = mountNavbar().findAll('.nav-links > *')
    expect(links).toHaveLength(4)
    expect(links[0].attributes('exact-active-class')).toBe('active')
    for (const link of links.slice(1)) {
      expect(link.attributes('active-class')).toBe('active')
    }
  })

  it('marks the wrapper as scrolled once past the threshold', async () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-scrolled')

    scrolled.value = true
    await nextTick()
    expect(wrapper.find('.site-nav-wrap').classes()).toContain('is-scrolled')
  })

  // 这里只断言状态 class；实际隐藏效果由 CSS 的 @media (max-width: 760px) 决定，
  // jsdom 不跑媒体查询，所以视觉效果只能目视验证。
  it('flags the nav as hidden while scrolling down', async () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-hidden')

    scrolled.value = true
    direction.value = 'down'
    await nextTick()
    expect(wrapper.find('.site-nav-wrap').classes()).toContain('is-hidden')

    direction.value = 'up'
    await nextTick()
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-hidden')
  })

  it('never hides the nav before the page is scrolled', async () => {
    const wrapper = mountNavbar()
    direction.value = 'down'
    await nextTick()
    expect(wrapper.find('.site-nav-wrap').classes()).not.toContain('is-hidden')
  })

  it('uses the built-in icon set instead of text glyphs', () => {
    const wrapper = mountNavbar()
    expect(wrapper.find('.nav-search .icon').exists()).toBe(true)
    expect(wrapper.find('.theme-button .icon').exists()).toBe(true)
    expect(wrapper.find('.menu-toggle .icon').exists()).toBe(true)
    expect(wrapper.find('.theme-button').text()).toBe('')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:run -- src/components/layout/__tests__/Navbar.test.ts`
Expected: FAIL，`.brand-mark--image` 不存在、`.site-nav-wrap` 无 `is-scrolled`、图标断言失败

- [ ] **Step 3: 重写 Navbar**

用以下内容整体替换 `frontend/src/components/layout/Navbar.vue`：

```vue
<template>
  <header class="site-nav-wrap" :class="{ 'is-scrolled': scrolled, 'is-hidden': hidden }">
    <nav class="site-nav" aria-label="主导航">
      <div class="nav-shell">
        <router-link to="/" class="brand" :aria-label="`${siteStore.settings.siteTitle} 首页`">
          <img
            v-if="siteStore.settings.siteLogoUrl"
            class="brand-mark brand-mark--image"
            :src="siteStore.settings.siteLogoUrl"
            alt=""
            aria-hidden="true"
          />
          <span v-else class="brand-mark" aria-hidden="true">
            <Icon name="logo" :size="16" />
          </span>
          <span class="brand-name">{{ siteStore.settings.siteTitle }}</span>
        </router-link>

        <div class="nav-links" aria-label="博客导航">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/archives" active-class="active">归档</router-link>
          <router-link to="/gallery" active-class="active">相册</router-link>
          <router-link to="/about" active-class="active">关于</router-link>
        </div>

        <form class="nav-search" role="search" @submit.prevent="search">
          <Icon name="search" :size="17" />
          <input v-model="searchQuery" type="search" placeholder="搜索文章" aria-label="搜索文章" />
          <kbd>↵</kbd>
        </form>

        <div class="nav-actions">
          <button
            class="icon-button theme-button"
            type="button"
            :aria-label="themeStore.isDarkMode ? '切换亮色模式' : '切换暗色模式'"
            @click="themeStore.toggleTheme"
          >
            <Icon :name="themeStore.isDarkMode ? 'sun' : 'moon'" :size="17" />
          </button>

          <button
            class="menu-toggle"
            type="button"
            :aria-expanded="mobileOpen"
            aria-label="切换导航菜单"
            @click="mobileOpen = !mobileOpen"
          >
            <Icon :name="mobileOpen ? 'close' : 'menu'" :size="18" />
          </button>
        </div>
      </div>

      <Transition name="mobile-nav">
        <div v-if="mobileOpen" class="mobile-panel">
          <form class="mobile-search" role="search" @submit.prevent="search">
            <input
              v-model="searchQuery"
              type="search"
              placeholder="搜索文章"
              aria-label="搜索文章"
            />
            <button type="submit">搜索</button>
          </form>
          <router-link to="/" @click="mobileOpen = false">首页</router-link>
          <router-link to="/archives" @click="mobileOpen = false">归档</router-link>
          <router-link to="/gallery" @click="mobileOpen = false">相册</router-link>
          <router-link to="/about" @click="mobileOpen = false">关于</router-link>
        </div>
      </Transition>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useSiteStore } from '@/stores/site'
import { useScrollState } from '@/composables/useScrollState'
import Icon from '@/components/common/Icon.vue'

const router = useRouter()
const themeStore = useThemeStore()
const siteStore = useSiteStore()
const { scrolled, direction } = useScrollState(16)

const searchQuery = ref('')
const mobileOpen = ref(false)

/**
 * 小屏下滚时把顶栏收走，上滚立刻归位。
 * 未滚过阈值前不隐藏 —— 否则首屏滑一下就找不到导航了。
 */
const hidden = computed(() => scrolled.value && direction.value === 'down')

const search = () => {
  const query = searchQuery.value.trim()
  if (!query) return
  mobileOpen.value = false
  searchQuery.value = ''
  router.push({ path: '/search', query: { q: query } })
}
</script>

<style scoped>
.site-nav-wrap {
  position: fixed;
  z-index: var(--z-sticky);
  top: 14px;
  left: 0;
  right: 0;
  padding: 0 20px;
  transition:
    transform var(--transition-slow),
    opacity var(--transition-slow);
}

/* 小屏下滚收起、上滚归位。桌面端不隐藏，避免鼠标用户找不到导航。 */
@media (max-width: 760px) {
  .site-nav-wrap.is-hidden {
    transform: translateY(-140%);
    opacity: 0;
    pointer-events: none;
  }
}

.site-nav {
  position: relative;
  max-width: 1120px;
  margin: 0 auto;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-2xl);
  background: var(--surface-primary);
  box-shadow: 0 8px 30px rgba(24, 31, 40, 0.07);
  transition:
    background var(--transition-slow),
    box-shadow var(--transition-slow),
    backdrop-filter var(--transition-slow),
    border-color var(--transition-slow);
}

/* 滚过后转成毛玻璃实心条：高度向下收，顶边不动，内容零位移。 */
.is-scrolled .site-nav {
  border-color: transparent;
  background: var(--navbar-bg);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  box-shadow: 0 10px 34px rgba(24, 31, 40, 0.12);
}

.nav-shell {
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 18px;
  transition: min-height var(--transition-slow);
}

.is-scrolled .nav-shell {
  min-height: 48px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-width: max-content;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 27px;
  height: 27px;
  border-radius: var(--radius-sm);
  color: white;
  background: var(--primary);
  font-size: 1rem;
  flex: 0 0 auto;
}

.brand-mark--image {
  display: block;
  object-fit: cover;
  background: transparent;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 胶囊选中态：选中带浅底，未选中只有 hover 才出底，避免导航变成一排实心色块。 */
.nav-links a,
.mobile-panel a {
  padding: 7px 14px;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.nav-links a:hover,
.mobile-panel a:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.nav-links a.active {
  background: var(--primary-light);
  color: var(--primary);
}

.nav-search {
  display: flex;
  align-items: center;
  gap: 9px;
  width: min(260px, 24vw);
  margin-left: auto;
  padding: 8px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-full);
  background: var(--bg-primary);
  color: var(--text-muted);
}

.nav-search input,
.mobile-search input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 0.8125rem;
}

.nav-search input::placeholder,
.mobile-search input::placeholder {
  color: var(--text-muted);
}

.nav-search kbd {
  min-width: 20px;
  text-align: center;
  color: var(--text-muted);
  font: 0.7rem var(--font-sans);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: max-content;
}

.icon-button,
.menu-toggle {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.icon-button:hover,
.menu-toggle:hover {
  background: var(--bg-secondary);
  color: var(--primary);
}

.menu-toggle {
  display: none;
}

.mobile-panel {
  display: none;
}

.mobile-search {
  display: flex;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-solid);
}

.mobile-search input {
  padding: 9px 12px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
}

.mobile-search button {
  border: 0;
  border-radius: var(--radius-full);
  padding: 0 14px;
  background: var(--primary);
  color: white;
  font-weight: 600;
}

.mobile-nav-enter-active,
.mobile-nav-leave-active {
  transition:
    opacity 150ms ease,
    transform 150ms ease;
}

.mobile-nav-enter-from,
.mobile-nav-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 760px) {
  .site-nav-wrap {
    top: 8px;
    padding: 0 10px;
  }
  .nav-shell {
    min-height: 52px;
    gap: 12px;
    padding: 0 12px;
  }
  .nav-links,
  .nav-search {
    display: none;
  }
  .nav-actions {
    margin-left: auto;
  }
  .menu-toggle {
    display: grid;
  }
  .mobile-panel {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 10px 12px 14px;
    border-top: 1px solid var(--border-solid);
  }
  .mobile-panel > a {
    text-align: left;
  }
}

@media (prefers-reduced-motion: reduce) {
  .site-nav-wrap,
  .site-nav,
  .nav-shell {
    transition: none;
  }
  .site-nav-wrap.is-hidden {
    transform: none;
    opacity: 0;
  }
}

@media (max-width: 420px) {
  .brand {
    font-size: 0.96rem;
  }
  .brand-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run -- src/components/layout/__tests__/Navbar.test.ts`
Expected: PASS，7 个用例全绿

- [ ] **Step 5: 目视确认滚动态**

Run: `npm run dev`
Expected:
- 顶部静止时是圆角胶囊、实白底、轻阴影
- 向下滚过一点后高度收紧、底色转半透明 + 毛玻璃、阴影加深，**正文位置不跳动**
- 选中项是浅蓝底胶囊，hover 其他项出浅灰底
- 375px 宽（DevTools）下滚导航收走，上滚立刻回来
- 深色模式下毛玻璃与胶囊底色都正常，文字不发灰

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/layout/Navbar.vue frontend/src/components/layout/__tests__/Navbar.test.ts
git commit -m "feat: rebuild navbar with scroll states, icon set and pill active state"
```

---

### Task 6: 全局锚点偏移 + 全量校验

**Files:**
- Modify: `frontend/src/styles/base.css:10-15`

**Interfaces:**
- Consumes: 无
- Produces: `html { scroll-padding-top: 96px }`，供 `PostDetail.vue` 后续的标题锚点复用

- [ ] **Step 1: 补 scroll-padding-top**

把 `base.css` 中的：

```css
html {
  font-size: 16px;
  scroll-behavior: smooth;
  -webkit-text-size-adjust: 100%;
  color-scheme: light dark;
}
```

替换为：

```css
html {
  font-size: 16px;
  scroll-behavior: smooth;
  /* 顶栏是 fixed 浮动胶囊，原生锚点跳转需要给它让位，
     否则 #hash 跳转后标题会被压在导航下面。 */
  scroll-padding-top: 96px;
  -webkit-text-size-adjust: 100%;
  color-scheme: light dark;
}
```

- [ ] **Step 2: 全量校验**

Run: `npm run lint:check && npm run format:check && npm run type-check && npm run test:run`
Expected: 四条全过；测试数量比改动前多出本计划新增的用例，且无既有用例失败

- [ ] **Step 3: 生产构建验证**

Run: `npm run build`
Expected: `vue-tsc --noEmit` 无报错，Vite 构建成功产出 `dist/`

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/base.css
git commit -m "style: add scroll-padding-top so anchors clear the floating navbar"
```

---

## 完成后状态

- `frontend/src/styles/variables.css` 圆角整体加大
- `frontend/src/components/common/icons.ts` + `Icon.vue` 提供 21 个内置描边图标，零新增依赖
- `frontend/src/composables/useScrollState.ts` 提供 rAF 节流的滚动状态
- `frontend/src/stores/site.ts` 新增 `faviconUrl` / `siteLogoUrl` 与 `applyFavicon`
- `frontend/src/components/layout/Navbar.vue` 支持滚动三态、图标、胶囊选中、可上传品牌 logo
- `frontend/src/styles/base.css` 补 `scroll-padding-top`

**不在本计划内**（后续计划处理）：后台上传接口与 `Settings.vue` 上传组件、`PostDetail.vue` 阅读体验增强、`AdminLayout.vue` 胶囊化与全站圆角扫描替换。

## 手动验证清单

1. `npm run dev` + `mvn spring-boot:run`（`backend/`）同时起着
2. 首页向下滚 → 顶栏收紧变毛玻璃且正文不跳
3. 375px 宽仿移动端滚动态
4. 深浅两套主题各过一遍
5. DevTools 开 `prefers-reduced-motion: reduce`，确认过渡被关掉但功能仍在
6. `npm run test:run` 全绿、`npm run build` 成功
