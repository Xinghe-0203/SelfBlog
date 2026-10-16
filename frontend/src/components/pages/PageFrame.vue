<script setup lang="ts">
import type { PageConfig } from '@/types/page'
import { pageStyleVars } from '@/composables/usePageConfig'
defineProps<{ config: PageConfig }>()
</script>
<template>
  <div
    class="page-frame"
    :style="pageStyleVars(config)"
    :data-page-width="config.style?.width"
    :data-page-background="config.style?.background"
    :data-page-density="config.style?.density"
  >
    <slot />
  </div>
</template>
<style>
.page-frame[data-page-width='narrow'] {
  --page-width: 760px;
}
.page-frame[data-page-width='standard'] {
  --page-width: 1040px;
}
.page-frame[data-page-width='wide'] {
  --page-width: 1240px;
}
.page-frame[data-page-background='plain'] {
  background: var(--surface-primary);
  border-radius: var(--radius-xl);
}
.page-frame[data-page-background='soft'] {
  background: var(--surface-secondary);
  border-radius: var(--radius-xl);
}
.page-frame[data-page-background='paper'] {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  box-shadow: inset 0 0 0 1px var(--border-light);
}
.page-frame[data-page-density='compact'] {
  --page-section-gap: 16px;
  --page-padding-block: 16px;
}
.page-frame[data-page-density='comfortable'] {
  --page-section-gap: 36px;
  --page-padding-block: 32px;
}
@media (forced-colors: active) {
  .page-frame[data-page-background] {
    background: Canvas;
    box-shadow: none;
  }
}
</style>
