<template>
  <div class="settings-page">
    <header class="page-heading"><div><p class="eyebrow">SITE CONTROL</p><h1>网站设置</h1><p>管理公开资料、阅读体验、安全入口和互动能力。</p></div><button class="save-button" type="button" :disabled="saving" @click="save">{{ saving ? '正在保存…' : '保存设置' }}</button></header>
    <p v-if="error" class="alert" role="alert">{{ error }}</p>
    <p v-if="saved" class="success" role="status">设置已保存。</p>
    <form class="settings-grid" @submit.prevent="save">
      <section class="settings-panel"><div class="panel-heading"><span>01</span><div><h2>站点资料</h2><p>读者会在页面标题、首页和页脚看到这些信息。</p></div></div>
        <label>网站名称<input v-model.trim="form.siteTitle" maxlength="80" required /></label>
        <label>网站简介<textarea v-model.trim="form.siteDescription" rows="3" maxlength="1000"></textarea></label>
        <label>作者名称<input v-model.trim="form.authorName" maxlength="80" required /></label>
        <label>作者介绍<textarea v-model.trim="form.authorBio" rows="3" maxlength="1000"></textarea></label>
        <SiteAssetField :model-value="form.authorAvatar" kind="avatar" label="作者头像" hint="支持上传或手填外链地址。" @update:model-value="applyAsset('authorAvatar', $event)" />
        <SiteAssetField :model-value="form.siteLogoUrl" kind="logo" label="站点 Logo" hint="公开页顶栏与后台侧边栏的品牌图标；留空使用内置图标。" @update:model-value="applyAsset('siteLogoUrl', $event)" />
        <SiteAssetField :model-value="form.faviconUrl" kind="favicon" label="网站图标" hint="浏览器标签页图标；留空使用默认图标。" @update:model-value="applyAsset('faviconUrl', $event)" />
        <label>页脚文字<input v-model.trim="form.footerText" maxlength="1000" /></label>
      </section>

      <section class="settings-panel"><div class="panel-heading"><span>02</span><div><h2>后台与访客</h2><p>后台入口可自定义；管理操作仍由管理员登录保护。</p></div></div>
        <label>后台路径<div class="path-input"><span>/</span><input v-model.trim="form.adminPath" maxlength="40" pattern="[A-Za-z0-9][A-Za-z0-9_-]{1,39}" required /></div><small>当前地址：{{ siteStore.adminRoot }}</small></label>
        <label class="toggle-row"><span><strong>匿名访客统计</strong><small>记录页面、时间和不可逆访客摘要，不保存原始 IP。</small></span><input v-model="form.visitorTrackingEnabled" type="checkbox" true-value="true" false-value="false" /></label>
      </section>

      <section class="settings-panel"><div class="panel-heading"><span>03</span><div><h2>评论与音乐</h2><p>为读者互动和背景声音设置开关。</p></div></div>
        <label class="toggle-row"><span><strong>开放文章评论</strong><small>关闭后访客无法查看或提交评论，已有评论仍保留。</small></span><input v-model="form.commentsEnabled" type="checkbox" true-value="true" false-value="false" /></label>
        <label class="toggle-row"><span><strong>启用背景音乐</strong><small>浏览器通常会阻止有声音的自动播放；访客可手动点击播放。</small></span><input v-model="form.musicEnabled" type="checkbox" true-value="true" false-value="false" /></label>
        <label>音乐名称<input v-model.trim="form.musicTitle" maxlength="80" /></label>
        <SiteAssetField :model-value="form.musicUrl" kind="music" label="背景音乐文件" hint="支持 MP3/OGG/WAV/M4A/AAC，最大 20MB；留空则播放器不出现。" @update:model-value="applyAsset('musicUrl', $event)" />
      </section>

      <section class="settings-panel"><div class="panel-heading"><span>04</span><div><h2>视觉主题</h2><p>即时切换网站的整体色调。</p></div></div>
        <label>页面皮肤<select v-model="form.themeSkin"><option value="ocean">海蓝 · 清爽</option><option value="forest">森林 · 自然</option><option value="lavender">薰衣草 · 柔和</option><option value="sunset">日落 · 暖色</option><option value="midnight">午夜 · 深色</option></select></label>
        <label>主题强调色<div class="color-input"><input v-model="form.themePrimaryColor" type="color" /><code>{{ form.themePrimaryColor }}</code></div></label>
        <div class="skin-preview" :data-skin="form.themeSkin"><span></span><span></span><span></span><b>{{ form.siteTitle || '博客预览' }}</b><small>文章 · 归档 · 关于</small></div>
      </section>

      <div class="form-footer"><p>更改后台路径后将自动跳转到新地址。</p><button class="save-button" type="submit" :disabled="saving">{{ saving ? '正在保存…' : '保存所有设置' }}</button></div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useSiteStore, type SiteSettings } from '@/stores/site'
import { siteApi } from '@/api/site'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'

const siteStore = useSiteStore()
const form = reactive<SiteSettings>({ ...siteStore.settings })
const saving = ref(false)
const saved = ref(false)
const error = ref('')

type AssetSettingKey = 'authorAvatar' | 'siteLogoUrl' | 'faviconUrl' | 'musicUrl'

/**
 * 上传成功先写表单 + 同步 store（导航栏 Logo、favicon 立即换新），
 * 真正落库要等「保存设置」——与 spec 的乐观更新约定一致。
 */
