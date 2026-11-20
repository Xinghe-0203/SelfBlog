/**
 * useScrollReveal — 基于 IntersectionObserver 的滚动入场。
 *
 * 元素进入视口时加上 `.is-visible`，配合 `styles/motion.css` 里的
 * `.reveal-ready.reveal-up` 等类完成「位移 + 淡入」的入场。
 *
 * 三个关键设计：
 *
 * 1. **共享一个观察器**。早先的实现在每个元素上各建一个 IntersectionObserver：
 *    一页 20 个卡片就是 20 个观察器、20 份回调开销。现在全局只有一个观察器，
 *    元素的差异（threshold / rootMargin）通过 WeakMap 里的选项表区分 ——
 *    观察器的 threshold 只能有一个值，所以不同 threshold 的元素分桶处理。
 * 2. **初始态由 JS 加类**。`.reveal-ready`（初始 opacity:0）只在指令挂载时
 *    才加上：脚本被拦截、hydration 失败或用户禁用 JS 时，元素仍然是可见的，
 *    不会留下一片空白。
 * 3. **尊重 reduced-motion**。系统开启「减少动态效果」时直接判定为可见，
 *    不做任何位移与淡入。
 *
 * 用法：
 *   <div class="reveal-up" v-scroll-reveal>…</div>
 *   <div class="reveal-up" v-scroll-reveal="{ threshold: 0.2, variant: 'reveal-scale' }">…</div>
 *   <div class="reveal-up" v-scroll-reveal="{ stagger: true }">…</div>   // 同级依次延迟
 */

import { onUnmounted, type Directive } from 'vue'

export interface ScrollRevealOptions {
  /** IntersectionObserver threshold (0-1). Default: 0.1 */
  threshold?: number
  /** IntersectionObserver rootMargin. Default: '0px 0px -20px 0px' */
  rootMargin?: string
  /** Whether to reveal only once. Default: true */
  once?: boolean
  /** 位移方向类名。Default: 'reveal-up' */
  variant?: RevealVariant
  /** 同级依次延迟（每档 70ms，最多 8 档）。Default: false */
  stagger?: boolean
}

/** 位移方向的四个取值（与 motion.css 里的类名一一对应）。 */
export type RevealVariant = 'reveal-up' | 'reveal-left' | 'reveal-right' | 'reveal-scale'

/** 解析后的选项：全部字段都有确定值。 */
type ResolvedOptions = Required<ScrollRevealOptions>

const DEFAULT_OPTIONS: ResolvedOptions = {
  threshold: 0.1,
  rootMargin: '0px 0px -20px 0px',
  once: true,
  variant: 'reveal-up',
  stagger: false
}

/** 观察器按 threshold + rootMargin 分桶：同一个桶共用一个观察器。 */
const buckets = new Map<string, { observer: IntersectionObserver; elements: Set<Element> }>()

/** 每个元素的实际选项（用来算 delay 与是否 once）。 */
const elementOptions = new WeakMap<Element, ResolvedOptions>()

/** 同级元素的序号：用于 stagger。 */
const staggerIndex = new WeakMap<Element, number>()

const REVEAL_READY = 'reveal-ready'
const REVEAL_VISIBLE = 'is-visible'

/** 系统是否要求减少动态效果。 */
function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
  try {
    // matchMedia 可能被 mock 成返回 undefined（测试里 resetAllMocks 会清掉实现），
    // 因此用可选链 + 布尔比较，而不是直接读 .matches。
    return window.matchMedia('(prefers-reduced-motion: reduce)')?.matches === true
  } catch {
    return false
  }
}

/**
 * 环境是否支持 IntersectionObserver。
 *
 * jsdom 与部分旧浏览器没有它：没有观察器就没有「进入视口」这个事件，
 * 元素会永远停在初始的 opacity:0 —— 那是一片空白，比没有动效糟糕得多。
 * 所以不支持时直接判定为可见。
 */
function supportsObserver(): boolean {
  return typeof IntersectionObserver !== 'undefined'
}

