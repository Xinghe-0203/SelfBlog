<template>
  <button
    type="button"
    class="back-to-top functional-glass"
    :class="{ visible: isVisible }"
    @click="scrollToTop"
    aria-label="返回顶部"
  >
    <Icon name="arrow-up" :size="19" />
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import Icon from './Icon.vue'

const isVisible = ref(false)
let ticking = false
let frame = 0

const handleScroll = () => {
  if (!ticking) {
    frame = requestAnimationFrame(() => {
      frame = 0
      isVisible.value = window.scrollY > 300
      ticking = false
    })
    ticking = true
  }
}

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  // 卸载前排队的帧仍会执行并写入已卸载组件的 ref，必须取消。
  if (frame) {
    cancelAnimationFrame(frame)
    frame = 0
  }
  ticking = false
})
</script>

<style scoped>
.back-to-top {
  position: fixed;
  bottom: 28px;
  right: 28px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-secondary);
  cursor: pointer;
  opacity: 0;
  visibility: hidden;
  transform: translateY(8px) scale(0.9);
  transition: all var(--transition-spring);
  z-index: var(--z-sticky);
  box-shadow: var(--shadow-md);
}

.back-to-top.visible {
  opacity: 1;
  visibility: visible;
  transform: translateY(0) scale(1);
}

.back-to-top:hover {
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  color: white;
  border-color: transparent;
  transform: translateY(-3px) scale(1.05);
  box-shadow: var(--shadow-lg), var(--shadow-glow-primary);
}

.back-to-top:active {
  transform: translateY(0) scale(0.97);
}

.back-to-top svg {
  transition: transform var(--transition);
}

.back-to-top:hover svg {
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .back-to-top {
    bottom: 20px;
    right: 20px;
  }
}

@media (max-width: 480px) {
  .back-to-top {
    bottom: 16px;
    right: 16px;
  }
}
</style>
