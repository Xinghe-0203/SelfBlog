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
    // 先解锁再读值：解锁放在这里，同一帧内后续的 scroll 事件会被 ticking 挡掉，
    // 直到下一帧才重新调度 —— 这才是"每帧最多处理一次"。
    ticking = false
    const y = window.scrollY
    scrolled.value = y > threshold
    // 只在真正发生位移时改方向：静止时保持上一次的方向。
    // 未过阈值不记 down —— 此时顶栏不会隐藏，方向保持中性的 up 更稳。
    if (y > threshold && y > lastY) direction.value = 'down'
    else if (y < lastY) direction.value = 'up'
    lastY = y
  }

  /** 已排队的帧 id；0 表示没有待处理的帧。 */
  let frame = 0

  const onScroll = (): void => {
    if (ticking) return
    ticking = true
    frame = requestAnimationFrame(() => {
      frame = 0
      update()
    })
  }

  onMounted(() => {
    lastY = window.scrollY
    update()
    window.addEventListener('scroll', onScroll, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll)
    // 卸载时可能还有一帧没跑：不取消就会在组件销毁后继续读 scrollY 并写 ref。
    if (frame) {
      cancelAnimationFrame(frame)
      frame = 0
    }
    ticking = false
  })

  return { scrolled, direction }
}