/** 同级里第几个：只看带同一个指令标记的元素。 */
function computeStaggerIndex(el: Element): number {
  const parent = el.parentElement
  if (!parent) return 0
  const siblings = Array.from(parent.children).filter(
    (node) => node instanceof Element && node.hasAttribute('data-scroll-reveal')
  )
  const index = siblings.indexOf(el)
  return index < 0 ? 0 : index
}

function bucketKey(options: ResolvedOptions): string {
  return `${options.threshold}|${options.rootMargin}`
}

function getBucket(options: ResolvedOptions) {
  const key = bucketKey(options)
  let bucket = buckets.get(key)
  if (!bucket) {
    const elements = new Set<Element>()
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const element = entry.target
          const opts = elementOptions.get(element)
          if (!opts) continue
          if (entry.isIntersecting) {
            element.classList.add(REVEAL_VISIBLE)
            if (opts.once) {
              observer.unobserve(element)
              elements.delete(element)
            }
          } else if (!opts.once) {
            element.classList.remove(REVEAL_VISIBLE)
          }
        }
      },
      { threshold: options.threshold, rootMargin: options.rootMargin }
    )
    bucket = { observer, elements }
    buckets.set(key, bucket)
  }
  return bucket
}

function revealNow(el: Element): void {
  el.classList.add(REVEAL_READY)
  el.classList.add(REVEAL_VISIBLE)
}

function setupElement(el: Element, options: ResolvedOptions): void {
  elementOptions.set(el, options)

  // 减少动态效果：不做入场，直接判定为可见。
  // 环境不支持 IntersectionObserver 时同理 —— 否则元素永远停在 opacity:0。
  if (prefersReducedMotion() || !supportsObserver()) {
    revealNow(el)
    return
  }

  el.classList.add(REVEAL_READY)
  el.classList.add(options.variant)
  if (options.stagger) {
    const index = Math.min(computeStaggerIndex(el), 7)
    staggerIndex.set(el, index)
    if (index > 0 && el instanceof HTMLElement) el.style.transitionDelay = `${index * 70}ms`
  }

  const bucket = getBucket(options)
  bucket.elements.add(el)
  bucket.observer.observe(el)
}

function teardownElement(el: Element): void {
  for (const bucket of buckets.values()) {
    bucket.observer.unobserve(el)
    bucket.elements.delete(el)
  }
  elementOptions.delete(el)
  staggerIndex.delete(el)
}

/**
 * Composable API — 供 script setup 里手动观察元素。
 *
 * 复用同一套观察器桶，因此不会重复创建 IntersectionObserver。
 */
export function useScrollReveal(userOptions?: ScrollRevealOptions) {
  const options: ResolvedOptions = { ...DEFAULT_OPTIONS, ...(userOptions ?? {}) }
  const observed: Element[] = []

  const observe = (target: Element | Element[] | NodeList | null): void => {
    if (!target) return
    const list: Element[] =
      target instanceof Element ? [target] : Array.from(target as ArrayLike<Element>)
    for (const el of list) {
      setupElement(el, options)
      observed.push(el)
    }
  }

  const unobserve = (target: Element): void => {
    teardownElement(target)
  }

  const disconnect = (): void => {
    for (const el of observed) teardownElement(el)
    observed.length = 0
  }

  onUnmounted(disconnect)

  return { observe, unobserve, disconnect }
}

/**
 * Vue 指令 API — 模板里用 v-scroll-reveal。
 *
 * 绑定值可以是选项对象，也可以是方向字符串（'reveal-left' 之类的简写）。
 */
export const vScrollReveal: Directive<Element, ScrollRevealOptions | string | undefined> = {
  mounted(el, binding) {
    const raw = binding.value
    const options: ResolvedOptions =
      typeof raw === 'string'
        ? { ...DEFAULT_OPTIONS, variant: raw as RevealVariant }
        : { ...DEFAULT_OPTIONS, ...(raw ?? {}) }

    // 标记给 stagger 计算用：同级里只有带标记的元素参与排序。
    el.setAttribute('data-scroll-reveal', '')
    setupElement(el, options)
  },
  unmounted(el) {
    teardownElement(el)
    el.removeAttribute('data-scroll-reveal')
  }
}
