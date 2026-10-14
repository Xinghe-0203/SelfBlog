<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { collectApi } from '@/api/collect'
import { useLogger } from '@/utils/logger'
import { toast } from '@/utils/toast'

const props = withDefaults(defineProps<{
  postId: number | string
  initialCollected?: boolean
  initialCount?: number
  size?: 'sm' | 'md' | 'lg'
}>(), {
  initialCollected: false,
  initialCount: 0,
  size: 'md'
})

const emit = defineEmits<{
  toggled: [isCollected: boolean, count: number]
}>()

const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('CollectButton')

const isCollected = ref(props.initialCollected)
const collectCount = ref(props.initialCount)
const isToggling = ref(false)

watch(() => props.initialCollected, (val) => { isCollected.value = val })
watch(() => props.initialCount, (val) => { collectCount.value = val })

const iconSize = { sm: 14, md: 18, lg: 22 }[props.size]

const toggleCollect = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (isToggling.value) return
  isToggling.value = true

  // 乐观更新
  const prevCollected = isCollected.value
  const prevCount = collectCount.value
  isCollected.value = !isCollected.value
  collectCount.value = Math.max(0, collectCount.value + (isCollected.value ? 1 : -1))
  emit('toggled', isCollected.value, collectCount.value)

  try {
    const res = await collectApi.toggleCollect(props.postId)
    const data = res.data as any
    if (data?.action) {
      isCollected.value = data.action === 'collect'
    }
    if (data?.collectCount !== undefined) {
      collectCount.value = data.collectCount
    }
    emit('toggled', isCollected.value, collectCount.value)
  } catch (err: any) {
    // 回滚
    isCollected.value = prevCollected
    collectCount.value = prevCount
    emit('toggled', isCollected.value, collectCount.value)
    logger.error('Failed to toggle collect', { error: err.message })
    if (err.response?.status === 401) {
      toast.warning('请先登录后再操作')
    } else {
      toast.error(err.response?.data?.message || '收藏操作失败，请重试')
    }
  } finally {
    isToggling.value = false
  }
}
</script>

<template>
  <button
    class="collect-btn"
    :class="[`collect-btn--${size}`, { active: isCollected }]"
    :disabled="!userStore.isLoggedIn || isToggling"
    :aria-label="'收藏，当前 ' + collectCount + ' 人已收藏'"
    @click="toggleCollect"
  >
    <slot :is-collected="isCollected" :count="collectCount">
      <svg :width="iconSize" :height="iconSize" viewBox="0 0 24 24" :fill="isCollected ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
      </svg>
      <span class="collect-btn__count">{{ collectCount }}</span>
    </slot>
  </button>
</template>

<style scoped>
.collect-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm, 6px);
  background: transparent;
  border: none;
  border-radius: var(--radius, 8px);
  color: var(--text-muted);
  cursor: pointer;
  font-weight: 500;
  transition: all var(--transition, 0.2s ease);
  user-select: none;
}

.collect-btn:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  transform: translateY(-1px);
}

.collect-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.collect-btn.active {
  color: var(--warning, #f59e0b);
  background: var(--warning-light, rgba(245, 158, 11, 0.1));
}

.collect-btn.active:hover:not(:disabled) {
  background: var(--warning, #f59e0b);
  color: white;
}

.collect-btn.active svg {
  animation: collect-pop 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Sizes */
.collect-btn--sm {
  padding: 4px 8px;
  font-size: 0.75rem;
}

.collect-btn--md {
  padding: 8px 14px;
  font-size: 0.8125rem;
}

.collect-btn--lg {
  padding: 10px 18px;
  font-size: 0.9375rem;
}

.collect-btn__count {
  min-width: 1em;
}

@keyframes collect-pop {
  0% { transform: scale(1) rotate(0deg); }
  30% { transform: scale(1.3) rotate(-5deg); }
  60% { transform: scale(0.9) rotate(3deg); }
  100% { transform: scale(1) rotate(0deg); }
}
</style>
