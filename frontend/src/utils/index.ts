/**
 * 工具函数库
 */

/**
 * 格式化日期
 */
export function formatDate(
  date: string | Date | null | undefined,
  format = 'YYYY-MM-DD HH:mm:ss'
): string {
  if (!date) return ''
  const target = new Date(date)
  if (isNaN(target.getTime())) return ''
  const d = target
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 截断文本
 */
export function truncateText(text: string | null | undefined, maxLength = 100): string {
  if (text == null) return ''
  if (!text || text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

/**
 * 获取安全的图片URL
 * 处理后端返回的相对路径（如 /uploads/xxx.jpg），确保在任何环境下都能正确访问
 */
export function getSafeImageUrl(
  url: string | null | undefined,
  fallback = '/default-image.png'
): string {
  if (!url) return fallback

  // 完整的 http/https URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) return url

  // 协议相对 URL（//example.com），直接返回
  if (url.startsWith('//')) return url

  // 相对路径（如 /uploads/xxx.jpg），直接返回
  // 在 Vercel 环境中，会通过 vercel.json 的 rewrites 代理到后端
  if (url.startsWith('/')) return url

  // 其他情况返回 fallback
  return fallback
}

/**
 * 验证密码强度
 *
 * 只判**档位**，不产出颜色/文案：颜色映射放在 PasswordChange.vue 的 CSS 里
 * （语义 token 会随主题/皮肤切换，JS 里写死的 hex 做不到）。
 */
export type PasswordStrengthLevel = 'empty' | 'weak' | 'medium' | 'strong'

export interface PasswordStrength {
  level: PasswordStrengthLevel
}

export function checkPasswordStrength(password: string | null | undefined): PasswordStrength {
  if (!password) return { level: 'empty' }
  let strength = 0

  if (password.length >= 8) strength++
  if (password.length >= 12) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[^a-zA-Z0-9]/.test(password)) strength++

  if (strength <= 2) return { level: 'weak' }
  if (strength <= 3) return { level: 'medium' }
  return { level: 'strong' }
}
