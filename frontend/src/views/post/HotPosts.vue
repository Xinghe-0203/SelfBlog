<template>
  <div class="post-search-page">
    <button class="back-link" @click="router.back()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      <span>返回</span>
    </button>

    <header class="page-head">
      <h1 class="page-title">热门文章</h1>
      <p class="page-sub">社区近期最受欢迎的内容</p>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="state-block">
      <div class="loading-spinner"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="state-block">
      <p class="state-error-text">加载失败，请稍后重试</p>
      <button class="btn btn-primary btn-sm" @click="fetchHotPosts">重新加载</button>
    </div>

    <!-- Posts -->
    <div v-else-if="posts.length > 0" class="post-stack">
      <router-link
        v-for="(post, index) in posts"
        :key="post.id"
        :to="`/post/${post.id}`"
        class="post-row"
      >
        <span class="post-rank" :class="{ 'rank-top': index < 3 }">{{ index + 1 }}</span>
        <div v-if="post.coverImage" class="post-row-cover">
          <img :src="getSafeImageUrl(post.coverImage)" alt="" />
        </div>
        <div class="post-row-body">
          <h2 class="post-row-title">{{ post.title }}</h2>
          <p class="post-row-excerpt">{{ truncateText(post.summary || post.content, 130) }}</p>
          <div class="post-row-meta">
            <img :src="post.avatar || '/default-avatar.png'" :alt="post.nickname || post.username" class="post-row-avatar" />
            <span class="post-row-author">{{ post.nickname || post.username || '匿名' }}</span>
            <span class="meta-dot">&middot;</span>
            <span class="post-row-time">{{ formatRelativeTime(post.createTime) }}</span>
          </div>
          <div class="post-row-stats">
            <span class="stat-item">{{ post.viewCount || 0 }} 阅读</span>
            <span class="stat-item">{{ post.likeCount || 0 }} 点赞</span>
          </div>
          <div v-if="post.tags && post.tags.length" class="post-row-tags">
            <span v-for="tag in post.tags.slice(0, 3)" :key="tag.id" class="mini-tag">{{ tag.name }}</span>
          </div>
        </div>
      </router-link>
    </div>

    <!-- Empty -->
    <div v-else class="state-block state-empty">
      <p class="state-empty-text">暂无文章</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { trendingApi } from '../../api/trending'
import { formatRelativeTime, truncateText, getSafeImageUrl } from '../../utils'
import { useLogger } from '../../utils/logger'

const router = useRouter()
const logger = useLogger('HotPosts')
const posts = ref<any[]>([])
const loading = ref(false)
const error = ref(false)

const fetchHotPosts = async () => {
  loading.value = true
  error.value = false
  try {
    const response = await trendingApi.getHotPosts({ pageNum: 1, pageSize: 50 })
    posts.value = response.data?.records || []
  } catch (err: any) {
    logger.error('Failed to fetch hot posts', { error: err.message })
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchHotPosts()
})
</script>

<style scoped>
/* =====================================================
   Post Search (Hot Posts) — Typography-Forward Minimalist
   ===================================================== */

.post-search-page {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);
}

/* ---------- Back link ---------- */

.back-link {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1_5);
  padding: var(--spacing-2) 0;
  font-size: var(--text-sm);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  color: var(--text-muted);
  background: none;
  border: none;
  cursor: pointer;
  transition: color var(--transition-fast);
  margin-bottom: var(--spacing-lg);
}

.back-link:hover {
  color: var(--primary);
}

/* ---------- Page header ---------- */

.page-head {
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-solid);
}

.page-title {
  font-family: var(--font-display);
  font-size: var(--text-4xl);
  font-weight: var(--font-extrabold);
  color: var(--text-primary);
  letter-spacing: -0.03em;
  line-height: var(--leading-tight);
  margin: 0 0 var(--spacing-2);
}

.page-sub {
  font-size: var(--text-base);
  color: var(--text-muted);
  margin: 0;
}

/* ---------- Post list ---------- */

.post-stack {
  display: flex;
  flex-direction: column;
}

.post-row {
  display: flex;
  gap: var(--spacing-4);
  padding: var(--spacing-5) 0;
  text-decoration: none;
  border-bottom: 1px solid var(--border);
  transition: background var(--transition-fast);
  align-items: flex-start;
}

.post-row:last-child {
  border-bottom: none;
}

.post-row:hover {
  background: var(--primary-subtle);
}

/* Rank number */
.post-rank {
  flex-shrink: 0;
  width: 32px;
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-muted);
  line-height: 1;
  padding-top: 2px;
  text-align: center;
}

.post-rank.rank-top {
  color: var(--accent);
}

/* Cover */
.post-row-cover {
  width: 120px;
  height: 80px;
  flex-shrink: 0;
  border-radius: var(--radius);
  overflow: hidden;
}

.post-row-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* Body */
.post-row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1_5);
}

.post-row-title {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0;
  line-height: var(--leading-snug);
  letter-spacing: var(--tracking-tight);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-row:hover .post-row-title {
  color: var(--primary);
}

.post-row-excerpt {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-normal);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-row-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: var(--text-xs);
  color: var(--text-muted);
}

.post-row-avatar {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
}

.post-row-author {
  font-weight: var(--font-medium);
  color: var(--text-secondary);
}

.meta-dot {
  opacity: 0.35;
}

.post-row-stats {
  display: flex;
  gap: var(--spacing-3);
  font-size: var(--text-xs);
  color: var(--text-muted);
}

.stat-item {
  display: inline-flex;
  align-items: center;
}

.post-row-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-1_5);
  margin-top: var(--spacing-1);
}

.mini-tag {
  font-size: 0.6875rem;
  font-weight: var(--font-medium);
  color: var(--primary);
  background: var(--primary-light);
  padding: 1px var(--spacing-2);
  border-radius: var(--radius-sm);
  line-height: 1.5;
}

/* ---------- States ---------- */

.state-block {
  text-align: center;
  padding: var(--spacing-3xl) var(--spacing-lg);
}

.state-error-text {
  font-size: var(--text-sm);
  color: var(--error);
  margin-bottom: var(--spacing-md);
}

.state-empty-text {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  color: var(--text-muted);
}

/* ---------- Responsive ---------- */

@media (max-width: 768px) {
  .post-search-page {
    padding: var(--spacing-lg) var(--spacing-md);
  }

  .page-title {
    font-size: var(--text-3xl);
  }

  .post-row {
    gap: var(--spacing-3);
    padding: var(--spacing-4) 0;
  }

  .post-rank {
    font-size: var(--text-xl);
    width: 24px;
  }

  .post-row-cover {
    width: 88px;
    height: 60px;
  }

  .post-row-title {
    font-size: var(--text-lg);
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: var(--text-2xl);
  }

  .post-row {
    flex-wrap: wrap;
  }

  .post-row-cover {
    width: 100%;
    height: 140px;
    order: -1;
    margin-bottom: var(--spacing-2);
  }

  .post-rank {
    width: 100%;
    text-align: left;
    font-size: var(--text-sm);
    font-family: var(--font-sans);
    font-weight: var(--font-semibold);
    color: var(--accent);
    margin-bottom: calc(-1 * var(--spacing-2));
  }
}
</style>
