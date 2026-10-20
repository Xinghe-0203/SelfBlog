<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { overlayTarget, useDialog } from '@/composables/useDialog'
import Icon from './Icon.vue'

const props = defineProps<{
  images?: string[]
  captions?: string[]
  initialIndex?: number
  show?: boolean
}>()
const emit = defineEmits<{ close: [] }>()
const panel = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
const open = computed(() => props.show === true && !!props.images?.length)
// Lightboxes visually outrank ordinary dialogs, so keyboard ownership does too.
const { isTop, layer } = useDialog(open, panel, () => emit('close'), 1)

watch(
  () => [props.show, props.initialIndex, props.images?.length] as const,
  () => {
    currentIndex.value = Math.max(
      0,
      Math.min(props.initialIndex || 0, (props.images?.length || 1) - 1)
    )
  },
  { immediate: true }
)

function prev(): void {
  if (currentIndex.value > 0) currentIndex.value--
}
function next(): void {
  if (currentIndex.value < (props.images?.length || 0) - 1) currentIndex.value++
}
function handleKeydown(event: KeyboardEvent): void {
  if (!open.value || !isTop()) return
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    prev()
  }
  if (event.key === 'ArrowRight') {
    event.preventDefault()
    next()
  }
}
document.addEventListener('keydown', handleKeydown)
onBeforeUnmount(() => document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <Teleport :to="overlayTarget()">
    <Transition name="preview" @before-leave="$event.setAttribute('inert', '')">
      <div
        v-if="open && images"
        ref="panel"
        class="preview-overlay"
        :style="{ zIndex: `calc(var(--z-preview) + ${layer})` }"
        role="dialog"
        aria-modal="true"
        aria-label="图片预览"
        tabindex="-1"
        @click.self="emit('close')"
      >
        <button type="button" class="preview-close" aria-label="关闭" @click="emit('close')">
          <Icon name="close" :size="22" />
        </button>
        <button
          v-if="images.length > 1"
          type="button"
          class="preview-nav prev"
          aria-label="上一张"
          :disabled="currentIndex === 0"
          @click="prev"
        >
          <Icon name="chevron-left" :size="24" />
        </button>
        <img
          :src="images[currentIndex]"
          class="preview-img"
          :alt="captions?.[currentIndex] || `图片 ${currentIndex + 1}`"
        />
        <button
          v-if="images.length > 1"
          type="button"
          class="preview-nav next"
          aria-label="下一张"
          :disabled="currentIndex >= images.length - 1"
          @click="next"
        >
          <Icon name="chevron-right" :size="24" />
        </button>
        <div class="preview-footer" aria-live="polite">
          <p v-if="captions?.[currentIndex]" class="preview-caption">
            {{ captions[currentIndex] }}
          </p>
          <span class="preview-counter">{{ currentIndex + 1 }} / {{ images.length }}</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.preview-overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-preview);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--preview-scrim);
  cursor: zoom-out;
}
.preview-close,
.preview-nav {
  position: absolute;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid rgb(255 255 255 / 20%);
  border-radius: var(--radius);
  background: rgb(20 26 36 / 80%);
  color: white;
}
.preview-close {
  top: 20px;
  right: 20px;
}
.preview-nav {
  top: 50%;
  transform: translateY(-50%);
}
.preview-nav.prev {
  left: 20px;
}
.preview-nav.next {
  right: 20px;
}
.preview-close:hover,
.preview-nav:hover {
  background: rgb(54 65 82 / 95%);
}
.preview-nav:disabled {
  opacity: 0.3;
  cursor: default;
}
.preview-img {
  max-width: calc(100% - 40px);
  max-height: calc(100dvh - 140px);
  object-fit: contain;
  border-radius: var(--radius);
  cursor: default;
}
.preview-footer {
  position: absolute;
  bottom: max(24px, env(safe-area-inset-bottom));
  left: 50%;
  transform: translateX(-50%);
  max-width: 85%;
  text-align: center;
}
.preview-caption {
  margin-bottom: 6px;
  color: white;
  font-size: var(--text-sm);
  overflow-wrap: anywhere;
}
.preview-counter {
  display: inline-block;
  padding: 6px 14px;
  border-radius: var(--radius);
  color: white;
  background: rgb(20 26 36 / 80%);
  font-size: var(--text-sm);
  font-variant-numeric: tabular-nums;
}
.preview-enter-active,
.preview-leave-active {
  transition: opacity 180ms ease;
}
.preview-enter-from,
.preview-leave-to {
  opacity: 0;
}
@media (max-width: 768px) {
  .preview-close {
    top: 10px;
    right: 10px;
  }
  .preview-nav.prev {
    left: 10px;
  }
  .preview-nav.next {
    right: 10px;
  }
  .preview-img {
    max-width: calc(100% - 20px);
  }
}
</style>
