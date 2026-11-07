/**
 * 全局搜索快捷键（Ctrl/Cmd+K 与 `/`）的判定。
 *
 * 文档类站点的惯例：任何非编辑场景下按 Ctrl/Cmd+K 或 `/` 直达搜索页。
 * 判定收敛成纯函数，App.vue 的 window keydown 监听只负责 preventDefault 与跳转；
 * 输入框、textarea、select、contenteditable 里的按键绝不能被抢走。
 */

/** 编辑态元素：用户在打字或操作控件，全局快捷键一律让路。 */
const EDITABLE_TAGS = new Set(['input', 'textarea', 'select'])

/** contenteditable 的三个「可编辑」取值：true、plaintext-only 与空字符串。 */
const EDITABLE_ATTR_VALUES = new Set(['', 'true', 'plaintext-only'])

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false
  if (target.isContentEditable) return true
  // jsdom 不实现 isContentEditable（恒为 undefined），单测环境用属性兜底；
  // 真实浏览器走上面的分支，两种读法语义一致。
  const editable = target.getAttribute('contenteditable')?.toLowerCase()
  if (editable !== undefined && EDITABLE_ATTR_VALUES.has(editable)) return true
  return EDITABLE_TAGS.has(target.tagName.toLowerCase())
}

export interface SearchShortcutMods {
  ctrlKey?: boolean
  metaKey?: boolean
  /** 长按产生的自动重复：一次按压只该跳一次。 */
  repeat?: boolean
}

/**
 * 这个按键事件是否应该打开搜索页。
 *
 * - repeat（长按重复）与编辑态元素直接忽略；
 * - Ctrl/Cmd+K 命中：e.key === 'k'，大小写都算（兼容大写锁定时 key 为 'K'）；
 * - 裸 `/` 命中；带 Ctrl/Meta 修饰的 `/` 不算（那是一些编辑器的组合键语义）。
 */
export function shouldOpenSearch(
  target: EventTarget | null,
  key: string,
  mods: SearchShortcutMods
): boolean {
  if (mods.repeat) return false
  if (isEditableTarget(target)) return false
  if (key === '/') return !mods.ctrlKey && !mods.metaKey
  return Boolean(mods.ctrlKey || mods.metaKey) && key.toLowerCase() === 'k'
}
