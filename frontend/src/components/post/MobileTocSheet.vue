<script setup lang="ts">
/**
 * MobileTocSheet.vue — 移动端文章目录抽屉
 *
 * 窄屏下侧边目录（PostDetail 的 .article-aside）会隐藏，这里用左下角浮动胶囊 + 底部抽屉
 * 复用同一份 toc 数据。不注册全局事件、不用 Teleport：状态全部收在组件内，
 * 测试可以像普通组件一样挂载；宽屏由 CSS 媒体查询整块隐藏（与 PostDetail 的 861px 断点一致）。
 */
import { ref } from 'vue'
import Icon from '@/components/common/Icon.vue'

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

function selectItem(item: TocItem): void {
  emit('select', item)
  open.value = false
}
</script>

<template>
  <div class="toc-sheet">
    <button type="button" class="toc-fab" aria-label="打开文章目录" @click="open = true">
      <Icon name="file-text" :size="18" />
      <span>目录</span>
    </button>

    <template v-if="open">
      <div class="toc-backdrop" @click="open = false"></div>
      <div class="toc-panel" role="dialog" aria-modal="true" aria-label="文章目录">
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
  </div>
</template>

<style scoped>
/* 左下角浮动胶囊：右下角已被返回顶部与音乐播放器占用，移动端目录放左侧互不遮挡 */
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
  border-radius: var(--radius-full);
  background: var(--surface-primary);
  color: var(--text-secondary);
  box-shadow: var(--shadow-lg);
  font: inherit;
  font-size: 0.8rem;
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
  background: rgba(0, 0, 0, 0.45);
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
  background: var(--surface-primary);
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
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: var(--radius-full);
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
  font-size: 0.82rem;
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

/* 861px 以上改用侧边目录，整块隐藏（含浮动胶囊） */
@media (min-width: 861px) {
  .toc-sheet {
    display: none;
  }
}
</style>
