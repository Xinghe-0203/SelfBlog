<script setup lang="ts">
/**
 * Input.vue — Island Input
 *
 * A refined text input with floating label aesthetic, subtle focus glow,
 * and clear error/success states. The border uses a muted 1px stroke
 * (#EAEAEA) that transitions to the primary teal on focus with a soft
 * concentric shadow — a "lit island" effect.
 *
 * Features:
 * - Label positioned above with small uppercase tracking
 * - Focus state: primary border + faint outer glow
 * - Error state: red border + error message below
 * - Success state: green border
 * - Prefix/suffix slots for icons or adornments
 * - Sizes: sm | md | lg
 */

import { computed, ref } from 'vue'

export interface InputProps {
  modelValue?: string | number
  type?: string
  label?: string
  placeholder?: string
  error?: string
  success?: string
  hint?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  size?: 'sm' | 'md' | 'lg'
  id?: string
  name?: string
  autocomplete?: string
  maxlength?: number
}

const props = withDefaults(defineProps<InputProps>(), {
  modelValue: '',
  type: 'text',
  placeholder: '',
  disabled: false,
  readonly: false,
  required: false,
  size: 'md',
  autocomplete: 'off'
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
  keydown: [event: KeyboardEvent]
}>()

const isFocused = ref(false)

const inputId = computed(() => props.id || `input-${Math.random().toString(36).slice(2, 9)}`)

const wrapperClasses = computed(() => [
  'input-wrap',
  `input-wrap--${props.size}`,
  {
    'input-wrap--focused': isFocused.value,
    'input-wrap--error': !!props.error,
    'input-wrap--success': !!props.success && !props.error,
    'input-wrap--disabled': props.disabled,
    'input-wrap--readonly': props.readonly,
    'input-wrap--has-value': props.modelValue !== '' && props.modelValue !== undefined
  }
])

const handleInput = (e: Event) => {
  const target = e.target as HTMLInputElement
  emit('update:modelValue', props.type === 'number' ? Number(target.value) : target.value)
}

const handleFocus = (e: FocusEvent) => {
  isFocused.value = true
  emit('focus', e)
}

const handleBlur = (e: FocusEvent) => {
  isFocused.value = false
  emit('blur', e)
}

const handleKeydown = (e: KeyboardEvent) => {
  emit('keydown', e)
}
</script>

<template>
  <div :class="wrapperClasses">
    <!-- Label -->
    <label v-if="label" :for="inputId" class="input-label">
      {{ label }}
      <span v-if="required" class="input-label__required" aria-hidden="true">*</span>
    </label>

    <!-- Input container -->
    <div class="input-container">
      <!-- Prefix slot -->
      <span v-if="$slots.prefix" class="input-adornment input-adornment--prefix">
        <slot name="prefix" />
      </span>

      <input
        :id="inputId"
        :type="type"
        :value="modelValue"
        :name="name"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :required="required"
        :autocomplete="autocomplete"
        :maxlength="maxlength"
        :aria-invalid="!!error"
        :aria-describedby="error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined"
        class="input-field"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />

      <!-- Suffix slot -->
      <span v-if="$slots.suffix" class="input-adornment input-adornment--suffix">
        <slot name="suffix" />
      </span>
    </div>

    <!-- Messages -->
    <div class="input-messages">
      <p
        v-if="error"
        :id="`${inputId}-error`"
        class="input-message input-message--error"
        role="alert"
      >
        {{ error }}
      </p>
      <p v-else-if="success" class="input-message input-message--success">
        {{ success }}
      </p>
      <p v-else-if="hint" :id="`${inputId}-hint`" class="input-message input-message--hint">
        {{ hint }}
      </p>
    </div>
  </div>
</template>

<style scoped>
/* ==================== Wrapper ==================== */
.input-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

/* ==================== Label ==================== */
.input-label {
  display: flex;
  align-items: center;
  gap: 2px;
  font-family: var(--font-sans);
  font-size: var(--text-2xs);
  font-weight: var(--font-medium, 500);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-secondary, #57534e);
  transition: color var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1));
  cursor: default;
}

