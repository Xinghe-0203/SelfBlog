import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { STORAGE_KEY_PREFIX } from '@/constants'

export interface SiteSettings {
  siteTitle: string
  siteDescription: string
  authorName: string
  authorBio: string
  authorAvatar: string
  adminPath: string
  commentsEnabled: string
  visitorTrackingEnabled: string
  musicEnabled: string
  musicUrl: string
  musicTitle: string
  themeSkin: string
  themePrimaryColor: string
  footerText: string
  /** 浏览器标签页图标，由后台上传后下发；为空时回落到 index.html 的静态 /favicon.svg。 */
  faviconUrl: string
  /** 顶栏与后台侧边栏的品牌图标；为空时回落内置 Icon name="logo"。 */
  siteLogoUrl: string
  /** 站点正式地址（origin）；为空时服务端回落到 SITE_BASE_URL 或当前访问地址。 */
  siteBaseUrl: string
  /** 页面背景取色模式：solid | gradient | image。 */
  backgroundType: string
  /** 纯色模式的页面底色；等于默认值时视为「未设置」，让主题皮肤自己决定底色。 */
  backgroundColor: string
  /** 渐变模式的起始色。 */
  backgroundGradientFrom: string
  /** 渐变模式的结束色。 */
  backgroundGradientTo: string
  /** 背景图地址（http(s) 或本站 /uploads/ 路径）。 */
  backgroundImageUrl: string
  /** 背景图上的黑色遮罩百分比 0–90，用来保证前景文字可读。 */
  backgroundOverlay: string
  /** 背景图是否固定（不随滚动移动）。 */
  backgroundFixed: string
  /** 卡片等组件的不透明度百分比 60–100；100 = 完全不透明，保持既有观感。 */
  surfaceOpacity: string
  /** 卡片的背景模糊半径 0–40（px），只在组件半透明时才有意义。 */
  surfaceBlur: string
  /** 是否启用后台 LLM 辅助（SEO 生成等）；关闭时相关接口返回 400。 */
  llmEnabled: string
  /** OpenAI 兼容的 API 根地址（如 https://api.openai.com/v1）；服务端保存时去尾斜杠。 */
  llmBaseUrl: string
  /** API Key。服务端下发时恒为掩码哨兵 SECRET_MASK（8 个 U+2022），提交掩码=保留已存密钥，提交空串=清除。 */
  llmApiKey: string
  /** 模型名，如 gpt-4o-mini。 */
  llmModel: string
  /** 单次上游调用超时秒数 5–120。 */
  llmTimeoutSeconds: string
  /** 发布文章后是否自动生成 SEO 元数据。 */
  llmAutoOnPublish: string
}

const defaults: SiteSettings = {
  siteTitle: '个人博客',
  siteDescription: '记录技术、生活与一路上的新发现。',
  authorName: '站长',
  authorBio: '欢迎来到我的个人博客。',
  authorAvatar: '',
  adminPath: 'admin',
  commentsEnabled: 'true',
  visitorTrackingEnabled: 'true',
  musicEnabled: 'false',
  musicUrl: '',
  musicTitle: '背景音乐',
  themeSkin: 'ocean',
  themePrimaryColor: '#165DFF',
  footerText: '用文字记录生活，用好奇探索世界。',
  faviconUrl: '',
  siteLogoUrl: '',
  siteBaseUrl: '',
  backgroundType: 'solid',
  backgroundColor: '#f5f6f8',
  backgroundGradientFrom: '#f5f6f8',
  backgroundGradientTo: '#e7eaf0',
  backgroundImageUrl: '',
  backgroundOverlay: '0',
  backgroundFixed: 'true',
  surfaceOpacity: '100',
  surfaceBlur: '0',
  llmEnabled: 'false',
  llmBaseUrl: '',
  llmApiKey: '',
  llmModel: '',
  llmTimeoutSeconds: '30',
  llmAutoOnPublish: 'false'
}

/** 运行时注入的 favicon 节点标记，用来和 index.html 里的静态兜底区分。 */
const FAVICON_DATA_ATTR = 'runtime'

