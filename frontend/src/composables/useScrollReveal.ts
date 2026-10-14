/**
 * useScrollReveal — IntersectionObserver-based scroll reveal composable
 *
 * Adds a `visible` class to elements when they enter the viewport,
 * enabling CSS transition classes like `.fade-in-up`, `.fade-in-left`, etc.
 *
 * Usage:
 *   <div class="fade-in-up" v-scroll-reveal>...</div>
 *
 *   // or with options:
 *   <div class="fade-in-up" v-scroll-reveal="{ threshold: 0.2, rootMargin: '0px 0px -40px 0px' }">...</div>
 *
 *   // or as a composable in <script setup>:
 *   const { observe, unobserve, disconnect } = useScrollReveal()
 *   onMounted(() => observe(elRef.value))
 */

import { ref, onUnmounted, type Directive, type Ref } from 'vue'

export interface ScrollRevealOptions {
  /** IntersectionObserver threshold (0-1). Default: 0.1 */
  threshold?: number
  /** IntersectionObserver rootMargin. Default: '0px 0px -20px 0px' */
  rootMargin?: string
  /** Whether to reveal only once. Default: true */
  once?: boolean
}

const DEFAULT_OPTIONS: Required<ScrollRevealOptions> = {
  threshold: 0.1,
  rootMargin: '0px 0px -20px 0px',
  once: true
}

/**
 * Composable API — for programmatic use in <script setup>
 */
export function useScrollReveal(userOptions?: ScrollRevealOptions) {
  const options = { ...DEFAULT_OPTIONS, ...userOptions }
  const observerRef: Ref<IntersectionObserver | null> = ref(null)

  const getObserver = (): IntersectionObserver => {
    if (!observerRef.value) {
      observerRef.value = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('visible')
              // If once-only, stop observing after reveal
              if (options.once) {
                observerRef.value?.unobserve(entry.target)
              }
            } else if (!options.once) {
              // Re-hide when scrolling back out (only if !once)
              entry.target.classList.remove('visible')
            }
          })
        },
        {
          threshold: options.threshold,
          rootMargin: options.rootMargin
        }
      )
    }
    return observerRef.value
  }

  /** Observe a single element or a list of elements */
  const observe = (target: Element | Element[] | NodeList | null) => {
    if (!target) return
    const observer = getObserver()
    if (target instanceof Element) {
      observer.observe(target)
    } else {
      Array.from(target).forEach((el) => observer.observe(el as Element))
    }
  }

  /** Stop observing a specific element */
  const unobserve = (target: Element) => {
    observerRef.value?.unobserve(target)
  }

  /** Disconnect the entire observer (cleanup) */
  const disconnect = () => {
    observerRef.value?.disconnect()
    observerRef.value = null
  }

  onUnmounted(() => {
    disconnect()
  })

  return { observe, unobserve, disconnect }
}

/**
 * Vue directive API — for template use: v-scroll-reveal
 *
 * Automatically observes/unobserves on mount/unmount.
 * Pass options via binding value: v-scroll-reveal="{ threshold: 0.2 }"
 */
export const vScrollReveal: Directive<Element, ScrollRevealOptions | undefined> = {
  mounted(el, binding) {
    const opts = { ...DEFAULT_OPTIONS, ...binding.value }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible')
            if (opts.once) {
              observer.unobserve(entry.target)
            }
          } else if (!opts.once) {
            entry.target.classList.remove('visible')
          }
        })
      },
      {
        threshold: opts.threshold,
        rootMargin: opts.rootMargin
      }
    )
    // Store observer reference on element for cleanup
    ;(el as any).__scrollRevealObserver = observer
    observer.observe(el)
  },
  unmounted(el) {
    const observer = (el as any).__scrollRevealObserver as IntersectionObserver | undefined
    observer?.disconnect()
    delete (el as any).__scrollRevealObserver
  }
}
