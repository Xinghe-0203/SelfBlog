/**
 * 成品主题预设（v2.0.30）
 *
 * 一个预设 = 一整套**已经搭配好的**视觉配方：配色（皮肤 + 强调色）、
 * 字体族、圆角风格、阴影层次、画布渐变、卡片玻璃感强度。
 *
 * 为什么要打包而不是让作者逐项调：
 * - 逐项调是「设置」，作者要懂 11 个键的含义才能调出好看的结果；
 * - 预设是「成品」，一点即换，切换后仍然可以继续微调单项。
 *
 * 预设只写**已有的**站点设置键，不引入新键 ⇒ 后端零改动、零迁移，
 * 旧站点也能直接用。
 */

/** 预设可用的皮肤 id；必须与 styles/skins.css 里实际存在的皮肤一致。 */
export type ThemePresetSkin =
  | 'ocean'
  | 'forest'
  | 'lavender'
  | 'sunset'
  | 'midnight'
  | 'ink'
  | 'dusk'
  | 'sand'

export interface ThemePreset {
  id: string
  name: string
  /** 一句话说明这套配方的性格，用于预设卡上的副标题。 */
  description: string
  skin: ThemePresetSkin
  /** 强调色 #RRGGBB。 */
  primaryColor: string
  fontFamily: 'system' | 'sans' | 'serif' | 'mono'
  radiusScale: 'none' | 'sharp' | 'default' | 'soft' | 'round'
  shadowDepth: 'flat' | 'default' | 'deep'
  background: {
    type: 'gradient'
    from: string
    to: string
  }
  /** 卡片不透明度 60–100（玻璃感开启时按 92 封顶）。 */
  surfaceOpacity: number
  /** 卡片背景模糊 0–40（px）。 */
  surfaceBlur: number
}

export const THEME_PRESETS: ThemePreset[] = [
  {
    id: 'paper',
    name: '纸间',
    description: '清透白纸配蓝调强调色，默认的干净底子。',
    skin: 'ocean',
    primaryColor: '#165DFF',
    fontFamily: 'system',
    radiusScale: 'default',
    shadowDepth: 'default',
    background: { type: 'gradient', from: '#f4f7fc', to: '#e9eef7' },
    surfaceOpacity: 88,
    surfaceBlur: 12
  },
  {
    id: 'ink',
    name: '墨青',
    description: '宋体标题配青墨强调色，硬朗圆角、几乎不投影。',
    skin: 'ink',
    primaryColor: '#1F7A72',
    fontFamily: 'serif',
    radiusScale: 'sharp',
    shadowDepth: 'flat',
    background: { type: 'gradient', from: '#f3f6f4', to: '#e4ece8' },
    surfaceOpacity: 90,
    surfaceBlur: 10
  },
  {
    id: 'sand',
    name: '暖砂',
    description: '衬线正文配陶土橙，圆角更软、投影更深，像摊开的旧纸。',
    skin: 'sand',
    primaryColor: '#C2703F',
    fontFamily: 'serif',
    radiusScale: 'soft',
    shadowDepth: 'deep',
    background: { type: 'gradient', from: '#faf6f0', to: '#efe3d6' },
    surfaceOpacity: 88,
    surfaceBlur: 12
  },
  {
    id: 'dusk',
    name: '暮紫',
    description: '深色画布配雾紫强调色，晚上看文章不刺眼。',
    skin: 'dusk',
    primaryColor: '#9D8BF5',
    fontFamily: 'sans',
    radiusScale: 'soft',
    shadowDepth: 'deep',
    background: { type: 'gradient', from: '#f4f7fc', to: '#e9eef7' },
    surfaceOpacity: 88,
    surfaceBlur: 12
  }
]

/** 预设里的数值字段在站点设置中都是字符串，这里统一转成字符串再写入。 */
function toSettingValue(value: string | number): string {
  return typeof value === 'number' ? String(value) : value
}

/**
 * 把一套预设写进站点设置表单对象。
 *
 * 只写预设覆盖到的键，作者在单项控件上做的其它调整（字号、行高、栏宽……）
 * 一律不动 —— 预设是「换一套底子」，不是「重置全部」。
 */
export function applyThemePreset(target: Record<string, string>, preset: ThemePreset): void {
  target.themeSkin = preset.skin
  target.themePrimaryColor = preset.primaryColor
  target.fontFamily = preset.fontFamily
  target.radiusScale = preset.radiusScale
  target.shadowDepth = preset.shadowDepth
  target.backgroundType = preset.background.type
  target.backgroundGradientFrom = preset.background.from
  target.backgroundGradientTo = preset.background.to
  target.surfaceOpacity = toSettingValue(preset.surfaceOpacity)
  target.surfaceBlur = toSettingValue(preset.surfaceBlur)
}

/** 判断当前设置是否与某套预设一致（用于在预设卡上标出「使用中」）。 */
export function isThemePresetActive(
  settings: Record<string, string | undefined>,
  preset: ThemePreset
): boolean {
  return (
    settings.themeSkin === preset.skin &&
    settings.themePrimaryColor === preset.primaryColor &&
    settings.fontFamily === preset.fontFamily &&
    settings.radiusScale === preset.radiusScale &&
    settings.shadowDepth === preset.shadowDepth &&
    settings.backgroundType === preset.background.type &&
    settings.backgroundGradientFrom === preset.background.from &&
    settings.backgroundGradientTo === preset.background.to
  )
}
