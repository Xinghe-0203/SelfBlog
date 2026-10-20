<template>
  <div class="settings-page">
    <header class="page-heading">
      <div>
        <h1>网站设置</h1>
        <p>管理公开资料、阅读体验、安全入口和互动能力。</p>
      </div>
      <button class="save-button btn btn-primary" type="button" :disabled="saving" @click="save">
        {{ saving ? '正在保存…' : '保存设置' }}
      </button>
    </header>
    <p v-if="error" class="alert" role="alert">{{ error }}</p>
    <p v-if="saved" class="success" role="status">设置已保存。</p>
    <form class="settings-grid" @submit.prevent="save">
      <section class="settings-panel">
        <div class="panel-heading">
          <div>
            <h2>站点资料</h2>
            <p>读者会在页面标题、首页和页脚看到这些信息。</p>
          </div>
        </div>
        <label>网站名称<input v-model.trim="form.siteTitle" maxlength="80" required /></label>
        <label
          >网站简介<textarea
            v-model.trim="form.siteDescription"
            rows="3"
            maxlength="1000"
          ></textarea>
        </label>
        <label>作者名称<input v-model.trim="form.authorName" maxlength="80" required /></label>
        <label
          >作者介绍<textarea v-model.trim="form.authorBio" rows="3" maxlength="1000"></textarea>
        </label>
        <SiteAssetField
          :model-value="form.authorAvatar"
          kind="avatar"
          label="作者头像"
          hint="支持上传或手填外链地址。"
          @update:model-value="setAsset('authorAvatar', $event)"
          @uploaded="applyUploadedAsset('authorAvatar', $event)"
        />
        <SiteAssetField
          :model-value="form.siteLogoUrl"
          kind="logo"
          label="站点 Logo"
          hint="公开页顶栏与后台侧边栏的品牌图标；留空使用内置图标。"
          @update:model-value="setAsset('siteLogoUrl', $event)"
          @uploaded="applyUploadedAsset('siteLogoUrl', $event)"
        />
        <SiteAssetField
          :model-value="form.faviconUrl"
          kind="favicon"
          label="网站图标"
          hint="浏览器标签页图标；留空使用默认图标。"
          @update:model-value="setAsset('faviconUrl', $event)"
          @uploaded="applyUploadedAsset('faviconUrl', $event)"
        />
        <label>页脚文字<input v-model.trim="form.footerText" maxlength="1000" /></label>
        <label
          >网站域名<input
            v-model.trim="form.siteBaseUrl"
            inputmode="url"
            maxlength="255"
            placeholder="https://blog.example.com"
          /><small
            >用于页面 canonical、分享卡片、订阅源与 sitemap 的正式地址。留空时回落到服务器 .env 的
            SITE_BASE_URL 或当前访问地址；只填站点地址，不要包含路径或参数。</small
          ></label
        >
      </section>

      <section class="settings-panel">
        <div class="panel-heading">
          <div>
            <h2>后台与访客</h2>
            <p>后台入口可自定义；管理操作仍由管理员登录保护。</p>
          </div>
        </div>
        <label
          >后台路径
          <div class="path-input">
            <span>/</span
            ><input
              v-model.trim="form.adminPath"
              maxlength="40"
              pattern="[A-Za-z0-9][A-Za-z0-9_-]{1,39}"
              required
            />
          </div>
          <small>当前地址：{{ siteStore.adminRoot }}</small></label
        >
        <label class="toggle-row"
          ><span
            ><strong>匿名访客统计</strong
            ><small>记录页面、时间和不可逆访客摘要，不保存原始 IP。</small></span
          ><input
            v-model="form.visitorTrackingEnabled"
            type="checkbox"
            true-value="true"
            false-value="false"
        /></label>
        <div class="security-row">
          <span
            ><strong>登录密码</strong
            ><small>修改成功后会退出登录，请用新密码重新进入后台。</small></span
          >
          <RouterLink to="/password-change" class="btn btn-ghost btn-sm">修改密码</RouterLink>
        </div>
      </section>

      <section class="settings-panel">
        <div class="panel-heading">
          <div>
            <h2>评论与音乐</h2>
            <p>为读者互动和背景声音设置开关。</p>
          </div>
        </div>
        <label class="toggle-row"
          ><span
            ><strong>开放文章评论</strong
            ><small>关闭后访客无法查看或提交评论，已有评论仍保留。</small></span
          ><input
            v-model="form.commentsEnabled"
            type="checkbox"
            true-value="true"
            false-value="false"
        /></label>
        <label class="toggle-row"
          ><span
            ><strong>启用背景音乐</strong
            ><small>浏览器通常会阻止有声音的自动播放；访客可手动点击播放。</small></span
          ><input v-model="form.musicEnabled" type="checkbox" true-value="true" false-value="false"
        /></label>
        <label>音乐名称<input v-model.trim="form.musicTitle" maxlength="80" /></label>
        <SiteAssetField
          :model-value="form.musicUrl"
          kind="music"
          label="背景音乐文件"
          hint="支持 MP3/OGG/WAV/M4A/AAC，最大 20MB；留空则播放器不出现。"
          @update:model-value="setAsset('musicUrl', $event)"
          @uploaded="applyUploadedAsset('musicUrl', $event)"
        />
      </section>

      <section class="settings-panel">
        <div class="panel-heading">
          <div>
            <h2>视觉主题</h2>
            <p>即时切换网站的整体色调。</p>
          </div>
        </div>
        <label
          >页面皮肤<select v-model="form.themeSkin">
            <option value="ocean">海蓝 · 清爽</option>
            <option value="forest">森林 · 自然</option>
            <option value="lavender">薰衣草 · 柔和</option>
            <option value="sunset">日落 · 暖色</option>
            <option value="midnight">午夜 · 深色</option>
          </select></label
        >
        <label
          >主题强调色
          <div class="color-input">
            <input v-model="form.themePrimaryColor" type="color" /><code>{{
              form.themePrimaryColor
            }}</code>
          </div></label
        >
        <div class="skin-preview" :data-skin="form.themeSkin">
          <span></span><span></span><span></span><b>{{ form.siteTitle || '博客预览' }}</b
          ><small>文章 · 归档 · 关于</small>
        </div>
      </section>

      <section class="settings-panel bg-panel">
        <div class="panel-heading">
          <div>
            <h2>页面背景</h2>
            <p class="panel-desc">
              决定全站页面的底色：纯色、渐变或一张背景图；卡片的不透明度与模糊也在同一处调整。
            </p>
          </div>
        </div>
        <div class="bg-body">
          <div class="bg-controls">
            <label
              >背景类型<select v-model="form.backgroundType" data-setting="backgroundType">
                <option value="solid">纯色</option>
                <option value="gradient">渐变</option>
                <option value="image">背景图</option>
              </select></label
            >
            <label v-if="backgroundType === 'solid'"
              >背景颜色
              <div class="color-input">
                <input
                  v-model="form.backgroundColor"
                  type="color"
                  data-setting="backgroundColor"
                /><code>{{ form.backgroundColor }}</code>
              </div></label
            >
            <template v-else-if="backgroundType === 'gradient'">
              <label
                >渐变起点
                <div class="color-input">
                  <input
                    v-model="form.backgroundGradientFrom"
                    type="color"
                    data-setting="backgroundGradientFrom"
                  /><code>{{ form.backgroundGradientFrom }}</code>
                </div></label
              >
              <label
                >渐变终点
                <div class="color-input">
                  <input
                    v-model="form.backgroundGradientTo"
                    type="color"
                    data-setting="backgroundGradientTo"
                  /><code>{{ form.backgroundGradientTo }}</code>
                </div></label
              >
            </template>
            <template v-else>
              <SiteAssetField
                :model-value="form.backgroundImageUrl"
                kind="background"
                label="背景图"
                hint="支持 PNG/JPG/WEBP，最大 8MB；留空则沿用纯色背景。"
                data-setting="backgroundImageUrl"
                @update:model-value="setAsset('backgroundImageUrl', $event)"
                @uploaded="applyUploadedAsset('backgroundImageUrl', $event)"
              />
              <label
                >遮罩浓度
                <div class="range-row">
                  <input
                    v-model="form.backgroundOverlay"
                    type="range"
                    min="0"
                    max="90"
                    step="1"
                    data-setting="backgroundOverlay"
                  /><code>{{ backgroundOverlay }}%</code>
                </div>
                <small>在背景图上叠一层黑色遮罩，数值越高文字越清晰。</small></label
              >
              <label class="toggle-row"
                ><span
                  ><strong>固定背景</strong><small>开启后页面滚动时背景图保持不动。</small></span
                ><input
                  v-model="form.backgroundFixed"
                  type="checkbox"
                  true-value="true"
                  false-value="false"
                  data-setting="backgroundFixed"
              /></label>
            </template>
            <label
              >卡片不透明度
              <div class="range-row">
                <input
                  v-model="form.surfaceOpacity"
                  type="range"
                  min="60"
                  max="100"
                  step="1"
                  data-setting="surfaceOpacity"
                /><code>{{ surfaceOpacity }}%</code>
              </div>
              <small>低于 100% 时卡片呈半透明，需配合模糊避免内容发虚。</small></label
            >
            <label
              >卡片模糊
              <div class="range-row">
                <input
                  v-model="form.surfaceBlur"
                  type="range"
                  min="0"
                  max="40"
                  step="1"
                  data-setting="surfaceBlur"
                /><code>{{ surfaceBlur }}px</code>
              </div>
              <small>为半透明卡片加上背景模糊，0 表示不模糊。</small></label
            >
          </div>
          <div class="bg-preview-column">
            <div
              class="bg-preview"
              :style="previewStyle"
              :data-bg-type="backgroundType"
              data-preview="page-background"
            >
              <span class="bg-preview-card" :style="previewCardStyle">内容卡片</span>
            </div>
            <small class="bg-preview-hint">预览仅为示意，保存后应用到全站。</small>
          </div>
        </div>
      </section>

      <div class="form-footer">
        <p>更改后台路径后将自动跳转到新地址。</p>
        <button class="save-button btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '正在保存…' : '保存所有设置' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useSiteStore, type SiteSettings } from '@/stores/site'
