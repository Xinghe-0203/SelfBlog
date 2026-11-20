<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { tagApi } from '@/api/tag'
import { useSiteStore } from '@/stores/site'
import Icon from '@/components/common/Icon.vue'
import PageFrame from '@/components/pages/PageFrame.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { usePageSeo } from '@/composables/usePageSeo'
import type { Tag } from '@/types'

const siteStore = useSiteStore()
const { config, text, module: showModule } = usePageConfig('tags')

usePageSeo('tags', () => ({
  title: text('title', '标签'),
  description: text('description', '按主题浏览这个博客写过的全部标签。'),
  url: '/tags'
}))

const tags = ref<Tag[]>([])
const loading = ref(true)
const error = ref('')

/**
 * 公开标签索引只列**有已发布文章**的标签。
 *
 * 0 篇的标签点进去是一张空白页：对读者是浪费一次点击，对搜索引擎是薄内容。
 * 标签本身不会因此丢失 —— 后台「标签管理」仍能看到并管理它，作者发第一篇文章
 * 挂上它之后，这里自然就会重新出现。
 */
const visibleTags = computed(() => tags.value.filter((tag) => (tag.postCount ?? 0) > 0))

/** 有文章数的标签排在前面；同名按名称升序（服务端已排序，这里只做兜底）。 */
const sortedTags = computed(() =>
  [...visibleTags.value].sort(
    (a, b) => (b.postCount ?? 0) - (a.postCount ?? 0) || a.name.localeCompare(b.name)
  )
)

/** 标签数量分布：用来给标签云一个「按热度」的视觉层级。 */
const maxCount = computed(() =>
  sortedTags.value.reduce((max, tag) => Math.max(max, tag.postCount ?? 0), 0)
)

/** 归一化后的热度（0.35 ~ 1），驱动字号与圆点的尺寸。 */
function heat(tag: Tag): number {
  const count = tag.postCount ?? 0
  if (!maxCount.value) return 0.35
  return 0.35 + (count / maxCount.value) * 0.65
}

async function fetchTags(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const response = await tagApi.getTagsWithCounts()
    tags.value = response.data || []
  } catch {
    tags.value = []
    error.value = '标签暂时无法加载，请稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(fetchTags)
</script>

<template>
  <PageFrame :config="config" class="tags-page">
    <header class="page-heading">
      <span class="heading-rule" aria-hidden="true"></span>
      <h1>{{ text('title', '标签') }}</h1>
      <p class="page-lead">
        {{ text('description', '按主题浏览写过的全部内容。标签下的数字是该主题已发布的文章数。') }}
      </p>
    </header>

    <div v-if="loading" class="state-panel" role="status">正在整理标签…</div>
    <div v-else-if="error" class="state-panel state-panel--error" role="alert">
      <p>{{ error }}</p>
      <button type="button" class="btn btn-secondary" @click="fetchTags">重新加载</button>
    </div>
    <div v-else-if="!sortedTags.length" class="state-panel">
      <p>还没有任何标签。</p>
      <router-link to="/">回到首页</router-link>
    </div>

    <ul v-else class="tag-cloud">
      <li v-for="tag in sortedTags" :key="tag.id">
        <router-link class="tag-pill" :to="`/tag/${tag.id}`" :style="{ '--tag-heat': heat(tag) }">
          <span class="tag-dot" aria-hidden="true"></span>
          <span class="tag-name">{{ tag.name }}</span>
          <span class="tag-count">{{ showModule('count') ? (tag.postCount ?? 0) : '' }}</span>
        </router-link>
      </li>
    </ul>

    <footer v-if="!loading && sortedTags.length" class="tags-footer">
      <Icon name="info" :size="15" />
      <span
        >共 {{ sortedTags.length }} 个标签，覆盖
        {{ siteStore.settings.siteTitle }} 的全部公开文章</span
      >
    </footer>
  </PageFrame>
</template>

<style scoped>
.tags-page {
  max-width: var(--page-width, 920px);
  margin: 0 auto;
  padding: var(--page-padding-block, 24px) 24px 48px;
  color: var(--text-primary);
}
.page-heading {
  padding-bottom: 26px;
}
.heading-rule {
  display: block;
  width: 26px;
  height: 3px;
  margin-bottom: 14px;
  border-radius: var(--radius-full);
  background: var(--accent);
}
.page-heading h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-4xl);
  font-weight: 700;
  letter-spacing: -0.03em;
}
.page-lead {
  margin: 14px 0 0;
  /* 中文一行放得下的量程：72ch 约 72 个汉字，超长才换行；
     56ch 会把一句话挤成两三行窄条（截图里就是这样）。 */
  max-width: 72ch;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.8;
}
/* 标签云：按文章数调整字号与圆点大小，热度越高越「重」。 */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.tag-cloud li {
  animation: enter-up 420ms var(--ease-out) both;
}
.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 40px;
  max-width: 100%;
  padding: 8px 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  background: var(--surface-raised);
  color: var(--text-primary);
  font-size: calc(var(--text-sm) + (var(--tag-heat) - 0.35) * 5px);
  font-weight: 600;
  text-decoration: none;
  transition:
    transform var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}
/* 长标签名不能把计数挤到字上：名字可截断，计数不缩。 */
.tag-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tag-pill:hover {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}
.tag-dot {
  width: calc(5px + var(--tag-heat) * 5px);
  height: calc(5px + var(--tag-heat) * 5px);
  border-radius: var(--radius-full);
  background: var(--accent);
  opacity: calc(0.35 + var(--tag-heat) * 0.65);
  transition: transform var(--duration-fast) var(--ease-spring);
}
.tag-pill:hover .tag-dot {
  transform: scale(1.25);
}
.tag-count {
  flex: 0 0 auto;
  min-width: 22px;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  background: var(--surface-control);
  /* 计数是**信息**而不是装饰：用次级档（对芯片面 ≥ 4.5:1），弱化档在这里过不了 AA。 */
  color: var(--text-secondary);
  font-size: var(--text-2xs);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  text-align: center;
}
.tags-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 34px;
  padding-top: 18px;
  border-top: 1px solid var(--border-light);
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.state-panel {
  display: grid;
  justify-items: start;
  gap: 16px;
  padding: 34px 26px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-paper);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.state-panel p {
  font-size: var(--text-sm);
}
@media (max-width: 600px) {
  .tags-page {
    padding: 8px 18px 36px;
  }
  .page-heading h1 {
    font-size: var(--text-3xl);
  }
}
</style>
