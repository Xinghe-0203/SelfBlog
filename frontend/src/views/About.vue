<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSiteStore } from '@/stores/site'
import { getSafeImageUrl } from '@/utils'
import Icon from '@/components/common/Icon.vue'
import PageFrame from '@/components/pages/PageFrame.vue'
import MarkdownContent from '@/components/pages/MarkdownContent.vue'
import { usePageConfig } from '@/composables/usePageConfig'
import { publicationKey, usePagesStore } from '@/stores/pages'
import { usePageSeo } from '@/composables/usePageSeo'
const siteStore = useSiteStore()
const pages = usePagesStore()
const { config, text, module: showModule, isPreview } = usePageConfig('about')
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
    <section v-if="showModule('author')" class="author-profile">
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
  background: var(--surface-primary);
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
  background: var(--surface-primary);
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
  .about-site {
    margin-inline: 6px;
  }
}
</style>
