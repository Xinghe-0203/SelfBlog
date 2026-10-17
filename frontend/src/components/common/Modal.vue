<script setup lang="ts">
import { computed, ref } from 'vue'
import { overlayTarget, useDialog } from '@/composables/useDialog'
import Icon from './Icon.vue'

const props = defineProps<{ show?: boolean; title?: string; width?: string; closable?: boolean }>()
const emit = defineEmits<{ close: [] }>()
const panel = ref<HTMLElement | null>(null)
const { layer } = useDialog(
  computed(() => props.show === true),
  panel,
  () => {
    if (props.closable) emit('close')
  }
)
</script>

<template>
  <Teleport :to="overlayTarget()">
    <Transition name="modal" @before-leave="$event.setAttribute('inert', '')">
      <div
        v-if="show"
        class="modal-overlay"
        :style="{ zIndex: `calc(var(--z-modal) + ${layer})` }"
        @click.self="closable && emit('close')"
      >
        <div
          ref="panel"
          class="modal-content"
          :style="{ maxWidth: width }"
          role="dialog"
          aria-modal="true"
          :aria-label="title || '对话框'"
          tabindex="-1"
        >
          <header class="modal-header">
            <h3>{{ title }}</h3>
            <button
              v-if="closable"
              type="button"
              class="modal-close"
              aria-label="关闭"
              @click="emit('close')"
            >
              <Icon name="close" :size="20" />
            </button>
          </header>
          <div class="modal-body"><slot /></div>
          <footer v-if="$slots.footer" class="modal-footer"><slot name="footer" /></footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--scrim);
}
.modal-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 580px;
  max-height: min(85dvh, 880px);
  overflow: hidden;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-2xl);
  background: var(--surface-primary);
  color: var(--text-primary);
  box-shadow: var(--shadow-xl);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
}
.modal-header h3 {
  font-size: 17px;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.modal-close {
  display: grid;
  flex: 0 0 44px;
  place-items: center;
  width: 44px;
  height: 44px;
  margin: -4px -10px -4px 0;
  border-radius: var(--radius);
  color: var(--text-secondary);
}
.modal-close:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}
.modal-body {
  flex: 1;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
  overscroll-behavior: contain;
}
.modal-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid var(--border-light);
  background: var(--surface-secondary);
}
.modal-enter-active,
.modal-leave-active {
  transition: opacity 180ms ease;
}
.modal-enter-active .modal-content,
.modal-leave-active .modal-content {
  transition: transform 220ms var(--ease-out);
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: translateY(8px);
}
@media (max-width: 640px) {
  .modal-overlay {
    padding: 12px;
  }
  .modal-content {
    max-height: 90dvh;
  }
  .modal-header,
  .modal-footer {
    padding-inline: 16px;
  }
  .modal-body {
    padding: 16px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .modal-enter-from .modal-content,
  .modal-leave-to .modal-content {
    transform: none;
  }
}
</style>
