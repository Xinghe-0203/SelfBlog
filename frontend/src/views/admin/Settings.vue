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

    <nav class="settings-tabs" role="tablist" aria-label="设置分组">
      <button
        v-for="tab in TABS"
        :key="tab.id"
        class="settings-tab"
        :class="{ 'is-active': activeTab === tab.id }"
        type="button"
        role="tab"
        :aria-selected="activeTab === tab.id"
        :data-tab="tab.id"
        @click="activeTab = tab.id"
      >
        <span class="settings-tab-label">{{ tab.label }}</span>
        <small class="settings-tab-hint">{{ tab.hint }}</small>
      </button>
    </nav>

    <form class="settings-grid" @submit.prevent="save">
      <!-- 站点资料 -->
      <div v-show="activeTab === 'profile'" class="tab-panels" data-tab-panel="profile">
        <section class="settings-panel settings-panel--wide" data-panel="profile">
          <div class="panel-heading">
            <div>
              <h2>站点资料</h2>
              <p>读者会在页面标题、首页和页脚看到这些信息。</p>
            </div>
          </div>
          <div class="field-grid">
            <label>网站名称<input v-model.trim="form.siteTitle" maxlength="80" required /></label>
            <label>作者名称<input v-model.trim="form.authorName" maxlength="80" required /></label>
            <label
              >网站简介<textarea
                v-model.trim="form.siteDescription"
                rows="3"
                maxlength="1000"
              ></textarea>
            </label>
            <label
              >作者介绍<textarea v-model.trim="form.authorBio" rows="3" maxlength="1000"></textarea>
            </label>
            <label>页脚文字<input v-model.trim="form.footerText" maxlength="1000" /></label>
            <label
              >网站域名<input
                v-model.trim="form.siteBaseUrl"
                inputmode="url"
                maxlength="255"
                placeholder="https://blog.example.com"
              /><small
                >用于页面 canonical、分享卡片、订阅源与 sitemap 的正式地址。留空时回落到服务器 .env
                的 SITE_BASE_URL 或当前访问地址；只填站点地址，不要包含路径或参数。</small
              ></label
            >
          </div>
          <div class="asset-grid">
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
          </div>
        </section>
      </div>

      <!-- 外观 -->
      <div v-show="activeTab === 'appearance'" class="tab-panels" data-tab-panel="appearance">
        <section class="settings-panel settings-panel--wide" data-panel="presets">
          <div class="panel-heading">
            <div>
              <h2>成品主题</h2>
              <p>
                一整套搭配好的视觉配方：配色、字体、圆角、阴影、画布渐变与卡片玻璃感一次换掉；之后仍可逐项微调。
              </p>
            </div>
          </div>
          <div class="preset-grid">
            <button
              v-for="preset in THEME_PRESETS"
              :key="preset.id"
              class="preset-card"
              :class="{ 'is-active': isPresetActive(preset) }"
              type="button"
              :data-preset="preset.id"
              @click="applyPreset(preset)"
            >
              <span class="preset-swatches" aria-hidden="true">
                <i :style="{ backgroundColor: preset.primaryColor }"></i>
                <i :style="{ backgroundColor: preset.background.from }"></i>
                <i :style="{ backgroundColor: preset.background.to }"></i>
              </span>
              <strong class="preset-name">{{ preset.name }}</strong>
              <small class="preset-desc">{{ preset.description }}</small>
              <span v-if="isPresetActive(preset)" class="preset-flag">使用中</span>
            </button>
          </div>
        </section>

        <section class="settings-panel" data-panel="skin">
          <div class="panel-heading">
            <div>
              <h2>视觉主题</h2>
              <p>即时切换网站的整体色调。</p>
            </div>
          </div>
          <label
            >页面皮肤<select v-model="form.themeSkin" data-setting="themeSkin">
              <option value="ocean">海蓝 · 清爽</option>
              <option value="forest">森林 · 自然</option>
              <option value="lavender">薰衣草 · 柔和</option>
              <option value="sunset">日落 · 暖色</option>
              <option value="midnight">午夜 · 深色</option>
              <option value="ink">墨青 · 素雅</option>
              <option value="dusk">暮紫 · 柔和</option>
              <option value="sand">暖砂 · 温润</option>
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

        <section class="settings-panel" data-panel="glass">
          <div class="panel-heading">
            <div>
              <h2>玻璃感</h2>
              <p>卡片要不要半透明并带背景模糊。</p>
            </div>
          </div>
          <label class="toggle-row"
            ><span
              ><strong>玻璃感卡片</strong
              ><small
                >开启后卡片半透明并透出背景，层次更轻；关闭后卡片为完全不透明的实色。</small
              ></span
            ><input
              v-model="form.glassMode"
              type="checkbox"
              true-value="on"
              false-value="off"
              data-setting="glassMode"
          /></label>
          <label
            >卡片不透明度
            <div class="range-row">
              <input
                v-model="form.surfaceOpacity"
                type="range"
                min="60"
                max="92"
                step="1"
                :disabled="!glassEnabled"
                data-setting="surfaceOpacity"
              /><code>{{ surfaceOpacity }}%</code>
            </div>
            <small>玻璃感开启时最高 92%，完全实色请关闭玻璃感。</small></label
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
                :disabled="!glassEnabled"
                data-setting="surfaceBlur"
              /><code>{{ surfaceBlur }}px</code>
            </div>
            <small>为半透明卡片加上背景模糊，0 表示不模糊。</small></label
          >
          <p v-if="!glassEnabled" class="panel-hint">关闭后卡片为完全不透明的实色。</p>
        </section>

        <section class="settings-panel settings-panel--wide bg-panel" data-panel="background">
          <div class="panel-heading">
            <div>
              <h2>页面背景</h2>
              <p class="panel-desc">
                决定全站页面的底色：纯色、渐变或一张背景图；卡片的不透明度与模糊在「玻璃感」里调整。
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

        <section
          class="settings-panel settings-panel--wide appearance-panel"
          data-panel="appearance"
        >
          <div class="panel-heading panel-heading-row">
            <div>
              <h2>外观深度自定义</h2>
              <p>
                这些设置只影响前台内容区（正文、卡片、列表与首页头图），导航栏与后台界面保持不变。「恢复默认外观」只改本页表单，仍需点底部保存才会生效。
              </p>
            </div>
            <button
              class="btn btn-ghost btn-sm"
              type="button"
              data-action="reset-appearance"
              @click="resetAppearance"
            >
              恢复默认外观
            </button>
          </div>
          <div class="appearance-grid">
            <label
              >正文字体<select v-model="form.fontFamily" data-setting="fontFamily">
                <option value="system">系统默认</option>
                <option value="sans">无衬线</option>
                <option value="serif">衬线（宋体）</option>
                <option value="mono">等宽</option>
              </select></label
            >
            <label
              >正文字号
              <div class="range-row">
                <input
                  type="range"
                  min="14"
                  max="19"
                  step="1"
                  data-setting="baseFontSize"
                  :value="appearanceNumber('baseFontSize')"
                  @input="setAppearanceNumber('baseFontSize', $event)"
                /><code>{{ appearanceReadout('baseFontSize') }}</code>
              </div>
              <small>14–19 px，只作用于文章与页面正文。</small></label
            >
            <label
              >正文行高
              <div class="range-row">
                <input
                  type="range"
                  min="1.5"
                  max="2.2"
                  step="0.05"
                  data-setting="bodyLineHeight"
                  :value="appearanceNumber('bodyLineHeight')"
                  @input="setAppearanceNumber('bodyLineHeight', $event)"
                /><code>{{ appearanceReadout('bodyLineHeight') }}</code>
              </div>
              <small>1.5–2.2 倍行距，越大越松散。</small></label
            >
            <label
              >阅读栏宽
              <div class="range-row">
                <input
                  type="range"
                  min="640"
                  max="960"
                  step="10"
                  data-setting="readerWidth"
                  :value="appearanceNumber('readerWidth')"
                  @input="setAppearanceNumber('readerWidth', $event)"
                /><code>{{ appearanceReadout('readerWidth') }}</code>
              </div>
              <small>640–960 px，正文可读性与宽屏留白的取舍。</small></label
            >
            <label
              >段落间距
              <div class="range-row">
                <input
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  data-setting="paragraphSpacing"
                  :value="appearanceNumber('paragraphSpacing')"
                  @input="setAppearanceNumber('paragraphSpacing', $event)"
                /><code>{{ appearanceReadout('paragraphSpacing') }}</code>
              </div>
              <small>0–2 em，正文段落之间的额外留白。</small></label
            >
            <label
              >圆角风格<select v-model="form.radiusScale" data-setting="radiusScale">
                <option value="none">无圆角</option>
                <option value="sharp">锐利</option>
                <option value="default">默认</option>
                <option value="soft">柔和</option>
                <option value="round">圆润</option>
              </select></label
            >
            <label
              >阴影层次<select v-model="form.shadowDepth" data-setting="shadowDepth">
                <option value="flat">扁平 · 无阴影</option>
                <option value="default">默认</option>
                <option value="deep">加深 · 更强悬浮感</option>
              </select></label
            >
            <label
              >布局密度<select v-model="form.layoutDensity" data-setting="layoutDensity">
                <option value="compact">紧凑</option>
                <option value="default">默认</option>
                <option value="comfortable">宽松</option>
              </select></label
            >
            <label
              >首页头图<select v-model="form.homeHeroStyle" data-setting="homeHeroStyle">
                <option value="default">默认</option>
                <option value="compact">紧凑</option>
                <option value="minimal">极简</option>
                <option value="off">关闭</option>
              </select></label
            >
            <label
              >文章封面<select v-model="form.postCoverStyle" data-setting="postCoverStyle">
                <option value="default">默认</option>
                <option value="wide">通栏</option>
                <option value="hidden">隐藏</option>
              </select></label
            >
            <label
              >动效强度<select v-model="form.motionIntensity" data-setting="motionIntensity">
                <option value="off">关闭</option>
                <option value="subtle">轻微</option>
                <option value="default">默认</option>
                <option value="lively">活泼</option>
              </select>
              <small>关闭后前台不再播放入场与滚动动效。</small></label
            >
          </div>
        </section>
      </div>

      <!-- 内容与互动 -->
      <div v-show="activeTab === 'interaction'" class="tab-panels" data-tab-panel="interaction">
        <section class="settings-panel" data-panel="interaction">
          <div class="panel-heading">
            <div>
              <h2>互动开关</h2>
              <p>决定读者能不能留言，以及后台要不要记录匿名访客。</p>
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
              data-setting="commentsEnabled"
          /></label>
          <label class="toggle-row"
            ><span
              ><strong>匿名访客统计</strong
              ><small>记录页面、时间和不可逆访客摘要，不保存原始 IP。</small></span
            ><input
              v-model="form.visitorTrackingEnabled"
              type="checkbox"
              true-value="true"
              false-value="false"
              data-setting="visitorTrackingEnabled"
          /></label>
        </section>

        <section class="settings-panel" data-panel="music">
          <div class="panel-heading">
            <div>
              <h2>背景音乐</h2>
              <p>为阅读页加一段背景声音。</p>
            </div>
          </div>
          <label class="toggle-row"
            ><span
              ><strong>启用背景音乐</strong
              ><small>浏览器通常会阻止有声音的自动播放；访客可手动点击播放。</small></span
            ><input
              v-model="form.musicEnabled"
              type="checkbox"
              true-value="true"
              false-value="false"
              data-setting="musicEnabled"
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

        <section class="settings-panel settings-panel--wide" data-panel="moderation">
          <div class="panel-heading">
            <div>
              <h2>留言审核</h2>
              <p>用模型帮访客留言做一次初筛。</p>
            </div>
          </div>
          <label class="toggle-row"
            ><span
              ><strong>访客评论自动审核</strong
              ><small
                >访客提交留言后异步调用模型审核，结论不是「通过」就落为待审状态；失败静默忽略，绝不影响留言提交。关闭时行为与现在一致。</small
              ></span
            ><input
              v-model="form.llmCommentModeration"
              type="checkbox"
              true-value="true"
              false-value="false"
              data-setting="llmCommentModeration"
          /></label>
          <p class="panel-hint">
            模型接口地址与密钥已移到
            <RouterLink :to="`${siteStore.adminRoot}/seo`">SEO 管理</RouterLink>
            页顶部，生成能力与连接配置现在在同一屏。
          </p>
        </section>
      </div>

      <!-- 系统与安全 -->
      <div v-show="activeTab === 'system'" class="tab-panels" data-tab-panel="system">
        <section class="settings-panel" data-panel="admin">
          <div class="panel-heading">
            <div>
              <h2>后台入口</h2>
              <p>后台路径可自定义；管理操作仍由管理员登录保护。</p>
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
          <div class="security-row">
            <span
              ><strong>登录密码</strong
              ><small>修改成功后会退出登录，请用新密码重新进入后台。</small></span
            >
            <RouterLink to="/password-change" class="btn btn-ghost btn-sm">修改密码</RouterLink>
          </div>
        </section>

        <section class="settings-panel" data-panel="visitors">
          <div class="panel-heading">
            <div>
              <h2>访客统计</h2>
              <p>匿名、不可逆，只用来了解文章有没有人看。</p>
            </div>
          </div>
          <p class="panel-hint">
            访客统计记录访问的页面、时间与一个不可逆的访客摘要，**不保存原始 IP**、不保存
            身份信息，也没有任何第三方追踪。开关在「内容与互动」分组里，原始记录可在
            <RouterLink :to="`${siteStore.adminRoot}/visitors`">访客记录</RouterLink>
            里查看。
          </p>
        </section>

        <section class="settings-panel settings-panel--wide" data-panel="llm-entry">
          <div class="panel-heading">
            <div>
              <h2>模型接口</h2>
              <p>AI 辅助用的 OpenAI 兼容接口，已经独立成一页。</p>
            </div>
          </div>
          <p class="panel-hint">
            API 地址、密钥、模型与思考参数都在
            <RouterLink :to="`${siteStore.adminRoot}/seo`">SEO 管理</RouterLink>
            页顶部的「AI 辅助与
            SEO」面板里配置，测连通性和批量生成也在同一页完成；这里不再重复维护一份。
          </p>
        </section>
      </div>

      <div class="form-footer">
        <p>四个分组的改动会一起保存；更改后台路径后将自动跳转到新地址。</p>
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
import {
  THEME_PRESETS,
  applyThemePreset,
  isThemePresetActive,
  type ThemePreset
} from '@/constants/themePresets'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'

