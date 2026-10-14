<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { likeApi } from '@/api/like'
import { useLogger } from '@/utils/logger'
import { toast } from '@/utils/toast'

const props = withDefaults(defineProps<{
  postId: number | string
  initialLiked?: boolean
  initialCount?: number
  size?: 'sm' | 'md' | 'lg'
  type?: 'post' | 'circle'
}>(), {
  initialLiked: false,
  initialCount: 0,
  size: 'md',
  type: 'post'
})

const emit = defineEmits<{
  toggled: [isLiked: boolean, count: number]
}>()

const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('LikeButton')

const isLiked = ref(props.initialLiked)
const likeCount = ref(props.initialCount)
const isToggling = ref(false)

watch(() => props.initialLiked, (val) => { isLiked.value = val })
watch(() => props.initialCount, (val) => { likeCount.value = val })

const iconSize = { sm: 14, md: 18, lg: 22 }[props.size]

const toggleLike = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (isToggling.value) return
  isToggling.value = true

  // 乐观更新
  const prevLiked = isLiked.value
  const prevCount = likeCount.value
  isLiked.value = !isLiked.value
  likeCount.value = Math.max(0, likeCount.value + (isLiked.value ? 1 : -1))
  emit('toggled', isLiked.value, likeCount.value)

  try {
    const res = await likeApi.toggleLike(props.postId)
    const data = res.data as any
    if (data?.action) {
      isLiked.value = data.action === 'like'
    }
    if (data?.likeCount !== undefined) {
      likeCount.value = data.likeCount
    }
    emit('toggled', isLiked.value, likeCount.value)
  } catch (err: any) {
    // 回滚
    isLiked.value = prevLiked
    likeCount.value = prevCount
    emit('toggled', isLiked.value, likeCount.value)
    logger.error('Failed to toggle like', { error: err.message })
    if (err.response?.status === 401) {
      toast.warning('请先登录后再操作')
    } else {
      toast.error(err.response?.data?.message || '点赞操作失败，请重试')
    }
  } finally {
    isToggling.value = false
  }
}
</script>

<template>
  <button
    class="like-btn"
    :class="[`like-btn--${size}`, { active: isLiked }]"
    :disabled="!userStore.isLoggedIn || isToggling"
    :aria-label="'点赞，当前 ' + likeCount + ' 人已赞'"
    @click="toggleLike"
  >
    <slot :is-liked="isLiked" :count="likeCount">
      <svg :width="iconSize" :height="iconSize" viewBox="0 0 24 24" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
      </svg>
      <span class="like-btn__count">{{ likeCount }}</span>
    </slot>
  </button>
</template>

<style scoped>
.like-btn {
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

.like-btn:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  transform: translateY(-1px);
}

.like-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.like-btn.active {
  color: var(--accent, #ff6b6b);
  background: var(--accent-light, rgba(255, 107, 107, 0.1));
}

.like-btn.active:hover:not(:disabled) {
  background: var(--accent, #ff6b6b);
  color: white;
}

.like-btn.active svg {
  animation: heart-pop 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Sizes */
.like-btn--sm {
  padding: 4px 8px;
  font-size: 0.75rem;
}

.like-btn--md {
  padding: 8px 14px;
  font-size: 0.8125rem;
}

.like-btn--lg {
  padding: 10px 18px;
  font-size: 0.9375rem;
}

.like-btn__count {
  min-width: 1em;
}

@keyframes heart-pop {
  0% { transform: scale(1); }
  30% { transform: scale(1.35); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1); }
}
</style>
