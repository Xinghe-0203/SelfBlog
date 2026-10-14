<script setup lang="ts">
/**
 * Card.vue — Borderless Island Card
 *
 * A borderless card that uses subtle background differentiation and
 * minimal shadow to create a "floating island" effect. On hover, a
 * slightly deeper shadow lifts the card — tactile and restrained.
 *
 * The card has three zones: header, body (default slot), and footer.
 * All are optional. Padding and rounding are controlled by size prop.
 *
 * Variants: default | flat | elevated | frosted
 * Sizes:    sm | md | lg
 */

import { computed } from 'vue'

export interface CardProps {
  variant?: 'default' | 'flat' | 'elevated' | 'frosted'
  size?: 'sm' | 'md' | 'lg'
  hoverable?: boolean
  padding?: boolean
  tag?: string
}

const props = withDefaults(defineProps<CardProps>(), {
  variant: 'default',
  size: 'md',
  hoverable: false,
  padding: true,
  tag: 'div',
})

const classes = computed(() => [
  'card',
  `card--${props.variant}`,
  `card--${props.size}`,
  {
    'card--hoverable': props.hoverable,
    'card--no-pad': !props.padding,
  },
])
</script>

<template>
  <component :is="tag" :class="classes">
    <!-- Header -->
    <div v-if="$slots.header" class="card__header">
      <slot name="header" />
    </div>

    <!-- Body (default slot) -->
    <div v-if="$slots.default" class="card__body">
      <slot />
    </div>

    <!-- Footer -->
    <div v-if="$slots.footer" class="card__footer">
      <slot name="footer" />
    </div>
  </component>
</template>

<style scoped>
/* ==================== Base Card ==================== */
.card {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg, 16px);
  overflow: hidden;
  transition:
    box-shadow var(--duration-slow, 350ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    transform var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    background var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1));
}

/* ==================== Sizes ==================== */
.card--sm .card__header,
.card--sm .card__body,
.card--sm .card__footer {
  padding: var(--spacing-sm, 0.5rem) var(--spacing-md, 1rem);
}

.card--md .card__header,
.card--md .card__body,
.card--md .card__footer {
  padding: var(--spacing-md, 1rem) var(--spacing-lg, 1.5rem);
}

.card--lg .card__header,
.card--lg .card__body,
.card--lg .card__footer {
  padding: var(--spacing-lg, 1.5rem) var(--spacing-xl, 2rem);
}

.card--no-pad .card__header,
.card--no-pad .card__body,
.card--no-pad .card__footer {
  padding: 0;
}

/* ==================== Header ==================== */
.card__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm, 0.5rem);
  border-bottom: 1px solid var(--border, rgba(0, 0, 0, 0.06));
  flex-shrink: 0;
}

/* ==================== Body ==================== */
.card__body {
  flex: 1;
  min-width: 0;
}

/* ==================== Footer ==================== */
.card__footer {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm, 0.5rem);
  border-top: 1px solid var(--border, rgba(0, 0, 0, 0.06));
  flex-shrink: 0;
}

/* ==================== DEFAULT — Subtle warm white, no border ==================== */
.card--default {
  background: rgba(255, 255, 255, 0.55);
  border: none;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.03),
    0 1px 2px rgba(0, 0, 0, 0.02);
}

.card--default.card--hoverable:hover {
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.05),
    0 2px 4px rgba(0, 0, 0, 0.03);
  transform: translateY(-2px);
}

/* ==================== FLAT — Solid surface, thin border ==================== */
.card--flat {
  background: var(--surface-solid, #FFFFFF);
  border: 1px solid var(--border-solid, #E7E5E4);
  box-shadow: none;
}

.card--flat.card--hoverable:hover {
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.05),
    0 2px 4px rgba(0, 0, 0, 0.03);
  transform: translateY(-2px);
}

/* ==================== ELEVATED — Shadow, no border ==================== */
.card--elevated {
  background: var(--surface-solid, #FFFFFF);
  border: none;
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.05),
    0 2px 4px rgba(0, 0, 0, 0.03);
}

.card--elevated.card--hoverable:hover {
  box-shadow:
    0 15px 30px rgba(0, 0, 0, 0.10),
    0 5px 15px rgba(0, 0, 0, 0.04);
  transform: translateY(-4px);
}

/* ==================== FROSTED — Glass morphism ==================== */
.card--frosted {
  background: var(--glass-bg, rgba(255, 255, 255, 0.78));
  backdrop-filter: var(--glass-blur, blur(12px));
  -webkit-backdrop-filter: var(--glass-blur, blur(12px));
  border: 1px solid var(--glass-border-frost, rgba(200, 200, 210, 0.30));
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.card--frosted.card--hoverable:hover {
  background: var(--glass-hover, rgba(255, 255, 255, 0.92));
  box-shadow:
    0 15px 30px rgba(0, 0, 0, 0.10),
    0 5px 15px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
}

/* ==================== Responsive ==================== */
@media (max-width: 640px) {
  .card--md .card__header,
  .card--md .card__body,
  .card--md .card__footer {
    padding: var(--spacing-sm, 0.5rem) var(--spacing-md, 1rem);
  }

  .card--lg .card__header,
  .card--lg .card__body,
  .card--lg .card__footer {
    padding: var(--spacing-md, 1rem) var(--spacing-lg, 1.5rem);
  }
}
</style>