.input-label__required {
  color: var(--error, #dc2626);
  font-size: var(--text-xs);
  line-height: 1;
}

.input-wrap--focused .input-label {
  color: var(--primary, #165dff);
}

.input-wrap--error .input-label {
  color: var(--error, #dc2626);
}

/* ==================== Container ==================== */
.input-container {
  position: relative;
  display: flex;
  align-items: center;
}

/* ==================== Field ==================== */
.input-field {
  width: 100%;
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  line-height: var(--leading-normal, 1.6);
  color: var(--text-primary, #292524);
  background: var(--surface-solid, #ffffff);
  border: 1px solid #eaeaea;
  border-radius: var(--radius, 8px);
  outline: none;
  transition:
    border-color var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    box-shadow var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1)),
    background var(--duration-normal, 200ms) var(--ease-default, cubic-bezier(0.4, 0, 0.2, 1));
}

.input-field::placeholder {
  color: var(--text-muted, #a8a29e);
}

/* Sizes */
.input-wrap--sm .input-field {
  padding: 7px 12px;
  font-size: var(--text-xs);
  border-radius: var(--radius-sm, 6px);
}

.input-wrap--md .input-field {
  padding: 10px 14px;
}

.input-wrap--lg .input-field {
  padding: 13px 16px;
  font-size: var(--text-base);
  border-radius: var(--radius-md, 12px);
}

/* With adornments */
.input-wrap--sm .input-field {
  padding-left: 12px;
  padding-right: 12px;
}

.input-container:has(.input-adornment--prefix) .input-field {
  padding-left: 38px;
}

.input-container:has(.input-adornment--suffix) .input-field {
  padding-right: 38px;
}

/* ==================== Focus — Lit island glow ==================== */
.input-field:hover:not(:disabled):not(:focus) {
  border-color: #d4d4d4;
}

.input-field:focus {
  border-color: var(--primary, #165dff);
  box-shadow:
    0 0 0 3px rgba(22, 93, 255, 0.08),
    0 0 0 1px rgba(22, 93, 255, 0.12);
}

/* ==================== Error state ==================== */
.input-wrap--error .input-field {
  border-color: var(--error, #dc2626);
}

.input-wrap--error .input-field:focus {
  box-shadow:
    0 0 0 3px rgba(220, 38, 38, 0.08),
    0 0 0 1px rgba(220, 38, 38, 0.15);
}

/* ==================== Success state ==================== */
.input-wrap--success .input-field {
  border-color: var(--success, #00b42a);
}

.input-wrap--success .input-field:focus {
  box-shadow:
    0 0 0 3px rgba(0, 180, 42, 0.08),
    0 0 0 1px rgba(0, 180, 42, 0.12);
}

/* ==================== Disabled / Readonly ==================== */
.input-wrap--disabled .input-field {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--bg-secondary, #f5f5f4);
}

.input-wrap--readonly .input-field {
  background: var(--bg-secondary, #f5f5f4);
  cursor: default;
}

/* ==================== Adornments ==================== */
.input-adornment {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted, #a8a29e);
  pointer-events: none;
  z-index: 1;
}

.input-adornment--prefix {
  left: 12px;
}

.input-adornment--suffix {
  right: 12px;
  pointer-events: auto;
}

.input-adornment :deep(svg) {
  width: 16px;
  height: 16px;
}

/* ==================== Messages ==================== */
.input-messages {
  min-height: 0;
}

.input-message {
  font-family: var(--font-sans);
  font-size: var(--text-2xs);
  line-height: 1.4;
  margin: 0;
  animation: input-msg-enter 0.15s ease-out;
}

.input-message--error {
  color: var(--error, #dc2626);
}

.input-message--success {
  color: var(--success, #00b42a);
}

.input-message--hint {
  color: var(--text-muted, #a8a29e);
}

@keyframes input-msg-enter {
  from {
    opacity: 0;
    transform: translateY(-2px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
