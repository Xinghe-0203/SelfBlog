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

      <section class="settings-panel llm-panel">
        <div class="panel-heading panel-heading-row">
          <div>
            <h2>AI 辅助与 SEO</h2>
            <p>
              接入任意 OpenAI
              兼容接口，用于自动生成文章标题、描述与关键词。密钥只保存在服务端，既不下发到前台，也不会写进日志。
            </p>
          </div>
          <button
            class="btn btn-ghost btn-sm"
            type="button"
            :disabled="testing"
            data-action="test-llm"
            @click="testConnection"
          >
            {{ testing ? '测试中…' : '测试连接' }}
          </button>
        </div>
        <label class="toggle-row"
          ><span
            ><strong>启用 AI 辅助</strong
            ><small>关闭后所有生成接口直接拒绝，已生成的 SEO 数据仍然生效。</small></span
          ><input
            v-model="form.llmEnabled"
            type="checkbox"
            true-value="true"
            false-value="false"
            data-setting="llmEnabled"
        /></label>
        <div class="llm-provider">
          <label
            >服务商<select v-model="form.llmProvider" data-setting="llmProvider">
              <option value="deepseek">DeepSeek 官方接口</option>
              <option value="custom">自定义 OpenAI 兼容接口</option>
            </select>
            <small
              >DeepSeek 才会发送思考模式与思考强度；自定义服务商按标准 OpenAI 兼容协议调用。</small
            ></label
          >
          <button
            class="btn btn-ghost btn-sm"
            type="button"
            data-action="llm-preset"
            @click="applyDeepSeekPreset"
          >
            填入 DeepSeek 默认
          </button>
        </div>
        <div class="llm-fields">
          <label
            >API 地址<input
              v-model.trim="form.llmBaseUrl"
              inputmode="url"
              maxlength="500"
              placeholder="https://api.openai.com/v1"
              data-setting="llmBaseUrl"
            /><small
              >OpenAI 兼容的根地址，客户端会自动补 `/chat/completions`；末尾斜杠会被去掉。</small
            ></label
          >
          <label
            >API Key<input
              v-model.trim="form.llmApiKey"
              type="password"
              maxlength="500"
              autocomplete="new-password"
              :placeholder="llmKeyConfigured ? '已配置' : '尚未配置'"
              data-setting="llmApiKey"
            /><small
              >已保存的密钥显示为 <code>{{ SECRET_MASK }}</code
              >：保持原样保存不会改动它，清空后保存即删除。<span
                class="llm-badge"
                :data-llm-key="llmKeyConfigured ? 'configured' : 'empty'"
                >{{ llmKeyConfigured ? '已配置' : '未配置' }}</span
              ></small
            ></label
          >
          <label v-if="modelOptions.length" class="llm-model-picker"
            >模型候选
            <div class="llm-model-row">
              <select v-model="form.llmModel" data-setting="llmModelSelect">
                <option v-for="model in modelOptions" :key="model.id" :value="model.id">
                  {{ model.name }}（{{ model.id }}）{{ model.supportsImage ? ' · 支持图像' : '' }}
                </option>
              </select>
              <button
                class="btn btn-ghost btn-sm"
                type="button"
                :disabled="loadingModels"
                data-action="refresh-models"
                @click="loadModels"
              >
                {{ loadingModels ? '探测中…' : '刷新' }}
              </button>
            </div>
            <small
              >来源：{{
                modelsSource === 'upstream' ? '上游实时列表' : '内置候选（上游暂不可用）'
              }}；也可在下方直接手填模型名。</small
            ></label
          >
          <label
            >模型名<input
              v-model.trim="form.llmModel"
              maxlength="100"
              placeholder="gpt-4o-mini"
              data-setting="llmModel"
            /><small>由服务商决定，例如 deepseek-flash、gpt-4o-mini、qwen-plus。</small></label
          >
          <label
            >单次调用超时（秒）<input
              v-model.trim="form.llmTimeoutSeconds"
              type="number"
              min="5"
              max="120"
              step="1"
              data-setting="llmTimeoutSeconds"
            /><small>5–120 秒。超时视为一次失败，不影响文章本身的保存。</small></label
          >
          <label
            >最大输出 token<input
              v-model.trim="form.llmMaxTokens"
              type="number"
              min="0"
              max="131072"
              step="1"
              data-setting="llmMaxTokens"
            /><small
              >0–131072。填 0 表示不限制（不向上游发送该参数），由服务商默认值决定。</small
            ></label
          >
          <label
            >思考强度<select
              v-model="form.llmReasoningEffort"
              :disabled="reasoningEffortDisabled"
              data-setting="llmReasoningEffort"
            >
              <option value="low">低 · 更快更省</option>
              <option value="high">高 · 更细致</option>
              <option value="max">最高 · 最慢最贵</option>
            </select>
            <small
              >仅在「DeepSeek + 思考模式开启」时生效；此时不再发送
              temperature，由模型自行推理。</small
            ></label
          >
        </div>
        <label class="toggle-row"
          ><span
            ><strong>思考模式</strong
            ><small
              >开启后模型先推理再作答，质量更高但更慢、更贵；仅 DeepSeek
              服务商支持。关闭时思考强度下拉不可用。</small
            ></span
          ><input
            v-model="form.llmThinking"
            type="checkbox"
            true-value="enabled"
            false-value="disabled"
            data-setting="llmThinking"
        /></label>
        <label class="toggle-row"
          ><span
            ><strong>发布后自动生成 SEO</strong
            ><small
              >文章发布后异步生成，失败只记日志、绝不阻塞保存。关闭时可到「SEO
              管理」手动批量生成。</small
            ></span
          ><input
            v-model="form.llmAutoOnPublish"
            type="checkbox"
            true-value="true"
            false-value="false"
            data-setting="llmAutoOnPublish"
        /></label>
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
        <p v-if="testResult" class="llm-test" :data-ok="testResult.ok ? 'true' : 'false'">
          {{ testResult.message
          }}<template v-if="testResult.ok">
            （{{ testResult.model }}，{{ testResult.latencyMs }}ms）</template
          >
        </p>
        <p v-else class="llm-hint">
          测试连接使用<strong>已保存</strong>的配置；改了地址或密钥请先保存再测试。
        </p>
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
import { seoApi } from '@/api/seo'
import type { SeoLlmTest } from '@/types/seo'
import type { LlmModel } from '@/types/llm'
import SiteAssetField from '@/components/admin/SiteAssetField.vue'

