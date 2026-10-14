<script setup lang="ts">
import { watch, onBeforeUnmount } from 'vue'

let bodyOverflowCount = 0

const props = defineProps<{
  show?: boolean
  title?: string
  width?: string
  closable?: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const handleKeydown = (e: KeyboardEvent): void => {
  if (e.key === 'Escape' && props.closable && props.show) {
    emit('close')
  }
}

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

onBeforeUnmount(() => {
  bodyOverflowCount = Math.max(0, bodyOverflowCount - 1)
  if (bodyOverflowCount === 0) {
    document.body.style.overflow = ''
  }
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="show" class="modal-overlay" role="dialog" aria-modal="true" @click.self="closable && emit('close')">
        <div class="modal-content" :style="{ maxWidth: width }">
          <div class="modal-header">
            <h3>{{ title }}</h3>
            <button v-if="closable" class="modal-close" aria-label="关闭" @click="emit('close')">&times;</button>
          </div>
          <div class="modal-body"><slot /></div>
          <div v-if="$slots.footer" class="modal-footer"><slot name="footer" /></div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: var(--z-modal);
}

.modal-content {
  width: 90%;
  max-width: 580px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg) var(--spacing-xl) 0;
  border-bottom: 1px solid var(--glass-border);
}

.modal-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  background: transparent;
  border: 1px solid transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: var(--radius);
  transition: all var(--transition);
}

.modal-close:hover {
  background: var(--error-light);
  color: var(--error);
  border-color: var(--error-light);
  transform: rotate(90deg);
}

.modal-body {
  padding: var(--spacing-lg) var(--spacing-xl);
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-xl);
  border-top: 1px solid var(--glass-border);
  background: var(--primary-light);
}

.modal-enter-active {
  transition: opacity var(--duration-normal) ease;
}

.modal-leave-active {
  transition: opacity var(--duration-normal) ease;
}

.modal-enter-active .modal-content {
  transition: transform var(--duration-slow) cubic-bezier(0.34, 1.56, 0.64, 1), opacity var(--duration-normal) ease;
}

.modal-leave-active .modal-content {
  transition: transform var(--duration-normal) ease, opacity var(--duration-normal) ease;
}

.modal-enter-from {
  opacity: 0;
}

.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content {
  transform: scale(0.92) translateY(20px);
  opacity: 0;
}

.modal-leave-to .modal-content {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}

@media (max-width: 640px) {
  .modal-overlay {
    padding: var(--spacing-sm);
    align-items: flex-end;
  }

  .modal-content {
    width: 100%;
    max-height: 90vh;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  }

  .modal-header {
    padding: var(--spacing-md) var(--spacing-lg) 0;
  }

  .modal-body {
    padding: var(--spacing-md) var(--spacing-lg);
  }

  .modal-footer {
    padding: var(--spacing-sm) var(--spacing-lg);
  }
}
</style>