/**
 * 预置层（index.html 首屏内联脚本）允许转发的 CSS 变量白名单。
 *
 * 首屏预置脚本是**零逻辑转发**：它只按这份白名单读取 localStorage 并写到
 * `document.documentElement`，因此这里既是持久化格式，也是安全边界 ——
 * 白名单外的键（例如 `--bg-primary`）永远不会被首屏脚本写进样式。
 */
export const PREPAINT_APPEARANCE_VARS = [
  '--ap-bg-color',
  '--ap-bg-image',
  '--ap-bg-attachment',
  '--ap-bg-size',
  '--ap-bg-repeat'
] as const

/** 首屏预置读取的存储键；与 index.html 内联脚本里的字面量必须一致。 */
export const APPEARANCE_STORAGE_KEY = `${STORAGE_KEY_PREFIX}site_appearance`

/**
 * 各模式的默认底色 —— 刻意与 ocean 皮肤的 `--bg-primary` / `--bg-tertiary` 相同。
 *
 * 默认值等于默认皮肤的画布色，意味着「作者没动过这个设置」时我们可以**不写任何变量**，
 * 让 `--bg-primary` 继续由皮肤或深色模式决定。否则换皮肤会被作者从未设过的
 * 默认色覆盖掉。
 */
const DEFAULT_BACKGROUND_COLOR = '#f5f6f8'
const DEFAULT_GRADIENT_TO = '#e7eaf0'

const HEX_COLOR = /^#[0-9a-f]{6}$/i

/** 取整并夹到 [min, max]；非法输入直接回落到 fallback。 */
function clampInt(raw: string | undefined, min: number, max: number, fallback: number): number {
  const parsed = Number.parseInt(String(raw ?? '').trim(), 10)
  if (!Number.isFinite(parsed)) return fallback
  return Math.min(max, Math.max(min, parsed))
}

/** 仅接受 `#RRGGBB`；其余（含服务端被绕过的脏数据）一律丢弃，避免拼进 CSS。 */
function normalizeHex(raw: string | undefined): string {
  const value = String(raw ?? '').trim()
  return HEX_COLOR.test(value) ? value : ''
}

/**
 * 背景图地址的二次校验。
 *
 * 服务端已经拒绝空白、引号、圆括号与反斜杠，但这里再挡一次：该值会被拼进
 * `url("…")`，任何能闭合引号或括号的字符都可能撑开一条 CSS 注入路径。
 * 前端不能假设数据一定来自本服务端的校验。
 */
