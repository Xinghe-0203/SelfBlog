<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{ value: number; duration?: number }>(), {
  duration: 900
})

/**
 * 数字滚动：把「文章 15」这种静态数字变成一个短暂的动效，
 * 让首屏的统计位不只是「摆在那儿的字」。
 *
 * 三条边界：
 * - **只动数字文本**，不改变布局宽度（用等宽数字 tabular-nums，避免抖动）；
 * - **尊重 reduced-motion**：系统要求减少动效时直接显示最终值；
 * - **环境降级**：没有 requestAnimationFrame / performance 的环境（jsdom、部分旧浏览器）
 *   直接显示最终值 —— 数字必须在任何环境下都是对的，动效只是锦上添花。
 */
const display = ref(0)
let frame = 0

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

/** 能否跑逐帧动画；不能就直接给最终值。 */
function canAnimate(): boolean {
  return (
    typeof requestAnimationFrame === 'function' &&
    typeof performance !== 'undefined' &&
    typeof performance.now === 'function'
  )
}

function animateTo(target: number): void {
  if (frame) {
    cancelAnimationFrame(frame)
    frame = 0
  }
  const value = Number(target) || 0
  if (!canAnimate() || prefersReducedMotion() || value <= 0) {
    display.value = value
    return
  }
  const start = performance.now()
  const step = (now: number): void => {
    const progress = Math.min(1, (now - start) / props.duration)
    // easeOutCubic：起步快、收尾稳，数字看起来是「停下来」而不是「被切断」。
    const eased = 1 - Math.pow(1 - progress, 3)
    display.value = Math.round(value * eased)
    if (progress < 1) frame = requestAnimationFrame(step)
    else frame = 0
  }
  frame = requestAnimationFrame(step)
}

watch(
  () => props.value,
  (value) => animateTo(Number(value) || 0),
  { immediate: true }
)

onUnmounted(() => {
  if (frame && typeof cancelAnimationFrame === 'function') cancelAnimationFrame(frame)
})
</script>

<template>
  <span class="count-up">{{ display }}</span>
</template>

<style scoped>
.count-up {
  /* 等宽数字：滚动过程中数字宽度不变，布局不会抖。 */
  font-variant-numeric: tabular-nums;
}
</style>
