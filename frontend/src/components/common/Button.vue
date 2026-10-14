<script setup lang="ts">
/**
 * Button.vue — Nested CTA & Island Button
 *
 * A high-contrast, tactile button component following the "island" metaphor:
 * each button is a self-contained interactive surface with distinct elevation,
 * shadow, and press-feedback. The primary variant uses a near-black (#111111)
 * background to create maximum visual weight against the frosted campus palette.
 *
 * Variants: primary | secondary | ghost | outline | danger | success | accent
 * Sizes:    sm | md | lg
 */

import { computed } from 'vue'

export interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger' | 'success' | 'accent'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
  block?: boolean
  icon?: boolean
  tag?: string
  type?: 'button' | 'submit' | 'reset'
  href?: string
}

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
  block: false,
  icon: false,
  tag: 'button',
  type: 'button',
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const classes = computed(() => [
  'btn',
  `btn--${props.variant}`,
  `btn--${props.size}`,
  {
    'btn--block': props.block,
    'btn--icon': props.icon,
    'btn--loading': props.loading,
    'btn--disabled': props.disabled,
  },
])

const is = computed(() => {
  if (props.href) return 'a'
  return props.tag
})

const handleClick = (e: MouseEvent) => {
  if (props.disabled || props.loading) {
    e.preventDefault()
    return
  }
  emit('click', e)
}
</script>

<template>
  <component
    :is="is"
    :class="classes"
    :disabled="is === 'button' ? disabled || loading : undefined"
    :href="href"
    :type="is === 'button' ? type : undefined"
    :role="is === 'a' ? 'button' : undefined"
    :aria-disabled="disabled || loading ? 'true' : undefined"
    :aria-busy="loading ? 'true' : undefined"
    @click="handleClick"
  >
    <!-- Loading spinner -->
    <span v-if="loading" class="btn__spinner" aria-hidden="true">
      <svg class="btn__spinner-svg" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-dasharray="56.5" stroke-dashoffset="16" />
      </svg>
    </span>

    <!-- Icon slot (left) -->
    <span v-if="$slots.icon && !loading" class="btn__icon-wrap" aria-hidden="true">
      <slot name="icon" />
    </span>

    <!-- Default content -->
    <span class="btn__content" :class="{ 'btn__content--hidden': loading }">
      <slot />
    </span>
  </component>
</template>

<style scoped>
/* ==================== Base Island ==================== */
.btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-family: var(--font-sans);
  font-weight: var(--font-medium, 500);
  line-height: 1;
  white-space: nowrap;
  user-select: none;
  cursor: pointer;
  border: 1.5px solid transparent;
  border-radius: var(--radius, 8px);
  text-decoration: none;
  isolation: isolate;
  transition:
    background var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    color var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    border-color var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    box-shadow var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    transform var(--duration-fast, 100ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1));
}