const siteStore = useSiteStore()

/**
 * 站点设置表单：在站点设置键之外带上「玻璃感」开关。
 *
 * <p>`glassMode` 由后端与 store 登记；这里用本地加宽的类型承接，
 * 免得表单要等 store 改动才能写。</p>
 */
type SettingsForm = SiteSettings & { glassMode?: string }

const form = reactive<SettingsForm>({ ...siteStore.settings, glassMode: 'on' })
const saving = ref(false)
const saved = ref(false)
const error = ref('')

/**
 * 设置页的四个分组。
 *
 * <p>分组只影响**怎么呈现**：所有输入绑的都是同一个响应式表单对象，
 * 切组不会丢任何已填的内容，保存也只有一条路径。</p>
 */
const TABS = [
  { id: 'profile', label: '站点资料', hint: '名称、简介与品牌素材' },
  { id: 'appearance', label: '外观', hint: '主题、背景与玻璃感' },
  { id: 'interaction', label: '内容与互动', hint: '评论、音乐与留言审核' },
  { id: 'system', label: '系统与安全', hint: '后台入口与模型接口' }
] as const

type TabId = (typeof TABS)[number]['id']

const activeTab = ref<TabId>('profile')

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

/**
 * 玻璃感开关：`off` 时卡片是完全不透明的实色，不透明度与模糊两个控件随之失效。
 *
 * <p>未识别的值一律按开启处理：玻璃感是新版默认形态，宁可多一层透明，
 * 也不要因为一个脏值把卡片变成刺眼的实色块。</p>
 */