const siteStore = useSiteStore()
const form = reactive<SiteSettings>({ ...siteStore.settings })
const saving = ref(false)
const saved = ref(false)
const error = ref('')

/**
 * 服务端用来表示「密钥已保存」的哨兵串（8 个 U+2022）。
 *
 * 它是契约的一部分，不是装饰：`PUT /api/admin/site-settings` 收到这个字面值
 * 就保留已存密钥，收到空串则删除。前端不能自己造别的占位符，否则一次保存
 * 就会把作者的密钥悄悄清掉。
 */
const SECRET_MASK = '••••••••'

const testing = ref(false)
const testResult = ref<SeoLlmTest | null>(null)

/** 密钥是否已配置：由哨兵串派生，避免把掩码当真实值展示或比较。 */
const llmKeyConfigured = computed(() => (form.llmApiKey ?? '') === SECRET_MASK)

/** DeepSeek 官方接口的默认值：一键预设写这两项，其余（密钥/超时）保持作者原样。 */
const DEEPSEEK_BASE_URL = 'https://api.deepseek.com'
const DEEPSEEK_MODEL = 'deepseek-flash'

/** 模型候选列表（`GET /admin/seo/models`）。后端恒 200，上游不可用时回落内置候选。 */
const modelOptions = ref<LlmModel[]>([])
const modelsSource = ref<'upstream' | 'fallback' | ''>('')
const loadingModels = ref(false)

