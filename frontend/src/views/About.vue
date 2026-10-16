<script setup lang="ts">
import { useSiteStore } from '@/stores/site'
import { getSafeImageUrl } from '@/utils'
import Icon from '@/components/common/Icon.vue'
const siteStore = useSiteStore()
</script>

<template>
  <div class="about-page">
    <section class="author-profile">
      <img v-if="siteStore.settings.authorAvatar" class="profile-avatar" :src="getSafeImageUrl(siteStore.settings.authorAvatar)" alt="作者头像" />
      <span v-else class="profile-avatar profile-initial" aria-hidden="true">{{ siteStore.settings.authorName.slice(0, 1) }}</span>
      <div class="profile-copy"><p class="profile-label">关于作者</p><h1>{{ siteStore.settings.authorName }}</h1><p class="author-bio">{{ siteStore.settings.authorBio }}</p></div>
    </section>
    <section class="about-site"><h2>{{ siteStore.settings.siteTitle }}</h2><p>{{ siteStore.settings.siteDescription }}</p><nav aria-label="继续阅读"><router-link to="/archives"><Icon name="archive" :size="18" />文章归档</router-link><router-link to="/gallery"><Icon name="image" :size="18" />相册</router-link><router-link to="/search"><Icon name="search" :size="18" />搜索文章</router-link></nav></section>
  </div>
</template>

<style scoped>
.about-page { max-width: 920px; margin-inline: auto; padding: 24px 24px 48px; }
.author-profile { display: flex; align-items: flex-start; gap: 28px; padding: 34px; border: 1px solid var(--border-light); border-radius: var(--radius-xl); background: var(--surface-primary); }
.profile-avatar { flex: 0 0 80px; width: 80px; height: 80px; border-radius: var(--radius-full); object-fit: cover; }
.profile-initial { display: grid; place-items: center; background: var(--accent-light); color: var(--accent); font-size: 28px; }
.profile-copy { min-width: 0; }
.profile-label { font-size: 12px; color: var(--text-muted); margin-bottom: 7px; }
.profile-copy h1 { font-size: 30px; overflow-wrap: anywhere; }
.author-bio { margin-top: 18px; white-space: pre-line; overflow-wrap: anywhere; font-size: 16px; line-height: 1.9; }
.about-site { margin: 32px 34px 0; }
.about-site h2 { font-size: 18px; }
.about-site p { margin-block: 12px 20px; font-size: 14px; }
.about-site nav { display: flex; flex-wrap: wrap; gap: 12px; }
.about-site a { display: flex; align-items: center; gap: 8px; min-height: 44px; padding: 8px 12px; border: 1px solid var(--border-light); border-radius: var(--radius); background: var(--surface-primary); color: var(--text-secondary); font-size: 13px; }
.about-site a:hover { color: var(--accent); border-color: var(--accent); }
@media (max-width: 600px) { .about-page { padding: 8px 18px 36px; } .author-profile { flex-direction: column; gap: 20px; padding: 24px; } .profile-avatar { flex-basis: 64px; width: 64px; height: 64px; } .profile-copy h1 { font-size: 26px; } .about-site { margin-inline: 6px; } }
</style>