const glassEnabled = computed(() => form.glassMode !== 'off')

/** 玻璃感开启时不透明度封顶 92（后端会把 100 夹到 92）；关闭时允许 100 的实色。 */
const surfaceOpacity = computed(() =>
  glassEnabled.value
    ? clampInt(form.surfaceOpacity, 60, 92, 88)
    : clampInt(form.surfaceOpacity, 60, 100, 100)
)

const surfaceBlur = computed(() => clampInt(form.surfaceBlur, 0, 40, 0))

/**
 * 点一下预设 = 一次写入 10 个键（皮肤、强调色、字体、圆角、阴影、画布渐变与玻璃感强度）。
 *
 * <p>预设是「换一套底子」而不是「重置全部」：作者在单项控件上做过的其它调整不动。</p>
 */
function applyPreset(preset: ThemePreset): void {
  // 站点设置是接口声明、没有索引签名，预设写入器按键值表工作，这里做一次显式断言。
  applyThemePreset(form as unknown as Record<string, string>, preset)
}

/** 当前设置是否与某套预设一致（用于在预设卡上标出「使用中」）。 */
function isPresetActive(preset: ThemePreset): boolean {
  return isThemePresetActive(form as unknown as Record<string, string | undefined>, preset)
}

/** 外观深度自定义里四个数值型设置键；其余七个是枚举字符串。 */
type AppearanceNumberKey = 'baseFontSize' | 'bodyLineHeight' | 'readerWidth' | 'paragraphSpacing'

