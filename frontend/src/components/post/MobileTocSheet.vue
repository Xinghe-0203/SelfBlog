<script setup lang="ts">
/**
 * MobileTocSheet.vue — 移动端文章目录抽屉
 *
 * 窄屏下侧边目录（PostDetail 的 .article-aside）会隐藏，这里用左下角浮动胶囊 + 底部抽屉
 * 复用同一份 toc 数据和全局浮层生命周期；1020px 及以下避免侧栏挤压正文。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Icon from '@/components/common/Icon.vue'
import { overlayTarget, useDialog } from '@/composables/useDialog'

interface TocItem {
  id: string
  text: string
  level: number
}

defineProps<{
  items: TocItem[]
  activeId?: string
}>()

const emit = defineEmits<{
  select: [item: TocItem]
}>()

const open = ref(false)
const panel = ref<HTMLElement | null>(null)
const { layer } = useDialog(open, panel, () => {
  open.value = false
})
const onResize = () => {
  if (window.innerWidth >= 1021) open.value = false
}
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))

function selectItem(item: TocItem): void {
  open.value = false
  emit('select', item)
}
</script>

<template>
  <div class="toc-sheet">
    <button
      type="button"
      class="toc-fab functional-glass"
      aria-label="打开文章目录"
      :aria-expanded="open"
      @click="open = true"
    >
      <Icon name="file-text" :size="18" />
      <span>目录</span>
    </button>

    <Teleport :to="overlayTarget()">
      <template v-if="open">
        <div
          class="toc-backdrop"
          :style="{ zIndex: `calc(var(--z-modal) + ${layer})` }"
          @click="open = false"
        ></div>
        <div
          ref="panel"
          class="toc-panel functional-glass"
          :style="{ zIndex: `calc(var(--z-modal) + ${layer} + 1)` }"
          role="dialog"
          aria-modal="true"
          aria-label="文章目录"
          tabindex="-1"
        >
          <header class="toc-panel-head">
            <span class="toc-panel-title">文章目录</span>
            <button type="button" class="toc-close" aria-label="关闭目录" @click="open = false">
              <Icon name="close" :size="16" />
            </button>
          </header>
          <nav class="toc-nav" aria-label="目录条目">
            <button
              v-for="item in items"
              :key="item.id"
              type="button"
              :class="[`level-${item.level}`, { active: item.id === activeId }]"
              @click="selectItem(item)"
            >
              {{ item.text }}
            </button>
          </nav>
        </div>
      </template>
    </Teleport>
  </div>
</template>

<style scoped>
/* 目录在左，返回顶部在右；音乐已移入导航。 */
.toc-fab {
  position: fixed;
  left: 18px;
  bottom: 24px;
  z-index: var(--z-fixed);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  min-height: 44px;
  color: var(--text-secondary);
  box-shadow: var(--shadow-lg);
  font: inherit;
  font-size: var(--text-xs);
  cursor: pointer;
  transition:
    color var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default);
}
.toc-fab:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.toc-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal-backdrop);
  background: var(--scrim);
}

.toc-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: var(--z-modal);
  display: flex;
  max-height: 70vh;
  flex-direction: column;
  padding: 18px 20px 24px;
  border-radius: var(--radius-2xl) var(--radius-2xl) 0 0;
  box-shadow: var(--shadow-2xl);
  color: var(--text-primary);
}
.toc-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-solid);
}
.toc-panel-title {
  font: 700 1rem var(--font-display);
}
.toc-close {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: var(--radius);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  cursor: pointer;
}
.toc-close:hover {
  color: var(--primary);
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: auto;
  padding-top: 12px;
}
.toc-nav button {
  padding: 9px 10px;
  border: 0;
  border-radius: var(--radius-sm);
  background: none;
  color: var(--text-secondary);
  font: inherit;
  font-size: var(--text-sm);
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
}
.toc-nav button.level-2 {
  padding-left: 22px;
}
.toc-nav button.level-3 {
  padding-left: 34px;
}
.toc-nav button.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

/* 1021px 以上改用侧边目录，整块隐藏（含浮动胶囊） */
@media (min-width: 1021px) {
  .toc-sheet {
    display: none;
  }
  .toc-panel,
  .toc-backdrop {
    display: none;
  }
}
</style>
