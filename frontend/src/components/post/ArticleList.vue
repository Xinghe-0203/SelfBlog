<script setup lang="ts">
import { ref } from 'vue'
import { formatDate, getSafeImageUrl, truncateText } from '@/utils'
import { estimateReadMinutes } from '@/utils/reading'
import type { Post } from '@/types'

withDefaults(defineProps<{ posts: Post[]; featured?: boolean; heading?: 'h2' | 'h3' }>(), {
  heading: 'h3'
})
const failed = ref(new Set<string>())
const cover = (post: Post) => getSafeImageUrl(post.coverImage || post.coverUrl, '')
</script>

<template>
  <div class="article-list" :class="{ 'article-list--featured': featured }">
    <article v-for="post in posts" :key="post.id" class="article-row">
      <div class="article-row-main">
        <div class="article-meta">
          <time :datetime="post.createTime || post.createdAt">{{
            formatDate(post.createTime || post.createdAt, 'YYYY年MM月DD日')
          }}</time
          ><span v-if="post.category">{{ post.category }}</span>
        </div>
        <component :is="heading" class="article-title"
          ><router-link :to="`/post/${post.id}`">{{ post.title }}</router-link></component
        >
        <p v-if="post.summary || post.content" class="article-summary">
          {{ truncateText(post.summary || post.content, 180) }}
        </p>
        <div class="article-footer">
          <div v-if="post.tags?.length" class="article-tags">
            <router-link v-for="tag in post.tags" :key="tag.id" :to="`/tag/${tag.id}`">{{
              tag.name
            }}</router-link>
          </div>
          <span class="reading-time">{{ estimateReadMinutes(post.content) }} 分钟阅读</span>
        </div>
      </div>
      <router-link
        v-if="cover(post) && !failed.has(cover(post))"
        :to="`/post/${post.id}`"
        class="row-cover"
        :aria-label="`阅读：${post.title}`"
        tabindex="-1"
        aria-hidden="true"
        ><img :src="cover(post)" alt="" loading="lazy" @error="failed.add(cover(post))"
      /></router-link>
    </article>
  </div>
</template>

<style scoped>
.article-list {
  padding-inline: 25px;
  border: 1px solid var(--border-solid);
  border-radius: var(--radius-xl);
  /* 浮起档：纯色画布别名回 --surface-primary（逐像素不变），壁纸画布上是纸面之上的一档。 */
  background: var(--surface-raised);
}
.article-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-block: 24px;
}
.article-row + .article-row {
  border-top: 1px solid var(--border-light);
}
.article-row-main {
  flex: 1;
  min-width: 0;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 14px;
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.article-title {
  margin: 8px 0;
  font-size: var(--text-lg);
  font-weight: 650;
  line-height: 1.5;
  letter-spacing: -0.025em;
  overflow-wrap: anywhere;
}
.article-title a {
  color: var(--text-primary);
}
.article-title a:hover {
  color: var(--accent);
}
.article-list--featured .article-row:first-child .article-title {
  font-size: var(--text-2xl);
}
.article-summary {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.85;
  overflow-wrap: anywhere;
}
.article-footer {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 13px;
}
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.article-tags a {
  padding: 2px 7px;
  border-radius: var(--radius-xs);
  color: var(--text-secondary);
  background: var(--bg-primary);
  font-size: var(--text-2xs);
}
.article-tags a:hover {
  color: var(--accent);
  background: var(--accent-light);
}
.reading-time {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.row-cover {
  flex: 0 0 130px;
  width: 130px;
  height: 94px;
  overflow: hidden;
  border-radius: var(--radius);
  background: var(--bg-secondary);
}
.row-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
@media (max-width: 600px) {
  .article-list {
    padding-inline: 18px;
    border-radius: var(--radius-lg);
  }
  .article-row {
    gap: 13px;
    padding-block: 22px;
  }
  .article-title {
    font-size: var(--text-base);
  }
  .article-list--featured .article-row:first-child .article-title {
    font-size: var(--text-xl);
  }
  .row-cover {
    flex-basis: 77px;
    width: 77px;
    height: 79px;
  }
  .article-meta {
    font-size: var(--text-2xs);
  }
}
</style>