function applyAsset(key: AssetSettingKey, url: string): void {
  const patch: Partial<SiteSettings> = { [key]: url.trim() }
  Object.assign(form, patch)
  siteStore.applySettings(patch)
}

async function load() {
  try {
    const response = await siteApi.getSettings()
    if (response.data) Object.assign(form, response.data)
  } catch {
    error.value = '设置暂时无法加载，请刷新后重试。'
  }
}

async function save() {
  saving.value = true
  saved.value = false
  error.value = ''
  const previousAdminPath = siteStore.adminPath
  try {
    const response = await siteApi.updateSettings({ ...form })
    if (response.data) {
      Object.assign(form, response.data)
      siteStore.applySettings(response.data)
    }
    saved.value = true
    if (previousAdminPath !== siteStore.adminPath) {
      window.location.assign(`${siteStore.adminRoot}/settings`)
    }
  } catch (err) {
    error.value = (err as Error & { userMessage?: string }).userMessage || '保存失败，请检查填写内容后重试。'
  } finally {
    saving.value = false
  }
}

onMounted(() => void load())
</script>

<style scoped>
.settings-page { display: grid; gap: 18px; color: var(--text-primary); }
.page-heading { display: flex; align-items: end; justify-content: space-between; gap: 16px; }
.page-heading h1 { margin: 5px 0; font: 700 2rem var(--font-display); }
.page-heading p:not(.eyebrow) { margin: 0; color: var(--text-muted); font-size: .8rem; }
.eyebrow { margin: 0; color: var(--primary); font-size: .68rem; font-weight: 700; letter-spacing: .16em; }
.save-button { padding: 10px 15px; border: 0; border-radius: 8px; background: var(--primary); color: white; font-weight: 700; cursor: pointer; }
.save-button:disabled { opacity: .6; cursor: wait; }
.settings-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.settings-panel { display: grid; align-content: start; gap: 15px; padding: 20px; border: 1px solid var(--border-solid); border-radius: 13px; background: var(--surface-primary); }
.panel-heading { display: flex; gap: 12px; padding-bottom: 12px; border-bottom: 1px solid var(--border-solid); }
.panel-heading > span { color: var(--primary); font: 700 .75rem var(--font-display); }
.panel-heading h2 { margin: 0 0 5px; font-size: 1rem; }
.panel-heading p { margin: 0; color: var(--text-muted); font-size: .74rem; line-height: 1.5; }
.settings-panel > label:not(.toggle-row) { display: grid; gap: 7px; color: var(--text-secondary); font-size: .78rem; }
input:not([type=checkbox]):not([type=color]), textarea, select { width: 100%; padding: 10px 11px; border: 1px solid var(--border-solid); border-radius: 8px; background: var(--bg-primary); color: var(--text-primary); font: inherit; font-size: .84rem; }
textarea { resize: vertical; }
label small { color: var(--text-muted); font-size: .7rem; line-height: 1.5; }
.toggle-row { display: flex; justify-content: space-between; align-items: center; gap: 14px; padding: 12px; border: 1px solid var(--border-solid); border-radius: 9px; }
.toggle-row span { display: grid; gap: 4px; }
.toggle-row strong { font-size: .82rem; }
.toggle-row small { max-width: 400px; color: var(--text-muted); font-size: .7rem; line-height: 1.5; }
.toggle-row input { flex: 0 0 auto; width: 18px; height: 18px; accent-color: var(--primary); }
.path-input { display: flex; align-items: center; border: 1px solid var(--border-solid); border-radius: 8px; background: var(--bg-primary); }
.path-input span { padding-left: 12px; color: var(--text-muted); }.path-input input { border: 0 !important; background: transparent !important; }
.color-input { display: flex; align-items: center; gap: 10px; }.color-input input { width: 46px; height: 37px; border: 1px solid var(--border-solid); border-radius: 7px; background: var(--surface-primary); cursor: pointer; }
.color-input code { color: var(--text-secondary); font-size: .8rem; }
.skin-preview { display: grid; grid-template-columns: 7px 1fr; align-content: center; gap: 8px; min-height: 85px; padding: 15px; border: 1px solid var(--border-solid); border-radius: 10px; background: var(--bg-secondary); }
.skin-preview span { grid-column: 1; width: 6px; height: 6px; border-radius: 50%; background: var(--primary); }.skin-preview b, .skin-preview small { grid-column: 2; }.skin-preview b { font-size: .84rem; }.skin-preview small { color: var(--text-muted); font-size: .68rem; }
.form-footer { grid-column: 1 / -1; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 2px; }.form-footer p { color: var(--text-muted); font-size: .75rem; }
.alert, .success { margin: 0; padding: 11px 13px; border-radius: 8px; font-size: .8rem; }.alert { background: #fff0ed; color: var(--error); }.success { background: #e8f8ef; color: #16794c; }
.skin-preview[data-skin='forest'] { --primary: #2d7c54; background: #edf2e5; }.skin-preview[data-skin='lavender'] { --primary: #7555b7; background: #f0edf8; }.skin-preview[data-skin='sunset'] { --primary: #c45b3e; background: #f6ede5; }.skin-preview[data-skin='midnight'] { --primary: #69aef7; background: #172332; color: #ecf2f8; }
@media (max-width: 760px) { .settings-grid { grid-template-columns: 1fr; }.form-footer { grid-column: 1; } }
@media (max-width: 500px) { .page-heading { align-items: start; flex-direction: column; }.form-footer { align-items: start; flex-direction: column; } }
</style>
