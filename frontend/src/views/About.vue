<script setup lang="ts">
import { useSiteStore } from '@/stores/site'
const siteStore = useSiteStore()
</script>

<template>
  <div class="about-page">
    <header class="about-hero">
      <p class="eyebrow">ABOUT THIS SITE</p>
      <h1>关于 {{ siteStore.settings.siteTitle }}</h1>
      <p class="lead">{{ siteStore.settings.siteDescription }}</p>
    </header>

    <div class="about-copy">
      <section>
        <h2>{{ siteStore.settings.authorName }}</h2>
        <p>{{ siteStore.settings.authorBio }}</p>
      </section>
      <section>
        <h2>如何浏览</h2>
        <p>
          你可以在首页查看最新文章，在归档页按月份回顾旧文，也可以通过搜索或文章标签找到感兴趣的内容。
        </p>
        <div class="about-links">
          <router-link to="/archives">查看文章归档 <span aria-hidden="true">↗</span></router-link>
          <router-link to="/search">搜索文章 <span aria-hidden="true">↗</span></router-link>
        </div>
      </section>
      <aside class="reader-note">
        <span class="note-mark" aria-hidden="true">“</span>
        <p>感谢阅读。愿每一次记录，都能成为下一次出发的线索。</p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.about-page {
  max-width: 920px;
  margin: 0 auto;
  padding: 58px 24px 30px;
  color: var(--text-primary);
}
.about-hero {
  padding: 38px 0 43px;
  border-bottom: 1px solid var(--border-solid);
}
.eyebrow {
  margin: 0 0 12px;
  color: var(--primary);
  font-size: 0.69rem;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.about-hero h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(2.25rem, 5vw, 3.5rem);
  letter-spacing: -0.05em;
}
.lead {
  max-width: 620px;
  margin: 16px 0 0;
  color: var(--text-secondary);
  font-size: 1.05rem;
  line-height: 1.8;
}
.about-copy {
  max-width: 700px;
  padding-top: 34px;
}
.about-copy section {
  margin-bottom: 35px;
}
.about-copy h2 {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: 1.34rem;
  letter-spacing: -0.025em;
}
.about-copy section p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.96rem;
  line-height: 1.95;
}
.about-links {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
  margin-top: 16px;
}
.about-links a {
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 600;
  text-decoration: none;
}
.about-links a:hover {
  text-decoration: underline;
}
.reader-note {
  display: flex;
  align-items: start;
  gap: 15px;
  margin-top: 40px;
  padding: 22px 24px;
  border-left: 3px solid var(--primary);
  background: var(--bg-secondary);
}
.note-mark {
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 3rem;
  line-height: 0.8;
}
.reader-note p {
  margin: 0;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 1.05rem;
  line-height: 1.7;
}
@media (max-width: 600px) {
  .about-page {
    padding: 34px 18px 20px;
  }
  .about-hero {
    padding: 25px 0 30px;
  }
}
</style>