import { siteApi } from '@/api/site'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'

const siteStore = useSiteStore()
const form = reactive<SiteSettings>({ ...siteStore.settings })
const saving = ref(false)
const saved = ref(false)
const error = ref('')

type AssetSettingKey =
  | 'authorAvatar'
  | 'siteLogoUrl'
  | 'faviconUrl'
  | 'musicUrl'
  | 'backgroundImageUrl'

/**
 * 把字符串型数值设置夹到合法区间。
 * 契约里所有设置都是字符串，后端是校验权威；这里只负责让预览和读数在
 * 手输/脏数据下仍然可用，不把非法值渲染成 NaN 或越界的 CSS。
 */
function clampInt(raw: string | undefined, min: number, max: number, fallback: number): number {
  const parsed = Number.parseInt(raw ?? '', 10)
  if (!Number.isFinite(parsed)) return fallback
  return Math.min(max, Math.max(min, parsed))
}

/** 背景类型：只有三种取值，未知值一律按纯色渲染，避免条件分支落空。 */
const backgroundType = computed<'solid' | 'gradient' | 'image'>(() => {
  const value = form.backgroundType
  return value === 'gradient' || value === 'image' ? value : 'solid'
})

const backgroundOverlay = computed(() => clampInt(form.backgroundOverlay, 0, 90, 0))
const surfaceOpacity = computed(() => clampInt(form.surfaceOpacity, 60, 100, 100))
const surfaceBlur = computed(() => clampInt(form.surfaceBlur, 0, 40, 0))

