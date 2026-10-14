<script setup lang="ts">
import { computed } from 'vue'

interface FilterOption {
  label: string
  value: string
}

const props = defineProps<{
  filters: FilterOption[]
  currentFilter: string
}>()

defineEmits<{
  update: [value: string]
}>()

const indicatorStyle = computed(() => {
  const idx = props.filters.findIndex((f) => f.value === props.currentFilter)
  const count = props.filters.length || 1
  return {
    width: `calc((100% - 8px) / ${count})`,
    transform: `translateX(${idx * 100}%)`
  }
})
</script>

<template>
  <div class="filter-tabs">
    <button
      v-for="f in filters"
      :key="f.value"
      class="filter-tab"
      :class="{ active: currentFilter === f.value }"
      @click="$emit('update', f.value)"
    >
      {{ f.label }}
    </button>
    <span class="filter-indicator" :style="indicatorStyle"></span>
  </div>
</template>

<style scoped>
.filter-tabs {
  position: relative;
  display: flex;
  gap: 0;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  padding: 4px;
  box-shadow: var(--glass-shadow);
}

.filter-tab {
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 8px 20px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  cursor: pointer;
  transition: all var(--transition);
  text-align: center;
}

.filter-tab:hover:not(.active) {
  color: var(--text-primary);
  background: var(--primary-light);
}

.filter-tab.active {
  color: white;
}

.filter-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  height: calc(100% - 8px);
  background: linear-gradient(135deg, var(--primary-start), var(--primary-end));
  border-radius: calc(var(--radius) - 4px);
  transition: transform var(--transition-slow) cubic-bezier(0.4, 0, 0.2, 1), width var(--transition-slow) cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 0;
  box-shadow: 0 2px 8px var(--primary-glow);
}
</style>