<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSiteStore } from '@/stores/site'
import { getSafeImageUrl } from '@/utils'
import Icon from '@/components/common/Icon.vue'
import PageFrame from '@/components/pages/PageFrame.vue'
import MarkdownContent from '@/components/pages/MarkdownContent.vue'
import { usePageConfig, BANNER_CARD_HEIGHTS } from '@/composables/usePageConfig'
import { publicationKey, usePagesStore } from '@/stores/pages'
import { usePageSeo } from '@/composables/usePageSeo'
const siteStore = useSiteStore()
const pages = usePagesStore()
const {
  config,
  text,
  module: showModule,
  isPreview,
  banner,
  bannerVars
} = usePageConfig('about', { heights: BANNER_CARD_HEIGHTS })
/**
 * 横幅的版式 class。
 *
 * 没有横幅时返回空对象 —— 作者卡的 class、style 属性与 DOM 结构都和改造前完全一致。
 */
const bannerClass = computed(() => {
  const current = banner.value
  if (!current) return {}
  return {
    'author-profile--banner': true,
    [`author-profile--banner-${current.layout}`]: true
  }
})
/** 文字明暗：tone=light，或 auto 且遮罩够深（≥40）时用浅色。 */
const bannerTone = computed(() => {
  const current = banner.value
  if (!current) return undefined
  return current.light ? 'light' : 'dark'
})
usePageSeo('about', () => ({
  title: text('title', '关于'),
  description: text('description', siteStore.settings.authorBio),
  url: '/about'
}))
const bodyError = ref('')
let bodyRequest = 0
async function loadBody() {
  const request = ++bodyRequest
  bodyError.value = ''
  try {
    await pages.loadPage('about')
  } catch {
    if (request === bodyRequest) bodyError.value = '自定义介绍暂时无法加载，可以重试。'
  }
}
onMounted(() => {
  if (!isPreview) void loadBody()
})
watch(
  () => (pages.pages.about ? publicationKey(pages.pages.about) : ''),
  () => {
    if (!isPreview && pages.pages.about && pages.pages.about.config.body == null) void loadBody()
  }
)
onBeforeUnmount(() => {
  bodyRequest++
})
</script>

<template>
  <PageFrame :config="config" class="about-page">
    <section
      v-if="showModule('author')"
      class="author-profile"
      :class="bannerClass"
      :style="bannerVars"
      :data-banner-tone="bannerTone"
    >
      <div v-if="banner" class="profile-banner" aria-hidden="true">
        <div class="profile-banner-media"></div>
        <div class="profile-banner-scrim"></div>
      </div>
      <img
        v-if="siteStore.settings.authorAvatar"
        class="profile-avatar"
        :src="getSafeImageUrl(siteStore.settings.authorAvatar)"
        alt="作者头像"
      />
      <span v-else class="profile-avatar profile-initial" aria-hidden="true">{{
        siteStore.settings.authorName.slice(0, 1)
      }}</span>
      <div class="profile-copy">
        <p class="profile-label">关于作者</p>
        <h1>{{ text('title', siteStore.settings.authorName) }}</h1>
        <p v-if="text('description', siteStore.settings.authorBio)" class="author-bio">
          {{ text('description', siteStore.settings.authorBio) }}
        </p>
      </div>
    </section>
    <header v-else class="about-heading">
      <h1>{{ text('title', '关于') }}</h1>
      <p v-if="text('description', '')">{{ text('description', '') }}</p>
    </header>
    <p v-if="bodyError" class="body-error" role="alert">
      {{ bodyError }} <button type="button" @click="loadBody">重试</button>
    </p>
    <MarkdownContent v-if="config.body" class="about-body" :source="config.body" />
    <section v-if="showModule('site') || showModule('links')" class="about-site">
      <template v-if="showModule('site')"
        ><h2>{{ siteStore.settings.siteTitle }}</h2>
        <p>{{ siteStore.settings.siteDescription }}</p></template
      >
      <nav v-if="showModule('links')" aria-label="继续阅读">
        <router-link to="/archives"><Icon name="archive" :size="18" />文章归档</router-link
        ><router-link to="/gallery"><Icon name="image" :size="18" />相册</router-link
        ><router-link to="/search"><Icon name="search" :size="18" />搜索文章</router-link>
      </nav>
    </section>
  </PageFrame>
</template>