.btn:focus-visible {
  outline: 2px solid var(--primary, #0D9488);
  outline-offset: 2px;
}

/* ==================== Sizes ==================== */
.btn--sm {
  padding: 7px 14px;
  font-size: var(--text-xs, 0.75rem);
  border-radius: var(--radius-sm, 6px);
  gap: 6px;
}

.btn--md {
  padding: 10px 20px;
  font-size: var(--text-sm, 0.8125rem);
  gap: 8px;
}

.btn--lg {
  padding: 13px 28px;
  font-size: var(--text-base, 0.9375rem);
  border-radius: var(--radius-md, 12px);
  gap: 10px;
}

/* ==================== Icon-only button ==================== */
.btn--icon.btn--sm { padding: 7px; }
.btn--icon.btn--md { padding: 10px; }
.btn--icon.btn--lg { padding: 13px; }

/* ==================== Block ==================== */
.btn--block {
  display: flex;
  width: 100%;
}

/* ==================== Disabled ==================== */
.btn--disabled,
.btn[disabled] {
  opacity: 0.45;
  cursor: not-allowed;
  pointer-events: none;
}

/* ==================== Loading ==================== */
.btn--loading {
  cursor: wait;
  pointer-events: none;
}

/* ==================== Spinner ==================== */
.btn__spinner {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn__spinner-svg {
  width: 1em;
  height: 1em;
  animation: btn-spin 0.7s linear infinite;
}

@keyframes btn-spin {
  to { transform: rotate(360deg); }
}

.btn__content--hidden {
  visibility: hidden;
}

/* ==================== Icon wrapper ==================== */
.btn__icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 1em;
  height: 1em;
}

.btn__icon-wrap :deep(svg) {
  width: 1em;
  height: 1em;
}

/* ==================== PRIMARY — Near-black island ==================== */
.btn--primary {
  background: #111111;
  color: #FFFFFF;
  border-color: #111111;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.20),
    0 4px 8px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.btn--primary:hover:not(:disabled) {
  background: #333333;
  border-color: #333333;
  box-shadow:
    0 2px 4px rgba(0, 0, 0, 0.20),
    0 8px 16px rgba(0, 0, 0, 0.10),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

.btn--primary:active:not(:disabled) {
  background: #0A0A0A;
  border-color: #0A0A0A;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.30),
    inset 0 1px 3px rgba(0, 0, 0, 0.15);
  transform: scale(0.98) translateY(0);
}

/* ==================== SECONDARY — Frosted glass island ==================== */
.btn--secondary {
  background: var(--glass-bg, rgba(255, 255, 255, 0.78));
  backdrop-filter: var(--glass-blur, blur(12px));
  -webkit-backdrop-filter: var(--glass-blur, blur(12px));
  color: var(--text-primary, #292524);
  border-color: var(--glass-border-frost, rgba(200, 200, 210, 0.30));
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 4px 12px rgba(0, 0, 0, 0.03),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.btn--secondary:hover:not(:disabled) {
  background: var(--glass-hover, rgba(255, 255, 255, 0.92));
  border-color: var(--primary, #0D9488);
  color: var(--primary, #0D9488);
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.06),
    0 8px 20px rgba(0, 0, 0, 0.04),
    0 0 0 1px rgba(13, 148, 136, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transform: translateY(-1px);
}

.btn--secondary:active:not(:disabled) {
  background: var(--glass-bg-heavy, rgba(255, 255, 255, 0.90));
  box-shadow: var(--shadow-inner, inset 0 2px 4px rgba(0, 0, 0, 0.04));
  transform: scale(0.98) translateY(0);
}

/* ==================== GHOST — Transparent surface ==================== */
.btn--ghost {
  background: transparent;
  color: var(--text-secondary, #57534E);
  border-color: transparent;
}

.btn--ghost:hover:not(:disabled) {
  background: var(--primary-light, rgba(13, 148, 136, 0.08));
  color: var(--primary, #0D9488);
}

.btn--ghost:active:not(:disabled) {
  background: rgba(13, 148, 136, 0.14);
  transform: scale(0.98);
}

/* ==================== OUTLINE — Border only, fill on hover ==================== */
.btn--outline {
  background: transparent;
  color: var(--primary, #0D9488);
  border-color: var(--primary, #0D9488);
}

.btn--outline:hover:not(:disabled) {
  background: var(--primary, #0D9488);
  color: #FFFFFF;
  box-shadow:
    0 2px 6px rgba(13, 148, 136, 0.20),
    0 8px 20px rgba(13, 148, 136, 0.10);
  transform: translateY(-1px);
}

.btn--outline:active:not(:disabled) {
  background: var(--primary-active, #115E59);
  border-color: var(--primary-active, #115E59);
  box-shadow: none;
  transform: scale(0.98) translateY(0);
}

/* ==================== DANGER ==================== */
.btn--danger {
  background: var(--error, #DC2626);
  color: #FFFFFF;
  border-color: var(--error, #DC2626);
  box-shadow:
    0 1px 2px rgba(220, 38, 38, 0.15),
    0 4px 8px rgba(220, 38, 38, 0.08);
}

.btn--danger:hover:not(:disabled) {
  background: var(--error-hover, #B91C1C);
  border-color: var(--error-hover, #B91C1C);
  box-shadow:
    0 2px 6px rgba(220, 38, 38, 0.20),
    0 8px 20px rgba(220, 38, 38, 0.12);
  transform: translateY(-1px);
}

.btn--danger:active:not(:disabled) {
  transform: scale(0.98) translateY(0);
}

/* ==================== SUCCESS ==================== */
.btn--success {
  background: var(--success, #059669);
  color: #FFFFFF;
  border-color: var(--success, #059669);
  box-shadow:
    0 1px 2px rgba(5, 150, 105, 0.15),
    0 4px 8px rgba(5, 150, 105, 0.08);
}

.btn--success:hover:not(:disabled) {
  background: var(--success-hover, #047857);
  border-color: var(--success-hover, #047857);
  box-shadow:
    0 2px 6px rgba(5, 150, 105, 0.20),
    0 8px 20px rgba(5, 150, 105, 0.12);
  transform: translateY(-1px);
}

.btn--success:active:not(:disabled) {
  transform: scale(0.98) translateY(0);
}

/* ==================== ACCENT — Coral island ==================== */
.btn--accent {
  background: var(--accent, #F97316);
  color: #FFFFFF;
  border-color: var(--accent, #F97316);
  box-shadow:
    0 1px 2px rgba(249, 115, 22, 0.15),
    0 4px 8px rgba(249, 115, 22, 0.08);
}

.btn--accent:hover:not(:disabled) {
  background: var(--accent-hover, #EA580C);
  border-color: var(--accent-hover, #EA580C);
  box-shadow:
    0 2px 6px rgba(249, 115, 22, 0.20),
    0 8px 20px rgba(249, 115, 22, 0.12);
  transform: translateY(-1px);
}

.btn--accent:active:not(:disabled) {
  transform: scale(0.98) translateY(0);
}

/* ==================== Responsive ==================== */
@media (max-width: 640px) {
  .btn--lg {
    padding: 11px 22px;
    font-size: var(--text-sm, 0.8125rem);
  }
}
</style>