/**
 * 思考强度只在「DeepSeek + 思考模式开启」时才会真正发给上游。
 *
 * <p>下拉在其余情况下 disabled 而不是隐藏：作者要能看见这个能力存在、并且知道
 * 为什么现在不生效（提示文案就在字段下方）。</p>
 */
const reasoningEffortDisabled = computed(
  () => form.llmThinking !== 'enabled' || form.llmProvider !== 'deepseek'
)

/** 填入 DeepSeek 默认值：只改地址与模型，不动已保存的密钥与其它设置。 */
function applyDeepSeekPreset(): void {
  form.llmProvider = 'deepseek'
  form.llmBaseUrl = DEEPSEEK_BASE_URL
  form.llmModel = DEEPSEEK_MODEL
}

/**
 * 探测上游模型候选。
 *
 * <p>失败一律**静默**回落为「没有候选下拉」，作者仍可手填模型名 —— 探测只是便利，
 * 不能因为它连不上就把整个设置页标成错误。</p>
 */
async function loadModels(): Promise<void> {
  loadingModels.value = true
  try {
    const response = await seoApi.listModels()
    const payload = response.data
    modelOptions.value = payload?.models ?? []
    modelsSource.value = payload?.source ?? 'fallback'
  } catch {
    modelOptions.value = []
    modelsSource.value = ''
  } finally {
    loadingModels.value = false
  }
}

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

/**
 * 用**已保存**的配置测一次连通性。
 *
 * 刻意不把表单里的未保存值发过去：否则会出现「测试通过但保存失败」的错位，
 * 而且把未保存的密钥塞进额外请求会让它多经一次网络。后端对上游失败一律
 * 返回 200 + `ok:false`，所以这里只需要一个 try/catch 兜住本地网络错误。
 */
async function testConnection() {
  testing.value = true
  testResult.value = null
  try {
    const response = await seoApi.testConnection()
    testResult.value = response.data
  } catch (err) {
    testResult.value = {
      ok: false,
      message:
        (err as Error & { userMessage?: string }).userMessage || '测试请求失败，请稍后重试。',
      latencyMs: 0,
      model: ''
    }
  } finally {
    testing.value = false
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

onMounted(() => {
  void load()
  // 模型候选是纯便利信息：与设置加载并行探测，失败静默。
  void loadModels()
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
/* AI 面板：两个开关行之间夹一组两列字段，面板本身跨两列避免密钥输入被挤成半宽。 */
.llm-panel {
  grid-column: 1 / -1;
}
.panel-heading-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}
.llm-provider {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
}
.llm-provider label {
  display: grid;
  flex: 1 1 auto;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.llm-provider label small {
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.llm-model-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.llm-model-row select {
  flex: 1 1 auto;
  min-width: 0;
}
.llm-model-row .btn {
  flex: 0 0 auto;
}
.llm-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.llm-fields label {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.llm-fields label small {
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.llm-fields code {
  padding: 1px 4px;
  border-radius: var(--radius-xs);
  background: var(--bg-tertiary);
  font-size: var(--text-2xs);
  letter-spacing: 0.08em;
}
.llm-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  font-size: var(--text-2xs);
  font-weight: 600;
}
.llm-badge[data-llm-key='configured'] {
  background: var(--success-light);
  color: var(--success);
}
.llm-badge[data-llm-key='empty'] {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}
.llm-test,
.llm-hint {
  margin: 0;
  padding: 11px 13px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
  line-height: 1.6;
}
.llm-test[data-ok='true'] {
  background: var(--success-light);
  color: var(--success);
}
.llm-test[data-ok='false'] {
  background: var(--error-light);
  color: var(--error);
}
.llm-hint {
  background: var(--bg-secondary);
  color: var(--text-muted);
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
  .llm-fields {
    grid-template-columns: 1fr;
  }
  .llm-provider {
    align-items: start;
    flex-direction: column;
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
  .form-footer {
    align-items: start;
    flex-direction: column;
  }
}
</style>
