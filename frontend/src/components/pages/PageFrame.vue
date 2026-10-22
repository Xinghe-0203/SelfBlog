<script setup lang="ts">
import { computed } from 'vue'
import type { PageConfig } from '@/types/page'
import { pageStyleVars } from '@/composables/usePageConfig'
const props = defineProps<{ config: PageConfig }>()
/**
 * 页面背景**永远**解析出一个具体值。
 *
 * 历史缺陷：`config.style.background` 为空时这个属性整个不渲染，而 `.page-frame`
 * 又没有兜底规则 —— 于是整页内容（文章标题/正文/目录）直接画在作者的壁纸照片上。
 * 现在缺省落到 `'auto'`：由样式表按「画布是照片 / 是纯色」决定要不要给纸面。
 * `'inherit'` 仍是唯一的退出开关，留给纯装饰页（它表示「作者明确要求不要表面」）。
 */
const background = computed(() => props.config.style?.background ?? 'auto')
</script>
<template>
  <div
    class="page-frame"
    :style="pageStyleVars(config)"
    :data-page-width="config.style?.width"
    :data-page-background="background"
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
/* 三级表面都消费 appearance.css 的 `--surface-*` 变量，而不是各自硬编码 token：
   纯色画布下变量别名回原 token（观感不变），壁纸画布下换成纸面/浮起两档实底。 */
.page-frame[data-page-background='plain'] {
  background: var(--surface-paper);
  border-radius: var(--radius-xl);
}
.page-frame[data-page-background='soft'] {
  background: var(--surface-raised);
  border-radius: var(--radius-xl);
}
.page-frame[data-page-background='paper'] {
  background: var(--surface-control);
  border-radius: var(--radius-xl);
  box-shadow: inset 0 0 0 1px var(--border-light);
}
/* 缺省档（没有显式配置背景）：只在「画布是图」时给纸面，纯色画布保持原来的透明观感。
   这条规则与 appearance.css 里的同名规则重复是**刻意**的：PageFrame 自带兜底，
   即使某个页面没被 main.css 的层叠顺序覆盖到，也不会裸奔在照片上。
   两处声明必须逐字一致（Lead 要求），改动请同时改 appearance.css。
   注意根选择器：`data-wallpaper` 挂在 <html>（documentElement）上 —— 首屏预置脚本
   只能写它，运行时 applyAppearance() 为同源也写它，因此这里是
   `html[data-wallpaper] #selfblog ...`，不是 `#selfblog[data-wallpaper] ...`。 */
html[data-wallpaper] #selfblog .page-frame[data-page-background='auto'] {
  background: var(--surface-paper);
  border-radius: var(--radius-2xl);
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
