/**
 * 内置 UI 图标集。
 *
 * 全部为 24×24 描边图形，靠 `currentColor` 取色，因此深浅两套主题
 * 自动适配，不需要维护两份资源。站点品牌图标（logo/favicon）不走这里，
 * 由后台上传后通过设置下发。
 */
export const icons = {
  logo: '<path d="M12 2.5 20.5 7.4v9.2L12 21.5 3.5 16.6V7.4z"/><path d="M9 15.5 12 8.5l3 7"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m16.5 16.5 4 4"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/>',
  moon: '<path d="M20.5 15.5A8.5 8.5 0 0 1 8.5 3.5 8.5 8.5 0 1 0 20.5 15.5Z"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  home: '<path d="M4 10.5 12 4l8 6.5V19a1 1 0 0 1-1 1h-4v-5H9v5H5a1 1 0 0 1-1-1z"/>',
  archive: '<path d="M4 5h16v3H4zM5.5 8v11h13V8M10 12h4"/>',
  image:
    '<rect x="3.5" y="4.5" width="17" height="15" rx="2.5"/><circle cx="8.5" cy="9.5" r="1.6"/><path d="M5 17.5 10 13l3.5 3.5L16 14l3.5 3.5"/>',
  info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5M12 7.6h.01"/>',
  settings:
    '<path d="M5 7h14M5 12h14M5 17h14"/><circle cx="9" cy="7" r="2"/><circle cx="15" cy="12" r="2"/><circle cx="8" cy="17" r="2"/>',
  gauge: '<path d="M4 17a8 8 0 1 1 16 0"/><path d="m12 17 4-5"/><circle cx="12" cy="17" r="1.4"/>',
  'file-text': '<path d="M6 3.5h7l5 5V20.5H6z"/><path d="M13 3.5v5h5M9 13h6M9 16.5h4"/>',
  tag: '<path d="M12.6 3.5H20.5v7.9L11 21 3 13z"/><circle cx="16.8" cy="7.2" r="1.4"/>',
  users:
    '<circle cx="9.5" cy="8.5" r="3.2"/><path d="M3.5 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5"/><path d="M16 5.6a3.2 3.2 0 0 1 0 5.9M17.5 14.9c2.1.6 3.5 2.4 3.5 4.6"/>',
  message:
    '<path d="M20.5 12.5c0 3.6-3.8 6.5-8.5 6.5-1 0-2-.1-2.9-.4L4 20.5l1.3-3.4A6.9 6.9 0 0 1 3.5 12.5C3.5 8.9 7.3 6 12 6s8.5 2.9 8.5 6.5z"/>',
  music:
    '<path d="M9 18V6.5l10-2v11"/><circle cx="6.5" cy="18" r="2.5"/><circle cx="16.5" cy="15.5" r="2.5"/>',
  play: '<path d="M8 5.5 18.5 12 8 18.5z"/>',
  pause: '<path d="M9 5.5v13M15 5.5v13"/>',
  'arrow-up': '<path d="M12 19V5M6 11l6-6 6 6"/>',
  external:
    '<path d="M14 4.5h5.5V10M19 5l-8 8"/><path d="M18 13.5V19a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5.5"/>'
} as const

export type IconName = keyof typeof icons

export const iconNames = Object.keys(icons) as IconName[]
