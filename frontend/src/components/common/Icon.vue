<script setup lang="ts">
import { computed } from 'vue'
import { icons, type IconName } from './icons'

const props = withDefaults(defineProps<{ name: IconName; size?: number }>(), {
  size: 20
})

/**
 * 片段来自本仓库的静态注册表，不掺入任何用户输入，因此 v-html 是安全的。
 * 不用外部图标库是为了保持零新增依赖，且描边图形能直接吃 currentColor。
 */
const fragment = computed(() => icons[props.name] ?? icons.logo)
</script>

<template>
  <!-- 片段来自本仓库 icons.ts 的静态注册表（纯 SVG path，无用户输入），因此 v-html 是安全的 -->
  <!-- eslint-disable vue/no-v-html -->
  <svg
    class="icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
    v-html="fragment"
  />
  <!-- eslint-enable vue/no-v-html -->
</template>

<style scoped>
.icon {
  display: block;
  flex: 0 0 auto;
}
</style>
