<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { followApi } from '@/api/follow'
import { useLogger } from '@/utils/logger'
import { toast } from '@/utils/toast'

const props = withDefaults(defineProps<{
  userId: number | string
  initialFollowing?: boolean
  initialCount?: number
  size?: 'sm' | 'md' | 'lg'
}>(), {
  initialFollowing: false,
  initialCount: 0,
  size: 'md'
})

const emit = defineEmits<{
  toggled: [isFollowing: boolean, count: number]
}>()

const router = useRouter()
const userStore = useUserStore()
const logger = useLogger('FollowButton')

const isFollowing = ref(props.initialFollowing)
const followCount = ref(props.initialCount)
const isToggling = ref(false)

watch(() => props.initialFollowing, (val) => { isFollowing.value = val })
watch(() => props.initialCount, (val) => { followCount.value = val })

const iconSize = { sm: 14, md: 16, lg: 18 }[props.size]

const toggleFollow = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (isToggling.value) return
  isToggling.value = true

  // 乐观更新
  const prevFollowing = isFollowing.value
  const prevCount = followCount.value
  isFollowing.value = !isFollowing.value
  followCount.value = Math.max(0, followCount.value + (isFollowing.value ? 1 : -1))
  emit('toggled', isFollowing.value, followCount.value)

  try {
    const res = await followApi.toggleFollow(props.userId)
    const data = res.data as any
    if (data?.action) {
      isFollowing.value = data.action === 'follow'
    }
    if (data?.followerCount !== undefined) {
      followCount.value = data.followerCount
    }
    emit('toggled', isFollowing.value, followCount.value)
    toast.success(isFollowing.value ? '关注成功' : '已取消关注')
  } catch (err: any) {
    // 回滚
    isFollowing.value = prevFollowing
    followCount.value = prevCount
    emit('toggled', isFollowing.value, followCount.value)
    logger.error('Failed to toggle follow', { error: err.message })
    if (err.response?.status === 401) {
      toast.warning('请先登录后再操作')
    } else {
      toast.error(err.response?.data?.message || '操作失败')
    }
  } finally {
    isToggling.value = false
  }
}
</script>

<template>
  <button
    class="follow-btn"
    :class="[`follow-btn--${size}`, isFollowing ? 'follow-btn--following' : 'follow-btn--follow']"
    :disabled="isToggling"
    :aria-label="isFollowing ? '已关注' : '关注'"
    @click="toggleFollow"
  >
    <slot :is-following="isFollowing" :count="followCount">
      <template v-if="isToggling">
        <span class="follow-btn__spinner"></span>
      </template>
      <template v-else>
        <svg v-if="isFollowing" :width="iconSize" :height="iconSize" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/>
          <polyline points="17 11 19 13 23 9"/>
        </svg>
        <svg v-else :width="iconSize" :height="iconSize" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/>
          <line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/>
        </svg>
        <span>{{ isFollowing ? '已关注' : '关注' }}</span>
      </template>
    </slot>
  </button>
</template>

<style scoped>
.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm, 6px);
  border: none;
  border-radius: var(--radius, 8px);
  cursor: pointer;
  font-weight: 500;
  transition: all var(--transition, 0.2s ease);
  user-select: none;
  white-space: nowrap;
}

.follow-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.follow-btn--follow {
  background: var(--primary);
  color: white;
}

.follow-btn--follow:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px var(--primary-glow, rgba(99, 102, 241, 0.4));
}

.follow-btn--following {
  background: var(--glass-bg, rgba(255, 255, 255, 0.1));
  color: var(--text-secondary);
  border: 1px solid var(--glass-border, rgba(255, 255, 255, 0.15));
}

.follow-btn--following:hover:not(:disabled) {
  background: var(--glass-hover, rgba(255, 255, 255, 0.15));
  color: var(--text-primary);
}

/* Sizes */
.follow-btn--sm {
  padding: 4px 10px;
  font-size: 0.75rem;
}

.follow-btn--md {
  padding: 8px 16px;
  font-size: 0.875rem;
}

.follow-btn--lg {
  padding: 10px 22px;
  font-size: 1rem;
}

.follow-btn__spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: follow-spin 0.6s linear infinite;
}

@keyframes follow-spin {
  to { transform: rotate(360deg); }
}
</style>