/** 数值键的默认值与读数单位；默认值必须与 `stores/site.ts` 的 defaults 一致。 */
const APPEARANCE_NUMBER_META: Record<AppearanceNumberKey, { fallback: number; unit: string }> = {
  baseFontSize: { fallback: 16, unit: ' px' },
  bodyLineHeight: { fallback: 1.75, unit: '' },
  readerWidth: { fallback: 760, unit: ' px' },
  paragraphSpacing: { fallback: 1, unit: ' em' }
}

/**
 * 「恢复默认外观」要写回的 11 个键。
 *
 * <p>只重置本地表单、**不自动保存**：作者仍要点底部保存才落库，避免一次误点
 * 就把线上外观改掉。取值与 `stores/site.ts` 的 defaults / 后端 defaults() 一致。</p>
 */
const APPEARANCE_DEFAULTS = {
  fontFamily: 'system',
  baseFontSize: '16',
  bodyLineHeight: '1.75',
  readerWidth: '760',
  paragraphSpacing: '1',
  radiusScale: 'default',
  shadowDepth: 'default',
  layoutDensity: 'default',
  homeHeroStyle: 'default',
  postCoverStyle: 'default',
  motionIntensity: 'default'
} as const

/**
 * 读数值型设置。
 *
 * <p>契约里字段全是字符串，range 的 `v-model` 直接绑会与 `SiteSettings` 的类型冲突，
 * 所以这里只把「读」做一次安全解析（脏数据不渲染成 NaN），写回仍是字符串。</p>
 */