/**
 * 预览用的背景图地址：剥掉服务端同样拒绝的字符（引号/括号/反斜杠/空白），
 * 免得一个未保存的脏值把预览的 CSS 拆坏。服务端仍是最终校验权威。
 * `|| ''` 兜底是必要的：设置接口返回的是部分字段，缺键时读 undefined 会把整页渲染打断。
 */
const previewImageUrl = computed(() => (form.backgroundImageUrl || '').replace(/[\s"'()\\]/g, ''))

/** 只读 form 的预览样式；不写全局 DOM，也不碰 CSS 自定义属性。 */
const previewStyle = computed<Record<string, string>>(() => {
  // 每个分支都建一个显式 Record：直接返回对象字面量会让 TS 推出
  // 各分支键集不同的联合类型，与 Record<string, string> 不兼容。
  const style: Record<string, string> = {}
  if (backgroundType.value === 'gradient') {
    style.backgroundImage = `linear-gradient(135deg, ${form.backgroundGradientFrom}, ${form.backgroundGradientTo})`
    return style
  }
  if (backgroundType.value === 'image') {
    const url = previewImageUrl.value
    style.backgroundColor = form.backgroundColor
    if (!url) {
      style.backgroundImage = 'none'
      return style
    }
    const shade = backgroundOverlay.value / 100
    style.backgroundImage = `linear-gradient(rgba(0, 0, 0, ${shade}), rgba(0, 0, 0, ${shade})), url("${url}")`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center'
    return style
  }
  style.backgroundColor = form.backgroundColor
  style.backgroundImage = 'none'
  return style
})

/** 预览里那张小卡片：用语义 token 混入透明度，深浅主题都跟着走。 */
const previewCardStyle = computed<Record<string, string>>(() => {
  const blur = surfaceBlur.value
  return {
    backgroundColor: `color-mix(in srgb, var(--surface-primary) ${surfaceOpacity.value}%, transparent)`,
    backdropFilter: blur > 0 ? `blur(${blur}px)` : 'none'
  }
})

function setAsset(key: AssetSettingKey, url: string): void {
  form[key] = url.trim()
}

/**
 * 上传成功：写表单 + 同步 store（导航栏 Logo、favicon 立即换新）。
 * 只有上传走这条即时同步；手填 URL 只写表单，保存时才生效。
 */
function applyUploadedAsset(key: AssetSettingKey, url: string): void {
  setAsset(key, url)
  siteStore.applySettings({ [key]: url.trim() })
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
    error.value =
      (err as Error & { userMessage?: string }).userMessage || '保存失败，请检查填写内容后重试。'
  } finally {
    saving.value = false
  }
}

onMounted(() => void load())
</script>

<style scoped>
.settings-page {
  display: grid;
  gap: 18px;
  color: var(--text-primary);
}
.page-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}
.page-heading h1 {
  margin: 0 0 8px;
  font-size: var(--text-2xl);
}
.page-heading p:not(.eyebrow) {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.settings-panel {
  display: grid;
  align-content: start;
  gap: 15px;
  padding: 20px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.panel-heading {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.panel-heading h2 {
  margin: 0 0 5px;
  font-size: var(--text-base);
}
.panel-heading p {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.settings-panel > label:not(.toggle-row) {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
input:not([type='checkbox']):not([type='color']):not([type='range']),
textarea,
select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-base);
}
textarea {
  resize: vertical;
}
label small {
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.toggle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 13px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
}
.toggle-row span {
  display: grid;
  gap: 4px;
}
.toggle-row strong {
  font-size: var(--text-sm);
}
.toggle-row small {
  max-width: 400px;
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.toggle-row input {
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  accent-color: var(--accent);
}
.security-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 13px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
}
.security-row span {
  display: grid;
  gap: 4px;
}
.security-row strong {
  font-size: var(--text-sm);
}
.security-row small {
  max-width: 400px;
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.path-input {
  display: flex;
  align-items: center;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
}
.path-input span {
  padding-left: 12px;
  color: var(--text-muted);
}
.path-input input {
  border: 0 !important;
  background: transparent !important;
}
.color-input {
  display: flex;
  align-items: center;
  gap: 10px;
}
.color-input input {
  width: 46px;
  height: 38px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  cursor: pointer;
}
.color-input code {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.skin-preview {
  display: grid;
  grid-template-columns: 7px 1fr;
  align-content: center;
  gap: 8px;
  min-height: 85px;
  padding: 15px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
}
.skin-preview span {
  grid-column: 1;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
}
.skin-preview b,
.skin-preview small {
  grid-column: 2;
}
.skin-preview b {
  font-size: var(--text-sm);
}
.skin-preview small {
  color: var(--text-muted);
  font-size: var(--text-2xs);
}
.bg-panel {
  grid-column: 1 / -1;
}
.bg-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}
.bg-controls {
  display: grid;
  gap: 15px;
}
.bg-controls > label:not(.toggle-row) {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.range-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.range-row input[type='range'] {
  --range-fill: var(--accent);
  --range-rail: var(--border-strong);
  flex: 1 1 auto;
  min-width: 0;
  height: 22px;
  margin: 0;
  appearance: none;
  -webkit-appearance: none;
  border: 0;
  background: transparent;
  cursor: pointer;
}
.range-row input[type='range']::-webkit-slider-runnable-track {
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--range-rail);
}
.range-row input[type='range']::-webkit-slider-thumb {
  appearance: none;
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  margin-top: -5px;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--range-fill);
}
.range-row input[type='range']::-moz-range-track {
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--range-rail);
}
.range-row input[type='range']::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--range-fill);
}
.range-row code {
  flex: 0 0 auto;
  min-width: 48px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  font-variant-numeric: tabular-nums;
  text-align: right;
}
.bg-preview-column {
  display: grid;
  gap: 8px;
}
.bg-preview {
  display: grid;
  place-items: center;
  min-height: 168px;
  padding: 18px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
}
.bg-preview-card {
  display: grid;
  place-items: center;
  width: 100%;
  max-width: 220px;
  min-height: 84px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
}
.bg-preview-hint {
  color: var(--text-muted);
  font-size: var(--text-2xs);
  line-height: 1.6;
}
.form-footer {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 2px;
}
.form-footer p {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.alert,
.success {
  margin: 0;
  padding: 12px 13px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
}
.alert {
  background: var(--error-light);
  color: var(--error);
}
.success {
  background: var(--success-light);
  color: var(--success);
}
.skin-preview[data-skin='forest'] {
  --primary: #2d7c54;
  background: #edf2e5;
}
.skin-preview[data-skin='lavender'] {
  --primary: #7555b7;
  background: #f0edf8;
}
.skin-preview[data-skin='sunset'] {
  --primary: #c45b3e;
  background: #f6ede5;
}
.skin-preview[data-skin='midnight'] {
  --primary: #69aef7;
  background: #172332;
  color: #ecf2f8;
}
@media (max-width: 760px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
  .bg-body {
    grid-template-columns: 1fr;
  }
  .form-footer {
    grid-column: 1;
  }
}
@media (max-width: 500px) {
  .page-heading {
    align-items: start;
    flex-direction: column;
  }
  .form-footer {
    align-items: start;
    flex-direction: column;
  }
}
</style>