function safeBackgroundUrl(raw: string | undefined): string {
  const value = String(raw ?? '').trim()
  if (!value) return ''
  if (!/^https?:\/\//i.test(value) && !value.startsWith('/uploads/')) return ''
  return /[\s"'()\\]/.test(value) ? '' : value
}

/**
 * 是否处于「深色画布」。
 *
 * 除了显式的 .dark-mode（系统深色 / 站内深色），**midnight 皮肤本身就是深色画布**：
 * 它把 --text-primary 换成了浅色，此时若再套用作者的浅色背景，就会变成浅底浅字。
 * 所以判断依据必须是「这块画布有多深」，而不只是那个 class。
 */
export function isDarkCanvas(skin: string): boolean {
  if (typeof document === 'undefined') return false
  if (document.documentElement.classList.contains('dark-mode')) return true
  return skin === 'midnight'
}

/**
 * 深色画布下背景图的最小遮罩百分比。
 *
 * 深色模式的文字是浅色，压在一张明亮照片上必然读不清；无论作者把遮罩调到多低，
 * 深色画布下都至少压这么厚，保证正文可读性优先于背景可见度。
 */
const DARK_CANVAS_MIN_OVERLAY = 45

/**
 * 有壁纸（照片 / 作者渐变）时「浮起表面」的不透明度下限。
 *
 * 背景色相不可控 ⇒ 表面合成色也不可控，于是用 alpha 下限把正文对比度兜住。
 * 以浅色画布 + `--surface-primary: #ffffff` 为例，合成色就是白纸压在最坏画布上：
 *
 *   WCAG 相对亮度：L(#262a31) ≈ 0.023，L(#67707e) ≈ 0.160，L(#707a88) ≈ 0.191
 *   正文（text-primary）要 4.5:1 ⇒ 纸面亮度 L ≥ 4.5 × (0.023 + 0.05) − 0.05 ≈ 0.278
 *   次要文字（text-secondary）    ⇒ L ≥ 4.5 × (0.160 + 0.05) − 0.05 ≈ 0.895
 *   弱化文字（text-muted）        ⇒ L ≥ 4.5 × (0.191 + 0.05) − 0.05 ≈ 1.036  ← 超过纯白（1.0）
 *
 * 最后一行是结论性的：**任何**半透明层都兜不住弱化文字，纯白底本身也只有 4.35:1。
 * 所以修复不能只靠调 alpha，必须结构性解决 —— 纸面档（page paper）在壁纸模式下
 * 直接取 100%（完全不透明，见下方 effectiveSurfaceOpacities），半透明只花在
 * 「叠在纸面上」的 raised / control 两档；它们的背景是纸面而不是照片，
 * 对比度天然安全。88 / 94 这两个下限是给 raised / control 用的经验值：
 * 浅色画布 88% 时合成色仍接近纸面（色调台阶可见、可读），深色画布取 94%，
 * 因为浅字压在半透明深色面上更容易掉到 2:1（见 m00001 的深色实测）。
 */
const WALLPAPER_RAISED_FLOOR_LIGHT = 88
const WALLPAPER_RAISED_FLOOR_DARK = 94
/** control 档比 raised 再透明一档，形成「纸面 → 卡片 → 控件」的可见台阶。 */
const SURFACE_TIER_STEP = 6

/**
 * 计算三级表面的**实际生效**不透明度。
 *
 * 存储契约（后端允许 60–100）保持不变：这里只夹紧「应用值」，作者的设置原样留在库里，
 * 换掉壁纸后立刻恢复他的选择。三档关系：
 *   - 无壁纸：三档同值 = 作者设置（不透明度 < 100 时仍是原来的单档半透明观感，零回归）；
 *   - 有壁纸：paper = 100（正文必须压在实心纸面上），raised = max(设置值, 88/94)，
 *     control = raised − 6（不低于 60，且不高于 raised）。
 *
 * @param rawOpacity 作者设置的 surfaceOpacity（字符串）
 * @param dark 是否深色画布
 * @param wallpaper 是否配置了壁纸/渐变（见 applyAppearance 的 data-wallpaper 判定）
 */
export function effectiveSurfaceOpacities(
  rawOpacity: string | undefined,
  dark: boolean,
  wallpaper: boolean
): { paper: number; raised: number; control: number } {
  const base = clampInt(rawOpacity, 60, 100, 100)
  if (!wallpaper) return { paper: base, raised: base, control: base }
  const raised = Math.max(base, dark ? WALLPAPER_RAISED_FLOOR_DARK : WALLPAPER_RAISED_FLOOR_LIGHT)
  const control = Math.min(raised, Math.max(60, raised - SURFACE_TIER_STEP))
  return { paper: 100, raised, control }
}

/**
 * 计算页面背景的 CSS 变量。
 *
 * 只产出当前模式真正需要的键，且**默认配置下不产出任何键** ——
 * 未被作者触碰过的背景继续跟随皮肤与深色模式，升级后观感逐像素不变。
 *
 * @param dark 是否深色画布（见 isDarkCanvas）。深色下忽略作者的纯色与渐变：
 *   两者都是按浅色画布挑选的，浅底配浅字会直接不可读。背景图保留，但强制加厚遮罩。
 */
export function buildBackgroundVars(
  settings: Partial<SiteSettings>,
  dark = false
): Record<string, string> {
  const type = String(settings.backgroundType ?? '').trim() || 'solid'
  const color = normalizeHex(settings.backgroundColor)
  const vars: Record<string, string> = {}

  if (type === 'image') {
    const image = safeBackgroundUrl(settings.backgroundImageUrl)
    const overlay = clampInt(settings.backgroundOverlay, 0, 90, 0)
    if (image) {
      const alpha = (dark ? Math.max(overlay, DARK_CANVAS_MIN_OVERLAY) : overlay) / 100
      // 多图层写法：第一层在上，遮罩压住背景图，底色仍由 background-color 兜底。
      const scrim =
        alpha > 0 ? `linear-gradient(rgba(0, 0, 0, ${alpha}), rgba(0, 0, 0, ${alpha})), ` : ''
      vars['--ap-bg-image'] = `${scrim}url("${image}")`
    }
    vars['--ap-bg-attachment'] = settings.backgroundFixed === 'false' ? 'scroll' : 'fixed'
    vars['--ap-bg-size'] = 'cover'
    vars['--ap-bg-repeat'] = 'no-repeat'
  } else if (type === 'gradient' && !dark) {
    const from = normalizeHex(settings.backgroundGradientFrom) || DEFAULT_BACKGROUND_COLOR
    const to = normalizeHex(settings.backgroundGradientTo) || DEFAULT_GRADIENT_TO
    vars['--ap-bg-image'] = `linear-gradient(180deg, ${from}, ${to})`
  }

  // 深色画布下不写纯色，让 --bg-primary 继续由深色主题决定。
  vars['--ap-bg-color'] =
    !dark && color && color.toLowerCase() !== DEFAULT_BACKGROUND_COLOR ? color : ''
  return vars
}

/**
 * 把背景变量持久化给首屏预置脚本。
 *
 * 只写白名单内的键；没有内容时整条删掉，避免 localStorage 留下空壳 ——
 * 空串在预置脚本里等于「未设置」，此时 CSS 会走自己的回落链。
 */
export function persistAppearance(vars: Record<string, string>): void {
  try {
    const payload: Record<string, string> = {}
    for (const key of PREPAINT_APPEARANCE_VARS) {
      const value = vars[key]
      if (value) payload[key] = value
    }
    if (Object.keys(payload).length === 0) {
      localStorage.removeItem(APPEARANCE_STORAGE_KEY)
    } else {
      localStorage.setItem(APPEARANCE_STORAGE_KEY, JSON.stringify(payload))
    }
  } catch {
    // 隐私模式等场景 localStorage 不可用；预置只是消闪烁的优化，失败不影响渲染。
  }
}

/** 把背景变量写到 `:root`，让首个 `<body>` 绘制就能用上（早于 #selfblog 挂载）。 */
function applyBackgroundVars(vars: Record<string, string>): void {
  const html = document.documentElement
  for (const key of PREPAINT_APPEARANCE_VARS) {
    const value = vars[key]
    if (value) html.style.setProperty(key, value)
    else html.style.removeProperty(key)
  }
}

/**
 * 由文件扩展名推导 favicon 的 MIME。
 * 浏览器只认 svg/ico/png 这几类；其余（webp 等）按 png 处理，
 * 猜错只是不显示，不会更糟。
 */
export function mimeForIcon(url: string): string {
  // 用 replace 剥掉 query/hash，而不是 split(...)[0]：tsconfig 开了
  // noUncheckedIndexedAccess，索引取值会退化成 string | undefined。
  const path = url.replace(/[?#].*$/, '').toLowerCase()
  if (path.endsWith('.svg')) return 'image/svg+xml'
  if (path.endsWith('.ico')) return 'image/x-icon'
  return 'image/png'
}

/**
 * 把 faviconUrl 同步到 <head>。
 *
 * 幂等：每次先清掉上一次运行时注入的节点，再按需注入。
 * url 为空时只清理、不注入 —— index.html 里的静态 /favicon.svg 从未被移除，
 * 因此"清空设置"会自然回落到默认图标。
 */
export function applyFavicon(url: string): void {
  document.head
    .querySelectorAll<HTMLLinkElement>(`link[rel][data-${FAVICON_DATA_ATTR}]`)
    .forEach((link) => link.remove())

  if (!url) return

  const link = document.createElement('link')
  link.rel = 'icon'
  link.type = mimeForIcon(url)
  link.href = url
  link.dataset[FAVICON_DATA_ATTR] = 'favicon'
  document.head.appendChild(link)
}

export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings>({ ...defaults })
  const adminPath = computed(() => settings.value.adminPath || 'admin')
  const adminRoot = computed(() => `/${adminPath.value}`)
  const isCommentsEnabled = computed(() => settings.value.commentsEnabled === 'true')
  const isMusicEnabled = computed(
    () => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl
  )

  function applyAppearance(): void {
    const skin = settings.value.themeSkin || 'ocean'
    const dark = isDarkCanvas(skin)
    // 深色画布要重算：切到深色时必须把作者的浅色背景撤掉，否则浅底浅字不可读。
    const vars = buildBackgroundVars(settings.value, dark)
    // 背景变量写 :root：早于 #selfblog 挂载也要生效（loadPublicSettings 在 mount 之前跑），
    // 且 #selfblog 会自然继承，无需二次写入。
    applyBackgroundVars(vars)
    persistAppearance(vars)

    // 画布是不是「图」——判定与 index.html 首屏预置脚本完全一致：
    // 只要最终会写入 --ap-bg-image（照片或作者渐变）就算。必须写在 #selfblog 的
    // 早退之前：预置脚本写的是 <html>，这里晚一拍补齐，两边同一个属性、同一个元素。
    const wallpaper = !!vars['--ap-bg-image']
    if (wallpaper) document.documentElement.dataset.wallpaper = ''
    else delete document.documentElement.dataset.wallpaper

    const root = document.getElementById('selfblog')
    if (!root) return

    root.dataset.skin = skin
    root.style.setProperty('--site-primary', settings.value.themePrimaryColor || '#165DFF')
    root.style.setProperty(
      '--accent',
      'color-mix(in srgb, var(--site-primary) var(--accent-strength, 100%), var(--accent-blend, white))'
    )
    root.style.setProperty('--primary', 'var(--accent)')
    root.style.setProperty(
      '--accent-light',
      'color-mix(in srgb, var(--accent) 10%, var(--surface-primary))'
    )
    root.style.setProperty(
      '--accent-dark',
      'color-mix(in srgb, var(--accent) 82%, var(--accent-shade, black))'
    )

    // 组件不透明度 / 模糊：默认值下不写变量也不加属性，
    // 保证「作者没配过」时走的是原来那套完全不透明的实心表面。
    // 三级表面的实际生效值在 effectiveSurfaceOpacities 里夹紧（含壁纸对比度下限），
    // 作者的原始设置不动 —— 撤掉壁纸立刻回到他自己的选择。
    const blur = clampInt(settings.value.surfaceBlur, 0, 40, 0)
    const tiers = effectiveSurfaceOpacities(settings.value.surfaceOpacity, dark, wallpaper)
    if (tiers.paper < 100) root.style.setProperty('--ap-surface-opacity', `${tiers.paper}%`)
    else root.style.removeProperty('--ap-surface-opacity')
    root.style.setProperty('--ap-surface-paper-opacity', `${tiers.paper}%`)
    root.style.setProperty('--ap-surface-raised-opacity', `${tiers.raised}%`)
    root.style.setProperty('--ap-surface-control-opacity', `${tiers.control}%`)
    if (blur > 0) root.style.setProperty('--ap-surface-blur', `${blur}px`)
    else root.style.removeProperty('--ap-surface-blur')

    // 用属性做开关，而不是让 backdrop-filter 常驻 blur(0px)：
    // 后者会给每张卡片都建合成层，白白付出层叠上下文的代价。
    // 壁纸模式下即使作者设了 100% 也要开：paper 是实心的，但 raised/control 两档
    // 仍是半透明（tiers.raised < 100 时才真的半透明）。
    if (tiers.raised < 100) root.dataset.surfaceTranslucent = ''
    else delete root.dataset.surfaceTranslucent
    if (blur > 0) root.dataset.surfaceBlur = ''
    else delete root.dataset.surfaceBlur
  }

  function applySettings(input: Partial<SiteSettings> | Record<string, string>): void {
    settings.value = { ...settings.value, ...input }
    applyAppearance()
    document.title = settings.value.siteTitle || defaults.siteTitle
    applyFavicon(settings.value.faviconUrl || '')
  }

  async function loadPublicSettings(): Promise<void> {
    try {
      const response = await fetch('/api/site/public', { headers: { Accept: 'application/json' } })
      if (!response.ok) return
      const envelope = (await response.json()) as { data?: Partial<SiteSettings> }
      if (envelope.data) applySettings(envelope.data)
    } catch {
      // Keep the local defaults available when the API is temporarily offline.
    }
  }

  return {
    settings,
    adminPath,
    adminRoot,
    isCommentsEnabled,
    isMusicEnabled,
    applySettings,
    applyAppearance,
    loadPublicSettings
  }
})
