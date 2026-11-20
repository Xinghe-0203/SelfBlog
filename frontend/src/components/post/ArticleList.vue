<script setup lang="ts">
import { computed, ref } from 'vue'
import { formatDate, getSafeImageUrl, truncateText } from '@/utils'
import { estimateReadMinutes } from '@/utils/reading'
import Icon from '@/components/common/Icon.vue'
import type { Post } from '@/types'

const props = withDefaults(
  defineProps<{ posts: Post[]; featured?: boolean; heading?: 'h2' | 'h3' }>(),
  { heading: 'h3' }
)
const failed = ref(new Set<string>())
const cover = (post: Post) => getSafeImageUrl(post.coverImage, '')
// 阅读时长按 post.id 记忆成 Map：estimateReadMinutes 要对全文跑多趟正则，
// 模板里每次渲染逐条重算太贵。Post.id 必填，Map 与同一数组同源，get 不会落空。
const readMinutes = computed(
  () =>
    new Map(
      props.posts.map((post): [number, number] => [post.id, estimateReadMinutes(post.content)])
    )
)
</script>

<template>
  <div class="article-list" :class="{ 'article-list--featured': featured }">
    <article
      v-for="(post, index) in posts"
      :key="post.id"
      class="article-row"
      :style="{ '--row-index': index }"
    >
      <div class="article-row-main">
        <div class="article-meta">
          <time :datetime="post.createTime">{{
            formatDate(post.createTime, 'YYYY年MM月DD日')
          }}</time>
          <span v-if="post.category" class="article-category">{{ post.category }}</span>
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
          <span class="reading-time"
            ><Icon name="clock" :size="13" />{{ readMinutes.get(post.id) }} 分钟阅读</span
          >
        </div>
      </div>
      <router-link
        v-if="cover(post) && !failed.has(cover(post))"
        :to="`/post/${post.id}`"
        class="row-cover"
        :aria-label="`阅读：${post.title}`"
        tabindex="-1"
        aria-hidden="true"
        ><img
          :src="cover(post)"
          alt=""
          :loading="featured && post === posts[0] ? 'eager' : 'lazy'"
          :fetchpriority="featured && post === posts[0] ? 'high' : 'auto'"
          decoding="async"
          width="320"
          height="200"
          @error="failed.add(cover(post))"
      /></router-link>
    </article>
  </div>
</template>

<style scoped>
/* ---------------------------------------------------------------------------
   文章列表：编辑部式的「封面故事 + 列表」
   ---------------------------------------------------------------------------
   featured = true 时第一行升级为封面故事：更大的标题、两栏网格、右侧封面。
   仍然是同一套 DOM（.article-row / h3 / .article-tags / .row-cover），
   差别全部由 CSS 完成 —— 列表组件的语义与可访问性结构保持不变。 */
.article-list {
  padding-inline: 26px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  /* 浮起档：纯色画布别名回 --surface-primary（逐像素不变），壁纸画布上是纸面之上的一档。 */
  background: var(--surface-raised);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}
.article-row {
  display: flex;
  align-items: center;
  gap: 22px;
  padding-block: 24px;
  /* 逐行入场：每行比上一行晚 55ms，读起来像「列表被铺开」而不是「啪地出现」。
     延迟由模板里的 --row-index 决定；动画只动 opacity/transform，不触发布局。 */
  animation: enter-up 420ms var(--ease-out) both;
  animation-delay: calc(var(--row-index, 0) * 45ms);
  /* 长列表的渲染优化：离屏的行跳过渲染，滚动到附近时再补上。
     contain-intrinsic-size 用 auto 记住上次渲染过的高度，滚动条长度不会跳。 */
  content-visibility: auto;
  contain-intrinsic-size: auto 132px;
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
  gap: 6px 12px;
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.article-category {
  padding: 1px 8px;
  border-radius: var(--radius-full);
  background: var(--surface-tertiary);
  color: var(--text-secondary);
  font-size: var(--text-2xs);
}
.article-title {
  margin: 9px 0 0;
  font-size: var(--text-lg);
  font-weight: 650;
  line-height: 1.5;
  letter-spacing: -0.015em;
  overflow-wrap: anywhere;
}
.article-title a {
  color: var(--text-primary);
  transition: color var(--transition-fast);
}
.article-title a:hover {
  color: var(--accent);
}
.article-summary {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.8;
  overflow-wrap: anywhere;
}
.article-footer {
  display: flex;
  align-items: center;
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
  padding: 2px 9px;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  background: var(--surface-tertiary);
  font-size: var(--text-2xs);
  transition:
    color var(--transition-fast),
    background var(--transition-fast);
}
.article-tags a:hover {
  color: var(--accent);
  background: var(--accent-light);
}
.reading-time {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.row-cover {
  flex: 0 0 132px;
  width: 132px;
  height: 92px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
}
.row-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}
.article-row:hover .row-cover img {
  transform: scale(1.04);
}

/* ---------------------------------------------------------------------------
   封面故事（featured 档的第一行）
   --------------------------------------------------------------------------- */
.article-list--featured .article-row:first-child {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: center;
  gap: 28px;
  padding-block: 28px 30px;
}
/* 只有真的带封面时才分两栏；没有封面的文章不能空出一列 300px 的白地。 */
.article-list--featured .article-row:first-child:has(.row-cover) {
  grid-template-columns: minmax(0, 1fr) 300px;
}
.article-list--featured .article-row:first-child .article-title {
  font-size: var(--text-2xl);
  font-weight: 700;
  line-height: 1.42;
  letter-spacing: -0.025em;
}
.article-list--featured .article-row:first-child .article-summary {
  font-size: var(--text-base);
  line-height: 1.8;
}
.article-list--featured .article-row:first-child .row-cover {
  flex: none;
  width: 100%;
  height: 186px;
  border-radius: var(--radius-lg);
}
.article-list--featured .article-row:first-child + .article-row {
  border-top-color: var(--border-light);
}

@media (max-width: 760px) {
  .article-list--featured .article-row:first-child {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }
  .article-list--featured .article-row:first-child .row-cover {
    order: -1;
    height: 168px;
  }
  .article-list--featured .article-row:first-child .article-title {
    font-size: var(--text-xl);
  }
}

@media (max-width: 600px) {
  .article-list {
    padding-inline: 18px;
    border-radius: var(--radius-lg);
  }
  .article-row {
    gap: 14px;
    padding-block: 20px;
  }
  .article-title {
    font-size: var(--text-base);
  }
  .article-list--featured .article-row:first-child .article-title {
    font-size: var(--text-xl);
  }
  .row-cover {
    flex-basis: 78px;
    width: 78px;
    height: 78px;
    border-radius: var(--radius);
  }
  .article-meta {
    font-size: var(--text-2xs);
  }
}
</style>
