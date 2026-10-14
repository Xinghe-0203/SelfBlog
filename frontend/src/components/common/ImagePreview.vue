<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps<{
  images?: string[]
  initialIndex?: number
  show?: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const currentIndex = ref(props.initialIndex ?? 0)

let bodyOverflowCount = 0

watch(
  () => props.show,
  (val) => {
    if (val) {
      bodyOverflowCount++
      if (bodyOverflowCount === 1) {
        document.body.style.overflow = 'hidden'
      }
      document.addEventListener('keydown', handleKeydown)
    } else {
      bodyOverflowCount = Math.max(0, bodyOverflowCount - 1)
      if (bodyOverflowCount === 0) {
        document.body.style.overflow = ''
      }
      document.removeEventListener('keydown', handleKeydown)
    }
  },
  { immediate: true }
)

const prev = (): void => {
  if (currentIndex.value > 0) currentIndex.value--
}
const next = (): void => {
  if (props.images && currentIndex.value < props.images.length - 1) currentIndex.value++
}

const handleKeydown = (e: KeyboardEvent): void => {
  if (e.key === 'Escape') emit('close')
  if (e.key === 'ArrowLeft') prev()
  if (e.key === 'ArrowRight') next()
}

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  bodyOverflowCount = Math.max(0, bodyOverflowCount - 1)
  if (bodyOverflowCount === 0) {
    document.body.style.overflow = ''
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="preview">
      <div v-if="show && images?.length" class="preview-overlay" role="dialog" aria-modal="true" @click.self="emit('close')" tabindex="0">
        <button class="preview-close" aria-label="关闭" @click="emit('close')">&times;</button>
        <button v-if="images.length > 1" class="preview-nav prev" aria-label="上一张" @click="prev">&#8249;</button>
        <img :src="images[currentIndex]" class="preview-img" @click.stop />
        <button v-if="images.length > 1" class="preview-nav next" aria-label="下一页" @click="next">&#8250;</button>
        <div class="preview-counter">{{ currentIndex + 1 }} / {{ images.length }}</div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.preview-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: var(--z-preview);
  cursor: zoom-out;
}

.preview-close {
  position: absolute;
  top: var(--spacing-lg);
  right: var(--spacing-lg);
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: white;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  cursor: pointer;
  z-index: 1;
  transition: all var(--transition);
}

.preview-close:hover {
  background: var(--error);
  border-color: var(--error);
  transform: rotate(90deg) scale(1.1);
}

.preview-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  color: white;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  cursor: pointer;
  z-index: 1;
  transition: all var(--transition);
}

.preview-nav:hover {
  background: var(--primary);
  border-color: var(--primary);
  transform: translateY(-50%) scale(1.1);
  box-shadow: var(--shadow-glow-primary);
}

.preview-nav.prev {
  left: var(--spacing-lg);
}

.preview-nav.next {
  right: var(--spacing-lg);
}

.preview-img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  border-radius: var(--radius-lg);
  cursor: default;
  box-shadow: var(--shadow-xl);
  transition: transform var(--transition-slow) cubic-bezier(0.34, 1.56, 0.64, 1);
}

.preview-img:hover {
  transform: scale(1.02);
}

.preview-counter {
  position: absolute;
  bottom: var(--spacing-xl);
  left: 50%;
  transform: translateX(-50%);
  padding: var(--spacing-sm) var(--spacing-lg);
  font-size: 0.875rem;
  color: white;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
}

.preview-enter-active {
  transition: opacity var(--duration-normal) ease;
}

.preview-leave-active {
  transition: opacity var(--duration-normal) ease;
}

.preview-enter-from {
  opacity: 0;
}

.preview-leave-to {
  opacity: 0;
}

.preview-enter-active .preview-img {
  animation: previewZoomIn var(--duration-slow) cubic-bezier(0.34, 1.56, 0.64, 1);
}

.preview-leave-active .preview-img {
  animation: previewZoomOut var(--duration-normal) ease forwards;
}

@keyframes previewZoomIn {
  from {
    transform: scale(0.8);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes previewZoomOut {
  from {
    transform: scale(1);
    opacity: 1;
  }
  to {
    transform: scale(0.9);
    opacity: 0;
  }
}

@media (max-width: 768px) {
  .preview-close {
    top: var(--spacing-sm);
    right: var(--spacing-sm);
    width: 40px;
    height: 40px;
    font-size: 1.5rem;
  }

  .preview-nav {
    width: 40px;
    height: 40px;
    font-size: 2rem;
  }

  .preview-nav.prev {
    left: var(--spacing-sm);
  }

  .preview-nav.next {
    right: var(--spacing-sm);
  }

  .preview-img {
    max-width: 95%;
    max-height: 85%;
    border-radius: var(--radius);
  }

  .preview-counter {
    bottom: var(--spacing-md);
    font-size: 0.75rem;
    padding: var(--spacing-xs) var(--spacing-md);
  }
}
</style>
