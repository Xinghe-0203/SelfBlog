<template>
  <button
    class="back-to-top"
    :class="{ visible: isVisible }"
    @click="scrollToTop"
    aria-label="返回顶部"
  >
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <polyline points="18 15 12 9 6 15"/>
    </svg>
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const isVisible = ref(false)
let ticking = false

const handleScroll = () => {
  if (!ticking) {
    requestAnimationFrame(() => {
      isVisible.value = window.scrollY > 300
      ticking = false
    })
    ticking = true
  }
}

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
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
  background: var(--surface-solid);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-full);
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
    width: 40px;
    height: 40px;
  }
}

@media (max-width: 480px) {
  .back-to-top {
    bottom: 16px;
    right: 16px;
    width: 38px;
    height: 38px;
  }
}
</style>