function appearanceNumber(key: AppearanceNumberKey): number {
  const parsed = Number.parseFloat(form[key] ?? '')
  return Number.isFinite(parsed) ? parsed : APPEARANCE_NUMBER_META[key].fallback
}

/** 滑杆旁读数，例如「16 px」「1.75」「760 px」「1 em」。 */
function appearanceReadout(key: AppearanceNumberKey): string {
  return `${appearanceNumber(key)}${APPEARANCE_NUMBER_META[key].unit}`
}

/** range 的 input 事件：把浏览器给的字符串原样写回表单，保持后端契约的 string 类型。 */
function setAppearanceNumber(key: AppearanceNumberKey, event: Event): void {
  const target = event.target as HTMLInputElement | null
  if (target) form[key] = target.value
}

/** 把 11 个外观键重置为默认值；只改表单，不触发保存。 */
function resetAppearance(): void {
  Object.assign(form, APPEARANCE_DEFAULTS)
}

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

/** 玻璃感开关的取值归一：只认 'off'，其余一律按开启处理。 */
function normalizeGlassMode(value: string | undefined): 'on' | 'off' {
  return value === 'off' ? 'off' : 'on'
}

async function load() {
  try {
    const response = await siteApi.getSettings()
    if (response.data) {
      Object.assign(form, response.data)
      form.glassMode = normalizeGlassMode(form.glassMode)
    }
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
      form.glassMode = normalizeGlassMode(form.glassMode)
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

onMounted(() => {
  void load()
})
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
/* 分组导航：一排等宽卡片，选中项用强调色描边，和后台其它页面的分段控件一致。 */
.settings-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.settings-tab {
  display: grid;
  gap: 3px;
  padding: 11px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
  color: var(--text-secondary);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}
.settings-tab:hover {
  border-color: var(--border-strong);
}
.settings-tab.is-active {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px var(--accent);
  color: var(--text-primary);
}
.settings-tab-label {
  font-size: var(--text-sm);
  font-weight: 600;
}
.settings-tab-hint {
  color: var(--text-muted);
  font-size: var(--text-2xs);
  line-height: 1.5;
}
.settings-grid {
  display: grid;
  gap: 14px;
}
.tab-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
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
.settings-panel--wide {
  grid-column: 1 / -1;
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
.panel-heading-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px 18px;
  align-items: start;
}
.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 15px 18px;
  align-items: start;
}
.settings-panel > label:not(.toggle-row),
.field-grid > label:not(.toggle-row) {
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
input:disabled,
select:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
textarea {
  resize: vertical;
}
label small {
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.panel-hint {
  margin: 0;
  padding: 11px 13px;
  border-radius: var(--radius);
  background: var(--bg-secondary);
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.7;
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
/* 成品主题预设卡：一排小卡，每张卡三个色块 + 名称 + 一句话说明。 */
.preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
}
.preset-card {
  position: relative;
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}
.preset-card:hover {
  border-color: var(--border-strong);
  transform: translateY(-1px);
}
.preset-card.is-active {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px var(--accent);
  color: var(--text-primary);
}
.preset-swatches {
  display: flex;
  gap: 6px;
}
.preset-swatches i {
  width: 22px;
  height: 22px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
}
.preset-name {
  font-size: var(--text-sm);
}
.preset-desc {
  color: var(--text-muted);
  font-size: var(--text-2xs);
  line-height: 1.6;
}
.preset-flag {
  justify-self: start;
  padding: 1px 8px;
  border-radius: var(--radius-full);
  background: var(--accent);
  color: var(--accent-contrast, #fff);
  font-size: var(--text-2xs);
  font-weight: 600;
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
.range-row input[type='range']:disabled {
  cursor: not-allowed;
  opacity: 0.55;
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
/* 外观深度自定义：11 个控件走两列网格；range 读数复用 .range-row。 */
.appearance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px 18px;
  align-items: start;
}
.appearance-grid > label {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.appearance-grid > label small {
  color: var(--text-muted);
  font-size: var(--text-xs);
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
.skin-preview[data-skin='ink'] {
  --primary: #1f7a72;
  background: #e4ece8;
}
.skin-preview[data-skin='dusk'] {
  --primary: #9d8bf5;
  background: #ece9fb;
}
.skin-preview[data-skin='sand'] {
  --primary: #c2703f;
  background: #efe3d6;
}
@media (max-width: 900px) {
  .settings-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .tab-panels {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 760px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
  .bg-body {
    grid-template-columns: 1fr;
  }
  .appearance-grid {
    grid-template-columns: 1fr;
  }
  .panel-heading-row {
    flex-direction: column;
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
  .settings-tabs {
    grid-template-columns: 1fr;
  }
  .form-footer {
    align-items: start;
    flex-direction: column;
  }
}
</style>