<style scoped>
.about-page {
  max-width: var(--page-width, 920px);
  margin-inline: auto;
  padding: var(--page-padding-block, 24px) 24px 48px;
}
.about-heading h1 {
  font-size: var(--text-3xl);
}
.about-heading p {
  margin-top: 16px;
  white-space: pre-line;
}
.about-body {
  margin-top: var(--page-section-gap, 32px);
}
.body-error {
  margin-block: 20px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.body-error button {
  min-height: 44px;
  padding-inline: 8px;
  color: var(--accent);
}
.author-profile {
  display: flex;
  align-items: flex-start;
  gap: 28px;
  padding: 34px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  /* 浮起档：原先这里写死 --surface-primary，于是壁纸模式下作者卡是唯一的
     不透明白块，和周围半透明的卡片「风格割裂」（用户截图 2 的原话）。
     改用档位变量后，纯色画布别名回 --surface-primary（逐像素不变），
     壁纸画布则与 .card/.sidebar-block 落在同一档。 */
  background: var(--surface-raised);
}
.profile-avatar {
  flex: 0 0 80px;
  width: 80px;
  height: 80px;
  border-radius: var(--radius-full);
  object-fit: cover;
}
.profile-initial {
  display: grid;
  place-items: center;
  background: var(--accent-light);
  color: var(--accent);
  font-size: var(--text-3xl);
}
.profile-copy {
  min-width: 0;
}
.profile-label {
  font-size: var(--text-xs);
  color: var(--text-muted);
  margin-bottom: 7px;
}
.profile-copy h1 {
  font-size: var(--text-3xl);
  overflow-wrap: anywhere;
}
.author-bio {
  margin-top: 18px;
  white-space: pre-line;
  overflow-wrap: anywhere;
  font-size: var(--text-base);
  line-height: 1.9;
}
/* ---------------------------------------------------------------------------
   横幅背景（作者在「页面管理 → 关于」里设置）
   ---------------------------------------------------------------------------
   没有横幅时下面每一条规则都不生效：作者卡的 class、style 属性与 DOM 结构
   都和改造前完全一致。变量由 usePageConfig 的 pageBannerVars() 拼好，
   背景图地址已经过白名单校验。 */
.author-profile--banner {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  min-height: var(--banner-height, 210px);
}
.profile-banner {
  position: absolute;
  z-index: 0;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.profile-banner-media {
  position: absolute;
  inset: 0;
  background-image: var(--banner-image);
  background-size: var(--banner-size, cover);
  background-position: var(--banner-position, center);
  background-repeat: no-repeat;
}
.profile-banner-scrim {
  position: absolute;
  inset: 0;
  /* 遮罩用「永远深色」的语义 token（--code-bg），不用会被暗色模式反转的灰度梯度。 */
  background: color-mix(in srgb, var(--code-bg) calc(var(--banner-overlay, 0) * 1%), transparent);
}
/* 左右分栏：头像与文字在左，背景图在右。 */
.author-profile--banner-split .profile-banner {
  position: relative;
  inset: auto;
  order: 3;
  align-self: stretch;
  width: 40%;
  min-height: var(--banner-height, 210px);
  border-radius: var(--radius-lg);
}
.author-profile--banner-split .profile-copy {
  flex: 1 1 auto;
}
/* 居中：整卡铺图，文字压在上面。 */
.author-profile--banner-centered .profile-avatar,
.author-profile--banner-centered .profile-copy {
  position: relative;
  z-index: 1;
}
.author-profile--banner-centered[data-banner-tone='light'] .profile-label,
.author-profile--banner-centered[data-banner-tone='light'] .profile-copy h1,
.author-profile--banner-centered[data-banner-tone='light'] .author-bio {
  color: var(--text-inverse);
}
/* 上下堆叠：图在上，文字在下。 */
.author-profile--banner-stacked {
  flex-direction: column;
}
.author-profile--banner-stacked .profile-banner {
  position: relative;
  inset: auto;
  order: -1;
  width: 100%;
  height: calc(var(--banner-height, 210px) * 0.58);
  border-radius: var(--radius-lg);
}
.about-site {
  margin: var(--page-section-gap, 32px) 34px 0;
}
.about-site h2 {
  font-size: var(--text-lg);
}
.about-site p {
  margin-block: 12px 20px;
  font-size: var(--text-sm);
}
.about-site nav {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.about-site a {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  /* 控件档：这三个芯片原先是不透明白块，和周围半透明表面「像另一套设计」。
     档位变量让它们与首页的芯片/按钮共用同一档底色。 */
  background: var(--surface-control);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.about-site a:hover {
  color: var(--accent);
  border-color: var(--accent);
}
@media (max-width: 600px) {
  .about-page {
    padding: 8px 18px 36px;
  }
  .author-profile {
    flex-direction: column;
    gap: 20px;
    padding: 24px;
  }
  .profile-avatar {
    flex-basis: 64px;
    width: 64px;
    height: 64px;
  }
  .profile-copy h1 {
    font-size: var(--text-2xl);
  }
  /* 横幅：窄屏下左右分栏退化成上下堆叠（图在上、文字在下）。 */
  .author-profile--banner-split .profile-banner {
    order: -1;
    width: 100%;
    height: calc(var(--banner-height, 210px) * 0.5);
    min-height: 0;
  }
  .about-site {
    margin-inline: 6px;
  }
}
</style>
