/**
 * 工具函数库
 */

/**
 * 格式化日期为相对时间
 */
export function formatRelativeTime(date: string | Date | null | undefined): string {
  if (!date) return ''
  const target = new Date(date)
  if (isNaN(target.getTime())) return ''
  const now = new Date()
  const diff = now.getTime() - target.getTime()
  if (diff < 0) {
    return formatDate(target)
  }

  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`

  // 30 天近似会把「还差几天满一年」的日期提前说成「1年前」；改用真实日历月差。
  let months =
    (now.getFullYear() - target.getFullYear()) * 12 + (now.getMonth() - target.getMonth())
  if (now.getDate() < target.getDate()) months -= 1
  if (months < 1) return `${days}天前`
  if (months < 12) return `${months}个月前`
  return `${Math.floor(months / 12)}年前`
}

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
 */
export interface PasswordStrength {
  level: 'none' | 'weak' | 'medium' | 'strong'
  text: string
  color: string
}

export function checkPasswordStrength(password: string | null | undefined): PasswordStrength {
  if (!password) return { level: 'none', text: '无', color: '#999' }
  let strength = 0

  if (password.length >= 8) strength++
  if (password.length >= 12) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[^a-zA-Z0-9]/.test(password)) strength++

  if (strength <= 2) return { level: 'weak', text: '弱', color: '#F44336' }
  if (strength <= 3) return { level: 'medium', text: '中', color: '#FF9800' }
  return { level: 'strong', text: '强', color: '#4CAF50' }
}
