<script setup lang="ts">
/**
 * Tag.vue — Muted Pastel Tag
 *
 * A small-label tag using muted pastel color washes with uppercase,
 * wide-tracked typography. Designed to sit next to content without
 * competing for attention. Supports pill and square shapes, closable
 * action, and semantic color variants.
 *
 * Colors use desaturated pastel backgrounds (10% opacity of each hue)
 * with matching text at higher saturation — soft but legible.
 */

import { computed } from 'vue'

export interface TagProps {
  variant?: 'default' | 'primary' | 'accent' | 'success' | 'warning' | 'danger' | 'info' | 'blue' | 'purple' | 'pink'
  size?: 'sm' | 'md'
  shape?: 'pill' | 'square'
  closable?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<TagProps>(), {
  variant: 'default',
  size: 'sm',
  shape: 'pill',
  closable: false,
  disabled: false,
})

const emit = defineEmits<{
  close: []
  click: [event: MouseEvent]
}>()

const classes = computed(() => [
  'tag',
  `tag--${props.variant}`,
  `tag--${props.size}`,
  `tag--${props.shape}`,
  {
    'tag--closable': props.closable,
    'tag--disabled': props.disabled,
  },
])

const handleClose = (e: MouseEvent) => {
  e.stopPropagation()
  if (!props.disabled) {
    emit('close')
  }
}

const handleClick = (e: MouseEvent) => {
  if (!props.disabled) {
    emit('click', e)
  }
}
</script>

<template>
  <span
    :class="classes"
    role="status"
    @click="handleClick"
  >
    <span class="tag__content">
      <slot />
    </span>

    <button
      v-if="closable"
      class="tag__close"
      type="button"
      :aria-label="'Remove tag'"
      :disabled="disabled"
      @click="handleClose"
    >
      <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
        <path d="M2 2l6 6M8 2l-6 6" />
      </svg>
    </button>
  </span>
</template>

<style scoped>
/* ==================== Base ==================== */
.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-sans);
  font-weight: var(--font-medium, 500);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  white-space: nowrap;
  cursor: default;
  transition:
    background var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    color var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    border-color var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    box-shadow var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1));
}

.tag--closable {
  cursor: pointer;
}

/* ==================== Shapes ==================== */
.tag--pill {
  border-radius: var(--radius-full, 9999px);
}

.tag--square {
  border-radius: var(--radius-xs, 4px);
}

/* ==================== Sizes ==================== */
.tag--sm {
  padding: 3px 10px;
  font-size: 0.625rem;
  line-height: 1.4;
}

.tag--md {
  padding: 4px 12px;
  font-size: 0.6875rem;
  line-height: 1.5;
}

/* ==================== Close button ==================== */
.tag__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  opacity: 0.5;
  cursor: pointer;
  border-radius: var(--radius-full, 9999px);
  flex-shrink: 0;
  transition: opacity var(--duration-fast, 100ms), background var(--duration-fast, 100ms);
}

.tag__close:hover {
  opacity: 1;
  background: rgba(0, 0, 0, 0.06);
}

.tag__close:disabled {
  cursor: not-allowed;
}

/* ==================== Disabled ==================== */
.tag--disabled {
  opacity: 0.45;
  cursor: not-allowed;
  pointer-events: none;
}

/* ==================== DEFAULT — Neutral warm gray ==================== */
.tag--default {
  background: var(--bg-secondary, #F5F5F4);
  color: var(--text-secondary, #57534E);
  border: 1px solid var(--border, rgba(0, 0, 0, 0.06));
}

/* ==================== PRIMARY — Teal pastel ==================== */
.tag--primary {
  background: rgba(13, 148, 136, 0.08);
  color: #0F766E;
  border: 1px solid rgba(13, 148, 136, 0.15);
}

.tag--closable.tag--primary:hover {
  background: rgba(13, 148, 136, 0.14);
}

/* ==================== ACCENT — Coral pastel ==================== */
.tag--accent {
  background: rgba(249, 115, 22, 0.08);
  color: #C2410C;
  border: 1px solid rgba(249, 115, 22, 0.15);
}

.tag--closable.tag--accent:hover {
  background: rgba(249, 115, 22, 0.14);
}

/* ==================== SUCCESS — Emerald pastel ==================== */
.tag--success {
  background: rgba(5, 150, 105, 0.08);
  color: #047857;
  border: 1px solid rgba(5, 150, 105, 0.15);
}

.tag--closable.tag--success:hover {
  background: rgba(5, 150, 105, 0.14);
}

/* ==================== WARNING — Amber pastel ==================== */
.tag--warning {
  background: rgba(217, 119, 6, 0.08);
  color: #B45309;
  border: 1px solid rgba(217, 119, 6, 0.15);
}

.tag--closable.tag--warning:hover {
  background: rgba(217, 119, 6, 0.14);
}

/* ==================== DANGER — Red pastel ==================== */
.tag--danger {
  background: rgba(220, 38, 38, 0.08);
  color: #B91C1C;
  border: 1px solid rgba(220, 38, 38, 0.15);
}

.tag--closable.tag--danger:hover {
  background: rgba(220, 38, 38, 0.14);
}

/* ==================== INFO — Blue pastel ==================== */
.tag--info {
  background: rgba(37, 99, 235, 0.08);
  color: #1D4ED8;
  border: 1px solid rgba(37, 99, 235, 0.15);
}

.tag--closable.tag--info:hover {
  background: rgba(37, 99, 235, 0.14);
}

/* ==================== BLUE — Sky pastel ==================== */
.tag--blue {
  background: rgba(59, 130, 246, 0.08);
  color: #2563EB;
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.tag--closable.tag--blue:hover {
  background: rgba(59, 130, 246, 0.14);
}

/* ==================== PURPLE — Violet pastel ==================== */
.tag--purple {
  background: rgba(139, 92, 246, 0.08);
  color: #7C3AED;
  border: 1px solid rgba(139, 92, 246, 0.15);
}

.tag--closable.tag--purple:hover {
  background: rgba(139, 92, 246, 0.14);
}

/* ==================== PINK — Rose pastel ==================== */
.tag--pink {
  background: rgba(236, 72, 153, 0.08);
  color: #DB2777;
  border: 1px solid rgba(236, 72, 153, 0.15);
}

.tag--closable.tag--pink:hover {
  background: rgba(236, 72, 153, 0.14);
}
</style>
